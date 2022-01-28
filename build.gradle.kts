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
  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("com.squareup.moshi:moshi:1.12.0") {
    because("For writing reports in JSON format")
  }
  implementation("com.squareup.moshi:moshi-kotlin:1.12.0") {
    because("For writing reports based on Kotlin classes")
  }
  implementation("com.squareup.moshi:moshi-adapters:1.12.0") {
    because("For writing reports based on Kotlin classes")
  }
  implementation("dev.zacsweers.moshix:moshi-sealed-runtime:0.14.1") {
    because("Better support for de/serializing sealed types")
  }
  implementation("dev.zacsweers.moshix:moshi-sealed-metadata-reflect:0.14.1") {
    because("Better support for de/serializing sealed types")
  }
  implementation("org.jetbrains.kotlin:kotlin-reflect") {
    because("For Kotlin ABI analysis")
  }
  implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.3.0") {
    because("For Kotlin ABI analysis")
  }
  implementation("com.github.ben-manes.caffeine:caffeine:3.0.4") {
    because("High performance, concurrent cache")
  }
  implementation("com.google.guava:guava:31.0.1-jre") {
    because("Graphs")
  }
  implementation(files("libs/asm-$asmVersion.jar"))
  implementation(files("libs/antlr-$internalAntlrVersion.jar"))

  compileOnly("com.android.tools.build:gradle:4.2.2") {
    because("Auto-wiring into Android projects")
  }
  compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin") {
    because("Auto-wiring into Kotlin projects")
  }

  testImplementation("org.spockframework:spock-core:2.0-groovy-3.0") {
    exclude(group = "org.codehaus.groovy")
    because("For Spock tests")
  }

  // JUnit5 / Jupiter Platform stuff
  // nb: explicit versions aren't required for the jupiter stuff because Spock depends on junit-bom
  testImplementation("org.junit.jupiter:junit-jupiter-api") {
    because("For running tests on the JUnit5 Jupiter platform")
  }
  testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine") {
    because("Baeldung said so")
  }
  testCompileOnly("junit:junit:4.13.2") {
    because("For running legacy JUnit 4 tests")
  }
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine") {
    because("For running legacy JUnit 4 tests")
  }

  testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0") {
    because("Writing manual stubs for Configuration seems stupid")
  }
  testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.5") {
    because("Easy in-memory compilation as a means to get compiled Kotlin class files")
  }
  testImplementation("com.squareup.okio:okio:2.10.0") {
    because("Easy IO APIs")
  }
  testImplementation("com.google.truth:truth:1.1.3")

  functionalTestImplementation(project(":testkit"))
  functionalTestImplementation("commons-io:commons-io:2.11.0") {
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

// 1 or 2
fun implementation(): String = providers.systemProperty("v")
  .forUseAtConfigurationTime()
  .getOrElse("1")

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
  systemProperty("v", implementation())

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
  val version = project.version.toString()

  // Can't publish snapshots to the portal
  onlyIf { !version.endsWith("SNAPSHOT") }
  shouldRunAfter(publishToMavenCentral)

  // Note that publishing non-snapshots requires a successful smokeTest
  if (!version.endsWith("SNAPSHOT")) {
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
