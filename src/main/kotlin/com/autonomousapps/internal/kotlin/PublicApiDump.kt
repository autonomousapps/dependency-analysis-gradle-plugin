/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 *
 * Copied from https://github.com/JetBrains/kotlin/tree/master/libraries/tools/binary-compatibility-validator
 */

package com.autonomousapps.internal.kotlin

import com.autonomousapps.internal.AbiExclusions
import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.asm.Opcodes
import com.autonomousapps.internal.asm.tree.ClassNode
import kotlinx.metadata.jvm.JvmFieldSignature
import kotlinx.metadata.jvm.JvmMethodSignature
import java.io.InputStream
import java.util.jar.JarFile

fun main(args: Array<String>) {
  val src = args[0]
  println(src)
  println("------------------\n")
  getBinaryAPI(JarFile(src)).filterOutNonPublic().dump()
}


fun JarFile.classEntries() = Sequence { entries().iterator() }.filter {
  !it.isDirectory && it.name.endsWith(".class") && !it.name.startsWith("META-INF/")
}

fun getBinaryAPI(jar: JarFile, visibilityFilter: (String) -> Boolean = { true }): List<ClassBinarySignature> =
    getBinaryAPI(jar.classEntries().map { entry -> jar.getInputStream(entry) }, visibilityFilter)

fun getBinaryAPI(classStreams: Sequence<InputStream>, visibilityFilter: (String) -> Boolean = { true }): List<ClassBinarySignature> {
  val classNodes = classStreams.map {
    it.use { stream ->
      val classNode = ClassNode()
      ClassReader(stream).accept(classNode, ClassReader.SKIP_CODE)
      classNode
    }
  }

  val visibilityMapNew = classNodes.readKotlinVisibilities().filterKeys(visibilityFilter)

  return classNodes
      .map {
        with(it) {
          val metadata = kotlinMetadata
          val mVisibility = visibilityMapNew[name]
          val classAccess = AccessFlags(effectiveAccess and Opcodes.ACC_STATIC.inv())

          val supertypes = listOf(superName) - "java/lang/Object" + interfaces.sorted()

          val memberSignatures = (
              fields.map { with(it) { FieldBinarySignature(JvmFieldSignature(name, desc), isPublishedApi(), AccessFlags(access)) } } +
                  methods.map { with(it) { MethodBinarySignature(JvmMethodSignature(name, desc), isPublishedApi(), AccessFlags(access)) } }
              ).filter {
            it.isEffectivelyPublic(classAccess, mVisibility)
          }

          val annotations = (visibleAnnotations.orEmpty() + invisibleAnnotations.orEmpty())
            .map { it.desc.replace("/", ".") }

          ClassBinarySignature(
            name = name,
            superName = superName,
            outerName = outerClassName,
            supertypes = supertypes,
            memberSignatures = memberSignatures,
            access = classAccess,
            isEffectivelyPublic = isEffectivelyPublic(mVisibility),
            isNotUsedWhenEmpty = metadata.isFileOrMultipartFacade() || isDefaultImpls(metadata),
            annotations = annotations,
            // TODO toe-hold for filtering by directory
            sourceFileLocation = null
          )
        }
      }
      .asIterable()
      .sortedBy { it.name }
}


internal fun List<ClassBinarySignature>.filterOutNonPublic(
  exclusions: AbiExclusions = AbiExclusions.NONE
): List<ClassBinarySignature> {
  val classByName = associateBy { it.name }

  // Library note - this function (plus the exclusions parameter above) are modified from the original
  // Kotlin sources this was borrowed from.
  fun ClassBinarySignature.isExcluded(): Boolean {
    return (sourceFileLocation?.let(exclusions::excludesPath) ?: false) ||
      exclusions.excludesClass(canonicalName) ||
      annotations.any(exclusions::excludesAnnotation)
  }

  fun ClassBinarySignature.isPublicAndAccessible(): Boolean =
      isEffectivelyPublic &&
          (outerName == null || classByName[outerName]?.let { outerClass ->
            !(this.access.isProtected && outerClass.access.isFinal)
                && outerClass.isPublicAndAccessible()
          } ?: true)

  fun supertypes(superName: String) = generateSequence({ classByName[superName] }, { classByName[it.superName] })

  fun ClassBinarySignature.flattenNonPublicBases(): ClassBinarySignature {

    val nonPublicSupertypes = supertypes(superName).takeWhile { !it.isPublicAndAccessible() }.toList()
    if (nonPublicSupertypes.isEmpty())
      return this

    val inheritedStaticSignatures = nonPublicSupertypes.flatMap { it.memberSignatures.filter { it.access.isStatic } }

    // not covered the case when there is public superclass after chain of private superclasses
    return this.copy(memberSignatures = memberSignatures + inheritedStaticSignatures, supertypes = supertypes - superName)
  }

  return filter { !it.isExcluded() && it.isPublicAndAccessible() }
      .map { it.flattenNonPublicBases() }
      .filterNot { it.isNotUsedWhenEmpty && it.memberSignatures.isEmpty() }
}

fun List<ClassBinarySignature>.dump() = dump(to = System.out)

fun <T : Appendable> List<ClassBinarySignature>.dump(to: T): T = to.apply {
  this@dump.forEach {
    append(it.signature).appendln(" {")
    it.memberSignatures.sortedWith(MEMBER_SORT_ORDER).forEach { append("\t").appendln(it.signature) }
    appendln("}\n")
  }
}
