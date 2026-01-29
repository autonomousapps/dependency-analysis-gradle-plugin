// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
plugins {
  id("build-logic.lib.java")
}

version = "${libs.versions.kotlineditor.core.get()}.0"

dagp {
  version(version)
  pom {
    name.set("KotlinEditor-Shaded")
    description.set("Shaded version of KotlinEditor")
    inceptionYear.set("2024")
  }
}

dependencies {
  runtimeOnly(libs.antlr.runtime)
  runtimeOnly(libs.kotlin.editor.core)
  runtimeOnly(libs.kotlin.editor.grammar)
}

// Excluding icu4j because it bloats artifact size significantly
configurations.runtimeClasspath {
  exclude(group = "com.ibm.icu", module = "icu4j")
}

tasks.shadowJar {
  relocate("org.antlr", "com.autonomousapps.internal.antlr")
  relocate("org.glassfish.json", "com.autonomousapps.internal.glassfish.json")
  relocate("javax.json", "com.autonomousapps.internal.javax.json")
  relocate("org.abego.treelayout", "com.autonomousapps.internal.abego.treelayout")
  relocate("org.stringtemplate.v4", "com.autonomousapps.internal.stringtemplate.v4")

  // Directly from KotlinEditor
  relocate("com.squareup.cash.grammar", "com.autonomousapps.internal.squareup.cash.grammar")
  relocate("cash.grammar", "com.autonomousapps.internal.cash.grammar")

  dependencies {
    // Don't bundle Kotlin or other Jetbrains dependencies
    exclude {
      it.moduleGroup.startsWith("org.jetbrains")
    }
    // Don't bundle in emoji support
    exclude {
      it.moduleGroup == "com.ibm.icu"
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
