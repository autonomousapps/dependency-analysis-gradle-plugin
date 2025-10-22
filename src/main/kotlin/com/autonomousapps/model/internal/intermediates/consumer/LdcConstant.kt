// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates.consumer

/**
 * This class corresponds to the
 * [`ldc` bytecode instruction](https://docs.oracle.com/javase/specs/jvms/se24/html/jvms-6.html#jvms-6.5.ldc), hence the
 * name. A constant has a name, a value, and a descriptor (e.g., `Ljava/lang/String;`, `I`, etc.).
 *
 * Unfortunately, due to constant inlining, the name of the constant will not be available in consumer bytecode. This
 * class is used in concert with the `Constant` class, along with source code parsing, in a heuristic to detect usage
 * of inlined constants.
 *
 * @see [com.autonomousapps.model.internal.intermediates.producer.Constant]
 */
internal data class LdcConstant(
  val descriptor: String,
  val value: String,
) : Comparable<LdcConstant> {

  override fun compareTo(other: LdcConstant): Int {
    return compareBy(LdcConstant::descriptor)
      .thenBy(LdcConstant::value)
      .compare(this, other)
  }

  companion object {
    const val INT = "I"
    const val LONG = "J"
    const val FLOAT = "F"
    const val DOUBLE = "D"
    const val STRING = "Ljava/lang/String;"

    fun of(value: Any?): LdcConstant {
      return LdcConstant(
        descriptor = asDescriptor(value),
        value = value.toString(),
      )
    }

    /**
     * We do this because the producer side gives us descriptors, and that's a simpler canonical form for equivalence-
     * testing.
     */
    private fun asDescriptor(value: Any?): String {
      requireNotNull(value)

      return when (val canonicalName = value.javaClass.canonicalName) {
        "java.lang.Integer" -> INT
        "java.lang.Long" -> LONG
        "java.lang.Float" -> FLOAT
        "java.lang.Double" -> DOUBLE
        "java.lang.String" -> STRING

        // Identical to the String case, but semantically different. Just need a default.
        else -> "L$canonicalName;"
      }
    }
  }
}
