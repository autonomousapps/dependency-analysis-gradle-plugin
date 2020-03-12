package com.autonomousapps

import com.autonomousapps.fixtures.*
import kotlin.Pair
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Shared
import spock.lang.Unroll

import static com.autonomousapps.fixtures.Dependencies.*
import static com.autonomousapps.fixtures.KotlinSources.CORE_KTX_LIB
import static com.autonomousapps.fixtures.Sources.DEFAULT_APP_SOURCES
import static com.autonomousapps.utils.Runner.build
import static com.autonomousapps.utils.Runner.buildAndFail

final class AndroidTests extends AbstractFunctionalTest {

  @Shared String agpVersion = System.getProperty("com.autonomousapps.agpversion")

  private ProjectDirProvider androidProject = null

  def cleanup() {
    if (androidProject != null) {
      clean(androidProject)
    }
  }

  @Unroll
  def "can configure java-only app module (gradle version #gradleVersion)"() {
    given:
    def project = new JavaOnlyAndroidProject(agpVersion)
    androidProject = project.newProject()

    expect:
    build(gradleVersion, androidProject, 'buildHealth')

    where:
    gradleVersion << gradleVersions(agpVersion)
  }

  @Unroll
  def "reports dependencies that could be compileOnly (gradle version #gradleVersion)"() {
    given:
    def project = new CompileOnlyTestProject(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdviceForApp = androidProject.adviceFor(project.appSpec)
    def expectedAdviceForApp = project.expectedAdviceForApp
    expectedAdviceForApp == actualAdviceForApp

    and:
    def actualAdviceForAndroidKotlinLib = androidProject.adviceFor(project.androidKotlinLib)
    def expectedAdviceForAndroidKotlinLib = project.expectedAdviceForAndroidKotlinLib
    expectedAdviceForAndroidKotlinLib == actualAdviceForAndroidKotlinLib

    and:
    def actualAdviceForJavaJvmLib = androidProject.adviceFor(project.javaJvmLib)
    def expectedAdviceForJavaJvmLib = project.expectedAdviceForJavaJvmLib
    expectedAdviceForJavaJvmLib == actualAdviceForJavaJvmLib

    where:
    gradleVersion << gradleVersions(agpVersion)
  }

  @Unroll
  def "finds constants in android-kotlin projects (gradle version #gradleVersion)"() {
    given:
    androidProject = AndroidConstantsProject.androidProjectThatUsesConstants(agpVersion)

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualUnusedDependencies = androidProject.unusedDependenciesFor("app")
    [] as List<String> == actualUnusedDependencies

    where:
    gradleVersion << gradleVersions(agpVersion)
  }

  @Unroll
  def "advice filters work (gradle version #gradleVersion)"() {
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
        }
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
    expectedAppAdvice == actualAppAdvice

    and: 'reports are as expected for lib_android'
    def expectedLibAndroidAdvice = NeedsAdviceProject.expectedLibAndroidAdvice([KOTLIN_STDLIB_JDK7_ID, CORE_ID] as Set<String>)
    def actualLibAndroidAdvice = androidProject.adviceFor('lib_android')
    expectedLibAndroidAdvice == actualLibAndroidAdvice

    and: 'reports are as expected for lib_jvm'
    def expectedLibJvmAdvice = NeedsAdviceProject.expectedLibJvmAdvice([KOTLIN_STDLIB_JDK7_ID, COMMONS_COLLECTIONS_ID] as Set<String>)
    def actualLibJvmAdvice = androidProject.adviceFor("lib_jvm")
    expectedLibJvmAdvice == actualLibJvmAdvice

    where:
    gradleVersion << gradleVersions(agpVersion)
  }

  @Unroll
  def "accurate advice can be given (gradle version #gradleVersion)"() {
    given:
    androidProject = NeedsAdviceProject.androidProjectThatNeedsAdvice(agpVersion, "")

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
    expectedAppAdvice == actualAppAdvice

    and: 'reports are as expected for lib_android'
    def expectedLibAndroidAdvice = NeedsAdviceProject.expectedLibAndroidAdvice([] as Set<String>)
    def actualLibAndroidAdvice = androidProject.adviceFor('lib_android')
    expectedLibAndroidAdvice == actualLibAndroidAdvice

    and: 'reports are as expected for lib_jvm'
    def expectedLibJvmAdvice = NeedsAdviceProject.expectedLibJvmAdvice([] as Set<String>)
    def actualLibJvmAdvice = androidProject.adviceFor('lib_jvm')
    expectedLibJvmAdvice == actualLibJvmAdvice

    where:
    gradleVersion << gradleVersions(agpVersion)
  }

  @Unroll
  def "plugin accounts for android resource usage (gradle version #gradleVersion)"() {
    given:
    def project = new AndroidResourceProject(agpVersion)
    def androidProject = project.newProject()

    when:
    def result = build(gradleVersion, androidProject, 'buildHealth')

    then:
    result.task(':buildHealth').outcome == TaskOutcome.SUCCESS

    and:
    def actualUnusedDepsForApp = androidProject.unusedDependenciesFor('app')
    def expectedUnusedDepsForApp = ['org.jetbrains.kotlin:kotlin-stdlib-jdk7']
    expectedUnusedDepsForApp == actualUnusedDepsForApp

    where:
    gradleVersion << gradleVersions(agpVersion)
  }

  @Unroll
  def "core ktx is a direct dependency (gradle version #gradleVersion)"() {
    given:
    def libName = 'lib'
    androidProject = new AndroidProject(
        agpVersion,
        new AppSpec(
            AppType.KOTLIN_ANDROID_APP,
            DEFAULT_APP_SOURCES,
            DEFAULT_APP_DEPENDENCIES
        ),
        [
            new LibrarySpec(
                libName,
                LibraryType.KOTLIN_ANDROID_LIB,
                [new Pair('implementation', 'androidx.core:core-ktx:1.1.0')],
                CORE_KTX_LIB
            )
        ],
        ''
    )

    when:
    def result = build(gradleVersion, androidProject, 'buildHealth')

    then: 'did the expected tasks run'
    result.task(":$libName:misusedDependenciesDebug").outcome == TaskOutcome.SUCCESS
    result.task(":$libName:abiAnalysisDebug").outcome == TaskOutcome.SUCCESS

    and: 'dependency reports as expected for lib'
    def actualCompletelyUnusedDepsForLib = androidProject.completelyUnusedDependenciesFor(libName)
    [] as List<String> == actualCompletelyUnusedDepsForLib

    and: 'dependency reports as expected for lib'
    def actualUnusedDependencies = androidProject.unusedDependenciesFor(libName)
    [] as List<String> == actualUnusedDependencies

    and: 'abi reports are correct'
    def actualAbi = androidProject.abiReportFor(libName)
    [] as List<String> == actualAbi

    where:
    gradleVersion << gradleVersions(agpVersion)
  }

  @Unroll
  def "buildHealth can be executed (gradle version #gradleVersion)"() {
    given:
    def project = new DefaultAndroidProject(agpVersion)
    androidProject = project.newProject()

    when:
    def result = build(gradleVersion, androidProject, 'buildHealth')

    then: 'expected tasks ran in root project'
    result.task(':abiReport').outcome == TaskOutcome.SUCCESS
    result.task(':misusedDependenciesReport').outcome == TaskOutcome.SUCCESS
    result.task(':adviceReport').outcome == TaskOutcome.SUCCESS
    result.task(':buildHealth').outcome == TaskOutcome.SUCCESS

    and: 'expected tasks ran in app project'
    result.task(":app:misusedDependenciesDebug").outcome == TaskOutcome.SUCCESS
    result.task(":app:adviceDebug").outcome == TaskOutcome.SUCCESS

    and: 'expected tasks ran in lib project'
    result.task(":lib:misusedDependenciesDebug").outcome == TaskOutcome.SUCCESS
    result.task(":lib:abiAnalysisDebug").outcome == TaskOutcome.SUCCESS
    result.task(":lib:adviceDebug").outcome == TaskOutcome.SUCCESS

    and: 'unused dependencies reports for app are correct'
    def actualUnusedDepsForApp = androidProject.completelyUnusedDependenciesFor('app')
    def expectedUnusedDepsForApp = [
        'androidx.constraintlayout:constraintlayout',
        'com.google.android.material:material'
    ]
    expectedUnusedDepsForApp == actualUnusedDepsForApp

    and: 'unused dependencies reports for lib are correct'
    def actualUnusedDepsForLib = androidProject.completelyUnusedDependenciesFor("lib")
    def expectedUnusedDepsForLib = ['androidx.constraintlayout:constraintlayout']
    expectedUnusedDepsForLib == actualUnusedDepsForLib

    and: 'abi reports are correct'
    def actualAbi = androidProject.abiReportFor('lib')
    def expectedAbi = ['androidx.core:core']
    expectedAbi == actualAbi

    where:
    gradleVersion << gradleVersions(agpVersion)
  }
}
