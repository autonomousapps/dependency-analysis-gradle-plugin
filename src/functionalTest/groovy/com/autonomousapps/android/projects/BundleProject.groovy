// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.firebaseAnalyticsKtx

final class BundleProject extends AbstractAndroidProject {

  final String agpVersion
  final GradleProject gradleProject

  BundleProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder()
      .withAndroidSubproject('lib') { a ->
        a.sources = sources
        a.manifest = null
        a.withBuildScript { bs ->
          bs.plugins = androidLib(false)
          bs.android = defaultAndroidLibBlock()
          bs.dependencies = [
            firebaseAnalyticsKtx("api")
          ]
        }
      }
      .write()
  }

  private sources = [
    new Source(
      SourceType.JAVA, 'Test', 'com/freeletics',
      """\
        package com.freeletics;

        import android.content.Context;
        import com.google.firebase.analytics.FirebaseAnalytics;
        
        public class Test {
          public void test(Context context) {
            FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(false);
          }
        }""".stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> libAdvice = [
    Advice.ofChange(moduleCoordinates('com.google.firebase:firebase-analytics-ktx:21.0.0'), 'api', 'implementation')
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':lib', libAdvice),
  ]
}
