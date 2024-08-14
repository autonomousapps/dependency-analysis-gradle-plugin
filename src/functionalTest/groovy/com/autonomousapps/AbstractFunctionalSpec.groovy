// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps

import com.autonomousapps.internal.GradleVersions
import com.autonomousapps.internal.android.AgpVersion
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.model.ProjectAdvice
import org.gradle.util.GradleVersion
import spock.lang.Specification

abstract class AbstractFunctionalSpec extends Specification {

  protected static final GRADLE_8_0 = GradleVersion.version('8.0.2')
  protected static final GRADLE_8_1 = GradleVersion.version('8.1.1')
  protected static final GRADLE_8_2 = GradleVersion.version('8.2.1')
  protected static final GRADLE_8_3 = GradleVersion.version('8.3')
  protected static final GRADLE_8_4 = GradleVersion.version('8.4')
  protected static final GRADLE_8_5 = GradleVersion.version('8.5')
  protected static final GRADLE_8_6 = GradleVersion.version('8.6')
  protected static final GRADLE_8_7 = GradleVersion.version('8.7')
  protected static final GRADLE_8_8 = GradleVersion.version('8.8')
  protected static final GRADLE_8_9 = GradleVersion.version('8.9')

  // For faster CI times, we only test min + max. Testing all would be preferable, but we don't have till the heat death
  // of the universe.
  protected static final SUPPORTED_GRADLE_VERSIONS = [
    GradleVersions.minGradleVersion,
    GRADLE_8_9,
  ]

  protected GradleProject gradleProject = null

  protected static Boolean quick() {
    return System.getProperty('com.autonomousapps.quick').toBoolean()
  }

  ProjectAdvice actualProjectAdvice(String projectName) {
    return AdviceHelper.actualProjectAdviceForProject(gradleProject, projectName)
  }

  Set<ProjectAdvice> actualProjectAdvice() {
    return AdviceHelper.actualProjectAdvice(gradleProject)
  }

  protected static boolean isCompatible(GradleVersion gradleVersion, AgpVersion agpVersion) {
    // See https://developer.android.com/build/releases/gradle-plugin#updating-gradle
    if (agpVersion >= AgpVersion.version('8.7.0')) {
      return gradleVersion >= GradleVersion.version('8.9')
    } else if (agpVersion >= AgpVersion.version('8.5.0')) {
      return gradleVersion >= GradleVersion.version('8.7')
    } else if (agpVersion >= AgpVersion.version('8.4.0')) {
      return gradleVersion >= GradleVersion.version('8.6')
    } else if (agpVersion >= AgpVersion.version('8.3.0')) {
      return gradleVersion >= GradleVersion.version('8.4')
    } else if (agpVersion >= AgpVersion.version('8.2.0')) {
      return gradleVersion >= GradleVersion.version('8.1')
    } else if (agpVersion >= AgpVersion.version('8.0.0')) {
      return gradleVersion >= GradleVersion.version('8.0')
    }

    throw new IllegalArgumentException("Unsupported AGP version supplied. Was $agpVersion")
  }

  /**
   * Testing against AGP versions listed in {@code AbstractAndroidSpec.SUPPORTED_AGP_VERSIONS}.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  protected static List<GradleVersion> gradleVersions() {
    def gradleVersions = SUPPORTED_GRADLE_VERSIONS

    // If a quick test is desired, return just the last combination
    if (quick()) {
      return [gradleVersions.last()]
    } else {
      return gradleVersions
    }
  }

  protected static List<GradleVersion> gradleVersionsSettingsApi() {
    return gradleVersions().findAll { it >= GRADLE_8_9 }
  }

  /**
   * For example, given:
   * 1. [GradleVersion(5.6.4), GradleVersion(6.0.1), GradleVersion(6.1.1)] (size=3)
   * 2. [true, false] (size=2)
   * ...
   * n. [another, list, of, items]
   *
   * I want to return:
   * [[GradleVersion(5.6.4), true], [GradleVersion(5.6.4), false], [GradleVersion(6.0.1), true], [GradleVersion(6.0.1), false], [GradleVersion(6.1.1), true], [GradleVersion(6.1.1), false]]
   *
   * @param pipes an iterable of pipes
   * @return a list of lists
   */
  protected static multivariableDataPipe(List<Object>... pipes) {
    return Arrays.asList(pipes).combinations()
  }
}
