// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates

import com.autonomousapps.model.Coordinates
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

// TODO any issues with Set<BundleTrace>?
@JsonClass(generateAdapter = false, generator = "sealed:type")
internal sealed class BundleTrace(
  val top: Coordinates,
  val bottom: Coordinates
) : Comparable<BundleTrace> {

  override fun compareTo(other: BundleTrace): Int {
    return when (this) {
      is DeclaredParent -> {
        when (other) {
          is DeclaredParent -> {
            compareBy(DeclaredParent::parent)
              .thenBy(DeclaredParent::child)
              .compare(this, other)
          }

          else -> 1
        }
      }

      is UsedChild -> {
        when (other) {
          is UsedChild -> {
            compareBy(UsedChild::parent)
              .thenBy(UsedChild::child)
              .compare(this, other)
          }

          is DeclaredParent -> -1
          else -> 1
        }
      }

      is PrimaryMap -> {
        when (other) {
          is PrimaryMap -> {
            compareBy(PrimaryMap::primary)
              .thenBy(PrimaryMap::subordinate)
              .compare(this, other)
          }

          else -> -1
        }
      }
    }
  }

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
