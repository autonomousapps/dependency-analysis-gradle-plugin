package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.CompileOnlyJarProject
import com.autonomousapps.jvm.projects.WarTestProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class CompileOnlyJarSpec extends AbstractJvmSpec {

  def "compileOnly file dependency should not be marked as transitive (#gradleVersion)"() {
    given:
    def project = new CompileOnlyJarProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':external:jar')
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  // The plugin cannot decide if something that is required for compilation is only needed at compile time.
  // Currently, such dependencies produce no advice at all. In the future the plugin could:
  // - Give an advice if one of these dependencies can be removed completely
  //   https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/648
  // - Give an advice id a dependency could be moved between 'compileOnly' <-> 'compileOnlyApi'
  //   (in projects where 'compileOnlyApi' exists)
  def "no advices for compileOnly, compileOnlyApi and providedCompile"() {
    given:
    def project = new WarTestProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
