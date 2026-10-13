// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal

import com.autonomousapps.internal.unsafeLazy
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.ProjectCoordinates
import com.autonomousapps.model.internal.CodeSource.Kind
import com.autonomousapps.model.internal.intermediates.ClassReference
import com.autonomousapps.model.internal.intermediates.consumer.LdcConstant
import com.autonomousapps.model.internal.intermediates.consumer.MemberAccess
import com.autonomousapps.model.source.SourceKind
import com.squareup.moshi.JsonClass
import org.gradle.api.file.Directory

/**
 * Represents a source-set/variant/compilation-specific view of the project under analysis.
 * - For **Android** projects, an instance of this class is for a single "variant," such as `flavorDebug`, etc.
 * - For **JVM** projects, an instance of this class is for a single source set, such as main, test, functionalTest,
 *   etc.
 * - For **KMP** projects, an instance of this class is for a single `KotlinCompilation`. A Kotlin compilation will
 *   include sources from multiple source sets, such as `commonMain` and `jvmMain`, or `commonMain`, `appleMain`, and
 *   `<X>Arm64`.
 *
 * @see <a href="https://kotlinlang.org/docs/multiplatform/multiplatform-discover-project.html#intermediate-source-sets">KMP intermediate source sets</a>
 */
@Suppress("MemberVisibilityCanBePrivate") // deliberate API
@JsonClass(generateAdapter = false)
internal data class ProjectVariant(
  val coordinates: ProjectCoordinates,
  val buildType: String?,
  val flavor: String?,
  val sourceKind: SourceKind,
  val sources: Set<Source>,
  val runtimeSources: Set<Source>,
  val classpath: Set<Coordinates>,
  val annotationProcessors: Set<Coordinates>,
  val testInstrumentationRunner: String?,
  val excludedIdentifiers: Set<ExcludedIdentifier>,
) {

  val codeSource: List<CodeSource> by unsafeLazy {
    sources.filterIsInstance<CodeSource>()
  }

  val classNames: Set<ClassReference> by unsafeLazy {
    codeSource.mapToOrderedSet { source ->
      ClassReference(
        name = source.className,
        kmpSourceSetName = source.kmpSourceSetName,
      )
    }
  }

  val usedNonAnnotationClassesBySrc: Set<ClassReference> by unsafeLazy {
    codeSource.flatMapToOrderedSet { source ->
      source.usedNonAnnotationClasses.map { usedNonAnnotationClass ->
        ClassReference(
          name = usedNonAnnotationClass,
          kmpSourceSetName = source.kmpSourceSetName,
        )
      }
    }
  }

  // TODO(tsr): improve documentation. What are the keys? The values?
  val usedNonAnnotationClassesWithinVisibleAnnotationBySrc: Map<ClassReference, ClassReference> by unsafeLazy {
    codeSource
      .flatMapToSet { source ->
        source.usedNonAnnotationClassesWithinVisibleAnnotation.entries
          .map { (key, value) ->
            val k = ClassReference(name = key, kmpSourceSetName = source.kmpSourceSetName)
            val v = ClassReference(name = value, kmpSourceSetName = source.kmpSourceSetName)
            k to v
          }
      }
      .toMap()
  }

  val usedAnnotationClassesBySrc: Set<ClassReference> by unsafeLazy {
    codeSource.flatMapToOrderedSet { source ->
      source.usedAnnotationClasses.map { usedAnnotationClass ->
        ClassReference(
          name = usedAnnotationClass,
          kmpSourceSetName = source.kmpSourceSetName,
        )
      }
    }
  }

  val usedClassesByRes: Set<ClassReference> by unsafeLazy {
    androidResSource.flatMapToOrderedSet { source ->
      source.usedClasses.map { usedClass ->
        ClassReference(
          name = usedClass,
          kmpSourceSetName = null,
        )
      }
    }
  }

  /** All class references from any context. */
  val usedClasses: Set<ClassReference> by unsafeLazy {
    usedClassesByRes + usedNonAnnotationClassesBySrc + usedNonAnnotationClassesWithinVisibleAnnotationBySrc.map { it.key }
  }

  val usedNonAnnotationClasses: Set<ClassReference> by unsafeLazy {
    usedClassesByRes + usedNonAnnotationClassesBySrc
  }

  val exposedClasses: Set<ClassReference> by unsafeLazy {
    codeSource.flatMapToOrderedSet { source ->
      source.exposedClasses.map { exposedClass ->
        ClassReference(
          name = exposedClass,
          kmpSourceSetName = source.kmpSourceSetName,
        )
      }
    }
  }

  val inferredConstants: Set<LdcConstant> by unsafeLazy {
    codeSource.flatMapToOrderedSet {
      it.inferredConstants
    }
  }

  val implementationClasses: Set<ClassReference> by unsafeLazy {
    usedNonAnnotationClasses - exposedClasses
  }

  /**
   * The set of super classes and interfaces not available from [codeSource] (therefore "external" to "this" module).
   */
  val externalSupers: Set<ClassReference> by unsafeLazy {
    val supers = codeSource
      .mapNotNull { source ->
        val superClass = source.superClass
        if (superClass != null) {
          ClassReference(
            name = superClass,
            kmpSourceSetName = source.kmpSourceSetName,
          )
        } else {
          null
        }
      }
      .filterNotToOrderedSet { it.name == "java.lang.Object" }

    val interfaces = codeSource.flatMapToOrderedSet { source ->
      source.interfaces.map { i ->
        ClassReference(
          name = i,
          kmpSourceSetName = source.kmpSourceSetName,
        )
      }
    }
    // These super classes and interfaces are not available from "this" module, so must come from dependencies.
    val externalSupers = supers - classNames
    val externalInterfaces = interfaces - classNames
    val externals = externalSupers + externalInterfaces

    externals
  }

  val androidResSource: List<AndroidResSource> by unsafeLazy {
    sources.filterIsInstance<AndroidResSource>()
  }

  val androidResRuntimeSource: List<AndroidResSource> by unsafeLazy {
    runtimeSources.filterIsInstance<AndroidResSource>()
  }

  val androidAssetsSource: List<AndroidAssetSource> by unsafeLazy {
    sources.filterIsInstance<AndroidAssetSource>()
  }

  val imports: Set<ClassReference> by unsafeLazy {
    codeSource.flatMapToOrderedSet { source ->
      source.imports.map { imp ->
        ClassReference(
          name = imp,
          kmpSourceSetName = source.kmpSourceSetName,
        )
      }
    }
  }

  /**
   * Every member access from this project to classes in another module. cf [usedClasses], which is a flat set of
   * referenced class names.
   *
   * TODO(tsr): MemberAccess has no reference to kmpSourceSet. Do we care?
   */
  val memberAccesses: Set<MemberAccess> by unsafeLazy {
    codeSource.flatMapToOrderedSet { source ->
      source.binaryClassAccesses.entries.flatMap { entry -> entry.value }
    }
  }

  val javaImports: Set<ClassReference> by unsafeLazy {
    codeSource
      .filter { it.kind == Kind.JAVA }
      .flatMapToOrderedSet { source ->
        source.imports.map { imp ->
          ClassReference(
            name = imp,
            kmpSourceSetName = source.kmpSourceSetName,
          )
        }
      }
  }

  val kotlinImports: Set<ClassReference> by unsafeLazy {
    codeSource
      .filter { it.kind == Kind.KOTLIN }
      .flatMapToOrderedSet { source ->
        source.imports.map { imp ->
          ClassReference(
            name = imp,
            kmpSourceSetName = source.kmpSourceSetName,
          )
        }
      }
  }

  val groovyImports: Set<ClassReference> by unsafeLazy {
    codeSource
      .filter { it.kind == Kind.GROOVY }
      .flatMapToOrderedSet { source ->
        source.imports.map { imp ->
          ClassReference(
            name = imp,
            kmpSourceSetName = source.kmpSourceSetName,
          )
        }
      }
  }

  val scalaImports: Set<ClassReference> by unsafeLazy {
    codeSource
      .filter { it.kind == Kind.SCALA }
      .flatMapToOrderedSet { source ->
        source.imports.map { imp ->
          ClassReference(
            name = imp,
            kmpSourceSetName = source.kmpSourceSetName,
          )
        }
      }
  }

  /**
   * Uses a similar heuristic to
   * [InlineMember.candidateImports][com.autonomousapps.model.internal.InlineMemberCapability.InlineMember.candidateImports]
   */
  val candidateInlineImports: Set<String> by unsafeLazy {
    // e.g., `androidx.core.net.toUri` -> `androidx.core.net`
    kotlinImports
      .map { it.name.substringBeforeLast('.') }
      .filterToOrderedSet { it.isNotEmpty() }
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
