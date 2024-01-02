// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.android.AndroidLayout
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor

final class ArbitraryFileProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  ArbitraryFileProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('lib') { a ->
        a.manifest = libraryManifest()
        a.sources = sources
        a.layouts = layouts
        a.withFile('src/main/res/layout/FOO', 'bar')
        a.withBuildScript { bs ->
          bs.plugins = [Plugins.androidLib]
          bs.android = defaultAndroidLibBlock(false)
          bs.withGroovy("""
          afterEvaluate {
            tasks.withType(com.android.build.gradle.tasks.MergeResources).configureEach {
              aaptEnv.set("FOO")
            }
          }""")
        }
      }
      .write()
  }

  private List<Source> sources = [
    new Source(
      SourceType.JAVA, 'Main', 'com/example',
      """\
        package com.example;
        
        public class Main {}""".stripIndent()
    ),
  ]

  private List<AndroidLayout> layouts = [
    new AndroidLayout("activity_main.xml", """\
      <?xml version="1.0" encoding="utf-8"?>
      <FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
       />""".stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':lib')
  ]
}
