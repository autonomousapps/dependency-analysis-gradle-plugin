package com.autonomousapps.convention

import com.gradle.publish.PluginBundleExtension
import nexus.Credentials
import nexus.NexusPublishTask
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import java.util.Locale

@Suppress("unused", "UnstableApiUsage")
class ConventionPlugin : Plugin<Project> {

  override fun apply(target: Project): Unit = target.run {
    pluginManager.apply("org.gradle.maven-publish")
    pluginManager.apply("org.gradle.signing")

    tasks.named("outgoingVariants").configure {
      it.notCompatibleWithConfigurationCache("Sigh")
    }

    val convention = ConventionExtension.of(this)
    val isSnapshot = convention.isSnapshot
    val publishedVersion = convention.publishedVersion

    val versionCatalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
    val signing = extensions.getByType(SigningExtension::class.java)
    val publishing = extensions.getByType(PublishingExtension::class.java)

    val publishToMavenCentral = tasks.register("publishToMavenCentral")

    extensions.configure(JavaPluginExtension::class.java) { j ->
      j.withJavadocJar()
      j.withSourcesJar()
      j.toolchain {
        it.languageVersion.set(
          JavaLanguageVersion.of(versionCatalog.findVersion("java").orElseThrow().requiredVersion)
        )
      }
    }

    afterEvaluate {
      val isPluginProject = pluginManager.hasPlugin("java-gradle-plugin")
      if (isPluginProject) {
        // For plugin projects, the task name is different because one gets added automatically with a publication named
        // "pluginMaven".
        publishToMavenCentral.configure {
          it.dependsOn("publishPluginMavenPublicationTo$SONATYPE_REPO_SUFFIX")
        }
      } else {
        // Not a plugin project. We don't need this publication for the plugin itself, because it already exists.
        publishing.publications.create(MAVEN_PUB_NAME, MavenPublication::class.java) { p ->
          p.from(project.components.getAt("java"))
        }

        publishToMavenCentral.configure {
          it.dependsOn("publish${MAVEN_PUB_NAME.capitalizeSafely()}PublicationTo$SONATYPE_REPO_SUFFIX")
        }
      }

      publishing.publications.all { pub ->
        signing.sign(pub)

        // Some weird behavior with the `com.gradle.plugin-publish` plugin.
        // I need to do this in afterEvaluate or it breaks.
        convention.pomConfiguration?.let { configure ->
          if (pub is MavenPublication) {
            setupPom(pub.pom, configure)
          }
        }
      }
    }

    publishing.repositories { r ->
      r.maven { a ->
        a.name = "local"
        a.url = project.uri(project.layout.buildDirectory.dir("repo"))
      }
    }

    val promoteTask = tasks.register("promote", NexusPublishTask::class.java) {
      it.onlyIf { !isSnapshot.get() }
      it.configureWith(Credentials(project))
    }

    publishToMavenCentral.configure { t ->
      with(t) {
        group = "publishing"
        finalizedBy(promoteTask)
        doLast {
          if (isSnapshot.get()) {
            logger.quiet("Browse files at https://oss.sonatype.org/content/repositories/snapshots/com/autonomousapps")
          } else {
            logger.quiet("After publishing to Sonatype, visit https://oss.sonatype.org to close and release from staging")
          }
        }
      }
    }

    // Used to set the description dynamically
    convention.setPublishToMavenCentral(publishToMavenCentral)

    pluginManager.withPlugin("com.gradle.plugin-publish") {
      extensions.getByType(PluginBundleExtension::class.java).plugins.all { pluginConfig ->
        publishToMavenCentral.configure { t ->
          // e.g. publishDependencyAnalysisPluginPluginMarkerMavenPublicationToSonatypeRepository
          t.dependsOn("publish${pluginConfig.name.capitalizeSafely()}PluginMarkerMavenPublicationTo$SONATYPE_REPO_SUFFIX")
        }
      }
    }

    tasks.withType(Sign::class.java).configureEach { t ->
      with(t) {
        notCompatibleWithConfigurationCache("https://github.com/gradle/gradle/issues/13470")
        inputs.property("version", publishedVersion)
        onlyIf { !isSnapshot.get() }
        doFirst {
          logger.quiet("Signing v${publishedVersion.get()}")
        }
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

  private fun setupPom(pom: MavenPom, configure: Action<MavenPom>) {
    pom.run {
      url.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
      inceptionYear.set("2022")
      licenses {
        it.license { l ->
          l.name.set("The Apache License, Version 2.0")
          l.url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
        }
      }
      developers {
        it.developer { d ->
          d.id.set("autonomousapps")
          d.name.set("Tony Robalik")
        }
      }
      scm {
        it.connection.set("scm:git:git://github.com/autonomousapps/dependency-analysis-android-gradle-plugin.git")
        it.developerConnection.set("scm:git:ssh://github.com/autonomousapps/dependency-analysis-android-gradle-plugin.git")
        it.url.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
      }
    }
    configure.execute(pom)
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
