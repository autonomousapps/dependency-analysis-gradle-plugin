package com.autonomousapps.kit.truth

import com.google.common.collect.Iterables
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth.assertAbout
import com.google.errorprone.annotations.CanIgnoreReturnValue
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome

public class BuildTaskSubject private constructor(
  failureMetadata: FailureMetadata,
  private val actual: BuildTask?,
) : Subject(failureMetadata, actual) {

  public companion object {
    private val BUILD_TASK_SUBJECT_FACTORY: Factory<BuildTaskSubject, BuildTask> =
      Factory { metadata, actual -> BuildTaskSubject(metadata, actual) }

    @JvmStatic
    public fun buildTasks(): Factory<BuildTaskSubject, BuildTask> = BUILD_TASK_SUBJECT_FACTORY

    @JvmStatic
    public fun assertThat(actual: BuildTask?): BuildTaskSubject {
      return assertAbout(buildTasks()).that(actual)
    }
  }

  @CanIgnoreReturnValue
  public fun succeeded(): BuildTaskSubject = hasOutcome(TaskOutcome.SUCCESS)

  @CanIgnoreReturnValue
  public fun failed(): BuildTaskSubject = hasOutcome(TaskOutcome.FAILED)

  @CanIgnoreReturnValue
  public fun fromCache(): BuildTaskSubject = hasOutcome(TaskOutcome.FROM_CACHE)

  @CanIgnoreReturnValue
  public fun noSource(): BuildTaskSubject = hasOutcome(TaskOutcome.NO_SOURCE)

  @CanIgnoreReturnValue
  public fun skipped(): BuildTaskSubject = hasOutcome(TaskOutcome.SKIPPED)

  @CanIgnoreReturnValue
  public fun upToDate(): BuildTaskSubject = hasOutcome(TaskOutcome.UP_TO_DATE)

  @CanIgnoreReturnValue
  public fun hasOutcomeIn(outcomes: Iterable<TaskOutcome>): BuildTaskSubject {
    if (actual == null) {
      failWithActual("expected to have a value", outcomes)
    }
    if (!Iterables.contains(outcomes, actual!!.outcome)) {
      failWithActual("expected any of", outcomes)
    }
    return this
  }

  @CanIgnoreReturnValue
  public fun hasOutcomeIn(vararg outcomes: TaskOutcome): BuildTaskSubject {
    if (actual == null) {
      failWithActual("expected to have a value", outcomes)
    }
    if (!Iterables.contains(outcomes.toList(), actual!!.outcome)) {
      failWithActual("expected any of", outcomes.toList())
    }
    return this
  }

  @CanIgnoreReturnValue
  private fun hasOutcome(expected: TaskOutcome): BuildTaskSubject {
    if (actual == null) {
      failWithActual("expected to have a value", expected)
    } else {
      check("getOutcome()").that(actual.outcome).isEqualTo(expected)
    }
    return this
  }

  @CanIgnoreReturnValue
  public fun hasPath(expected: String): BuildTaskSubject {
    if (actual == null) {
      failWithActual("expected to have a value", expected)
    } else {
      check("getPath()").that(actual.path).isEqualTo(expected)
    }
    return this
  }

  public fun and(): BuildTaskSubject = this
}
