package com.autonomousapps.extension

import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class IssueHandlerTest {

  private val project = ProjectBuilder.builder().build()
  private val objects = project.objects
  private val issueHandler = IssueHandler(objects)

  @Test fun `when no behavior is defined for a sourceSet, defer to global behavior`() {
    // Given
    issueHandler.all {
      onAny {
        severity("fail")
      }
      sourceSet("custom") {
        onIncorrectConfiguration {
          severity("ignore")
        }
      }
    }

    // When
    val behaviors = issueHandler.anyIssueFor("project").resolve()

    // Then
    assertThat(behaviors)
      .comparingElementsUsing(BEHAVIOR_EQUIVALENCE)
      .containsExactly(
        Fail::class.java to Issue.ALL_SOURCE_SETS,
        Fail::class.java to "custom"
      )
  }

  private fun List<Provider<Behavior>>.resolve(): List<Behavior> = map { it.get() }

  private companion object {
    val BEHAVIOR_EQUIVALENCE: Correspondence<Behavior, Pair<Class<out Behavior>, String>> =
      Correspondence.transforming(::decompose, "matches behavior")

    private fun decompose(behavior: Behavior?): Pair<Class<out Behavior>, String> {
      assertThat(behavior).isNotNull()
      return behavior!!.javaClass to behavior.sourceSetName
    }
  }
}
