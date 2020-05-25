plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
  jcenter()
}

dependencies {
  implementation("com.gradle.plugin-publish:com.gradle.plugin-publish.gradle.plugin:0.11.0") {
    because("For extending Gradle Plugin-Publish Plugin functionality")
  }
  implementation("com.squareup.okhttp3:okhttp:4.7.2") {
    because("Closing and releasing Sonatype Nexus staging repo")
  }

  val retrofitVersion = "2.9.0"
  implementation("com.squareup.retrofit2:retrofit:$retrofitVersion") {
    because("Closing and releasing Sonatype Nexus staging repo")
  }
  implementation("com.squareup.retrofit2:converter-moshi:$retrofitVersion") {
    because("Closing and releasing Sonatype Nexus staging repo")
  }

  val moshiVersion = "1.9.2"
  implementation("com.squareup.moshi:moshi:$moshiVersion") {
    because("Closing and releasing Sonatype Nexus staging repo")
  }
  implementation("com.squareup.moshi:moshi-kotlin:$moshiVersion") {
    because("Closing and releasing Sonatype Nexus staging repo")
  }
}
