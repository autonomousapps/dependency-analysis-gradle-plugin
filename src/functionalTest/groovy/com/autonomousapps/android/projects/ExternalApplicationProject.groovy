package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.*
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.Dependency.appcompat
import static com.autonomousapps.kit.Dependency.project

final class ExternalApplicationProject extends AbstractProject {

  final GradleProject gradleProject
  private final String agpVersion

  ExternalApplicationProject(String agpVersion) {
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
        bs.android = AndroidBlock.defaultAndroidAppBlock()
        bs.dependencies = [
          appcompat('implementation'),
          project('implementation', ':lib'),
        ]
      }
      app.manifest = AndroidManifest.app('com.example.lib.ExternalApp')
    }
    builder.withAndroidLibProject('lib', 'com.example.lib') { lib ->
      lib.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLibPlugin]
        bs.android = AndroidBlock.defaultAndroidLibBlock()
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
