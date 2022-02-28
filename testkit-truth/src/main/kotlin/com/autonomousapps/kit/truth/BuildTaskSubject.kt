package com.autonomousapps.kit.truth

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth.assertAbout
import com.google.errorprone.annotations.CanIgnoreReturnValue
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome

class BuildTaskSubject private constructor(
  failureMetadata: FailureMetadata,
  private val actual: BuildTask?
) : Subject(failureMetadata, actual) {

  companion object {
    private val BUILD_TASK_SUBJECT_FACTORY: Factory<BuildTaskSubject, BuildTask> =
      Factory { metadata, actual -> BuildTaskSubject(metadata, actual) }

    @JvmStatic
    fun buildTasks(): Factory<BuildTaskSubject, BuildTask> = BUILD_TASK_SUBJECT_FACTORY

    @JvmStatic
    fun assertThat(actual: BuildTask?): BuildTaskSubject {
      return assertAbout(buildTasks()).that(actual)
    }
  }

  @CanIgnoreReturnValue
  fun succeeded() = hasOutcome(TaskOutcome.SUCCESS)

  @CanIgnoreReturnValue
  fun failed() = hasOutcome(TaskOutcome.FAILED)

  @CanIgnoreReturnValue
  fun fromCache() = hasOutcome(TaskOutcome.FROM_CACHE)

  @CanIgnoreReturnValue
  fun noSource() = hasOutcome(TaskOutcome.NO_SOURCE)

  @CanIgnoreReturnValue
  fun skipped() = hasOutcome(TaskOutcome.SKIPPED)

  @CanIgnoreReturnValue
  fun upToDate() = hasOutcome(TaskOutcome.UP_TO_DATE)

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
  fun hasPath(expected: String): BuildTaskSubject {
    if (actual == null) {
      failWithActual("expected to have a value", expected)
    } else {
      check("getPath()").that(actual.path).isEqualTo(expected)
    }
    return this
  }

  fun and(): BuildTaskSubject = this
}
