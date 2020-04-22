@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `plugin-publishing`
  id("org.jetbrains.kotlin.jvm") version "1.3.72"
  `kotlin-dsl`
  groovy
  //id("com.bnorm.power.kotlin-power-assert") version "0.3.0"
}

repositories {
  jcenter()
  google()
  maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
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

//tasks.withType<KotlinCompile>().matching {
//    // compileTestKotlin, compileFunctionalTestKotlin, ...
//    // useIR leads to compilation failures for non-standard test source sets :'(
//    //it.name.endsWith("TestKotlin")
//    it.name == "compileTestKotlin"
//}.configureEach {
//    kotlinOptions {
//        // For use with the "com.bnorm.power.kotlin-power-assert" plugin, enabling power asserts in tests
//        // https://github.com/bnorm/kotlin-power-assert
//        useIR = true
//    }
//}

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

// Permits testing against different versions of AGP
val agpVersion: String = System.getProperty("funcTest.agpVersion", "3.6.3")

val asmVersion = "8.0.1.0"

val antlrVersion by extra("4.8")
val internalAntlrVersion by extra("$antlrVersion.0")

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
  implementation(files("libs/asm-$asmVersion.jar"))
  implementation(files("libs/antlr-$internalAntlrVersion.jar"))

  compileOnly("com.android.tools.build:gradle:3.6.3") {
    because("Auto-wiring into Android projects")
  }
  compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin") {
    because("Auto-wiring into Kotlin projects")
  }

  testImplementation("org.spockframework:spock-core:1.3-groovy-2.5") {
    exclude(module = "groovy-all")
    because("For Spock tests")
  }
  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
  testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0") {
    because("Writing manual stubs for Configuration seems stupid")
  }

  functionalTestImplementation("org.spockframework:spock-core:1.3-groovy-2.5") {
    exclude(module = "groovy-all")
    because("For Spock tests")
  }
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

tasks.withType<Sign>().configureEach {
  onlyIf {
    !gradle.taskGraph.hasTask(installForFuncTest.get())
  }
}

tasks.withType<Test>().configureEach {
  jvmArgs(
    "-XX:+HeapDumpOnOutOfMemoryError", "-XX:GCTimeLimit=20", "-XX:GCHeapFreeLimit=10",
    "-XX:MaxMetaspaceSize=512m"
  )
}

// Add a task to run the functional tests
// quickTest only runs against the latest gradle version. For iterating faster
val quickTest: Boolean = System.getProperty("funcTest.quick") != null
val functionalTest by tasks.registering(Test::class) {
  dependsOn(deleteOldFuncTests, installForFuncTest)
  mustRunAfter(tasks.named("test"))

  description = "Runs the functional tests."
  group = "verification"

  // forking JVMs is very expensive, and only necessary with full test runs
  //setForkEvery(if (quickTest) 0 else 2)
  maxParallelForks = Runtime.getRuntime().availableProcessors() / 2

  testClassesDirs = functionalTestSourceSet.output.classesDirs
  classpath = functionalTestSourceSet.runtimeClasspath

  // Workaround for https://github.com/gradle/gradle/issues/4506#issuecomment-570815277
  systemProperty("org.gradle.testkit.dir", file("${buildDir}/tmp/test-kit"))
  systemProperty("com.autonomousapps.pluginversion", version.toString())
  systemProperty("com.autonomousapps.agpversion", agpVersion) // TODO not used anymore, but would be nice
  systemProperty("com.autonomousapps.quick", "$quickTest")

  beforeTest(closureOf<TestDescriptor> {
    logger.lifecycle("Running test: $this")
  })
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
