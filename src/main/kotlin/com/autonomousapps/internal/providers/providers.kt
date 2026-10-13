// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.providers

import org.gradle.api.provider.Provider

/** For human-readable error messages. */
internal fun Provider<*>.isAvailable(): String = if (isPresent) "available" else "not available"
