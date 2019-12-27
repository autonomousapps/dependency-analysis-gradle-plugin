package com.autonomousapps.utils

import java.io.File

const val WORKSPACE = "build/functionalTest"

interface ProjectDirProvider {
    val projectDir: File
}

interface Module {
    val dir: File
}

enum class LibraryType {
    ANDROID_LIBRARY, JAVA_LIBRARY, KOTLIN_LIBRARY
}

interface LibrarySpec {
    val name: String
    val type: LibraryType
}

fun libraryFactory(projectDir: File, librarySpec: LibrarySpec): Module {
    return when(librarySpec.type) {
        LibraryType.ANDROID_LIBRARY -> AndroidLibModule(projectDir, librarySpec.name)
        LibraryType.JAVA_LIBRARY -> JavaLibModule(projectDir, librarySpec.name)
        LibraryType.KOTLIN_LIBRARY -> KotlinJvmLibModule(projectDir, librarySpec.name)
    }
}

abstract class BaseGradleProject(val projectDir: File): Module {
    override val dir = projectDir.also { it.mkdirs() }
    fun withBuildFile(content: String, name: String = "build.gradle") {
        projectDir.resolve(name).also { it.parentFile.mkdirs() }.writeText(content.trimIndent())
    }
    fun withFile(relativePath: String, content: String ) {
        projectDir.resolve(relativePath).also { it.parentFile.mkdirs() }.writeText(content.trimIndent())
    }
}

abstract class AndroidGradleProject(projectDir: File): BaseGradleProject(projectDir)  {
    val mainDir = projectDir.resolve("src/main").also { it.mkdirs() }
    val mainJavaSrcDir = mainDir.resolve("java").also { it.mkdirs() }
    val resDir = mainDir.resolve("res").also { it.mkdirs() }

    fun withManifestFile(content: String, relativePath: String = "AndroidManifest.xml"  ) {
        mainDir.resolve(relativePath).writeText(content.trimIndent())
    }
    fun withJavaSrcFile(relativePath: String, content: String ) {
        val srcFile = mainJavaSrcDir.resolve(relativePath).also { it.parentFile.mkdirs() }
        srcFile.writeText(content.trimIndent())
    }
    fun withStylesFile(content: String, relativePath: String = "values/styles.xml"  ) {
        resDir.resolve(relativePath).also { it.parentFile.mkdirs() }.writeText(content.trimIndent())
    }
    fun withColorsFile(content: String, relativePath: String = "values/colors.xml"  ) {
        resDir.resolve(relativePath).also { it.parentFile.mkdirs() }.writeText(content.trimIndent())
    }
}

abstract class JavaGradleProject(projectDir: File): BaseGradleProject(projectDir) {
    val mainSrcDir = projectDir.resolve("src/main/java").also { it.mkdirs() }
    fun withSrcFile(relativePath: String, content: String ) {
        mainSrcDir.resolve(relativePath).also { it.parentFile.mkdirs() }.writeText(content.trimIndent())
    }
}

abstract class KotlinGradleProject(projectDir: File): BaseGradleProject(projectDir) {
    val mainSrcDir = projectDir.resolve("src/main/kotlin").also { it.mkdirs() }
    fun withSrcFile(relativePath: String, content: String ) {
        mainSrcDir.resolve(relativePath).also { it.parentFile.mkdirs() }.writeText(content.trimIndent())
    }
}

abstract class RootGradleProject(projectDir: File): BaseGradleProject(projectDir) {
    fun withSettingsFile(content: String, name: String = "settings.gradle") {
        projectDir.resolve(name).also { it.parentFile.mkdirs() }.writeText(content.trimIndent())
    }
}


/**
 * A typical Android project, with an "app" module (which has applied the `com.android.application` plugin, one or more
 * android-library modules (`com.android.library` plugin), and one or more java-library modules (`java-library` plugin).
 *
 * @param librarySpecs a list of android-library project names and types. Can be null. See [LibrarySpec] and
 * [LibraryType].
 */
class AndroidProject(
    agpVersion: String = "3.5.3",
    librarySpecs: List<LibrarySpec>? = null
) : ProjectDirProvider {

    private val rootProject = RootProject(agpVersion, librarySpecs)

    /**
     * Feed this to a [GradleRunner][org.gradle.testkit.runner.GradleRunner].
     */
    override val projectDir = rootProject.projectDir

    // A collection of Android modules (one "app" module and zero or more library modules), keyed by their respective
    // names)
    private val modules: Map<String, Module> = mapOf(
        "app" to AppModule(projectDir, librarySpecs),
        *librarySpecs?.map { spec ->
            spec.name to libraryFactory(projectDir, spec)
        }?.toTypedArray() ?: emptyArray()
    )

    fun project(moduleName: String) = modules[moduleName] ?: error("No '$moduleName' project found!")
}

/**
 * Typical root project of an Android build. Contains a `settings.gradle` and `build.gradle`.
 */
class RootProject(agpVersion: String = "3.5.3", librarySpecs: List<LibrarySpec>? = null)
    : RootGradleProject(File(WORKSPACE)) {

    init {
        withSettingsFile("""
            |rootProject.name = 'real-app'
            |
            |include(':app')
            |${librarySpecs?.map { it.name }?.joinToString("\n") { "include(':$it')" }}
        """.trimMargin("|"))

        withBuildFile("""
            buildscript {
                repositories {
                    google()
                    jcenter()
                }
                dependencies {
                    classpath 'com.android.tools.build:gradle:$agpVersion'
                    classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.61"
                }
            }
            plugins {
                id('com.autonomousapps.dependency-analysis')
            }
            subprojects {
                repositories {
                    google()
                    jcenter()
                }
            }
        """)
    }
}

/**
 * The "app" module, a typical `com.android.application` project, with the `kotlin-android` plugin applied as well.
 */
class AppModule(rootProjectDir: File, librarySpecs: List<LibrarySpec>? = null)
    : AndroidGradleProject(rootProjectDir.resolve("app").also { it.mkdirs() }) {

    init {
        withBuildFile("""
            plugins {
                id('com.android.application')
                id('kotlin-android')
            }
            android {
                compileSdkVersion 29
                defaultConfig {
                    applicationId "com.autonomousapps.test"
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
            
                implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.3.50"
                implementation 'androidx.appcompat:appcompat:1.1.0'
                implementation 'androidx.core:core-ktx:1.1.0'
                implementation 'com.google.android.material:material:1.0.0'
                implementation 'androidx.constraintlayout:constraintlayout:1.1.3'
                implementation 'androidx.navigation:navigation-fragment-ktx:2.1.0'
                implementation 'androidx.navigation:navigation-ui-ktx:2.1.0'
            }
        """
        )
        withManifestFile("""
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="com.autonomousapps.test"
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

        withJavaSrcFile("com/autonomousapps/test/MainActivity.kt","""
            package com.autonomousapps.test
            
            import androidx.appcompat.app.AppCompatActivity
            
            class MainActivity : AppCompatActivity() {
            }
        """
        )
    }
}

class AndroidLibModule(rootProjectDir: File, libName: String)
    : AndroidGradleProject(rootProjectDir.resolve(libName).also { it.mkdirs() }) {

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
                implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.3.50"
                implementation 'androidx.appcompat:appcompat:1.1.0'
                implementation 'androidx.core:core-ktx:1.1.0'
                implementation 'com.google.android.material:material:1.0.0'
                implementation 'androidx.constraintlayout:constraintlayout:1.1.3'
                implementation 'androidx.navigation:navigation-fragment-ktx:2.1.0'
                implementation 'androidx.navigation:navigation-ui-ktx:2.1.0'
            }
        """
        )
        withManifestFile("""
            <?xml version="1.0" encoding="utf-8"?>
            <manifest package="com.autonomousapps.test.$libName" />            
        """
        )
        withJavaSrcFile("com/autonomousapps/test/android/${libName}/Library.kt","""
            package com.autonomousapps.test.android.$libName
             
            import androidx.core.provider.FontRequest
             
            class Library {
                fun magic() = 42
                
                fun font() = FontRequest("foo", "foo", "foo", 0) 
            }
        """
        )
    }
}

/**
 * No Kotlin in this one.
 */
class JavaLibModule(rootProjectDir: File, libName: String)
    : JavaGradleProject(rootProjectDir.resolve(libName).also { it.mkdirs() }) {

    init {
        withBuildFile("""
            plugins {
                id('java-library')
            }
            dependencies {
                api 'org.apache.commons:commons-math3:3.6.1'
                implementation 'com.google.guava:guava:28.0-jre'
            }
        """
        )
        withSrcFile("com/autonomousapps/test/java/${libName}/Library.java","""
            package com.autonomousapps.test.java.$libName;
              
            class Library {
                public int magic() {
                    return 42;
                }
            }
        """
        )
    }
}

/**
 * No Android or Java, just Kotlin.
 */
class KotlinJvmLibModule(rootProjectDir: File, libName: String)
    : KotlinGradleProject(rootProjectDir.resolve(libName).also { it.mkdirs() }) {

    init {
        withBuildFile("""
            plugins {
                id('java-library')
                id('org.jetbrains.kotlin.jvm')
            }
            dependencies {
                implementation platform('org.jetbrains.kotlin:kotlin-bom')
                implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8'
                api 'org.apache.commons:commons-math3:3.6.1'
                implementation 'com.google.guava:guava:28.0-jre'
            }
        """
        )
        withSrcFile("com/autonomousapps/test/kotlin/${libName}/Library.kt", """
            package com.autonomousapps.test.kotlin.$libName
              
            class Library {
                fun magic() = 42
            }
        """
        )
    }
}
