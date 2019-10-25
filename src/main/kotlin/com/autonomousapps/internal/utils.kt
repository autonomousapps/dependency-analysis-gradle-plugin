package com.autonomousapps.internal

import java.util.Locale

internal fun String.capitalize() = substring(0, 1).toUpperCase(Locale.ROOT) + substring(1)
