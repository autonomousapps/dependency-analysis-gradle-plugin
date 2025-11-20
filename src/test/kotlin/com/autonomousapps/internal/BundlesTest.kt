// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import com.autonomousapps.ProjectType
import com.autonomousapps.extension.DependenciesHandler
import com.autonomousapps.internal.utils.intoSet
import com.autonomousapps.model.*
import com.autonomousapps.model.internal.DependencyGraphView
import com.autonomousapps.model.internal.declaration.Bucket
import com.autonomousapps.model.internal.declaration.ConfigurationNames
import com.autonomousapps.model.internal.declaration.Declaration
import com.autonomousapps.model.internal.intermediates.Reason
import com.autonomousapps.model.internal.intermediates.Usage
import com.autonomousapps.model.source.JvmSourceKind
import com.autonomousapps.model.source.KmpSourceKind
import com.autonomousapps.model.source.SourceKind
import com.autonomousapps.test.usage
import com.google.common.truth.Truth.assertThat
import org.gradle.api.model.ObjectFactory
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Suppress("UnstableApiUsage")
class BundlesTest {

  private class RealDependenciesHandler(objects: ObjectFactory) : DependenciesHandler(objects)

  private val project = ProjectBuilder.builder().build()
  private val objects = project.objects
  private val dependenciesHandler = RealDependenciesHandler(objects)
  private val gvi = GradleVariantIdentification.EMPTY

  private val jvmConfigurationNames = ConfigurationNames(ProjectType.JVM, setOf("main", "test"))
  private val kmpConfigurationNames = ConfigurationNames(
    projectType = ProjectType.KMP,
    supportedSourceSetNames = setOf(
      "commonMain",
      "commonTest",
      "jvmMain",
      "jvmTest",
    ),
  )

  @Nested inner class DefaultBundles {
    @Test fun `kotlin stdlib is a default bundle`() {
      val consumer = ProjectCoordinates(":consumer", gvi)
      val stdlibJdk8 = ModuleCoordinates("org.jetbrains.kotlin:kotlin-stdlib-jdk8", "1", gvi)
      val stdlib = ModuleCoordinates("org.jetbrains.kotlin:kotlin-stdlib", "1", gvi)

      // Usages of project :consumer
      val stdlibJdk8Usages = stdlibJdk8 to usage(Bucket.NONE, JvmSourceKind.MAIN, "main").intoSet()
      val stdlibUsages = stdlib to usage(Bucket.API, JvmSourceKind.MAIN, "main").intoSet()
      val dependencyUsages: Map<Coordinates, Set<Usage>> = listOf(stdlibJdk8Usages, stdlibUsages).toMap()

      // Dependency graph rooted on :consumer
      val graph = newGraphFrom(
        // :consumer -> stdlib-jdk8 -> stdlib
        listOf(consumer to stdlibJdk8, stdlibJdk8 to stdlib)
      )

      // the thing under test
      val bundles = Bundles.of(
        projectPath = ":consumer",
        dependencyGraph = graph,
        bundleRules = dependenciesHandler.serializableBundles(),
        dependencyUsages = dependencyUsages,
        declarations = emptySet(), // TODO(tsr)
        configurationNames = jvmConfigurationNames,
        ignoreKtx = false
      )

      assertThat(bundles.hasParentInBundle(stdlib)).isTrue()
      assertThat(bundles.hasUsedChild(stdlibJdk8)).isTrue()
    }

    /**
     * Because of implicit KMP bundles, will transform:
     * ```
     * add okio-jvm to jvmMainApi
     * ```
     * to
     * ```
     * change okio from commonMainImplementation to commonMainApi
     * ```
     * because okio is the implicit parent of okio-jvm, and in this case okio is declared on commonMainImplementation.
     */
    @Test fun `transforms add (jvmMainApi) to change (commonMainImplementation to commonMainApi) for kmp (implicit bundles)`() {
      // Given a project that has a dependency graph with three nodes (itself, okio, and okio-jvm)
      val consumer = ProjectCoordinates(":consumer", gvi)
      val okio = ModuleCoordinates("com.squareup.okio:okio", "3.16.4", gvi)
      val okioJvm = ModuleCoordinates("com.squareup.okio:okio-jvm", "3.16.4", gvi)
      val graph = newGraphFrom(
        // :consumer -> okio -> okioJvm
        listOf(consumer to okio, okio to okioJvm),
        sourceKind = KmpSourceKind.JVM_MAIN,
        configurationName = "jvmCompileClasspath",
        graphKey = "jvmMain,CUSTOM_JVM,jvmCompileClasspath",
      )

      // ...a single declaration: commonMainImplementation(libs.okio)
      val declarations = Declaration(
        identifier = "com.squareup.okio:okio",
        version = "3.16.4",
        configurationName = "commonMainImplementation",
        gradleVariantIdentification = gvi,
      ).intoSet()

      // ...usages of project :consumer
      val unused = Reason.Unused.intoSet()
      val abi = Reason.Abi("Uses 1 class: okio.Buffer").intoSet()
      val okioUsages = okio to usage(Bucket.NONE, KmpSourceKind.JVM_MAIN, reasons = unused).intoSet()
      val okioJvmUsages = okioJvm to usage(Bucket.API, KmpSourceKind.JVM_MAIN, reasons = abi).intoSet()
      val dependencyUsages = listOf(okioUsages, okioJvmUsages).toMap<Coordinates, Set<Usage>>()

      // When we build the thing under test
      val bundles = Bundles.of(
        projectPath = ":consumer",
        dependencyGraph = graph,
        bundleRules = dependenciesHandler.serializableBundles(),
        dependencyUsages = dependencyUsages,
        declarations = declarations,
        configurationNames = kmpConfigurationNames,
        ignoreKtx = false
      )

      // TODO also test changeAdvice and removeAdvice?
      // Then it mutates the advice as expected
      val addAdvice = Advice.ofAdd(okioJvm, "jvmMainApi")
      val expectedAdvice = Advice.ofChange(okio, "commonMainImplementation", "commonMainApi")
      assertThat(bundles.maybeParent(addAdvice, okioJvm)).isEqualTo(expectedAdvice)
    }

    /**
     * Because of implicit KMP bundles, will transform:
     * ```
     * add okio-jvm to jvmMainApi
     * ```
     * to
     * ```
     * change okio from jvmMainImplementation to jvmMainApi
     * ```
     * because okio is the implicit parent of okio-jvm, and in this case okio is declared on jvmMainImplementation.
     */
    @Test fun `transforms change (jvmMainImplementation-jvmMainApi) for okio-jvm to okio for kmp (implicit bundles)`() {
      // Given a project that has a dependency graph with three nodes (itself, okio, and okio-jvm)
      val consumer = ProjectCoordinates(":consumer", gvi)
      val okio = ModuleCoordinates("com.squareup.okio:okio", "3.16.4", gvi)
      val okioJvm = ModuleCoordinates("com.squareup.okio:okio-jvm", "3.16.4", gvi)
      val graph = newGraphFrom(
        // :consumer -> okio -> okioJvm
        listOf(consumer to okio, okio to okioJvm),
        sourceKind = KmpSourceKind.JVM_MAIN,
        configurationName = "jvmCompileClasspath",
        graphKey = "jvmMain,CUSTOM_JVM,jvmCompileClasspath",
      )

      // ...a single declaration: jvmMainImplementation(libs.okio)
      val declarations = Declaration(
        identifier = "com.squareup.okio:okio",
        version = "3.16.4",
        configurationName = "jvmMainImplementation",
        gradleVariantIdentification = gvi,
      ).intoSet()

      // ...usages of project :consumer
      val unused = Reason.Unused.intoSet()
      val abi = Reason.Abi("Uses 1 class: okio.Buffer").intoSet()
      val okioUsages = okio to usage(Bucket.NONE, KmpSourceKind.JVM_MAIN, reasons = unused).intoSet()
      val okioJvmUsages = okioJvm to usage(Bucket.API, KmpSourceKind.JVM_MAIN, reasons = abi).intoSet()
      val dependencyUsages = listOf(okioUsages, okioJvmUsages).toMap<Coordinates, Set<Usage>>()

      // When we build the thing under test
      val bundles = Bundles.of(
        projectPath = ":consumer",
        dependencyGraph = graph,
        bundleRules = dependenciesHandler.serializableBundles(),
        dependencyUsages = dependencyUsages,
        declarations = declarations,
        configurationNames = kmpConfigurationNames,
        ignoreKtx = false
      )

      // TODO also test changeAdvice and removeAdvice?
      // Then it mutates the advice as expected
      val addAdvice = Advice.ofAdd(okioJvm, "jvmMainApi")
      val expectedAdvice = Advice.ofChange(okio, "jvmMainImplementation", "jvmMainApi")
      assertThat(bundles.maybeParent(addAdvice, okioJvm)).isEqualTo(expectedAdvice)
    }
  }

  @Nested inner class Primary {
    @Test fun `supports facades with a primary entry point`() {
      // Define a bundle with a primary identifier
      dependenciesHandler.bundles.create("facade").apply {
        primary(":entry-point")
        includeDependency(":entry-point")
        includeDependency(":used")
      }
      val bundles = buildBundles()

      val badAdvice = Advice.ofAdd(ProjectCoordinates(":used", gvi), "implementation")
      val expectedAdvice = Advice.ofAdd(ProjectCoordinates(":entry-point", gvi), "implementation")
      assertThat(bundles.maybePrimary(badAdvice, badAdvice.coordinates)).isEqualTo(expectedAdvice)
    }

    @Test fun `without a primary, bundle ignored`() {
      // Define a bundle without a primary identifier
      dependenciesHandler.bundles.create("facade").apply {
        includeDependency(":entry-point")
        includeDependency(":used")
      }
      val bundles = buildBundles()

      // Advice is unchanged
      val advice = Advice.ofAdd(ProjectCoordinates(":used", gvi), "implementation")
      assertThat(bundles.maybePrimary(advice, advice.coordinates)).isEqualTo(advice)
    }

    private fun buildBundles(): Bundles {
      // Coordinates
      val consumer = ProjectCoordinates(":consumer", gvi)
      val unused = ProjectCoordinates(":unused", gvi)
      val entryPoint = ProjectCoordinates(":entry-point", gvi)
      val used = ProjectCoordinates(":used", gvi)

      // Usages of project :consumer
      val unusedUsages = unused to usage(Bucket.NONE, JvmSourceKind.MAIN, "main").intoSet()
      val entryPointUsages = entryPoint to usage(Bucket.NONE, JvmSourceKind.MAIN, "main").intoSet()
      val usedUsages = used to usage(Bucket.IMPL, JvmSourceKind.MAIN, "main").intoSet()
      val dependencyUsages: Map<Coordinates, Set<Usage>> = listOf(unusedUsages, entryPointUsages, usedUsages).toMap()

      // Dependency graph rooted on :consumer
      val graph = newGraphFrom(
        // :consumer -> :unused -> :entry-point -> :used
        listOf(consumer to unused, unused to entryPoint, entryPoint to used)
      )

      return Bundles.of(
        projectPath = ":consumer",
        dependencyGraph = graph,
        bundleRules = dependenciesHandler.serializableBundles(),
        dependencyUsages = dependencyUsages,
        declarations = emptySet(), // TODO(tsr)
        configurationNames = jvmConfigurationNames,
        ignoreKtx = false
      )
    }
  }

  private fun newGraphFrom(
    edges: List<Pair<Coordinates, Coordinates>>,
    sourceKind: SourceKind = JvmSourceKind.MAIN,
    configurationName: String = "compileClasspath",
    graphKey: String = "main,Main",
  ): Map<String, DependencyGraphView> {
    val graph = DependencyGraphView.newGraphBuilder().apply {
      edges.forEach { putEdge(it.first, it.second) }
    }.build()

    return mapOf(
      graphKey to DependencyGraphView(
        sourceKind = sourceKind,
        configurationName = configurationName,
        graph = graph
      )
    )
  }
}
