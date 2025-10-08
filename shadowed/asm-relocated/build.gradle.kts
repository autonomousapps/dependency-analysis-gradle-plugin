// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
plugins {
  id("build-logic.lib.java")
}

version = "9.9.0"
val versionAsm = "9.9"

dagp {
  version(version)
  pom {
    name.set("asm, relocated")
    description.set("asm, relocated")
    inceptionYear.set("2022")
  }
}

dependencies {
  runtimeOnly("org.ow2.asm:asm:$versionAsm")
  runtimeOnly("org.ow2.asm:asm-tree:$versionAsm")
}

configurations.all {
  resolutionStrategy {
    eachDependency {
      if (requested.group == "org.ow2.asm") {
        useVersion(versionAsm)
      }
    }
  }
}

tasks.shadowJar {
  relocate("org.objectweb.asm", "com.autonomousapps.internal.asm")

  dependencies {
    // Don't bundle Kotlin or other Jetbrains dependencies
    exclude {
      it.moduleGroup.startsWith("org.jetbrains")
    }
  }
}

val javaComponent = components["java"] as AdhocComponentWithVariants
listOf("apiElements", "runtimeElements")
  .map { configurations[it] }
  .forEach { unpublishable ->
    // Hide the un-shadowed variants in local consumption, by mangling their attributes
    unpublishable.attributes {
      attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named("DO_NOT_USE"))
    }

    // Hide the un-shadowed variants in publishing
    javaComponent.withVariantsFromConfiguration(unpublishable) { skip() }
  }
