@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.gradle.build-scan") version "3.0"
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "0.10.1"
    id("org.jetbrains.kotlin.jvm") version "1.3.61"
    `kotlin-dsl`
}

repositories {
    jcenter()
    google()
}

// This is needed for the smoke tests
val latestRelease = "0.13.1"
version = "0.13.2-SNAPSHOT"
group = "com.autonomousapps"

buildScan {
    publishAlways()
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

// Add a source set for the functional test suite. This must come _above_ the `dependencies` block.
val functionalTestSourceSet = sourceSets.create("functionalTest") {
    compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
    runtimeClasspath += output + compileClasspath
}
val functionalTestImplementation = configurations.getByName("functionalTestImplementation")
    .extendsFrom(configurations.getByName("testImplementation"))

// Add a source set for the smoke test suite. This must come _above_ the `dependencies` block.
val smokeTestSourceSet = sourceSets.create("smokeTest") {
    compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
    runtimeClasspath += output + compileClasspath
}
configurations.getByName("smokeTestImplementation")
    .extendsFrom(functionalTestImplementation)

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.squareup.moshi:moshi:1.8.0") {
        because("For writing reports in JSON format")
    }
    implementation("com.squareup.moshi:moshi-kotlin:1.8.0") {
        because("For writing reports based on Kotlin classes")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.6.10") {
        because("For writing HTML reports")
    }
    implementation("org.jetbrains.kotlin:kotlin-reflect") {
        because("For Kotlin ABI analysis")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.1.0") {
        because("For Kotlin ABI analysis")
    }
    implementation(files("libs/asm-7.2.jar"))

    compileOnly("com.android.tools.build:gradle:3.5.3") {
        because("Auto-wiring into Android projects")
    }
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin") {
        because("Auto-wiring into Kotlin projects")
    }

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0") {
        because("Writing manual stubs for Configuration seems stupid")
    }

    functionalTestImplementation("commons-io:commons-io:2.6") {
        because("For FileUtils.deleteDirectory()")
    }
}

tasks.jar {
    // Bundle shaded ASM jar into final artifact
    from(zipTree("libs/asm-7.2.jar"))
}

gradlePlugin {
    plugins {
        create("dependencyAnalysisPlugin") {
            id = "com.autonomousapps.dependency-analysis"
            implementationClass = "com.autonomousapps.DependencyAnalysisPlugin"
        }
    }
}

// For publishing to the Gradle Plugin Portal
// https://plugins.gradle.org/docs/publish-plugin
pluginBundle {
    website = "https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin"
    vcsUrl = "https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin"

    description = "A plugin to report mis-used dependencies in your Android project"

    (plugins) {
        "dependencyAnalysisPlugin" {
            displayName = "Android Dependency Analysis Gradle Plugin"
            tags = listOf("android", "dependencies")
        }
    }
}

gradlePlugin.testSourceSets(functionalTestSourceSet, smokeTestSourceSet)

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    mustRunAfter(tasks.named("test"))

    description = "Runs the functional tests."
    group = "verification"

    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath

    // Workaround for https://github.com/gradle/gradle/issues/4506#issuecomment-570815277
    systemProperty("org.gradle.testkit.dir", file("${buildDir}/tmp/test-kit"))

    beforeTest(closureOf<TestDescriptor> {
        logger.lifecycle("Running test: $this")
    })
}

// Add a task to run the smoke tests. This is imperfect in that it requires something to be published for testing.
// Ideally we'll be publishing snapshots at some point, and we'll test those before publishing the stable version.
val smokeTest by tasks.registering(Test::class) {
    mustRunAfter(tasks.named("test"), functionalTest)

    description = "Runs the smoke tests."
    group = "verification"

    testClassesDirs = smokeTestSourceSet.output.classesDirs
    classpath = smokeTestSourceSet.runtimeClasspath

    systemProperty("com.autonomousapps.version", latestRelease)

    beforeTest(closureOf<TestDescriptor> {
        logger.lifecycle("Running test: $this")
    })
}

tasks.withType<PluginUnderTestMetadata>().configureEach {
    pluginClasspath.from(configurations.compileOnly)
}

val check = tasks.named("check")
check.configure {
    // Run the functional tests as part of `check`
    // Do NOT add smokeTest here. It would be too slow.
    dependsOn(functionalTest)
}

tasks.named("publishPlugins") {
    // Note that publishing requires a successful smokeTest
    dependsOn(check, smokeTest)
}
