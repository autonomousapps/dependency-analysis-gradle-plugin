// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.artifacts.processing

import java.io.InputStream

internal class ClassFile(private val inputStreamSupplier: () -> InputStream, val packagePath: String) {

  fun readBytes(): ByteArray = inputStreamSupplier().use { it.readBytes() }
}
