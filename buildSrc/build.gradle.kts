plugins {
  `kotlin-dsl`
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
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
