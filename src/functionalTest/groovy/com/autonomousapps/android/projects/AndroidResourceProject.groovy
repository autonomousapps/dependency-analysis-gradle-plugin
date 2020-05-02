package com.autonomousapps.android.projects

import com.autonomousapps.fixtures.*
import kotlin.Pair

import static com.autonomousapps.fixtures.Dependencies.APPCOMPAT
import static com.autonomousapps.fixtures.Dependencies.getDEPENDENCIES_KOTLIN_STDLIB
import static com.autonomousapps.fixtures.Fixtures.DEFAULT_PACKAGE_NAME

/**
 * In this app project, the only reference to the lib project is through a color resource.
 * Does the plugin correctly say that 'lib' is a used dependency?
 */
final class AndroidResourceProject {

  AndroidResourceProject(String agpVersion) {
    this.agpVersion = agpVersion
  }

  AndroidProject newProject() {
    return new AndroidProject(rootSpec, appSpec, librarySpecs)
  }

  private final String agpVersion
  private final String libName = 'lib'
  private final appSpec = new AppSpec(
    AppType.KOTLIN_ANDROID_APP,
    [
      'MainActivity.kt': """\
        package $DEFAULT_PACKAGE_NAME
        
        import androidx.appcompat.app.AppCompatActivity
        import $DEFAULT_PACKAGE_NAME.${libName}.R
        
        class MainActivity : AppCompatActivity() {
          val i = R.color.libColor
        }
      """.stripIndent()
    ],
    [] as Set<AndroidLayout>,
    DEPENDENCIES_KOTLIN_STDLIB + [new Pair<String, String>('implementation', APPCOMPAT)]
  )
  private final librarySpecs = [
    new LibrarySpec(
      libName,
      LibraryType.KOTLIN_ANDROID_LIB,
      false,
      [],
      DEPENDENCIES_KOTLIN_STDLIB,
      [:]
    )
  ]
  private final RootSpec rootSpec = new RootSpec(
    librarySpecs, "", RootSpec.defaultGradleProperties(), agpVersion,
    RootSpec.defaultSettingsScript(agpVersion, librarySpecs), RootSpec.defaultBuildScript(agpVersion, "")
  )
}
