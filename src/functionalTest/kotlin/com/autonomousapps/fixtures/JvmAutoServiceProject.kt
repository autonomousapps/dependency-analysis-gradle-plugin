package com.autonomousapps.fixtures

import com.autonomousapps.model.Advice
import java.io.File

class JvmAutoServiceProject : ProjectDirProvider {
  private val rootSpec = RootSpec(
    buildScript = buildScript(),
    sources = setOf(
      Source(
        path = DEFAULT_PACKAGE_PATH,
        name = "MyProcessor.java",
        source = """
        package $DEFAULT_PACKAGE_NAME;
        
        import javax.annotation.processing.Processor;
        
        import com.google.auto.service.AutoService;
        import net.ltgt.gradle.incap.IncrementalAnnotationProcessor;
        import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType;
        
        @IncrementalAnnotationProcessor(IncrementalAnnotationProcessorType.AGGREGATING)
        @AutoService(Processor.class)
        public abstract class MyProcessor implements Processor {
        }
      """.trimIndent()
      )
    )
  )

  private val rootProject = RootProject(rootSpec)

  override val projectDir: File = rootProject.projectDir

  override fun project(moduleName: String): Module {
    if (moduleName == ":") {
      return rootProject
    } else {
      error("No '$moduleName' project found!")
    }
  }

  companion object {
    private fun buildScript(): String {
      return """
        plugins {
          id 'java-library'
          id 'com.autonomousapps.dependency-analysis' version '${System.getProperty("com.autonomousapps.plugin-under-test.version")}'
        }
        
        java {
          sourceCompatibility = JavaVersion.VERSION_1_8
          targetCompatibility = JavaVersion.VERSION_1_8
        }
        
        repositories {
          google()
          mavenCentral()
        }
        
        dependencies {
          compileOnly 'com.google.auto.service:auto-service-annotations:1.0-rc6'
          annotationProcessor 'com.google.auto.service:auto-service:1.0-rc6'
          
          compileOnly 'net.ltgt.gradle.incap:incap:0.2'
          annotationProcessor 'net.ltgt.gradle.incap:incap-processor:0.2'
        }
    """.trimIndent()
    }

    @JvmStatic
    fun expectedAdvice() = emptySet<Advice>()
  }
}
