package com.autonomousapps.advice

import java.io.Serializable

/**
 * Associates a [variant] ("main", "debug", "release", ...) with a [filePath] (to a file such as
 * Java, Kotlin, or XML).
 */
data class VariantFile(
  val variant: String,
  val filePath: String
) : Serializable
