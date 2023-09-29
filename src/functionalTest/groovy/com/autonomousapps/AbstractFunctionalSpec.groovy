package com.autonomousapps

import com.autonomousapps.fixtures.ProjectDirProvider
import com.autonomousapps.internal.GradleVersions
import com.autonomousapps.internal.android.AgpVersion
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.model.ProjectAdvice
import org.apache.commons.io.FileUtils
import org.gradle.util.GradleVersion
import spock.lang.Specification

import static com.autonomousapps.utils.DebugAware.debug

abstract class AbstractFunctionalSpec extends Specification {

  protected static final GRADLE_7_5 = GradleVersion.version('7.5.1')
  protected static final GRADLE_7_6 = GradleVersion.version('7.6.2')
  protected static final GRADLE_8_0 = GradleVersion.version('8.0.2')
  protected static final GRADLE_8_1 = GradleVersion.version('8.1.1')
  protected static final GRADLE_8_2 = GradleVersion.version('8.2.1')
  protected static final GRADLE_8_3 = GradleVersion.version('8.3')
  protected static final GRADLE_8_4 = GradleVersion.version('8.4-rc-2')

  // For faster CI times, we only test min + max. Testing all would be preferable, but we don't have till the heat death
  // of the universe to wait.
  protected static final SUPPORTED_GRADLE_VERSIONS = [
    GradleVersions.minGradleVersion,
    GRADLE_8_3,
  ]

  protected GradleProject gradleProject = null

  protected static Boolean quick() {
    return System.getProperty('com.autonomousapps.quick').toBoolean()
  }

  protected static void clean(ProjectDirProvider projectDirProvider) {
    clean(projectDirProvider.projectDir)
  }

  protected static void clean(File rootDir) {
    if (!isDebug()) {
      try {
        FileUtils.deleteDirectory(rootDir)
      } catch (FileNotFoundException e) {
        println("FileNotFoundException: ${e.localizedMessage}")
      }
    }
  }

  ProjectAdvice actualProjectAdvice(String projectName) {
    return AdviceHelper.actualProjectAdviceForProject(gradleProject, projectName)
  }

  Set<ProjectAdvice> actualProjectAdvice() {
    return AdviceHelper.actualProjectAdvice(gradleProject)
  }

  protected static boolean isCompatible(GradleVersion gradleVersion, AgpVersion agpVersion) {
    if (agpVersion >= AgpVersion.version('8.2.0')) {
      return gradleVersion >= GradleVersion.version('8.1')
    } else if (agpVersion >= AgpVersion.version('8.0.0')) {
      return gradleVersion >= GradleVersion.version('8.0')
    } else if (agpVersion >= AgpVersion.version('7.4.0')) {
      return gradleVersion >= GradleVersion.version('7.5')
    } else if (agpVersion >= AgpVersion.version('7.3.0')) {
      return gradleVersion >= GradleVersion.version('7.4')
    } else if (agpVersion >= AgpVersion.version('7.2.0')) {
      return gradleVersion >= GradleVersion.version('7.3')
    } else if (agpVersion >= AgpVersion.version('7.1.0')) {
      return gradleVersion >= GradleVersion.version('7.2') &&
        gradleVersion < GradleVersion.version('8.0-rc-1')
    } else if (agpVersion >= AgpVersion.version('7.0.0')) {
      return gradleVersion >= GradleVersion.version('7.0') &&
        gradleVersion < GradleVersion.version('8.0-rc-1')
    } else if (agpVersion >= AgpVersion.version('4.2.0')) {
      return gradleVersion >= GradleVersion.version('6.7') &&
        gradleVersion < GradleVersion.version('8.0-rc-1')
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
