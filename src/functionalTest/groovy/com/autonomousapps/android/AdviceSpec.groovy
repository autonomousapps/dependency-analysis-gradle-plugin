// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.android.projects.AdviceFilterProject
import org.intellij.lang.annotations.Language

import static com.autonomousapps.utils.Runner.build
import static com.autonomousapps.utils.Runner.buildAndFail
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class AdviceSpec extends AbstractAndroidSpec {

  def "can filter unused dependencies (#gradleVersion AGP #agpVersion)"() {
    given:
    @Language("Groovy")
    def extension = """\
      dependencyAnalysis {
        issues {
          all {
            onUnusedDependencies {
              severity 'fail'
              exclude ':lib_android'
            }
          }
          project(':app') {
            onUnusedDependencies {
              exclude 'commons-io:commons-io'
            }
          }
        }
      }
    """
    def project = new AdviceFilterProject(
      agpVersion: agpVersion,
      rootAdditions: extension
    )
    gradleProject = project.gradleProject

    when:
    buildAndFail(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    def buildHealth = project.actualBuildHealth()
    def appAdvice = buildHealth.find { it.projectPath == ':app' }.dependencyAdvice
    assertThat(appAdvice)
      .containsExactlyElementsIn(project.expectedAppAdvice(
        project.removeLibAndroid, project.removeCommonsIo,
        // This is being filtered out by `RuntimeOnlyFilter` because it's still being brought in transitively by
        // :lib_android.
        project.addKotlinxCoroutinesAndroid, project.addKotlinxCoroutinesAndroidAgp9,
      ))

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "can filter used transitive dependencies (#gradleVersion AGP #agpVersion)"() {
    given:
    @Language("Groovy")
    def extension = """\
      dependencyAnalysis {
        issues {
          all {
            onUsedTransitiveDependencies {
              severity 'fail'
              exclude ':lib_android'
            }
          }
          project(':app') {
            onUsedTransitiveDependencies {
              exclude 'org.apache.commons:commons-collections4'
            }
          }
        }
      }
    """
    def project = new AdviceFilterProject(
      agpVersion: agpVersion,
      rootAdditions: extension
    )
    gradleProject = project.gradleProject

    when:
    buildAndFail(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    def buildHealth = project.actualBuildHealth()
    def appAdvice = buildHealth.find { it.projectPath == ':app' }.dependencyAdvice
    assertThat(appAdvice)
      .containsExactlyElementsIn(project.expectedAppAdvice(project.addCommonsCollections))

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "can filter incorrect configuration dependencies (#gradleVersion AGP #agpVersion)"() {
    given:
    @Language("Groovy")
    def extension = """\
      dependencyAnalysis {
        issues {
          all {
            onIncorrectConfiguration {
              severity 'fail'
              exclude 'androidx.annotation:annotation'
            }
          }
          project(':lib_android') {
            onIncorrectConfiguration {
              severity 'fail'
              exclude 'androidx.appcompat:appcompat'
            }
          }
        }
      }
    """
    def project = new AdviceFilterProject(
      agpVersion: agpVersion,
      rootAdditions: extension
    )
    gradleProject = project.gradleProject

    when:
    def result = buildAndFail(
      gradleVersion, gradleProject.rootDir,
      'buildHealth',
      'lib_android:reason', '--id', 'androidx.transition:transition:',
    )

    then:
    def buildHealth = project.actualBuildHealth()
    def appAdvice = buildHealth.find { it.projectPath == ':app' }.dependencyAdvice
    assertThat(appAdvice)
      .containsExactlyElementsIn(project.expectedAppAdvice())

    and: 'lib_android advice does not include excludes'
    def libAndroidAdvice = buildHealth.find { it.projectPath == ':lib_android' }.dependencyAdvice
    assertThat(libAndroidAdvice).containsExactlyElementsIn(project.expectedLibAndroidAdvice())
    assertThat(result.output).contains('* Accessed 1 time by reflection: (1) androidx.fragment:fragment:1.5.4 in class androidx.fragment.app.FragmentTransition: androidx.transition.FragmentTransitionSupport (implies runtimeOnly).')

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "can filter compileOnly dependencies (#gradleVersion AGP #agpVersion)"() {
    given:
    @Language("Groovy")
    def extension = """\
      dependencyAnalysis {
        issues {
          project(':lib_android') {
            onCompileOnly {
              severity 'fail'
              exclude 'androidx.annotation:annotation'
            }
          }
        }
      }
    """
    def project = new AdviceFilterProject(
      agpVersion: agpVersion,
      rootAdditions: extension
    )
    gradleProject = project.gradleProject

    when:
    buildAndFail(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    def buildHealth = project.actualBuildHealth()
    def libAndroidAdvice = buildHealth.find { it.projectPath == ':lib_android' }.dependencyAdvice
    assertThat(libAndroidAdvice)
      .containsExactlyElementsIn(project.expectedLibAndroidAdvice())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "can filter runtimeOnly dependencies (#gradleVersion AGP #agpVersion)"() {
    given:
    @Language("Groovy")
    def extension = """\
      dependencyAnalysis {
        issues {
          project(':app') {
            onRuntimeOnly {
              severity 'fail'
              exclude 'nl.littlerobots.rxlint:rxlint'
            }
          }
        }
      }
    """
    def project = new AdviceFilterProject(
      agpVersion: agpVersion,
      rootAdditions: extension
    )
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    def buildHealth = project.actualBuildHealth()
    def libAndroidAdvice = buildHealth.find { it.projectPath == ':app' }.dependencyAdvice
    assertThat(libAndroidAdvice)
      .containsExactlyElementsIn(project.expectedAppAdvice(project.changeRxlint))

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "can fail on unused annotation processors (#gradleVersion AGP #agpVersion)"() {
    given:
    @Language("Groovy")
    def extension = """\
      dependencyAnalysis {
        issues {
          project(':lib_jvm') {
            onUnusedAnnotationProcessors {
              severity 'fail'
            }
          }
        }
      }
    """
    def project = new AdviceFilterProject(
      agpVersion: agpVersion,
      rootAdditions: extension
    )
    gradleProject = project.gradleProject

    when:
    buildAndFail(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    def buildHealth = project.actualBuildHealth()
    def libJvmAdvice = buildHealth.find { it.projectPath == ':lib_jvm' }.dependencyAdvice
    assertThat(libJvmAdvice).containsExactlyElementsIn(project.expectedLibJvmAdvice())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
