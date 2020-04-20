package com.autonomousapps.android

import com.autonomousapps.fixtures.AndroidConstantsProject
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build

final class ConstantsSpec extends AbstractAndroidSpec {

  @Unroll
  def "finds constants in android-kotlin projects (#gradleVersion AGP #agpVersion)"() {
    given:
    androidProject = AndroidConstantsProject.androidProjectThatUsesConstants(agpVersion)

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualUnusedDependencies = androidProject.unusedDependenciesFor("app")
    [] as List<String> == actualUnusedDependencies

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
