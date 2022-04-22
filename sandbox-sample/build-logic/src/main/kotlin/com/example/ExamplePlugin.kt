package com.example

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * ```
 * plugins {
 *   id "com.example.example"
 * }
 * ```
 */
@Suppress("unused")
class ExamplePlugin : Plugin<Project> {

  override fun apply(target: Project): Unit = target.run {
    tasks.register("example", ExampleTask::class.java) {
      it.hasKapt.set(providers.provider { plugins.hasPlugin("kotlin-kapt") })
    }
  }

  abstract class ExampleTask : DefaultTask() {

    @get:Input
    abstract val hasKapt: Property<Boolean>

    @TaskAction fun action() {
      val hasKapt = hasKapt.get()
      logger.quiet("Kapt is applied: $hasKapt")
    }
  }
}