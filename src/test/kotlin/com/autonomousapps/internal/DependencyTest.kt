package com.autonomousapps.internal

import com.autonomousapps.advice.Dependency
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * The [Dependency] model object has custom `equals`, etc, and we want to preserve that behavior.
 */
class DependencyTest {

  private val orgDotSomethingGroup = "org.something"
  private val orgDotSomethingV1 = Dependency("$orgDotSomethingGroup:artifact", "1.0")
  private val orgDotSomethingV2 = Dependency("$orgDotSomethingGroup:artifact", "2.0")

  private val comDotSomethingV1 = Dependency("com.something:artifact", "1.0")

  private val projA = Dependency(":a")
  private val projB = Dependency(":b")

  @Test fun `compare by identifier - org is greater than com`() {
    assertThat(orgDotSomethingV1.compareTo(comDotSomethingV1)).isGreaterThan(0)
  }

  @Test fun `compare by identifier - com is less than org`() {
    assertThat(comDotSomethingV1.compareTo(orgDotSomethingV1)).isLessThan(0)
  }

  @Test fun `compare by version - higher is greater than lower`() {
    assertThat(orgDotSomethingV2.compareTo(orgDotSomethingV1)).isGreaterThan(0)
  }

  @Test fun `compare by version - lower is less than higher`() {
    assertThat(orgDotSomethingV1.compareTo(orgDotSomethingV2)).isLessThan(0)
  }

  @Test fun `external modules are greater than internal projects`() {
    assertThat(orgDotSomethingV1.compareTo(projA)).isGreaterThan(0)
  }

  @Test fun testToString() {
    assertThat(projA.toString()).isEqualTo(":a")
    assertThat(projB.toString()).isEqualTo(":b")
    assertThat(orgDotSomethingV1.toString()).isEqualTo("org.something:artifact:1.0")
  }

  @Test fun `identifier matters for equality`() {
    assertThat(orgDotSomethingV1).isNotEqualTo(comDotSomethingV1)
  }

  @Test fun `identifier matters for hashCode`() {
    assertThat(orgDotSomethingV1.hashCode()).isNotEqualTo(comDotSomethingV1.hashCode())
  }

  @Test fun `version matters for equality`() {
    assertThat(orgDotSomethingV1).isNotEqualTo(orgDotSomethingV2)
  }

  @Test fun `version matters for hashCode`() {
    assertThat(orgDotSomethingV1.hashCode()).isNotEqualTo(orgDotSomethingV2.hashCode())
  }

  @Test fun `different projects are not equal to each other`() {
    assertThat(projA).isNotEqualTo(projB)
  }

  @Test fun `external modules have groups`() {
    assertThat(orgDotSomethingV1.group).isEqualTo(orgDotSomethingGroup)
  }

  @Test fun `projects don't have groups`() {
    assertThat(projA.group).isEqualTo(null)
  }

  @Test fun `empty resolvedVersion is an error`() {
    assertThrows(IllegalStateException::class.java) {
      Dependency(identifier = "foo", resolvedVersion = "")
    }
  }
}