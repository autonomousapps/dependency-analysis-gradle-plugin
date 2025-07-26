// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.exception

import org.gradle.api.tasks.VerificationException

public class BuildHealthException(msg: String) : VerificationException(msg)
