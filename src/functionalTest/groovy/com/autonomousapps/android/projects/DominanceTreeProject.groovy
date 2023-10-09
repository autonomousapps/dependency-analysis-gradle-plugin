package com.autonomousapps.android.projects

import com.autonomousapps.kit.*
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.Plugin

import static com.autonomousapps.kit.gradle.Dependency.appcompat
import static com.autonomousapps.kit.gradle.Dependency.project

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
        bs.plugins = [Plugin.androidAppPlugin]
        bs.android = androidAppBlock(false)
        bs.dependencies = [
          appcompat('implementation'),
          project('implementation', ':lib'),
        ]
      }
    }
    builder.withAndroidLibProject('lib', 'com.example.lib') { lib ->
      lib.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLibPlugin]
        bs.android = androidLibBlock(false, 'com.example.lib')
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
      4364.27 KiB :app
      +--- 4362.54 KiB (1512.16 KiB) androidx.appcompat:appcompat:1.1.0
      |    +--- 1503.70 KiB (1433.59 KiB) androidx.core:core:1.1.0
      |    |    \\--- 70.10 KiB androidx.versionedparcelable:versionedparcelable:1.1.0
      |    +--- 713.91 KiB (397.32 KiB) androidx.fragment:fragment:1.1.0
      |    |    +--- 131.11 KiB (83.83 KiB) androidx.loader:loader:1.0.0
      |    |    |    \\--- 47.29 KiB (19.97 KiB) androidx.lifecycle:lifecycle-livedata:2.0.0
      |    |    |         +--- 17.31 KiB androidx.lifecycle:lifecycle-livedata-core:2.0.0
      |    |    |         \\--- 10.00 KiB androidx.arch.core:core-runtime:2.0.0
      |    |    +--- 112.83 KiB androidx.viewpager:viewpager:1.0.0
      |    |    +--- 55.63 KiB (36.99 KiB) androidx.activity:activity:1.0.0
      |    |    |    \\--- 18.64 KiB androidx.savedstate:savedstate:1.0.0
      |    |    \\--- 17.02 KiB androidx.lifecycle:lifecycle-viewmodel:2.1.0
      |    +--- 340.11 KiB (154.90 KiB) androidx.appcompat:appcompat-resources:1.1.0
      |    |    +--- 98.24 KiB (87.35 KiB) androidx.vectordrawable:vectordrawable-animated:1.1.0
      |    |    |    \\--- 10.90 KiB androidx.interpolator:interpolator:1.0.0
      |    |    \\--- 86.96 KiB androidx.vectordrawable:vectordrawable:1.1.0
      |    +--- 77.92 KiB androidx.customview:customview:1.0.0
      |    +--- 71.38 KiB androidx.drawerlayout:drawerlayout:1.0.0
      |    +--- 41.95 KiB androidx.collection:collection:1.1.0
      |    +--- 27.81 KiB androidx.annotation:annotation:1.1.0
      |    +--- 22.96 KiB androidx.cursoradapter:cursoradapter:1.0.0
      |    +--- 21.18 KiB androidx.lifecycle:lifecycle-common:2.1.0
      |    +--- 18.47 KiB androidx.lifecycle:lifecycle-runtime:2.1.0
      |    \\--- 10.99 KiB androidx.arch.core:core-common:2.1.0
      \\--- 1.73 KiB :lib""".stripIndent().readLines()
}
