package com.autonomousapps.internal

import com.autonomousapps.advice.Dependency
import com.google.common.truth.Truth.assertThat
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

  // TODO test compareTo with dependency projects

  @Test fun testToString() {
    assertThat(projA.toString()).isEqualTo(":a")
    assertThat(projB.toString()).isEqualTo(":b")
    assertThat(orgDotSomethingV1.toString()).isEqualTo("org.something:artifact:1.0")
  }

  // TODO fix up
  @Test fun testEqualsAndHashCode() {
//    // equality should only depend on identifier
//    assertThat(orgDotSomethingV1).isEqualTo(orgDotSomethingV2)
//    // hash code should only depend on identifier
//    assertThat(orgDotSomethingV1.hashCode()).isEqualTo(orgDotSomethingV2.hashCode())
    // equality does not depend on the version
    assertThat(orgDotSomethingV1).isNotEqualTo(comDotSomethingV1)
    // hash code does not depend on version
    assertThat(orgDotSomethingV1.hashCode()).isNotEqualTo(comDotSomethingV1.hashCode())
    // project equality
    assertThat(projA).isNotEqualTo(projB)
  }

  @Test fun facade() {
    assertThat(orgDotSomethingV1.group).isEqualTo(orgDotSomethingGroup)
    assertThat(projA.group).isEqualTo(null)
  }
}