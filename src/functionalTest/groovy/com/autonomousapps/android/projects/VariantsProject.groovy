// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.Repository
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.testImplementation
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.junit

/** nb: This has to use the `com.github.YarikSOffice.venom:venom` dependency or the issue cannot be reproduced. */
final class VariantsProject extends AbstractAndroidProject {

  final GradleProject gradleProject

  private final androidTestCore = testImplementation('androidx.test:core:1.7.0')

  VariantsProject(String agpVersion) {
    super(agpVersion)
    this.gradleProject = build()
  }

  @SuppressWarnings('DuplicatedCode')
  private GradleProject build() {
    return newAndroidGradleProjectBuilder()
      .withRootProject { r ->
        // Add jitpack
        r.withSettingsScript { s ->
          s.withDependencyRepositories(Repository.JITPACK)
        }

        if (isAtLeastAgp9) {
          // See https://developer.android.com/build/releases/agp-9-0-0-release-notes#android-gradle-plugin-behavior-changes
          r.gradleProperties += GradleProperties.of('android.onlyEnableUnitTestForTheTestedBuildType=false')
        }
      }
      .withAndroidSubproject('app') { app ->
        app.sources = appSources
        app.manifest = AndroidManifest.appEmpty()
        app.withFile(
          'src/debug/AndroidManifest.xml',
          '''\
          <?xml version="1.0" encoding="utf-8"?>
          <manifest xmlns:android="http://schemas.android.com/apk/res/android">
            <application android:name=".DebugApplication" />
          </manifest>'''.stripIndent()
        )
        app.withBuildScript { bs ->
          bs.plugins = androidApp(false)
          bs.android = defaultAndroidAppBlock(false, 'example.app')
          bs.dependencies(
            junit('testImplementation'),
            androidTestCore,
            new Dependency('testReleaseImplementation', ':lib'),
            new Dependency('debugImplementation', 'com.github.YarikSOffice.venom:venom:0.7.1'),
          )
        }
      }
      .withSubproject('lib') { p ->
        p.withBuildScript { bs ->
          bs.plugins(javaLibrary)
        }
      }
      .write()
  }

  private List<Source> appSources = [
    Source.java(
      '''\
      package example.app;

      import android.app.Application;
      import com.github.venom.Venom;

      public class DebugApplication extends Application {
        @Override
        public void onCreate() {
          super.onCreate();
          Venom.createInstance(this);
        }
      }'''.stripIndent()
    )
      .withSourceSet('debug')
      .build(),
    Source.java(
      '''\
      package example.app;

      import org.junit.Test;
      
      public class ApplicationTest {
        @Test
        public void test() {}
      }
      '''.stripIndent()
    )
      .withSourceSet('test')
      .build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> appAdvice = [
    Advice.ofRemove(projectCoordinates(':lib'), 'testReleaseImplementation'),
    Advice.ofChange(moduleCoordinates(androidTestCore), 'testImplementation', 'testRuntimeOnly')
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':app', appAdvice),
    emptyProjectAdviceFor(':lib'),
  ]
}
