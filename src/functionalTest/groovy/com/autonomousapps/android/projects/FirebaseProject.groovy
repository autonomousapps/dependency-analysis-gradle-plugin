// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.android.AndroidColorRes
import com.autonomousapps.kit.android.AndroidStyleRes
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.dependencies.Plugins

import static com.autonomousapps.kit.gradle.dependencies.Dependencies.appcompat
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.firebaseAnalytics

final class FirebaseProject extends AbstractAndroidProject {

  final String agpVersion
  final GradleProject gradleProject

  FirebaseProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('app') { a ->
        a.sources = sources
        a.styles = AndroidStyleRes.DEFAULT
        a.colors = AndroidColorRes.DEFAULT
        a.withBuildScript { bs ->
          bs.plugins = [Plugins.androidApp]
          bs.android = defaultAndroidAppBlock(false)
          bs.dependencies = dependencies
        }
      }
      .write()
  }

  private List<Dependency> dependencies = [
    appcompat("implementation"),
    firebaseAnalytics("implementation")
  ]

  private sources = [
    new Source(
      SourceType.JAVA, 'MainActivity', 'com/example',
      """\
        package com.example;
        
        import android.os.Bundle;
        import androidx.appcompat.app.AppCompatActivity;
        import com.google.firebase.analytics.FirebaseAnalytics;

        public class MainActivity extends AppCompatActivity {
          private FirebaseAnalytics analytics;

          @Override
          public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            analytics = FirebaseAnalytics.getInstance(this);
          }
        }
      """
    )
  ]
}
