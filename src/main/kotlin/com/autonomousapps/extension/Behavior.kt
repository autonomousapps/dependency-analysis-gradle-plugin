// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.extension

import java.io.Serializable

public sealed class Behavior(
  public val filter: Set<Exclusion> = setOf(),
  public val sourceSetName: String = Issue.ALL_SOURCE_SETS
) : Serializable, Comparable<Behavior> {

  /**
   * [Fail] > [Ignore] > [Warn] > [Undefined].
   */
  override fun compareTo(other: Behavior): Int {
    return when (other) {
      is Undefined -> {
        if (this is Undefined) 0 else 1
      }

      is Fail -> {
        if (this is Fail) 0 else -1
      }

      is Ignore -> {
        when (this) {
          is Fail -> 1
          is Ignore -> 0
          is Warn -> -1
          is Undefined -> -1
        }
      }

      is Warn -> {
        when (this) {
          is Fail -> 1
          is Ignore -> 1
          is Warn -> 0
          is Undefined -> -1
        }
      }
    }
  }
}

public class Fail(
  filter: Set<Exclusion> = mutableSetOf(),
  sourceSetName: String = Issue.ALL_SOURCE_SETS
) : Behavior(
  filter = filter,
  sourceSetName = sourceSetName
)

public class Warn(
  filter: Set<Exclusion> = mutableSetOf(),
  sourceSetName: String = Issue.ALL_SOURCE_SETS
) : Behavior(
  filter = filter,
  sourceSetName = sourceSetName
)

public class Ignore(
  sourceSetName: String = Issue.ALL_SOURCE_SETS
) : Behavior(
  sourceSetName = sourceSetName
)

public class Undefined(
  filter: Set<Exclusion> = mutableSetOf(),
  sourceSetName: String = Issue.ALL_SOURCE_SETS
) : Behavior(
  filter = filter,
  sourceSetName = sourceSetName
)
