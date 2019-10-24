@file:Suppress("UnstableApiUsage")

plugins {
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "0.10.1"
    id("org.jetbrains.kotlin.jvm") version "1.3.50"
    id("com.gradle.build-scan") version "2.4.1"
    `kotlin-dsl`
}

repositories {
    jcenter()
    google()
}

buildScan {
    publishAlways()
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.squareup.moshi:moshi:1.8.0") {
        because("For writing reports in JSON format")
    }
    implementation("com.squareup.moshi:moshi-kotlin:1.8.0") {
        because("For writing reports based on Kotlin classes")
    }
    implementation("org.ow2.asm:asm:7.2")
    implementation("org.ow2.asm:asm-tree:7.2")

    compileOnly("com.android.tools.build:gradle:3.5.1") {
        because("Auto-wiring into Android projects")
    }
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.50") {
        because("Auto-wiring into Kotlin projects")
    }

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

gradlePlugin {
    plugins {
        create("clocPlugin") {
            id = "com.autonomousapps.dependency-analysis"
            implementationClass = "com.autonomousapps.DependencyAnalysisPlugin"
        }
    }
}
