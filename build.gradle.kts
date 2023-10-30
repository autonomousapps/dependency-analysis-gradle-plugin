@file:Suppress("UnstableApiUsage", "HasPlatformType", "PropertyName")

import org.jetbrains.kotlin.cli.common.toBooleanLenient

plugins {
  id("java-gradle-plugin")
  id("com.gradle.plugin-publish")
  `kotlin-dsl`
  id("groovy")
  id("convention")
  id("com.autonomousapps.dependency-analysis")
  id("com.autonomousapps.testkit-plugin")
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
    description.set("Analyzes dependency usage in Android and Java/Kotlin projects")
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

dependencies {
  implementation(platform(libs.kotlin.bom))

  api(libs.guava) {
    because("Graphs")
  }
  api(libs.javax.inject)
  api(libs.moshi.core)
  api(libs.moshix.sealed.runtime)

  implementation(project(":graph-support"))
  implementation(libs.kotlin.stdlib.jdk8)
  implementation(libs.moshi.kotlin)
  implementation(libs.moshix.sealed.reflect)
  implementation(libs.okio)

  implementation(libs.kotlinx.metadata.jvm) {
    because("For Kotlin ABI analysis")
    // Depends on Kotlin 1.6, which I don't want. We also don't want to set a strict constraint, because
    // I think that is exposed to consumers, and which would invariably break their projects. In the end,
    // this is merely aesthetic.
    isTransitive = false
  }
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

  // KGP automatically adds an 'api' to all source sets even when it makes no sense. To appease DAGP, we respect that.
  // This might go away with Kotlin 2.0.
  functionalTestApi("com.autonomousapps:gradle-testkit-support")
  functionalTestImplementation("com.autonomousapps:gradle-testkit-truth")

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

// TODO: I'm not sure I need this for _unit tests_
tasks.withType<Test>().configureEach {
  jvmArgs("-XX:+HeapDumpOnOutOfMemoryError", "-XX:MaxMetaspaceSize=1g")
}

// CI cannot handle too much parallelization. Runs out of metaspace.
fun maxParallelForks() =
  if (isCi) 1
  else Runtime.getRuntime().availableProcessors() / 2

val isCi = providers.environmentVariable("CI")
  .getOrElse("false")
  .toBooleanLenient()!!

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

  systemProperty("com.autonomousapps.quick", "${quickTest()}")

  beforeTest(closureOf<TestDescriptor> {
    logger.lifecycle("Running test: $this")
  })

  // ./gradlew :functionalTest -DfuncTest.package=<all|jvm|android>
  when (providers.systemProperty("funcTest.package").getOrElse("all").lowercase()) {
    "jvm" -> {
      logger.quiet("Run JVM tests")
      include("com/autonomousapps/jvm/**")
    }

    "android" -> {
      logger.quiet("Run Android tests")
      include("com/autonomousapps/android/**")

      // Android requires JDK 17 from AGP 8.0.
      javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
      })
    }

    else -> {
      logger.quiet("Run all tests")

      // Android requires JDK 17 from AGP 8.0.
      javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
      })
    }
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
  }

  issues {
    all {
      onAny {
        severity("fail")
      }
    }
  }
}
