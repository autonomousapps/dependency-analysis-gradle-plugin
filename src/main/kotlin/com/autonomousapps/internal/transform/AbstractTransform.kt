// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.transform

import com.autonomousapps.graph.Graphs.children
import com.autonomousapps.graph.Graphs.root
import com.autonomousapps.internal.unsafeLazy
import com.autonomousapps.internal.utils.mapToSet
import com.autonomousapps.internal.utils.newSetMultimap
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.IncludedBuildCoordinates
import com.autonomousapps.model.internal.DependencyGraphView
import com.autonomousapps.model.internal.ProjectType
import com.autonomousapps.model.internal.declaration.Bucket
import com.autonomousapps.model.internal.declaration.Declaration
import com.autonomousapps.model.internal.intermediates.Usage
import com.autonomousapps.model.source.SourceKind
import com.google.common.collect.SetMultimap
import org.gradle.api.attributes.Category

@Suppress("UnstableApiUsage")
internal abstract class AbstractTransform(
  protected val buildPath: String,
  private val dependencyGraph: Map<String, DependencyGraphView>,
  private val isKaptApplied: Boolean,
) : Usage.Transform {

  protected val mapper = UsageToConfigurationMapper(
    isKaptApplied = isKaptApplied,
    projectType = ProjectType.ANDROID,
  )

  /**
   * Returns the set of direct (non-transitive) dependencies from [dependencyGraph], associated with the source sets
   * ([Variant.variant][SourceKind]) they're related to.
   *
   * These are _direct_ dependencies that are not _declared_ because they're coming from associated classpaths. For
   * example, the `test` source set extends from the `main` source set (and also the compile and runtime classpaths).
   */
  protected val directDependencies: SetMultimap<String, SourceKind> by unsafeLazy {
    newSetMultimap<String, SourceKind>().apply {
      dependencyGraph.values.map { graphView ->
        val root = graphView.graph.root()
        graphView.graph.children(root).forEach { directDependency ->
          val identifier = directDependency.normalizedIdentifier(buildPath)
          put(identifier, graphView.sourceKind)
        }
      }
    }
  }

  /**
   * This results in a map like:
   * * "group:name:1.0" -> (compileClasspath, runtimeClasspath)
   * * ":project" -> (compileClasspath)
   *
   * etc.
   */
  protected val dependenciesToClasspaths: SetMultimap<String, String> by unsafeLazy {
    newSetMultimap<String, String>().apply {
      dependencyGraph.values.map { graphView ->
        graphView.graph.nodes().forEach { node ->
          val identifier = node.normalizedIdentifier(buildPath)
          put(identifier, graphView.configurationName)
        }
      }
    }
  }

  protected fun Set<Declaration>.forCoordinates(coordinates: Coordinates): Set<Declaration> {
    return asSequence()
      .filter { declaration ->
        declaration.identifier == coordinates.identifier
          // In the special case of IncludedBuildCoordinates, the declaration might be a 'project(...)' dependency
          // if subprojects inside an included build depend on each other.
          || (coordinates is IncludedBuildCoordinates) && declaration.identifier == coordinates.resolvedProject.identifier
      }
      .filter { it.isJarDependency() && it.gradleVariantIdentification.variantMatches(coordinates) }
      .toSet()
  }

  protected fun isSingleBucketForSingleVariant(usages: Set<Usage>): Boolean {
    return if (usages.size == 1) {
      true
    } else {
      usages.mapToSet { it.bucket }.size == 1 && usages.mapToSet { it.sourceKind.base() }.size == 1
    }
  }

  protected fun Sequence<Usage>.filterUsed() = filterNot { it.bucket == Bucket.NONE }

  /**
   * Does the dependency point to one (or multiple) Jars, or is it just Metadata (i.e. a platform)
   * that we always want to keep?
   */
  protected fun Declaration.isJarDependency() =
    gradleVariantIdentification.attributes[Category.CATEGORY_ATTRIBUTE.name].let {
      it != Category.REGULAR_PLATFORM && it != Category.ENFORCED_PLATFORM
    }
}
