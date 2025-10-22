// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

/**
 * ```
 * // Groovy DSL
 * repositories {
 *   maven { url 'https://repo.spring.io/release' }
 * }
 *
 * // Kotlin DSL
 * repositories {
 *   // 1
 *   maven { url = uri("https://repo.spring.io/release") }
 *
 *   // 2
 *   maven(url = "https://repo.spring.io/release")
 * }
 * ```
 */
public class Repositories @JvmOverloads constructor(
  public val repositories: MutableList<Repository> = mutableListOf(),
) : Element.Block {

  public constructor(vararg repositories: Repository) : this(repositories.toMutableList())

  public val isEmpty: Boolean = repositories.isEmpty()

  override val name: String = "repositories"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    repositories.forEach { it.render(s) }
  }

  public operator fun plus(other: Repositories): Repositories {
    return Repositories((repositories + other.repositories).distinct().toMutableList())
  }

  public operator fun plus(other: Iterable<Repository>): Repositories {
    return Repositories((repositories + other).distinct().toMutableList())
  }

  public companion object {
    @JvmField
    public val EMPTY: Repositories = Repositories(mutableListOf())

    @JvmField
    public val DEFAULT_DEPENDENCIES: Repositories = Repositories(Repository.DEFAULT.toMutableList())

    @JvmField
    public val DEFAULT_PLUGINS: Repositories = Repositories(Repository.DEFAULT_PLUGINS.toMutableList())
  }
}
