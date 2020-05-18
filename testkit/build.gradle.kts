plugins {
  groovy
  kotlin("jvm")
}

repositories {
  jcenter()
}

version = "1.0"

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  implementation(gradleTestKit())
  implementation("org.spockframework:spock-core:1.3-groovy-2.5") {
    exclude(module = "groovy-all")
    because("For Spock tests")
  }
}

tasks.withType<GroovyCompile>().configureEach {
  options.isIncremental = true
}
