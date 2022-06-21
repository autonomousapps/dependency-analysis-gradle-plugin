@file:Suppress("UnstableApiUsage")

import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation

plugins {
  `java-library`
  antlr
  id("com.github.johnrengelman.shadow")
  groovy
  id("convention")
}

group = "com.autonomousapps"
version = "4.9.2.1"

val isSnapshot = version.toString().endsWith("SNAPSHOT", true)

// https://docs.gradle.org/current/userguide/antlr_plugin.html
// https://discuss.gradle.org/t/using-gradle-2-10s-antlr-plugin-to-import-an-antlr-4-lexer-grammar-into-another-grammar/14970/6
tasks.generateGrammarSource {
  /*
   * Ignore implied package structure for .g4 files and instead use this for all generated source.
   */
  val pkg = "com.autonomousapps.internal.grammar"
  val dir = pkg.replace('.', '/')
  outputDirectory = file("$buildDir/generated-src/antlr/main/$dir")
  arguments = arguments + listOf(
    // Specify the package declaration for generated Java source
    "-package", pkg,
    // Specify that generated Java source should go into the outputDirectory, regardless of package structure
    "-Xexact-output-dir",
    // Specify the location of "libs"; i.e., for grammars composed of multiple files
    "-lib", "src/main/antlr/$dir"
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

dependencies {
  val antlrVersion = "4.9.2"
  antlr("org.antlr:antlr4:$antlrVersion")
  implementation("org.antlr:antlr4-runtime:$antlrVersion")

  testImplementation(libs.spock)
  testImplementation(libs.truth)
}

val relocateShadowJar = tasks.register<ConfigureShadowRelocation>("relocateShadowJar") {
  notCompatibleWithConfigurationCache("Shadow plugin is incompatible")
  target = tasks.shadowJar.get()
}

tasks.shadowJar {
  dependsOn(relocateShadowJar)
  archiveClassifier.set("")
  relocate("org.antlr", "com.autonomousapps.internal.antlr")
}

tasks.named<Jar>("sourcesJar") {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val javaComponent = components["java"] as AdhocComponentWithVariants
listOf("apiElements", "runtimeElements").forEach { unpublishable ->
  javaComponent.withVariantsFromConfiguration(configurations[unpublishable]) {
    skip()
  }
}
