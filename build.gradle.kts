@file:Suppress("UnstableApiUsage", "HasPlatformType", "PropertyName")

import org.jetbrains.kotlin.cli.common.toBooleanLenient
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `plugin-publishing`
  id("org.jetbrains.kotlin.jvm")
  `kotlin-dsl`
  groovy
}

// This version string comes from gradle.properties
val VERSION: String by project
version = VERSION
group = "com.autonomousapps"

val isSnapshot: Boolean = project.version.toString().endsWith("SNAPSHOT")
val isRelease: Boolean = !isSnapshot

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xinline-classes", "-Xopt-in=kotlin.RequiresOptIn", "-Xsam-conversions=class")
    // allWarningsAsErrors = true

    // nb: this is unconfigurable, since Gradle controls it https://docs.gradle.org/7.3/userguide/compatibility.html#kotlin
    //languageVersion = "1.5"
    //apiVersion = "1.5"
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

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

val asmVersion = "9.2.0.1"

val antlrVersion by extra("4.9.2")
val internalAntlrVersion by extra("4.8.2") // TODO re-publish internal antlr jar

dependencies {
  implementation(platform(libs.kotlinBom))

  implementation(libs.kotlinJdk8)
  implementation(libs.moshi.core) {
    because("For writing reports in JSON format")
  }
  implementation(libs.moshi.kotlin) {
    because("For writing reports based on Kotlin classes")
  }
  implementation(libs.moshi.adapters) {
    because("For writing reports based on Kotlin classes")
  }
  implementation(libs.moshix.sealedRuntime) {
    because("Better support for de/serializing sealed types")
  }
  implementation(libs.moshix.sealedMetadataReflect) {
    because("Better support for de/serializing sealed types")
  }
  implementation(libs.kotlinReflect) {
    because("For Kotlin ABI analysis")
  }
  implementation(libs.kotlinx.metadataJvm) {
    because("For Kotlin ABI analysis")
  }
  implementation(libs.caffeine) {
    because("High performance, concurrent cache")
  }
  implementation(libs.guava) {
    because("Graphs")
  }
  implementation(files("libs/asm-$asmVersion.jar"))
  implementation(files("libs/antlr-$internalAntlrVersion.jar"))

  compileOnly(libs.agp) {
    because("Auto-wiring into Android projects")
  }
  compileOnly(libs.kgp) {
    because("Auto-wiring into Kotlin projects")
  }

  testImplementation(libs.spock) {
    exclude(group = "org.codehaus.groovy")
    because("For Spock tests")
  }

  // JUnit5 / Jupiter Platform stuff
  // nb: explicit versions aren't required for the jupiter stuff because Spock depends on junit-bom
  testImplementation(libs.junit.core) {
    because("For running tests on the JUnit5 Jupiter platform")
  }
  testImplementation(libs.junit.params)
  testRuntimeOnly(libs.junit.engine) {
    because("Baeldung said so")
  }
  testCompileOnly("junit:junit:4.13.2") {
    because("For running legacy JUnit 4 tests")
  }
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine") {
    because("For running legacy JUnit 4 tests")
  }

  testImplementation(libs.mockito.kotlin) {
    because("Writing manual stubs for Configuration seems stupid")
  }
  testImplementation(libs.kotlinCompileTesting) {
    because("Easy in-memory compilation as a means to get compiled Kotlin class files")
  }
  testImplementation(libs.okio) {
    because("Easy IO APIs")
  }
  testImplementation(libs.truth)

  functionalTestImplementation(project(":testkit"))
  functionalTestImplementation(libs.commonsIo) {
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
  delete(layout.buildDirectory.file("functionalTest"))
}

val gcLogDirectory = layout.buildDirectory.dir("gc").get()
val createGcLogDirectoryTask = tasks.register("createGcLogDirectory") {
  doLast {
    mkdir(gcLogDirectory)
  }
}

tasks.withType<Test>().configureEach {
  dependsOn(createGcLogDirectoryTask)

  val file = gcLogDirectory.file("${name}.log").asFile.path
  jvmArgs(
    "-XX:+HeapDumpOnOutOfMemoryError", "-XX:GCTimeLimit=20", "-XX:GCHeapFreeLimit=10",
    "-XX:MaxMetaspaceSize=1g",
    "-Xlog:gc+cpu,heap*,metaspace*:${file}::filesize=20M:filecount=0"
  )
}

// CI cannot handle too much parallelization. Runs out of metaspace.
fun maxParallelForks() =
  if (isCi) 1
  else Runtime.getRuntime().availableProcessors() / 2

val isCi = providers.environmentVariable("CI")
  .forUseAtConfigurationTime()
  .getOrElse("false")
  .toBooleanLenient()!!

// This will slow down tests on CI, but maybe it won't run out of metaspace.
fun forkEvery(): Long = if (isCi) 40 else 0

// Add a task to run the functional tests
// quickTest only runs against the latest gradle version. For iterating faster
fun quickTest(): Boolean = providers.systemProperty("funcTest.quick")
  .forUseAtConfigurationTime()
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

val publishToMavenCentral = tasks.named("publishToMavenCentral") {
  // Note that publishing a release requires a successful smokeTest
  if (isRelease) {
    dependsOn(check, smokeTest)
    finalizedBy(tasks.named("promote"))
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
