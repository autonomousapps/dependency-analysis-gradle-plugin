package com.autonomousapps.model.intermediates

import com.autonomousapps.model.Coordinates
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

// TODO any issues with Set<BundleTrace>?
@JsonClass(generateAdapter = false, generator = "sealed:type")
internal sealed class BundleTrace(
  val top: Coordinates,
  val bottom: Coordinates
) {

  @TypeLabel("parent")
  @JsonClass(generateAdapter = false)
  internal data class DeclaredParent(
    val parent: Coordinates,
    val child: Coordinates
  ) : BundleTrace(parent, child)

  @TypeLabel("child")
  @JsonClass(generateAdapter = false)
  internal data class UsedChild(
    val parent: Coordinates,
    val child: Coordinates
  ) : BundleTrace(parent, child)

  @TypeLabel("primary")
  @JsonClass(generateAdapter = false)
  internal data class PrimaryMap(
    val primary: Coordinates,
    val subordinate: Coordinates
  ) : BundleTrace(primary, subordinate)
}

