// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

internal object ClassNames {

  fun canonicalize(className: String): String = className.replace('/', '.')

  fun isEnum(superClassName: String?): Boolean = superClassName == "java/lang/Enum"

  fun isSecurityProvider(superClassName: String?): Boolean = superClassName == "java/security/Provider"
}
