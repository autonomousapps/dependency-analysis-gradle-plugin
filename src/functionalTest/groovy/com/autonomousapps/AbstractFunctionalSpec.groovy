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

  protected static final GRADLE_7_0 = GradleVersion.version('7.0.2')
  protected static final GRADLE_7_1 = GradleVersion.version('7.1.1')
  protected static final GRADLE_7_2 = GradleVersion.version('7.2')
  protected static final GRADLE_7_3 = GradleVersion.version('7.3')

  private static final SUPPORTED_GRADLE_VERSIONS = [
    GRADLE_7_0,
    GRADLE_7_1,
    GRADLE_7_2,
    GRADLE_7_3,
  ]

  protected GradleProject gradleProject = null

  protected static Boolean quick() {
    return System.getProperty('com.autonomousapps.quick').toBoolean()
  }

  static Boolean isV1() {
    return System.getProperty('v') == '1'
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

//  List<Advice> actualAdvice(String projectName = null) {
//    if (projectName == null) {
//      return AdviceHelper.actualAdviceForFirstSubproject(gradleProject)
//    } else {
//      return AdviceHelper.actualAdviceForSubproject(gradleProject, projectName)
//    }
//  }

  ComprehensiveAdvice actualComprehensiveAdvice(String projectName) {
    return AdviceHelper.actualComprehensiveAdviceForProject(gradleProject, projectName)
  }

  String actualAdviceConsole() {
    return AdviceHelper.actualConsoleAdvice(gradleProject)
  }

  @SuppressWarnings('GroovyAssignabilityCheck')
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
    if (agpVersion >= AgpVersion.version('7.2.0')) {
      return gradleVersion >= GradleVersion.version('7.3')
    } else if (agpVersion >= AgpVersion.version('7.1.0')) {
      return gradleVersion >= GradleVersion.version('7.2')
    } else if (agpVersion >= AgpVersion.version('7.0.0')) {
      return gradleVersion >= GradleVersion.version('7.0')
    } else if (agpVersion >= AgpVersion.version('4.2.0')) {
      return gradleVersion >= GradleVersion.version('6.7')
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
