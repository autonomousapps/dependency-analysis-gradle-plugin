package com.autonomousapps.android

import com.autonomousapps.fixtures.NeedsAdviceProject
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Unroll

import static com.autonomousapps.fixtures.Dependencies.*
import static com.autonomousapps.utils.Runner.build
import static com.autonomousapps.utils.Runner.buildAndFail
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class AdviceSpec extends AbstractAndroidSpec {
  @Unroll
  def "advice filters work (#gradleVersion AGP #agpVersion)"() {
    given:
    def extension = """\
      dependencyAnalysis {
        issues {
          onAny {
              fail("$KOTLIN_STDLIB_JDK7_ID")
          }
          onUnusedDependencies {
              fail(":lib_android")
          }
          onUsedTransitiveDependencies {
              fail("$CORE_ID")
          }
          onIncorrectConfiguration {
              fail("$COMMONS_COLLECTIONS_ID")
          }
          onUnusedAnnotationProcessors {
              fail()
          }
        }
        setFacadeGroups()
      }
    """.stripIndent()
    androidProject = NeedsAdviceProject.androidProjectThatNeedsAdvice(agpVersion, extension)

    when:
    def result = buildAndFail(gradleVersion, androidProject, 'buildHealth')

    then: 'core tasks ran and were successful'
    result.task(':buildHealth').outcome == TaskOutcome.SUCCESS
    result.task(':failOrWarn').outcome == TaskOutcome.FAILED
    result.task(':app:adviceDebug').outcome == TaskOutcome.SUCCESS
    result.task(':lib_android:adviceDebug').outcome == TaskOutcome.SUCCESS
    result.task(':lib_jvm:adviceMain').outcome == TaskOutcome.SUCCESS

    and: 'reports are as expected for app'
    def expectedAppAdvice = NeedsAdviceProject.expectedAppAdvice([KOTLIN_STDLIB_JDK7_ID, ':lib_android'] as Set<String>)
    def actualAppAdvice = androidProject.adviceFor('app')
    assertThat(actualAppAdvice).containsExactlyElementsIn(expectedAppAdvice)

    and: 'reports are as expected for lib_android'
    def expectedLibAndroidAdvice = NeedsAdviceProject.expectedLibAndroidAdvice([KOTLIN_STDLIB_JDK7_ID, CORE_ID] as Set<String>)
    def actualLibAndroidAdvice = androidProject.adviceFor('lib_android')
    assertThat(actualLibAndroidAdvice).containsExactlyElementsIn(expectedLibAndroidAdvice)

    and: 'reports are as expected for lib_jvm'
    def expectedLibJvmAdvice = NeedsAdviceProject.expectedLibJvmAdvice([KOTLIN_STDLIB_JDK7_ID, COMMONS_COLLECTIONS_ID] as Set<String>)
    def actualLibJvmAdvice = androidProject.adviceFor("lib_jvm")
    assertThat(actualLibJvmAdvice).containsExactlyElementsIn(expectedLibJvmAdvice)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  @Unroll
  def "accurate advice can be given (#gradleVersion AGP #agpVersion)"() {
    given:
    def extension = """\
      dependencyAnalysis {
        setFacadeGroups()
      }
    """.stripIndent()
    androidProject = NeedsAdviceProject.androidProjectThatNeedsAdvice(agpVersion, extension)

    when:
    def result = build(gradleVersion, androidProject, 'buildHealth')

    then: 'core tasks ran and were successful'
    result.task(':buildHealth').outcome == TaskOutcome.SUCCESS
    result.task(':app:adviceDebug').outcome == TaskOutcome.SUCCESS
    result.task(':lib_android:adviceDebug').outcome == TaskOutcome.SUCCESS
    result.task(':lib_jvm:adviceMain').outcome == TaskOutcome.SUCCESS

    and: 'reports are as expected for app'
    def expectedAppAdvice = NeedsAdviceProject.expectedAppAdvice([] as Set<String>)
    def actualAppAdvice = androidProject.adviceFor('app')
    assertThat(actualAppAdvice).containsExactlyElementsIn(expectedAppAdvice)

    and: 'reports are as expected for lib_android'
    def expectedLibAndroidAdvice = NeedsAdviceProject.expectedLibAndroidAdvice([] as Set<String>)
    def actualLibAndroidAdvice = androidProject.adviceFor('lib_android')
    assertThat(actualLibAndroidAdvice).containsExactlyElementsIn(expectedLibAndroidAdvice)

    and: 'reports are as expected for lib_jvm'
    def expectedLibJvmAdvice = NeedsAdviceProject.expectedLibJvmAdvice([] as Set<String>)
    def actualLibJvmAdvice = androidProject.adviceFor('lib_jvm')
    assertThat(actualLibJvmAdvice).containsExactlyElementsIn(expectedLibJvmAdvice)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
