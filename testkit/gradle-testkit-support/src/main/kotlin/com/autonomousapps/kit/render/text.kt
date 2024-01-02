// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:JvmName("Text")

package com.autonomousapps.kit.render

internal fun String.escape(): String = replace("\\", "\\\\")
