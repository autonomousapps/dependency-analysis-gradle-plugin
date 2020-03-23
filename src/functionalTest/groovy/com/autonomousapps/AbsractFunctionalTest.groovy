package com.autonomousapps

import com.autonomousapps.fixtures.ProjectDirProvider
import org.apache.commons.io.FileUtils
import org.gradle.util.GradleVersion
import spock.lang.Specification

import static com.autonomousapps.fixtures.Fixtures.WORKSPACE

abstract class AbstractFunctionalTest extends Specification {

  def setup() {
    FileUtils.deleteDirectory(new File(WORKSPACE))
  }

  /**
   * Testing against AGP versions:
   * - 3.5.3
   * - 3.6.1
   * - 4.0.0-beta01, whose min Gradle version is 6.1
   * - 4.1.0-alpha02, whose min Gradle version is 6.2.1
   */
  protected static List<GradleVersion> gradleVersions(String agpVersion = '') {
    if (agpVersion.startsWith('4.0.0')) {
      return [
          GradleVersion.version('6.1.1'),
          GradleVersion.version('6.2.2'),
          GradleVersion.version('6.3')
      ]
    } else if (agpVersion.startsWith('4.1.0')) {
      return [
          GradleVersion.version('6.2.2'),
          GradleVersion.version('6.3')
      ]
    } else {
      return [
          GradleVersion.version('5.6.4'),
          GradleVersion.version('6.0.1'),
          GradleVersion.version('6.1.1'),
          GradleVersion.version('6.2.2'),
          GradleVersion.version('6.3')
      ]
    }
  }

  protected static void clean(ProjectDirProvider projectDirProvider) {
    FileUtils.deleteDirectory(projectDirProvider.projectDir)
  }
}
