package com.autonomousapps

import com.autonomousapps.fixtures.ProjectDirProvider
import org.apache.commons.io.FileUtils
import org.gradle.util.GradleVersion
import spock.lang.Specification

import static com.autonomousapps.fixtures.Fixtures.WORKSPACE

abstract class AbstractFunctionalSpec extends Specification {

  protected static Boolean quick() {
    return System.getProperty("com.autonomousapps.quick").toBoolean()
  }

  protected static void clean(ProjectDirProvider projectDirProvider) {
    FileUtils.deleteDirectory(projectDirProvider.projectDir)
  }

  /**
   * Testing against AGP versions:
   * - 3.5.3
   * - 3.6.3
   * - 4.0.0-beta04, whose min Gradle version is 6.1
   * - 4.1.0-alpha06, whose min Gradle version is 6.2.1
   */
  protected static List<GradleVersion> gradleVersions(String agpVersion = '') {
    List<GradleVersion> versions

    if (agpVersion.startsWith('4.1.0')) {
      versions = [GradleVersion.version('6.3')]
    } else {
      versions = [
        GradleVersion.version('6.1.1'),
        GradleVersion.version('6.2.2'),
        GradleVersion.version('6.3')
      ]
    }

    if (quick()) {
      return [versions.last()]
    } else {
      return versions
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
