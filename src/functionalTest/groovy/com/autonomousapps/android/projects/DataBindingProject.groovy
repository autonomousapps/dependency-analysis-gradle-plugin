package com.autonomousapps.android.projects

import com.autonomousapps.fixtures.*
import com.autonomousapps.internal.android.AgpVersion
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice
import kotlin.Pair

import static com.autonomousapps.AdviceHelper.moduleCoordinates
import static com.autonomousapps.fixtures.Dependencies.APPCOMPAT
import static com.autonomousapps.fixtures.Dependencies.KOTLIN_STDLIB_JDK7
import static com.autonomousapps.fixtures.Dependencies.getDEPENDENCIES_KOTLIN_STDLIB

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

  Set<Advice> expectedAdviceForApp() {
    if (AgpVersion.version(agpVersion) >= AgpVersion.version('7.4.0')) {
      [Advice.ofRemove(moduleCoordinates(KOTLIN_STDLIB_JDK7), 'implementation')]
    } else {
      []
    }
  }
}
