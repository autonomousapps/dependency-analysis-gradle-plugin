package com.autonomousapps.android.projects

import com.autonomousapps.kit.*
import com.autonomousapps.model.ModuleAdvice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.Dependency.*

/**
 * app (is android and should be)
 * +--- assets (is android and should be)
 * +--- lib-android (is android and shouldn't be)
 *      \--- lib-java
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
    def builder = newGradleProjectBuilder()
    builder.withRootProject { root ->
      root.gradleProperties = GradleProperties.minimalAndroidProperties()
      root.withBuildScript { bs ->
        bs.buildscript = BuildscriptBlock.defaultAndroidBuildscriptBlock(agpVersion)
        bs.additions = """\
          dependencyAnalysis {
            issues {
              all {
                onModuleStructure {
                  ${expectedResult.issue}
                }
              }
            }
          }
        """.stripIndent()
      }
    }
    builder.withAndroidSubproject('app') { app ->
      app.withBuildScript { bs ->
        bs.plugins = [Plugin.androidAppPlugin]
        bs.android = androidAppBlock(false)
        bs.dependencies = [
          appcompat('implementation'),
          project('implementation', ':assets'),
        ]
      }
      app.sources = sources
    }
    builder.withAndroidLibProject('assets', 'com.example.lib.assets') { assets ->
      assets.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLibPlugin]
        bs.android = androidLibBlock(false, 'com.example.lib.assets')
      }
      assets.withFile('src/main/assets/some_fancy_asset.txt', 'https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/657')
    }
    builder.withAndroidLibProject('lib-android', 'com.example.lib') { lib ->
      lib.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLibPlugin]
        bs.android = androidLibBlock(false, 'com.example.lib')
        bs.dependencies = [
          project('implementation', ':lib-java'),
          commonsCollections('implementation'),
        ]
      }
      lib.colors = null
      lib.styles = null
      lib.strings = null
      lib.layouts = null
    }
    builder.withSubproject('lib-java') { lib ->
      lib.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
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

  private static Set<ModuleAdvice> libAndroidScore = [
    androidScoreBuilder().with {
      hasAndroidAssets = false
      hasAndroidRes = false
      usesAndroidClasses = false
      hasBuildConfig = false
      hasAndroidDependencies = false
      build()
    }
  ]

  final Map<String, Set<ModuleAdvice>> expectedModuleAdvice = [
    ':app'        : emptyModuleAdvice,
    ':assets'     : assetsScore,
    ':lib-android': libAndroidScore,
    ':lib-java'   : emptyModuleAdvice,
  ]

  final Map<String, Set<ModuleAdvice>> expectedModuleAdviceForIgnore = [
    ':app'        : emptyModuleAdvice,
    ':assets'     : emptyModuleAdvice,
    ':lib-android': emptyModuleAdvice,
    ':lib-java'   : emptyModuleAdvice,
  ]
}
