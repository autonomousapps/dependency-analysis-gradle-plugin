package com.autonomousapps

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension

/**
 * ```
 * plugins {
 *   id 'com.autonomousapps.testkit'
 * }
 * ```
 */
@Suppress("UnstableApiUsage")
public class GradleTestKitPlugin : Plugin<Project> {

  override fun apply(target: Project): Unit = target.run {
    // All projects get the extension and publishing setup
    val extension = GradleTestKitSupportExtension.create(this)

    // Only plugin projects get this
    pluginManager.withPlugin("java-gradle-plugin") {
      val sourceSets = extensions.getByType(SourceSetContainer::class.java)
      val functionalTestSourceSet = sourceSets.create(extension.sourceSetName)

      extensions.getByType(GradlePluginDevelopmentExtension::class.java)
        .testSourceSet(functionalTestSourceSet)

      // Ensure build/functionalTest doesn't grow without bound when tests sometimes fail to clean up after themselves.
      val deleteOldFuncTests = tasks.register("deleteOldFuncTests", Delete::class.java) { t ->
        t.delete(layout.buildDirectory.file(extension.sourceSetName))
      }

      // Automate this somewhere? Unclear how.
      tasks.register("deleteFuncTestRepo", Delete::class.java) { t ->
        t.delete(layout.buildDirectory.file(extension.funcTestRepoName))
      }

      val gradleTest = tasks.register(extension.sourceSetName, Test::class.java) { t ->
        with(t) {
          dependsOn(deleteOldFuncTests, extension.installForFunctionalTest)
          mustRunAfter(tasks.named("test"))

          description = "Runs the functional tests."
          group = "verification"

          testClassesDirs = functionalTestSourceSet.output.classesDirs
          classpath = functionalTestSourceSet.runtimeClasspath

          // Gradle tests generally require more metaspace
          jvmArgs("-XX:+HeapDumpOnOutOfMemoryError", "-XX:MaxMetaspaceSize=1g")

          systemProperty(
            "com.autonomousapps.plugin-under-test.repo",
            target.provider { extension.funcTestRepo.absolutePath }.get()
          )
          systemProperty(
            "com.autonomousapps.plugin-under-test.version",
            target.provider { version.toString() }.get()
          )
        }
      }
      extension.setTestTask(gradleTest)

      tasks.named("check") {
        it.dependsOn(gradleTest)
      }
    }
  }
}
