package com.autonomousapps.convention

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

public abstract class LibJavaConventionPlugin : Plugin<Project> {

  override fun apply(target: Project): Unit = target.run {
    pluginManager.run {
      apply("java-library")
      apply("com.gradleup.shadow")
    }
    BaseConventionPlugin(this).configure()

    configurePublishing()
    configureShadowJar()
  }

  private fun Project.configurePublishing() {
    extensions.getByType(MavenPublishBaseExtension::class.java).run {
      configure(
        JavaLibrary(
          javadocJar = JavadocJar.Javadoc(),
          sourcesJar = true,
        )
      )
    }
  }

  private fun Project.configureShadowJar() {
    tasks.named("jar", Jar::class.java) { t ->
      // Change the classifier of the original 'jar' task so that it does not overlap with the 'shadowJar' task
      t.archiveClassifier.set("plain")
    }
    val shadowJar = tasks.named("shadowJar", ShadowJar::class.java) { t ->
      t.archiveClassifier.set("")
    }
    tasks.named("assemble") { t ->
      t.dependsOn(shadowJar)
    }
  }
}
