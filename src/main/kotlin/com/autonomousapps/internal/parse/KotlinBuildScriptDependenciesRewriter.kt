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
import com.autonomousapps.model.ProjectCoordinates
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
  /** Original file content before preprocessing, used for style detection */
  private val originalFileContent: String,
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
          createStyleAwareAdvicePrinterForAdvice(a).toDeclaration(a)
        }
      )
    }
  }

  /**
   * Creates an AdvicePrinter for a specific advice, applying style preference only to type-safe accessors.
   */
  private fun createStyleAwareAdvicePrinterForAdvice(advice: Advice): AdvicePrinter {
    val useParentheses = if (advice.coordinates is ProjectCoordinates) {
      // For type-safe project accessors, use the file's style preference
      detectFileStylePreference()
    } else {
      // For all other dependencies (regular libraries, etc.), always use parentheses
      true
    }
    
    return printer.copy(useParenthesesSyntax = useParentheses)
  }

  /**
   * Detects the overall file style preference by analyzing the original content.
   * Returns true if the file generally uses parentheses, false if it prefers non-parentheses style.
   */
  private fun detectFileStylePreference(): Boolean {
    // Count type-safe accessors with parentheses vs without
    val parenthesesCount = countTypeAwareAccessorsWithParentheses(originalFileContent)
    val nonParenthesesCount = countTypeAwareAccessorsWithoutParentheses(originalFileContent)
    
    // If we have non-parentheses accessors, prefer that style
    // Otherwise, prefer parentheses (default)
    return nonParenthesesCount == 0 || parenthesesCount >= nonParenthesesCount
  }

  private fun countTypeAwareAccessorsWithParentheses(content: String): Int {
    val pattern = Regex("""\b\w+\s*\(\s*((?:projects|libs)\.[\w.]+)\s*\)""")
    return pattern.findAll(content).count()
  }

  private fun countTypeAwareAccessorsWithoutParentheses(content: String): Int {
    val pattern = Regex("""\b\w+\s+((?:projects|libs)\.[\w.]+)(?!\s*\()""")
    return pattern.findAll(content).count()
  }

  /**
   * Checks if a dependency declaration needs syntax restoration.
   * This happens when preprocessing added parentheses to a type-safe accessor that originally didn't have them.
   */
  private fun needsSyntaxRestoration(currentText: String): Boolean {
    // Check if current text has parentheses around a type-safe accessor
    val pattern = Regex("""\b\w+\s*\(\s*((?:projects|libs)\.[\w.]+)\s*\)""")
    if (pattern.find(currentText.trim()) == null) {
      return false
    }
    
    // Check if the original file content had this same dependency without parentheses
    val cleanText = currentText.trim()
    val nonParenthesesVersion = cleanText.replace(Regex("""\(\s*((?:projects|libs)\.[\w.]+)\s*\)""")) { match ->
      " ${match.groupValues[1]}"
    }
    
    return originalFileContent.contains(nonParenthesesVersion)
  }

  /**
   * Restores the original syntax for a dependency declaration.
   * Converts "implementation(projects.myModule)" back to "implementation projects.myModule" 
   * if that was the original syntax.
   */
  private fun restoreOriginalSyntax(currentText: String): String {
    val pattern = Regex("""\b(\w+)\s*\(\s*((?:projects|libs)\.[\w.]+)\s*\)""")
    return pattern.replace(currentText) { match ->
      val configuration = match.groupValues[1]
      val accessor = match.groupValues[2]
      "$configuration $accessor"
    }
  }

  private fun handleDependencies(ctx: NamedBlockContext) {
    if (inBuildscriptBlock) return

    val dependencyContainer = dependencyExtractor.collectDependencies(ctx)
    dependencyContainer.getDependencyDeclarationsWithContext().forEach {
      val context = it.statement.leafRule() as? PostfixUnaryExpressionContext ?: return@forEach
      val declaration = it.declaration

      val advice = adviceFinder.findAdvice(declaration)
      if (advice != null) {
        // Handle dependencies with advice
        if (advice.isAnyRemove()) {
          rewriter.delete(context.start, context.stop)
          rewriter.deleteWhitespaceToLeft(context.start)
          rewriter.deleteNewlineToRight(context.stop)
        } else if (advice.isAnyChange()) {
          val originalText = tokens.getText(context.start, context.stop)
          val styleAwareReplacement = createStyleAwareReplacement(advice, originalText)
          rewriter.replace(context.start, context.stop, styleAwareReplacement.trim())
        }
      } else {
        // Handle dependencies without advice - restore original syntax if needed
        val currentText = tokens.getText(context.start, context.stop)
        if (needsSyntaxRestoration(currentText)) {
          val restoredText = restoreOriginalSyntax(currentText)
          rewriter.replace(context.start, context.stop, restoredText.trim())
        }
      }
    }
  }

  /**
   * Creates a style-aware replacement for the given advice, preserving the original syntax style.
   */
  private fun createStyleAwareReplacement(advice: Advice, originalText: String): String {
    // Detect if the original text used parentheses or not
    val useParentheses = detectParenthesesSyntax(originalText)
    
    // Create style-aware printer that preserves the detected style
    val styleAwarePrinter = printer.copy(useParenthesesSyntax = useParentheses)
    
    return styleAwarePrinter.toDeclaration(advice)
  }

  /**
   * Detects whether the original dependency declaration used parentheses syntax.
   * Returns true for "implementation(projects.myModule)", false for "implementation projects.myModule"
   */
  private fun detectParenthesesSyntax(originalText: String): Boolean {
    // Check if the current parsed text has parentheses
    val hasCurrentParentheses = originalText.contains("(") && originalText.contains(")")
    
    if (!hasCurrentParentheses) {
      // If current text has no parentheses, it's definitely non-parentheses style
      return false
    }
    
    // If current text has parentheses, check if it was originally parentheses or was preprocessed
    // Look for the same pattern in the original file content
    val cleanText = originalText.trim()
    val pattern = Regex.escape(cleanText).replace("\\(", "\\s*\\(")
    
    // If we find this exact pattern in original content, it was originally parentheses
    if (originalFileContent.contains(Regex(pattern))) {
      return true
    }
    
    // Check if we can find a non-parentheses version in the original content
    val nonParenthesesPattern = cleanText
      .replace(Regex("""\(\s*"""), " ")
      .replace(Regex("""\s*\)"""), "")
    
    return !originalFileContent.contains(nonParenthesesPattern)
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

      // Preprocess file to handle non-parentheses type-safe accessors
      val originalContent = file.toFile().readText()
      val preprocessedContent = normalizeTypeSafeAccessorSyntax(originalContent)
      val inputStream = preprocessedContent.byteInputStream()

      return Parser(
        file = inputStream,
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
            originalFileContent = originalContent
          )
        }
      ).listener()
    }

    /**
     * Normalizes type-safe accessor syntax to be compatible with the ANTLR parser.
     * Converts: "implementation projects.myModule" -> "implementation(projects.myModule)"
     */
    private fun normalizeTypeSafeAccessorSyntax(content: String): String {
      // Pattern to match: configurationName projects.accessor or libs.accessor
      val pattern = Regex(
        """\b(\w+)\s+((?:projects|libs)\.[\w.]+)(?!\s*\()""",
        RegexOption.MULTILINE
      )
      
      return pattern.replace(content) { matchResult ->
        val configuration = matchResult.groupValues[1]
        val accessor = matchResult.groupValues[2]
        "$configuration($accessor)"
      }
    }
  }
}
