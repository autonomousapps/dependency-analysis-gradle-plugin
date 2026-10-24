// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates

internal data class ClassReference(
  val name: String,
  val kmpCommonSourceSetName: String?,
) : Comparable<ClassReference> {
  override fun compareTo(other: ClassReference): Int {
    return compareBy(ClassReference::name)
      .thenComparing(compareBy<ClassReference, String?>(nullsFirst()) { it.kmpCommonSourceSetName })
      .compare(this, other)
  }
}
