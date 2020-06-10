package com.autonomousapps.internal

import com.autonomousapps.advice.VariantFile
import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.utils.JAVA_FQCN_REGEX
import com.autonomousapps.internal.utils.JAVA_FQCN_REGEX_SLASHY
import com.autonomousapps.internal.utils.buildDocument
import com.autonomousapps.internal.utils.filterToSet
import com.autonomousapps.internal.utils.flatMapToSet
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
  private val kaptJavaSource: Set<File>,
  private val testFiles: Set<File>
) {

  private val logger = getLogger<ProjectClassReferenceParser>()

  /**
   * Source is either a jar or set of class files.
   */
  protected abstract fun parseBytecode(): List<VariantClass>

  protected fun variantsFromFile(file: File): Set<String> {
    return variantsFromPath(file.path)
  }

  /**
   * Associate file paths to variants/source sets.
   */
  protected fun variantsFromPath(path: String): Set<String> {
    val fileExtension = path.substring(path.lastIndexOf("."))
    return variantFiles.filter {
      path.endsWith("${it.filePath}${fileExtension}")
    }.mapToOrderedSet {
      it.variant
    }
  }

  private fun parseLayouts(): List<VariantClass> {
    return layouts.flatMap { layoutFile ->
      val variants = variantsFromFile(layoutFile)
      buildDocument(layoutFile).getElementsByTagName("*")
        .map { it.nodeName }
        .filter { nodeName ->
          nodeName.contains(".")
        }.map {
          VariantClass(it, variants)
        }
    }
  }

  // TODO Highly tempted to just remove this entirely. Would anything break?
  // TODO replace with antlr-based solution
  private fun parseKaptJavaSource(): List<VariantClass> {
    return kaptJavaSource
      .flatMap { file ->
        val variants = variantsFromFile(file)
        // This regex is grabbing things that aren't class names. E.g., urls, method calls. Maybe it
        // doesn't matter, though. If they can't be associated with a module, then they're ignored
        // later in the analysis. Some FQCN references are only available via import statements;
        // others via FQCN in the body. Should be improved, but it's unclear how best.
        file.readLines().flatMapToSet { line -> JAVA_FQCN_REGEX.findAll(line).toList() }
          .mapToSet { matchResult -> matchResult.value }
          .mapToSet { clazz -> clazz.removeSuffix(".class") }
          .mapToSet { clazz -> VariantClass(clazz, variants) }
      }
  }

  private fun parseTestSource(): List<VariantClass> {
    return testFiles
      .filter { it.extension == "class" }
      .map { classFile ->
        val variants = variantsFromFile(classFile)
        val usedClasses = classFile.inputStream().use { BytecodeParser(it.readBytes(), logger).parse() }
        variants to usedClasses
      }.flatMap { (variants, classes) ->
        classes.map {
          VariantClass(it, variants)
        }
      }
  }

  // TODO some jars only have metadata. What to do about them?
  // 1. e.g. kotlin-stdlib-common-1.3.50.jar
  // 2. e.g. legacy-support-v4-1.0.0/jars/classes.jar
  internal fun analyze(): Set<VariantClass> {
    val variants = parseBytecode().plus(parseLayouts()).plus(parseTestSource())//.plus(parseKaptJavaSource()))
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
 * Given a jar and, optionally, a set of Android layout files and Kapt-generated Java stubs, produce a set of FQCN
 * references present in these inputs, as strings. These inputs are part of a single logical whole, viz., the Gradle
 * project being analyzed.
 */
internal class JarReader(
  variantFiles: Set<VariantFile>,
  jarFile: File,
  layouts: Set<File>,
  testFiles: Set<File>,
  kaptJavaSource: Set<File>
) : ProjectClassReferenceParser(
  variantFiles = variantFiles,
  layouts = layouts,
  kaptJavaSource = kaptJavaSource,
  testFiles = testFiles
) {

  private val logger = getLogger<JarReader>()
  private val zipFile = ZipFile(jarFile)

  override fun parseBytecode(): List<VariantClass> {
    return zipFile.entries().toList()
      .filterToSet { it.name.endsWith(".class") }
      .map { classEntry ->
        val variants = variantsFromPath(classEntry.name)
        val usedClasses = zipFile.getInputStream(classEntry).use { BytecodeParser(it.readBytes(), logger).parse() }
        variants to usedClasses
      }.flatMap { (variants, classes) ->
        classes.map { VariantClass(it, variants) }
      }
  }
}

/**
 * Given a set of .class files and, optionally, a set of Android layout files and Kapt-generated Java stubs, produce a
 * set of FQCN references present in these inputs, as strings. These inputs are part of a single logical whole, viz.,
 * the Gradle project being analyzed.
 */
internal class ClassSetReader(
  private val classes: Set<File>,
  variantFiles: Set<VariantFile>,
  layouts: Set<File>,
  kaptJavaSource: Set<File>,
  testFiles: Set<File>
) : ProjectClassReferenceParser(
  variantFiles = variantFiles,
  layouts = layouts,
  kaptJavaSource = kaptJavaSource,
  testFiles = testFiles
) {

  private val logger = getLogger<ClassSetReader>()

  override fun parseBytecode(): List<VariantClass> {
    return classes.map { classFile ->
      val variants = variantsFromFile(classFile)
      val usedClasses = classFile.inputStream().use { BytecodeParser(it.readBytes(), logger).parse() }
      variants to usedClasses
    }.flatMap { (variants, classes) ->
      classes.map {
        VariantClass(it, variants)
      }
    }
  }
}

private class BytecodeParser(
  private val bytes: ByteArray,
  private val logger: Logger
) {
  /**
   * This (currently, maybe forever) fails to detect constant usage in Kotlin-generated class files. Works just fine
   * for Java.
   */
  fun parse(): Set<String> {
    // The "onEach"s are for debugging
    val constantPool = ConstantPoolParser.getConstantPoolClassReferences(bytes)
      // Constant pool has a lot of weird bullshit in it
      .filter { JAVA_FQCN_REGEX_SLASHY.matches(it) }

    val classEntries = ClassReader(bytes).let { classReader ->
      ClassAnalyzer(logger).apply {
        classReader.accept(this, 0)
      }
    }.classes()

    return constantPool.plus(classEntries)
      // Filter out `java` packages, but not `javax`
      .filterNot { it.startsWith("java/") }
      .mapToSet { it.replace("/", ".") }
  }
}
