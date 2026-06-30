// Copyright (c) 2026. Tony Robalik.
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
 *
 * @see [Repository]
 */
public class Repositories @JvmOverloads constructor(
  public val repositories: MutableList<out Element> = mutableListOf(),
) : Element.Block {

  public interface Element {
    // Duplicating Scribe's primary interface method
    public fun render(scribe: Scribe): String
  }

  public constructor(vararg repositories: Element) : this(repositories.toMutableList())

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
