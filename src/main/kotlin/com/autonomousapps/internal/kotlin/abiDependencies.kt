// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.kotlin

import com.autonomousapps.internal.AbiExclusions
import com.autonomousapps.internal.ClassNames.canonicalize
import com.autonomousapps.internal.utils.DESC_REGEX
import com.autonomousapps.internal.utils.allItems
import com.autonomousapps.internal.utils.flatMapToSet
import com.autonomousapps.model.intermediates.consumer.ExplodingAbi
import java.io.File

internal fun computeAbi(
  classFiles: Set<File>,
  exclusions: AbiExclusions,
  abiDumpFile: File? = null
): Set<ExplodingAbi> = getBinaryAPI(classFiles).explodedAbi(exclusions, abiDumpFile)

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
      exposedClasses += classSignature.genericTypes
        .flatMapToSet { DESC_REGEX.findAll(it).allItems() }
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

      classSignature.memberSignatures.forEach { sig ->
        sig.annotations.flatMapTo(exposedClasses) { DESC_REGEX.findAll(it).allItems() }
        if (sig is MethodBinarySignature) {
          sig.parameterAnnotations.flatMapTo(exposedClasses) { DESC_REGEX.findAll(it).allItems() }
          sig.typeAnnotations.flatMapTo(exposedClasses) { DESC_REGEX.findAll(it).allItems() }
          exposedClasses.addAll(sig.exceptions) // no need for DESC_REGEX
        }
      }

      // return
      ExplodingAbi(
        className = classSignature.canonicalName,
        sourceFile = classSignature.sourceFile,
        exposedClasses = exposedClasses.asSequence()
          // We don't report that the JDK is part of the ABI
          .filterNot { it.startsWith("java/") }
          .map { canonicalize(it) }
          .toSortedSet(),
      )
    }.toSortedSet()
}
