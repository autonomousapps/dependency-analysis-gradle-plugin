package com.autonomousapps.convention

import com.autonomousapps.convention.ConventionPlugin.Companion.SONATYPE_REPO_NAME
import nexus.Credentials
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.tasks.TaskProvider

abstract class ConventionExtension(
  private val project: Project,
  private val publishing: PublishingExtension,
) {

  private val pomConfigured: Property<Boolean> = project.objects.property(Boolean::class.java)
  private val pTMCDescription: Property<String> = project.objects.property(String::class.java)
  private var publishToMavenCentral: TaskProvider<Task>? = null

  internal var pomConfiguration: Action<MavenPom>? = null
  internal val publishedVersion: Property<String> = project.objects.property(String::class.java)
  internal val isSnapshot: Provider<Boolean> = publishedVersion.map {
    it.endsWith("SNAPSHOT")
  }

  fun version(version: Any?) {
    if (version !is String) {
      throw InvalidUserDataException("version must be a string. Was ${version?.javaClass?.canonicalName}")
    }
    publishedVersion.set(version)
    publishedVersion.disallowChanges()
    setupPublishingRepo()
  }

  fun pom(configure: Action<MavenPom>) {
    pomConfigured.set(true)
    pomConfigured.disallowChanges()
    pomConfiguration = configure
  }

  fun publishTaskDescription(description: String) {
    pTMCDescription.set(description)
    pTMCDescription.disallowChanges()
    publishToMavenCentral?.configure {
      it.description = description
    }
  }

  internal fun setPublishToMavenCentral(task: TaskProvider<Task>) {
    publishToMavenCentral = task
    if (pTMCDescription.isPresent) {
      task.configure {
        it.description = pTMCDescription.get()
      }
    }
  }

  private fun setupPublishingRepo() {
    publishing.repositories { r ->
      val credentials = Credentials(project)
      val sonatypeUsername = credentials.username()
      val sonatypePassword = credentials.password()
      if (sonatypeUsername != null && sonatypePassword != null) {
        r.maven { a ->
          a.name = SONATYPE_REPO_NAME

          val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
          val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
          a.url = project.uri(if (isSnapshot.get()) snapshotsRepoUrl else releasesRepoUrl)

          a.credentials {
            it.username = sonatypeUsername
            it.password = sonatypePassword
          }
        }
      }
    }
  }

  internal companion object {
    fun of(project: Project): ConventionExtension = project.extensions.create(
      "dagp",
      ConventionExtension::class.java,
      project,
      project.extensions.getByType(PublishingExtension::class.java),
    )
  }
}
