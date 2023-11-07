plugins {
  id("convention")
  id("org.jetbrains.dokka")
  id("com.autonomousapps.testkit")
}

version = "1.4-SNAPSHOT"

dagp {
  version(version)
  pom {
    name.set("TestKit Truth")
    description.set("A Truth extension for Gradle TestKit")
    url.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
    inceptionYear.set("2022")
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
  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

  api(kotlin("stdlib"))
  api(gradleTestKit())
  api(libs.truth)

  dokkaHtmlPlugin(libs.kotlin.dokka)
}
