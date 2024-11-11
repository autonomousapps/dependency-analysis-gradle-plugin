plugins {
  id("convention")
  id("com.github.johnrengelman.shadow")
}

version = "${libs.versions.kotlineditor.core.get()}.0-SNAPSHOT"

dagp {
  version(version)
  pom {
    name.set("KotlinEditor-Shaded")
    description.set("Shaded version of KotlinEditor")
    inceptionYear.set("2024")
  }
  publishTaskDescription("Publishes to Maven Central and promotes.")
}

dependencies {
  implementation(libs.kotlin.editor.core)
  implementation(libs.kotlin.editor.grammar)

  runtimeOnly(libs.antlr.runtime)
}

// Excluding icu4j because it bloats artifact size significantly
configurations.runtimeClasspath {
  exclude(group = "com.ibm.icu", module = "icu4j")
}

tasks.jar {
  // Change the classifier of the original 'jar' task so that it does not overlap with the 'shadowJar' task
  archiveClassifier.set("plain")
}

tasks.shadowJar {
  archiveClassifier.set("")

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
