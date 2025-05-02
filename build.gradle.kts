// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage", "HasPlatformType", "PropertyName")

plugins {
  id("java-gradle-plugin")
  id("com.gradle.plugin-publish")
  `kotlin-dsl`
  id("groovy")
  id("convention")
  alias(libs.plugins.dependencyAnalysis)
  id("com.autonomousapps.testkit")
}

// This version string comes from gradle.properties
val VERSION: String by project
version = VERSION

val isSnapshot: Boolean = project.version.toString().endsWith("SNAPSHOT")
val isRelease: Boolean = !isSnapshot

dagp {
  version(version)
  pom {
    name.set("Dependency Analysis Gradle Plugin")
    description.set("Analyzes dependency usage in Android and JVM projects")
    inceptionYear.set("2019")
  }
  publishTaskDescription(
    "Publishes plugin marker and plugin artifacts to Maven Central " +
      "(${if (version.toString().endsWith("SNAPSHOT")) "snapshots" else "staging"})"
  )
}

// For publishing to the Gradle Plugin Portal
// https://plugins.gradle.org/docs/publish-plugin
gradlePlugin {
  plugins {
    create("dependencyAnalysisPlugin") {
      id = "com.autonomousapps.dependency-analysis"
      implementationClass = "com.autonomousapps.DependencyAnalysisPlugin"

      displayName = "Dependency Analysis Gradle Plugin"
      description = "A plugin to report mis-used dependencies in your JVM or Android project"
      tags.set(listOf("java", "kotlin", "groovy", "scala", "android", "dependencies"))
    }

    create("buildHealthPlugin") {
      id = "com.autonomousapps.build-health"
      implementationClass = "com.autonomousapps.BuildHealthPlugin"

      displayName = "Build Health Gradle Plugin"
      description = "A plugin to report on the health of your JVM or Android build"
      tags.set(listOf("java", "kotlin", "groovy", "scala", "android", "dependencies"))
    }
  }

  website.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
  vcsUrl.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
}

val main = sourceSets["main"]
val commonTest = sourceSets.create("commonTest") {
  java {
    srcDir("src/commonTest/kotlin")
  }
}

sourceSets {
  test {
    compileClasspath += commonTest.output
    runtimeClasspath += output + compileClasspath
  }
}

// Add a source set for the functional test suite. This must come _above_ the `dependencies` block.
val functionalTestSourceSet = sourceSets.maybeCreate("functionalTest").apply {
  compileClasspath += main.output + configurations["testRuntimeClasspath"] + commonTest.output
  runtimeClasspath += output + compileClasspath
}

val functionalTestImplementation = configurations
  .getByName("functionalTestImplementation")
  .extendsFrom(configurations.getByName("testImplementation"))
val functionalTestApi = configurations.getByName("functionalTestApi")

val compileFunctionalTestKotlin = tasks.named("compileFunctionalTestKotlin")
tasks.named<AbstractCompile>("compileFunctionalTestGroovy") {
  dependsOn(compileFunctionalTestKotlin)
  classpath += files(compileFunctionalTestKotlin.get().outputs.files)
}

// Add a source set for the smoke test suite. This must come _above_ the `dependencies` block.
val smokeTestSourceSet = sourceSets.create("smokeTest") {
  compileClasspath += main.output + configurations["testRuntimeClasspath"]
  runtimeClasspath += output + compileClasspath
}
val smokeTestImplementation = configurations
  .getByName("smokeTestImplementation")
  .extendsFrom(functionalTestImplementation)

gradleTestKitSupport {
  withSupportLibrary()
  withTruthLibrary()
}

dependencies {
  implementation(platform(libs.okio.bom))

  api(libs.javax.inject)
  api(libs.moshi.core)
  api(libs.moshix.sealed.runtime)

  implementation(project(":graph-support"))
  implementation(libs.guava)
  implementation(libs.kotlin.stdlib.jdk8)
  implementation(libs.kotlin.editor.relocated)
  // implementation(libs.kryo5)
  implementation(libs.moshi.kotlin)
  implementation(libs.moshix.sealed.reflect)
  implementation(libs.okio)
  implementation(libs.kotlin.metadata.jvm)
  implementation(libs.caffeine) {
    because("High performance, concurrent cache")
  }
  implementation(libs.relocated.antlr)
  implementation(libs.relocated.asm)

  runtimeOnly(libs.kotlin.reflect) {
    because("For Kotlin ABI analysis")
  }

  compileOnly(libs.agp) {
    because("Auto-wiring into Android projects")
  }
  compileOnly(libs.android.tools.common) {
    because("com.android.Version")
  }
  compileOnly(libs.kotlin.gradle) {
    because("Auto-wiring into Kotlin projects")
  }

  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.spock) {
    exclude(group = "org.codehaus.groovy")
    because("For Spock tests")
  }
  testImplementation(libs.junit.api)
  testImplementation(libs.junit.params)
  testImplementation(libs.truth)
  testRuntimeOnly(libs.junit.engine)

  smokeTestImplementation(libs.commons.io) {
    because("For FileUtils.deleteDirectory()")
  }
}

fun shadowed(): Action<ExternalModuleDependency> = Action {
  attributes {
    attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED))
  }
}

// additive (vs testSourceSets() which _sets_)
gradlePlugin.testSourceSet(smokeTestSourceSet)

// CI cannot handle too much parallelization. Runs out of metaspace.
fun maxParallelForks() =
  if (isCi) 1
  else Runtime.getRuntime().availableProcessors() / 2

val isCi = providers.environmentVariable("CI")
  .getOrElse("false")
  .toBoolean()

// This will slow down tests on CI, but maybe it won't run out of metaspace.
fun forkEvery(): Long = if (isCi) 40 else 0

// Add a task to run the functional tests
// quickTest only runs against the latest gradle version. For iterating faster
fun quickTest(): Boolean = providers
  .systemProperty("funcTest.quick")
  .orNull != null

val functionalTest = tasks.named("functionalTest", Test::class) {
  // forking JVMs is very expensive, and only necessary with full test runs
  forkEvery = forkEvery()
  maxParallelForks = maxParallelForks()

  jvmArgs("-XX:+HeapDumpOnOutOfMemoryError", "-XX:MaxMetaspaceSize=1g")

  systemProperty("com.autonomousapps.quick", "${quickTest()}")
  systemProperty("com.autonomousapps.test.versions.kotlin", libs.versions.kotlin.get())

  beforeTest(closureOf<TestDescriptor> {
    logger.lifecycle("Running test: $this")
  })

  // ./gradlew :functionalTest -DfuncTest.package=<all|jvm|android>
  val testKindLog = when (providers.systemProperty("funcTest.package").getOrElse("all").lowercase()) {
    "jvm" -> {
      include("com/autonomousapps/jvm/**")

      "Run JVM tests"
    }

    "android" -> {
      include("com/autonomousapps/android/**")

      // Android requires JDK 17 from AGP 8.0.
      javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
      })

      "Run Android tests"
    }

    else -> {
      // Android requires JDK 17 from AGP 8.0.
      javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
      })

      "Run all tests"
    }
  }

  doFirst {
    logger.quiet(">>> $testKindLog (use '-DfuncTest.package=<android|jvm|all>' to change filter)")
  }
}

val quickFunctionalTest by tasks.registering {
  dependsOn(functionalTest)
  System.setProperty("funcTest.quick", "true")
}

val smokeTestVersionKey = "com.autonomousapps.version"
val smokeTestVersion: String = System.getProperty(smokeTestVersionKey, latestRelease())

val smokeTest by tasks.registering(Test::class) {
  mustRunAfter(tasks.named("test"), functionalTest)

  description = "Runs the smoke tests."
  group = "verification"

  testClassesDirs = smokeTestSourceSet.output.classesDirs
  classpath = smokeTestSourceSet.runtimeClasspath

  jvmArgs("-XX:+HeapDumpOnOutOfMemoryError", "-XX:MaxMetaspaceSize=1g")

  systemProperty(smokeTestVersionKey, smokeTestVersion)

  beforeTest(closureOf<TestDescriptor> {
    logger.lifecycle("Running test: $this")
  })
}

/**
 * Algorithm:
 * 1. If version is !SNAPSHOT, latestRelease is identical, but with -SNAPSHOT suffix.
 * 2. If version is SNAPSHOT, latestRelease is non-SNAPSHOT, and patch should be -1 by comparison.
 */
fun latestRelease(): String {
  val v = version.toString()
  return if (isRelease) {
    "$v-SNAPSHOT"
  } else {
    val regex = """(\d+).(\d+).(\d+)(?:-rc\d{2})?-SNAPSHOT""".toRegex()
    val groups = regex.find(v)!!.groupValues
    val major = groups[1].toInt()
    val minor = groups[2].toInt()
    val patch = groups[3].toInt()

    "$major.$minor.${patch - 1}"
  }
}

val check = tasks.named("check")

val publishToMavenCentral = tasks.named("publishToMavenCentral") {
  // Note that publishing a release requires a successful smokeTest
  if (isRelease) {
    dependsOn(check, smokeTest)
  }
}

val publishToPluginPortal = tasks.named("publishPlugins") {
  // Can't publish snapshots to the portal
  onlyIf { isRelease }
  shouldRunAfter(publishToMavenCentral)

  // Note that publishing a release requires a successful smokeTest
  if (isRelease) {
    dependsOn(check, smokeTest)
  }
}

tasks.register("publishEverywhere") {
  dependsOn(publishToMavenCentral, publishToPluginPortal)

  group = "publishing"
  description = "Publishes to Plugin Portal and Maven Central"
}

tasks.withType<GroovyCompile>().configureEach {
  options.isIncremental = true
}

// TODO(tsr): gzip. also register this task in ProjectPlugin
// To run:
// ```
// ./gradlew :readFile --input path/to/gzipped-file
// ```
tasks.register<com.autonomousapps.convention.tasks.GunzipTask>("gunzip") {
  runtimeClasspath.setFrom(sourceSets.main.map { it.runtimeClasspath })
  projectDir.set(layout.projectDirectory)
  outputDir.set(layout.buildDirectory.dir("gzip"))
}

dependencyAnalysis {
  structure {
    bundle("agp") {
      primary("com.android.tools.build:gradle")
      includeGroup("com.android.tools")
      includeGroup("com.android.tools.build")
    }
    bundle("kgp") {
      includeDependency("org.jetbrains.kotlin:kotlin-gradle-plugin")
      includeDependency("org.jetbrains.kotlin:kotlin-gradle-plugin-api")
    }
    bundle("truth") {
      includeDependency(libs.truth)
      // Truth's Subject class makes use of the @Nullable annotation from this library when creating `Factory`s. It ends
      // up in the bytecode, but I don't really have any control over that.
      includeDependency("org.checkerframework:checker-qual")
    }
  }

  abi {
    exclusions {
      excludeSourceSets(
        // These source sets have an "...Api" configuration, but have no ABI, semantically. Exclude them.
        "functionalTest", "smokeTest"
      )
    }
  }

  issues {
    all {
      onAny {
        severity("fail")
      }
    }
  }
}
