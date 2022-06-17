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

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

  api(kotlin("stdlib"))
  api(gradleTestKit())
  api("com.google.truth:truth:1.1.3")
  api(project(":testkit-truth"))

  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}
