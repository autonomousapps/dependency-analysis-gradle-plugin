package com.autonomousapps.extension

import java.io.Serializable

sealed class Behavior(val filter: Set<String> = setOf()) : Serializable, Comparable<Behavior> {
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
          is Fail, Ignore -> 1
          is Warn -> 0
          is Undefined -> -1
        }
      }
    }
  }
}

class Fail(filter: Set<String> = mutableSetOf()) : Behavior(filter)
class Warn(filter: Set<String> = mutableSetOf()) : Behavior(filter)
object Ignore : Behavior()
class Undefined(filter: Set<String> = mutableSetOf()) : Behavior(filter)
