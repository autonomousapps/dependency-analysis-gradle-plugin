import nexus.NexusPublishTask

plugins {
  kotlin("jvm")
  id("org.jetbrains.dokka")
  `maven-publish`
  signing
}

group = "com.autonomousapps"
version = "1.2-SNAPSHOT"

val isSnapshot = version.toString().endsWith("SNAPSHOT", true)

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
  withSourcesJar()
  withJavadocJar()
}

kotlin {
  explicitApi()
}

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

  api(kotlin("stdlib"))
  api(gradleTestKit())
  api("com.google.truth:truth:1.1.3")

  dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.5.31")
}

val dokkaJavadoc = tasks.named("dokkaJavadoc")
// This task is added by Gradle when we use java.withJavadocJar()
val javadocJar = tasks.named<Jar>("javadocJar") {
  from(dokkaJavadoc)
}

publishing {
  publications {
    create<MavenPublication>("truth") {
      from(components["java"])
      configurePom(pom)
      signing.sign(this)

      versionMapping {
        usage("java-api") {
          fromResolutionOf("runtimeClasspath")
        }
        usage("java-runtime") {
          fromResolutionResult()
        }
      }
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
    name.set("TestKit Truth")
    description.set("A Truth extension for Gradle TestKit")
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

val promoteTask = tasks.register<NexusPublishTask>("promote") {
  onlyIf { !isSnapshot }
}

val publishToMavenCentral = tasks.register("publishToMavenCentral") {
  group = "publishing"
  description = "Publishes to Maven Central and promotes."

  dependsOn("publishTruthPublicationToSonatypeRepository")
  finalizedBy(promoteTask)

  doLast {
    if (isSnapshot) {
      logger.quiet("Browse files at https://oss.sonatype.org/content/repositories/snapshots/com/autonomousapps/testkit-truth/")
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
