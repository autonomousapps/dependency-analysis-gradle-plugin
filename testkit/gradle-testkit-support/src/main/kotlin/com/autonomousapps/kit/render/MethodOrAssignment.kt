// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.render

/**
 * Sometimes the differences between Groovy and Kotlin DSL are not just whether parentheses are optional or single- vs
 * double-quotes, but whether a property is configured via a method call or an assignment. For example, a repository
 * may be declared like so:
 *
 * ```
 * // valid Groovy syntax
 * maven { url '...' }
 * maven { url = '...' }
 * ```
 * ```
 * // valid Kotlin syntax
 * maven { url = uri("...") }
 * maven(url = "...")
 * ```
 */
internal class MethodOrAssignment {
  // TODO(tsr): use?
}
