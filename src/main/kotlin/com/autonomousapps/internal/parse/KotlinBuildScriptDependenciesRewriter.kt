// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.parse

import com.autonomousapps.exception.BuildScriptParseException
import com.autonomousapps.internal.advice.AdvicePrinter
import com.autonomousapps.internal.antlr.v4.runtime.CharStream
import com.autonomousapps.internal.antlr.v4.runtime.CommonTokenStream
import com.autonomousapps.internal.antlr.v4.runtime.Token
import com.autonomousapps.internal.cash.grammar.kotlindsl.parse.Parser
import com.autonomousapps.internal.cash.grammar.kotlindsl.parse.Rewriter
import com.autonomousapps.internal.cash.grammar.kotlindsl.utils.Blocks.isBuildscript
import com.autonomousapps.internal.cash.grammar.kotlindsl.utils.Blocks.isDependencies
import com.autonomousapps.internal.cash.grammar.kotlindsl.utils.CollectingErrorListener
import com.autonomousapps.internal.cash.grammar.kotlindsl.utils.Context.leafRule
import com.autonomousapps.internal.cash.grammar.kotlindsl.utils.DependencyExtractor
import com.autonomousapps.internal.cash.grammar.kotlindsl.utils.Whitespace
import com.autonomousapps.internal.squareup.cash.grammar.KotlinParser.*
import com.autonomousapps.internal.squareup.cash.grammar.KotlinParserBaseListener
import com.autonomousapps.internal.utils.filterToOrderedSet
import com.autonomousapps.internal.utils.ifNotEmpty
import com.autonomousapps.model.Advice
import java.nio.file.Path

/**
 * Rewrites a Kotlin build script based on provided advice, which includes:
 *
 * - When entering the `dependencies` block, processing each dependency declaration to either remove or update it according to the advice provided.
 * - Upon exiting the `dependencies` block, adding any new dependencies.
 * - At the end of the script, adding a `dependencies` block if new dependencies are present and the block is currently missing.
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
) : BuildScriptDependenciesRewriter, KotlinParserBaseListener() {

  private val rewriter = Rewriter(tokens)
  private val indent = Whitespace.computeIndent(tokens, input)

  private val adviceFinder = AdviceFinder(advice, reversedDependencyMap)
  private val dependencyExtractor = DependencyExtractor(
    input = input,
    tokens = tokens,
    indent = indent,
  )

  private var hasDependenciesBlock = false
  private var inBuildscriptBlock = false

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

    if (ctx.isDependencies) {
      handleDependencies(ctx)
    }
  }

  override fun exitNamedBlock(ctx: NamedBlockContext) {
    if (ctx.isDependencies && !inBuildscriptBlock) {
      hasDependenciesBlock = true
      val ktfmt = ctx.start.line == ctx.stop.line
      insertAdvice(advice, ctx.stop, withDependenciesBlock = false, ktfmt = ktfmt)
    }

    // Must be last
    if (ctx.isBuildscript) {
      inBuildscriptBlock = false
    }

    dependencyExtractor.onExitBlock()
  }

  override fun exitScript(ctx: ScriptContext) {
    // Exit early if this build script has a dependencies block. If it doesn't, we may need to add missing dependencies.
    if (hasDependenciesBlock) return

    insertAdvice(advice, ctx.stop, withDependenciesBlock = true, ktfmt = false)
  }

  /**
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
  private fun insertAdvice(advice: Set<Advice>, beforeToken: Token, withDependenciesBlock: Boolean, ktfmt: Boolean) {
    val prefix = if (withDependenciesBlock) "\ndependencies {\n" else if (ktfmt) "\n" else ""
    val postfix = if (withDependenciesBlock) "\n}\n" else "\n"

    advice
      .filterToOrderedSet { it.isAnyAdd() }
      .ifNotEmpty { addAdvice ->
        rewriter.insertBefore(
          beforeToken,
          addAdvice.joinToString(prefix = prefix, postfix = postfix, separator = "\n") { a ->
            printer.toDeclaration(a)
          }
        )
      }
  }

  private fun handleDependencies(ctx: NamedBlockContext) {
    if (inBuildscriptBlock) return

    val dependencyContainer = dependencyExtractor.collectDependencies(ctx)
    dependencyContainer.getDependencyDeclarationsWithContext().forEach {
      val context = it.statement.leafRule() as? PostfixUnaryExpressionContext ?: return@forEach
      val declaration = it.declaration

      adviceFinder.findAdvice(declaration)?.let { a ->
        if (a.isAnyRemove()) {
          rewriter.delete(context.start, context.stop)
          rewriter.deleteWhitespaceToLeft(context.start)
          rewriter.deleteNewlineToRight(context.stop)
        } else if (a.isAnyChange()) {
          rewriter.replace(context.start, context.stop, printer.toDeclaration(a).trim())
        }
      }
    }
  }

  companion object {
    @JvmStatic
    fun of(
      file: Path,
      advice: Set<Advice>,
      advicePrinter: AdvicePrinter,
      reversedDependencyMap: (String) -> String = { it }
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
            reversedDependencyMap = reversedDependencyMap
          )
        }
      ).listener()
    }
  }
}
