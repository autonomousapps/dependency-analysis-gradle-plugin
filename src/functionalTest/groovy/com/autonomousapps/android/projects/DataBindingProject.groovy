package com.autonomousapps.android.projects

import com.autonomousapps.fixtures.*
import com.autonomousapps.internal.android.AgpVersion
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice
import kotlin.Pair

import static com.autonomousapps.AdviceHelper.moduleCoordinates
import static com.autonomousapps.fixtures.Dependencies.APPCOMPAT
import static com.autonomousapps.fixtures.Dependencies.getDEPENDENCIES_KOTLIN_STDLIB

/**
 * In this app project, the only reference to the lib project is through a color resource.
 * Does the plugin correctly say that 'lib' is a used dependency?
 */
final class DataBindingProject {

  DataBindingProject(String agpVersion) {
    this.agpVersion = agpVersion
  }

  AndroidProject newProject() {
    return new AndroidProject(rootSpec, appSpec)
  }

  private final String agpVersion
  final appSpec = new AppSpec(
    AppType.KOTLIN_ANDROID_APP,
    [
      'MainActivity.kt': """\
        import androidx.appcompat.app.AppCompatActivity
                
        class MainActivity : AppCompatActivity() {
        }
      """.stripIndent()
    ],
    [] as Set<AndroidLayout>,
    DEPENDENCIES_KOTLIN_STDLIB + [new Pair<String, String>('implementation', APPCOMPAT)],
    "android.buildFeatures.dataBinding true"
  )

  private final RootSpec rootSpec = new RootSpec(
    null, "", RootSpec.defaultGradleProperties(), agpVersion
  )

  Set<ProjectAdvice> expectedAdviceForApp() {
    if (AgpVersion.version(agpVersion) >= AgpVersion.version('7.4.0')) {
      [Advice.ofRemove(moduleCoordinates('org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.6.10'), 'implementation')]
    } else {
      []
    }
  }
}
