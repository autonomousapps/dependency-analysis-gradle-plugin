package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.AdviceHelper
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.kit.*

final class NoOpProject extends AbstractProject {

  final GradleProject gradleProject
  private final String agpVersion

  NoOpProject(String agpVersion) {
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return minimalAndroidProjectBuilder(agpVersion).tap {
      withAndroidSubproject('app') { app ->
        app.sources = sources
        app.withBuildScript { script ->
          script.plugins = [Plugin.androidAppPlugin]
          script.android = AndroidBlock.defaultAndroidAppBlock(false)
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

  @SuppressWarnings('GroovyAssignabilityCheck')
  List<ComprehensiveAdvice> actualBuildHealth() {
    AdviceHelper.actualBuildHealth(gradleProject)
  }

  @SuppressWarnings('GrMethodMayBeStatic')
  List<ComprehensiveAdvice> expectedBuildHealth() {
    return AdviceHelper.emptyBuildHealthFor(':app')
  }
}
