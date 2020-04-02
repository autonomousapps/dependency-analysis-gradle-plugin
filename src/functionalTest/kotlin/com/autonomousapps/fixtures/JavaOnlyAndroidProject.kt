package com.autonomousapps.fixtures

class JavaOnlyAndroidProject(
  private val agpVersion: String
) {
  private val appSpec = AppSpec(
    type = AppType.JAVA_ANDROID_APP,
    sources = mapOf("MainActivity.java" to """
                import androidx.appcompat.app.AppCompatActivity;
                
                public class MainActivity extends AppCompatActivity {
                    public void doNothing() {
                    }
                }
            """.trimIndent()),
    dependencies = listOf(
      "implementation" to APPCOMPAT
    )
  )

  fun newProject() = AndroidProject(
    rootSpec = RootSpec(agpVersion = agpVersion),
    appSpec = appSpec
  )
}
