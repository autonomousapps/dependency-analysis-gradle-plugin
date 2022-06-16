@file:Suppress("UnstableApiUsage")

import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation

plugins {
  `java-library`
  antlr
  id("com.github.johnrengelman.shadow")
  groovy
  `maven-publish`
  signing
}

group = "com.autonomousapps"
version = "4.9.2"

val isSnapshot = version.toString().endsWith("SNAPSHOT", true)

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt()))
  }

  withJavadocJar()
  withSourcesJar()
}

// https://docs.gradle.org/current/userguide/antlr_plugin.html
// https://discuss.gradle.org/t/using-gradle-2-10s-antlr-plugin-to-import-an-antlr-4-lexer-grammar-into-another-grammar/14970/6
tasks.generateGrammarSource {
  /*
   * Ignore implied package structure for .g4 files and instead use this for all generated source.
   */
  val pkg = "com.autonomousapps.internal.grammar"
  val dir = pkg.replace('.', '/')
  outputDirectory = file("$buildDir/generated-src/antlr/main/$dir")
  arguments = arguments + listOf(
    // Specify the package declaration for generated Java source
    "-package", pkg,
    // Specify that generated Java source should go into the outputDirectory, regardless of package structure
    "-Xexact-output-dir",
    // Specify the location of "libs"; i.e., for grammars composed of multiple files
    "-lib", "src/main/antlr/$dir"
  )
}

dependencies {
  val antlrVersion = "4.9.2"
  antlr("org.antlr:antlr4:$antlrVersion")
  implementation("org.antlr:antlr4-runtime:$antlrVersion")

  testImplementation("org.spockframework:spock-core:2.1-groovy-3.0") {
    because("For Spock tests")
  }
  testImplementation("com.google.truth:truth:1.1.3") {
    because("Groovy's == behavior on Comparable classes is beyond stupid")
  }
}

val relocateShadowJar = tasks.register<ConfigureShadowRelocation>("relocateShadowJar") {
  target = tasks.shadowJar.get()
}

tasks.shadowJar {
  dependsOn(relocateShadowJar)
  archiveClassifier.set("")
  relocate("org.antlr", "com.autonomousapps.internal.antlr")
}

tasks.named<Jar>("sourcesJar") {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

publishing {
  publications {
    create<MavenPublication>("shadow") {
//      project.shadow.component(this)
      from(components["java"])
      configurePom(pom)
      signing.sign(this)
    }
  }
  repositories {
    maven {
      name = "local"
      url = uri("$buildDir/repo")
    }

    val credentials = nexus.Credentials(project)
    val sonatypeUsername = credentials.username()
    val sonatypePassword = credentials.password()
    if (sonatypeUsername != null && sonatypePassword != null) {
      maven {
        name = "sonatype"

        val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
        val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
        url = uri(if (isSnapshot) snapshotsRepoUrl else releasesRepoUrl)

        credentials {
          username = sonatypeUsername
          password = sonatypePassword
        }
      }
    }
  }
}

fun configurePom(pom: MavenPom) {
  pom.apply {
    name.set("Simple shaded JVM grammar")
    description.set("Simple shaded JVM grammar")
    url.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
    inceptionYear.set("2022")
    licenses {
      license {
        name.set("The Apache License, Version 2.0")
        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
      }
    }
    developers {
      developer {
        id.set("autonomousapps")
        name.set("Tony Robalik")
      }
    }
    scm {
      connection.set("scm:git:git://github.com/autonomousapps/dependency-analysis-android-gradle-plugin.git")
      developerConnection.set("scm:git:ssh://github.com/autonomousapps/dependency-analysis-android-gradle-plugin.git")
      url.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
    }
  }
}

val promoteTask = tasks.register<nexus.NexusPublishTask>("promote") {
  onlyIf { !isSnapshot }
}

val publishToMavenCentral = tasks.register("publishToMavenCentral") {
  group = "publishing"
  description = "Publishes to Maven Central and promotes."

  dependsOn("publishShadowPublicationToSonatypeRepository")
  finalizedBy(promoteTask)

  doLast {
    if (isSnapshot) {
      logger.quiet("Browse files at https://oss.sonatype.org/content/repositories/snapshots/com/autonomousapps/antlr/")
    } else {
      logger.quiet("After publishing to Sonatype, visit https://oss.sonatype.org to close and release from staging")
    }
  }
}

tasks.withType<Sign>().configureEach {
  onlyIf {
    !isSnapshot && gradle.taskGraph.hasTask(publishToMavenCentral.get())
  }
  doFirst {
    logger.quiet("Signing v$version")
  }
}
