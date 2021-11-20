package com.autonomousapps.internal

import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.utils.JAVA_FQCN_REGEX_SLASHY
import com.autonomousapps.internal.utils.asClassFiles
import com.autonomousapps.internal.utils.getLogger
import com.autonomousapps.model.intermediates.ExplodingBytecode
import org.gradle.api.logging.Logger
import java.io.File
import java.util.zip.ZipFile

internal sealed class ClassReferenceParser2(private val buildDir: File) {

  /** Source is either a jar or set of class files. */
  protected abstract fun parseBytecode(): Set<ExplodingBytecode>

  protected fun relativize(file: File) = file.toRelativeString(buildDir)

  // TODO some jars only have metadata. What to do about them?
  // 1. e.g. kotlin-stdlib-common-1.3.50.jar
  // 2. e.g. legacy-support-v4-1.0.0/jars/classes.jar
  internal fun analyze(): Set<ExplodingBytecode> {
    return parseBytecode()
  }
}

// TODO I'd like to stop parsing jars, as I only do it for Android libs, and I ought to be able to get the class files
/** Given a jar, produce a set of FQCN references present in it. */
internal class JarParser(
  jarFile: File,
  buildDir: File
) : ClassReferenceParser2(buildDir) {

  private val logger = getLogger<JarReader>()
  private val zipFile = ZipFile(jarFile)

  override fun parseBytecode(): Set<ExplodingBytecode> {
    return zipFile.asClassFiles()
      .map { classEntry ->
        val explodedClass = zipFile.getInputStream(classEntry).use {
          BytecodeReader(it.readBytes(), logger).parse()
        }

        ExplodingBytecode(
          relativePath = classEntry.name,
          className = explodedClass.className.replace('/', '.'),
          sourceFile = explodedClass.source,
          usedClasses = explodedClass.usedClasses
        )
      }.toSet()
  }
}

/** Given a set of .class files, produce a set of FQCN references present in that set. */
internal class ClassFilesParser(
  private val classes: Set<File>,
  buildDir: File
) : ClassReferenceParser2(buildDir) {

  private val logger = getLogger<ClassFilesParser>()

  override fun parseBytecode(): Set<ExplodingBytecode> {
    return classes.map { classFile ->
      val explodedClass = classFile.inputStream().use {
        BytecodeReader(it.readBytes(), logger).parse()
      }

      ExplodingBytecode(
        relativePath = relativize(classFile),
        className = explodedClass.className.replace('/', '.'),
        sourceFile = explodedClass.source,
        usedClasses = explodedClass.usedClasses,
      )
    }.toSet()
  }
}

private class BytecodeReader(
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
  fun parse(): ExplodedClass {
    val constantPool = ConstantPoolParser.getConstantPoolClassReferences(bytes)
      // Constant pool has a lot of weird bullshit in it
      .filter { JAVA_FQCN_REGEX_SLASHY.matches(it) }

    val classAnalyzer = ClassReader(bytes).let { classReader ->
      ClassAnalyzer(logger).apply {
        classReader.accept(this, 0)
      }
    }

    return ExplodedClass(
      source = classAnalyzer.source,
      className = classAnalyzer.className,
      usedClasses = constantPool.asSequence().plus(classAnalyzer.classes)
        // Filter out `java` packages, but not `javax`
        .filterNot { it.startsWith("java/") }
        // Filter out a "used class" that is exactly the class under analysis
        .filterNot { it == classAnalyzer.className }
        // More human-readable
        .map { it.replace('/', '.') }
        .toSortedSet()
    )
  }
}

private class ExplodedClass(
  val source: String?,
  val className: String,
  val usedClasses: Set<String>
)
