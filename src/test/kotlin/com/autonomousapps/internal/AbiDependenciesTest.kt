package com.autonomousapps.internal

import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.kotlin.abiDependencies
import com.autonomousapps.internal.utils.mapToSet
import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import okio.buffer
import okio.sink
import okio.source
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

class AbiDependenciesTest {

  @get:Rule val temporaryFolder = TemporaryFolder()

  private val publicComponent = PublicComponent(
    dependency = Dependency("junit:junit"),
    classes = setOf("org.junit.rules.TemporaryFolder")
  )
  private val jarDependencies = listOf(
    Component(
      dependency = publicComponent.dependency,
      isTransitive = false,
      isCompileOnlyAnnotations = false,
      classes = publicComponent.classes,
      constantFields = emptyMap(),
      ktFiles = emptyList()
    )
  )

  private val dependencies = jarDependencies.mapToSet { it.dependency }

  private val simpleTestFile = SourceFile.kotlin("Test.kt", """
        package test
 
        import org.junit.Ignore
        import org.junit.rules.TemporaryFolder
        
        @Ignore
        class TestClass {
          fun newTemporaryFolder(): TemporaryFolder {
            error("Not real code")
          }
        }
      """)

  @Test
  fun noExclusions() {
    val jar = compile(simpleTestFile)

    val abiDependencies = abiDependencies(jar, jarDependencies, AbiExclusions.NONE)
    assertThat(abiDependencies).isEqualTo(setOf(publicComponent))
  }

  @Test
  fun excludeAnnotation() {
    val jar = compile(simpleTestFile)

    val abiDependencies = abiDependencies(
      jar,
      jarDependencies,
      AbiExclusions(annotationExclusions = setOf(".*\\.Ignore"))
    )
    assertThat(abiDependencies).isEmpty()
  }

  @Test
  fun excludePackage() {
    val jar = compile(simpleTestFile)

    val abiDependencies = abiDependencies(
      jar,
      jarDependencies,
      AbiExclusions(classExclusions = setOf("(.*\\.)?test(\\..*)?"))
    )
    assertThat(abiDependencies).isEmpty()
  }

  private fun compile(vararg sourceFiles: SourceFile): File {
    val outputDir = temporaryFolder.newFolder()
    val result = KotlinCompilation().apply {
      sources = sourceFiles.toList()
      inheritClassPath = true
      workingDir = outputDir
      verbose = false
    }.compile()

    check(result.exitCode == ExitCode.OK) {
      "Compilation failed"
    }

    val classesDir = outputDir.resolve("classes")
    val compiledFiles = result.compiledClassAndResourceFiles
    val manifest = Manifest()
    manifest.mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
    val outputJar = temporaryFolder.newFile("output.jar")
    JarOutputStream(FileOutputStream(outputJar), manifest).use { jar ->
      for (compiledFile in compiledFiles) {
        val entry = JarEntry(compiledFile.toRelativeString(classesDir).replace("\\", "/"))
        entry.time = compiledFile.lastModified()
        jar.putNextEntry(entry)
        compiledFile.source().use { fileSource ->
          jar.sink().buffer().apply {
            writeAll(fileSource)
            flush()
            // We don't close because we're still using the underlying jar
          }
        }
        jar.closeEntry()
      }
    }

    return outputJar
  }
}