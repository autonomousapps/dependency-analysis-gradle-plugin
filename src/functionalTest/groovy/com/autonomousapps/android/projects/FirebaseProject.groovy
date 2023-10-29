package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
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
    def builder = newGradleProjectBuilder()
    builder.withRootProject { root ->
      root.gradleProperties = GradleProperties.minimalAndroidProperties()
      root.withBuildScript { bs ->
        bs.buildscript = BuildscriptBlock.defaultAndroidBuildscriptBlock(agpVersion)
      }
    }
    builder.withAndroidSubproject('app') { a ->
      a.sources = sources
      a.withBuildScript { bs ->
        bs.plugins = [Plugins.androidApp]
        bs.android = androidAppBlock(false)
        bs.dependencies = dependencies
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
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
