package com.autonomousapps.internal.kotlin

import com.autonomousapps.internal.Component
import com.autonomousapps.internal.utils.DESC_REGEX
import com.autonomousapps.internal.Dependency
import com.autonomousapps.internal.utils.allItems
import java.io.File
import java.util.jar.JarFile

/**
 * Given a jar and a list of its dependencies (as [Component]s), return the set of [Dependency]s that represents this
 * jar's ABI (or public API). [abiDumpFile] is used only to write a rich ABI representation, and may be omitted.
 */
fun abiDependencies(jarFile: File, jarDependencies: List<Component>, abiDumpFile: File? = null): Set<Dependency> =
    getBinaryAPI(JarFile(jarFile)).filterOutNonPublic()
        .also { publicApi ->
          abiDumpFile?.let { file ->
            file.bufferedWriter().use { writer -> publicApi.dump(writer) }
          }
        }
        .flatMap { classSignature ->
          val superTypes = classSignature.supertypes
          val memberTypes = classSignature.memberSignatures.map {
            // descriptor, e.g. `(JLjava/lang/String;JI)Lio/reactivex/Single;`
            // This one takes a long, a String, a long, and an int, and returns a Single
            it.desc
          }.flatMap {
            DESC_REGEX.findAll(it).allItems()
          }
          superTypes + memberTypes
        }.map {
          it.replace("/", ".")
        }.mapNotNull { fqcn ->
          jarDependencies.find { component ->
            component.classes.contains(fqcn)
          }?.dependency
        }.toSortedSet()
