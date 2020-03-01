@file:Suppress("UnstableApiUsage")

import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation

plugins {
    `java-library`
    antlr
    id("com.github.johnrengelman.shadow") version "5.2.0"
    `maven-publish`
}

repositories {
    jcenter()
}

group = "com.autonomousapps"
val antlrVersion: String by rootProject.extra // e.g., 4.8
val internalAntlrVersion: String by rootProject.extra // e.g., 4.8.0

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

    withJavadocJar()
    withSourcesJar()
}

// https://docs.gradle.org/current/userguide/antlr_plugin.html
// https://discuss.gradle.org/t/using-gradle-2-10s-antlr-plugin-to-import-an-antlr-4-lexer-grammar-into-another-grammar/14970/6
tasks.generateGrammarSource {
    /*
     * Ignore implied package structure for .g4 files and instead use this for all generated source.
     */
    val pkg = "com.autonomousapps.internal.grammar"
    val dir = pkg.replace(".", "/")
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

// Publish with `./gradlew antlr:publishShadowPublicationToMavenRepository`
publishing {
    publications {
        create<MavenPublication>("shadow") {
            groupId = "autonomousapps"
            artifactId = "antlr"
            version = internalAntlrVersion

            //from components.java
            project.shadow.component(this)
        }
    }
    repositories {
        maven {
            url = uri("$buildDir/repo")
        }
    }
}

dependencies {
    antlr("org.antlr:antlr4:$antlrVersion")
    implementation("org.antlr:antlr4-runtime:$antlrVersion")
}

//jar.enabled = false

tasks.register<ConfigureShadowRelocation>("relocateShadowJar") {
    target = tasks.shadowJar.get()
}

tasks.shadowJar {
    dependsOn(tasks.getByName("relocateShadowJar"))
    archiveClassifier.set("")
    relocate("org.antlr", "com.autonomousapps.internal.antlr")
}
