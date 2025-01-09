package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.CategoryProject

import static com.autonomousapps.utils.Runner.build

// This passes on Gradle 8.6 and failed on Gradle 8.7 (pre-fix)
// https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1159
final class CategorySpec extends AbstractJvmSpec {

  def "project bundles are respected (#gradleVersion)"() {
    given:
    def project = new CategoryProject()
    gradleProject = project.gradleProject

    expect:
    build(gradleVersion, gradleProject.rootDir, 'testCodeCoverageReport')

    where:
    gradleVersion << gradleVersions()
  }
}
