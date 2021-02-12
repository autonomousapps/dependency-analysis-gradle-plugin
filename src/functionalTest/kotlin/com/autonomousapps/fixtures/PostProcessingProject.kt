package com.autonomousapps.fixtures

import java.io.File

class PostProcessingProject : ProjectDirProvider {

  private val rootSpec = RootSpec(buildScript = buildScript())

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
          id 'com.autonomousapps.dependency-analysis' version '${System.getProperty("com.autonomousapps.pluginversion")}'
        }
        
        java {
          sourceCompatibility = JavaVersion.VERSION_1_8
          targetCompatibility = JavaVersion.VERSION_1_8
        }
        
        repositories {
          mavenCentral()
        }
        
        dependencies {
          api 'org.apache.commons:commons-math3:3.6.1'
          implementation 'com.google.guava:guava:28.2-jre'
        }
        
        tasks.register("postProcess", PostTask) {
          input.set(dependencyAnalysis.adviceOutput())          
        }
        
        abstract class PostTask extends DefaultTask {
        
          @InputFile
          final RegularFileProperty input = project.objects.fileProperty()
        
          @TaskAction def action() {
            println(input.get().asFile.text)
          }
        }
    """
    }
  }
}
