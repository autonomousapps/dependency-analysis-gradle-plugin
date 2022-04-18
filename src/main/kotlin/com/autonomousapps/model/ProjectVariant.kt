package com.autonomousapps.model

import com.autonomousapps.internal.unsafeLazy
import com.autonomousapps.internal.utils.flatMapToOrderedSet
import com.autonomousapps.internal.utils.flatMapToSet
import com.autonomousapps.model.CodeSource.Kind
import com.autonomousapps.model.declaration.Variant

/**
 * Represents a variant-specific view of the project under analysis.
 */
data class ProjectVariant(
  val coordinates: ProjectCoordinates,
  val buildType: String?,
  val flavor: String?,
  val variant: Variant,
  val sources: Set<Source>,
  val classpath: Set<Coordinates>,
  val annotationProcessors: Set<Coordinates>
) {

  val usedClasses: Set<String> by unsafeLazy {
    usedClassesByRes + usedClassesBySrc
  }

  val usedClassesBySrc: Set<String> by unsafeLazy {
    codeSource.flatMapToSet {
      it.usedClasses
    }
  }

  val usedClassesByRes: Set<String> by unsafeLazy {
    androidResSource.flatMapToSet {
      it.usedClasses
    }
  }

  val exposedClasses: Set<String> by unsafeLazy {
    codeSource.flatMapToSet {
      it.exposedClasses
    }
  }

  val implementationClasses: Set<String> by unsafeLazy {
    usedClasses - exposedClasses
  }

  val codeSource: List<CodeSource> by unsafeLazy {
    sources.filterIsInstance<CodeSource>()
  }

  val androidResSource: List<AndroidResSource> by unsafeLazy {
    sources.filterIsInstance<AndroidResSource>()
  }

  val javaImports: Set<String> by unsafeLazy {
    codeSource.filter { it.kind == Kind.JAVA }
      .flatMapToOrderedSet { it.imports }
  }

  val kotlinImports: Set<String> by unsafeLazy {
    codeSource.filter { it.kind == Kind.KOTLIN }
      .flatMapToOrderedSet { it.imports }
  }

  val imports: Set<String> by unsafeLazy {
    sortedSetOf<String>().apply {
      addAll(javaImports)
      addAll(kotlinImports)
    }
  }
}
