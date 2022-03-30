package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.AdviceHelper
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.kit.*

import static com.autonomousapps.kit.Dependency.appcompat
import static com.autonomousapps.kit.Dependency.constraintLayout

final class ArbitraryFileProject extends AbstractProject {

  final GradleProject gradleProject
  private final String agpVersion

  ArbitraryFileProject(String agpVersion) {
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
    builder.withAndroidSubproject('app') { a ->
      a.sources = sources
      a.layouts = layouts
      a.withFile("src/main/res/layout/FOO", "bar")
      a.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLibPlugin]
        bs.android = AndroidBlock.defaultAndroidLibBlock(false)
        bs.dependencies = []
        bs.additions = """
          afterEvaluate {
            tasks.withType(com.android.build.gradle.tasks.MergeResources).configureEach {
              aaptEnv.set("FOO")
            }
          }
        """.stripIndent()
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private List<Source> sources = [
    new Source(
      SourceType.JAVA, "Main", "com/example",
      """\
        package com.example;
        
        public class Main {}
      """.stripIndent()
    ),
  ]

  private List<AndroidLayout> layouts = [
    new AndroidLayout("activity_main.xml", """\
      <?xml version="1.0" encoding="utf-8"?>
      <FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
       />        
      """.stripIndent()
    )
  ]

  List<ComprehensiveAdvice> actualBuildHealth() {
    return AdviceHelper.actualBuildHealth(gradleProject)
  }

  final List<ComprehensiveAdvice> expectedBuildHealth = [
    new ComprehensiveAdvice(":app", [] as Set, [] as Set, false),
  ]
}
