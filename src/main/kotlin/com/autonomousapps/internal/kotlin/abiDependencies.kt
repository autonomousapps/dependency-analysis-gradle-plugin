package com.autonomousapps.internal.kotlin

import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.AbiExclusions
import com.autonomousapps.internal.Component
import com.autonomousapps.internal.PublicComponent
import com.autonomousapps.internal.utils.DESC_REGEX
import com.autonomousapps.internal.utils.allItems
import com.autonomousapps.internal.utils.filterToSet
import com.autonomousapps.internal.utils.flatMapToSet
import com.autonomousapps.model.intermediates.ExplodingAbi
import java.io.File
import java.util.jar.JarFile

internal fun computeAbi(
  classFiles: Set<File>,
  exclusions: AbiExclusions,
  abiDumpFile: File? = null
): Set<ExplodingAbi> = getBinaryAPI(classFiles).explodedAbi(exclusions, abiDumpFile)

internal fun computeAbi(
  jarFile: File,
  exclusions: AbiExclusions,
  abiDumpFile: File? = null
): Set<ExplodingAbi> = getBinaryAPI(JarFile(jarFile)).explodedAbi(exclusions, abiDumpFile)

/**
 * Given a jar and a list of its dependencies (as [Component]s), return the set of [Dependency]s
 * that represents this jar's ABI (or public API).
 *
 * [exclusions] indicate exclusion rules (generated code, etc).
 * [abiDumpFile] is used only to write a rich ABI representation, and may be omitted.
 */
internal fun abiDependencies(
  jarFile: File,
  jarDependencies: List<Component>,
  exclusions: AbiExclusions,
  abiDumpFile: File? = null
): Set<PublicComponent> = getBinaryAPI(JarFile(jarFile)).dependencies(jarDependencies, exclusions, abiDumpFile)

/**
 * Given a set of `.class` files and a list of its dependencies (as [Component]s), return the set of
 * [Dependency]s that represents this project's ABI (or public API).
 *
 * [exclusions] indicate exclusion rules (generated code, etc).
 * [abiDumpFile] is used only to write a rich ABI representation, and may be omitted.
 */
internal fun abiDependencies(
  classFiles: Set<File>,
  jarDependencies: List<Component>,
  exclusions: AbiExclusions,
  abiDumpFile: File? = null
): Set<PublicComponent> = getBinaryAPI(classFiles).dependencies(jarDependencies, exclusions, abiDumpFile)

private fun List<ClassBinarySignature>.dependencies(
  jarDependencies: List<Component>,
  exclusions: AbiExclusions,
  abiDumpFile: File? = null
): Set<PublicComponent> {

  val publicComponents = mutableSetOf<PublicComponent>()
  val classNames = explodedAbi(exclusions, abiDumpFile).flatten()

  jarDependencies.forEach { component ->
    val classes = component.classes.filterToSet {
      classNames.contains(it)
    }
    if (classes.isNotEmpty()) {
      publicComponents.add(PublicComponent(component.dependency, classes))
    }
  }

  return publicComponents
}

private fun Set<ExplodingAbi>.flatten(): Set<String> {
  return asSequence()
    .flatMap { it.exposedClasses }
    .toSortedSet()
}

private fun List<ClassBinarySignature>.explodedAbi(
  exclusions: AbiExclusions,
  abiDumpFile: File? = null
): Set<ExplodingAbi> {
  return filterOutNonPublic(exclusions)
    .also { publicApi ->
      abiDumpFile?.let { file ->
        file.bufferedWriter().use { writer -> publicApi.dump(writer) }
      }
    }
    .asSequence()
    .map { classSignature ->
      val exposedClasses = sortedSetOf<String>()

      exposedClasses += classSignature.supertypes
      exposedClasses += classSignature.memberSignatures
        .map {
          // descriptor, e.g. `(JLjava/lang/String;JI)Lio/reactivex/Single;`
          // This one takes a long, a String, a long, and an int, and returns a Single
          it.desc
        }.flatMapToSet { DESC_REGEX.findAll(it).allItems() }
      exposedClasses += classSignature.memberSignatures
        .flatMap { it.genericTypes }
        .flatMapToSet { DESC_REGEX.findAll(it).allItems() }

      exposedClasses += classSignature.annotations
        .map {
          // If the descriptor looks like "Lsome/thing;", then extract some/thing
          DESC_REGEX.find(it)?.groupValues?.get(1) ?: it
        }

      // TODO shouldn't iterate through memberSignatures more than once
      exposedClasses += classSignature.memberSignatures
        .flatMap { it.annotations }
        .flatMapToSet { DESC_REGEX.findAll(it).allItems() }
      exposedClasses += classSignature.memberSignatures
        .filterIsInstance<MethodBinarySignature>()
        .flatMapToSet { it.parameterAnnotations }
        .flatMapToSet { DESC_REGEX.findAll(it).allItems() }
      exposedClasses += classSignature.memberSignatures
        .filterIsInstance<MethodBinarySignature>()
        .flatMapToSet { it.typeAnnotations }
        .flatMapToSet { DESC_REGEX.findAll(it).allItems() }

      exposedClasses += classSignature.memberSignatures
        .filterIsInstance<MethodBinarySignature>()
        .flatMapToSet { it.exceptions } // no need for DESC_REGEX

      // return
      ExplodingAbi(
        className = classSignature.canonicalName,
        sourceFile = classSignature.sourceFile,
        exposedClasses = exposedClasses.asSequence()
          // We don't report that the JDK is part of the ABI
          .filterNot { it.startsWith("java/") }
          .map { it.replace("/", ".") }
          .toSet()
      )
    }.toSortedSet()
}
