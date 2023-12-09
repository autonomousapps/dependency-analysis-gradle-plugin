/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 *
 * Copied from https://github.com/JetBrains/kotlin/tree/master/libraries/tools/binary-compatibility-validator
 */

package com.autonomousapps.internal.kotlin

import com.autonomousapps.internal.asm.tree.ClassNode
import kotlinx.metadata.*
import kotlinx.metadata.jvm.*

internal val ClassNode.kotlinMetadata: KotlinClassMetadata?
  get() {
    val metadata = findAnnotation("kotlin/Metadata", false) ?: return null

    @Suppress("UNCHECKED_CAST")
    val header = with(metadata) {
      Metadata(
        kind = get("k") as Int?,
        metadataVersion = (get("mv") as List<Int>?)?.toIntArray(),
        data1 = (get("d1") as List<String>?)?.toTypedArray(),
        data2 = (get("d2") as List<String>?)?.toTypedArray(),
        extraString = get("xs") as String?,
        packageName = get("pn") as String?,
        extraInt = get("xi") as Int?
      )
    }
    return KotlinClassMetadata.readLenient(header)
  }

internal fun KotlinClassMetadata?.isFileOrMultipartFacade() =
  this is KotlinClassMetadata.FileFacade || this is KotlinClassMetadata.MultiFileClassFacade

internal fun KotlinClassMetadata?.isSyntheticClass() = this is KotlinClassMetadata.SyntheticClass

internal fun KotlinClassMetadata.toClassVisibility(classNode: ClassNode): ClassVisibility {
  var visibility: Visibility? = null
  var isCompanion = false
  var _facadeClassName: String? = null
  val members = mutableListOf<MemberVisibility>()

  fun addMember(signature: JvmMemberSignature?, visibility: Visibility, isReified: Boolean) {
    if (signature != null) {
      members.add(MemberVisibility(signature, visibility, isReified))
    }
  }

  val container: KmDeclarationContainer? = when (this) {
    is KotlinClassMetadata.Class ->
      kmClass.also { klass ->
        visibility = klass.visibility
        isCompanion = klass.kind == ClassKind.COMPANION_OBJECT

        for (constructor in klass.constructors) {
          addMember(constructor.signature, constructor.visibility, isReified = false)
        }
      }

    is KotlinClassMetadata.FileFacade -> kmPackage
    is KotlinClassMetadata.MultiFileClassPart -> kmPackage.also { _facadeClassName = this.facadeClassName }
    else -> null
  }

  if (container != null) {
    fun List<KmTypeParameter>.containsReified() = any { it.isReified }

    for (function in container.functions) {
      addMember(function.signature, function.visibility, function.typeParameters.containsReified())
    }

    for (property in container.properties) {
      val isReified = property.typeParameters.containsReified()
      addMember(property.getterSignature, property.visibility, isReified)
      addMember(property.setterSignature, property.visibility, isReified)
      addMember(property.fieldSignature, property.visibility, isReified = false)
    }
  }

  return ClassVisibility(classNode.name, visibility, isCompanion, members.associateBy { it.member }, _facadeClassName)
}

internal fun ClassNode.toClassVisibility() = kotlinMetadata?.toClassVisibility(this)

internal fun Set<ClassNode>.readKotlinVisibilities(): Map<String, ClassVisibility> =
  mapNotNull { it.toClassVisibility() }
    .associateBy { it.name }
    .apply {
      values.asSequence().filter { it.isCompanion }.forEach {
        val containingClassName = it.name.substringBeforeLast('$')
        getValue(containingClassName).companionVisibilities = it
      }

      values.asSequence().filter { it.facadeClassName != null }.forEach {
        getValue(it.facadeClassName!!).partVisibilities.add(it)
      }
    }
