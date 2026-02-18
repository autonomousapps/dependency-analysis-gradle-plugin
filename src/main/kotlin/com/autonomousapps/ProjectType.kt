// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps

/** A Gradle project (or module) can be one of these three types, for analysis purposes. */
public enum class ProjectType {
  ANDROID,
  JVM,
  KMP,
  ;

  internal companion object {
    fun of(isAndroidProject: Boolean, isKmpProject: Boolean): ProjectType {
      return if (isAndroidProject) {
        ANDROID
      } else if (isKmpProject) {
        KMP
      } else {
        JVM
      }
    }
  }
}
