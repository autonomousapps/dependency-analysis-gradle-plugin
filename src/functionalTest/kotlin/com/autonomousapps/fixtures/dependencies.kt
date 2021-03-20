@file:JvmName("Dependencies")

package com.autonomousapps.fixtures

const val KOTLIN_STDLIB_ID = "org.jetbrains.kotlin:kotlin-stdlib"
const val KOTLIN_STDLIB = "$KOTLIN_STDLIB_ID:1.5.21"
const val KOTLIN_STDLIB_JDK7_ID = "org.jetbrains.kotlin:kotlin-stdlib-jdk7"
const val KOTLIN_STDLIB_JDK7 = "$KOTLIN_STDLIB_JDK7_ID:1.5.21"
const val COMMONS_IO_ID = "commons-io:commons-io"
const val COMMONS_IO = "$COMMONS_IO_ID:2.6"
const val COMMONS_TEXT_ID = "org.apache.commons:commons-text"
const val COMMONS_TEXT = "$COMMONS_TEXT_ID:1.8"
const val COMMONS_COLLECTIONS_ID = "org.apache.commons:commons-collections4"
const val COMMONS_COLLECTIONS = "$COMMONS_COLLECTIONS_ID:4.4"
const val COMMONS_LANG3_ID = "org.apache.commons:commons-lang3"
const val COMMONS_LANG3 = "$COMMONS_LANG3_ID:3.9"
const val JETBRAINS_ANNOTATIONS_ID = "org.jetbrains:annotations"
const val JETBRAINS_ANNOTATIONS = "$JETBRAINS_ANNOTATIONS_ID:13.0"

const val APPCOMPAT_ID = "androidx.appcompat:appcompat"
const val APPCOMPAT = "$APPCOMPAT_ID:1.1.0"

// This is also brought in transitively by APPCOMPAT
const val ANDROIDX_ANNOTATIONS_ID = "androidx.annotation:annotation"
const val ANDROIDX_ANNOTATIONS = "$ANDROIDX_ANNOTATIONS_ID:1.1.0"
const val CORE_ID = "androidx.core:core"
const val CORE = "$CORE_ID:1.1.0"
const val CORE_KTX_ID = "androidx.core:core-ktx"
const val CORE_KTX = "$CORE_KTX_ID:1.1.0"
const val CONSTRAINT_LAYOUT_ID = "androidx.constraintlayout:constraintlayout"
const val CONSTRAINT_LAYOUT = "$CONSTRAINT_LAYOUT_ID:1.1.3"
const val MATERIAL_ID = "com.google.android.material:material"
const val MATERIAL = "$MATERIAL_ID:1.0.0"
const val NAV_FRAGMENT_KTX_ID = "androidx.navigation:navigation-fragment-ktx"
const val NAV_FRAGMENT_KTX = "$NAV_FRAGMENT_KTX_ID:2.1.0"
const val NAV_UI_KTX_ID = "androidx.navigation:navigation-ui-ktx"
const val NAV_UI_KTX = "$NAV_UI_KTX_ID:2.1.0"

// Annotation Processors
const val TP_COMPILER_ID = "com.github.stephanenicolas.toothpick:toothpick-compiler"
const val TP_COMPILER = "$TP_COMPILER_ID:3.1.0"

val DEPENDENCIES_KOTLIN_STDLIB = listOf("implementation" to KOTLIN_STDLIB_JDK7)

val DEFAULT_APP_DEPENDENCIES = DEPENDENCIES_KOTLIN_STDLIB + listOf(
    "implementation" to APPCOMPAT,
    "implementation" to CORE_KTX,
    "implementation" to MATERIAL,
    "implementation" to CONSTRAINT_LAYOUT,
    "implementation" to NAV_FRAGMENT_KTX,
    "implementation" to NAV_UI_KTX
)

val DEFAULT_LIB_DEPENDENCIES = DEPENDENCIES_KOTLIN_STDLIB + listOf(
    "implementation" to APPCOMPAT,
    "implementation" to CORE_KTX,
    "implementation" to MATERIAL,
    "implementation" to CONSTRAINT_LAYOUT,
    "implementation" to NAV_FRAGMENT_KTX,
    "implementation" to NAV_UI_KTX
)
