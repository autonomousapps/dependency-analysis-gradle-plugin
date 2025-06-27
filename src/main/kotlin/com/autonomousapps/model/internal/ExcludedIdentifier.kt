package com.autonomousapps.model.internal

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
internal data class ExcludedIdentifier(val identifier: String) : Comparable<ExcludedIdentifier> {
  override fun compareTo(other: ExcludedIdentifier): Int {
    return identifier.compareTo(other.identifier)
  }
}
