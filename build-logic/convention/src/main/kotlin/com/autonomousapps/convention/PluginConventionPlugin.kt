// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.convention

import com.autonomousapps.convention.internal.kotlin.configureDokka
import com.autonomousapps.convention.internal.kotlin.configureKotlin
import com.gradle.publish.PublishTask
import com.vanniktech.maven.publish.GradlePublishPlugin
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.plugin.devel.tasks.ValidatePlugins

public abstract class PluginConventionPlugin : Plugin<Project> {

  override fun apply(target: Project): Unit = target.run {
    pluginManager.run {
      apply("java-gradle-plugin")
      apply("org.jetbrains.kotlin.jvm")
      apply("com.gradle.plugin-publish")
    }
    BaseConventionPlugin(this).configure()

    val versionCatalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

    configureDokka(versionCatalog)
    configureKotlin(versionCatalog)
    configurePlugins()
    configurePublishing()
    disableConfigurationCache()
  }

  /**
   * @see <a href="https://github.com/gradle/gradle/issues/22600">Enable stricter validation of plugins by default for validatePlugins task</a>
   */
  private fun Project.configurePlugins() {
    tasks.withType(ValidatePlugins::class.java).configureEach { t ->
      t.enableStricterValidation.set(true)
    }
  }

  private fun Project.configurePublishing() {
    extensions.getByType(MavenPublishBaseExtension::class.java).run {
      configure(GradlePublishPlugin())
    }
  }

  private fun Project.disableConfigurationCache() {
    tasks.withType(PublishTask::class.java).configureEach { t ->
      t.notCompatibleWithConfigurationCache("Various problems")
    }
    tasks.withType(AbstractPublishToMaven::class.java) { t ->
      t.notCompatibleWithConfigurationCache("Various problems")
    }
  }
}
