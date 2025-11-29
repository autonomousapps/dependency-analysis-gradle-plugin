// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.truth

import com.google.common.truth.FailureMetadata
import com.google.common.truth.IterableSubject
import com.google.common.truth.Ordered
import com.google.common.truth.Truth.assertThat
import com.google.errorprone.annotations.CanIgnoreReturnValue
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome

public class BuildTaskListSubject private constructor(
  failureMetadata: FailureMetadata,
  private val actual: List<BuildTask>?
) : IterableSubject(failureMetadata, actual?.map { it.path }) {

  public companion object {
    private val BUILD_TASK_LIST_SUBJECT_FACTORY: Factory<BuildTaskListSubject, List<BuildTask>> =
      Factory { metadata, actual -> BuildTaskListSubject(metadata, actual) }

    @JvmStatic
    public fun buildTaskList(): Factory<BuildTaskListSubject, List<BuildTask>> = BUILD_TASK_LIST_SUBJECT_FACTORY

    /** Ordered implementation that does nothing because it's already known to be true. */
    private val IN_ORDER: Ordered = Ordered {}

    /** Ordered implementation that does nothing because an earlier check already caused a failure. */
    private val ALREADY_FAILED: Ordered = Ordered {}
  }

  @CanIgnoreReturnValue
  public fun containsExactlyPathsIn(expected: Iterable<String>): Ordered {
    val actualPaths = actual?.map { it.path }
    return assertThat(actualPaths).containsExactlyElementsIn(expected)
  }

  @CanIgnoreReturnValue
  public fun containsExactlyPathsIn(vararg expected: String): Ordered {
    val actualPaths = actual?.map { it.path }
    return assertThat(actualPaths).containsExactlyElementsIn(expected)
  }

  @CanIgnoreReturnValue
  public fun containsAtLeastPathsIn(expected: Iterable<String>): Ordered {
    val actualPaths = actual?.map { it.path }
    return assertThat(actualPaths).containsAtLeastElementsIn(expected)
  }

  @CanIgnoreReturnValue
  public fun containsAtLeastPathsIn(vararg expected: String): Ordered {
    val actualPaths = actual?.map { it.path }
    return assertThat(actualPaths).containsAtLeastElementsIn(expected)
  }

  @CanIgnoreReturnValue
  public fun containsExactlyOutcomesIn(expected: Iterable<TaskOutcome>): Ordered {
    val actualOutcomes = actual?.map { it.outcome }
    return assertThat(actualOutcomes).containsExactlyElementsIn(expected)
  }

  @CanIgnoreReturnValue
  public fun containsExactlyOutcomesIn(vararg expected: TaskOutcome): Ordered {
    val actualOutcomes = actual?.map { it.outcome }
    return assertThat(actualOutcomes).containsExactlyElementsIn(expected)
  }

  @CanIgnoreReturnValue
  public fun doesNotContain(expected: Iterable<String>) {
    val actualPaths = actual?.map { it.path }
    return assertThat(actualPaths).containsNoneIn(expected)
  }

  @CanIgnoreReturnValue
  public fun doesNotContain(vararg expected: String) {
    val actualPaths = actual?.map { it.path }
    return assertThat(actualPaths).containsNoneIn(expected)
  }
}
