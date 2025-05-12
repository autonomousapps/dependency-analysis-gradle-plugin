package com.autonomousapps.tasks

import com.autonomousapps.model.declaration.internal.Bucket
import com.autonomousapps.model.internal.intermediates.Usage
import com.autonomousapps.model.source.JvmSourceKind
import com.autonomousapps.tasks.ReasonTask.ExplainDependencyAdviceAction.Companion.findFilteredDependencyKey
import org.gradle.api.InvalidUserDataException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ReasonTaskTest {

  private val usage = Usage(null, null, JvmSourceKind.MAIN, Bucket.NONE, emptySet())

  private val entries = mapOf(
    "demo-gradle-multi-module:list|:list" to usage,
    "demo-gradle-multi-module:list:sublist|:list:sublist" to usage,
    "demo-gradle-multi-module:list-default|:list-default" to usage,
    "demo-gradle-multi-module:list-impl|:list-impl" to usage,
    "com.squareup.okio:okio-jvm:3.0.0" to usage,
    "com.squareup.okio:okio:3.0.0" to usage
  ).entries

  @Test
  fun shouldThrowOnProjectModuleAmbiguity() {
    val ex = assertThrows(InvalidUserDataException::class.java) { findFilteredDependencyKey(entries, ":li") }
    assertEquals(
      "Coordinates ':li' matches more than 1 dependency [:list, :list:sublist, :list-default, :list-impl]", ex.message
    )
  }

  @Test
  fun shouldMatchEqualProjectModule() {
    val key = findFilteredDependencyKey(entries, ":list")
    assertEquals("demo-gradle-multi-module:list|:list", key)
  }

  @Test
  fun shouldMatchPrefixProjectModuleColon() {
    val key = findFilteredDependencyKey(entries, ":list:")
    assertEquals("demo-gradle-multi-module:list:sublist|:list:sublist", key)
  }

  @Test
  fun shouldMatchPrefixProjectModule() {
    val key = findFilteredDependencyKey(entries, ":list-d")
    assertEquals("demo-gradle-multi-module:list-default|:list-default", key)
  }

  @Test
  fun shouldThrowOnLibraryAmbiguity() {
    val ex = assertThrows(InvalidUserDataException::class.java) {
      findFilteredDependencyKey(entries, "com.squareup.okio:oki")
    }
    assertEquals(
      "Coordinates 'com.squareup.okio:oki' matches more than 1 dependency [com.squareup.okio:okio-jvm:3.0.0, com.squareup.okio:okio:3.0.0]",
      ex.message
    )
  }

  @Test
  fun shouldMatchEqualLibrary() {
    val key = findFilteredDependencyKey(entries, "com.squareup.okio:okio:3.0.0")
    assertEquals("com.squareup.okio:okio:3.0.0", key)
  }

  @Test
  fun shouldMatchPrefixLibraryColon() {
    val key = findFilteredDependencyKey(entries, "com.squareup.okio:okio:")
    assertEquals("com.squareup.okio:okio:3.0.0", key)
  }

  @Test
  fun shouldMatchLibraryVersion() {
    val key = findFilteredDependencyKey(entries, "com.squareup.okio:okio:3.0.0")
    assertEquals("com.squareup.okio:okio:3.0.0", key)
  }

  @Test
  fun shouldMatchPrefixLibrary() {
    val key = findFilteredDependencyKey(entries, "com.squareup.okio:okio-")
    assertEquals("com.squareup.okio:okio-jvm:3.0.0", key)
  }
}
