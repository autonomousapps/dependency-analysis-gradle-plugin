// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.parse

import com.autonomousapps.internal.antlr.v4.runtime.CharStreams
import com.autonomousapps.internal.antlr.v4.runtime.CommonTokenStream
import com.autonomousapps.internal.antlr.v4.runtime.tree.ParseTreeWalker
import com.autonomousapps.internal.grammar.SimpleBaseListener
import com.autonomousapps.internal.grammar.SimpleLexer
import com.autonomousapps.internal.grammar.SimpleParser
import java.io.File
import java.io.FileInputStream

internal class SourceListener : SimpleBaseListener() {

  private val imports = mutableSetOf<String>()

  fun imports(): Set<String> = imports

  override fun enterImportDeclaration(ctx: SimpleParser.ImportDeclarationContext) {
    val qualifiedName = ctx.qualifiedName().text
    val import = if (ctx.children.any { it.text == "*" }) {
      "$qualifiedName.*"
    } else {
      qualifiedName
    }

    imports.add(import)
  }

  internal companion object {
    fun parseSourceFileForImports(file: File): Set<String> {
      val parser = newSimpleParser(file)
      val importListener = walkTree(parser)
      return importListener.imports()
    }

    private fun newSimpleParser(file: File): SimpleParser {
      val input = FileInputStream(file).use { fis -> CharStreams.fromStream(fis) }
      val lexer = SimpleLexer(input)
      val tokens = CommonTokenStream(lexer)
      return SimpleParser(tokens)
    }

    private fun walkTree(parser: SimpleParser): SourceListener {
      val tree = parser.file()
      val walker = ParseTreeWalker()
      val importListener = SourceListener()
      walker.walk(importListener, tree)
      return importListener
    }
  }
}
