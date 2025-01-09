// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

plugins {
  `java-library`
  antlr
  id("com.gradleup.shadow")
  groovy
  id("convention")
  // This project doesn't need Kotlin, but it is now applied thanks to `convention`. problem?
}

version = "${libs.versions.antlr.base.get()}.6"

val isSnapshot = version.toString().endsWith("SNAPSHOT", true)

// https://docs.gradle.org/current/userguide/antlr_plugin.html
// https://discuss.gradle.org/t/using-gradle-2-10s-antlr-plugin-to-import-an-antlr-4-lexer-grammar-into-another-grammar/14970/6
/* Ignore implied package structure for .g4 files and instead use this for all generated source. */
val pkg = "com.autonomousapps.internal.grammar"
val dir = pkg.replace('.', '/')
val antlrSrc = "src/main/antlr/$dir"
tasks.generateGrammarSource {
  outputDirectory = file(layout.buildDirectory.dir("generated-src/antlr/main/$dir"))
  arguments = arguments + listOf(
    // Specify the package declaration for generated Java source
    "-package", pkg,
    // Specify that generated Java source should go into the outputDirectory, regardless of package structure
    "-Xexact-output-dir",
    // Specify the location of "libs"; i.e., for grammars composed of multiple files
    "-lib", antlrSrc
  )
}

dagp {
  version(version)
  pom {
    name.set("Simple shaded JVM grammar")
    description.set("Simple shaded JVM grammar")
    inceptionYear.set("2022")
  }
  publishTaskDescription("Publishes to Maven Central and promotes.")
}

// Excluding icu4j because it bloats artifact size significantly
configurations.runtimeClasspath {
  exclude(group = "com.ibm.icu", module = "icu4j")
}

dependencies {
  antlr(libs.antlr.core)
  runtimeOnly(libs.antlr.runtime)
  implementation(libs.grammar)

  testImplementation(libs.spock)
  testImplementation(libs.truth)
  testImplementation(libs.junit.api)
  testRuntimeOnly(libs.junit.engine)
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

tasks.named<Jar>("sourcesJar") {
  dependsOn(tasks.generateGrammarSource)
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
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
