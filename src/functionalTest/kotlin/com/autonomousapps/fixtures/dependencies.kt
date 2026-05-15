// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:JvmName("Dependencies")

package com.autonomousapps.fixtures

import com.autonomousapps.kit.gradle.dependencies.Plugins.KOTLIN_VERSION

const val KOTLIN_STDLIB_JDK7_ID = "org.jetbrains.kotlin:kotlin-stdlib-jdk7"
val KOTLIN_STDLIB_JDK7 = "$KOTLIN_STDLIB_JDK7_ID:${KOTLIN_VERSION}"

const val APPCOMPAT_ID = "androidx.appcompat:appcompat"
const val APPCOMPAT = "$APPCOMPAT_ID:1.1.0"
const val CORE_KTX_ID = "androidx.core:core-ktx"
const val CORE_KTX = "$CORE_KTX_ID:1.1.0"
const val CONSTRAINT_LAYOUT_ID = "androidx.constraintlayout:constraintlayout"
const val CONSTRAINT_LAYOUT = "$CONSTRAINT_LAYOUT_ID:1.1.3"
const val MATERIAL_ID = "com.google.android.material:material"
const val MATERIAL = "$MATERIAL_ID:1.0.0"
const val NAV_FRAGMENT_KTX_ID = "androidx.navigation:navigation-fragment-ktx"
const val NAV_FRAGMENT_KTX = "$NAV_FRAGMENT_KTX_ID:2.9.7"
const val NAV_UI_KTX_ID = "androidx.navigation:navigation-ui-ktx"
const val NAV_UI_KTX = "$NAV_UI_KTX_ID:2.9.7"
private val DEPENDENCIES_KOTLIN_STDLIB_JDK7 = listOf("implementation" to KOTLIN_STDLIB_JDK7)
val DEFAULT_APP_DEPENDENCIES = DEPENDENCIES_KOTLIN_STDLIB_JDK7 + listOf(
  "implementation" to APPCOMPAT,
  "implementation" to CORE_KTX,
  "implementation" to MATERIAL,
  "implementation" to CONSTRAINT_LAYOUT,
  "implementation" to NAV_FRAGMENT_KTX,
  "implementation" to NAV_UI_KTX
)

val DEFAULT_LIB_DEPENDENCIES = DEPENDENCIES_KOTLIN_STDLIB_JDK7 + listOf(
  "implementation" to APPCOMPAT,
  "implementation" to CORE_KTX,
  "implementation" to MATERIAL,
  "implementation" to CONSTRAINT_LAYOUT,
  "implementation" to NAV_FRAGMENT_KTX,
  "implementation" to NAV_UI_KTX
)
