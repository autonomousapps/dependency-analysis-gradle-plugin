package com.autonomousapps

import com.autonomousapps.kit.AbstractGradleProject
import com.autonomousapps.kit.GradleBuilder.build
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.*
import com.autonomousapps.kit.gradle.Dependency.Companion.classpath
import com.autonomousapps.kit.gradle.Dependency.Companion.project
import com.autonomousapps.kit.truth.BuildResultSubject.Companion.buildResults
import com.autonomousapps.kit.truth.BuildTaskListSubject.Companion.buildTaskList
import com.autonomousapps.kit.truth.BuildTaskSubject.Companion.buildTasks
import com.google.common.truth.Truth.assertAbout
import org.gradle.util.GradleVersion
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

// TODO: the fact that this test runs another test inside itself is kind of cute, but it doesn't appear to be working.
//  This line can be changed without apparently affecting the outcome:
//  ```Truth.assertThat(result.getOutput()).contains("Ouroboros!");```
internal class PluginTest {

  private companion object {
    @JvmStatic fun gradleVersions(): List<GradleVersion> {
      return listOf(
        GradleVersion.current(),
        GradleVersion.version("8.4"),
        GradleVersion.version("8.5-rc-2"),
      ).distinctBy { it.version }
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("gradleVersions")
  fun `can apply plugin to a multi-project and composite build`(gradleVersion: GradleVersion) {
    // Given
    val project = PluginProject()
    val gradleProject = project.gradleProject

    // When we run the functional tests
    val result = build(gradleVersion, gradleProject.rootDir, ":plugin:functionalTest")

    // Then the installation tasks ran in order, followed by the test task
    // We do the checks in separate pieces to account for some allowable non-deterministic ordering
    // :build-logic:lib before :plugin
    assertAbout(buildTaskList()).that(result.tasks).containsAtLeastPathsIn(
      ":build-logic:lib:publishAllPublicationsToFunctionalTestRepository",
      ":build-logic:lib:installForFunctionalTest",
      ":plugin:installForFunctionalTest",
      ":plugin:functionalTest",
    ).inOrder()
    // :lib before :plugin
    assertAbout(buildTaskList()).that(result.tasks).containsAtLeastPathsIn(
      ":lib:publishAllPublicationsToFunctionalTestRepository",
      ":lib:installForFunctionalTest",
      ":plugin:installForFunctionalTest",
      ":plugin:functionalTest",
    ).inOrder()
    // all :plugin tasks in order
    assertAbout(buildTaskList()).that(result.tasks).containsAtLeastPathsIn(
      ":plugin:publishAllPublicationsToFunctionalTestRepository",
      ":plugin:installForFunctionalTest",
      ":plugin:functionalTest",
    ).inOrder()

    assertAbout(buildTasks())
      .that(result.task(":build-logic:lib:publishAllPublicationsToFunctionalTestRepository"))
      .succeeded()
    assertAbout(buildTasks())
      .that(result.task(":build-logic:lib:installForFunctionalTest"))
      .succeeded()

    // and the main build installation tasks were successful
    assertAbout(buildTasks())
      .that(result.task(":lib:publishAllPublicationsToFunctionalTestRepository"))
      .succeeded()
    assertAbout(buildTasks())
      .that(result.task(":lib:installForFunctionalTest"))
      .succeeded()
    assertAbout(buildTasks())
      .that(result.task(":plugin:publishAllPublicationsToFunctionalTestRepository"))
      .succeeded()
    assertAbout(buildTasks())
      .that(result.task(":plugin:installForFunctionalTest"))
      .succeeded()

    // and the test task succeeded
    assertAbout(buildTasks())
      .that(result.task(":plugin:functionalTest"))
      .succeeded()

    // And there was no warning about multiple publications overwriting each other
    assertAbout(buildResults())
      .that(result).output()
      // For example:
      // Multiple publications with coordinates 'the-project:plugin:unspecified' are published to repository 'FunctionalTest'. The publications 'pluginMaven' in project ':plugin' and 'testKitSupportForJava' in project ':plugin' will overwrite each other!
      .doesNotContainMatch("Multiple publications with coordinates.+will overwrite each other!")
  }
}

private class PluginProject : AbstractGradleProject() {

  val gradleProject: GradleProject = build()

  private fun build(): GradleProject {
    val buildscriptBlock = BuildscriptBlock(
      repositories = Repositories.DEFAULT_PLUGINS,
      dependencies = Dependencies(classpath("com.autonomousapps:gradle-testkit-plugin:+"))
    )
    val testkitPlugin = Plugin("com.autonomousapps.testkit")

    return newGradleProjectBuilder()
      // included build
      .withIncludedBuild("build-logic") {
        withRootProject {
          withBuildScript {
            buildscript = buildscriptBlock
          }
        }
        withSubproject("lib") {
          withBuildScript {
            plugins(Plugin.javaLibrary, testkitPlugin)
          }
        }
      }
      // main build
      .withRootProject {
        settingsScript.apply {
          additions = """
            includeBuild 'build-logic'
          """.trimIndent()
        }
        withBuildScript {
          buildscript = buildscriptBlock
        }
      }
      .withSubproject("plugin") {
        sources = pluginSources()
        withBuildScript {
          plugins(Plugin.javaGradle, testkitPlugin)
          dependencies(
            project("implementation", ":lib"),
            Dependency("functionalTestImplementation", "com.google.truth:truth:1.1.3"),
            Dependency("functionalTestImplementation", "org.junit.jupiter:junit-jupiter-api:5.8.2"),
            Dependency("functionalTestImplementation", "org.junit.jupiter:junit-jupiter-engine:5.8.2"),
          )
          withGroovy(
            """
              gradleTestKitSupport {
                withIncludedBuildProjects('build-logic:lib')
                withSupportLibrary()
                withTruthLibrary()
              }
              
              gradlePlugin {
                plugins {
                  myPlugin {
                    id = "my-plugin"
                    implementationClass = "com.example.test.MyPlugin"
                  }
                }
              }
            """.trimIndent()
          )
        }
      }
      .withSubproject("lib") {
        withBuildScript {
          plugins(Plugin.javaLibrary, testkitPlugin)
        }
      }
      .write()
  }

  private fun pluginSources() = mutableListOf(
    Source.java(
      """
        package com.example.test;
        
        import org.gradle.api.Plugin;
        import org.gradle.api.Project;
        
        public class MyPlugin implements Plugin<Project> {
          @Override public void apply(Project target) {
            target.getLogger().quiet("Ouroboros!");
          }
        }
      """.trimIndent()
    )
      .withPath(packagePath = "com.example.test", className = "MyPlugin")
      .build(),

    Source.java(
      """
        package com.example.test;
        
        import com.autonomousapps.kit.*;
        import com.autonomousapps.kit.gradle.*;
        import kotlin.Unit;
        
        public class MyFixture extends AbstractGradleProject {
        
          public GradleProject gradleProject = build();
          
          private GradleProject build() {
            return newGradleProjectBuilder(GradleProject.DslKind.GROOVY)
                .withRootProject(r -> {
                  r.withBuildScript(bs -> {
                    bs.plugins(new Plugin("my-plugin", PLUGIN_UNDER_TEST_VERSION));
                    return Unit.INSTANCE;
                  });
                  return Unit.INSTANCE;
                })
                .write();
          }
        }
      """.trimIndent()
    )
      .withSourceSet("functionalTest")
      .withPath("com.example.test", "MyFixture")
      .build(),

    Source.java(
      """
        package com.example.test;
        
        import com.autonomousapps.kit.*;
        import com.google.common.truth.Truth;
        import org.gradle.testkit.runner.BuildResult;
        import org.gradle.testkit.runner.GradleRunner;
        import org.junit.jupiter.api.Test;
        
        public class MyTest {
          @Test public void test() {
            // Given
            GradleProject project = new MyFixture().gradleProject;

            // When
            BuildResult result = build(project.getRootDir(), "help");

            // Then
            Truth.assertThat(result.getOutput()).contains("Ouroboros!");
          }
          
          // TODO: Having issues with importing GradleBuilder into this Java context
          private BuildResult build(java.io.File file, String arg) {
            return GradleRunner.create()
              .forwardOutput()
              .withProjectDir(file)
              .withArguments(arg, "-s")
              .build();
          }
        }
      """.trimIndent()
    )
      .withSourceSet("functionalTest")
      .withPath("com.example.test", "MyTest")
      .build()
  )
}
