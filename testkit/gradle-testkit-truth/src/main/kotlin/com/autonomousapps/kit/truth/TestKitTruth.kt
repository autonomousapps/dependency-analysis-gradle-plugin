// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.truth

import com.autonomousapps.kit.artifacts.BuildArtifact
import com.autonomousapps.kit.truth.BuildResultSubject.Companion.buildResults
import com.autonomousapps.kit.truth.BuildTaskListSubject.Companion.buildTaskList
import com.autonomousapps.kit.truth.BuildTaskSubject.Companion.buildTasks
import com.autonomousapps.kit.truth.artifact.BuildArtifactsSubject
import com.autonomousapps.kit.truth.artifact.BuildArtifactsSubject.Companion.buildArtifacts
import com.google.common.truth.Truth.assertAbout
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask

public class TestKitTruth {
  public companion object {
    @JvmStatic
    public fun assertThat(target: BuildResult): BuildResultSubject = assertAbout(buildResults()).that(target)

    @JvmStatic
    public fun assertThat(target: BuildTask): BuildTaskSubject = assertAbout(buildTasks()).that(target)

    @JvmStatic
    public fun assertThat(target: List<BuildTask>): BuildTaskListSubject = assertAbout(buildTaskList()).that(target)

    @JvmStatic
    public fun assertThat(target: BuildArtifact): BuildArtifactsSubject = assertAbout(buildArtifacts()).that(target)
  }
}
