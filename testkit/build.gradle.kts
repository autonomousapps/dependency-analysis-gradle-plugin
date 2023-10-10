plugins {
  kotlin("jvm")
  id("convention")
}

group = "com.autonomousapps"
version = "0.1-SNAPSHOT"

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

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

  api(kotlin("stdlib"))
  api(gradleTestKit())
  api(libs.truth)
  api(project(":testkit-truth"))

  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.api)
  testRuntimeOnly(libs.junit.engine)
}
