// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.convention

import com.autonomousapps.convention.tasks.metalava.MetalavaConfigurer
import com.vanniktech.maven.publish.tasks.JavadocJar
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.plugins.signing.Sign
import org.jetbrains.dokka.gradle.DokkaTask

@Suppress("unused")
internal class BaseConventionPlugin(private val project: Project) {

  private val versionCatalog = project.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

  fun configure(): Unit = project.run {
    pluginManager.run {
      apply("com.vanniktech.maven.publish.base")
      apply("org.gradle.signing")
      apply("org.jetbrains.dokka")
      apply("com.autonomousapps.dependency-analysis")
      apply("com.autonomousapps.testkit")
    }

    group = "com.autonomousapps"

    val convention = DagpExtension.of(this)
    val isSnapshot = convention.isSnapshot
    val publishedVersion = convention.publishedVersion

    val jdkVersion = JavaLanguageVersion.of(versionCatalog.findVersion("jdkVersion").orElseThrow().requiredVersion)
    val javaTarget = versionCatalog.findVersion("javaTarget").orElseThrow().requiredVersion.toInt()

    extensions.configure(JavaPluginExtension::class.java) { j ->
      // This breaks publishing for some reason when using gradle-maven-publish-plugin
      //j.withJavadocJar()
      // We need this for some reason, even with configuring gradle-maven-publish-plugin
      j.withSourcesJar()

      // j.toolchain {
      //   it.languageVersion.set(jdkVersion)
      // }
    }
    tasks.withType(JavaCompile::class.java).configureEach { t ->
      t.options.release.set(javaTarget)
    }
    tasks.withType(GroovyCompile::class.java).configureEach { t ->
      t.options.release.set(javaTarget)
    }

    tasks.withType(AbstractArchiveTask::class.java).configureEach { t ->
      t.isPreserveFileTimestamps = false
      t.isReproducibleFileOrder = true
    }

    tasks.withType(DokkaTask::class.java).configureEach { t ->
      t.notCompatibleWithConfigurationCache("Uses 'project' at execution time")
    }
    tasks.withType(JavadocJar::class.java).configureEach { t ->
      t.notCompatibleWithConfigurationCache("Uses 'project' at execution time")
    }

    // We only use the Jupiter platform (JUnit 5)
    configurations.all {
      it.exclude(mapOf("group" to "junit", "module" to "junit"))
      it.exclude(mapOf("group" to "org.junit.vintage", "module" to "junit-vintage-engine"))
    }

    dependencies.let { handler ->
      handler.add("implementation", handler.platform(versionCatalog.findLibrary("kotlin-bom").get()))
    }

    tasks.withType(Test::class.java).configureEach {
      it.useJUnitPlatform()
    }

    val isCi: Provider<Boolean> = providers
      .environmentVariable("CI")
      .orElse("false")
      .map { it.toBoolean() }

    val taskGraph = gradle.taskGraph
    val isFunctionalTest: Provider<Boolean> = providers
      .provider { taskGraph.hasTask(":functionalTest") }

    tasks.withType(Sign::class.java).configureEach { t ->
      with(t) {
        inputs.property("version", publishedVersion)
        inputs.property("is-ci", isCi)
        inputs.property("is-functional-test", isFunctionalTest)

        // Don't sign snapshots
        onlyIf("Not a snapshot") { !isSnapshot.get() }
        // We currently don't support publishing from CI
        onlyIf("release environment") { !isCi.get() }
        // Don't sign when running functional tests
        onlyIf("not running functional tests") { !isFunctionalTest.get() }

        doFirst {
          logger.quiet("Signing v${publishedVersion.get()}")
        }
      }
    }

    tasks.withType(DokkaTask::class.java).configureEach { t ->
      t.inputs.property("is-functional-test", isFunctionalTest)

      // Don't sign when running functional tests
      t.onlyIf("not running functional tests") { !isFunctionalTest.get() }
    }

    configureMetalava()
  }

  private fun Project.configureMetalava() {
    MetalavaConfigurer(this, versionCatalog).configure()
  }
}
