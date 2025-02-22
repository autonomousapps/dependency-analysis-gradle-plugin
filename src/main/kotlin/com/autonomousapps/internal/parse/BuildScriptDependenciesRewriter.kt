package com.autonomousapps.internal.parse

import com.autonomousapps.internal.advice.AdvicePrinter
import com.autonomousapps.internal.advice.DslKind
import com.autonomousapps.model.Advice
import java.io.File

internal interface BuildScriptDependenciesRewriter {
  fun rewritten(): String

  companion object {
    fun of(
      file: File,
      advice: Set<Advice>,
      advicePrinter: AdvicePrinter,
      reversedDependencyMap: (String) -> String = { it },
    ): BuildScriptDependenciesRewriter {
      val dslKind = DslKind.from(file)
      val filePath = file.toPath()

      return when (dslKind) {
        DslKind.KOTLIN -> KotlinBuildScriptDependenciesRewriter.of(filePath, advice, advicePrinter, reversedDependencyMap)
        DslKind.GROOVY -> GroovyBuildScriptDependenciesRewriter.of(filePath, advice, advicePrinter, reversedDependencyMap)
      }
    }
  }
}

