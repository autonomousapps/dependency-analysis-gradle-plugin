package com.autonomousapps.convention

import com.gradle.publish.PluginBundleExtension
import nexus.NexusPublishTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import java.util.Locale

@Suppress("unused")
class ConventionPlugin : Plugin<Project> {
  override fun apply(target: Project): Unit = target.run {
    pluginManager.apply("org.gradle.maven-publish")
    pluginManager.apply("org.gradle.signing")

    val convention = ConventionExtension.of(this)
    val isSnapshot = convention.isSnapshot
    val publishedVersion = convention.publishedVersion

    val versionCatalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

    extensions.configure(JavaPluginExtension::class.java) { j ->
      j.withJavadocJar()
      j.withSourcesJar()
      j.toolchain {
        it.languageVersion.set(
          JavaLanguageVersion.of(versionCatalog.findVersion("java").orElseThrow().requiredVersion)
        )
      }
    }

    val signing = extensions.getByType(SigningExtension::class.java)
    val publishing = extensions.getByType(PublishingExtension::class.java)

    publishing.publications.create(MAVEN_PUB_NAME, MavenPublication::class.java) { p ->
      p.from(project.components.getAt("java"))
    }

    publishing.publications.all {
      signing.sign(it)
    }

    publishing.repositories { r ->
      r.maven { a ->
        a.name = "local"
        a.url = project.uri(project.layout.buildDirectory.dir("repo"))
      }
    }

    val promoteTask = tasks.register("promote", NexusPublishTask::class.java) {
      it.onlyIf { !isSnapshot.get() }
    }

    val publishToMavenCentral = tasks.register("publishToMavenCentral") { t ->
      t.group = "publishing"

      // task name based on combination of publication name "maven" and repo name "sonatype"
      t.dependsOn("publish${MAVEN_PUB_NAME.capitalizeSafely()}PublicationTo$SONATYPE_REPO_SUFFIX")
      t.finalizedBy(promoteTask)

      t.doLast {
        if (isSnapshot.get()) {
          logger.quiet("Browse files at https://oss.sonatype.org/content/repositories/snapshots/com/autonomousapps")
        } else {
          logger.quiet("After publishing to Sonatype, visit https://oss.sonatype.org to close and release from staging")
        }
      }
    }

    // Used to set the description dynamically
    convention.setPublishToMavenCentral(publishToMavenCentral)

    pluginManager.withPlugin("com.gradle.plugin-publish") {
      extensions.getByType(PluginBundleExtension::class.java).plugins.all { pluginConfig ->
        publishToMavenCentral.configure { t ->
          //publishDependencyAnalysisPluginPluginMarkerMavenPublicationToSonatypeRepository
          t.dependsOn("publish${pluginConfig.name.capitalizeSafely()}PluginMarkerMavenPublicationTo$SONATYPE_REPO_SUFFIX")
        }
      }
    }

    tasks.withType(Sign::class.java).configureEach { t ->
      t.inputs.property("version", publishedVersion)
      t.onlyIf { !isSnapshot.get() }
      t.doFirst {
        logger.quiet("Signing v${publishedVersion.get()}")
      }
    }

    // TODO do this?
    //publishing {
    //  publications {
    //    create<MavenPublication>("truth") {
    //      from(components["java"])
    //      configurePom(pom)
    //      signing.sign(this)
    //
    //      versionMapping {
    //        usage("java-api") {
    //          fromResolutionOf("runtimeClasspath")
    //        }
    //        usage("java-runtime") {
    //          fromResolutionResult()
    //        }
    //      }
    //    }
    //  }
    //}
  }

  internal companion object {
    const val MAVEN_PUB_NAME = "maven"
    const val SONATYPE_REPO_NAME = "sonatype"
    val SONATYPE_REPO_SUFFIX = "${SONATYPE_REPO_NAME.capitalizeSafely()}Repository"
  }
}

// copied from StringsJVM.kt
private fun String.capitalizeSafely(locale: Locale = Locale.ROOT): String {
  if (isNotEmpty()) {
    val firstChar = this[0]
    if (firstChar.isLowerCase()) {
      return buildString {
        val titleChar = firstChar.titlecaseChar()
        if (titleChar != firstChar.uppercaseChar()) {
          append(titleChar)
        } else {
          append(this@capitalizeSafely.substring(0, 1).uppercase(locale))
        }
        append(this@capitalizeSafely.substring(1))
      }
    }
  }
  return this
}
