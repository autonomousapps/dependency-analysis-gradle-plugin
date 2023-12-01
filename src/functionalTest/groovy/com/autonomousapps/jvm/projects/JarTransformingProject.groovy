package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.commonsCollections

final class JarTransformingProject extends AbstractProject {

  final GradleProject gradleProject

  JarTransformingProject() {
    this.gradleProject = build()
  }

  private def commonsCollections = commonsCollections("api")

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj') { s ->
      s.sources = PROJ_SOURCES
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibrary]
        bs.sourceSets = ['integrationTest']
        bs.dependencies = [
          commonsCollections,
        ]
        bs.withGroovy('''
          // We do some post processing of Jars on the classpath using a transform (split one Jar into multiple).
          // A similar situation can also be created by publishing a component that has multiple Jars
          // using Gradle Metadata to express that. 
          def artifactType = Attribute.of("artifactType", String)
          def split = Attribute.of("split", Boolean)
          dependencies.attributesSchema { attribute(split) }
          dependencies.artifactTypes.jar.attributes.attribute(split, false)
          dependencies.registerTransform(SplitJarTransform) {
            from.attribute(split, false).attribute(artifactType, "jar")
            to.attribute(split, true).attribute(artifactType, "jar")
          }
          configurations.compileClasspath.attributes.attribute(split, false)
          configurations.runtimeClasspath.attributes.attribute(split, false)
          
          afterEvaluate {
            tasks.named("artifactsReportMain") {
              // also use our custom view as input for the artifacts report task
              classpath = configurations.compileClasspath.incoming.artifactView {
                attributes.attribute(artifactType, "jar")
                attributes.attribute(split, true)
                lenient(true)
              }.artifacts
            }
          }
          
          import org.gradle.api.artifacts.transform.*
          
          abstract class SplitJarTransform implements TransformAction<TransformParameters.None> {
            @InputArtifact
            @PathSensitive(PathSensitivity.NAME_ONLY)
            abstract Provider<FileSystemLocation> getInputJar()
            
            @Override
            void transform(TransformOutputs outputs) {
              def jarFile = inputJar.get().asFile
              def splitOutJar = outputs.file("split-out-${jarFile.name}") // add the empty jar file first
              
              def os = new java.util.jar.JarOutputStream(new FileOutputStream(splitOutJar)) // empty for test case
              os.close()
              
              outputs.file(jarFile) // add the jar file with the classes last
            }
          }
        ''')
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private List<Source> PROJ_SOURCES = [
    new Source(
      SourceType.JAVA, "Example", "com/example",
      """\
        package com.example;
        
        import org.apache.commons.collections4.map.HashedMap;
        
        public class Example {
          private HashedMap<String, String> map;
        }""".stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> projAdvice = [
    Advice.ofChange(moduleCoordinates(commonsCollections), commonsCollections.configuration, 'implementation')
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':proj', projAdvice)
  ]
}
