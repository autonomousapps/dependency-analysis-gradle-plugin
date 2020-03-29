plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
  jcenter()
}

dependencies {
  implementation("com.gradle.plugin-publish:com.gradle.plugin-publish.gradle.plugin:0.11.0")
}
