// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.convention

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.publish.maven.MavenPom

public abstract class DagpExtension(
  private val project: Project,
  private val mavenPublish: MavenPublishBaseExtension,
) {

  private val pomConfigured: Property<Boolean> = project.objects.property(Boolean::class.java)

  internal val publishedVersion: Property<String> = project.objects.property(String::class.java)
  internal val isSnapshot: Provider<Boolean> = publishedVersion.map {
    it.endsWith("SNAPSHOT")
  }

  public fun version(version: Any?) {
    if (version !is String) {
      throw InvalidUserDataException("version must be a string. Was ${version?.javaClass?.canonicalName}")
    }
    publishedVersion.set(version)
    publishedVersion.disallowChanges()
    setupPublishingRepo()
  }

  public fun pom(configure: Action<MavenPom>) {
    pomConfigured.set(true)
    pomConfigured.disallowChanges()

    mavenPublish.pom(configure)
  }

  private fun setupPublishingRepo() {
    // TODO(tsr): delete this commented-out line once we're sure it all works
    // mavenPublish.publishToMavenCentral()
    mavenPublish.publishToMavenCentral(automaticRelease = true)

    project.tasks.named("publishToMavenCentral") { t ->
      t.notCompatibleWithConfigurationCache("Cannot serialize object of type DefaultProject")
      t.inputs.property("is-snapshot", isSnapshot)

      t.doLast {
        if (isSnapshot.get()) {
          t.logger.quiet("Browse files at https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/com/autonomousapps/")
        } else {
          t.logger.quiet(
            "After publishing to Central, visit https://central.sonatype.com/publishing/deployments to finish publishing the deployment"
          )
        }
      }
    }

    mavenPublish.signAllPublications()
    mavenPublish.pom { pom ->
      pom.url.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
      pom.licenses {
        it.license { l ->
          l.name.set("The Apache License, Version 2.0")
          l.url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
        }
      }
      pom.developers {
        it.developer { d ->
          d.id.set("autonomousapps")
          d.name.set("Tony Robalik")
        }
      }
      pom.scm {
        it.connection.set("scm:git:git://github.com/autonomousapps/dependency-analysis-android-gradle-plugin.git")
        it.developerConnection.set(
          "scm:git:ssh://github.com/autonomousapps/dependency-analysis-android-gradle-plugin.git"
        )
        it.url.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
      }
    }

    // TODO(tsr): update URL
    //val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
    //val snapshotsRepoUrl = "https://central.sonatype.com/repository/maven-snapshots/"
  }

  internal companion object {
    fun of(project: Project): DagpExtension = project.extensions.create(
      "dagp",
      DagpExtension::class.java,
      project,
      project.extensions.getByType(MavenPublishBaseExtension::class.java),
    )
  }
}
