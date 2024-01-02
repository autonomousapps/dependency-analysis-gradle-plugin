// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.parse

import com.autonomousapps.exception.BuildScriptParseException
import com.autonomousapps.grammar.gradle.GradleScript
import com.autonomousapps.grammar.gradle.GradleScript.*
import com.autonomousapps.grammar.gradle.GradleScriptBaseListener
import com.autonomousapps.grammar.gradle.GradleScriptLexer
import com.autonomousapps.internal.advice.AdvicePrinter
import com.autonomousapps.internal.antlr.v4.runtime.*
import com.autonomousapps.internal.antlr.v4.runtime.tree.ParseTreeWalker
import com.autonomousapps.internal.parse.GradleBuildScriptDependenciesRewriter.CtxDependency.DependencyKind
import com.autonomousapps.internal.utils.filterToOrderedSet
import com.autonomousapps.internal.utils.filterToSet
import com.autonomousapps.internal.utils.ifNotEmpty
import com.autonomousapps.model.Advice
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

internal class GradleBuildScriptDependenciesRewriter private constructor(
  private val tokens: CommonTokenStream,
  private val rewriter: TokenStreamRewriter,
  private val errorListener: RewriterErrorListener,
  private val printer: AdvicePrinter,
  private val advice: Set<Advice>,
  /** Reverse map from custom representation to standard. */
  private val reversedDependencyMap: (String) -> String,
) : GradleScriptBaseListener() {

  private class RewriterErrorListener : AbstractErrorListener() {
    val errorMessages = mutableListOf<String>()

    override fun syntaxError(
      recognizer: Recognizer<*, *>,
      offendingSymbol: Any,
      line: Int,
      charPositionInLine: Int,
      msg: String,
      e: RecognitionException?,
    ) {
      errorMessages.add("$msg; $line:$charPositionInLine")
    }
  }

  private class CtxDependency(
    val dependency: DependencyContext,
    val configuration: ConfigurationContext,
  ) {

    enum class DependencyKind {
      PROJECT,
      FILE,
      EXTERNAL
    }

    fun dependencyKind(): DependencyKind {
      return if (isProjectDependency()) {
        DependencyKind.PROJECT
      } else if (isFileDependency()) {
        DependencyKind.FILE
      } else if (isExternalDependency()) {
        DependencyKind.EXTERNAL
      } else {
        throw RuntimeException("Unknown dependency kind. Was '$dependency'")
      }
    }

    private fun isProjectDependency() = dependency.projectDependency() != null
    private fun isFileDependency() = dependency.fileDependency() != null
    private fun isExternalDependency() = dependency.externalDependency() != null
  }

  private var hasDependenciesBlock = false
  private var inBuildscriptBlock = false

  @Throws(BuildScriptParseException::class)
  fun rewritten(): String {
    errorListener.errorMessages.ifNotEmpty {
      throw BuildScriptParseException.withErrors(errorListener.errorMessages)
    }
    return rewriter.text
  }

  override fun enterBuildscript(ctx: BuildscriptContext) {
    inBuildscriptBlock = true
  }

  override fun exitBuildscript(ctx: BuildscriptContext) {
    inBuildscriptBlock = false
  }

  override fun enterDependencies(ctx: DependenciesContext) {
    hasDependenciesBlock = true
  }

  override fun exitDependencies(ctx: DependenciesContext) {
    // Don't touch buildscript dependencies
    if (inBuildscriptBlock) return

    // Add all new dependencies to the end of the block
    advice.filterToSet { it.isAnyAdd() }.ifNotEmpty { addAdvice ->
      val closeBrace = ctx.stop
      rewriter.insertBefore(closeBrace, addAdvice.joinToString(separator = "\n", postfix = "\n") { a ->
        printer.toDeclaration(a)
      })
    }
  }

  override fun enterNormalDeclaration(ctx: NormalDeclarationContext) {
    handleDeclaration(ctx, CtxDependency(ctx.dependency(), ctx.configuration()))
  }

  override fun enterPlatformDeclaration(ctx: PlatformDeclarationContext) {
    handleDeclaration(ctx, CtxDependency(ctx.dependency(), ctx.configuration()))
  }

  override fun enterTestFixturesDeclaration(ctx: TestFixturesDeclarationContext) {
    handleDeclaration(ctx, CtxDependency(ctx.dependency(), ctx.configuration()))
  }

  override fun exitScript(ctx: ScriptContext) {
    // Exit early if this build script has a dependencies block. If it doesn't, we may need to add missing dependencies.
    if (hasDependenciesBlock) return

    advice.filterToOrderedSet { it.isAnyAdd() }.ifNotEmpty { addAdvice ->
      rewriter.insertBefore(
        ctx.stop,
        addAdvice.joinToString(prefix = "\ndependencies {\n", postfix = "\n}\n", separator = "\n") { a ->
          printer.toDeclaration(a)
        }
      )
    }
  }

  private fun handleDeclaration(ctx: ParserRuleContext, ctxDependency: CtxDependency) {
    // Don't touch buildscript dependencies
    if (inBuildscriptBlock) return

    findAdvice(ctxDependency)?.let { a ->
      if (a.isAnyRemove()) {
        // Delete preceding whitespace. This is a bit too aggressive; let's see if anyone complains!
        tokens.getHiddenTokensToLeft(ctx.start.tokenIndex).orEmpty().forEach { rewriter.delete(it) }

        // Remove declaration of unused dependency
        rewriter.delete(ctx.start, ctx.stop)
      } else if (a.isAnyChange()) {
        val c = ctxDependency.configuration
        rewriter.replace(c.start, c.stop, a.toConfiguration)
      }
    }
  }

  /**
   * Find advice matching the given dependency. This requires some normalization, since what the parser provides is
   * slightly different than the representation in the advice.
   */
  private fun findAdvice(ctxDependency: CtxDependency): Advice? {
    val currentConfiguration = tokens.getText(ctxDependency.configuration)
    val dependency = reversedDependencyMap(tokens.getText(ctxDependency.dependency))

    fun String.normalize(vararg prefixes: String): String {
      var s = this
      prefixes.forEach { s = s.removePrefix(it) }
      return s.removePrefix("(")
        .removePrefix("'").removePrefix("\"")
        .removeSuffix(")")
        .removeSuffix("'").removeSuffix("\"")
    }

    val normalizedGav = when (ctxDependency.dependencyKind()) {
      // strip "project()" (where parens are optional because Groovy)
      // strip double or single quotes as well
      DependencyKind.PROJECT -> dependency.normalize("project")

      // strip "file()" or "files()" (where parens are optional because Groovy)
      // strip double or single quotes as well
      DependencyKind.FILE -> dependency.normalize("files", "file")

      // nothing to change
      DependencyKind.EXTERNAL -> dependency
    }

    return advice.find {
      it.coordinates.gav() == normalizedGav && it.fromConfiguration == currentConfiguration
    }
  }

  internal companion object {
    fun newRewriter(
      file: Path,
      advice: Set<Advice>,
      advicePrinter: AdvicePrinter,
      reversedDependencyMap: (String) -> String = { it },
    ): GradleBuildScriptDependenciesRewriter {
      val input = Files.newInputStream(file, StandardOpenOption.READ).use { CharStreams.fromStream(it) }
      val lexer = GradleScriptLexer(input)
      val tokens = CommonTokenStream(lexer)
      val parser = GradleScript(tokens)

      val errorListener = RewriterErrorListener()
      parser.addErrorListener(errorListener)

      val walker = ParseTreeWalker()
      val listener = GradleBuildScriptDependenciesRewriter(
        tokens = tokens,
        rewriter = TokenStreamRewriter(tokens),
        errorListener = errorListener,
        printer = advicePrinter,
        advice = advice,
        reversedDependencyMap = reversedDependencyMap,
      )
      val tree = parser.script()
      walker.walk(listener, tree)

      return listener
    }
  }
}
