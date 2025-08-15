// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.parse

import com.autonomousapps.exception.BuildScriptParseException
import com.autonomousapps.internal.advice.AdvicePrinter
import com.autonomousapps.internal.antlr.v4.runtime.*
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
  /** Style information from preprocessing */
  private val styleMap: Map<String, Boolean>,
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
      insertAdvice(advice, ctx.stop, withDependenciesBlock = false)
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

    insertAdvice(advice, ctx.stop, withDependenciesBlock = true)
  }

  private fun insertAdvice(advice: Set<Advice>, beforeToken: Token, withDependenciesBlock: Boolean) {
    val prefix = if (withDependenciesBlock) "\ndependencies {\n" else ""
    val postfix = if (withDependenciesBlock) "\n}\n" else "\n"

    advice.filterToOrderedSet { it.isAnyAdd() }.ifNotEmpty { addAdvice ->
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

      val advice = adviceFinder.findAdvice(declaration)
      val originalText = tokens.getText(context.start, context.stop)
      
      if (advice != null) {
        if (advice.isAnyRemove()) {
          rewriter.delete(context.start, context.stop)
          rewriter.deleteWhitespaceToLeft(context.start)
          rewriter.deleteNewlineToRight(context.stop)
        } else if (advice.isAnyChange()) {
          val replacement = createStyleAwareReplacement(advice, originalText)
          rewriter.replace(context.start, context.stop, replacement.trim())
        }
      } else if (TypeSafeAccessorPreprocessor.isPreprocessedAccessor(originalText)) {
        // Restore original syntax for unchanged dependencies that were preprocessed
        val restoredText = TypeSafeAccessorPreprocessor.restoreOriginalSyntax(originalText, styleMap)
        if (restoredText != originalText) {
          rewriter.replace(context.start, context.stop, restoredText.trim())
        }
      }
    }
  }

  /**
   * Creates a replacement string that preserves the original syntax style.
   */
  private fun createStyleAwareReplacement(advice: Advice, originalText: String): String {
    val accessorInfo = TypeSafeAccessorUtils.extractAccessorInfo(originalText)
    
    // For type-safe accessors, check if original had parentheses
    val useParentheses = if (accessorInfo != null) {
      val key = "${accessorInfo.configuration} ${accessorInfo.accessor}"
      styleMap[key] ?: true  // Default to parentheses if not in style map
    } else {
      true // Non-type-safe accessors use parentheses
    }
    
    // Create a style-aware printer for this specific replacement
    return createAdvicePrinterWithStyle(useParentheses).toDeclaration(advice)
  }
  
  /**
   * Creates an AdvicePrinter with the same configuration as the main printer
   * but with the specified parentheses style.
   */
  private fun createAdvicePrinterWithStyle(useParentheses: Boolean): AdvicePrinter {
    // We need to access the main printer's configuration
    // For now, we'll restore the public accessors temporarily
    return AdvicePrinter(
      dslKind = com.autonomousapps.internal.advice.DslKind.KOTLIN,
      dependencyMap = printer.getDependencyMap,
      useTypesafeProjectAccessors = printer.usesTypesafeProjectAccessors,
      useParenthesesSyntax = useParentheses
    )
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
      val originalContent = file.toFile().readText()
      val preprocessingResult = TypeSafeAccessorPreprocessor.preprocess(originalContent)

      return Parser(
        file = TypeSafeAccessorPreprocessor.createInputStream(preprocessingResult),
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
            styleMap = preprocessingResult.styleMap
          )
        }
      ).listener()
    }
  }
}
