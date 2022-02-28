import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  id("org.jetbrains.dokka") version "0.10.0"
  `maven-publish`
  signing
}

repositories {
  mavenCentral()
}

group = "com.autonomousapps"
version = "0.1"

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
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

tasks.dokka {
  outputFormat = "html"
  outputDirectory = "$buildDir/javadoc"
}

val dokkaJar by tasks.registering(Jar::class) {
  group = JavaBasePlugin.DOCUMENTATION_GROUP
  description = "Assembles Kotlin docs with Dokka"
  archiveClassifier.set("javadoc")
  from(tasks.dokka)
}

publishing {
  publications {
    create<MavenPublication>("kit") {
      from(components["java"])
      artifact(dokkaJar.get())
      signing.sign(this)
    }
  }
  repositories {
    maven {
      url = uri("$buildDir/repository")
    }
  }
}

tasks.withType<Sign>().configureEach {
  onlyIf {
    val isNotSnapshot = !version.toString().endsWith("SNAPSHOT")
    isNotSnapshot && (findProperty("signing.keyId") != null)
  }
}
