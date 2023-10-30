@file:Suppress("UnstableApiUsage")

plugins {
  id("java-gradle-plugin")
  id("convention")
  id("org.jetbrains.dokka")
  id("com.autonomousapps.dependency-analysis")
}

version = "0.1-SNAPSHOT"

dagp {
  version(version)
  pom {
    name.set("Gradle TestKit Support Plugin (for plugins)")
    description.set("Make it less difficult to use Gradle TestKit to test your Gradle plugins")
    inceptionYear.set("2023")
  }
  publishTaskDescription(
    "Publishes plugin marker and plugin artifacts to Maven Central " +
      "(${if (version.toString().endsWith("SNAPSHOT")) "snapshots" else "staging"})"
  )
}

gradlePlugin {
  plugins {
    create("plugin") {
      id = "com.autonomousapps.testkit-plugin"
      implementationClass = "com.autonomousapps.GradleTestKitPlugin"

      displayName = "Gradle TestKit Support Plugin (for plugins)"
      description = "Make it less difficult to use Gradle TestKit to test your Gradle plugins"
      tags.set(listOf("testing"))
    }
    create("subprojects") {
      id = "com.autonomousapps.testkit-dependency"
      implementationClass = "com.autonomousapps.GradleTestKitSubPlugin"

      displayName = "Gradle TestKit Support Plugin (for plugin dependencies)"
      description = "Make it less difficult to use Gradle TestKit to test your Gradle plugins"
      tags.set(listOf("testing"))
    }
  }

  website.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
  vcsUrl.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
}

kotlin {
  explicitApi()
}

val dokkaJavadoc = tasks.named("dokkaJavadoc")
// This task is added by Gradle when we use java.withJavadocJar()
tasks.named<Jar>("javadocJar") {
  from(dokkaJavadoc)
}

// This task fails and is a dependency of javadocJar (which doesn't fail), probably because there's no Java? Just
// disable it.
tasks.named("javadoc") {
  enabled = false
}

dependencies {
  api(platform(libs.kotlin.bom))
  api(gradleTestKit())

  dokkaHtmlPlugin(libs.kotlin.dokka)
}
