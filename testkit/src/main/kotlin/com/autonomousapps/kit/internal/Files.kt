package com.autonomousapps.kit.internal

import java.io.File
import java.nio.charset.Charset

internal fun File.writeAny(any: Any, charset: Charset = Charsets.UTF_8): Unit {
  writeBytes(any.toString().toByteArray(charset))
}
