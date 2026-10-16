// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.extension

import com.google.common.truth.Truth.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class IssueTest {

  @Test fun `wildcard and regex exclusions can be combined across calls`() {
    val issue = ProjectBuilder.builder().build().objects.newInstance(Issue::class.java)
    issue.exclude("com.group:*", "com.example:exact")
    issue.exclude("com.other:*")
    issue.excludeRegex(".*:internal$")

    val filter = issue.behavior().get().filter
    assertThat(filter.anyMatches("com.group:foo")).isTrue()
    assertThat(filter.anyMatches("com.example:exact")).isTrue()
    assertThat(filter.anyMatches("com.other:artifact")).isTrue()
    assertThat(filter.anyMatches(":project:internal")).isTrue()
    assertThat(filter.anyMatches("com.example:other")).isFalse()
    assertThat(filter.anyMatches("com.example:exact:1.0")).isFalse()
  }
}
