plugins {
  id("convention")
  id("org.jetbrains.dokka")
  id("com.autonomousapps.dependency-analysis")
  id("com.autonomousapps.testkit")
}

version = "0.12"

dagp {
  version(version)
  pom {
    name.set("TestKit")
    description.set("A DSL for building test fixtures with Gradle TestKit")
    url.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
    inceptionYear.set("2023")
  }
  publishTaskDescription("Publishes to Maven Central and promotes.")
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

  implementation(libs.truth)

  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.api)
  testRuntimeOnly(libs.junit.engine)

  dokkaHtmlPlugin(libs.kotlin.dokka)
}
