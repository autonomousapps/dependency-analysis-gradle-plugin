// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model

import com.autonomousapps.internal.unsafeLazy
import com.autonomousapps.internal.utils.flatMapToOrderedSet
import com.autonomousapps.internal.utils.flatMapToSet
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.model.CodeSource.Kind
import com.autonomousapps.model.declaration.Variant
import com.squareup.moshi.JsonClass
import org.gradle.api.file.Directory

/** Represents a variant-specific view of the project under analysis. */
@Suppress("MemberVisibilityCanBePrivate") // deliberate API
@JsonClass(generateAdapter = false)
data class ProjectVariant(
  val coordinates: ProjectCoordinates,
  val buildType: String?,
  val flavor: String?,
  val variant: Variant,
  val sources: Set<Source>,
  val classpath: Set<Coordinates>,
  val annotationProcessors: Set<Coordinates>,
  val testInstrumentationRunner: String?
) {

  val usedNonAnnotationClassesBySrc: Set<String> by unsafeLazy {
    codeSource.flatMapToSet {
      it.usedNonAnnotationClasses
    }
  }

  val usedAnnotationClassesBySrc: Set<String> by unsafeLazy {
    codeSource.flatMapToSet {
      it.usedAnnotationClasses
    }
  }

  /**
   * For typealiases, we check for presence in the bytecode in any context, annotation or otherwise. We do not check
   * usages in Android res.
   */
  val usedClassesBySrc: Set<String> by unsafeLazy {
    usedNonAnnotationClassesBySrc + usedAnnotationClassesBySrc
  }

  val usedClassesByRes: Set<String> by unsafeLazy {
    androidResSource.flatMapToSet {
      it.usedClasses
    }
  }

  /** All class references from any context. */
  val usedClasses: Set<String> by unsafeLazy {
    codeSource.flatMapToSet {
      usedClassesBySrc + usedClassesByRes
    }
  }

  val usedNonAnnotationClasses: Set<String> by unsafeLazy {
    usedClassesByRes + usedNonAnnotationClassesBySrc
  }

  val exposedClasses: Set<String> by unsafeLazy {
    codeSource.flatMapToSet {
      it.exposedClasses
    }
  }

  val implementationClasses: Set<String> by unsafeLazy {
    usedNonAnnotationClasses - exposedClasses
  }

  val codeSource: List<CodeSource> by unsafeLazy {
    sources.filterIsInstance<CodeSource>()
  }

  val androidResSource: List<AndroidResSource> by unsafeLazy {
    sources.filterIsInstance<AndroidResSource>()
  }

  val androidAssetsSource: List<AndroidAssetSource> by unsafeLazy {
    sources.filterIsInstance<AndroidAssetSource>()
  }

  val imports: Set<String> by unsafeLazy {
    codeSource.flatMapToOrderedSet { it.imports }
  }

  val javaImports: Set<String> by unsafeLazy {
    codeSource.filter { it.kind == Kind.JAVA }
      .flatMapToOrderedSet { it.imports }
  }

  val kotlinImports: Set<String> by unsafeLazy {
    codeSource.filter { it.kind == Kind.KOTLIN }
      .flatMapToOrderedSet { it.imports }
  }

  val groovyImports: Set<String> by unsafeLazy {
    codeSource.filter { it.kind == Kind.GROOVY }
      .flatMapToOrderedSet { it.imports }
  }

  val scalaImports: Set<String> by unsafeLazy {
    codeSource.filter { it.kind == Kind.SCALA }
      .flatMapToOrderedSet { it.imports }
  }

  internal fun dependencies(dependenciesDir: Directory): Set<Dependency> {
    return classpath.asSequence()
      .plus(annotationProcessors)
      .map {
        val file = dependenciesDir.file(it.toFileName())
        if (file.asFile.exists()) {
          file.fromJson<Dependency>()
        } else {
          error("No file ${it.toFileName()}")
        }
      }
      .toSet()
  }
}
