package com.autonomousapps.android

import com.autonomousapps.AbstractFunctionalSpec
import com.autonomousapps.fixtures.ProjectDirProvider
import com.autonomousapps.internal.android.AgpVersion
import org.gradle.util.GradleVersion

abstract class AbstractAndroidSpec extends AbstractFunctionalSpec {

  protected ProjectDirProvider androidProject = null

  @SuppressWarnings('unused')
  def cleanup() {
    if (androidProject != null) {
      clean(androidProject)
    }
    if (gradleProject != null) {
      clean(gradleProject.rootDir)
    }
  }

  protected static final AGP_3_5 = AgpVersion.version('3.5.4')
  protected static final AGP_3_6 = AgpVersion.version('3.6.4')
  protected static final AGP_4_0 = AgpVersion.version('4.0.1')
  protected static final AGP_4_1 = AgpVersion.version('4.1.3')
  protected static final AGP_4_2 = AgpVersion.version('4.2.1')
  protected static final AGP_7_0 = AgpVersion.version('7.0.0-beta04')
  protected static final AGP_7_1 = AgpVersion.version('7.1.0-alpha02')

  /** Alias for latest supported version. Deliberately _not_ 7.1-alpha */
  protected static final LATEST_AGP_VERSION = AGP_7_0

  private static final SUPPORTED_AGP_VERSIONS = [
    AGP_3_5,
//    AGP_3_6,
//    AGP_4_0,
//    AGP_4_1,
    AGP_4_2,
    AGP_7_0,
//    AGP_7_1,
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
    // Cartesian product
    def matrix = Arrays.asList(gradleVersions(), agpVersions(minAgpVersion)).combinations()

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
