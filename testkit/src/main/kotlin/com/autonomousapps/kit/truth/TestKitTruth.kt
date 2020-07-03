package com.autonomousapps.kit.truth

import com.autonomousapps.kit.truth.BuildResultSubject.Companion.buildResults
import com.autonomousapps.kit.truth.BuildTaskListSubject.Companion.buildTaskList
import com.autonomousapps.kit.truth.BuildTaskSubject.Companion.buildTasks
import com.google.common.truth.Truth.assertAbout
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask

class TestKitTruth {
  companion object {
    @JvmStatic
    fun assertThat(target: BuildResult): BuildResultSubject {
      return assertAbout(buildResults()).that(target)
    }

    @JvmStatic
    fun assertThat(target: BuildTask): BuildTaskSubject {
      return assertAbout(buildTasks()).that(target)
    }

    @JvmStatic
    fun assertThat(target: List<BuildTask>): BuildTaskListSubject {
      return assertAbout(buildTaskList()).that(target)
    }
  }
}
