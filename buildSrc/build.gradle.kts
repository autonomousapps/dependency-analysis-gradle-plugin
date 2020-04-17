plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
  jcenter()
}

dependencies {
  implementation("com.gradle.plugin-publish:com.gradle.plugin-publish.gradle.plugin:0.11.0")
  implementation("com.github.johnrengelman.shadow:com.github.johnrengelman.shadow.gradle.plugin:5.2.0")
}
