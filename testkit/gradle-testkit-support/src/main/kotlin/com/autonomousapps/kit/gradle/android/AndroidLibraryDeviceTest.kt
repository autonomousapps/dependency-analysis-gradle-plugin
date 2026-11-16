// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.android

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class AndroidLibraryDeviceTest(
  public val instrumentationRunner: String?,
  public val execution: String?,
) : Element.Block {

  override val name: String = "withDeviceTest"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    instrumentationRunner?.let { s.append("instrumentationRunner = $it") }
    execution?.let { s.append("execution = $it") }
  }

  public class Builder {
    public var instrumentationRunner: String? = null
    public var execution: String? = null

    public fun build(): AndroidLibraryDeviceTest {
      return AndroidLibraryDeviceTest(
        instrumentationRunner = instrumentationRunner,
        execution = execution,
      )
    }
  }
}
