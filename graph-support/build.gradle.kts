import org.jetbrains.kotlin.cli.common.toBooleanLenient

plugins {
  id("convention")
  id("com.autonomousapps.testkit-dependency")
  id("com.autonomousapps.dependency-analysis")
}

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

tasks.withType<Sign> {
  onlyIf("release environment") {
    // We currently don't support publishing from CI
    !providers.environmentVariable("CI")
      .getOrElse("false")
      .toBooleanLenient()!!
  }
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
