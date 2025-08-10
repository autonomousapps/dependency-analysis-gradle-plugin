// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.android.AndroidColorRes
import com.autonomousapps.kit.android.AndroidStyleRes
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.appcompat

final class AndroidAssetsProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  AndroidAssetsProject(String agpVersion) {
    super(agpVersion)

    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('app') { app ->
        app.withBuildScript { bs ->
          bs.plugins = [Plugins.androidApp, Plugins.dependencyAnalysisNoVersion]
          bs.android = defaultAndroidAppBlock(false)
          bs.dependencies = [
            appcompat('implementation'),
            project('implementation', ':assets'),
          ]
        }
        app.sources = sources
        app.styles = AndroidStyleRes.DEFAULT
        app.colors = AndroidColorRes.DEFAULT
      }
      .withAndroidLibProject('lib', 'com.example.lib') { lib ->
        lib.manifest = libraryManifest('com.example.lib')
        lib.withBuildScript { bs ->
          bs.plugins = [Plugins.androidLib, Plugins.dependencyAnalysisNoVersion]
          bs.android = defaultAndroidLibBlock(false, 'com.example.lib')
          bs.dependencies = [
            project('implementation', ':assets'),
          ]
        }
      }
      .withAndroidLibProject('assets', 'com.example.lib.assets') { assets ->
        assets.withBuildScript { bs ->
          bs.plugins = [Plugins.androidLib, Plugins.dependencyAnalysisNoVersion]
          bs.android = defaultAndroidLibBlock(false, 'com.example.lib.assets')
        }
        assets.withFile('src/main/assets/some_fancy_asset.txt',
          'https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/657')
        assets.manifest = libraryManifest('com.example.lib.assets')
      }
      .write()
  }

  private sources = [
    new Source(
      SourceType.JAVA, 'App', 'com/example',
      """\
        package com.example;
        
        import android.content.Context;
        import java.io.IOException;
        
        public class App {
        
          private Context context;
          
          private void accessAssets() throws IOException {
            context.getAssets().open("some_fancy_asset.txt");
          }
        }
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> appAdvice = [
    Advice.ofChange(projectCoordinates(':assets'), 'implementation', 'runtimeOnly'),
  ]

  private final Set<Advice> libAdvice = [
    Advice.ofRemove(projectCoordinates(':assets'), 'implementation')
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':app', appAdvice),
    projectAdviceForDependencies(':lib', libAdvice),
    emptyProjectAdviceFor(':assets'),
  ]
}
