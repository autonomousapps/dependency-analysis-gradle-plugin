package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.android.AndroidColorRes
import com.autonomousapps.kit.android.AndroidStyleRes
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.appcompat
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.kotlinStdLib

final class LeakCanaryProject extends AbstractAndroidProject {

  private static final String LEAK_CANARY_VERSION = '2.14'

  private final String agpVersion
  final GradleProject gradleProject

  LeakCanaryProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('app') { subproject ->
        subproject.sources = appSources()
        subproject.styles = AndroidStyleRes.DEFAULT
        subproject.colors = AndroidColorRes.DEFAULT

        subproject.withBuildScript { buildScript ->
          buildScript.plugins(Plugins.androidApp, Plugins.kotlinAndroidNoVersion, Plugins.dependencyAnalysisNoVersion)
          buildScript.android = defaultAndroidAppBlock()
          buildScript.dependencies(
            kotlinStdLib('implementation'),
            appcompat('implementation'),
            new Dependency('debugImplementation', "com.squareup.leakcanary:leakcanary-android:$LEAK_CANARY_VERSION"),
          )
        }
      }
      .write()
  }

  private static List<Source> appSources() {
    return [
      Source.kotlin(
        '''\
          package com.autonomousapps.test
          
          import androidx.appcompat.app.AppCompatActivity
          
          class MainActivity : AppCompatActivity()
        '''
      )
        .withPath('com.autonomousapps.test', 'App') // TODO(tsr) delete line
        .build()
    ]
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private static Set<Advice> appAdvice() {
    def leakcanaryAndroid = moduleCoordinates('com.squareup.leakcanary:leakcanary-android', LEAK_CANARY_VERSION)
    def leakcanaryAndroidCore = moduleCoordinates('com.squareup.leakcanary:leakcanary-android-core', LEAK_CANARY_VERSION)
    return [
      // This is the advice I WANT to give. It was changed in the linked PR. What seems to be happening is the merged
      // manifest in the [debugMain, debugTest, debugAndroidTest] source all contain references to the style ref
      // `leak_canary_LeakCanary_Base` from `leakcanaryAndroidCore`. This makes them IMPL dependencies according to the
      // current analysis. So we have potentially two bugs:
      // 1. In android, both test and androidTest classpaths inherit from the main classpath, so we shouldn't see advice
      //    to add more declarations, especially given the current declaration is variant-specific (debugImplementation)
      // 2. I still think I'd prefer the advice be to change the declaration to debugRuntimeOnly. I should validate that
      //    "works," and then think about how I might change the analysis to support that conclusion.
      // PR: https://github.com/autonomousapps/dependency-analysis-gradle-plugin/pull/1431
      //Advice.ofChange(leakcanaryAndroid, 'debugImplementation', 'debugRuntimeOnly'),

      // This below is what doesn't seem right
      Advice.ofAdd(leakcanaryAndroidCore, 'debugImplementation'),
      Advice.ofAdd(leakcanaryAndroidCore, 'testImplementation'),
      Advice.ofAdd(leakcanaryAndroid, 'testImplementation'),
      Advice.ofAdd(leakcanaryAndroidCore, 'androidTestImplementation'),
      Advice.ofAdd(leakcanaryAndroid, 'androidTestImplementation'),
    ]
  }

  Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':app', appAdvice()),
  ]
}
