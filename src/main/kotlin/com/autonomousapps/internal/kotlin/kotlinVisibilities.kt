// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0

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
