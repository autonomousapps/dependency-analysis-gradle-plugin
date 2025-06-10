package com.autonomousapps.convention

import com.autonomousapps.convention.internal.kotlin.configureDokka
import com.autonomousapps.convention.internal.kotlin.configureKotlin
import com.gradle.publish.PublishTask
import com.vanniktech.maven.publish.GradlePublishPlugin
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
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
    // TODO(tsr): do this in a follow-up
    // configurePlugins()
    configurePublishing()
    disableConfigurationCache()
  }

  private fun Project.configurePlugins() {
    // https://github.com/gradle/gradle/issues/22600
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
  }
}
