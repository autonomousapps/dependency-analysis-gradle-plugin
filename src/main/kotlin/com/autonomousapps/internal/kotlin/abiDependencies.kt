package com.autonomousapps.internal.kotlin

import com.autonomousapps.internal.Component
import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.AbiExclusions
import com.autonomousapps.internal.utils.*
import com.autonomousapps.internal.utils.DESC_REGEX
import com.autonomousapps.internal.utils.allItems
import java.io.File
import java.util.jar.JarFile

/**
 * Given a jar and a list of its dependencies (as [Component]s), return the set of [Dependency]s that represents this
 * jar's ABI (or public API).
 *
 * [exclusions] indicate exclusion rules (generated code, etc).
 *
 * [abiDumpFile] is used only to write a rich ABI representation, and may be omitted.
 */
internal fun abiDependencies(
  jarFile: File,
  jarDependencies: List<Component>,
  exclusions: AbiExclusions,
  abiDumpFile: File? = null
): Set<Dependency> =
    getBinaryAPI(JarFile(jarFile)).filterOutNonPublic(exclusions)
        .also { publicApi ->
          abiDumpFile?.let { file ->
            file.bufferedWriter().use { writer -> publicApi.dump(writer) }
          }
        }
        .flatMapToSet { classSignature ->
          val superTypes = classSignature.supertypes
          val memberTypes = classSignature.memberSignatures.map {
            // descriptor, e.g. `(JLjava/lang/String;JI)Lio/reactivex/Single;`
            // This one takes a long, a String, a long, and an int, and returns a Single
            it.desc
          }.flatMapToSet {
            DESC_REGEX.findAll(it).allItems()
          }
          superTypes + memberTypes
        }.mapToSet {
          it.replace("/", ".")
        }.mapNotNullToOrderedSet { fqcn ->
          jarDependencies.find { component ->
            component.classes.contains(fqcn)
          }?.dependency
        }
