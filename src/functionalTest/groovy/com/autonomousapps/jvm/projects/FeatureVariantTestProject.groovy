package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.Dependency.*

final class FeatureVariantTestProject extends AbstractProject {

  final GradleProject gradleProject

  FeatureVariantTestProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('producer') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.sourceSets = ['extraFeature',
                         'java.registerFeature("extraFeature") { usingSourceSet(sourceSets.extraFeature) }']
        bs.dependencies = [
          commonsCollections('api'),
          commonsCollections('extraFeatureApi')
        ]
        bs.additions = 'group = "examplegroup"'
      }
    }
    builder.withSubproject('consumer') { s ->
      s.sources = consumerSources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.dependencies = [
          project('api', ':producer', 'examplegroup:producer-extra-feature')
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private sources = [
    new Source(
      SourceType.JAVA, "Example", "com/example",
      """\
        package com.example;
        
        import org.apache.commons.collections4.bag.HashBag;
        
        public class Example {
          public HashBag<String> bag;
        }
      """.stripIndent()
    ),
    new Source(
      SourceType.JAVA, "ExtraFeature", "com/example/extra",
      """\
        package com.example.extra;
        
        import org.apache.commons.collections4.bag.HashBag;
        
        public class ExtraFeature {
          private HashBag<String> internalBag;
        }
      """.stripIndent(),
      "extraFeature"
    )
  ]

  private consumerSources = [
    new Source(
      SourceType.JAVA, "Consumer", "com/example/consumer",
      """\
        package com.example.consumer;
        
        import com.example.extra.ExtraFeature;
        
        public class Consumer {
          private ExtraFeature extra;
        }
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> expectedProducerAdvice = [
    Advice.ofChange(moduleCoordinates(commonsCollections('')), 'extraFeatureApi', 'extraFeatureImplementation'),
  ]

  // Note: 'producer-extra-feature.jar' is considered part of the 'main variant' of ':producer', which is not correct.
  // This is due to the following:
  // - PhysicalArtifact.coordinates only knows about component IDs, 'Module GA' or 'Project Name', but not capabilities.
  //   This should probably be improved by adding the Capabilities GA coordinates to the 'Coordinates' data classes.
  // - Right now, the plugin thinks that the 'producer-extra-feature.jar' artifact belongs to
  //   'ProjectCoordinates(identifier=:producer)'. It finds classes in that Jar that are used.
  //   But there is no dependency (without requires capabilities) to the producer project.
  //   It gives the advice to add it.
  private final Set<Advice> expectedConsumerAdvice = [
    Advice.ofAdd(projectCoordinates(':producer'), 'implementation'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    // Not yet implemented:
    // missing advice to move dependency 'consumer' -> 'producer-extra-feature' to implementation
    projectAdviceForDependencies(':consumer', expectedConsumerAdvice),
    projectAdviceForDependencies(':producer', expectedProducerAdvice)
  ]

}
