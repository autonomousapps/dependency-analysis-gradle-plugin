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

  // TODO the name of the functional test source set should be configurable.
  //  and if it is "test", then we should skip some of this.
  override fun apply(target: Project): Unit = target.run {
    // All projects get the extension and publishing setup
    GradleTestKitSupportExtension.create(this)
    val configurator = PublishingConfigurator(this)

    // Only plugin projects get this
    pluginManager.withPlugin("java-gradle-plugin") {
      val sourceSets = extensions.getByType(SourceSetContainer::class.java)
      val functionalTestSourceSet = sourceSets.create("functionalTest")

      val gradlePlugin = extensions.getByType(GradlePluginDevelopmentExtension::class.java)
      gradlePlugin.testSourceSet(functionalTestSourceSet)

      // Ensure build/functionalTest doesn't grow without bound when tests sometimes fail to clean up after themselves.
      val deleteOldFuncTests = tasks.register("deleteOldFuncTests", Delete::class.java) { t ->
        t.delete(layout.buildDirectory.file("functionalTest"))
      }

      // Automate this somewhere? Unclear how.
      tasks.register("deleteFuncTestRepo", Delete::class.java) { t ->
        t.delete(layout.buildDirectory.file(configurator.funcTestRepoName))
      }

      val functionalTest = tasks.register("functionalTest", Test::class.java) { t ->
        with(t) {
          dependsOn(deleteOldFuncTests, configurator.installForFunctionalTest)
          mustRunAfter(tasks.named("test"))

          description = "Runs the functional tests."
          group = "verification"

          testClassesDirs = functionalTestSourceSet.output.classesDirs
          classpath = functionalTestSourceSet.runtimeClasspath

          // Gradle tests generally require more metaspace
          jvmArgs("-XX:+HeapDumpOnOutOfMemoryError", "-XX:MaxMetaspaceSize=1g")

          systemProperty("com.autonomousapps.plugin-under-test.repo", configurator.funcTestRepo.absolutePath)
          systemProperty("com.autonomousapps.plugin-under-test.version", version.toString())
        }
      }

      tasks.named("check") {
        it.dependsOn(functionalTest)
      }
    }
  }
}
