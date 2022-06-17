plugins {
  `java-gradle-plugin`
  id("org.jetbrains.kotlin.jvm")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
  }
}

gradlePlugin {
  plugins {
    create("build-logic") {
      id = "convention"
      implementationClass = "com.autonomousapps.convention.ConventionPlugin"
    }
  }
}

dependencies {
  implementation(enforcedPlatform(libs.kotlin.bom))

  implementation("com.gradle.plugin-publish:com.gradle.plugin-publish.gradle.plugin:0.11.0") {
    because("For extending Gradle Plugin-Publish Plugin functionality")
  }
  implementation(libs.okhttp3) {
    because("Closing and releasing Sonatype Nexus staging repo")
  }
  implementation(libs.retrofit.core) {
    because("Closing and releasing Sonatype Nexus staging repo")
  }
  implementation(libs.retrofit.converter.moshi) {
    because("Closing and releasing Sonatype Nexus staging repo")
  }

  implementation(libs.moshi.core) {
    because("Closing and releasing Sonatype Nexus staging repo")
  }
  implementation(libs.moshi.kotlin) {
    because("Closing and releasing Sonatype Nexus staging repo")
  }
}
