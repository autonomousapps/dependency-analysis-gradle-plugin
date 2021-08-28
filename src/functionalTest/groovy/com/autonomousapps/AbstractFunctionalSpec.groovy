package com.autonomousapps

import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.advice.Pebble
import com.autonomousapps.fixtures.ProjectDirProvider
import com.autonomousapps.internal.android.AgpVersion
import com.autonomousapps.kit.GradleProject
import org.apache.commons.io.FileUtils
import org.gradle.util.GradleVersion
import spock.lang.Specification

import static com.autonomousapps.utils.DebugAware.debug

abstract class AbstractFunctionalSpec extends Specification {

  protected static final GRADLE_6_1 = GradleVersion.version('6.1.1')
  protected static final GRADLE_6_9 = GradleVersion.version('6.9')
  protected static final GRADLE_7_1 = GradleVersion.version('7.1.1')
  protected static final GRADLE_7_2 = GradleVersion.version('7.2')

  private static final SUPPORTED_GRADLE_VERSIONS = [
    GRADLE_6_1,
//    GradleVersion.version('6.2.2'),
//    GradleVersion.version('6.3'),
//    GradleVersion.version('6.4.1'),
//    GradleVersion.version('6.5.1'),
//    GradleVersion.version('6.6.1'),
//    GradleVersion.version('6.7.1'),
//    GradleVersion.version('6.8.3'),
    GRADLE_6_9,
//    GradleVersion.version('7.0.2'),
//    GRADLE_7_1,
    GRADLE_7_2,
  ]

  protected GradleProject gradleProject = null

  protected static Boolean quick() {
    return System.getProperty("com.autonomousapps.quick").toBoolean()
  }

  protected static void clean(ProjectDirProvider projectDirProvider) {
    clean(projectDirProvider.projectDir)
  }

  protected static void clean(File rootDir) {
    if (!isDebug()) {
      FileUtils.deleteDirectory(rootDir)
    }
  }

  List<Advice> actualAdvice(String projectName = null) {
    if (projectName == null) {
      return AdviceHelper.actualAdviceForFirstSubproject(gradleProject)
    } else {
      return AdviceHelper.actualAdviceForSubproject(gradleProject, projectName)
    }
  }

  ComprehensiveAdvice actualComprehensiveAdvice(String projectName) {
    return AdviceHelper.actualComprehensiveAdviceForProject(gradleProject, projectName)
  }

  String actualAdviceConsole() {
    return AdviceHelper.actualConsoleAdvice(gradleProject)
  }

  List<ComprehensiveAdvice> actualBuildHealth() {
    return AdviceHelper.actualBuildHealth(gradleProject)
  }

  List<ComprehensiveAdvice> actualStrictBuildHealth() {
    return AdviceHelper.actualStrictBuildHealth(gradleProject)
  }

  List<ComprehensiveAdvice> actualMinimizedBuildHealth() {
    return AdviceHelper.actualMinimizedBuildHealth(gradleProject)
  }

  Pebble actualRipples() {
    return AdviceHelper.actualRipples(gradleProject)
  }

  protected static boolean isCompatible(GradleVersion gradleVersion, AgpVersion agpVersion) {
    if (agpVersion >= AgpVersion.version('7.0.0')) {
      return gradleVersion >= GradleVersion.version('7.0')
    } else if (agpVersion >= AgpVersion.version('4.2.0')) {
      return gradleVersion >= GradleVersion.version('6.7')
    } else if (agpVersion >= AgpVersion.version('4.1.0')) {
      return gradleVersion >= GradleVersion.version('6.5')
    } else {
      // AGP 4.1+ required for Gradle 7
      return gradleVersion.baseVersion < GradleVersion.version('7.0').baseVersion
    }
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
