// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.convention

import com.autonomousapps.convention.internal.kotlin.KotlinConfigurer
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

public abstract class LibKotlinConventionPlugin : Plugin<Project> {

  override fun apply(target: Project): Unit = target.run {
    pluginManager.apply("org.jetbrains.kotlin.jvm")
    BaseConventionPlugin(this).configure()

    configureKotlin()
    configurePublishing()
  }

  private fun Project.configureKotlin() {
    KotlinConfigurer(this).configure()
  }

  private fun Project.configurePublishing() {
    extensions.getByType(MavenPublishBaseExtension::class.java).run {
      configure(
        KotlinJvm(
          javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationJavadoc"),
          sourcesJar = true,
        )
      )
    }
  }
}
