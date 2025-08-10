// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.utils

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Borrowed from Apache Commons IO and heavily modified.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
internal object FileUtils {

  /** The number of bytes in a kilobyte. */
  const val ONE_KB: Long = 1024

  /** The number of bytes in a kilobyte. */
  val ONE_KB_BI: BigDecimal = BigDecimal.valueOf(ONE_KB)

  /** The number of bytes in a megabyte. */
  const val ONE_MB: Long = ONE_KB * ONE_KB

  /** The number of bytes in a megabyte. */
  val ONE_MB_BI: BigDecimal = ONE_KB_BI.multiply(ONE_KB_BI)

  /** The number of bytes in a gigabyte. */
  const val ONE_GB: Long = ONE_KB * ONE_MB

  /** The number of bytes in a gigabyte. */
  val ONE_GB_BI: BigDecimal = ONE_KB_BI.multiply(ONE_MB_BI)

  /** The number of bytes in a terabyte. */
  const val ONE_TB: Long = ONE_KB * ONE_GB

  /** The number of bytes in a terabyte. */
  val ONE_TB_BI: BigDecimal = ONE_KB_BI.multiply(ONE_GB_BI)

  /** The number of bytes in a petabyte. */
  const val ONE_PB: Long = ONE_KB * ONE_TB

  /** The number of bytes in a petabyte. */
  val ONE_PB_BI: BigDecimal = ONE_KB_BI.multiply(ONE_TB_BI)

  /** The number of bytes in an exabyte. */
  const val ONE_EB: Long = ONE_KB * ONE_PB

  /** The number of bytes in an exabyte. */
  val ONE_EB_BI: BigDecimal = ONE_KB_BI.multiply(ONE_PB_BI)

  /** The number of bytes in a zettabyte. */
  val ONE_ZB: BigDecimal = BigDecimal.valueOf(ONE_KB).multiply(BigDecimal.valueOf(ONE_EB))

  /** The number of bytes in a yottabyte. */
  val ONE_YB: BigDecimal = ONE_KB_BI.multiply(ONE_ZB)

  internal enum class Scale(val divisor: BigDecimal) {
    B(BigDecimal.ONE),
    KiB(ONE_KB_BI),
    MiB(ONE_MB_BI),
    GiB(ONE_GB_BI),
    TiB(ONE_TB_BI),
    PiB(ONE_PB_BI),
    EiB(ONE_EB_BI),
    ZiB(ONE_ZB),
    YiB(ONE_YB),
    ;

    fun unit(): String = name
  }

  /**
   * Returns a human-readable version of the file size, where the input represents a specific number of bytes.
   *
   * If the size is over 1GB, the size is returned as the number of whole GB, i.e. the size is rounded down to the
   * nearest GB boundary.
   *
   * Similarly for the 1MB and 1KB boundaries.
   *
   * @param size the number of bytes
   * @return a human-readable display value (includes units - EB, PB, TB, GB, MB, KB or bytes)
   * @see [IO-226 - should the rounding be changed?](https://issues.apache.org/jira/browse/IO-226)
   */
  fun byteCountToDisplaySize(size: Long, scale: Scale? = null): String {
    val value = BigDecimal.valueOf(size).scaled()
    return if (scale == null) {
      byteCountToDisplaySize(value)
    } else {
      byteCountToDisplaySize(value, scale)
    }
  }

  /**
   * Returns a human-readable version of the file size, where the input represents a specific number of bytes.
   *
   * If the size is over 1GB, the size is returned as the number of whole GB, i.e. the size is rounded down to the
   * nearest GB boundary.
   *
   * Similarly for the 1MB and 1KB boundaries.
   *
   * @param size the number of bytes
   * @return a human-readable display value (includes units - EB, PB, TB, GB, MB, KB or bytes)
   * @throws NullPointerException if the given {@code BigDecimal} is {@code null}.
   * @see <a href="https://issues.apache.org/jira/browse/IO-226">IO-226 - should the rounding be changed?</a>
   */
  fun byteCountToDisplaySize(size: BigDecimal): String {
    val s = when {
      size.divide(ONE_EB_BI) > BigDecimal.ONE -> size.divide(ONE_EB_BI) to "EiB"
      size.divide(ONE_PB_BI) > BigDecimal.ONE -> size.divide(ONE_PB_BI) to "PiB"
      size.divide(ONE_TB_BI) > BigDecimal.ONE -> size.divide(ONE_TB_BI) to "TiB"
      size.divide(ONE_GB_BI) > BigDecimal.ONE -> size.divide(ONE_GB_BI) to "GiB"
      size.divide(ONE_MB_BI) > BigDecimal.ONE -> size.divide(ONE_MB_BI) to "MiB"
      size.divide(ONE_KB_BI) > BigDecimal.ONE -> size.divide(ONE_KB_BI) to "KiB"
      else -> size to "bytes"
    }

    return "${s.first.scaled()} ${s.second}"
  }

  fun byteCountToDisplaySize(size: BigDecimal, scale: Scale): String {
    val s = size.divide(scale.divisor) to scale.unit()
    return "${s.first.scaled()} ${s.second}"
  }

  fun getScale(size: Long): Scale = getScale(BigDecimal.valueOf(size).scaled())

  fun getScale(size: BigDecimal): Scale {
    val scaleDivisor = BigDecimal.valueOf(5)
    return when {
      size.divide(ONE_YB) > scaleDivisor -> Scale.YiB
      size.divide(ONE_ZB) > scaleDivisor -> Scale.ZiB
      size.divide(ONE_EB_BI) > scaleDivisor -> Scale.EiB
      size.divide(ONE_PB_BI) > scaleDivisor -> Scale.PiB
      size.divide(ONE_TB_BI) > scaleDivisor -> Scale.TiB
      size.divide(ONE_GB_BI) > scaleDivisor -> Scale.GiB
      size.divide(ONE_MB_BI) > scaleDivisor -> Scale.MiB
      size.divide(ONE_KB_BI) > scaleDivisor -> Scale.KiB
      else -> Scale.B
    }
  }

  private fun BigDecimal.scaled(): BigDecimal = setScale(2, RoundingMode.HALF_UP)
}
