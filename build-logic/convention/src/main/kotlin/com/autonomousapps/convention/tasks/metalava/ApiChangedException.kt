// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.convention.tasks.metalava

import java.lang.RuntimeException

public class ApiChangedException(msg: String, cause: Throwable) : RuntimeException(msg, cause)
