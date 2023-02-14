package com.autonomousapps.model

import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = false)
data class GradleVariantIdentification(
  val capabilities: Set<String>,
  val attributes: Map<String, String>
  // classifier: String
): Serializable, Comparable<GradleVariantIdentification> {
  override fun compareTo(other: GradleVariantIdentification): Int {
    return toSingleString().compareTo(other.toSingleString())
  }

  private fun toSingleString() =
    capabilities.joinToString() + attributes.map { (k, v) -> "$k=$v" }.joinToString()
}
