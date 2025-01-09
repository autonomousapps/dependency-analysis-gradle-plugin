package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.AbiAnnotationsProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class ProjectGraphSpec extends AbstractJvmSpec {

  def "can generate project graph (#gradleVersion)"() {
    given:
    def project = new AbiAnnotationsProject(AbiAnnotationsProject.Target.CLASS)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':proj:generateProjectGraphMain')

    then:
    def compileGraph = gradleProject
      .singleArtifact('proj', 'reports/dependency-analysis/main/graph/project/project-compile-classpath.gv')
    def runtimeGraph = gradleProject
      .singleArtifact('proj', 'reports/dependency-analysis/main/graph/project/project-runtime-classpath.gv')
    
    assertThat(compileGraph.exists()).isTrue()
    assertThat(runtimeGraph.exists()).isTrue()

    where:
    gradleVersion << gradleVersions()
  }
}
