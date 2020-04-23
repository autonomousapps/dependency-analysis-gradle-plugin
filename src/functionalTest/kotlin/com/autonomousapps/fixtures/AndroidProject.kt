@file:JvmName("Fixtures")

package com.autonomousapps.fixtures

import java.io.File

const val WORKSPACE = "build/functionalTest"

const val DEFAULT_PACKAGE_NAME = "com.autonomousapps.test"
const val DEFAULT_PACKAGE_PATH = "com/autonomousapps/test"

interface Module {
  val dir: File
  val variant: String?
}

interface ModuleSpec {
  val name: String
}

class AppSpec @JvmOverloads constructor(
  val type: AppType = AppType.KOTLIN_ANDROID_APP,
  val sources: Map<String, String> = DEFAULT_APP_SOURCES,
  val layouts: Set<AndroidLayout> = emptySet(),
  val dependencies: List<Pair<String, String>> = DEFAULT_APP_DEPENDENCIES,
  val buildAdditions: String = "",
  val plugins: Set<String>? = null
) : ModuleSpec {

  override val name: String = "app"

  fun formattedDependencies(): String {
    return dependencies.joinToString(separator = "\n\t") { (conf, dep) ->
      if (dep.startsWith("project")) {
        "$conf $dep"
      } else {
        "$conf \"$dep\""
      }
    }
  }
}

enum class AppType {
  /**
   * Indicates a module with only the `com.android.application` plugin applied.
   */
  JAVA_ANDROID_APP,

  /**
   * Indicates a module with the `com.android.application` and `org.jetbrains.kotlin.jvm` plugins applied.
   */
  KOTLIN_ANDROID_APP
}

enum class LibraryType {
  /**
   * Indicates a module with the `com.android.library` and `kotlin-android` plugins applied.
   */
  KOTLIN_ANDROID_LIB,

  /**
   * Indicates a module with the `java-library` plugin applied.
   */
  JAVA_JVM_LIB,

  /**
   * Indicates a module with the `com.android.library` and `kotlin-android` plugins applied.
   */
  KOTLIN_JVM_LIB
}

class LibrarySpec(
  override val name: String,
  val type: LibraryType,
  val applyPlugin: Boolean = false,
  val dependencies: List<Pair<String, String>> = DEFAULT_LIB_DEPENDENCIES,
  val sources: Map<String, String> = defaultSources(type)
) : ModuleSpec {

  companion object {
    @JvmStatic
    fun defaultSources(type: LibraryType): Map<String, String> = when (type) {
      LibraryType.KOTLIN_ANDROID_LIB -> DEFAULT_SOURCE_KOTLIN_ANDROID
      LibraryType.JAVA_JVM_LIB -> DEFAULT_SOURCE_JAVA
      LibraryType.KOTLIN_JVM_LIB -> DEFAULT_SOURCE_KOTLIN_JVM
    }
  }

  fun formattedDependencies(): String = dependencies.joinToString(separator = "\n") { (conf, dep) ->
    if (dep.startsWith("project")) {
      "$conf $dep"
    } else {
      "$conf \"$dep\""
    }
  }
}

fun libraryFactory(projectDir: File, librarySpec: LibrarySpec): Module {
  return when (librarySpec.type) {
    LibraryType.KOTLIN_ANDROID_LIB -> AndroidKotlinLibModule(projectDir, librarySpec)
    LibraryType.JAVA_JVM_LIB -> JavaJvmLibModule(projectDir, librarySpec)
    LibraryType.KOTLIN_JVM_LIB -> KotlinJvmLibModule(projectDir, librarySpec)
  }
}

abstract class BaseGradleProject(val projectDir: File) : Module {

  override val dir = projectDir.also { it.mkdirs() }

  fun withBuildFile(content: String, name: String = "build.gradle") {
    projectDir.resolve(name).also { it.parentFile.mkdirs() }.writeText(content.trimMargin())
  }

  fun withFile(relativePath: String, content: String) {
    projectDir.resolve(relativePath).also { it.parentFile.mkdirs() }.writeText(content.trimIndent())
  }

  fun withSources(sources: Set<Source>?) {
    sources?.forEach {
        withFile("src/main/java/${it.path}/${it.name}", it.source)
    }
  }
}

abstract class AndroidGradleProject(projectDir: File) : BaseGradleProject(projectDir) {
  private val mainDir = projectDir.resolve("src/main").also { it.mkdirs() }
  private val mainJavaSrcDir = mainDir.resolve("java").also { it.mkdirs() }
  private val resDir = mainDir.resolve("res").also { it.mkdirs() }

  fun withManifestFile(content: String, relativePath: String = "AndroidManifest.xml") {
    mainDir.resolve(relativePath).writeText(content.trimIndent())
  }

  fun withJavaSrcFile(relativePath: String, content: String) {
    val srcFile = mainJavaSrcDir.resolve(relativePath).also { it.parentFile.mkdirs() }
    srcFile.writeText(content.trimIndent())
  }

  fun withStylesFile(content: String, relativePath: String = "values/styles.xml") {
    resDir.resolve(relativePath).also { it.parentFile.mkdirs() }.writeText(content.trimIndent())
  }

  fun withColorsFile(content: String, relativePath: String = "values/colors.xml") {
    resDir.resolve(relativePath).also { it.parentFile.mkdirs() }.writeText(content.trimIndent())
  }

  fun withLayoutFile(content: String, relativePath: String = "layout", fileName: String) {
    resDir.resolve("$relativePath/$fileName").also { it.parentFile.mkdirs() }
      .writeText(content.trimIndent())
  }
}

abstract class JavaGradleProject(projectDir: File) : BaseGradleProject(projectDir) {
  private val mainSrcDir = projectDir.resolve("src/main/java").also { it.mkdirs() }
  fun withSrcFile(relativePath: String, content: String) {
    mainSrcDir.resolve(relativePath).also { it.parentFile.mkdirs() }.writeText(content.trimIndent())
  }
}

abstract class KotlinGradleProject(projectDir: File) : BaseGradleProject(projectDir) {
  private val mainSrcDir = projectDir.resolve("src/main/kotlin").also { it.mkdirs() }
  fun withSrcFile(relativePath: String, content: String) {
    mainSrcDir.resolve(relativePath).also { it.parentFile.mkdirs() }.writeText(content.trimIndent())
  }
}

abstract class RootGradleProject(projectDir: File) : BaseGradleProject(projectDir) {
  fun withGradlePropertiesFile(content: String, name: String = "gradle.properties") {
    projectDir.resolve(name).also { it.parentFile.mkdirs() }.writeText(content.trimIndent())
  }

  fun withSettingsFile(content: String, name: String = "settings.gradle") {
    projectDir.resolve(name).also { it.parentFile.mkdirs() }.writeText(content.trimIndent())
  }
}

/**
 * A typical Android project, with an "app" module (which has applied the `com.android.application` plugin, one or more
 * android-library modules (`com.android.library` plugin), and one or more java-library modules (`java-library` plugin).
 *
 * @param librarySpecs a list of library project names and types. Can be null. See [LibrarySpec] and
 * [LibraryType].
 */
class AndroidProject(
  rootSpec: RootSpec = RootSpec(),
  appSpec: AppSpec = AppSpec(
    sources = DEFAULT_APP_SOURCES,
    dependencies = DEFAULT_APP_DEPENDENCIES
  ),
  librarySpecs: List<LibrarySpec>? = null
) : ProjectDirProvider {

  private val rootProject = RootProject(rootSpec)

  /**
   * Feed this to a [GradleRunner][org.gradle.testkit.runner.GradleRunner].
   */
  override val projectDir = rootProject.projectDir

  // A collection of Android modules (one "app" module and zero or more library modules), keyed by their respective
  // names.
  private val modules: Map<String, Module> = mapOf(
    "app" to AppModule(projectDir, appSpec, librarySpecs),
    *librarySpecs?.map { spec ->
      spec.name to libraryFactory(projectDir, spec)
    }?.toTypedArray() ?: emptyArray()
  )

  override fun project(moduleName: String): Module {
    if (moduleName == ":") {
      return rootProject
    }
    return modules[moduleName] ?: error("No '$moduleName' project found!")
  }
}

/**
 * The "app" module, a typical `com.android.application` project, with the `kotlin-android` plugin applied as well.
 */
class AppModule(
  rootProjectDir: File,
  private val appSpec: AppSpec,
  librarySpecs: List<LibrarySpec>? = null
) : AndroidGradleProject(rootProjectDir.resolve("app").also { it.mkdirs() }) {

  override val variant = "debug"

  init {
    val agpVersion = "\${com.android.builder.model.Version.ANDROID_GRADLE_PLUGIN_VERSION}"
    val afterEvaluate = "afterEvaluate { println \"AGP version: $agpVersion\" }"

    withBuildFile("""
      |${plugins()}
      |
      |android {
      |  compileSdkVersion 29
      |  defaultConfig {
      |    applicationId "$DEFAULT_PACKAGE_NAME"
      |    minSdkVersion 21
      |    targetSdkVersion 29
      |    versionCode 1
      |    versionName "1.0"
      |  }
      |  compileOptions {
      |    sourceCompatibility JavaVersion.VERSION_1_8
      |    targetCompatibility JavaVersion.VERSION_1_8
      |  }
      |  ${kotlinOptions()}
      |}
      |dependencies {
      |  ${librarySpecs?.map { it.name }?.joinToString("\n\t") { "implementation project(':$it')" } ?: "" }
      |  ${appSpec.formattedDependencies()}
      |}
      |
      |$afterEvaluate
      |
      |${appSpec.buildAdditions}"""
    )

    withManifestFile("""
      <?xml version="1.0" encoding="utf-8"?>
      <manifest xmlns:android="http://schemas.android.com/apk/res/android"
        package="$DEFAULT_PACKAGE_NAME"
        >
      
        <application
          android:allowBackup="true"
          android:label="Test app"
          android:theme="@style/AppTheme"
          >
          <activity
            android:name=".MainActivity"
            android:label="MainActivity"
            android:theme="@style/AppTheme.NoActionBar">
              <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
              </intent-filter>
            </activity>
          </application>
      </manifest>"""
    )

    withStylesFile("""
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
              <style name="AppTheme" parent="Theme.AppCompat.Light.DarkActionBar">
                <item name="colorPrimary">@color/colorPrimary</item>
                <item name="colorPrimaryDark">@color/colorPrimaryDark</item>
                <item name="colorAccent">@color/colorAccent</item>
              </style>
            
              <style name="AppTheme.NoActionBar">
                <item name="windowActionBar">false</item>
                <item name="windowNoTitle">true</item>
              </style>
            
              <style name="AppTheme.AppBarOverlay" parent="ThemeOverlay.AppCompat.Dark.ActionBar" />
              <style name="AppTheme.PopupOverlay" parent="ThemeOverlay.AppCompat.Light" />
            </resources>"""
    )

    withColorsFile("""
      <?xml version="1.0" encoding="utf-8"?>
      <resources>
        <color name="colorPrimaryDark">#0568ae</color>
        <color name="colorPrimary">#009fdb</color>
        <color name="colorAccent">#009fdb</color>
      </resources>"""
    )

    appSpec.layouts.forEach { layout ->
      withLayoutFile(content = layout.content, fileName = layout.fileName)
    }

    appSpec.sources.forEach { (name, source) ->
      // TODO add package automatically (as in AndroidKotlinLibModule)
      withJavaSrcFile("$DEFAULT_PACKAGE_PATH/$name", source)
    }
  }

  private fun plugins() = when (appSpec.type) {
    AppType.KOTLIN_ANDROID_APP ->
      """
        plugins {
          id('com.android.application')
          id('kotlin-android')
          ${appSpec.plugins?.joinToString("\n") { "id('$it')" } ?: ""}
        }
      """.trimIndent()
    AppType.JAVA_ANDROID_APP ->
      """
        plugins {
          id('com.android.application')
          ${appSpec.plugins?.joinToString("\n") { "id('$it')" } ?: ""}
        }
      """.trimIndent()
  }

  private fun kotlinOptions() = when (appSpec.type) {
    AppType.KOTLIN_ANDROID_APP ->
      """
      kotlinOptions {
        jvmTarget = "1.8"
      }
    """.trimIndent()
    AppType.JAVA_ANDROID_APP -> ""
  }
}

/**
 * Creates a module with the `com.android.library` and `kotlin-android` plugins applied.
 */
class AndroidKotlinLibModule(rootProjectDir: File, librarySpec: LibrarySpec)
  : AndroidGradleProject(rootProjectDir.resolve(librarySpec.name).also { it.mkdirs() }) {

  override val variant = "debug"

  init {
    withBuildFile("""
            plugins {
                id('com.android.library')
                id('kotlin-android')
                ${if (librarySpec.applyPlugin) "id 'com.autonomousapps.dependency-analysis' version '${System.getProperty("com.autonomousapps.pluginversion")}'" else ""}
            }
            android {
                compileSdkVersion 29
                defaultConfig {
                    minSdkVersion 21
                    targetSdkVersion 29
                    versionCode 1
                    versionName "1.0"
                }
                compileOptions {
                    sourceCompatibility JavaVersion.VERSION_1_8
                    targetCompatibility JavaVersion.VERSION_1_8
                }
                kotlinOptions {
                    jvmTarget = "1.8"
                }
            }
            dependencies {
                ${librarySpec.formattedDependencies()}
            }
        """
    )
    withManifestFile("""
            <?xml version="1.0" encoding="utf-8"?>
            <manifest package="$DEFAULT_PACKAGE_NAME.${librarySpec.name}" />            
        """
    )
    withColorsFile("""
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="colorPrimaryDark">#0568ae</color>
                <color name="colorPrimary">#009fdb</color>
                <color name="colorAccent">#009fdb</color>
                <color name="libColor">#000000</color>
            </resources>
        """
    )
    librarySpec.sources.forEach { (name, source) ->
      withJavaSrcFile(
        "$DEFAULT_PACKAGE_PATH/android/$name",
        "package $DEFAULT_PACKAGE_NAME.android\n\n$source"
      )
    }
  }
}
