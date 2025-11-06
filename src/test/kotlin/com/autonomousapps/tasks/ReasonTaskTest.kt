// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.utils.intoSet
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.internal.declaration.Bucket
import com.autonomousapps.model.internal.intermediates.Usage
import com.autonomousapps.model.source.JvmSourceKind
import com.autonomousapps.tasks.ReasonTask.ExplainDependencyAdviceAction.Companion.findDependency
import com.google.common.truth.Truth.assertThat
import org.gradle.api.InvalidUserDataException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ReasonTaskTest {

  private val usage = Usage(null, null, JvmSourceKind.MAIN, Bucket.NONE, emptySet()).intoSet()

  private val entries = mapOf(
    Coordinates.of(":list") to usage,
    Coordinates.of(":list:sublist") to usage,
    Coordinates.of(":list-default") to usage,
    Coordinates.of(":list-impl") to usage,
    Coordinates.of("com.squareup.okio:okio-jvm:3.0.0") to usage,
    Coordinates.of("com.squareup.okio:okio:3.0.0") to usage
  ).entries

  @Test
  fun shouldThrowOnProjectModuleAmbiguity() {
    val ex = assertThrows(InvalidUserDataException::class.java) { findDependency(entries, ":li", "", ":") }
    assertThat(ex.message).isEqualTo("Coordinates ':li' matches more than 1 dependency: [:list, :list:sublist, :list-default, :list-impl]")
  }

  @Test
  fun shouldMatchEqualProjectModule() {
    val dependency = findDependency(entries, ":list", "", ":")
    assertThat(dependency).isNotNull()
    assertThat(dependency!!.gav()).isEqualTo(":list")
  }

  @Test
  fun shouldMatchPrefixProjectModuleColon() {
    val dependency = findDependency(entries, ":list:", "", ":")
    assertEquals(":list:sublist", dependency!!.gav())
  }

  @Test
  fun shouldMatchPrefixProjectModule() {
    val dependency = findDependency(entries, ":list-d", "", ":")
    assertEquals(":list-default", dependency!!.gav())
  }

  @Test
  fun shouldThrowOnLibraryAmbiguity() {
    val ex = assertThrows(InvalidUserDataException::class.java) {
      findDependency(entries, "com.squareup.okio:oki", "", ":")
    }
    assertEquals(
      "Coordinates 'com.squareup.okio:oki' matches more than 1 dependency: [com.squareup.okio:okio-jvm:3.0.0, com.squareup.okio:okio:3.0.0]",
      ex.message
    )
  }

  @Test
  fun shouldMatchEqualLibrary() {
    val dependency = findDependency(entries, "com.squareup.okio:okio:3.0.0", "", ":")
    assertEquals("com.squareup.okio:okio:3.0.0", dependency!!.gav())
  }

  @Test
  fun shouldMatchPrefixLibraryColon() {
    val dependency = findDependency(entries, "com.squareup.okio:okio:", "", ":")
    assertEquals("com.squareup.okio:okio:3.0.0", dependency!!.gav())
  }

  @Test
  fun shouldMatchLibraryVersion() {
    val dependency = findDependency(entries, "com.squareup.okio:okio:3.0.0", "", ":")
    assertEquals("com.squareup.okio:okio:3.0.0", dependency!!.gav())
  }

  @Test
  fun shouldMatchPrefixLibrary() {
    val dependency = findDependency(entries, "com.squareup.okio:okio-", "", ":")
    assertEquals("com.squareup.okio:okio-jvm:3.0.0", dependency!!.gav())
  }
}
