package com.autonomousapps.internal.parse

import com.autonomousapps.exception.BuildScriptParseException
import com.autonomousapps.internal.advice.AdvicePrinter
import com.autonomousapps.internal.antlr.v4.runtime.CharStreams
import com.autonomousapps.internal.antlr.v4.runtime.CommonTokenStream
import com.autonomousapps.internal.antlr.v4.runtime.ParserRuleContext
import com.autonomousapps.internal.antlr.v4.runtime.RecognitionException
import com.autonomousapps.internal.antlr.v4.runtime.Recognizer
import com.autonomousapps.internal.antlr.v4.runtime.TokenStreamRewriter
import com.autonomousapps.internal.antlr.v4.runtime.tree.ParseTreeWalker
import com.autonomousapps.internal.grammar.GradleGroovyScriptBaseListener
import com.autonomousapps.internal.grammar.GradleGroovyScriptLexer
import com.autonomousapps.internal.grammar.GradleGroovyScriptParser
import com.autonomousapps.internal.grammar.GradleGroovyScriptParser.BuildscriptContext
import com.autonomousapps.internal.grammar.GradleGroovyScriptParser.ConfigurationContext
import com.autonomousapps.internal.grammar.GradleGroovyScriptParser.DependenciesContext
import com.autonomousapps.internal.grammar.GradleGroovyScriptParser.DependencyContext
import com.autonomousapps.internal.grammar.GradleGroovyScriptParser.ExternalDeclarationContext
import com.autonomousapps.internal.grammar.GradleGroovyScriptParser.FileDeclarationContext
import com.autonomousapps.internal.grammar.GradleGroovyScriptParser.LocalDeclarationContext
import com.autonomousapps.internal.grammar.GradleGroovyScriptParser.ScriptContext
import com.autonomousapps.internal.utils.filterToOrderedSet
import com.autonomousapps.internal.utils.filterToSet
import com.autonomousapps.internal.utils.ifNotEmpty
import com.autonomousapps.model.Advice
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

@Suppress("DuplicatedCode")
internal class GradleBuildScriptDependenciesRewriter private constructor(
  private val tokens: CommonTokenStream,
  private val rewriter: TokenStreamRewriter,
  private val errorListener: RewriterErrorListener,
  private val printer: AdvicePrinter,
  private val advice: Set<Advice>,
  /** Reverse map from custom representation to standard. */
  private val reversedDependencyMap: (String) -> String,
) : GradleGroovyScriptBaseListener() {

  private class RewriterErrorListener : AbstractErrorListener() {
    val errorMessages = mutableListOf<String>()

    override fun syntaxError(
      recognizer: Recognizer<*, *>,
      offendingSymbol: Any,
      line: Int,
      charPositionInLine: Int,
      msg: String,
      e: RecognitionException?
    ) {
      errorMessages.add(msg)
    }
  }

  private class CtxDependency(
    val dependency: DependencyContext,
    val configuration: ConfigurationContext
  )

  val originalDependencies = mutableListOf<String>()

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

  override fun enterExternalDeclaration(ctx: ExternalDeclarationContext) {
    handleDeclaration(ctx, CtxDependency(ctx.dependency(), ctx.configuration()))
  }

  override fun enterLocalDeclaration(ctx: LocalDeclarationContext) {
    handleDeclaration(ctx, CtxDependency(ctx.dependency(), ctx.configuration()))
  }

  override fun enterFileDeclaration(ctx: FileDeclarationContext) {
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

    val dependency = reversedDependencyMap(tokens.getText(ctxDependency.dependency))
    originalDependencies += dependency

    advice.find { it.coordinates.gav() == dependency }?.let { a ->
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

  internal companion object {
    fun newRewriter(
      file: Path,
      advice: Set<Advice>,
      advicePrinter: AdvicePrinter,
      reversedDependencyMap: (String) -> String = { it },
    ): GradleBuildScriptDependenciesRewriter {
      val input = Files.newInputStream(file, StandardOpenOption.READ).use { CharStreams.fromStream(it) }
      val lexer = GradleGroovyScriptLexer(input)
      val tokens = CommonTokenStream(lexer)
      val parser = GradleGroovyScriptParser(tokens)

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
