package com.autonomousapps.internal

import com.autonomousapps.advice.VariantFile
import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.utils.JAVA_FQCN_REGEX_SLASHY
import com.autonomousapps.internal.utils.asClassFiles
import com.autonomousapps.internal.utils.buildDocument
import com.autonomousapps.internal.utils.getLogger
import com.autonomousapps.internal.utils.map
import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.internal.utils.mapToSet
import org.gradle.api.logging.Logger
import java.io.File
import java.util.zip.ZipFile

internal sealed class ProjectClassReferenceParser(
  protected val variantFiles: Set<VariantFile>,
  private val layouts: Set<File>,
  private val testFiles: Set<File>
) {

  private val logger = getLogger<ProjectClassReferenceParser>()

  /**
   * Source is either a jar or set of class files.
   */
  protected abstract fun parseBytecode(): List<VariantClass>

  /**
   * Associate file paths to variants/source sets.
   */
  protected fun variantsFromPath(normalizedPath: NormalizedPath): Set<String> {
    val path = normalizedPath.value
    val fileExtension = path.substring(path.lastIndexOf("."))

    // TODO two unique files may have the same path (test/foo/Bar.kt, main/foo/Bar.kt) and will be mis-associated with both variants. Unclear if this matters.
    return variantFiles.filter {
      path.endsWith("${it.filePath}${fileExtension}")
    }.mapToOrderedSet {
      it.variant
    }
  }

  private fun parseLayouts(): List<VariantClass> {
    return layouts.flatMap { layoutFile ->
      val variants = variantsFromPath(normalizePath(layoutFile.path))
      buildDocument(layoutFile).getElementsByTagName("*")
        .map { it.nodeName }
        .filter { nodeName ->
          nodeName.contains(".")
        }.map {
          VariantClass(it, variants)
        }
    }
  }

  private fun parseTestSource(): List<VariantClass> {
    return testFiles
      .filter { it.extension == "class" }
      .map { classFile ->
        val parsedClass = classFile.inputStream().use {
          BytecodeParser(it.readBytes(), logger).parse()
        }
        val usedClasses = parsedClass.second

        val normalizedPath = normalizePath(classFile.path, parsedClass.first)
        val variants = variantsFromPath(normalizedPath)
        variants to usedClasses
      }.flatMap { (variants, classes) ->
        classes.map {
          VariantClass(it, variants)
        }
      }
  }

  // Example filePath: /path/to/project/build/tmp/kotlin-classes/debug/com/example/MainActivity$onCreate$$inlined$AppBarConfiguration$default$1.class
  // Example source: AppBarConfiguration.kt (this doesn't exist as a real file in the project: it's generated)
  // We want /path/to/project/build/tmp/kotlin-classes/debug/com/example/AppBarConfiguration.kt
  protected fun normalizePath(filePath: String, source: String? = null): NormalizedPath {
    if (source == null) return NormalizedPath(filePath)

    val dirPath = filePath.substringBeforeLast(File.separator) + File.separator
    val sourceName = source.substringBeforeLast(".")
    val fileExtension = filePath.substring(filePath.lastIndexOf("."))

    return NormalizedPath(dirPath + sourceName + fileExtension)
  }

  // TODO some jars only have metadata. What to do about them?
  // 1. e.g. kotlin-stdlib-common-1.3.50.jar
  // 2. e.g. legacy-support-v4-1.0.0/jars/classes.jar
  internal fun analyze(): Set<VariantClass> {
    val variants = parseBytecode().plus(parseLayouts()).plus(parseTestSource())
    return variants.merge()
  }

  private fun List<VariantClass>.merge(): Set<VariantClass> {
    // a Collection<VariantClass> is functionally a map
    val map = LinkedHashMap<String, MutableSet<String>>()
    forEach {
      val theClass = it.theClass
      val variants = it.variants
      map.merge(theClass, variants.toSortedSet()) { oldSet, newSet ->
        oldSet.apply { addAll(newSet) }
      }
    }
    return map.map { (theClass, variants) ->
      VariantClass(theClass, variants)
    }.toSortedSet()
  }
}

/**
 * Given a jar and, optionally, and a set of Android layout files, produce a set of FQCN references
 * present in these inputs, as strings. These inputs are part of a single logical whole, viz., the
 * Gradle project being analyzed.
 */
internal class JarReader(
  variantFiles: Set<VariantFile>,
  jarFile: File,
  layouts: Set<File>,
  testFiles: Set<File>
) : ProjectClassReferenceParser(
  variantFiles = variantFiles,
  layouts = layouts,
  testFiles = testFiles
) {

  private val logger = getLogger<JarReader>()
  private val zipFile = ZipFile(jarFile)

  override fun parseBytecode(): List<VariantClass> {
    return zipFile.asClassFiles()
      .map { classEntry ->
        val parsedClass = zipFile.getInputStream(classEntry).use {
          BytecodeParser(it.readBytes(), logger).parse()
        }
        val usedClasses = parsedClass.second

        val normalizedPath = normalizePath(classEntry.name, parsedClass.first)
        val variants = variantsFromPath(normalizedPath)
        variants to usedClasses
      }.flatMap { (variants, classes) ->
        classes.map { VariantClass(it, variants) }
      }
  }
}

/**
 * Given a set of .class files and, optionally, a set of Android layout files, produce a set of
 * FQCN references present in these inputs, as strings. These inputs are part of a single logical
 * whole, viz., the Gradle project being analyzed.
 */
internal class ClassSetReader(
  variantFiles: Set<VariantFile>,
  private val classes: Set<File>,
  layouts: Set<File>,
  testFiles: Set<File>
) : ProjectClassReferenceParser(
  variantFiles = variantFiles,
  layouts = layouts,
  testFiles = testFiles
) {

  private val logger = getLogger<ClassSetReader>()

  override fun parseBytecode(): List<VariantClass> {
    return classes.map { classFile ->
      val parsedClass = classFile.inputStream().use {
        BytecodeParser(it.readBytes(), logger).parse()
      }
      val usedClasses = parsedClass.second

      val normalizedPath = normalizePath(classFile.path, parsedClass.first)
      val variants = variantsFromPath(normalizedPath)
      variants to usedClasses
    }.flatMap { (variants, usedClasses) ->
      usedClasses.map { usedClass ->
        VariantClass(usedClass, variants)
      }
    }
  }
}

private class BytecodeParser(
  private val bytes: ByteArray,
  private val logger: Logger
) {
  /**
   * This (currently, maybe forever) fails to detect constant usage in Kotlin-generated class files.
   * Works just fine for Java.
   *
   * Returns a pair of values:
   * 1. The "source" of the class file (the source file name, like "Main.kt").
   * 2. The classes used by that class file.
   */
  fun parse(): Pair<String?, Set<String>> {
    val constantPool = ConstantPoolParser.getConstantPoolClassReferences(bytes)
      // Constant pool has a lot of weird bullshit in it
      .filter { JAVA_FQCN_REGEX_SLASHY.matches(it) }

    val classEntries = ClassReader(bytes).let { classReader ->
      ClassAnalyzer(logger).apply {
        classReader.accept(this, 0)
      }
    }.classes()

    return classEntries.first to constantPool.plus(classEntries.second)
      // Filter out `java` packages, but not `javax`
      .filterNot { it.startsWith("java/") }
      .mapToSet { it.replace("/", ".") }
  }
}

// TODO replace with value class when Gradle supports targeting Kotlin 1.5+
//  https://docs.gradle.org/7.3/userguide/compatibility.html#kotlin
//  https://kotlinlang.org/docs/inline-classes.html
//  https://blog.jetbrains.com/kotlin/2021/02/new-language-features-preview-in-kotlin-1-4-30/#inline-value-classes-stabilization
internal inline class NormalizedPath(val value: String)
