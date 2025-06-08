// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.convention

import com.vanniktech.maven.publish.tasks.JavadocJar
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.dokka.gradle.DokkaTask

@Suppress("unused")
internal class BaseConventionPlugin(private val project: Project) {

  fun configure() = project.run {
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

    val versionCatalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
    val signing = extensions.getByType(SigningExtension::class.java)
    val publishing = extensions.getByType(PublishingExtension::class.java)
    val javaVersion = JavaLanguageVersion.of(versionCatalog.findVersion("java").orElseThrow().requiredVersion)

    extensions.configure(JavaPluginExtension::class.java) { j ->
      // This breaks publishing for some reason when using gradle-maven-publish-plugin
      //j.withJavadocJar()
      // We need this for some reason, even with configuring gradle-maven-publish-plugin
      j.withSourcesJar()

      j.toolchain {
        it.languageVersion.set(javaVersion)
      }
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

    // afterEvaluate {
    // val isPluginProject = pluginManager.hasPlugin("java-gradle-plugin")
    // if (isPluginProject) {
    //   // For plugin projects, the task name is different because one gets added automatically with a publication named
    //   // "pluginMaven".
    //   publishToMavenCentral.configure { t ->
    //     t.dependsOn(tasks.named { it == "publishPluginMavenPublicationTo$SONATYPE_REPO_SUFFIX" })
    //   }
    // } else {
    //   // Not a plugin project. We don't need this publication for the plugin itself, because it already exists.
    //   publishing.publications.create(MAVEN_PUB_NAME, MavenPublication::class.java) { p ->
    //     p.from(project.components.getAt("java"))
    //   }
    //
    //   publishToMavenCentral.configure { t ->
    //     t.dependsOn(tasks.named { it == "publish${MAVEN_PUB_NAME.capitalizeSafely()}PublicationTo$SONATYPE_REPO_SUFFIX" })
    //   }
    // }

    // val promoteTask = tasks.register("promote", NexusPublishTask::class.java) {
    //   it.onlyIf { !isSnapshot.get() }
    //   it.configureWith(Credentials(project))
    // }

    // Used to set the description dynamically
    // convention.setPublishToMavenCentral(publishToMavenCentral)

    //pluginManager.withPlugin("com.gradle.plugin-publish")
    // pluginManager.withPlugin("java-gradle-plugin") {
    //   extensions.getByType(GradlePluginDevelopmentExtension::class.java).plugins.all { pluginConfig ->
    //     publishToMavenCentral.configure { t ->
    //       t.dependsOn(
    //         // e.g. publishDependencyAnalysisPluginPluginMarkerMavenPublicationToSonatypeRepository
    //         tasks.named { it == "publish${pluginConfig.name.capitalizeSafely()}PluginMarkerMavenPublicationTo$SONATYPE_REPO_SUFFIX" }
    //         // "publish${pluginConfig.name.capitalizeSafely()}PluginMarkerMavenPublicationTo$SONATYPE_REPO_SUFFIX"
    //       )
    //     }
    //   }
    // }

    val isCi: Provider<Boolean> = providers
      .environmentVariable("CI")
      .orElse("false")
      .map { it.toBoolean() }

    tasks.withType(Sign::class.java).configureEach { t ->
      with(t) {
        inputs.property("version", publishedVersion)
        inputs.property("is-ci", isCi)

        // Don't sign snapshots
        onlyIf("Not a snapshot") { !isSnapshot.get() }
        // We currently don't support publishing from CI
        onlyIf("release environment") { !isCi.get() }

        doFirst {
          logger.quiet("Signing v${publishedVersion.get()}")
        }
      }
    }
  }

  // internal companion object {
  //   const val MAVEN_PUB_NAME = "maven"
  //   const val SONATYPE_REPO_NAME = "sonatype"
  //   val SONATYPE_REPO_SUFFIX = "${SONATYPE_REPO_NAME.capitalizeSafely()}Repository"
  // }
}

// copied from StringsJVM.kt
// private fun String.capitalizeSafely(locale: Locale = Locale.ROOT): String {
//   if (isNotEmpty()) {
//     val firstChar = this[0]
//     if (firstChar.isLowerCase()) {
//       return buildString {
//         val titleChar = firstChar.titlecaseChar()
//         if (titleChar != firstChar.uppercaseChar()) {
//           append(titleChar)
//         } else {
//           append(this@capitalizeSafely.substring(0, 1).uppercase(locale))
//         }
//         append(this@capitalizeSafely.substring(1))
//       }
//     }
//   }
//   return this
// }
