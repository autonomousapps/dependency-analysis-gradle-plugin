import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation

plugins {
  `java-library`
  id("com.github.johnrengelman.shadow")
  `maven-publish`
  signing
}

group = "com.autonomousapps"
version = "9.2.0.1"

val isSnapshot = version.toString().endsWith("SNAPSHOT", true)

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt()))
  }

  withJavadocJar()
  withSourcesJar()
}

val VERSION_ASM = "9.2"

dependencies {
  implementation("org.ow2.asm:asm:$VERSION_ASM")
  implementation("org.ow2.asm:asm-tree:$VERSION_ASM")
}

configurations.all {
  resolutionStrategy {
    eachDependency {
      if (requested.group == "org.ow2.asm") {
        useVersion(VERSION_ASM)
      }
    }
  }
}

val relocateShadowJar = tasks.register<ConfigureShadowRelocation>("relocateShadowJar") {
  target = tasks.shadowJar.get()
}

tasks.shadowJar {
  dependsOn(relocateShadowJar)
  archiveClassifier.set("")
  relocate("org.objectweb.asm", "com.autonomousapps.internal.asm")
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
    name.set("asm, relocated")
    description.set("asm, relocated")
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
      logger.quiet("Browse files at https://oss.sonatype.org/content/repositories/snapshots/com/autonomousapps/asm-relocated/")
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
