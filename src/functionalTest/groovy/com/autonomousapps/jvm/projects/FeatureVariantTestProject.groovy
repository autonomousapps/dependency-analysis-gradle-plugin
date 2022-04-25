package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.Dependency.commonsCollections
import static com.autonomousapps.kit.Dependency.project

final class FeatureVariantTestProject extends AbstractProject {

  final GradleProject gradleProject

  FeatureVariantTestProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj') { s ->
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
          project('api', ':proj', 'examplegroup:proj-extra-feature')
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

  @SuppressWarnings('GroovyAssignabilityCheck')
  List<ComprehensiveAdvice> actualBuildHealth() {
    actualBuildHealth(gradleProject)
  }

  // Note: The 'proj-extra.jar' is considered part of the 'main variant' of ':proj', which is not correct.
  private final Set<Advice> expectedConsumerAdvice = [
    Advice.ofAdd(transitiveDependency(dependency: ':proj'), 'implementation'),
  ]

  final List<ComprehensiveAdvice> expectedBuildHealth = [
    // Not yet implemented: missing advice to move the dependency to 'extra' to implementation
    compAdviceForDependencies(':consumer', expectedConsumerAdvice),
    // Not yet implemented: missing advice to move the dependency of 'extra' to extraFeatureImplementation
    emptyCompAdviceFor(':proj')
  ]

}
