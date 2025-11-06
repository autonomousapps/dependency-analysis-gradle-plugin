// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.internal.intermediates.Usage

internal data class UsageContainer(val usages: List<UsagePair>) {
  fun toMap(): Map<Coordinates, Set<Usage>> {
    return usages.associate { (coordinates, usages) ->
      coordinates to usages
    }
  }

  companion object {
    fun of(map: Map<Coordinates, Set<Usage>>): UsageContainer {
      return UsageContainer(map.entries.map(UsagePair::of))
    }
  }

  internal data class UsagePair(val coordinates: Coordinates, val usages: Set<Usage>) {
    companion object {
      fun of(entry: Map.Entry<Coordinates, Set<Usage>>): UsagePair {
        return UsagePair(entry.key, entry.value)
      }
    }
  }
}
