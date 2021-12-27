package com.autonomousapps.android

import com.autonomousapps.fixtures.*
import kotlin.Pair
import org.gradle.testkit.runner.TaskOutcome
import org.spockframework.runtime.extension.builtin.PreconditionContext
import spock.lang.IgnoreIf

import static com.autonomousapps.fixtures.Dependencies.getDEFAULT_APP_DEPENDENCIES
import static com.autonomousapps.fixtures.KotlinSources.getCORE_KTX_LIB
import static com.autonomousapps.fixtures.Sources.getDEFAULT_APP_SOURCES
import static com.autonomousapps.utils.Runner.build

// This test is far too old to be worth porting
@IgnoreIf({ PreconditionContext it -> it.sys.v == '2' })
final class CoreKtxSpec extends AbstractAndroidSpec {

  def "core ktx is a direct dependency (#gradleVersion AGP #agpVersion)"() {
    given:
    def libName = 'lib'
    def librarySpecs = [
      new LibrarySpec(
        libName,
        LibraryType.KOTLIN_ANDROID_LIB,
        false,
        [],
        [new Pair('implementation', 'androidx.core:core-ktx:1.1.0')],
        CORE_KTX_LIB
      )
    ]
    androidProject = new AndroidProject(
      new RootSpec(
        librarySpecs, "", RootSpec.defaultGradleProperties(), agpVersion as String,
        RootSpec.defaultSettingsScript(agpVersion, librarySpecs),
        RootSpec.defaultBuildScript(agpVersion, "")
      ),
      new AppSpec(
        AppType.KOTLIN_ANDROID_APP,
        DEFAULT_APP_SOURCES,
        [] as Set<AndroidLayout>,
        DEFAULT_APP_DEPENDENCIES
      ),
      [
        new LibrarySpec(
          libName,
          LibraryType.KOTLIN_ANDROID_LIB,
          false,
          [],
          [new Pair('implementation', 'androidx.core:core-ktx:1.1.0')],
          CORE_KTX_LIB
        )
      ]
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
    // Kotlin plugin adds this dependency. This is a legacy test that looks at an intermediate output, not the final advice output
    ['org.jetbrains.kotlin:kotlin-stdlib-jdk8'] as List<String> == actualUnusedDependencies

    and: 'abi reports are correct'
    def actualAbi = androidProject.abiReportFor(libName)
    ['org.jetbrains.kotlin:kotlin-stdlib'] as List<String> == actualAbi

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
