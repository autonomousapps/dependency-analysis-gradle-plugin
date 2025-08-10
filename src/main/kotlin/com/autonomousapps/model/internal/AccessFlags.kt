// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal

import com.autonomousapps.internal.asm.Opcodes

internal data class AccessFlags(val access: Int) {
  val isPublic: Boolean get() = isPublic(access)
  val isProtected: Boolean get() = isProtected(access)
  val isPrivate: Boolean get() = isPrivate(access)
  val isStatic: Boolean get() = isStatic(access)
  val isFinal: Boolean get() = isFinal(access)
  val isSynthetic: Boolean get() = isSynthetic(access)

  val isPublicConstant: Boolean get() = isPublic && isStatic && isFinal

  fun getModifiers(): List<String> = ACCESS_NAMES.entries.mapNotNull { if (access and it.key != 0) it.value else null }
  fun getModifierString(): String = getModifiers().joinToString(" ")

  companion object {
    val ACCESS_NAMES = mapOf(
      Opcodes.ACC_PUBLIC to "public",
      Opcodes.ACC_PROTECTED to "protected",
      Opcodes.ACC_PRIVATE to "private",
      Opcodes.ACC_STATIC to "static",
      Opcodes.ACC_FINAL to "final",
      Opcodes.ACC_ABSTRACT to "abstract",
      Opcodes.ACC_SYNTHETIC to "synthetic",
      Opcodes.ACC_INTERFACE to "interface",
      Opcodes.ACC_ANNOTATION to "annotation"
    )

    fun isPublic(access: Int) = access and Opcodes.ACC_PUBLIC != 0 || access and Opcodes.ACC_PROTECTED != 0
    fun isProtected(access: Int) = access and Opcodes.ACC_PROTECTED != 0
    fun isPrivate(access: Int) = access and Opcodes.ACC_PRIVATE != 0
    fun isStatic(access: Int) = access and Opcodes.ACC_STATIC != 0
    fun isFinal(access: Int) = access and Opcodes.ACC_FINAL != 0
    fun isSynthetic(access: Int) = access and Opcodes.ACC_SYNTHETIC != 0
  }
}
