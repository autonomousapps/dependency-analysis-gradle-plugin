plugins {
  kotlin("jvm")
}

group = "com.autonomousapps"
version = "0.1"

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt()))
  }
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
