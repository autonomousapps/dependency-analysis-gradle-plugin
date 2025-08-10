// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.utils

final class Strings {

  private Strings() {
  }

  static String ensurePrefix(String string, String prefix = ':') {
    if (string.startsWith(prefix)) {
      return string
    } else {
      return prefix + string
    }
  }
}
