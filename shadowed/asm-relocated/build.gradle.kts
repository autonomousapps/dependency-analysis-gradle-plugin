// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
plugins {
  `java-library`
  id("com.github.johnrengelman.shadow")
  id("convention")
  // This project doesn't need Kotlin, but it is now applied thanks to `convention`. problem?
}

version = "9.6.0.1"

val isSnapshot = version.toString().endsWith("SNAPSHOT", true)
val VERSION_ASM = "9.6"

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

tasks.jar {
  // Change the classifier of the original 'jar' task so that it does not overlap with the 'shadowJar' task
  archiveClassifier.set("plain")
}

tasks.shadowJar {
  archiveClassifier.set("")

  relocate("org.objectweb.asm", "com.autonomousapps.internal.asm")

  dependencies {
    // Don't bundle Kotlin or other Jetbrains dependencies
    exclude {
      it.moduleGroup.startsWith("org.jetbrains")
    }
  }
}

tasks.assemble {
  dependsOn(tasks.shadowJar)
}

val javaComponent = components["java"] as AdhocComponentWithVariants
listOf("apiElements", "runtimeElements").forEach { unpublishable ->
  // Hide the un-shadowed variants in local consumption
  configurations[unpublishable].isCanBeConsumed = false
  // Hide the un-shadowed variants in publishing
  javaComponent.withVariantsFromConfiguration(configurations[unpublishable]) { skip() }
}
