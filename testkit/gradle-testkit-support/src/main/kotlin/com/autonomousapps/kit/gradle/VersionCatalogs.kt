// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

/**
 * dependencyResolutionManagement {
 *   versionCatalogs {
 *     create("libs") {
 *       from(files("..."))
 *     }
 *   }
 * }
 */
public class VersionCatalogs(
  public val versionCatalogs: List<VersionCatalog>
) : Element.Block {

  override val name: String = "versionCatalogs"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    versionCatalogs.forEach { catalog ->
      catalog.render(s)
    }
  }

  public companion object {
    @JvmStatic
    public fun of(vararg versionCatalogs: VersionCatalog): VersionCatalogs {
      return VersionCatalogs(versionCatalogs.toList())
    }

    /**
     * ```
     * VersionCatalogs.of(
     *   "myLibs" to "gradle/my-libs.versions.toml",
     *   "myOtherLibs" to "gradle/my-other-libs.versions.toml"
     * )
     * ```
     */
    @JvmStatic
    public fun of(vararg versionCatalogs: Pair<String, String>): VersionCatalogs {
      return VersionCatalogs(versionCatalogs.map { VersionCatalog(it.first, it.second) })
    }
  }
}
