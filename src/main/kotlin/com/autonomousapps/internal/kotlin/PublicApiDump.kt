// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.kotlin

import com.autonomousapps.internal.AbiExclusions
import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.asm.Opcodes
import com.autonomousapps.internal.asm.tree.ClassNode
import com.autonomousapps.internal.utils.annotationTypes
import com.autonomousapps.internal.utils.appendReproducibleNewLine
import com.autonomousapps.internal.utils.filterNotToSet
import com.autonomousapps.internal.utils.genericTypes
import com.autonomousapps.model.internal.AccessFlags
import java.io.File
import java.io.InputStream
import java.io.PrintStream
import java.util.jar.JarFile
import kotlin.metadata.KmClassifier
import kotlin.metadata.KmType
import kotlin.metadata.isSuspend
import kotlin.metadata.jvm.JvmFieldSignature
import kotlin.metadata.jvm.JvmMethodSignature
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.signature

internal fun main(args: Array<String>) {
  val src = args[0]
  println(src)
  println("------------------\n")
  getBinaryAPI(JarFile(src)).filterOutNonPublic().dump()
}

internal fun JarFile.classEntries() = Sequence { entries().iterator() }.filter {
  !it.isDirectory && it.name.endsWith(".class") && !it.name.startsWith("META-INF/")
}

internal fun getBinaryAPI(jar: JarFile, visibilityFilter: (String) -> Boolean = { true }): List<ClassBinarySignature> =
  getBinaryAPI(jar.classEntries().map { entry -> jar.getInputStream(entry) }, visibilityFilter =  visibilityFilter)

internal fun getBinaryAPI(
  classes: Set<File>,
  sourceFiles: Set<File>,
  visibilityFilter: (String) -> Boolean = { true }
): List<ClassBinarySignature> =
  getBinaryAPI(classes.asSequence().map { it.inputStream() }, sourceFiles, visibilityFilter)

internal fun getBinaryAPI(
  classStreams: Sequence<InputStream>,
  sourceFiles: Set<File> = emptySet(),
  visibilityFilter: (String) -> Boolean = { true }
): List<ClassBinarySignature> {
  val classNodes = classStreams.map {
    it.use { stream ->
      val classNode = ClassNode()
      ClassReader(stream).accept(classNode, ClassReader.SKIP_CODE)
      classNode
    }
  }.toSet() // eagerly transform this into a Set, because it will be iterated several times

  val moduleInfo = classNodes.find { it.name == "module-info" }
  val exportedPackages = moduleInfo?.module?.exportedPackages()

  val visibilityMapNew = classNodes.readKotlinVisibilities().filterKeys(visibilityFilter)

  val sourceFilePackageReversedBySourceFile = sourceFiles.associateWith {
    it.parentFile.invariantSeparatorsPath.split('/').reversed()
  }

  return classNodes
    .filter { it != moduleInfo }
    .map { clazz ->
      with(clazz) {
        val metadata = kotlinMetadata

        val kmFunctions = when (val md = metadata) {
          is KotlinClassMetadata.Class -> md.kmClass.functions
          is KotlinClassMetadata.FileFacade -> md.kmPackage.functions
          is KotlinClassMetadata.MultiFileClassPart -> md.kmPackage.functions
          else -> null
        }.orEmpty()
        val jvmFunctionMap = kmFunctions.filter { it.signature != null }.associateBy { it.signature!! }

        val mVisibility = visibilityMapNew[name]
        val classAccess = AccessFlags(effectiveAccess and Opcodes.ACC_STATIC.inv())

        val supertypes = listOf(superName) - "java/lang/Object" + interfaces.sorted()

        val memberSignatures = (
          fields.map { field ->
            with(field) {
              FieldBinarySignature(
                jvmMember = JvmFieldSignature(name, desc),
                genericTypes = signature?.genericTypes().orEmpty(),
                annotations = visibleAnnotations.annotationTypes(),
                invisibleAnnotations = invisibleAnnotations.annotationTypes(),
                isPublishedApi = isPublishedApi(),
                access = AccessFlags(access)
              )
            }
          } + methods.map { method ->
            with(method) {
              val parameterAnnotations = visibleParameterAnnotations.orEmpty()
                .filterNotNull()
                .flatMap { annos ->
                  annos
                    .filterNotNull()
                    .mapNotNull { it.desc }
                }

              val typeAnnotations = visibleTypeAnnotations.orEmpty()
                .filterNotNull()
                .map { it.desc }

              MethodBinarySignature(
                jvmMember = JvmMethodSignature(name, desc),
                genericTypes = signature?.genericTypes().orEmpty(),
                annotations = visibleAnnotations.annotationTypes(),
                invisibleAnnotations = invisibleAnnotations.annotationTypes(),
                parameterAnnotations = parameterAnnotations,
                typeAnnotations = typeAnnotations,
                isPublishedApi = isPublishedApi(),
                access = AccessFlags(access),
                // nb: MethodNode.exceptions is NOT expressed as a type descriptor, rather as a path.
                // e.g., not `Lcom/example/Foo;`, but just `com/example/Foo`
                exceptions = exceptions
              )
            }
          }
          ).filter {
            it.isEffectivelyPublic(classAccess, exportedPackages?.contains(clazz.packageName()) ?: true, mVisibility)
          }

        val genericTypes = signature?.genericTypes().orEmpty()
          // Strip out JDK classes
          .filterNotToSet { it.startsWith("Ljava/lang") }

        val suspendReturnTypes = methods.mapNotNull { methodNode ->
          jvmFunctionMap[JvmMethodSignature(methodNode.name, methodNode.desc)]
        }.filter { kmFunction ->
          kmFunction.isSuspend
        }.flatMap { kmFunction ->
          extractClassesAsDescriptors(kmFunction.returnType)
        }.toSet()

        val sourceFileName = clazz.sourceFile ?: "${clazz.name.substringAfterLast('/')}."
        val clazzPackageReversed = clazz.name.substringBeforeLast('/').split('/').reversed()
        val sourceFile = sourceFilePackageReversedBySourceFile
          .filterKeys { it.name.startsWith(sourceFileName) }
          .maxByOrNull { (_, sourceFilePackageReversed) ->
            sourceFilePackageReversed
              .asSequence()
              .zip(clazzPackageReversed.asSequence())
              .takeWhile { (sourceFilePart, clazzPart) -> sourceFilePart == clazzPart }
              .count()
          }
          ?.key
          ?.invariantSeparatorsPath
          ?: sourceFileName

        ClassBinarySignature(
          name = name,
          superName = superName,
          outerName = outerClassName,
          supertypes = supertypes,
          genericTypes = genericTypes + suspendReturnTypes,
          memberSignatures = memberSignatures,
          access = classAccess,
          isEffectivelyPublic = isEffectivelyPublic(mVisibility),
          isNotUsedWhenEmpty = metadata.isFileOrMultipartFacade() || isDefaultImpls(metadata),
          annotations = visibleAnnotations.annotationTypes(),
          invisibleAnnotations = invisibleAnnotations.annotationTypes(),
          sourceFile = sourceFile
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

  fun ClassBinarySignature.isIncluded(): Boolean {
    return exclusions.includeAll() ||
      exclusions.includesClass(canonicalName) ||
      annotations.any(exclusions::includesAnnotation) ||
      invisibleAnnotations.any(exclusions::includesAnnotation) ||
      memberSignatures.any { it.annotations.any(exclusions::includesAnnotation) }
  }

  // Library note - this function (plus the exclusions parameter above) are modified from the original
  // Kotlin sources this was borrowed from.
  fun ClassBinarySignature.isExcluded(): Boolean {
    return (sourceFile?.let(exclusions::excludesPath) ?: false) ||
      exclusions.excludesClass(canonicalName) ||
      annotations.any(exclusions::excludesAnnotation) ||
      invisibleAnnotations.any(exclusions::excludesAnnotation) ||
      memberSignatures.any { it.annotations.any(exclusions::excludesAnnotation) }
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
    return this.copy(
      memberSignatures = memberSignatures + inheritedStaticSignatures,
      supertypes = supertypes - superName
    )
  }

  return filter {
    it.isIncluded() && !it.isExcluded() && it.isPublicAndAccessible()
  }.map {
    it.flattenNonPublicBases()
  }.filterNot {
    it.isNotUsedWhenEmpty && it.memberSignatures.isEmpty()
  }
}

internal fun List<ClassBinarySignature>.dump(): PrintStream = dump(to = System.out)

internal fun <T : Appendable> List<ClassBinarySignature>.dump(to: T): T = to.apply {
  this@dump.forEach { classBinarySig ->
    classBinarySig.annotations.forEach { anno ->
      appendReproducibleNewLine("@$anno")
    }
    append(classBinarySig.signature).appendReproducibleNewLine(" {")
    classBinarySig.memberSignatures.sortedWith(MEMBER_SORT_ORDER).forEach { memberBinarySig ->
      memberBinarySig.annotations.forEach { anno ->
        append("\t").appendReproducibleNewLine("@$anno")
      }
      append("\t").appendReproducibleNewLine(memberBinarySig.signature)
      if (memberBinarySig is MethodBinarySignature) {
        if (memberBinarySig.parameterAnnotations.isNotEmpty()) {
          appendReproducibleNewLine("\t- Parameter annotations:")
          memberBinarySig.parameterAnnotations.forEach { anno ->
            appendReproducibleNewLine("\t  - $anno")
          }
        }
        if (memberBinarySig.typeAnnotations.isNotEmpty()) {
          appendReproducibleNewLine("\t- Type annotations:")
          memberBinarySig.typeAnnotations.forEach { anno ->
            appendReproducibleNewLine("\t  - $anno")
          }
        }
      }
    }
    appendReproducibleNewLine("}\n")
  }
}

private fun extractClassesAsDescriptors(type: KmType): Set<String> {
  val classes = mutableSetOf<String>()

  when (val classifier = type.classifier) {
    is KmClassifier.Class -> classes.add("L${classifier.name};")
    is KmClassifier.TypeAlias -> classes.add("L${classifier.name};")

    // Type parameters are not concrete types, so we don't add them directly.
    // Their bounds might be relevant, but the requirement is to extract constituent types.
    is KmClassifier.TypeParameter -> Unit
  }

  type.arguments.forEach { projection ->
    projection.type?.let { argumentType ->
      classes.addAll(extractClassesAsDescriptors(argumentType))
    }
  }

  return classes
}
