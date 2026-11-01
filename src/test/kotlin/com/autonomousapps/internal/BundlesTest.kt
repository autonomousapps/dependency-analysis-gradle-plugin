// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import com.autonomousapps.extension.DependenciesHandler
import com.autonomousapps.internal.utils.intoSet
import com.autonomousapps.model.*
import com.autonomousapps.model.internal.DependencyGraphView
import com.autonomousapps.model.internal.ProjectType
import com.autonomousapps.model.internal.declaration.Bucket
import com.autonomousapps.model.internal.declaration.ConfigurationNames
import com.autonomousapps.model.internal.declaration.Declaration
import com.autonomousapps.model.internal.intermediates.Reason
import com.autonomousapps.model.internal.intermediates.Usage
import com.autonomousapps.model.source.AndroidSourceKind
import com.autonomousapps.model.source.JvmSourceKind
import com.autonomousapps.model.source.KmpSourceKind
import com.autonomousapps.model.source.SourceKind
import com.autonomousapps.test.usage
import com.google.common.truth.Truth.assertThat
import org.gradle.api.model.ObjectFactory
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BundlesTest {

  private class RealDependenciesHandler(objects: ObjectFactory) : DependenciesHandler(objects)

  private val project = ProjectBuilder.builder().build()
  private val objects = project.objects
  private val dependenciesHandler = RealDependenciesHandler(objects)
  private val gvi = GradleVariantIdentification.EMPTY

  private val androidConfigurationNames = ConfigurationNames(
    projectType = ProjectType.ANDROID,
    supportedSourceSetNames = setOf(
      "main",
      "debug",
      "test",
    ),
    buildTypes = emptySet(),
    productFlavors = emptySet(),
  )
  private val jvmConfigurationNames = ConfigurationNames(
    projectType = ProjectType.JVM,
    supportedSourceSetNames = setOf("main", "test"),
    buildTypes = emptySet(),
    productFlavors = emptySet(),
  )
  private val kmpConfigurationNames = ConfigurationNames(
    projectType = ProjectType.KMP,
    supportedSourceSetNames = setOf(
      "commonMain",
      "commonTest",
      "jvmMain",
      "jvmTest",
    ),
    buildTypes = emptySet(),
    productFlavors = emptySet(),
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
        declarations = emptySet(),
        configurationNames = jvmConfigurationNames,
        ignoreKtx = false
      )

      assertThat(bundles.hasParent(stdlib)).isTrue()
      assertThat(bundles.hasUsedChild(stdlibJdk8)).isTrue()
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
    @Test fun `transforms change (jvmMainImplementation-jvmMainApi) for okio-jvm to okio for kmp`() {
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

      // Then it mutates the advice as expected
      val addAdvice = Advice.ofAdd(okioJvm, "jvmMainApi")
      val expectedAdvice = Advice.ofChange(okio, "jvmMainImplementation", "jvmMainApi")
      assertThat(bundles.maybeParent(addAdvice, okioJvm)).isEqualTo(expectedAdvice)
    }

    /**
     * Metro (and other plugins) can add dependencies, meaning those dependencies are undeclared. When this happens, we
     * can't recommend upgrading the _parent_ of a -jvm (etc.) dependency because the user has no control over that.
     * This test validates that `bundles.maybeParent(addAdvice)` returns that same `addAdvice` in this scenario.
     *
     * @see <a href="https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1653">Issue 1653</a>.
     */
    @Test fun `does not transform change (jvmMainImplementation-jvmMainApi) for metro-runtime for kmp`() {
      // Given a project that has a dependency graph with three nodes (itself, okio, and okio-jvm)
      val consumer = ProjectCoordinates(":consumer", gvi)
      val metroRuntime = ModuleCoordinates("dev.zacsweers.metro:runtime", "0.10.2", gvi)
      val metroRuntimeJvm = ModuleCoordinates("dev.zacsweers.metro:runtime-jvm", "0.10.2", gvi)
      val graph = newGraphFrom(
        // :consumer -> metroRuntime -> metroRuntimeJvm
        listOf(consumer to metroRuntime, metroRuntime to metroRuntimeJvm),
        sourceKind = KmpSourceKind.JVM_MAIN,
        configurationName = "jvmCompileClasspath",
        graphKey = "jvmMain,CUSTOM_JVM,jvmCompileClasspath",
      )

      // no declarations (the dep is provided by a plugin!)
      val declarations = emptySet<Declaration>()

      // ...usages of project :consumer
      val unused = Reason.Unused.intoSet()
      val abi = Reason.Abi("Imports 1 class: dev.zacsweers.metro.ContributesTo").intoSet()
      val metroRuntimeUsages = metroRuntime to usage(Bucket.NONE, KmpSourceKind.JVM_MAIN, reasons = unused).intoSet()
      val metroRuntimeJvmUsages = metroRuntimeJvm to usage(Bucket.API, KmpSourceKind.JVM_MAIN, reasons = abi).intoSet()
      val dependencyUsages = listOf(metroRuntimeUsages, metroRuntimeJvmUsages).toMap<Coordinates, Set<Usage>>()

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

      // Then the advice remains unchanged
      val addAdvice = Advice.ofAdd(metroRuntimeJvm, "jvmMainApi")
      assertThat(bundles.maybeParent(addAdvice, metroRuntimeJvm)).isEqualTo(addAdvice)
    }
  }

  @Nested inner class ExplicitBundles {
    /**
     * Because of user-supplied bundle, will transform:
     * ```
     * add foo-bar to debugApi
     * ```
     * to
     * ```
     * change foo from implementation to debugApi
     * ```
     * because foo-bar is explicitly declared the primary of foo-bar, and in this case foo is declared on
     * implementation.
     */
    @Test fun `transforms change (debugApi-api) for foo-bar to foo for non-KMP user-supplied bundle`() {
      // Given a project that has a dependency graph with three nodes (itself, okio, and okio-jvm)
      val consumer = ProjectCoordinates(":consumer", gvi)
      val foo = ModuleCoordinates("com.foo:foo", "1", gvi)
      val fooBar = ModuleCoordinates("com.foo:foo-bar", "1", gvi)
      val graph = newGraphFrom(
        // :consumer -> foo -> foo-bar
        listOf(consumer to foo, foo to fooBar),
        sourceKind = AndroidSourceKind.MAIN,
      )

      // ...a single declaration: implementation(libs.foo)
      val declarations = Declaration(
        identifier = "com.foo:foo",
        version = "1",
        configurationName = "implementation",
        gradleVariantIdentification = gvi,
      ).intoSet()

      // ...usages of project :consumer
      val unused = Reason.Unused.intoSet()
      val abi = Reason.Abi("Uses 1 class: foo.FooBar").intoSet()
      val fooUsages = foo to usage(Bucket.NONE, AndroidSourceKind.MAIN, reasons = unused).intoSet()
      val fooBarUsages = fooBar to usage(Bucket.API, AndroidSourceKind.main("debug"), reasons = abi).intoSet()
      val dependencyUsages = listOf(fooUsages, fooBarUsages).toMap<Coordinates, Set<Usage>>()

      // ...and the explicit bundle
      dependenciesHandler.bundle("foo") {
        it.primary("com.foo:foo")
        it.includeGroup("com.foo")
      }

      // When we build the thing under test
      val bundles = Bundles.of(
        projectPath = ":consumer",
        dependencyGraph = graph,
        bundleRules = dependenciesHandler.serializableBundles(),
        dependencyUsages = dependencyUsages,
        declarations = declarations,
        configurationNames = androidConfigurationNames,
        ignoreKtx = false
      )

      // Then it mutates the advice as expected
      val addAdvice = Advice.ofAdd(fooBar, "debugApi")
      val expectedAdvice = Advice.ofChange(foo, "implementation", "debugApi")
      assertThat(bundles.maybeParent(addAdvice, fooBar)).isEqualTo(expectedAdvice)
    }
  }

  @Nested inner class FlatFiles {
    private val consumer = ProjectCoordinates(":consumer", gvi)
    private val facade = ProjectCoordinates(":facade", gvi)
    private val file = FlatCoordinates("local.jar")

    @Test fun `project bundles include flat artifacts outside their resolution subtree`() {
      configureBundle()
      val included = IncludedBuildCoordinates("example:facade", facade, gvi)
      listOf(facade, included).forEach { parent ->
        val bundles = buildBundles(parent, used = true)
        assertThat(bundles.hasUsedChild(parent)).isTrue()
        assertThat(bundles.findParent(file)).isEqualTo(parent)
      }
    }

    @Test fun `unused flat artifacts do not keep a consumer dependency alive`() {
      configureBundle()
      assertThat(buildBundles(facade, used = false).hasUsedChild(facade)).isFalse()
    }

    @Test fun `module bundles do not absorb unrelated flat artifacts`() {
      val module = ModuleCoordinates("example:facade", "1", gvi)
      dependenciesHandler.bundle("local") {
        it.includeDependency(module.identifier)
        it.includeDependency(file.identifier)
      }
      val bundles = buildBundles(module, used = true)
      assertThat(bundles.hasUsedChild(module)).isFalse()
      assertThat(bundles.hasParent(file)).isFalse()
    }

    @Test fun `only api file declarations are protected by a bundle containing the current project`() {
      configureBundle()
      val declarations = setOf("api", "implementation", "runtimeOnly").mapTo(mutableSetOf()) { configuration ->
        Declaration(file.identifier, configurationName = configuration, gradleVariantIdentification = gvi)
      }
      val bundles = Bundles.of(
        projectPath = facade.identifier,
        dependencyGraph = newGraphFrom(listOf(facade to file)),
        bundleRules = dependenciesHandler.serializableBundles(),
        dependencyUsages = emptyMap(),
        declarations = declarations,
        configurationNames = jvmConfigurationNames,
        ignoreKtx = false,
      )
      assertThat(bundles.exportingProject(file, "api")).isEqualTo(facade)
      assertThat(bundles.exportingProject(file, "implementation")).isNull()
      assertThat(bundles.exportingProject(file, "runtimeOnly")).isNull()
      assertThat(bundles.exportingProject(file, null)).isNull()
    }

    @Test fun `api file declarations without an explicit project bundle are not protected`() {
      val bundles = Bundles.of(
        projectPath = facade.identifier,
        dependencyGraph = newGraphFrom(listOf(facade to file)),
        bundleRules = dependenciesHandler.serializableBundles(),
        dependencyUsages = emptyMap(),
        declarations = setOf(Declaration(file.identifier, configurationName = "api", gradleVariantIdentification = gvi)),
        configurationNames = jvmConfigurationNames,
        ignoreKtx = false,
      )
      assertThat(bundles.exportingProject(file, "api")).isNull()
    }

    private fun configureBundle() {
      dependenciesHandler.bundle("local") {
        it.includeDependency(facade.identifier)
        it.includeDependency(file.identifier)
      }
    }

    private fun buildBundles(parent: Coordinates, used: Boolean): Bundles = Bundles.of(
      projectPath = consumer.identifier,
      // Files are siblings of the facade, and may be visited before it.
      dependencyGraph = newGraphFrom(listOf(consumer to file, consumer to parent)),
      bundleRules = dependenciesHandler.serializableBundles(),
      dependencyUsages = mapOf(file to usage(if (used) Bucket.IMPL else Bucket.NONE, JvmSourceKind.MAIN, "main").intoSet()),
      declarations = emptySet(),
      configurationNames = jvmConfigurationNames,
      ignoreKtx = false,
    )
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
        declarations = emptySet(), // TODO(tsr): tests pass with an empty set, so meh
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
