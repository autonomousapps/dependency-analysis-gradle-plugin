package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.android.AndroidColorRes
import com.autonomousapps.kit.android.AndroidStyleRes
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.appcompat

final class EmptyResFile extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  EmptyResFile(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('app') { app ->
        app.withBuildScript { bs ->
          bs.plugins = androidAppPlugin
          bs.android = defaultAndroidAppBlock(false)
          bs.dependencies = [
            appcompat("implementation")
          ]
        }

        app.styles = AndroidStyleRes.DEFAULT
        app.colors = AndroidColorRes.DEFAULT
        // https://github.com/androidx/androidx/blob/androidx-main/security/security-app-authenticator/src/androidTest/res/raw/no_root_element.xml
        app.withFile('src/androidTest/res/raw/no_root_element.xml', """\
          <?xml version="1.0" encoding="utf-8"?>
          <!--
            Hi!
            -->""".stripIndent()
        )
      }
      .write()
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':app'),
  ]
}
