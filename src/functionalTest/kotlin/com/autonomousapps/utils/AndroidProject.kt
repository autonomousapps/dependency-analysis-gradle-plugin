package com.autonomousapps.utils

import java.io.File

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
class RootProject(agpVersion: String = "3.5.3", librarySpecs: List<LibrarySpec>? = null) {

    val projectDir = File("build/functionalTest").also { it.mkdirs() }

    init {
        projectDir.resolve("settings.gradle").writeText("""
            |rootProject.name = 'real-app'
            |
            |include(':app')
            |${librarySpecs?.map { it.name }?.joinToString("\n") { "include(':$it')" }}
        """.trimMargin("|"))
        projectDir.resolve("build.gradle").writeText("""
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
        """.trimIndent())
    }
}

/**
 * The "app" module, a typical `com.android.application` project, with the `kotlin-android` plugin applied as well.
 */
class AppModule(projectDir: File, librarySpecs: List<LibrarySpec>? = null) : Module {

    override val dir = projectDir.resolve("app").also { it.mkdirs() }

    init {
        dir.resolve("build.gradle").writeText("""
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
        """.trimIndent()
        )
        val mainDir = dir.resolve("src/main")
        mainDir.mkdirs()
        //android:icon="@mipmap/ic_launcher"
        //android:roundIcon="@mipmap/ic_launcher_round"
        mainDir.resolve("AndroidManifest.xml").writeText("""
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
        """.trimIndent()
        )
        val resDir = mainDir.resolve("res")
        resDir.mkdirs()

        val valuesDir = resDir.resolve("values")
        valuesDir.mkdirs()
        valuesDir.resolve("styles.xml").writeText("""
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
        """.trimIndent()
        )
        valuesDir.resolve("colors.xml").writeText("""
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="colorPrimaryDark">#0568ae</color>
                <color name="colorPrimary">#009fdb</color>
                <color name="colorAccent">#009fdb</color>
            </resources>
        """.trimIndent()
        )
        val packageRoot = mainDir.resolve("java/com/autonomousapps/test")
        packageRoot.mkdirs()
        packageRoot.resolve("MainActivity.kt").writeText("""
            package com.autonomousapps.test
            
            import androidx.appcompat.app.AppCompatActivity
            
            class MainActivity : AppCompatActivity() {
            }
        """.trimIndent()
        )
    }
}

class AndroidLibModule(projectDir: File, libName: String) : Module {

    override val dir = projectDir.resolve(libName).also { it.mkdirs() }

    init {
        dir.resolve("build.gradle").writeText("""
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
        """.trimIndent()
        )
        val mainDir = dir.resolve("src/main")
        mainDir.mkdirs()
        mainDir.resolve("AndroidManifest.xml").writeText("""
            <?xml version="1.0" encoding="utf-8"?>
            <manifest package="com.autonomousapps.test.$libName" />            
        """.trimIndent()
        )

        val packageRoot = mainDir.resolve("java/com/autonomousapps/test/android/$libName")
        packageRoot.mkdirs()
        packageRoot.resolve("Library.kt").writeText("""
            package com.autonomousapps.test.android.$libName
             
            import androidx.core.provider.FontRequest
             
            class Library {
                fun magic() = 42
                
                fun font() = FontRequest("foo", "foo", "foo", 0) 
            }
        """.trimIndent()
        )
    }
}

/**
 * No Kotlin in this one.
 */
class JavaLibModule(projectDir: File, libName: String) : Module {

    override val dir = projectDir.resolve(libName).also { it.mkdirs() }

    init {
        dir.resolve("build.gradle").writeText("""
            plugins {
                id('java-library')
            }
            dependencies {
                api 'org.apache.commons:commons-math3:3.6.1'
                implementation 'com.google.guava:guava:28.0-jre'
            }
        """.trimIndent()
        )
        val mainDir = dir.resolve("src/main")
        mainDir.mkdirs()

        val packageRoot = mainDir.resolve("java/com/autonomousapps/test/java/$libName")
        packageRoot.mkdirs()
        packageRoot.resolve("Library.java").writeText("""
            package com.autonomousapps.test.java.$libName
              
            class Library {
                public int magic() {
                    return 42;
                }
            }
        """.trimIndent()
        )
    }
}

/**
 * No Android or Java, just Kotlin.
 */
class KotlinJvmLibModule(projectDir: File, libName: String) : Module {

    override val dir = projectDir.resolve(libName).also { it.mkdirs() }

    init {
        dir.resolve("build.gradle").writeText("""
            plugins {
                id('java-library')
                id('org.jetbrains.kotlin.jvm') version '1.3.61'
            }
            dependencies {
                implementation platform('org.jetbrains.kotlin:kotlin-bom')
                implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8'
                api 'org.apache.commons:commons-math3:3.6.1'
                implementation 'com.google.guava:guava:28.0-jre'
            }
        """.trimIndent()
        )
        val mainDir = dir.resolve("src/main")
        mainDir.mkdirs()

        val packageRoot = mainDir.resolve("java/com/autonomousapps/test/kotlin/$libName")
        packageRoot.mkdirs()
        packageRoot.resolve("Library.java").writeText("""
            package com.autonomousapps.test.kotlin.$libName
              
            class Library {
                fun magic() = 42
            }
        """.trimIndent()
        )
    }
}
