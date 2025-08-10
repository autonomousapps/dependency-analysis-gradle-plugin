// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
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
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('lib') { a ->
        a.sources = sources
        a.manifest = libraryManifest()
        a.withBuildScript { bs ->
          bs.plugins = androidLibPlugin
          bs.android = defaultAndroidLibBlock(false)
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
        }
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':lib'),
  ]
}
