// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
plugins {
  id("build-logic.lib.java")
  id("antlr")
  id("groovy")
}

version = "${libs.versions.antlr.base.get()}.0"

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
}

// Excluding icu4j because it bloats artifact size significantly
configurations.runtimeClasspath {
  exclude(group = "com.ibm.icu", module = "icu4j")
}

dependencies {
  antlr(libs.antlr.core)

  runtimeOnly(libs.antlr.runtime)
  runtimeOnly(libs.grammar)

  testImplementation(libs.groovy)
  testImplementation(libs.spock)
  testImplementation(libs.truth)

  testRuntimeOnly(libs.junit.engine)
  testRuntimeOnly(libs.junit.launcher)
}

// Antlr's tasks aren't wired into src dirs correctly. This workaround connects the task dependencies without also
// adding the generated files in a second time.
val emptyFileCollection = project.files()
sourceSets {
  main {
    java {
      srcDir(tasks.generateGrammarSource.map { emptyFileCollection })
    }
  }
  test {
    java {
      srcDir(tasks.generateTestGrammarSource.map { emptyFileCollection })
    }
  }
}

tasks.shadowJar {
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

tasks.sourcesJar {
  dependsOn(tasks.generateGrammarSource)
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
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
