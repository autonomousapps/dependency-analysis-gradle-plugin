package com.autonomousapps.internal

import com.autonomousapps.extension.DependenciesHandler
import com.autonomousapps.internal.utils.intoSet
import com.autonomousapps.model.*
import com.autonomousapps.model.declaration.Bucket
import com.autonomousapps.model.declaration.Variant
import com.autonomousapps.model.intermediates.Usage
import com.autonomousapps.test.usage
import com.google.common.truth.Truth.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Suppress("UnstableApiUsage")
class BundlesTest {

  private val project = ProjectBuilder.builder().build()
  private val objects = project.objects
  private val dependenciesHandler = DependenciesHandler(objects)

  @Nested inner class DefaultBundles {
    @Test fun `kotlin stdlib is a default bundle`() {
      val consumer = ProjectCoordinates(":consumer")
      val stdlibJdk8 = ModuleCoordinates("org.jetbrains.kotlin:kotlin-stdlib-jdk8", "1")
      val stdlib = ModuleCoordinates("org.jetbrains.kotlin:kotlin-stdlib", "1")

      // Usages of project :consumer
      val stdlibJdk8Usages = stdlibJdk8 to usage(Bucket.NONE, "main").intoSet()
      val stdlibUsages = stdlib to usage(Bucket.API, "main").intoSet()
      val dependencyUsages: Map<Coordinates, Set<Usage>> = listOf(stdlibJdk8Usages, stdlibUsages).toMap()

      // Dependency graph rooted on :consumer
      val graph = newGraphFrom(
        // :consumer -> stdlib-jdk8 -> stdlib
        listOf(consumer to stdlibJdk8, stdlibJdk8 to stdlib)
      )

      // the thing under test
      val bundles = Bundles.of(
        projectNode = ProjectCoordinates(":consumer"),
        dependencyGraph = graph,
        bundleRules = dependenciesHandler.serializableBundles(),
        dependencyUsages = dependencyUsages,
        ignoreKtx = false
      )

      assertThat(bundles.hasParentInBundle(stdlib)).isTrue()
      assertThat(bundles.hasUsedChild(stdlibJdk8)).isTrue()
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

      val badAdvice = Advice.ofAdd(ProjectCoordinates(":used"), "implementation")
      val expectedAdvice = Advice.ofAdd(ProjectCoordinates(":entry-point"), "implementation")
      assertThat(bundles.primary(badAdvice)).isEqualTo(expectedAdvice)
    }

    @Test fun `without a primary, bundle ignored`() {
      // Define a bundle without a primary identifier
      dependenciesHandler.bundles.create("facade").apply {
        includeDependency(":entry-point")
        includeDependency(":used")
      }
      val bundles = buildBundles()

      // Advice is unchanged
      val advice = Advice.ofAdd(ProjectCoordinates(":used"), "implementation")
      assertThat(bundles.primary(advice)).isEqualTo(advice)
    }

    private fun buildBundles(): Bundles {
      // Coordinates
      val consumer = ProjectCoordinates(":consumer")
      val unused = ProjectCoordinates(":unused")
      val entryPoint = ProjectCoordinates(":entry-point")
      val used = ProjectCoordinates(":used")

      // Usages of project :consumer
      val unusedUsages = unused to usage(Bucket.NONE, "main").intoSet()
      val entryPointUsages = entryPoint to usage(Bucket.NONE, "main").intoSet()
      val usedUsages = used to usage(Bucket.IMPL, "main").intoSet()
      val dependencyUsages: Map<Coordinates, Set<Usage>> = listOf(unusedUsages, entryPointUsages, usedUsages).toMap()

      // Dependency graph rooted on :consumer
      val graph = newGraphFrom(
        // :consumer -> :unused -> :entry-point -> :used
        listOf(consumer to unused, unused to entryPoint, entryPoint to used)
      )

      return Bundles.of(
        projectNode = ProjectCoordinates(":consumer"),
        dependencyGraph = graph,
        bundleRules = dependenciesHandler.serializableBundles(),
        dependencyUsages = dependencyUsages,
        ignoreKtx = false
      )
    }
  }

  private fun newGraphFrom(
    edges: List<Pair<Coordinates, Coordinates>>,
    variant: Variant = Variant.MAIN,
    configurationName: String = "compileClasspath"
  ): Map<String, DependencyGraphView> {
    val graph = DependencyGraphView.newGraphBuilder().apply {
      edges.forEach { putEdge(it.first, it.second) }
    }.build()

    return mapOf(
      "main,Main" to DependencyGraphView(
        variant = variant,
        configurationName = configurationName,
        graph = graph
      )
    )
  }
}
