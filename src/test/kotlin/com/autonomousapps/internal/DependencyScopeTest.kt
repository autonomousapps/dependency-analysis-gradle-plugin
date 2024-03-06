// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class DependencyScopeTest {

  @Test fun `the source set for runtimeOnly is main`() {
    assertThat(DependencyScope.sourceSetName("runtimeOnly")).isEqualTo("main")
  }

  @Test fun `the source set for testImplementation is test`() {
    assertThat(DependencyScope.sourceSetName("testImplementation")).isEqualTo("test")
  }

  @Test fun `the source set for fooApi is foo`() {
    assertThat(DependencyScope.sourceSetName("fooApi")).isEqualTo("foo")
  }

  @Test fun `the source set for fooCompileOnly is foo`() {
    assertThat(DependencyScope.sourceSetName("fooCompileOnly")).isEqualTo("foo")
  }

  @Test fun `the source set for fooCompileOnlyApi is foo (and not fooCompileOnly)`() {
    assertThat(DependencyScope.sourceSetName("fooCompileOnlyApi")).isEqualTo("foo")
  }
}
