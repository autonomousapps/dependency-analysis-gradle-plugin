package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.android.AndroidColorRes
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.android.AndroidStyleRes
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
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('app') { app ->
        app.withBuildScript { bs ->
          bs.plugins = [Plugins.androidApp]
          bs.android = defaultAndroidAppBlock(false)
          bs.dependencies = [
            appcompat('implementation'),
            project('implementation', ':lib'),
          ]
        }
        app.styles = AndroidStyleRes.DEFAULT
        app.colors = AndroidColorRes.DEFAULT
        app.manifest = AndroidManifest.app('com.example.lib.ExternalApp')
      }
      .withAndroidLibProject('lib', 'com.example.lib') { lib ->
        lib.withBuildScript { bs ->
          bs.plugins = [Plugins.androidLib]
          bs.android = defaultAndroidLibBlock(false)
        }
        lib.sources = libSources
      }
      .write()
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
