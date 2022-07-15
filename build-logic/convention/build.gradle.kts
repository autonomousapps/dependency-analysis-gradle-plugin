plugins {
  `java-gradle-plugin`
  id("org.jetbrains.kotlin.jvm")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
  }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  kotlinOptions {
    jvmTarget = libs.versions.java.get()
    freeCompilerArgs = listOf("-Xsam-conversions=class")
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
  implementation(platform(libs.kotlin.bom))

  implementation(libs.gradle.publish.plugin) {
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
