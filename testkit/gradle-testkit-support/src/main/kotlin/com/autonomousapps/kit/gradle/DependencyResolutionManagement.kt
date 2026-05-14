// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class DependencyResolutionManagement @JvmOverloads constructor(
  private val repositories: Repositories?,
  private val repositoriesMode: RepositoriesMode? = null,
  private val versionCatalogs: VersionCatalogs? = null,
) : Element.Block {

  public enum class RepositoriesMode : Element.Line {
    PREFER_PROJECT,
    PREFER_SETTINGS,
    FAIL_ON_PROJECT_REPOS,
    ;

    override fun render(scribe: Scribe): String = scribe.line {
      it.append("repositoriesMode = RepositoriesMode.")
      it.append(name)
    }
  }

  override val name: String = "dependencyResolutionManagement"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    repositoriesMode?.render(s)
    repositories?.render(s)
    versionCatalogs?.render(s)
  }

  public class Builder {
    public var repositories: Repositories = Repositories.DEFAULT_DEPENDENCIES
    public var repositoriesMode: RepositoriesMode? = null
    public var versionCatalogs: VersionCatalogs? = null

    public fun withRepositories(repositories: Repositories): Builder {
      this.repositories = repositories
      return this
    }

    public fun withRepositoriesMode(repositoriesMode: RepositoriesMode): Builder {
      this.repositoriesMode = repositoriesMode
      return this
    }

    public fun withVersionCatalogs(versionCatalogs: VersionCatalogs): Builder {
      this.versionCatalogs = versionCatalogs
      return this
    }

    public fun build(): DependencyResolutionManagement {
      return DependencyResolutionManagement(
        repositories = repositories,
        repositoriesMode = repositoriesMode,
        versionCatalogs = versionCatalogs,
      )
    }
  }

  public companion object {
    @JvmField
    public val DEFAULT: DependencyResolutionManagement = DependencyResolutionManagement(
      repositories = Repositories.DEFAULT_DEPENDENCIES,
    )
  }
}
