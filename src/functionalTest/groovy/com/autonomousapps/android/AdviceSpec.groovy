package com.autonomousapps.android

import com.autonomousapps.android.projects.AdviceFilterProject
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build
import static com.autonomousapps.utils.Runner.buildAndFail
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class AdviceSpec extends AbstractAndroidSpec {

  @Unroll
  def "can filter unused dependencies (#gradleVersion AGP #agpVersion)"() {
    given:
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
    def result = buildAndFail(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'core tasks ran and were successful'
    result.task(':buildHealth').outcome == TaskOutcome.SUCCESS
    result.task(':failOrWarn').outcome == TaskOutcome.FAILED

    and: 'app advice does not include excludes'
    def buildHealth = project.actualBuildHealth()
    def appAdvice = buildHealth.find { it.projectPath == ':app' }.dependencyAdvice
    assertThat(appAdvice)
      .containsExactlyElementsIn(project.expectedAppAdvice(
        project.removeLibAndroid, project.removeCommonsIo
      ))

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  @Unroll
  def "can filter used transitive dependencies (#gradleVersion AGP #agpVersion)"() {
    given:
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
    def result = buildAndFail(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'core tasks ran and were successful'
    result.task(':buildHealth').outcome == TaskOutcome.SUCCESS
    result.task(':failOrWarn').outcome == TaskOutcome.FAILED

    and: 'app advice does not include excludes'
    def buildHealth = project.actualBuildHealth()
    def appAdvice = buildHealth.find { it.projectPath == ':app' }.dependencyAdvice
    assertThat(appAdvice)
      .containsExactlyElementsIn(project.expectedAppAdvice(project.addCommonsCollections))

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  @Unroll
  def "can filter incorrect configuration dependencies (#gradleVersion AGP #agpVersion)"() {
    given:
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
    def result = buildAndFail(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'core tasks ran and were successful'
    result.task(':buildHealth').outcome == TaskOutcome.SUCCESS
    result.task(':failOrWarn').outcome == TaskOutcome.FAILED
    def buildHealth = project.actualBuildHealth()

    and: 'app advice does not include excludes'
    def appAdvice = buildHealth.find { it.projectPath == ':app' }.dependencyAdvice
    assertThat(appAdvice)
      .containsExactlyElementsIn(project.expectedAppAdvice())

    and: 'lib_android advice does not include excludes'
    def libAndroidAdvice = buildHealth.find { it.projectPath == ':lib_android' }.dependencyAdvice
    assertThat(libAndroidAdvice)
      .containsExactlyElementsIn(project.expectedLibAndroidAdvice(project.changeAppcompat))

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  @Unroll
  def "can filter compileOnly dependencies (#gradleVersion AGP #agpVersion)"() {
    given:
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
    def result = build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'core tasks ran and were successful'
    result.task(':buildHealth').outcome == TaskOutcome.SUCCESS
    result.task(':failOrWarn').outcome == TaskOutcome.SUCCESS
    def buildHealth = project.actualBuildHealth()

    and: 'lib_android advice does not include excludes'
    def libAndroidAdvice = buildHealth.find { it.projectPath == ':lib_android' }.dependencyAdvice
    assertThat(libAndroidAdvice)
      .containsExactlyElementsIn(project.expectedLibAndroidAdvice(project.changeAndroidxAnnotation))

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  @Unroll
  def "can fail on unused annotation processors (#gradleVersion AGP #agpVersion)"() {
    given:
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
    def result = buildAndFail(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'core tasks ran and were successful'
    result.task(':buildHealth').outcome == TaskOutcome.SUCCESS
    result.task(':failOrWarn').outcome == TaskOutcome.FAILED
    def buildHealth = project.actualBuildHealth()

    and: 'lib_jvm advice does not include excludes'
    def libJvmAdvice = buildHealth.find { it.projectPath == ':lib_jvm' }.dependencyAdvice
    assertThat(libJvmAdvice).containsExactlyElementsIn(project.expectedLibJvmAdvice())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
