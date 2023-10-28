package com.autonomousapps.android.projects

import com.autonomousapps.kit.*
import com.autonomousapps.kit.android.AndroidLayout
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor

final class ArbitraryFileProject extends AbstractAndroidProject {

  private static final APPCOMPAT = Dependency.appcompat('implementation')

  final GradleProject gradleProject
  private final String agpVersion

  ArbitraryFileProject(String agpVersion) {
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
    }
    builder.withAndroidSubproject('lib') { a ->
      a.manifest = libraryManifest()
      a.sources = sources
      a.layouts = layouts
      a.withFile('src/main/res/layout/FOO', 'bar')
      a.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLib]
        bs.android = androidLibBlock(false)
        bs.dependencies = [APPCOMPAT]
        bs.additions = """
          afterEvaluate {
            tasks.withType(com.android.build.gradle.tasks.MergeResources).configureEach {
              aaptEnv.set("FOO")
            }
          }""".stripIndent()
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
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
