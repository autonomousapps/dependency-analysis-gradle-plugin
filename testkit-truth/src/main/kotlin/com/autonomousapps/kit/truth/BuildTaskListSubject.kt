package com.autonomousapps.kit.truth

import com.google.common.truth.FailureMetadata
import com.google.common.truth.IterableSubject
import com.google.common.truth.Ordered
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth.assertThat
import com.google.errorprone.annotations.CanIgnoreReturnValue
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome

class BuildTaskListSubject private constructor(
  failureMetadata: FailureMetadata,
  private val actual: List<BuildTask>
) : IterableSubject(failureMetadata, actual.map { it.path }) {

  companion object {
    private val BUILD_TASK_LIST_SUBJECT_FACTORY: Factory<BuildTaskListSubject, List<BuildTask>> =
      Factory { metadata, actual -> BuildTaskListSubject(metadata, actual) }

    @JvmStatic
    fun buildTaskList(): Factory<BuildTaskListSubject, List<BuildTask>> = BUILD_TASK_LIST_SUBJECT_FACTORY

    /** Ordered implementation that does nothing because it's already known to be true. */
    private val IN_ORDER: Ordered = Ordered {}

    /** Ordered implementation that does nothing because an earlier check already caused a failure. */
    private val ALREADY_FAILED: Ordered = Ordered {}
  }

  @CanIgnoreReturnValue
  fun containsExactlyPathsIn(expected: List<String>): Ordered {
    val actualPaths = actual.map { it.path }
    return assertThat(actualPaths).containsExactlyElementsIn(expected)
  }

  @CanIgnoreReturnValue
  fun containsExactlyOutcomesIn(expected: List<TaskOutcome>): Ordered {
    val actualOutcomes = actual.map { it.outcome }
    return assertThat(actualOutcomes).containsExactlyElementsIn(expected)
  }

  @CanIgnoreReturnValue
  fun doesNotContain(expected: List<String>) {
    val actualPaths = actual.map { it.path }
    return assertThat(actualPaths).containsNoneIn(expected)
  }
}
