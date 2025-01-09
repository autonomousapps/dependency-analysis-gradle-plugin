/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 *
 * Copied from https://github.com/JetBrains/kotlin/tree/master/libraries/tools/binary-compatibility-validator
 */

package com.autonomousapps.internal.kotlin

import kotlin.metadata.Visibility
import kotlin.metadata.jvm.JvmMemberSignature

internal class ClassVisibility(
  val name: String,
  val visibility: Visibility?,
  val isCompanion: Boolean,
  val members: Map<JvmMemberSignature, MemberVisibility>,
  val facadeClassName: String? = null,
) {

  var companionVisibilities: ClassVisibility? = null
  val partVisibilities = mutableListOf<ClassVisibility>()
}

internal fun ClassVisibility.findMember(signature: JvmMemberSignature): MemberVisibility? =
  members[signature] ?: partVisibilities.firstNotNullOfOrNull { it.members[signature] }

internal data class MemberVisibility(
  val member: JvmMemberSignature,
  val visibility: Visibility,
  val isReified: Boolean,
)

private fun isPublic(visibility: Visibility?, isPublishedApi: Boolean): Boolean {
  return visibility == null
    || visibility == Visibility.PUBLIC
    || visibility == Visibility.PROTECTED
    || (isPublishedApi && visibility == Visibility.INTERNAL)
}

internal fun ClassVisibility.isPublic(isPublishedApi: Boolean) = isPublic(visibility, isPublishedApi)

// Assuming isReified implies inline
internal fun MemberVisibility.isPublic(isPublishedApi: Boolean) = !isReified && isPublic(visibility, isPublishedApi)
