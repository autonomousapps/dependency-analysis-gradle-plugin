package com.autonomousapps.internal.graph

import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.graph.DependencyGraph
import com.autonomousapps.graph.merge
import com.autonomousapps.test.*
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class GraphMinimizerTest {

  /**
   * :a
   * |
   * :b
   * | \
   * c  d
   * |  |
   * e  f
   *
   * :a uses e and f. :b uses e and f, but does not use c or d. :a should not have to add e and f.
   */
  @Test fun `minimized advice takes into account upstream additions`() {
    // Given
    val graphA = graphFrom(
      ":a", ":b",
      ":b", "c",
      ":b", "d",
      "c", "e",
      "d", "f"
    )
    val graphB = graphFrom(
      ":b", "c",
      ":b", "d",
      "c", "e",
      "d", "f"
    )
    val dependentsGraph = listOf(graphA, graphB).merge().reversed()
    val adviceA = compAdviceFor(
      ":a",
      addAdvice("e", "implementation"),
      addAdvice("f", "implementation")
    )
    val adviceB = compAdviceFor(
      ":b",
      removeAdvice("c", "api"),
      removeAdvice("d", "api"),
      addAdvice("e", "api"),
      addAdvice("f", "api")
    )
    val origBuildHealth = listOf(adviceA, adviceB)
    val expectedBuildHealth = listOf(compAdviceFor(":a"), adviceB)

    // When
    val actual = GraphMinimizer(origBuildHealth, dependentsGraph) {
      when (it) {
        ":a" -> graphA
        ":b" -> graphB
        else -> throw IllegalArgumentException("No graph for project $it")
      }
    }.minimalBuildHealth

    // Then: advice is not changed
    assertThat(actual).containsExactlyElementsIn(expectedBuildHealth)
  }

  /**
   * :a
   * |
   * :b
   * | \
   * c  d
   * |  |
   * e  f
   *
   * :a uses e and f. :a does not use :b. :a must add e and f directly.
   */
  @Test fun `minimized advice takes into account the project's own removals`() {
    // Given
    val graphA = graphFrom(
      ":a", ":b",
      ":b", "c",
      ":b", "d",
      "c", "e",
      "d", "f"
    )
    val graphB = graphFrom(
      ":b", "c",
      ":b", "d",
      "c", "e",
      "d", "f"
    )
    val dependentsGraph = listOf(graphA, graphB).merge().reversed()
    val adviceA = compAdviceFor(
      ":a",
      addAdvice("e", "implementation"),
      addAdvice("f", "implementation"),
      removeAdvice(":b", "implementation")
    )
    val adviceB = compAdviceFor(":b")

    // When
    val origBuildHealth = listOf(adviceA, adviceB)
    val actual = GraphMinimizer(origBuildHealth, dependentsGraph) {
      when (it) {
        ":a" -> graphA
        ":b" -> graphB
        else -> throw IllegalArgumentException("No graph for project $it")
      }
    }.minimalBuildHealth

    // Then: advice is not changed
    assertThat(actual).containsExactlyElementsIn(origBuildHealth)
  }

  /**
   * :a
   * |
   * :b
   * | \
   * c  d
   *
   * :a uses c and d. :b uses neither. :a must add them directly.
   */
  @Test fun `minimized advice takes into account transitive removals`() {
    // Given
    val graphA = graphFrom(
      ":a", ":b",
      ":b", "c",
      ":b", "d"
    )
    val graphB = graphFrom(
      ":b", "c",
      ":b", "d"
    )
    val dependentsGraph = listOf(graphA, graphB).merge().reversed()
    val adviceA = compAdviceFor(
      ":a",
      addAdvice("c", "implementation"),
      addAdvice("d", "implementation")
    )
    val adviceB = compAdviceFor(
      ":b",
      removeAdvice("c", "api"),
      removeAdvice("d", "api")
    )

    // When
    val origBuildHealth = listOf(adviceA, adviceB)
    val actual = GraphMinimizer(origBuildHealth, dependentsGraph) {
      when (it) {
        ":a" -> graphA
        ":b" -> graphB
        else -> throw IllegalArgumentException("No graph for project $it")
      }
    }.minimalBuildHealth

    // Then: advice is not changed
    assertThat(actual).containsExactlyElementsIn(origBuildHealth)
  }

  /**
   * :a
   * |
   * :b
   * |
   * c
   * | ...
   *
   * :a depends on :b and :b exposes c (and its transitive deps) to :a. Advice is to explicitly
   * declare c et al on :a as api deps. This is correct because we should always declare API deps,
   * even if they're already in the graph.
   */
  @Test fun `strict advice is the same as minimal advice because the transitive dep is part of the ABI`() {
    expect(ProjectWithCorrectApiAdvice())
  }

  /**
   * :a
   * |
   * :b
   * |
   * c
   * | ...
   *
   * :a depends on :b and :b exposes c (and its transitive deps) to :a. Advice is to explicitly
   * declare c et al on :a as implementation deps. This is unnecessary because we shouldn't
   * re-declare deps already in the graph, if those deps are not exposed up the graph.
   */
  @Test fun `minimized advice can eliminate add-to-impl advice`() {
    expect(ProjectWithRemovableImplAdvice())
  }

  /**
   * :a
   * |
   * :b
   * | \
   * c  d
   *
   * :a depends on :b and :b exposes c and d (and their transitive deps) to :a. Advice is to
   * explicitly declare a transitive dep of c and d on :a as implementation deps, and to downgrade
   * c from api to implementation. We can strip add-advice on a because, even though one path to
   * transitive dep (via c) is removed, there remains another (via d).
   */
  @Test fun `multiple paths to a transitive dep are accounted for`() {
    expect(ProjectWithMultiplePathsToKotlinStdlib())
  }

  private fun <T : BaseProject> expect(project: T) {
    // When
    val actual = GraphMinimizer(
      project.strictBuildHealth, project.dependentsGraph(), project.graphProvider()
    ).minimalBuildHealth

    // Then
    assertThat(actual).containsExactlyElementsIn(project.minimizedBuildHealth)
  }

  /**
   * This simple base project has two modules, :app and :lib. :app depends on :lib and :lib depends
   * on kotlin-stdlib, moshi-kotlin, and moshi-adapters, which bring along their transitive
   * dependencies.
   */
  private abstract class BaseProject {
    // This structure indicates that :lib has implementation dependencies on its three dependencies
    open fun appGraph() = graphFrom(":app", ":lib")
    open fun libGraph() = graphFrom(
      ":lib", "kotlin-stdlib-jdk8",
      ":lib", "moshi-kotlin",
      ":lib", "moshi-adapters",
      "kotlin-stdlib-jdk8", "kotlin-stdlib-jdk7",
      "kotlin-stdlib-jdk7", "kotlin-stdlib",
      "kotlin-stdlib", "jb:annotations",
      "kotlin-stdlib", "kotlin-stdlib-common",
      "moshi-kotlin", "kotlin-reflect", // TODO missing kotlin-reflect's dependencies
      "moshi-kotlin", "kotlin-stdlib",
      "moshi-kotlin", "moshi",
      "moshi-adapters", "moshi",
      "moshi-adapters", "retrofit2",
      "moshi", "okio",
      "retrofit2", "okhttp",
      "okhttp", "okio"
    )

    fun dependentsGraph() = listOf(appGraph(), libGraph()).merge().reversed()

    abstract val strictBuildHealth: List<ComprehensiveAdvice>
    abstract val minimizedBuildHealth: List<ComprehensiveAdvice>

    fun graphProvider(): (String) -> DependencyGraph = {
      when (it) {
        ":app" -> appGraph()
        ":lib" -> libGraph()
        else -> throw IllegalArgumentException("Unexpected test project: $it")
      }
    }
  }

  private class ProjectWithCorrectApiAdvice : BaseProject() {
    // This structure indicates that :lib has an api dependency on kotlin-stdlib-jdk8
    override fun appGraph() = graphFrom(
      ":app", ":lib",
      ":lib", "kotlin-stdlib-jdk8",
      "kotlin-stdlib-jdk8", "kotlin-stdlib-jdk7",
      "kotlin-stdlib-jdk7", "kotlin-stdlib",
      "kotlin-stdlib", "jb:annotations",
      "kotlin-stdlib", "kotlin-stdlib-common"
    )

    override val strictBuildHealth = listOf(
      compAdviceFor(":app", addAdvice("kotlin-stdlib", "api")),
      compAdviceFor(":lib")
    )
    override val minimizedBuildHealth = strictBuildHealth
  }

  private class ProjectWithRemovableImplAdvice : BaseProject() {
    // This structure indicates that :lib has an api dependency on kotlin-stdlib-jdk8
    override fun appGraph() = graphFrom(
      ":app", ":lib",
      ":lib", "kotlin-stdlib-jdk8",
      "kotlin-stdlib-jdk8", "kotlin-stdlib-jdk7",
      "kotlin-stdlib-jdk7", "kotlin-stdlib",
      "kotlin-stdlib", "jb:annotations",
      "kotlin-stdlib", "kotlin-stdlib-common"
    )

    override val strictBuildHealth = listOf(
      compAdviceFor(":app", addAdvice("kotlin-stdlib", "implementation")),
      compAdviceFor(":lib")
    )
    override val minimizedBuildHealth = listOf(
      compAdviceFor(":app"),
      compAdviceFor(":lib")
    )
  }

  private class ProjectWithMultiplePathsToKotlinStdlib : BaseProject() {
    // This structure indicates that :lib has api dependencies on kotlin-stdlib-jdk8 & moshi-kotlin
    override fun appGraph() = graphFrom(
      ":app", ":lib",
      ":lib", "kotlin-stdlib-jdk8",
      ":lib", "moshi-kotlin",
      "kotlin-stdlib-jdk8", "kotlin-stdlib-jdk7",
      "kotlin-stdlib-jdk7", "kotlin-stdlib",
      "kotlin-stdlib", "jb:annotations",
      "kotlin-stdlib", "kotlin-stdlib-common",
      "moshi-kotlin", "kotlin-reflect",
      "moshi-kotlin", "kotlin-stdlib",
      "moshi-kotlin", "moshi",
      "moshi", "okio",
      "retrofit2", "okhttp",
      "okhttp", "okio"
    )

    override val strictBuildHealth = listOf(
      compAdviceFor(":app", addAdvice("kotlin-stdlib", "implementation")),
      compAdviceFor(":lib", changeAdvice("kotlin-stdlib-jdk8", "api", "implementation"))
    )

    // :app's add-advice is removed because kotlin-stdlib is available via an alternative path
    // (through moshi)
    override val minimizedBuildHealth = listOf(
      compAdviceFor(":app"),
      compAdviceFor(":lib", changeAdvice("kotlin-stdlib-jdk8", "api", "implementation"))
    )
  }
}
