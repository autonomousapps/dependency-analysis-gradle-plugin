// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.extension

import com.google.common.truth.Truth.assertThat
import org.gradle.api.model.ObjectFactory
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class BundleHandlerTest {

  private val project = ProjectBuilder.builder().build()
  private val objects = project.objects
  private val bundleHandler = RealBundleHandler("test", objects)

  private class RealBundleHandler(name: String, objects: ObjectFactory) : BundleHandler(name, objects)

  @Test fun includeGroup() {
    // I like this better, but the IDE does not comprehend it and I can't stand the red squigglies.
//    val handler = DependenciesHandler(objects)
//    handler.bundle("test") {
//      includeGroup("org.jetbrains.kotlin")
//    }
//    val groupHandler = handler.bundles.getAt("test")
    bundleHandler.includeGroup("org.jetbrains.kotlin")

    assertThat(bundleHandler.anyMatch("org.jetbrains.kotlin:kotlin-stdlib")).isTrue()
    assertThat(bundleHandler.anyMatch("org.jetbrains.kotlin:kotlin-stdlib-jdk8")).isTrue()
    assertThat(bundleHandler.anyMatch("org.jetbrains.kotlin:kotlin-stdlib-jdk7")).isTrue()
    assertThat(bundleHandler.anyMatch("org.jetbrains:kotlin-stdlib")).isFalse()
  }

  @Test fun includeDependency() {
    bundleHandler.includeDependency("org.jetbrains.kotlin:kotlin-stdlib")
    bundleHandler.includeDependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    assertThat(bundleHandler.anyMatch("org.jetbrains.kotlin:kotlin-stdlib")).isTrue()
    assertThat(bundleHandler.anyMatch("org.jetbrains.kotlin:kotlin-stdlib-jdk8")).isTrue()
    assertThat(bundleHandler.anyMatch("org.jetbrains.kotlin:kotlin-stdlib-jdk7")).isFalse()
    assertThat(bundleHandler.anyMatch("org.jetbrains.kotlinx:kotlinx-coroutines-core")).isFalse()
  }

  @Test fun `include by string`() {
    bundleHandler.include("org.jetbrains.*")

    assertThat(bundleHandler.anyMatch("org.jetbrains.kotlin:kotlin-stdlib")).isTrue()
    assertThat(bundleHandler.anyMatch("org.jetbrains.kotlin:kotlin-stdlib-jdk8")).isTrue()
    assertThat(bundleHandler.anyMatch("org.jetbrains.kotlin:kotlin-stdlib-jdk7")).isTrue()
    assertThat(bundleHandler.anyMatch("org.jetbrains.kotlinx:kotlinx-coroutines-core")).isTrue()
    assertThat(bundleHandler.anyMatch("com.some:thing")).isFalse()
  }

  @Test fun `include by regex`() {
    bundleHandler.include("org.jetbrains.*".toRegex())

    assertThat(bundleHandler.anyMatch("org.jetbrains.kotlin:kotlin-stdlib")).isTrue()
    assertThat(bundleHandler.anyMatch("org.jetbrains.kotlin:kotlin-stdlib-jdk8")).isTrue()
    assertThat(bundleHandler.anyMatch("org.jetbrains.kotlin:kotlin-stdlib-jdk7")).isTrue()
    assertThat(bundleHandler.anyMatch("org.jetbrains.kotlinx:kotlinx-coroutines-core")).isTrue()
    assertThat(bundleHandler.anyMatch("com.some:thing")).isFalse()
  }

  private fun BundleHandler.anyMatch(input: CharSequence): Boolean {
    return includes.get().any { it.matches(input) }
  }
}
