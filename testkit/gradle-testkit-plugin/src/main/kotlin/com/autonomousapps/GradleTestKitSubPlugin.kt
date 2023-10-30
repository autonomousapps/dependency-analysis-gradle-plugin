package com.autonomousapps

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * ```
 * plugins {
 *   id 'com.autonomousapps.testkit-dependency'
 * }
 * ```
 */
public class GradleTestKitSubPlugin : Plugin<Project> {

  override fun apply(target: Project): Unit = target.run {
    PublishingConfigurator(this)
  }
}
