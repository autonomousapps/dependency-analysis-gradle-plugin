package com.autonomousapps.kit.truth

import com.autonomousapps.kit.truth.BuildTaskListSubject.Companion.buildTaskList
import com.autonomousapps.kit.truth.BuildTaskSubject.Companion.buildTasks
import com.google.common.truth.Fact.simpleFact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.IterableSubject
import com.google.common.truth.StringSubject
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth.assertAbout
import com.google.errorprone.annotations.CanIgnoreReturnValue
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

public class BuildResultSubject private constructor(
  failureMetadata: FailureMetadata,
  private val actual: BuildResult?
) : Subject(failureMetadata, actual) {

  public companion object {
    private val BUILD_RESULT_SUBJECT_FACTORY: Factory<BuildResultSubject, BuildResult> =
      Factory { metadata, actual -> BuildResultSubject(metadata, actual) }

    @JvmStatic
    public fun buildResults(): Factory<BuildResultSubject, BuildResult> = BUILD_RESULT_SUBJECT_FACTORY

    @JvmStatic
    public fun assertThat(actual: BuildResult?): BuildResultSubject {
      return assertAbout(buildResults()).that(actual)
    }
  }

  public fun output(): StringSubject {
    if (actual == null) {
      failWithActual(simpleFact("build result was null"))
    }
    return check("getOutput()").that(actual!!.output)
  }

  @CanIgnoreReturnValue
  public fun task(path: String): BuildTaskSubject {
    if (actual == null) {
      failWithActual(simpleFact("build result was null"))
    }
    val tasks = actual!!.tasks.map { it.path }

    check("getTasks()").that(tasks).contains(path)

    return check("task(%s)", path).about(buildTasks()).that(actual.task(path))
  }

  public fun doesNotHaveTask(path: String) {
    if (actual == null) {
      failWithActual(simpleFact("build result was null"))
    }
    val tasks = actual!!.tasks.map { it.path }

    check("getTasks()").that(tasks).doesNotContain(path)
  }

  public fun getTasks(): BuildTaskListSubject {
    if (actual == null) {
      failWithActual(simpleFact("build result was null"))
    }
    return check("getTasks()").about(buildTaskList()).that(actual!!.tasks)
  }

  public fun tasks(outcome: TaskOutcome): BuildTaskListSubject {
    if (actual == null) {
      failWithActual(simpleFact("build result was null"))
    }
    return check("tasks(%s)", outcome).about(buildTaskList()).that(actual!!.tasks(outcome))
  }

  public fun taskPaths(outcome: TaskOutcome): IterableSubject {
    if (actual == null) {
      failWithActual(simpleFact("build result was null"))
    }
    return check("taskPaths(%s)", outcome).that(actual!!.taskPaths(outcome))
  }

  override fun actualCustomStringRepresentation(): String {
    return actual?.output ?: "null"
  }
}
