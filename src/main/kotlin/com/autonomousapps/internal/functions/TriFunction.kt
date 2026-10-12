/**
 * ============================================================================
 * (C) Copyright 2016 - 2026 Schalk W. Cronjé and/or respective authors as
 *      identified in documentation sections.
 *
 * This software is licensed under the Mozilla Public License 2.0
 * See https://www.mozilla.org/en-US/MPL/2.0/ for license details
 * ============================================================================
 */
package com.autonomousapps.internal.functions

import java.util.function.Function

/**
 * A Kotlin version of Grolifant's `TriFunction`.
 *
 * @see <a href="https://gitlab.com/ysb33rOrg/gradle/grolifant/-/blob/master/grolifant-core/src/main/groovy/org/ysb33r/grolifant5/api/core/functions/TriFunction.java?ref_type=heads">Grolifant</a>
 */
internal fun interface TriFunction<T, U, V, R> {
  fun apply(t: T, u: U, v: V): R

  fun <K> andThen(after: Function<in R, out K>): TriFunction<T, U, V, K> {
    return TriFunction { t: T, u: U, v: V -> after.apply(apply(t, u, v)) }
  }
}
