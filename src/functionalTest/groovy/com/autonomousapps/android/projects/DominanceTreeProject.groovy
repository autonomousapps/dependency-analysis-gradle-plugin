package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.dependencies.Plugins

import static com.autonomousapps.kit.gradle.Dependency.project
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.appcompat

final class DominanceTreeProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  DominanceTreeProject(String agpVersion) {
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
    builder.withAndroidSubproject('app') { app ->
      app.withBuildScript { bs ->
        bs.plugins = [Plugins.androidApp]
        bs.android = defaultAndroidAppBlock(false)
        bs.dependencies = [
          appcompat('implementation'),
          project('implementation', ':lib'),
        ]
      }
    }
    builder.withAndroidLibProject('lib', 'com.example.lib') { lib ->
      lib.withBuildScript { bs ->
        bs.plugins = [Plugins.androidLib]
        bs.android = defaultAndroidLibBlock(false, 'com.example.lib')
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  List<String> actualTree() {
    return gradleProject.rootDir.toPath()
      .resolve('app/build/reports/dependency-analysis/debugMain/graph/graph-dominator.txt')
      .readLines()
  }

  final expectedTree = """\
      4308.16 KiB :app
      +--- 4307.89 KiB (1496.37 KiB) androidx.appcompat:appcompat:1.1.0
      |    +--- 1482.96 KiB (1413.62 KiB) androidx.core:core:1.1.0
      |    |    \\--- 69.33 KiB androidx.versionedparcelable:versionedparcelable:1.1.0
      |    +--- 703.31 KiB (393.27 KiB) androidx.fragment:fragment:1.1.0
      |    |    +--- 128.23 KiB (82.23 KiB) androidx.loader:loader:1.0.0
      |    |    |    \\--- 45.99 KiB (19.43 KiB) androidx.lifecycle:lifecycle-livedata:2.0.0
      |    |    |         +--- 16.91 KiB androidx.lifecycle:lifecycle-livedata-core:2.0.0
      |    |    |         \\--- 9.65 KiB androidx.arch.core:core-runtime:2.0.0
      |    |    +--- 111.19 KiB androidx.viewpager:viewpager:1.0.0
      |    |    +--- 54.09 KiB (35.90 KiB) androidx.activity:activity:1.0.0
      |    |    |    \\--- 18.20 KiB androidx.savedstate:savedstate:1.0.0
      |    |    \\--- 16.53 KiB androidx.lifecycle:lifecycle-viewmodel:2.1.0
      |    +--- 335.70 KiB (152.99 KiB) androidx.appcompat:appcompat-resources:1.1.0
      |    |    +--- 96.84 KiB (86.21 KiB) androidx.vectordrawable:vectordrawable-animated:1.1.0
      |    |    |    \\--- 10.63 KiB androidx.interpolator:interpolator:1.0.0
      |    |    \\--- 85.88 KiB androidx.vectordrawable:vectordrawable:1.1.0
      |    +--- 76.69 KiB androidx.customview:customview:1.0.0
      |    +--- 70.34 KiB androidx.drawerlayout:drawerlayout:1.0.0
      |    +--- 41.95 KiB androidx.collection:collection:1.1.0
      |    +--- 27.81 KiB androidx.annotation:annotation:1.1.0
      |    +--- 22.47 KiB androidx.cursoradapter:cursoradapter:1.0.0
      |    +--- 21.18 KiB androidx.lifecycle:lifecycle-common:2.1.0
      |    +--- 18.12 KiB androidx.lifecycle:lifecycle-runtime:2.1.0
      |    \\--- 10.99 KiB androidx.arch.core:core-common:2.1.0
      \\--- 0.27 KiB :lib""".stripIndent().readLines()
}
