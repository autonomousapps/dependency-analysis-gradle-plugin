// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.internal.utils.OpaqueNames
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.implementation

final class BuildLogicVersionCatalogProject extends AbstractProject {

  private static final String BUILD_LOGIC = 'build-logic'
  private final boolean used
  final GradleProject gradleProject

  BuildLogicVersionCatalogProject(boolean used) {
    this.used = used
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withRootProject { r ->
        r.withVersionCatalog(
          """\
            [versions]
            dagp = "3.7.0"
            
            [libraries]
            dagp = { module = "com.autonomousapps:dependency-analysis-gradle-plugin", version.ref = "dagp" }
            
            [plugins]
            dependencyAnalysis = { id = "com.autonomousapps.dependency-analysis", version.ref = "dagp" }""".stripIndent()
        )
        r.settingsScript.additions = "includeBuild '$BUILD_LOGIC'"
      }
      .withIncludedBuild(BUILD_LOGIC) { buildLogic ->
        buildLogic.withRootProject { r ->
          r.gradleProperties += ADDITIONAL_PROPERTIES
          r.withSettingsScript { s ->
            s.additions = """\
            dependencyResolutionManagement {
              versionCatalogs {
                create("libs") { 
                  from(files("../gradle/libs.versions.toml"))
                } 
              }
            }""".stripIndent()
          }
          r.withBuildScript { bs ->
            bs.plugins(Plugins.dependencyAnalysis, Plugins.kotlinJvm, Plugin.javaGradle)
            bs.dependencies(
              // Kotlin DSL: files(libs::class.java.superclass.protectionDomain.codeSource.location)
              implementation('files(libs.class.superclass.protectionDomain.codeSource.location)').raw()
            )
          }
          r.sources = pluginSources()
        }
      }
      .write()
  }

  private List<Source> pluginSources() {
    if (used) {
      [
        Source.kotlin(
          '''\
          package mutual.aid
          
          import org.gradle.accessors.dm.LibrariesForLibs
          import org.gradle.api.Plugin
          import org.gradle.api.Project 
          
          abstract class MyPlugin : Plugin<Project> {
            override fun apply(target: Project) {
              val libs: LibrariesForLibs = target.extensions.getByName("libs") as LibrariesForLibs
              val dagp = libs.plugins.dependencyAnalysis
            }
          }'''.stripIndent()
        ).build()
      ]
    } else {
      [
        Source.kotlin(
          '''\
          package mutual.aid
          
          import org.gradle.api.Plugin
          import org.gradle.api.Project
          
          abstract class MyPlugin : Plugin<Project> {
            override fun apply(target: Project) {}
          }'''.stripIndent()
        ).build()
      ]
    }
  }

  Set<ProjectAdvice> actualProjectAdvice() {
    return [
      actualProjectAdviceForProject(gradleProject.includedBuilds.first(), ':'),
    ]
  }

  private Set<Advice> unusedAdvice() {
    [Advice.ofRemove(flatCoordinates(OpaqueNames.GRADLE_VERSION_CATALOG), 'implementation')]
  }

  Set<ProjectAdvice> expectedProjectAdvice() {
    if (used) {
      [emptyProjectAdviceFor(':')]
    } else {
      [projectAdviceForDependencies(':', unusedAdvice())]
    }
  }

  String expectedReason() {
    if (used) {
      '* Uses 2 classes: org.gradle.accessors.dm.LibrariesForLibs, org.gradle.accessors.dm.LibrariesForLibs$PluginAccessors (implies implementation).'
    } else {
      '''\
        You asked about the dependency 'gradle-version-catalog'.
        You have been advised to remove this dependency from 'implementation'.'''.stripIndent()
    }
  }
}
