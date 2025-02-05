// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import com.autonomousapps.internal.ClassNames.canonicalize
import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.utils.JAVA_FQCN_REGEX_SLASHY
import com.autonomousapps.internal.utils.asSequenceOfClassFiles
import com.autonomousapps.internal.utils.efficient
import com.autonomousapps.internal.utils.getLogger
import com.autonomousapps.model.internal.intermediates.consumer.ExplodingBytecode
import com.autonomousapps.model.internal.intermediates.consumer.MemberAccess
import org.gradle.api.logging.Logger
import java.io.File
import java.nio.file.Paths
import java.util.jar.JarFile

internal sealed class ClassReferenceParser(private val buildDir: File) {

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

/** Given a set of .class files, produce a set of FQCN references present in that set. */
internal class ClassFilesParser(
  private val classes: Set<File>,
  private val jarFiles: Set<File>,
  buildDir: File
) : ClassReferenceParser(buildDir) {

  private val logger = getLogger<ClassFilesParser>()

  override fun parseBytecode(): Set<ExplodingBytecode> {
    return (classesExplodingBytecode() + jarsExplodingBytecode()).toSet()
  }

  private fun classesExplodingBytecode(): List<ExplodingBytecode> {
    return classes.asSequenceOfClassFiles().map { classFile ->
      val classFilePath = classFile.path
      val explodedClass = classFile.inputStream().use {
        BytecodeReader(it.readBytes(), logger, classFilePath).parse()
      }

      explodedClass.toExplodingBytecode(relativize(classFile))
    }.toList()
  }

  private fun jarsExplodingBytecode(): List<ExplodingBytecode> {
    return jarFiles.asSequence().map { file ->
      val jarFile = JarFile(file)

      jarFile.asSequenceOfClassFiles().map { classFileEntry ->
        val classFilePath = Paths.get(file.path, classFileEntry.name).toString()
        val classFileRelativePath = Paths.get(relativize(file), classFileEntry.name).toString()
        val explodedClass = jarFile.getInputStream(classFileEntry).use {
          BytecodeReader(it.readBytes(), logger, classFilePath).parse()
        }

        explodedClass.toExplodingBytecode(classFileRelativePath)
      }
    }.flatten().toList()
  }

  private fun ExplodedClass.toExplodingBytecode(relativePath: String): ExplodingBytecode {
    return ExplodingBytecode(
      relativePath = relativePath,
      className = className,
      superClass = superClass,
      interfaces = interfaces,
      sourceFile = source,
      nonAnnotationClasses = nonAnnotationClasses,
      annotationClasses = annotationClasses,
      invisibleAnnotationClasses = invisibleAnnotationClasses,
      binaryClassAccesses = binaryClasses,
    )
  }
}

private class BytecodeReader(
  private val bytes: ByteArray,
  private val logger: Logger,
  private val classFilePath: String,
) {
  /**
   * This (currently, maybe forever) fails to detect constant usage in Kotlin-generated class files.
   * Works just fine for Java (but
   * [not the ecj compiler](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/735)).
   *
   * Returns a pair of values:
   * 1. The "source" of the class file (the source file name, like "Main.kt").
   * 2. The classes used by that class file.
   */
  fun parse(): ExplodedClass {
    val constantPool = ConstantPoolParser.getConstantPoolClassReferences(bytes, classFilePath)
      // Constant pool has a lot of weird bullshit in it
      .filter { JAVA_FQCN_REGEX_SLASHY.matches(it) }

    val classAnalyzer = ClassReader(bytes).let { classReader ->
      ClassAnalyzer(logger).apply {
        classReader.accept(this, 0)
      }
    }

    val usedVisibleAnnotationClasses = classAnalyzer.classes.asSequence()
      .filter { it.kind == ClassRef.Kind.ANNOTATION_VISIBLE }
      .map { it.classRef }
      .toSet()
    // TODO(tsr): use this somehow? I think these should be considered compileOnly candidates
    //  Look at `CompileOnlySpec#annotations can be compileOnly`. It detects usage of Producer because it is imported,
    //  but doesn't see it in the bytecode. I think this can be improved. Finding it in the bytecode is preferable to
    //  the import heuristic. We'll need to differentiate in/visible annotations though.
    val usedInvisibleAnnotationClasses = classAnalyzer.classes.asSequence()
      .filter { it.kind == ClassRef.Kind.ANNOTATION_HIDDEN }
      .map { it.classRef }
      .toSet()
    val usedNonAnnotationClasses = classAnalyzer.classes.asSequence()
      .filter { it.kind == ClassRef.Kind.NOT_ANNOTATION }
      .map { it.classRef }
      .toSet()

    return ExplodedClass(
      source = classAnalyzer.source,
      className = canonicalize(classAnalyzer.className),
      superClass = classAnalyzer.superClass?.let { canonicalize(it) },
      interfaces = classAnalyzer.interfaces.asSequence().fixup(classAnalyzer),
      nonAnnotationClasses = constantPool.asSequence().plus(usedNonAnnotationClasses).fixup(classAnalyzer),
      annotationClasses = usedVisibleAnnotationClasses.asSequence().fixup(classAnalyzer),
      invisibleAnnotationClasses = usedInvisibleAnnotationClasses.asSequence().fixup(classAnalyzer),
      binaryClasses = classAnalyzer.getBinaryClasses().fixup(classAnalyzer),
    )
  }

  // Change this in concert with the Map.fixup() function below
  private fun Sequence<String>.fixup(classAnalyzer: ClassAnalyzer): Set<String> {
    return this
      // Filter out `java` packages, but not `javax`
      .filterNot { it.startsWith("java/") }
      // Filter out a "used class" that is exactly the class under analysis
      .filterNot { it == classAnalyzer.className }
      // Filter out parent class name for inner classes
      .filterNot { it.substringBefore('$') == classAnalyzer.className.substringBefore('$') }
      // More human-readable
      .map { canonicalize(it) }
      .toSortedSet()
      .efficient()
  }

  // TODO(tsr): decide whether to dottify (canonicalize) the class names or leave them slashy
  // Change this in concert with the Sequence.fixup() function above
  private fun Map<String, Set<MemberAccess>>.fixup(classAnalyzer: ClassAnalyzer): Map<String, Set<MemberAccess>> {
    return this
      // Filter out `java` packages, but not `javax`
      .filterKeys { !it.startsWith("java/") }
      // Filter out a "used class" that is exactly the class under analysis
      .filterKeys { it != classAnalyzer.className }
      // Filter out parent class name for inner classes
      .filterKeys { it.substringBefore('$') != classAnalyzer.className.substringBefore('$') }
  }
}

private class ExplodedClass(
  val source: String?,
  val className: String,
  val superClass: String?,
  val interfaces: Set<String>,
  val nonAnnotationClasses: Set<String>,
  val annotationClasses: Set<String>,
  val invisibleAnnotationClasses: Set<String>,
  val binaryClasses: Map<String, Set<MemberAccess>>,
)
