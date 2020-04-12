package com.autonomousapps.internal

import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.utils.*
import org.gradle.api.logging.Logger
import java.io.File
import java.util.zip.ZipFile

internal sealed class ProjectClassReferenceParser(
  private val layouts: Set<File>,
  private val kaptJavaSource: Set<File>
) {

  /**
   * Source is either a jar or set of class files.
   */
  protected abstract fun parseBytecode(): Set<String>

  private fun parseLayouts(): Set<String> {
    return layouts.map { layoutFile ->
      val document = buildDocument(layoutFile)
      document.getElementsByTagName("*")
    }.flatMapToSet { nodeList ->
      nodeList.map { it.nodeName }.filter { it.contains(".") }
    }
  }

  // TODO replace with antlr-based solution
  private fun parseKaptJavaSource(): Set<String> {
    return kaptJavaSource
      .flatMapToSet { it.readLines() }
      // This is grabbing things that aren't class names. E.g., urls, method calls. Maybe it doesn't matter, though.
      // If they can't be associated with a module, then they're ignored later in the analysis. Some FQCN references
      // are only available via import statements; others via FQCN in the body. Should be improved, but it's unclear
      // how best.
      .flatMapToSet { JAVA_FQCN_REGEX.findAll(it).toList() }
      .mapToSet { it.value }
      .mapToSet { it.removeSuffix(".class") }
  }

  // TODO some jars only have metadata. What to do about them?
  // 1. e.g. kotlin-stdlib-common-1.3.50.jar
  // 2. e.g. legacy-support-v4-1.0.0/jars/classes.jar
  internal fun analyze(): Set<String> = (parseBytecode() + parseLayouts() + parseKaptJavaSource()).toSortedSet()
}

/**
 * Given a jar and, optionally, a set of Android layout files and Kapt-generated Java stubs, produce a set of FQCN
 * references present in these inputs, as strings. These inputs are part of a single logical whole, viz., the Gradle
 * project being analyzed.
 */
internal class JarReader(
  jarFile: File,
  layouts: Set<File>,
  kaptJavaSource: Set<File>
) : ProjectClassReferenceParser(layouts = layouts, kaptJavaSource = kaptJavaSource) {

  private val logger = getLogger<JarReader>()
  private val zipFile = ZipFile(jarFile)

  override fun parseBytecode(): Set<String> {
    return zipFile.entries().toList()
      .filterToSet { it.name.endsWith(".class") }
      .flatMapToSet { classEntry ->
        zipFile.getInputStream(classEntry).use { BytecodeParser(it.readBytes(), logger).parse() }
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
  layouts: Set<File>,
  kaptJavaSource: Set<File>
) : ProjectClassReferenceParser(layouts = layouts, kaptJavaSource = kaptJavaSource) {

  private val logger = getLogger<ClassSetReader>()

  override fun parseBytecode(): Set<String> {
    return classes.flatMapToSet { classFile ->
      classFile.inputStream().use { BytecodeParser(it.readBytes(), logger).parse() }
    }
  }
}

private class BytecodeParser(private val bytes: ByteArray, private val logger: Logger) {
  /**
   * This (currently, maybe forever) fails to detect constant usage in Kotlin-generated class files. Works just fine
   * for Java.
   */
  fun parse(): Set<String> {
    // The "onEach"s are for debugging
    val constantPool = ConstantPoolParser.getConstantPoolClassReferences(bytes)
      //.onEach { println("CONSTANT: $it") }
      // Constant pool has a lot of weird bullshit in it
      .filter { JAVA_FQCN_REGEX_SLASHY.matches(it) }
    //.onEach { println("CONSTANT: $it") }

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
