// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.android

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class AndroidLibraryHostTest(
  public val enableCoverage: Boolean,
  public val isReturnDefaultValues: Boolean,
  public val isIncludeAndroidResources: Boolean,
) : Element.Block {

  override val name: String = "withHostTest"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    // 'false' is the default value for each, so we don't write that out
    if (enableCoverage) s.append("enableCoverage = ").append(true)
    if (isReturnDefaultValues) s.append("isReturnDefaultValues = ").append(true)
    if (isIncludeAndroidResources) s.append("isIncludeAndroidResources = ").append(true)
  }

  public class Builder {
    public var enableCoverage: Boolean = false
    public var isReturnDefaultValues: Boolean = false
    public var isIncludeAndroidResources: Boolean = false
    //targetSdk {}

    public fun build(): AndroidLibraryHostTest {
      return AndroidLibraryHostTest(
        enableCoverage = enableCoverage,
        isReturnDefaultValues = isReturnDefaultValues,
        isIncludeAndroidResources = isIncludeAndroidResources,
      )
    }
  }
}
