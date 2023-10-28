package com.autonomousapps.android.projects

import com.autonomousapps.kit.*
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.Repository
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor

final class NoOpProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  NoOpProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return minimalAndroidProjectBuilder(agpVersion).tap {
      withAndroidSubproject('app') { app ->
        app.sources = sources
        app.withBuildScript { script ->
          script.plugins = [Plugin.androidApp]
          script.android = androidAppBlock(false)
          script.dependencies = dependencies
          script.repositories = Repository.DEFAULT + Repository.ofMaven('https://jitpack.io')
        }
      }
    }.build().tap {
      writer().write()
    }
  }

  private List<Source> sources = [
    new Source(
      SourceType.JAVA, "MainActivity", "com/example",
      """\
        package com.example;
        
        import androidx.appcompat.app.AppCompatActivity;
        import com.github.venom.Venom;
        
        public class MainActivity extends AppCompatActivity {
          
          public void thing() {
            Venom.createInstance(this).initialize();
          }       
        }
      """.stripIndent()
    )
  ]

  private List<Dependency> dependencies = [
    Dependency.appcompat("implementation"),
    new Dependency("releaseImplementation", "com.github.YarikSOffice.Venom:venom-no-op:0.4.1"),
    new Dependency("debugImplementation", "com.github.YarikSOffice.Venom:venom:0.4.1")
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  Set<ProjectAdvice> expectedBuildHealth = [emptyProjectAdviceFor(':app')]
}
