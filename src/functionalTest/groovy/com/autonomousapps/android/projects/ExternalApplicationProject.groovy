package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.project
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.appcompat

final class ExternalApplicationProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  ExternalApplicationProject(String agpVersion) {
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
        bs.plugins = [Plugins.androidApp]
        bs.android = androidAppBlock(false)
        bs.dependencies = [
          appcompat('implementation'),
          project('implementation', ':lib'),
        ]
      }
      app.manifest = AndroidManifest.app('com.example.lib.ExternalApp')
    }
    builder.withAndroidLibProject('lib', 'com.example.lib') { lib ->
      lib.withBuildScript { bs ->
        bs.plugins = [Plugins.androidLib]
        bs.android = androidLibBlock(false)
      }
      lib.sources = libSources
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private libSources = [
    new Source(
      SourceType.JAVA, 'ExternalApp', 'com/example/lib',
      """\
        package com.example.lib;
        
        import android.app.Application;
        
        public class ExternalApp extends Application {}
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':app'),
    emptyProjectAdviceFor(':lib'),
  ]
}
