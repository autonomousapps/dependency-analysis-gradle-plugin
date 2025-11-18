// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.strings

/**
 * Replace all '/' characters with '.' characters, and also remove surrounding 'L' and ';'. I.e., convert from binary
 * representation to "human-readable" representation.
 *
 * The user-facing regex expects FQCNs to be delimited with dots, not slashes
 */
internal fun String.binaryToHuman() = dotty().removeSurrounding("L", ";")

/** Replace all '/' characters with '.' characters. */
internal fun String.dotty() = replace('/', '.')

/** Replace all '.' characters with '/' characters. */
internal fun String.slashy() = replace('.', '/')
