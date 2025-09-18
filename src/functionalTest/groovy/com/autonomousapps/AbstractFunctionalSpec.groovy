// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps

import com.autonomousapps.internal.GradleVersions
import com.autonomousapps.internal.android.AgpVersion
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.model.ProjectAdvice
import org.gradle.util.GradleVersion
import spock.lang.Specification

abstract class AbstractFunctionalSpec extends Specification {

  @SuppressWarnings('unused')
  protected static final String FLAG_LOG_BYTECODE = "-D${Flags.BYTECODE_LOGGING}=true"

  protected static final GRADLE_LATEST = GradleVersions.maxGradleVersion

  // For faster CI times, we only test min + max. Testing all would be preferable, but we don't have till the heat death
  // of the universe.
  protected static final SUPPORTED_GRADLE_VERSIONS = [
    GradleVersions.minGradleVersion,
    GRADLE_LATEST,
  ]

  protected GradleProject gradleProject = null

  /**
   * <a href="https://docs.github.com/en/actions/writing-workflows/choosing-what-your-workflow-does/store-information-in-variables#default-environment-variables">Default environment variables on Github Actions</a>
   */
  private static boolean isCi = System.getenv("CI") == "true"

//  def cleanup() {
//    // Delete fixtures on CI to prevent disk space growing out of bounds
//    if (gradleProject != null && isCi) {
//      try {
//        gradleProject.rootDir.deleteDir()
//      } catch (Throwable t) {
//      }
//    }
//  }

  protected static Boolean quick() {
    return System.getProperty('com.autonomousapps.quick').toBoolean()
  }

  protected static List<String> reasonFor(String modulePath, String query) {
    return ["$modulePath:reason", '--id', query]
  }

  ProjectAdvice actualProjectAdvice(String projectName) {
    return AdviceHelper.actualProjectAdviceForProject(gradleProject, projectName)
  }

  Set<ProjectAdvice> actualProjectAdvice() {
    return AdviceHelper.actualProjectAdvice(gradleProject)
  }

  protected static boolean isCompatible(GradleVersion gradleVersion, AgpVersion agpVersion) {
    // See https://developer.android.com/build/releases/gradle-plugin#updating-gradle
    if (agpVersion >= AgpVersion.version('8.13.0')) {
      return gradleVersion >= GradleVersion.version('8.13')
    } else if (agpVersion >= AgpVersion.version('8.12.0')) {
      return gradleVersion >= GradleVersion.version('8.13')
    } else if (agpVersion >= AgpVersion.version('8.11.0')) {
      return gradleVersion >= GradleVersion.version('8.13')
    } else if (agpVersion >= AgpVersion.version('8.10.0')) {
      return gradleVersion >= GradleVersion.version('8.11.1')
    } else if (agpVersion >= AgpVersion.version('8.9.0')) {
      return gradleVersion >= GradleVersion.version('8.11.1')
    } else if (agpVersion >= AgpVersion.version('8.8.0')) {
      return gradleVersion >= GradleVersion.version('8.10.2')
    } else if (agpVersion >= AgpVersion.version('8.7.0')) {
      return gradleVersion >= GradleVersion.version('8.9')
    } else if (agpVersion >= AgpVersion.version('8.6.0')) {
      return gradleVersion >= GradleVersion.version('8.7')
    } else if (agpVersion >= AgpVersion.version('8.5.0')) {
      return gradleVersion >= GradleVersion.version('8.7')
    } else if (agpVersion >= AgpVersion.version('8.4.0')) {
      return gradleVersion >= GradleVersion.version('8.6')
    }

    throw new IllegalArgumentException("Unsupported AGP version supplied. Was $agpVersion")
  }

  /**
   * Testing against AGP versions:
   * - 3.5.4
   * - 3.6.4
   * - 4.0.1, whose min Gradle version is 6.1
   * - 4.1.0, whose min Gradle version is 6.5
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
