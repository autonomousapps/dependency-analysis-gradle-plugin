import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation

plugins {
  `java-library`
  id("com.github.johnrengelman.shadow")
  id("convention")
}

group = "com.autonomousapps"
version = "9.2.0.1"

val isSnapshot = version.toString().endsWith("SNAPSHOT", true)
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

dagp {
  version(version)
  pom {
    name.set("asm, relocated")
    description.set("asm, relocated")
    inceptionYear.set("2022")
  }
  publishTaskDescription("Publishes to Maven Central and promotes.")
}

val relocateShadowJar = tasks.register<ConfigureShadowRelocation>("relocateShadowJar") {
  notCompatibleWithConfigurationCache("Shadow plugin is incompatible")
  target = tasks.shadowJar.get()
}

tasks.shadowJar {
  dependsOn(relocateShadowJar)
  archiveClassifier.set("")
  relocate("org.objectweb.asm", "com.autonomousapps.internal.asm")
}
