// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.parse

import com.autonomousapps.internal.advice.AdvicePrinter
import com.autonomousapps.internal.advice.DslKind
import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

internal class GradleBuildScriptDependenciesRewriterTest {

  @TempDir
  lateinit var dir: Path

  private fun Path.writeText(text: String): Path = Files.writeString(this, text)

  @Test fun `can update dependencies`() {
    // Given
    val sourceFile = dir.resolve("build.gradle")
    sourceFile.writeText(
      """
        import foo
        import static bar;

        plugins {
          id 'foo'
        }
        
        repositories {	
          google()	
          mavenCentral()	
        }
              
        apply plugin: 'bar'
        ext.magic = 42
              
        android {
          whatever
        }
              
        dependencies {
          implementation 'heart:of-gold:1.+'
          api project(':marvin')
              
          testImplementation('pan-galactic:gargle-blaster:2.0-SNAPSHOT') {
            because "life's too short not to"
          }
        }
              
        println 'hello, world!'
      """.trimIndent()
    )
    val advice = setOf(
      Advice.ofChange(Coordinates.of(":marvin"), "api", "compileOnly"),
      Advice.ofRemove(Coordinates.of("pan-galactic:gargle-blaster:2.0-SNAPSHOT"), "testImplementation"),
      Advice.ofAdd(Coordinates.of(":sad-robot"), "runtimeOnly"),
    )

    // When
    val parser = GradleBuildScriptDependenciesRewriter.newRewriter(
      sourceFile,
      advice,
      AdvicePrinter(DslKind.GROOVY),
    )

    // Then
    assertThat(parser.rewritten().trimmedLines()).containsExactlyElementsIn(
      """
        import foo
        import static bar;

        plugins {
          id 'foo'
        }
        
        repositories {
          google()
          mavenCentral()
        }
                
        apply plugin: 'bar'
        ext.magic = 42
                
        android {
          whatever
        }
        
        dependencies {
          implementation 'heart:of-gold:1.+'
          compileOnly project(':marvin')
          runtimeOnly project(':sad-robot')
        }
                
        println 'hello, world!'
      """.trimIndent().trimmedLines()
    )
  }

  @Test fun `can update dependencies with dependencyMap`() {
    // Given
    val sourceFile = dir.resolve("build.gradle")
    sourceFile.writeText(
      """
        import foo
        import static bar;

        plugins {
          id 'foo'
        }
              
        apply plugin: 'bar'
        ext.magic = 42
              
        android {
          whatever
        }
              
        dependencies {
          implementation 'heart:of-gold:1.+'
          api project(':marvin')
              
          testImplementation('pan-galactic:gargle-blaster:2.0-SNAPSHOT') {
            because "life's too short not to"
          }
        }
              
        println 'hello, world!'
      """.trimIndent()
    )
    val advice = setOf(
      Advice.ofChange(Coordinates.of(":marvin"), "api", "compileOnly"),
      Advice.ofRemove(Coordinates.of("pan-galactic:gargle-blaster:2.0-SNAPSHOT"), "testImplementation"),
      Advice.ofAdd(Coordinates.of(":sad-robot"), "runtimeOnly"),
      Advice.ofAdd(Coordinates.of("magrathea:asleep:1000000"), "implementation"),
    )

    // When
    val parser = GradleBuildScriptDependenciesRewriter.newRewriter(
      sourceFile,
      advice,
      AdvicePrinter(
        dslKind = DslKind.GROOVY,
        dependencyMap = {
          when (it) {
            ":sad-robot" -> "':depressed-robot'"
            "magrathea:asleep:1000000" -> "deps.magrathea"
            else -> it
          }
        }
      ),
      reversedDependencyMap = {
        when (it) {
          "':depressed-robot'" -> ":sad-robot"
          "deps.magrathea" -> "magrathea:asleep:1000000"
          else -> it
        }
      }
    )

    // Then
    assertThat(parser.rewritten().trimmedLines()).containsExactlyElementsIn(
      """
        import foo
        import static bar;
        
        plugins {
          id 'foo'
        }
                        
        apply plugin: 'bar'
        ext.magic = 42
                
        android {
          whatever
        }
        
        dependencies {
          implementation 'heart:of-gold:1.+'
          compileOnly project(':marvin')
          implementation deps.magrathea
          runtimeOnly project(':depressed-robot')
        }
                
        println 'hello, world!'
      """.trimIndent().trimmedLines()
    )
  }

  @Test fun `ignores buildscript dependencies`() {
    // Given
    val sourceFile = dir.resolve("build.gradle")
    sourceFile.writeText(
      """
        import foo
        import static bar;

        // see https://github.com/dropbox/dropbox-sdk-java/blob/master/build.gradle
        buildscript {
          ext.foo = 'ba/r'
          fizzle()
          repositories {
            google()
            maven { url 'https://plugins.gradle.org/m2/' }
          }
          dependencies {
            classpath 'com.github.ben-manes:gradle-versions-plugin:0.27.0'
            classpath files('gradle/dropbox-pem-converter-plugin')
          }
        }
        
        plugins {
          id 'foo'
        }
        
        repositories {	
          google()	
          mavenCentral()	
        }
              
        apply plugin: 'bar'
        ext.magic = 42
              
        android {
          whatever
        }
              
        dependencies {
          implementation 'heart:of-gold:1.+'
          api project(':marvin')
              
          testImplementation('pan-galactic:gargle-blaster:2.0-SNAPSHOT') {
            because "life's too short not to"
          }
        }
              
        println 'hello, world!'
      """.trimIndent()
    )
    val advice = setOf(
      Advice.ofChange(Coordinates.of(":marvin"), "api", "compileOnly"),
      Advice.ofRemove(Coordinates.of("pan-galactic:gargle-blaster:2.0-SNAPSHOT"), "testImplementation"),
      Advice.ofAdd(Coordinates.of(":sad-robot"), "runtimeOnly"),
    )

    // When
    val parser = GradleBuildScriptDependenciesRewriter.newRewriter(
      sourceFile,
      advice,
      AdvicePrinter(DslKind.GROOVY),
    )

    // Then
    assertThat(parser.rewritten().trimmedLines()).containsExactlyElementsIn(
      """
        import foo
        import static bar;

        // see https://github.com/dropbox/dropbox-sdk-java/blob/master/build.gradle
        buildscript {
          ext.foo = 'ba/r'
          fizzle()
          repositories {
            google()
            maven { url 'https://plugins.gradle.org/m2/' }
          }
          dependencies {
            classpath 'com.github.ben-manes:gradle-versions-plugin:0.27.0'
            classpath files('gradle/dropbox-pem-converter-plugin')
          }
        }
        
        plugins {
          id 'foo'
        }
        
        repositories {
          google()
          mavenCentral()
        }
                
        apply plugin: 'bar'
        ext.magic = 42
                
        android {
          whatever
        }
        
        dependencies {
          implementation 'heart:of-gold:1.+'
          compileOnly project(':marvin')
          runtimeOnly project(':sad-robot')
        }
                
        println 'hello, world!'
      """.trimIndent().trimmedLines()
    )
  }

  @Test fun `can handle testFixtures`() {
    // Given
    val sourceFile = dir.resolve("build.gradle")
    sourceFile.writeText(
      """
        dependencies {
          implementation 'heart:of-gold:1.+'
          implementation testFixtures(project(":foo"))
        }
      """.trimIndent()
    )

    // When
    val parser = GradleBuildScriptDependenciesRewriter.newRewriter(
      sourceFile,
      emptySet(),
      AdvicePrinter(DslKind.GROOVY),
    )

    // Then
    assertThat(parser.rewritten().trimmedLines()).containsExactlyElementsIn(
      """
        dependencies {
          implementation 'heart:of-gold:1.+'
          implementation testFixtures(project(":foo"))
        }
      """.trimIndent().trimmedLines()
    ).inOrder()
  }

  @Test fun `can add dependencies to build script that didn't have a dependencies block`() {
    // Given
    val sourceFile = dir.resolve("build.gradle")
    sourceFile.writeText(
      """
        import foo
        import static bar;

        plugins {
          id 'foo'
        }
        
        repositories {	
          google()	
          mavenCentral()	
        }
              
        apply plugin: 'bar'
        ext.magic = 42
              
        android {
          whatever
        }
      """.trimIndent()
    )
    val advice = setOf(
      Advice.ofChange(Coordinates.of(":marvin"), "api", "compileOnly"),
      Advice.ofRemove(Coordinates.of("pan-galactic:gargle-blaster:2.0-SNAPSHOT"), "testImplementation"),
      Advice.ofAdd(Coordinates.of(":sad-robot"), "runtimeOnly"),
    )

    // When
    val parser = GradleBuildScriptDependenciesRewriter.newRewriter(
      sourceFile,
      advice,
      AdvicePrinter(DslKind.GROOVY),
    )

    // Then
    assertThat(parser.rewritten().trimmedLines()).containsExactlyElementsIn(
      """
        import foo
        import static bar;

        plugins {
          id 'foo'
        }
        
        repositories {
          google()
          mavenCentral()
        }
                
        apply plugin: 'bar'
        ext.magic = 42
                
        android {
          whatever
        }
        
        dependencies {
          runtimeOnly project(':sad-robot')
        }
      """.trimIndent().trimmedLines()
    )
  }

  @Test fun `only removes dependencies on expected configuration`() {
    // Given
    val sourceFile = dir.resolve("build.gradle")
    sourceFile.writeText(
      """
        import foo
        import static bar;

        plugins {
          id 'foo'
        }
        
        repositories {	
          google()	
          mavenCentral()	
        }
              
        apply plugin: 'bar'
        ext.magic = 42
              
        android {
          whatever
        }
              
        dependencies {
          implementation 'heart:of-gold:1.+'
          api project(':marvin')
              
          testImplementation('pan-galactic:gargle-blaster:2.0-SNAPSHOT') {
            because "life's too short not to"
          }
        }
              
        println 'hello, world!'
      """.trimIndent()
    )
    val advice = setOf(
      Advice.ofChange(Coordinates.of(":marvin"), "api", "compileOnly"),
      Advice.ofRemove(Coordinates.of("pan-galactic:gargle-blaster:2.0-SNAPSHOT"), "implementation"),
      Advice.ofAdd(Coordinates.of(":sad-robot"), "runtimeOnly"),
    )

    // When
    val parser = GradleBuildScriptDependenciesRewriter.newRewriter(
      sourceFile,
      advice,
      AdvicePrinter(DslKind.GROOVY),
    )

    // Then
    assertThat(parser.rewritten().trimmedLines()).containsExactlyElementsIn(
      """
        import foo
        import static bar;

        plugins {
          id 'foo'
        }
        
        repositories {
          google()
          mavenCentral()
        }
                
        apply plugin: 'bar'
        ext.magic = 42
                
        android {
          whatever
        }
        
        dependencies {
          implementation 'heart:of-gold:1.+'
          compileOnly project(':marvin')
          runtimeOnly project(':sad-robot')
          
          testImplementation('pan-galactic:gargle-blaster:2.0-SNAPSHOT') {
            because "life's too short not to"
          }
        }
                
        println 'hello, world!'
      """.trimIndent().trimmedLines()
    )
  }

  private fun String.trimmedLines() = lines().map { it.trimEnd() }
}
