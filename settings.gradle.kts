plugins {
    id("com.gradle.enterprise") version "3.1.1"
}

gradleEnterprise {
    buildScan {
        publishAlways()
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}

rootProject.name = "dependency-analysis-gradle-plugin"

include(":antlr")