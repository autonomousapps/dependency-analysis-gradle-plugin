package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.*
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor

/**
 * https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/826.
 */
final class AndroidTextQuestionMarkProject extends AbstractProject {

  final GradleProject gradleProject
  private final String agpVersion

  AndroidTextQuestionMarkProject(String agpVersion) {
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
    }
    builder.withAndroidSubproject('app') { app ->
      app.withBuildScript { bs ->
        bs.plugins = [Plugin.androidAppPlugin]
        bs.android = AndroidBlock.defaultAndroidAppBlock()
        bs.dependencies = [Dependency.appcompat('implementation')]
      }
      app.manifest = AndroidManifest.app("com.example.MainApplication")
      app.withFile('src/main/res/layout/main.xml', """\
        <?xml version="1.0" encoding="utf-8"?>
        <TextView xmlns:android="http://schemas.android.com/apk/res/android"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          android:text="?" />
      """.stripIndent())
    }

    builder.build().tap {
      writer().write()
    }
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':app')
  ]
}
