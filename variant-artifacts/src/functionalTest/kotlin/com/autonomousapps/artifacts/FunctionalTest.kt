package com.autonomousapps.artifacts

import com.autonomousapps.kit.AbstractGradleProject
import com.autonomousapps.kit.GradleBuilder.build
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.*
import com.autonomousapps.kit.truth.BuildTaskSubject.Companion.buildTasks
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import kotlin.io.path.readText

internal class FunctionalTest {

  @Test fun test() {
    // Given
    val project = VariantArtifactsProject().build()

    // When
    val result = build(project.rootDir, ":consumer:aggregate")

    // Then producer tasks ran
    assertAbout(buildTasks())
      .that(result.task(":producer:publishPath"))
      .succeeded()

    // ...and producer tasks have expected output
    val producer = project.singleArtifact(":producer", "path.txt")
    assertThat(producer.exists()).isTrue()
    assertThat(producer.asPath.readText()).isEqualTo(":producer")

    // ...and aggregation task ran and has expected output
    val aggregate = project.singleArtifact(":consumer", "paths.txt")
    assertThat(aggregate.exists()).isTrue()
    assertThat(aggregate.asPath.readText()).isEqualTo(":producer")
  }

  private class VariantArtifactsProject : AbstractGradleProject() {

    private val variantArtifacts = Dependency(
      "classpath",
      "com.autonomousapps:variant-artifacts:$PLUGIN_UNDER_TEST_VERSION",
    )

    fun build(): GradleProject {
      return newGradleProjectBuilder(GradleProject.DslKind.KOTLIN)
        .withRootProject {
          gradleProperties += GradleProperties.of(GradleProperties.CONFIGURATION_CACHE_STABLE)
        }
        .withSubproject("consumer") {
          withBuildScript {
            imports = Imports.of(
              "com.autonomousapps.artifacts.Publisher.Companion.interProjectPublisher",
              "com.autonomousapps.artifacts.Resolver.Companion.interProjectResolver",
              "com.autonomousapps.artifacts.ArtifactDescription",
            )

            buildscript = BuildscriptBlock(
              repositories = Repositories.DEFAULT_PLUGINS,
              dependencies = Dependencies(variantArtifacts),
            )

            plugins(Plugin.java)

            withKotlin(
              """
                pluginManager.apply(FakeResolverPlugin::class.java)

                dependencies {
                  "projectPath"(project(":producer"))
                }

                class FakeResolverPlugin : Plugin<Project> {
                  override fun apply(target: Project): Unit = target.run {
                    val publisher = interProjectPublisher(
                      project = project,
                      artifactDescription = FakeArtifacts.Kind.PROJECT_PATH,
                    )
                    val resolver = interProjectResolver(
                      project = project,
                      artifactDescription = FakeArtifacts.Kind.PROJECT_PATH,
                    )

                    tasks.register<AggregationTask>("aggregate") {
                      inputFiles.setFrom(resolver.artifactFilesProvider())
                      output.set(layout.buildDirectory.file("paths.txt"))
                    }
                  }
                }
                
                abstract class AggregationTask : DefaultTask() {
                
                  @get:PathSensitive(PathSensitivity.RELATIVE)
                  @get:InputFiles
                  abstract val inputFiles: ConfigurableFileCollection
                
                  @get:OutputFile
                  abstract val output: RegularFileProperty
                
                  @TaskAction fun action() {
                    val output = output.get().asFile
                    output.delete()
                  
                    val contents = inputFiles
                      .files
                      .onEach { f ->
                        logger.quiet("File: ${'$'}{f.absolutePath}")
                      }
                      .joinToString(separator = "\n") { f -> f.readText() }
    
                    output.writeText(contents)
                  }
                }

                interface FakeArtifacts : Named {
                  companion object {
                    @JvmField
                    val FAKE_ARTIFACTS_ATTRIBUTE: Attribute<FakeArtifacts> =
                      Attribute.of("fake.internal.artifacts", FakeArtifacts::class.java)
                  }
                
                  enum class Kind : ArtifactDescription<FakeArtifacts> {
                    PROJECT_PATH,
                    ;
                
                    override val attribute: Attribute<FakeArtifacts> = FAKE_ARTIFACTS_ATTRIBUTE
                
                    override val categoryName: String = "com.autonomousapps.variant-artifacts.project-path"
                  }
                }
              """.trimIndent()
            )
          }
        }
        .withSubproject("producer") {
          withBuildScript {
            imports = Imports.of(
              "com.autonomousapps.artifacts.Publisher.Companion.interProjectPublisher",
              "com.autonomousapps.artifacts.Resolver.Companion.interProjectResolver",
              "com.autonomousapps.artifacts.ArtifactDescription",
            )

            buildscript = BuildscriptBlock(
              repositories = Repositories.DEFAULT_PLUGINS,
              dependencies = Dependencies(variantArtifacts),
            )

            plugins(Plugin.java)

            withKotlin(
              """
                pluginManager.apply(FakePublisherPlugin::class.java)

                class FakePublisherPlugin : Plugin<Project> {
                  override fun apply(target: Project): Unit = target.run {
                    val publisher = interProjectPublisher(
                      project = project,
                      artifactDescription = FakeArtifacts.Kind.PROJECT_PATH,
                    )
                
                    val publishPath = tasks.register<PublishTask>("publishPath") {
                      projectPath.set(target.path)
                      output.set(layout.buildDirectory.file("path.txt"))
                    }
                
                    publisher.publish(publishPath.flatMap { it.output })
                  }
                }
                
                abstract class PublishTask : DefaultTask() {
                
                  @get:Input
                  abstract val projectPath: Property<String>
                
                  @get:OutputFile
                  abstract val output: RegularFileProperty
                
                  @TaskAction fun action() {
                    val output = output.get().asFile
                    output.delete()
                
                    output.writeText(projectPath.get())
                  }
                }

                interface FakeArtifacts : Named {
                  companion object {
                    @JvmField
                    val FAKE_ARTIFACTS_ATTRIBUTE: Attribute<FakeArtifacts> =
                      Attribute.of("fake.internal.artifacts", FakeArtifacts::class.java)
                  }
                
                  enum class Kind : ArtifactDescription<FakeArtifacts> {
                    PROJECT_PATH,
                    ;
                
                    override val attribute: Attribute<FakeArtifacts> = FAKE_ARTIFACTS_ATTRIBUTE
                
                    override val categoryName: String = "com.autonomousapps.variant-artifacts.project-path"
                  }
                }
              """.trimIndent()
            )
          }
        }
        .write()
    }
  }
}
