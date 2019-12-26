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

version = "0.12.1-SNAPSHOT"
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

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
    compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
    runtimeClasspath += output + compileClasspath
}

gradlePlugin.testSourceSets(functionalTestSourceSet)
configurations.getByName("functionalTestImplementation").extendsFrom(configurations.getByName("testImplementation"))

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    description = "Runs the functional tests."
    group = "verification"

    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath

    mustRunAfter(tasks.named("test"))
}

tasks.withType<PluginUnderTestMetadata>().configureEach {
    pluginClasspath.from(configurations.compileOnly)
}

tasks.check {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}
