@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `plugin-publishing`
  id("org.jetbrains.kotlin.jvm") version "1.4.21"
  `kotlin-dsl`
  groovy
}

repositories {
  google()
  mavenCentral()
}

// This version string comes from gradle.properties
val VERSION: String by project
version = VERSION
group = "com.autonomousapps"

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

val compileFunctionalTestKotlin = tasks.named("compileFunctionalTestKotlin")
tasks.named<AbstractCompile>("compileFunctionalTestGroovy") {
  dependsOn(compileFunctionalTestKotlin)
  classpath += files(compileFunctionalTestKotlin.get().outputs.files)
}

// Add a source set for the smoke test suite. This must come _above_ the `dependencies` block.
val smokeTestSourceSet = sourceSets.create("smokeTest") {
  compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
  runtimeClasspath += output + compileClasspath
}
configurations.getByName("smokeTestImplementation")
  .extendsFrom(functionalTestImplementation)

val asmVersion = "8.0.1.0"

val antlrVersion by extra("4.8")
val internalAntlrVersion by extra("$antlrVersion.2")

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("com.squareup.moshi:moshi:1.9.2") {
    because("For writing reports in JSON format")
  }
  implementation("com.squareup.moshi:moshi-kotlin:1.9.2") {
    because("For writing reports based on Kotlin classes")
  }
  implementation("org.jetbrains.kotlin:kotlin-reflect") {
    because("For Kotlin ABI analysis")
  }
  implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.1.0") {
    because("For Kotlin ABI analysis")
  }
  implementation("com.github.ben-manes.caffeine:caffeine:2.8.5") {
    because("High performance, concurrent cache")
  }
  // This can be removed when we migrate to a version of AGP that uses this itself
  implementation("org.jetbrains.intellij.deps:trove4j:1.0.20181211") {
    because("This version is on mavenCentral")
  }
  implementation(files("libs/asm-$asmVersion.jar"))
  implementation(files("libs/antlr-$internalAntlrVersion.jar"))

  compileOnly("com.android.tools.build:gradle:4.1.2") {
    because("Auto-wiring into Android projects")
    exclude(group = "org.jetbrains.trove4j", module = "trove4j")
  }
  compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin") {
    because("Auto-wiring into Kotlin projects")
  }

  // Cannot use groovy-3.0 because it conflicts with Gradle itself
  // Cannot use JUnit5 because I use JUnit4 Rules
  testImplementation("org.spockframework:spock-core:1.3-groovy-2.5") {//1.3-groovy-2.5//2.0-M3-groovy-2.5//2.0-M3-groovy-3.0
    exclude(module = "groovy-all")
    because("For Spock tests")
  }
  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
  testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0") {
    because("Writing manual stubs for Configuration seems stupid")
  }
  testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.2.8") {
    because("Easy in-memory compilation as a means to get compiled Kotlin class files")
  }
  testImplementation("com.squareup.okio:okio:2.6.0") {
    because("Easy IO APIs")
  }
  testImplementation("com.google.truth:truth:1.0.1") {
    because("Groovy's == behavior on Comparable classes is beyond stupid")
  }

  functionalTestImplementation(project(":testkit"))
  functionalTestImplementation("commons-io:commons-io:2.6") {
    because("For FileUtils.deleteDirectory()")
  }
}

tasks.jar {
  // Bundle shaded jars into final artifact
  from(zipTree("libs/asm-$asmVersion.jar"))
  from(zipTree("libs/antlr-$internalAntlrVersion.jar"))
}

gradlePlugin.testSourceSets(functionalTestSourceSet, smokeTestSourceSet)

val installForFuncTest by tasks.registering {
  dependsOn(
    "publishDependencyAnalysisPluginPluginMarkerMavenPublicationToMavenLocal",
    "publishPluginPublicationToMavenLocal"
  )
}

// Ensure build/functionalTest doesn't grow without bound when tests sometimes fail to clean up
// after themselves.
val deleteOldFuncTests = tasks.register<Delete>("deleteOldFuncTests") {
  delete(project.layout.buildDirectory.file("functionalTest"))
}

tasks.withType<Test>().configureEach {
  jvmArgs(
    "-XX:+HeapDumpOnOutOfMemoryError", "-XX:GCTimeLimit=20", "-XX:GCHeapFreeLimit=10",
    "-XX:MaxMetaspaceSize=1g"
  )
}

// CI cannot handle too much parallelization. Runs out of metaspace.
fun maxParallelForks() =
  if (System.getenv("CI")?.toBoolean() == true) 1
  else Runtime.getRuntime().availableProcessors() / 2

// This will slow down tests on CI, but maybe it won't run out of metaspace.
val forkEvery =
  if (System.getenv("CI")?.toBoolean() == true) 0
  else 0

// Add a task to run the functional tests
// quickTest only runs against the latest gradle version. For iterating faster
fun quickTest(): Boolean = System.getProperty("funcTest.quick") != null

val functionalTest by tasks.registering(Test::class) {
  dependsOn(deleteOldFuncTests, installForFuncTest)
  mustRunAfter(tasks.named("test"))

  description = "Runs the functional tests."
  group = "verification"

  // forking JVMs is very expensive, and only necessary with full test runs
  setForkEvery(forkEvery)
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
  val v = version as String
  return if (!v.endsWith("SNAPSHOT")) {
    "$v-SNAPSHOT"
  } else {
    val regex = """(\d+).(\d+).(\d+)-SNAPSHOT""".toRegex()
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

val publishToMavenCentral = tasks.named("publishToMavenCentral") {
  // Note that publishing non-snapshots requires a successful smokeTest
  if (!(project.version as String).endsWith("SNAPSHOT")) {
    dependsOn(check, smokeTest)
    finalizedBy(tasks.named("promote"))
  }
}

val publishToPluginPortal = tasks.named("publishPlugins") {
  shouldRunAfter(publishToMavenCentral)

  // Note that publishing non-snapshots requires a successful smokeTest
  if (!(project.version as String).endsWith("SNAPSHOT")) {
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
