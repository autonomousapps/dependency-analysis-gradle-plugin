@file:Suppress("UnstableApiUsage")

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

version = "0.4"
group = "com.autonomousapps"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
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

buildScan {
    publishAlways()
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
}

tasks.withType(PluginUnderTestMetadata::class.java).configureEach {
    pluginClasspath.from(configurations.compileOnly)
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
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

tasks.check {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
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
    implementation(files("libs/asm-7.2.jar"))

    compileOnly("com.android.tools.build:gradle:3.5.2") {
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
