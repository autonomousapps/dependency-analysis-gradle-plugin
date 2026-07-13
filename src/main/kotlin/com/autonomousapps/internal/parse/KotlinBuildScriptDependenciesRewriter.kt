// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.parse

import com.autonomousapps.exception.BuildScriptParseException
import com.autonomousapps.internal.advice.AdvicePrinter
import com.autonomousapps.internal.antlr.v4.runtime.CharStream
import com.autonomousapps.internal.antlr.v4.runtime.CommonTokenStream
import com.autonomousapps.internal.antlr.v4.runtime.ParserRuleContext
import com.autonomousapps.internal.antlr.v4.runtime.Token
import com.autonomousapps.internal.cash.grammar.kotlindsl.parse.Parser
import com.autonomousapps.internal.cash.grammar.kotlindsl.parse.Rewriter
import com.autonomousapps.internal.cash.grammar.kotlindsl.utils.Blocks.isBuildscript
import com.autonomousapps.internal.cash.grammar.kotlindsl.utils.Blocks.isDependencies
import com.autonomousapps.internal.cash.grammar.kotlindsl.utils.CollectingErrorListener
import com.autonomousapps.internal.cash.grammar.kotlindsl.utils.Context.leafRule
import com.autonomousapps.internal.cash.grammar.kotlindsl.utils.DependencyExtractor
import com.autonomousapps.internal.cash.grammar.kotlindsl.utils.Whitespace
import com.autonomousapps.internal.parse.advice.AdviceFinder
import com.autonomousapps.internal.squareup.cash.grammar.KotlinParser.*
import com.autonomousapps.internal.squareup.cash.grammar.KotlinParserBaseListener
import com.autonomousapps.internal.utils.filterToOrderedSet
import com.autonomousapps.internal.utils.ifNotEmpty
import com.autonomousapps.model.Advice
import com.autonomousapps.model.internal.ProjectType
import java.nio.file.Path

/**
 * Rewrites a Kotlin build script based on provided advice, which includes:
 *
 * - When entering the `dependencies` block, processing each dependency declaration to either remove or update it
 *   according to the advice provided.
 * - Upon exiting the `dependencies` block, adding any new dependencies.
 * - At the end of the script, adding a `dependencies` block if new dependencies are present and the block is currently
 *   missing.
 *
 * Note: Buildscript dependencies are ignored.
 */
internal class KotlinBuildScriptDependenciesRewriter(
  private val input: CharStream,
  private val tokens: CommonTokenStream,
  private val errorListener: CollectingErrorListener,
  private val advice: Set<Advice>,
  private val printer: AdvicePrinter,
  /** Reverse map from custom representation to standard. */
  private val reversedDependencyMap: (String) -> String,
  private val projectType: ProjectType,
  private val sourceSetNames: Set<String>,
) : BuildScriptDependenciesRewriter, KotlinParserBaseListener() {

  private val rewriter = TrackingRewriter(Rewriter(tokens))
  private val indent = Whitespace.computeIndent(tokens, input)

  private val addAdvice: MutableSet<Advice> = advice.filterTo(sortedSetOf()) { it.isAnyAdd() }

  private val adviceFinder = AdviceFinder.of(projectType, advice, reversedDependencyMap)
  private val dependencyExtractor = DependencyExtractor(
    input = input,
    tokens = tokens,
    indent = indent,
  )

  private var hasDependenciesBlock = false
  private var inBuildscriptBlock = false
  private var inKotlinBlock = false
  private var inKotlinSourceSetsBlock = false

  /** Returns true if there are any changes. */
  override fun hasChanges(): Boolean = rewriter.hasChanges

  /**
   * Returns build script with advice applied.
   *
   * Throws [BuildScriptParseException] if the script has some idiosyncrasy that impairs parsing.
   *
   */
  @Throws(BuildScriptParseException::class)
  override fun rewritten(): String {
    errorListener.getErrorMessages().ifNotEmpty {
      throw BuildScriptParseException.withErrors(it)
    }

    return rewriter.text
  }

  override fun enterNamedBlock(ctx: NamedBlockContext) {
    dependencyExtractor.onEnterBlock()

    if (ctx.isBuildscript) {
      inBuildscriptBlock = true
    }

    if (isInKotlinBlock(ctx)) {
      inKotlinBlock = true
    }

    if (isInKotlinSourceSetsBlock(ctx)) {
      inKotlinSourceSetsBlock
    }

    if (ctx.isDependencies) {
      handleDependencies(ctx)
    }
  }

  /** Entering or exiting `kotlin { ... }` */
  private fun isInKotlinBlock(ctx: NamedBlockContext): Boolean {
    return ctx.name().text == "kotlin"
  }

  /** Entering or exiting `kotlin { sourceSets { ... } }` */
  private fun isInKotlinSourceSetsBlock(ctx: NamedBlockContext): Boolean {
    val name = ctx.name().text
    return (name == "sourceSets" && inKotlinBlock) || name == "kotlin.sourceSets"
  }

  override fun exitNamedBlock(ctx: NamedBlockContext) {
    if (projectType == ProjectType.KMP) {
      if (ctx.isDependencies && !inBuildscriptBlock) {
        insertAdvice(
          beforeToken = ctx.stop,
          addDependenciesBlock = false,
          scope = scope(ctx),
          ktfmt = ctx.start.line == ctx.stop.line,
          exitingScript = false,
        )
      } else if (isInKotlinSourceSetsBlock(ctx)) {
        // Exiting the kotlin.sourceSets block (KMP). Add any deps that didn't have a pre-existing dependencies block.
        insertAdvice(
          beforeToken = ctx.stop,
          addDependenciesBlock = true,
          scope = scope(ctx),
          ktfmt = ctx.start.line == ctx.stop.line,
          exitingScript = false,
        )
      }
    } else if (ctx.isDependencies && !inBuildscriptBlock) {
      // Exiting the dependencies block (non-KMP)
      hasDependenciesBlock = true

      insertAdvice(
        beforeToken = ctx.stop,
        addDependenciesBlock = false,
        ktfmt = ctx.start.line == ctx.stop.line,
      )
    }

    /*
     * Order is important (inner to outer)
     */

    if (ctx.isBuildscript) {
      inBuildscriptBlock = false
    }
    if (isInKotlinSourceSetsBlock(ctx)) {
      inKotlinSourceSetsBlock = false
    }
    if (isInKotlinBlock(ctx)) {
      inKotlinBlock = false
    }

    dependencyExtractor.onExitBlock()
  }

  override fun exitScript(ctx: ScriptContext) {
    // Exit early if this build script has a dependencies block. If it doesn't, we may need to add missing dependencies.
    if (hasDependenciesBlock) return

    if (projectType == ProjectType.KMP) {
      insertAdvice(
        beforeToken = ctx.stop,
        addDependenciesBlock = true,
        scope = "",
        ktfmt = false,
        exitingScript = true,
      )
    } else {
      insertAdvice(
        beforeToken = ctx.stop,
        addDependenciesBlock = true,
        ktfmt = false,
      )
    }
  }

  /**
   * non-KMP.
   *
   * ktfmt has this extremely dumb behavior where it collapses blocks to be on one line whenever possible. It will do
   * this:
   * ```
   * dependencies { implementation(libs.foo.bar) }
   * ```
   * This probably happens with other tools but I'm feeling petty. If we detect that the start and stop of a
   * `dependencies` block is on the same line, we insert a single newline before adding new dependencies, else the build
   * script wouldn't compile because it was looking like this:
   * ```
   * dependencies { implementation(libs.foo.bar) implementation(libs.baz) }
   * ```
   */
  private fun insertAdvice(beforeToken: Token, addDependenciesBlock: Boolean, ktfmt: Boolean) {
    require(projectType != ProjectType.KMP) { "Expected non-KMP project." }

    if (addAdvice.isEmpty()) return

    val prefix = if (addDependenciesBlock) "\ndependencies {\n" else if (ktfmt) "\n" else ""
    val postfix = if (addDependenciesBlock) "\n}\n" else "\n"

    rewriter.insertBefore(
      beforeToken,
      addAdvice.joinToString(prefix = prefix, postfix = postfix, separator = "\n") { a ->
        printer.toDeclaration(a)
      }
    )

    addAdvice.clear()
  }

  /** KMP. */
  private fun insertAdvice(
    beforeToken: Token,
    addDependenciesBlock: Boolean,
    scope: String,
    ktfmt: Boolean,
    exitingScript: Boolean,
  ) {
    require(projectType == ProjectType.KMP) { "Expected KMP project. Was '$projectType'." }

    if (addAdvice.isEmpty()) return

    val localIndent = if (ktfmt) {
      ""
    } else if (!exitingScript) {
      " ".repeat(beforeToken.charPositionInLine)
    } else {
      indent
    }
    val prefix = if (addDependenciesBlock) "\ndependencies {\n" else if (ktfmt) "\n" else ""
    val postfix = if (addDependenciesBlock) "\n}\n" else "\n$localIndent"

    addAdvice
      // Find scoped advice. If scope="", then this will be all add-advice.
      .filterToOrderedSet { a -> a.toConfiguration!!.startsWith(scope) }
      .ifNotEmpty { advice ->
        if (addDependenciesBlock && !exitingScript) {
          // adding a dependencies block at the end of a kotlin.sourceSets block
          sourceSetNames.forEach { sourceSetName ->
            advice
              .filterToOrderedSet { it.toConfiguration!!.startsWith(sourceSetName) }
              .ifNotEmpty { scopedAdvice ->
                val text = buildString {
                  append(localIndent)
                  appendLine("$sourceSetName.dependencies {")
                  scopedAdvice.forEach { a ->
                    append(localIndent)
                    append(localIndent)
                    appendLine(printer.toDeclaration(a, sourceSetName))
                  }
                  append(localIndent)
                  append(localIndent)
                  appendLine("}")
                  append(localIndent)
                }

                rewriter.insertBefore(beforeToken, text)

                addAdvice.removeAll(scopedAdvice)
              }
          }
        } else if (exitingScript) {
          // adding a dependencies block at the end of a script that previously didn't have one
          val text = buildString {
            appendLine()
            appendLine("kotlin {")
            append(localIndent)
            appendLine("sourceSets {")

            sourceSetNames.forEach { sourceSetName ->
              advice
                .filterToOrderedSet { it.toConfiguration!!.startsWith(sourceSetName) }
                .ifNotEmpty { scopedAdvice ->
                  append(localIndent)
                  append(localIndent)
                  appendLine("$sourceSetName.dependencies {")
                  scopedAdvice.forEach { a ->
                    append(localIndent)
                    append(localIndent)
                    appendLine(printer.toDeclaration(a, sourceSetName))
                  }
                  append(localIndent)
                  append(localIndent)
                  appendLine("}")

                  addAdvice.removeAll(scopedAdvice)
                }
            }

            append(localIndent)
            appendLine("}")
            appendLine("}")
          }

          rewriter.insertBefore(beforeToken, text)
        } else {
          // Exiting a scoped dependencies block
          require(scope.isNotEmpty()) {
            "Expected scope to be non-empty. E.g., \"commonMain\", \"jvmTest\", etc."
          }

          rewriter.insertBefore(
            beforeToken,
            advice.joinToString(prefix = prefix, postfix = postfix, separator = "\n") { a ->
              printer.toDeclaration(a, scope)
            }
          )

          addAdvice.removeAll(advice)
        }
      }
  }

  private fun handleDependencies(ctx: NamedBlockContext) {
    if (inBuildscriptBlock) return

    // TODO(tsr): dependencyDeclarations incorrectly calls `api(project.dependencies.platform("com.squareup.okio:okio-bom:3.16.4"))`
    //  a PROJECT (vs MODULE) dependency with DEFAULT (vs PLATFORM) capabilities. Fix this (probably in kotlin-editor).
    val dependencyContainer = dependencyExtractor.collectDependencies(ctx)
    dependencyContainer.getDependencyDeclarationsWithContext().forEach {
      val context = it.statement.leafRule() as? PostfixUnaryExpressionContext ?: return@forEach
      val declaration = it.declaration

      val scope = scope(ctx)
      adviceFinder.findAdvice(declaration, scope)?.let { a ->
        if (a.isAnyRemove()) {
          rewriter.delete(context.start, context.stop)
          rewriter.deleteWhitespaceToLeft(context.start)
          rewriter.deleteNewlineToRight(context.stop)
        } else if (a.isAnyChange()) {
          rewriter.replace(context.start, context.stop, printer.toDeclaration(a, scope).trim())
        }
      }
    }
  }

  /**
   * For KMP projects. For non-KMP, returns an empty string.
   *
   * 1:
   * ```
   * commonMain.dependencies { ... }
   * ```
   *
   * 2:
   * ```
   * commonMain {
   *   dependencies { ... }
   * }
   * ```
   *
   * In either case, return "commonMain". Otherwise, return an empty string.
   */
  private fun scope(ctx: NamedBlockContext): String {
    return if (projectType == ProjectType.KMP) {
      val name = ctx.name().text
      if (name.endsWith(".dependencies")) {
        // commonMain.dependencies { ... }
        name.substringBefore('.')
      } else if (name == "dependencies") {
        // commonMain { dependencies { ... } }
        val parentBlock = parentBlock(ctx)
        if (parentBlock != null) {
          // commonMain
          parentBlock.name().text
        } else {
          // some other weird thing
          ""
        }
      } else {
        // ctx is not a dependencies block
        ""
      }
    } else {
      // not a KMP project
      // dependencies
      ""
    }
  }

  private fun parentBlock(ctx: ParserRuleContext): NamedBlockContext? {
    var parent = ctx.parent
    while (parent !is ScriptContext) {
      if (parent is NamedBlockContext) {
        return parent
      }

      parent = parent.parent
    }

    return null
  }

  companion object {
    @JvmStatic
    fun of(
      projectType: ProjectType,
      sourceSetNames: Set<String>,
      file: Path,
      advice: Set<Advice>,
      advicePrinter: AdvicePrinter,
      reversedDependencyMap: (String) -> String = { it },
    ): KotlinBuildScriptDependenciesRewriter {
      val errorListener = CollectingErrorListener()

      return Parser(
        file = Parser.readOnlyInputStream(file),
        errorListener = errorListener,
        startRule = { it.script() },
        listenerFactory = { input, tokens, _ ->
          KotlinBuildScriptDependenciesRewriter(
            input = input,
            tokens = tokens,
            errorListener = errorListener,
            advice = advice,
            printer = advicePrinter,
            reversedDependencyMap = reversedDependencyMap,
            projectType = projectType,
            sourceSetNames = sourceSetNames,
          )
        }
      ).listener()
    }
  }
}
