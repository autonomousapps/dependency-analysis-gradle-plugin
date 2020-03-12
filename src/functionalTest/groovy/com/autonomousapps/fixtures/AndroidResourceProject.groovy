package com.autonomousapps.fixtures

import kotlin.Pair

import static com.autonomousapps.fixtures.Dependencies.APPCOMPAT
import static com.autonomousapps.fixtures.Dependencies.getDEPENDENCIES_KOTLIN_STDLIB
import static com.autonomousapps.fixtures.Fixtures.DEFAULT_PACKAGE_NAME

/**
 * In this app project, the only reference to the lib project is through a color resource.
 * Does the plugin correctly say that 'lib' is a used dependency?
 */
final class AndroidResourceProject {

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
      DEPENDENCIES_KOTLIN_STDLIB + [new Pair('implementation', APPCOMPAT)]
  )
  private final librarySpecs = [
      new LibrarySpec(
          libName,
          LibraryType.KOTLIN_ANDROID_LIB,
          DEPENDENCIES_KOTLIN_STDLIB,
          [:]
      )
  ]

  AndroidResourceProject(String agpVersion) {
    this.agpVersion = agpVersion
  }

  AndroidProject newProject() {
    return new AndroidProject(agpVersion, appSpec, librarySpecs, "")
  }
}
