plugins {
  id("org.jetbrains.kotlin.jvm")
  id("convention")
  id("com.autonomousapps.dependency-analysis")
}

group = "com.autonomousapps"
version = "0.2"

kotlin {
  explicitApi()
}

dagp {
  version(version)
  pom {
    name.set("Graph Support Library")
    description.set("A graph support library for the JVM")
    url.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
    inceptionYear.set("2022")
  }
  publishTaskDescription("Publishes to Maven Central and promotes.")
}

// We only use the Jupiter platform (JUnit 5)
configurations.all {
  exclude(mapOf("group" to "junit", "module" to "junit"))
  exclude(mapOf("group" to "org.junit.vintage", "module" to "junit-vintage-engine"))
}

dependencies {
  api(libs.guava) {
    because("Graphs")
  }

  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.api)
  testImplementation(libs.truth)
  testRuntimeOnly(libs.junit.engine)

  testImplementation(libs.truth)
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}
