package com.autonomousapps.fixtures

import java.io.File

const val WORKSPACE = "build/functionalTest"

const val DEFAULT_PACKAGE_NAME = "com.autonomousapps.test"
const val DEFAULT_PACKAGE_PATH = "com/autonomousapps/test"

interface ProjectDirProvider {
    val projectDir: File

    fun project(moduleName: String): Module
}

interface Module {
    val dir: File
    val variant: String?
}

class AppSpec(
    val sources: Map<String, String> = DEFAULT_APP_SOURCES,
    val dependencies: List<Pair<String, String>> = DEFAULT_APP_DEPENDENCIES
) {
    fun formattedDependencies(): String {
        return dependencies.joinToString(separator = "\n") { (conf, dep) ->
            if (dep.startsWith("project")) {
                "$conf $dep"
            } else {
                "$conf \"$dep\""
            }
        }
    }
}

enum class LibraryType {
    KOTLIN_ANDROID, JAVA_JVM, KOTLIN_JVM
}

class LibrarySpec(
    val name: String,
    val type: LibraryType,
    val dependencies: List<Pair<String, String>> = DEFAULT_LIB_DEPENDENCIES,
    val sources: Map<String, String> = when (type) {
        LibraryType.KOTLIN_ANDROID -> DEFAULT_SOURCE_KOTLIN_ANDROID
        LibraryType.JAVA_JVM -> DEFAULT_SOURCE_JAVA
        LibraryType.KOTLIN_JVM -> DEFAULT_SOURCE_KOTLIN_JVM
    }
) {
    fun formattedDependencies(): String {
        return dependencies.joinToString(separator = "\n") { (conf, dep) ->
            if (dep.startsWith("project")) {
                "$conf $dep"
            } else {
                "$conf \"$dep\""
            }
        }
    }
}

fun libraryFactory(projectDir: File, librarySpec: LibrarySpec): Module {
    return when (librarySpec.type) {
        LibraryType.KOTLIN_ANDROID -> AndroidLibModule(projectDir, librarySpec)
        LibraryType.JAVA_JVM -> JavaLibModule(projectDir, librarySpec)
        LibraryType.KOTLIN_JVM -> KotlinJvmLibModule(projectDir, librarySpec)
    }
}

abstract class BaseGradleProject(val projectDir: File) : Module {
    override val dir = projectDir.also { it.mkdirs() }
    fun withBuildFile(content: String, name: String = "build.gradle") {
        projectDir.resolve(name).also { it.parentFile.mkdirs() }.writeText(content.trimIndent())
    }

    fun withFile(relativePath: String, content: String) {
        projectDir.resolve(relativePath).also { it.parentFile.mkdirs() }.writeText(content.trimIndent())
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
    agpVersion: String = "3.5.3",
    appSpec: AppSpec = DEFAULT_APP_SPEC,
    librarySpecs: List<LibrarySpec>? = null
) : ProjectDirProvider {

    private val rootProject = RootProject(librarySpecs, agpVersion)

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

    override fun project(moduleName: String) = modules[moduleName] ?: error("No '$moduleName' project found!")
}

/**
 * The "app" module, a typical `com.android.application` project, with the `kotlin-android` plugin applied as well.
 */
class AppModule(rootProjectDir: File, appSpec: AppSpec, librarySpecs: List<LibrarySpec>? = null)
    : AndroidGradleProject(rootProjectDir.resolve("app").also { it.mkdirs() }) {

    override val variant = "debug"

    init {
        val agpVersion = "\${com.android.builder.model.Version.ANDROID_GRADLE_PLUGIN_VERSION}"
        val afterEvaluate = "afterEvaluate { println \"AGP version: $agpVersion\" }"
        withBuildFile("""
            plugins {
                id('com.android.application')
                id('kotlin-android')
            }
            android {
                compileSdkVersion 29
                defaultConfig {
                    applicationId "$DEFAULT_PACKAGE_NAME"
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
                ${librarySpecs?.map { it.name }?.joinToString("\n") { "implementation project(':$it')" }}
                ${appSpec.formattedDependencies()}
            }
            
            $afterEvaluate
        """
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
                        android:theme="@style/AppTheme.NoActionBar"
                        >
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>
            </manifest>            
        """
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
            </resources>
        """
        )
        withColorsFile("""
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="colorPrimaryDark">#0568ae</color>
                <color name="colorPrimary">#009fdb</color>
                <color name="colorAccent">#009fdb</color>
            </resources>
        """
        )

        appSpec.sources.forEach { (name, source) ->
            withJavaSrcFile("$DEFAULT_PACKAGE_PATH/$name", source)
        }
    }
}

class AndroidLibModule(rootProjectDir: File, librarySpec: LibrarySpec)
    : AndroidGradleProject(rootProjectDir.resolve(librarySpec.name).also { it.mkdirs() }) {

    override val variant = "debug"

    init {
        withBuildFile("""
            plugins {
                id('com.android.library')
                id('kotlin-android')
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

val DEFAULT_APP_SOURCES = mapOf("MainActivity.kt" to """
    package $DEFAULT_PACKAGE_NAME
                
    import androidx.appcompat.app.AppCompatActivity
                
    class MainActivity : AppCompatActivity() {
    }
""".trimIndent())

val DEPENDENCIES_KOTLIN_STDLIB = listOf("implementation" to "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.3.61")

val DEFAULT_APP_DEPENDENCIES = DEPENDENCIES_KOTLIN_STDLIB + listOf(
    "implementation" to "androidx.appcompat:appcompat:1.1.0",
    "implementation" to "androidx.core:core-ktx:1.1.0",
    "implementation" to "com.google.android.material:material:1.0.0",
    "implementation" to "androidx.constraintlayout:constraintlayout:1.1.3",
    "implementation" to "androidx.navigation:navigation-fragment-ktx:2.1.0",
    "implementation" to "androidx.navigation:navigation-ui-ktx:2.1.0"
)

val DEFAULT_APP_SPEC = AppSpec(
    sources = DEFAULT_APP_SOURCES,
    dependencies = DEFAULT_APP_DEPENDENCIES
)

val DEFAULT_LIB_DEPENDENCIES = DEPENDENCIES_KOTLIN_STDLIB + listOf(
    "implementation" to "androidx.appcompat:appcompat:1.1.0",
    "implementation" to "androidx.core:core-ktx:1.1.0",
    "implementation" to "com.google.android.material:material:1.0.0",
    "implementation" to "androidx.constraintlayout:constraintlayout:1.1.3",
    "implementation" to "androidx.navigation:navigation-fragment-ktx:2.1.0",
    "implementation" to "androidx.navigation:navigation-ui-ktx:2.1.0"
)

val DEFAULT_SOURCE_KOTLIN_ANDROID = mapOf("Library.kt" to """ 
    import androidx.core.provider.FontRequest
             
    class Library {
        fun magic() = 42

        fun font() = FontRequest("foo", "foo", "foo", 0) 
    }
""".trimIndent())

val DEFAULT_SOURCE_KOTLIN_JVM = mapOf("Library.kt" to """  
    class Library {
        fun magic() = 42 
    }
""".trimIndent())

val DEFAULT_SOURCE_JAVA = mapOf("Library.java" to """  
    class Library {
        public int magic() {
            return 42;
        }
    }
""".trimIndent())
