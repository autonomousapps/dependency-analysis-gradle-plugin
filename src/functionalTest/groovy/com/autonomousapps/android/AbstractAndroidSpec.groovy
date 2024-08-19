// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.AbstractFunctionalSpec
import com.autonomousapps.fixtures.ProjectDirProvider
import com.autonomousapps.internal.android.AgpVersion
import org.gradle.util.GradleVersion

/**
 * @see <a href="https://maven.google.com/web/index.html?#com.android.tools.build:gradle">AGP artifacts</a>
 */
abstract class AbstractAndroidSpec extends AbstractFunctionalSpec {

  protected ProjectDirProvider androidProject = null

  protected static final AGP_8_0 = AgpVersion.version('8.0.2')
  protected static final AGP_8_1 = AgpVersion.version('8.1.4')
  protected static final AGP_8_2 = AgpVersion.version('8.2.0')
  protected static final AGP_8_3 = AgpVersion.version('8.3.2')
  protected static final AGP_8_4 = AgpVersion.version('8.4.2')
  protected static final AGP_8_5 = AgpVersion.version('8.5.2')
  protected static final AGP_8_6 = AgpVersion.version('8.6.0-rc01')
  protected static final AGP_8_7 = AgpVersion.version('8.7.0-alpha06')

  protected static final AGP_LATEST = AGP_8_7

  /**
   * TODO(tsr): this doc is perpetually out of date.
   *
   * {@code AGP_8_0} represents the minimum stable _tested_ version. {@code AGP_8_5} represents the maximum stable
   * _tested_ version. We also test against the latest alpha, {@code AGP_8_7} at time of writing. DAGP may work with
   * other versions of AGP, but they aren't tested, primarily for CI performance reasons.
   *
   * @see <a href="https://maven.google.com/web/index.html?#com.android.tools.build:gradle">AGP releases</a>
   */
  protected static final SUPPORTED_AGP_VERSIONS = [
    AGP_8_0,
    AGP_8_5,
    AGP_8_7,
  ]

  protected static List<AgpVersion> agpVersions(AgpVersion minAgpVersion = AgpVersion.AGP_MIN) {
    List<AgpVersion> versions = SUPPORTED_AGP_VERSIONS
    versions.removeAll {
      it < minAgpVersion
    }

    if (quick()) {
      return [versions.last()]
    } else {
      return versions
    }
  }

  @SuppressWarnings(["GroovyAssignabilityCheck", "GrUnresolvedAccess"])
  protected static List<List<Object>> gradleAgpMatrix(AgpVersion minAgpVersion = AgpVersion.AGP_MIN) {
    return gradleAgpMatrix(gradleVersions(), minAgpVersion)
  }

  protected static List<List<Object>> gradleAgpMatrixSettingsApi(AgpVersion minAgpVersion = AgpVersion.AGP_MIN) {
    return gradleAgpMatrix(gradleVersionsSettingsApi(), minAgpVersion)
  }

  @SuppressWarnings(["GroovyAssignabilityCheck", "GrUnresolvedAccess"])
  protected static List<List<Object>> gradleAgpMatrix(
    List<GradleVersion> gradleVersions,
    AgpVersion minAgpVersion = AgpVersion.AGP_MIN
  ) {
    // Cartesian product
    def agpVersions = agpVersions(minAgpVersion)
    def matrix = Arrays.asList(gradleVersions, agpVersions).combinations()

    // Strip out incompatible combinations
    matrix.removeAll { m ->
      GradleVersion g = m[0] as GradleVersion
      AgpVersion a = m[1] as AgpVersion
      !isCompatible(g, a)
    }

    // Transform from AgpVersion to its string representation
    matrix = matrix.collect { [it[0], it[1].version] }

    // If a quick test is desired, return just the last combination
    if (quick()) {
      return [matrix.last()]
    } else {
      return matrix
    }
  }

  @SuppressWarnings(["GroovyAssignabilityCheck", "GrUnresolvedAccess"])
  protected static gradleAgpMatrixPlus(
    AgpVersion minAgpVersion = AgpVersion.AGP_MIN,
    List<Object>... others
  ) {
    // Cartesian product
    def matrix = Arrays.asList(gradleVersions(), agpVersions(minAgpVersion), *others).combinations()

    // Strip out incompatible combinations
    matrix.removeAll { m ->
      GradleVersion g = m[0] as GradleVersion
      AgpVersion a = m[1] as AgpVersion
      !isCompatible(g, a)
    }

    // Transform from AgpVersion to its string representation
    matrix = matrix.collect {
      it[1] = it[1].version
      it
    }

    // If a quick test is desired, return just the last combination
    if (quick()) {
      return [matrix.last()]
    } else {
      return matrix
    }
  }
}
