// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.android.AndroidColorRes
import com.autonomousapps.kit.android.AndroidStringRes
import com.autonomousapps.kit.android.AndroidStyleRes
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ModuleAdvice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.appcompat
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.commonsCollections

/**
 * app (is android and should be)
 * +--- assets (is android and should be)
 * +--- lib-android-java-deps (is android and shouldn't be)
 *      \--- lib-java
 * +--- lib-android-android-deps (is android and should be)
 *      \--- assets
 * \--- lib-java (is java and not a candidate)
 */
final class CouldBeAndroidProject extends AbstractAndroidProject {

  enum ExpectedResult {
    WARN('severity("warn")'),
    FAIL('severity("fail")'),
    IGNORE('severity("ignore")'),
    IGNORE_ANDROID('exclude("android")');

    final issue

    ExpectedResult(String issue) {
      this.issue = issue
    }
  }

  final GradleProject gradleProject
  private final String agpVersion
  private final ExpectedResult expectedResult

  CouldBeAndroidProject(String agpVersion, ExpectedResult expectedResult) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.expectedResult = expectedResult
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withRootProject { root ->
        root.withBuildScript { bs ->
          bs.withGroovy("""\
          dependencyAnalysis {
            issues {
              all {
                onModuleStructure {
                  ${expectedResult.issue}
                }
              }
            }
          }""")
        }
      }
      .withAndroidSubproject('app') { app ->
        app.withBuildScript { bs ->
          bs.plugins = androidAppPlugin
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
      .withAndroidLibProject('assets', 'com.example.lib.assets') { assets ->
        assets.withBuildScript { bs ->
          bs.plugins = androidLibPlugin
          bs.android = defaultAndroidLibBlock(false, 'com.example.lib.assets')
        }
        assets.withFile(
          'src/main/assets/some_fancy_asset.txt',
          'https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/657'
        )
        assets.strings = AndroidStringRes.DEFAULT
      }
      .withAndroidLibProject('lib-android-java-deps', 'com.example.lib') { lib ->
        lib.withBuildScript { bs ->
          bs.plugins = androidLibPlugin
          bs.android = defaultAndroidLibBlock(false, 'com.example.lib')
          bs.dependencies = [
            project('implementation', ':lib-java'),
            commonsCollections('implementation'),
          ]
        }
      }
      .withAndroidLibProject('lib-android-android-deps', 'com.example.lib') { lib ->
        lib.withBuildScript { bs ->
          bs.plugins = androidLibPlugin
          bs.android = defaultAndroidLibBlock(false, 'com.example.lib')
          bs.dependencies = [
            project('implementation', ':assets')
          ]
        }
      }
      .withSubproject('lib-java') { lib ->
        lib.withBuildScript { bs ->
          bs.plugins = [Plugin.javaLibrary, Plugins.dependencyAnalysisNoVersion]
        }
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

  private static Set<ModuleAdvice> assetsScore = [
    androidScoreBuilder().with {
      hasAndroidAssets = true
      hasAndroidRes = true
      usesAndroidClasses = false
      hasBuildConfig = false
      hasAndroidDependencies = false
      build()
    }
  ]

  private static Set<ModuleAdvice> libAndroidHasJavaDepsScore = [
    androidScoreBuilder().with {
      hasAndroidAssets = false
      hasAndroidRes = false
      usesAndroidClasses = false
      hasBuildConfig = false
      hasAndroidDependencies = false
      build()
    }
  ]
  private static Set<ModuleAdvice> libAndroidHasAndroidDepsScore = [
    androidScoreBuilder().with {
      hasAndroidAssets = false
      hasAndroidRes = false
      usesAndroidClasses = false
      hasBuildConfig = false
      hasAndroidDependencies = true
      build()
    }
  ]

  final Map<String, Set<ModuleAdvice>> expectedModuleAdvice = [
    ':app'                     : emptyModuleAdvice,
    ':assets'                  : assetsScore,
    ':lib-android-java-deps'   : libAndroidHasJavaDepsScore,
    ':lib-android-android-deps': libAndroidHasAndroidDepsScore,
    ':lib-java'                : emptyModuleAdvice,
  ]

  final Map<String, Set<ModuleAdvice>> expectedModuleAdviceForIgnore = [
    ':app'                     : emptyModuleAdvice,
  ':assets'                    : emptyModuleAdvice,
    ':lib-android-java-deps'   : emptyModuleAdvice,
    ':lib-android-android-deps': emptyModuleAdvice,
    ':lib-java'                : emptyModuleAdvice,
  ]
}
