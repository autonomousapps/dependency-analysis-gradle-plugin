// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import com.autonomousapps.GradleTestKitSupportExtension
import com.autonomousapps.internal.arguments.RepoAndVersionArgumentProvider
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension

// TODO(tsr): consider merging this code into the extension. Not entirely clear on the separation of concerns.
@Suppress("UnstableApiUsage")
internal class Configurer(
  private val project: Project,
  private val extension: GradleTestKitSupportExtension,
) {

  fun configure() {
    project.conf()
  }

  private fun Project.conf() {
    val sourceSets = extensions.getByType(SourceSetContainer::class.java)
    val functionalTestSourceSet = sourceSets.create(extension.sourceSetName)

    extensions.findByType(GradlePluginDevelopmentExtension::class.java)
      ?.testSourceSet(functionalTestSourceSet)

    // Ensure build/functionalTest doesn't grow without bound when tests sometimes fail to clean up after themselves.
    val deleteOldFuncTests = tasks.register("deleteOldFuncTests", Delete::class.java) { t ->
      t.delete(layout.buildDirectory.file(extension.sourceSetName))
    }

    // Automate this somewhere? Unclear how.
    tasks.register("deleteFuncTestRepo", Delete::class.java) { t ->
      t.delete(layout.buildDirectory.file(extension.funcTestRepoName))
    }

    val functionalTest = tasks.register(extension.sourceSetName, Test::class.java) { t ->
      with(t) {
        dependsOn(deleteOldFuncTests, extension.installForFunctionalTest)
        mustRunAfter(tasks.named("test"))

        description = "Runs the functional tests."
        group = "verification"

        testClassesDirs = functionalTestSourceSet.output.classesDirs
        classpath = functionalTestSourceSet.runtimeClasspath

        // Gradle tests generally require more metaspace
        jvmArgs("-XX:+HeapDumpOnOutOfMemoryError", "-XX:MaxMetaspaceSize=1g")

        jvmArgumentProviders += RepoAndVersionArgumentProvider(
          repo = extension.funcTestRepo.invariantSeparatorsPath,
          version = version.toString(),
        )
      }
    }
    extension.setTestTask(functionalTest)

    tasks.named("check") {
      it.dependsOn(functionalTest)
    }
  }
}
