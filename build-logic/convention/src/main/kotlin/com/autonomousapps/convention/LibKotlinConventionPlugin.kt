package com.autonomousapps.convention

import com.autonomousapps.convention.internal.kotlin.configureDokka
import com.autonomousapps.convention.internal.kotlin.configureKotlin
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension

public abstract class LibKotlinConventionPlugin : Plugin<Project> {

  override fun apply(target: Project): Unit = target.run {
    pluginManager.apply("org.jetbrains.kotlin.jvm")
    BaseConventionPlugin(this).configure()

    val versionCatalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

    configureDokka(versionCatalog)
    configureKotlin(versionCatalog)
    configurePublishing()
  }

  private fun Project.configurePublishing() {
    extensions.getByType(MavenPublishBaseExtension::class.java).run {
      configure(
        KotlinJvm(
          javadocJar = JavadocJar.Dokka("dokkaHtml"),
          sourcesJar = true,
        )
      )
    }
  }
}
