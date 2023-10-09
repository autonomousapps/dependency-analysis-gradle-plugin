package com.autonomousapps.android.projects

import com.autonomousapps.kit.*
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.appcompat
import static com.autonomousapps.kit.gradle.Dependency.project

final class AndroidAssetsProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  AndroidAssetsProject(String agpVersion) {
    super(agpVersion)

    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { root ->
      root.gradleProperties = GradleProperties.minimalAndroidProperties()
      root.withBuildScript { bs ->
        bs.buildscript = BuildscriptBlock.defaultAndroidBuildscriptBlock(agpVersion)
      }
//      root.withFile('local.properties', """\
//        sdk.dir=/Users/trobalik/Library/Android/Sdk
//      """.stripIndent())
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
    builder.withAndroidLibProject('lib', 'com.example.lib') { lib ->
      lib.manifest = libraryManifest('com.example.lib')
      lib.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLibPlugin]
        bs.android = androidLibBlock(false, 'com.example.lib')
        bs.dependencies = [
          project('implementation', ':assets'),
        ]
      }
    }
    builder.withAndroidLibProject('assets', 'com.example.lib.assets') { assets ->
      assets.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLibPlugin]
        bs.android = androidLibBlock(false, 'com.example.lib.assets')
      }
      assets.withFile('src/main/assets/some_fancy_asset.txt', 'https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/657')
      assets.manifest = libraryManifest('com.example.lib.assets')
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
