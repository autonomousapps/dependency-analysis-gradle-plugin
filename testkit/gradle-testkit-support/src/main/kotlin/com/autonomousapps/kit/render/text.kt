@file:JvmName("Text")

package com.autonomousapps.kit.render

internal fun String.escape(): String = replace("\\", "\\\\")
