// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal

import com.autonomousapps.internal.unsafeLazy
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.ProjectCoordinates
import com.autonomousapps.model.internal.CodeSource.Kind
import com.autonomousapps.model.source.SourceKind
import com.squareup.moshi.JsonClass
import org.gradle.api.file.Directory

/** Represents a variant-specific view of the project under analysis. */
@Suppress("MemberVisibilityCanBePrivate") // deliberate API
@JsonClass(generateAdapter = false)
internal data class ProjectVariant(
  val coordinates: ProjectCoordinates,
  val buildType: String?,
  val flavor: String?,
  val sourceKind: SourceKind,
  val sources: Set<Source>,
  val classpath: Set<Coordinates>,
  val annotationProcessors: Set<Coordinates>,
  val testInstrumentationRunner: String?
) {

  val codeSource: List<CodeSource> by unsafeLazy {
    sources.filterIsInstance<CodeSource>()
  }

  val classNames: Set<String> by unsafeLazy {
    codeSource.mapToOrderedSet { src -> src.className }
  }

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

  /** Invisible annotations are required at compile time but not at runtime. */
  val usedInvisibleAnnotationClassesBySrc: Set<String> by unsafeLazy {
    codeSource.flatMapToSet {
      it.usedInvisibleAnnotationClasses
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

  /**
   * The set of super classes and interfaces not available from [codeSource] (therefore "external" to "this" module).
   */
  val externalSupers: Set<String> by unsafeLazy {
    val supers = codeSource.mapNotNullToOrderedSet { src -> src.superClass }
    val interfaces = codeSource.flatMapToOrderedSet { src -> src.interfaces }
    // These super classes and interfaces are not available from "this" module, so must come from dependencies.
    val externalSupers = supers - classNames
    val externalInterfaces = interfaces - classNames
    val externals = externalSupers + externalInterfaces

    externals
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

  // /**
  //  * Every member access from this project to classes in another module. cf [usedClasses], which is a flat set of
  //  * referenced class names.
  //  */
  // val memberAccesses: Set<MemberAccess> by unsafeLazy {
  //   codeSource.flatMapToOrderedSet { src ->
  //     src.binaryClassAccesses.entries.flatMap { entry -> entry.value }
  //   }
  // }

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
