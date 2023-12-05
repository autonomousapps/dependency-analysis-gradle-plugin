@file:JvmName("Text")

package com.autonomousapps.kit.utils

internal fun String.escape(): String = replace("\\", "\\\\")
