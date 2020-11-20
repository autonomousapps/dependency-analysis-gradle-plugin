package com.autonomousapps.internal.kotlin

import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.AbiExclusions
import com.autonomousapps.internal.Component
import com.autonomousapps.internal.PublicComponent
import com.autonomousapps.internal.utils.*
import java.io.File
import java.util.jar.JarFile

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
): Set<PublicComponent> =
  getBinaryAPI(JarFile(jarFile)).dependencies(jarDependencies, exclusions, abiDumpFile)

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
): Set<PublicComponent> =
  getBinaryAPI(classFiles).dependencies(jarDependencies, exclusions, abiDumpFile)

private fun List<ClassBinarySignature>.dependencies(
  jarDependencies: List<Component>,
  exclusions: AbiExclusions,
  abiDumpFile: File? = null
): Set<PublicComponent> {

  val publicComponents = mutableSetOf<PublicComponent>()

  val classNames = filterOutNonPublic(exclusions)
    .also { publicApi ->
      abiDumpFile?.let { file ->
        file.bufferedWriter().use { writer -> publicApi.dump(writer) }
      }
    }
    .flatMapToSet { classSignature ->
      val superTypes = classSignature.supertypes
      val annotations = classSignature.annotations
        .map {
          // If the descriptor looks like "Lsome/thing;", then extract some/thing
          DESC_REGEX.find(it)?.groupValues?.get(1) ?: it
        }
      val memberTypes = classSignature.memberSignatures
        .map {
          // descriptor, e.g. `(JLjava/lang/String;JI)Lio/reactivex/Single;`
          // This one takes a long, a String, a long, and an int, and returns a Single
          it.desc
        }.flatMapToSet { DESC_REGEX.findAll(it).allItems() }
      val memberAnnotations = classSignature.memberSignatures
        .flatMap { it.annotations }
        .flatMapToSet { DESC_REGEX.findAll(it).allItems() }
      val parameterAnnotations = classSignature.memberSignatures
        .filterIsInstance<MethodBinarySignature>()
        .flatMapToSet { it.parameterAnnotations }
        .flatMapToSet { DESC_REGEX.findAll(it).allItems() }

      // return
      superTypes + memberTypes + annotations + memberAnnotations + parameterAnnotations
    }.mapToSet {
      it.replace("/", ".")
    }

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
