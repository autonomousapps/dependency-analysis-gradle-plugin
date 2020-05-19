plugins {
  kotlin("jvm")
  id("org.jetbrains.dokka") version "0.10.0"
  `maven-publish`
  signing
}

repositories {
  jcenter()
}

group = "com.autonomousapps"
version = "0.1"

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  implementation(kotlin("stdlib"))
//  implementation(gradleTestKit())
//  implementation("org.spockframework:spock-core:1.3-groovy-2.5") {
//    exclude(module = "groovy-all")
//    because("For Spock tests")
//  }
}

tasks.withType<GroovyCompile>().configureEach {
  options.isIncremental = true
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
    isNotSnapshot //&& gradle.taskGraph.hasTask(publishToMavenCentral.get())
  }
}

