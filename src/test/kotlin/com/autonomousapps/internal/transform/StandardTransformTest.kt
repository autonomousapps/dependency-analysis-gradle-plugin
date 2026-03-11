// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.transform

import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.internal.DependencyGraphView
import com.autonomousapps.model.internal.ProjectType
import com.autonomousapps.model.internal.declaration.Bucket
import com.autonomousapps.model.internal.declaration.ConfigurationNames
import com.autonomousapps.model.internal.declaration.Declaration
import com.autonomousapps.model.internal.intermediates.Usage
import com.autonomousapps.model.source.KmpSourceKind
import com.autonomousapps.model.source.SourceKind
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * Regression tests for StandardTransform.
 *
 * @see <a href="https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1649">Issue #1649</a>
 */
internal class StandardTransformTest {

  // The resolved coordinates include the default capability (the identifier itself).
  private val caffeine = ModuleCoordinates(
    "com.github.ben-manes.caffeine:caffeine",
    "3.2.3",
    GradleVariantIdentification(setOf("com.github.ben-manes.caffeine:caffeine"), emptyMap()),
  )

  /**
   * In a KMP project with both Android and JVM targets, a dep declared on `androidMainImplementation`
   * and used in `androidMain` source should produce no advice (it's already correctly placed).
   *
   * Before the fix, this crashed with:
   *   IllegalArgumentException: Change advice cannot be from and to the same configuration (androidMainImplementation)
   *
   * The crash happened because `KmpSourceKind.of(compilation)` uses the actual compilation classpath names
   * (e.g. "debugCompileClasspath") while `ConfigurationNames.sourceKindFrom` infers "androidMainCompileClasspath".
   * Since `KmpSourceKind` was a data class comparing all fields, the usage and declaration sourceKinds were
   * not equal, causing both an ofAdd and ofRemove for the same configuration.
   */
  @Test
  fun `KMP - used dep on androidMainImplementation does not crash or generate advice`() {
    // Given
    val declaration = Declaration(
      identifier = "com.github.ben-manes.caffeine:caffeine",
      version = "3.2.3",
      configurationName = "androidMainImplementation",
      gradleVariantIdentification = GradleVariantIdentification.EMPTY,
    )

    // The sourceKind as created from the actual compilation: classpath names come from the compilation,
    // not inferred from the source set name. For Android KMP compilations, the compilation classpath
    // name (e.g. "debugCompileClasspath") may differ from the inferred name ("androidMainCompileClasspath").
    val compilationSourceKind = KmpSourceKind(
      name = "androidMain",
      kind = SourceKind.CUSTOM_JVM_KIND,
      compileClasspathName = "debugCompileClasspath",
      runtimeClasspathName = "debugRuntimeClasspath",
    )

    // The dep is on the debug runtime classpath (it's declared, so it would be resolved).
    val root = caffeine // use dep as root to keep graph simple
    val depGraph = DependencyGraphView.newGraphBuilder().apply {
      addNode(root)
    }.build()
    val graphView = DependencyGraphView(compilationSourceKind, "debugRuntimeClasspath", depGraph)

    val dependencyGraph = mapOf("androidMain,CUSTOM_JVM,debugRuntimeClasspath" to graphView)

    val supportedSourceSetNames = setOf("commonMain", "commonTest", "jvmMain", "jvmTest", "androidMain")
    val configurationNames = ConfigurationNames(ProjectType.KMP, supportedSourceSetNames)

    val transform = StandardTransform(
      coordinates = caffeine,
      declarations = setOf(declaration),
      dependencyGraph = dependencyGraph,
      buildPath = ":",
      projectType = ProjectType.KMP,
      configurationNames = configurationNames,
    )

    // CAFFEINE is used in androidMain (bucket=IMPL), sourceKind has compilation classpath names
    val usage = Usage(
      buildType = null,
      flavor = null,
      sourceKind = compilationSourceKind,
      bucket = Bucket.IMPL,
      reasons = emptySet(),
    )

    // When - before the fix, this crashed with:
    //   IllegalArgumentException: Change advice cannot be from and to the same configuration (androidMainImplementation)
    val advice = transform.reduce(setOf(usage))

    // Then - dep is correctly placed, no advice needed
    assertThat(advice).isEmpty()
  }

  /**
   * Same scenario with multiple Android variant compilations (debug + release), each producing a usage
   * with different compilation classpath names but the same source set name "androidMain".
   */
  @Test
  fun `KMP - used dep on androidMainImplementation with multiple Android variants does not crash or generate advice`() {
    // Given
    val declaration = Declaration(
      identifier = "com.github.ben-manes.caffeine:caffeine",
      version = "3.2.3",
      configurationName = "androidMainImplementation",
      gradleVariantIdentification = GradleVariantIdentification.EMPTY,
    )

    val debugSourceKind = KmpSourceKind(
      name = "androidMain",
      kind = SourceKind.CUSTOM_JVM_KIND,
      compileClasspathName = "debugCompileClasspath",
      runtimeClasspathName = "debugRuntimeClasspath",
    )
    val releaseSourceKind = KmpSourceKind(
      name = "androidMain",
      kind = SourceKind.CUSTOM_JVM_KIND,
      compileClasspathName = "releaseCompileClasspath",
      runtimeClasspathName = "releaseRuntimeClasspath",
    )

    val depGraph = DependencyGraphView.newGraphBuilder().apply {
      addNode(caffeine)
    }.build()
    val debugGraphView = DependencyGraphView(debugSourceKind, "debugRuntimeClasspath", depGraph)
    val releaseGraphView = DependencyGraphView(releaseSourceKind, "releaseRuntimeClasspath", depGraph)

    val dependencyGraph = mapOf(
      "androidMain,CUSTOM_JVM,debugRuntimeClasspath" to debugGraphView,
      "androidMain,CUSTOM_JVM,releaseRuntimeClasspath" to releaseGraphView,
    )

    val supportedSourceSetNames = setOf("commonMain", "commonTest", "jvmMain", "jvmTest", "androidMain")
    val configurationNames = ConfigurationNames(ProjectType.KMP, supportedSourceSetNames)

    val transform = StandardTransform(
      coordinates = caffeine,
      declarations = setOf(declaration),
      dependencyGraph = dependencyGraph,
      buildPath = ":",
      projectType = ProjectType.KMP,
      configurationNames = configurationNames,
    )

    val debugUsage = Usage(
      buildType = null,
      flavor = null,
      sourceKind = debugSourceKind,
      bucket = Bucket.IMPL,
      reasons = emptySet(),
    )
    val releaseUsage = Usage(
      buildType = null,
      flavor = null,
      sourceKind = releaseSourceKind,
      bucket = Bucket.IMPL,
      reasons = emptySet(),
    )

    // When - before the fix, this crashed with IllegalArgumentException
    val advice = transform.reduce(setOf(debugUsage, releaseUsage))

    // Then
    assertThat(advice).isEmpty()
  }
}
