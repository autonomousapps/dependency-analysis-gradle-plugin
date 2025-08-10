// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.commonsCollections

final class AndroidTestsAreIgnorableProject extends AbstractAndroidProject {

  final GradleProject gradleProject

  AndroidTestsAreIgnorableProject(String agpVersion) {
    super(agpVersion)
    gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withRootProject { r ->
        r.withBuildScript { bs ->
          bs.withGroovy(
            '''\
            dependencyAnalysis {
              issues {
                all {
                  ignoreSourceSet("androidTest")
                }
              }
            }
            '''
          )
        }
      }
      .withAndroidSubproject('lib') { lib ->
        lib.manifest = AndroidManifest.defaultLib('my.android.lib')
        lib.withBuildScript { bs ->
          bs.plugins = [Plugins.androidLib, Plugins.dependencyAnalysisNoVersion]
          bs.android = defaultAndroidLibBlock(false, 'my.android.lib')
          bs.dependencies = [
            commonsCollections('androidTestImplementation'),
          ]
        }
      }
      .write()
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':lib'),
  ]
}
