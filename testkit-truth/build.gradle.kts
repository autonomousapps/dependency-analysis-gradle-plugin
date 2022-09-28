plugins {
  kotlin("jvm")
  id("org.jetbrains.dokka")
  id("convention")
}

group = "com.autonomousapps"
version = "1.2-SNAPSHOT"

val isSnapshot = version.toString().endsWith("SNAPSHOT", true)

kotlin {
  explicitApi()
}

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

  api(kotlin("stdlib"))
  api(gradleTestKit())
  api(libs.truth)

  dokkaHtmlPlugin(libs.kotlin.dokka)
}

val dokkaJavadoc = tasks.named("dokkaJavadoc")
// This task is added by Gradle when we use java.withJavadocJar()
val javadocJar = tasks.named<Jar>("javadocJar") {
  from(dokkaJavadoc)
}

dagp {
  version(version)
  pom {
    name.set("TestKit Truth")
    description.set("A Truth extension for Gradle TestKit")
    url.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
    inceptionYear.set("2022")
  }
  publishTaskDescription("Publishes to Maven Central and promotes.")
}

// TODO I think this is unused...
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
