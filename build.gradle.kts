@file:Suppress("UnstableApiUsage", "HasPlatformType", "PropertyName")

import org.jetbrains.kotlin.cli.common.toBooleanLenient
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
plugins {
  `java-gradle-plugin`
//  id("com.gradle.plugin-publish")
  id("org.jetbrains.kotlin.jvm")
  `kotlin-dsl`
  groovy
  id("convention")
  id("com.autonomousapps.dependency-analysis")
}

// This version string comes from gradle.properties
val VERSION: String by project
version = VERSION
group = "com.autonomousapps"

val isSnapshot: Boolean = project.version.toString().endsWith("SNAPSHOT")
val isRelease: Boolean = !isSnapshot

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    jvmTarget = libs.versions.java.get()
    freeCompilerArgs = listOf("-Xinline-classes", "-opt-in=kotlin.RequiresOptIn", "-Xsam-conversions=class")
  }
}

dagp {
  version(version)
  pom {
    name.set("Dependency Analysis Gradle Plugin")
    description.set("Analyzes dependency usage in Android and Java/Kotlin projects")
    inceptionYear.set("2019")
  }
//  publishTaskDescription(
//    "Publishes plugin marker and plugin artifacts to Maven Central " +
//      "(${if (version.toString().endsWith("SNAPSHOT")) "snapshots" else "staging"})"
//  )
}

gradlePlugin {
  plugins {
    create("dependencyAnalysisPlugin") {
      id = "com.autonomousapps.dependency-analysis"
      implementationClass = "com.autonomousapps.DependencyAnalysisPlugin"
    }
  }
}

//// For publishing to the Gradle Plugin Portal
//// https://plugins.gradle.org/docs/publish-plugin
//pluginBundle {
//  website = "https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin"
//  vcsUrl = "https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin"
//
//  description = "A plugin to report mis-used dependencies in your Android project"
//
//  (plugins) {
//    "dependencyAnalysisPlugin" {
//      displayName = "Android Dependency Analysis Gradle Plugin"
//      tags = listOf("android", "dependencies")
//    }
//  }
//}

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
val functionalTestSourceSet = sourceSets.create("functionalTest") {
  compileClasspath += main.output + configurations["testRuntimeClasspath"] + commonTest.output
  runtimeClasspath += output + compileClasspath
}

val functionalTestImplementation = configurations
  .getByName("functionalTestImplementation")
  .extendsFrom(configurations.getByName("testImplementation"))

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
configurations
  .getByName("smokeTestImplementation")
  .extendsFrom(functionalTestImplementation)

// We only use the Jupiter platform (JUnit 5)
configurations.all {
  exclude(mapOf("group" to "junit", "module" to "junit"))
  exclude(mapOf("group" to "org.junit.vintage", "module" to "junit-vintage-engine"))
}

dependencies {
  implementation(libs.relocated.antlr)
  implementation(libs.relocated.asm)
  implementation(platform(libs.kotlin.bom))

  api(libs.javax.inject)
  api(libs.moshi.core)
  api(libs.moshix.sealed.runtime)

  implementation(project(":graph-support"))
  implementation(libs.kotlin.stdlib.jdk8)
  implementation(libs.moshi.kotlin)
  implementation(libs.moshix.sealed.reflect)
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
  implementation(libs.guava) {
    because("Graphs")
  }

  runtimeOnly(libs.kotlin.reflect) {
    because("For Kotlin ABI analysis")
  }

  compileOnly(libs.agp) {
    because("Auto-wiring into Android projects")
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

  functionalTestImplementation(project(":testkit"))
  functionalTestImplementation(libs.commons.io) {
    because("For FileUtils.deleteDirectory()")
  }
}

fun shadowed(): Action<ExternalModuleDependency> = Action {
  attributes {
    attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED))
  }
}

gradlePlugin.testSourceSets(functionalTestSourceSet, smokeTestSourceSet)

val installForFuncTest by tasks.registering {
  dependsOn(
    "publishDependencyAnalysisPluginPluginMarkerMavenPublicationToMavenLocal",
    "publishPluginMavenPublicationToMavenLocal"
  )
}

// Ensure build/functionalTest doesn't grow without bound when tests sometimes fail to clean up
// after themselves.
val deleteOldFuncTests = tasks.register<Delete>("deleteOldFuncTests") {
  delete(layout.buildDirectory.file("functionalTest"))
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
  jvmArgs(
    "-XX:+HeapDumpOnOutOfMemoryError", "-XX:GCTimeLimit=20", "-XX:GCHeapFreeLimit=10",
    "-XX:MaxMetaspaceSize=1g"
  )
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

val functionalTest by tasks.registering(Test::class) {
  dependsOn(deleteOldFuncTests, installForFuncTest)
  mustRunAfter(tasks.named("test"))

  description = "Runs the functional tests."
  group = "verification"

  // forking JVMs is very expensive, and only necessary with full test runs
  setForkEvery(forkEvery())
  maxParallelForks = maxParallelForks()

  testClassesDirs = functionalTestSourceSet.output.classesDirs
  classpath = functionalTestSourceSet.runtimeClasspath

  // Workaround for https://github.com/gradle/gradle/issues/4506#issuecomment-570815277
  systemProperty("org.gradle.testkit.dir", file("${buildDir}/tmp/test-kit"))
  systemProperty("com.autonomousapps.pluginversion", version.toString())
  systemProperty("com.autonomousapps.quick", "${quickTest()}")

  beforeTest(closureOf<TestDescriptor> {
    logger.lifecycle("Running test: $this")
  })
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
check.configure {
  // Run the functional tests as part of `check`
  // Do NOT add smokeTest here. It would be too slow.
  dependsOn(functionalTest)
}

tasks.withType<GroovyCompile>().configureEach {
  options.isIncremental = true
}

val publishToMavenCentral = tasks.named("publishToMavenCentral") {
  // Note that publishing a release requires a successful smokeTest
  if (isRelease) {
    dependsOn(check, smokeTest)
  }
}

//val publishToPluginPortal = tasks.named("publishPlugins") {
//  // Can't publish snapshots to the portal
//  onlyIf { isRelease }
//  shouldRunAfter(publishToMavenCentral)
//
//  // Note that publishing a release requires a successful smokeTest
//  if (isRelease) {
//    dependsOn(check, smokeTest)
//  }
//}

tasks.register("publishEverywhere") {
//  dependsOn(publishToMavenCentral, publishToPluginPortal)

  group = "publishing"
  description = "Publishes to Plugin Portal and Maven Central"
}

dependencyAnalysis {
  dependencies {
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
      onUsedTransitiveDependencies {
        exclude(
          "xml-apis:xml-apis", // org.w3c.dom, also provided transitively via AGP 4.2.2!
        )
      }
      onIncorrectConfiguration {
        exclude(
          "com.google.guava:guava" // exposes Graph. Would rather not change to `api`.
        )
      }
    }
  }
}
