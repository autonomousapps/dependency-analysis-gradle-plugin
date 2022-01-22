plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
  mavenCentral()
}

// Eliminating this warning:
//  'compileJava' task (current target is 11) and 'compileKotlin' task (current target is 1.8) jvm target compatibility should be set to the same Java version.
java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom"))

  implementation("com.gradle.plugin-publish:com.gradle.plugin-publish.gradle.plugin:0.11.0") {
    because("For extending Gradle Plugin-Publish Plugin functionality")
  }
  implementation("com.squareup.okhttp3:okhttp:4.9.0") {
    because("Closing and releasing Sonatype Nexus staging repo")
  }

  val retrofitVersion = "2.9.0"
  implementation("com.squareup.retrofit2:retrofit:$retrofitVersion") {
    because("Closing and releasing Sonatype Nexus staging repo")
  }
  implementation("com.squareup.retrofit2:converter-moshi:$retrofitVersion") {
    because("Closing and releasing Sonatype Nexus staging repo")
  }

  val moshiVersion = "1.11.0"
  implementation("com.squareup.moshi:moshi:$moshiVersion") {
    because("Closing and releasing Sonatype Nexus staging repo")
  }
  implementation("com.squareup.moshi:moshi-kotlin:$moshiVersion") {
    because("Closing and releasing Sonatype Nexus staging repo")
  }
}
