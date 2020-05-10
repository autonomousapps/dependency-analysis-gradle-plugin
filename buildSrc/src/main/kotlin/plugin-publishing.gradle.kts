@file:Suppress("UnstableApiUsage")

import org.gradle.api.publish.maven.MavenPom

plugins {
  `java-gradle-plugin`
  `maven-publish`
  id("com.gradle.plugin-publish")
  signing
}

val VERSION: String by project

java {
  withJavadocJar()
  withSourcesJar()
}

gradlePlugin {
  plugins {
    create("dependencyAnalysisPlugin") {
      id = "com.autonomousapps.dependency-analysis"
      implementationClass = "com.autonomousapps.DependencyAnalysisPlugin"
    }
  }
}

// For publishing to the Gradle Plugin Portal
// https://plugins.gradle.org/docs/publish-plugin
pluginBundle {
  website = "https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin"
  vcsUrl = "https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin"

  description = "A plugin to report mis-used dependencies in your Android project"

  (plugins) {
    "dependencyAnalysisPlugin" {
      displayName = "Android Dependency Analysis Gradle Plugin"
      tags = listOf("android", "dependencies")
    }
  }

  mavenCoordinates {
    groupId = "com.autonomousapps"
    artifactId = "dependency-analysis-gradle-plugin"
  }
}

fun secret(name: String): String? = (project.properties[name] ?: System.getenv(name))?.toString()

// For publishing to other repositories
publishing {
  publications {
    afterEvaluate {
      // Must be in afterEvaluate. Trying to be clever with publications.withType.matching results in malformed pom files
      named<MavenPublication>("dependencyAnalysisPluginPluginMarkerMaven") {
        configurePom(pom)
        signing.sign(this)
      }
    }

    create<MavenPublication>("plugin") {
      from(components["java"])
      configurePom(pom)
      signing.sign(this)

      versionMapping {
        usage("java-api") {
          fromResolutionOf("runtimeClasspath")
        }
        usage("java-runtime") {
          fromResolutionResult()
        }
      }
    }
    repositories {
      val sonatypeUsername = secret("sonatypeUsername")
      val sonatypePassword = secret("sonatypePassword")
      if (sonatypeUsername != null && sonatypePassword != null) {
        maven {
          name = "sonatype"

          val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
          val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
          url = uri(if (VERSION.endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)

          credentials {
            username = sonatypeUsername
            password = sonatypePassword
          }
        }
      }
    }
  }
}

fun configurePom(pom: MavenPom) {
  pom.apply {
    name.set("Dependency Analysis Gradle Plugin")
    description.set("Analyzes dependency usage in Android and Java/Kotlin projects")
    url.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
    inceptionYear.set("2019")
    licenses {
      license {
        name.set("The Apache License, Version 2.0")
        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
      }
    }
    developers {
      developer {
        id.set("autonomousapps")
        name.set("Tony Robalik")
      }
    }
    scm {
      connection.set("scm:git:git://github.com/autonomousapps/dependency-analysis-android-gradle-plugin.git")
      developerConnection.set("scm:git:ssh://github.com/autonomousapps/dependency-analysis-android-gradle-plugin.git")
      url.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
    }
  }
}

val theDescription =
  "Publishes plugin marker and plugin artifacts to Maven Central (${if (VERSION.endsWith("SNAPSHOT")) "snapshots" else "staging"})"

val publishToMavenCentral = tasks.register("publishToMavenCentral") {
  group = "publishing"
  description = theDescription

  dependsOn(
    "publishDependencyAnalysisPluginPluginMarkerMavenPublicationToSonatypeRepository",
    "publishPluginPublicationToSonatypeRepository"
  )
  doLast {
    if (VERSION.endsWith("SNAPSHOT")) {
      logger.quiet("Browse files at https://oss.sonatype.org/content/repositories/snapshots/com/autonomousapps")
    } else {
      logger.quiet("After publishing to Sonatype, visit https://oss.sonatype.org to close and release from staging")
    }
  }
}

tasks.withType<Sign>().configureEach {
  onlyIf {
    val isNotSnapshot = !VERSION.endsWith("SNAPSHOT")
    isNotSnapshot && gradle.taskGraph.hasTask(publishToMavenCentral.get())
  }
}
