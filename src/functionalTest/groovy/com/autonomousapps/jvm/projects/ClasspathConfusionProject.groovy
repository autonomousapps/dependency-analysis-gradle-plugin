package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.moduleCoordinates
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies
import static com.autonomousapps.kit.gradle.Dependency.project

final class ClasspathConfusionProject extends AbstractProject {

  private static final oldCommonsCollectionsApi = new Dependency(
    'api',
    'org.apache.commons:commons-collections4:4.3'
  )
  private static final commonsCollectionsTestImplementation = new Dependency(
    'testImplementation',
    'org.apache.commons:commons-collections4:4.4'
  )

  final GradleProject gradleProject

  ClasspathConfusionProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('consumer') { s ->
        s.sources = consumerSources
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [
            // Brings along a different version of commons collections on main classpath
            project('implementation', ':producer'),

            // We do in fact use it here, and don't want to remove it
            // The bug is this report of an unused dependency:
            //   testImplementation 'org.apache.commons:commons-collections4:4.3'
            // Not only is it used, we're actually using v4.4. So, two bugs!
            commonsCollectionsTestImplementation,
          ]
        }
      }
      .withSubproject('producer') { s ->
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          // Expose this different version of the dep to consumers
          bs.dependencies = [oldCommonsCollectionsApi]
        }
      }
      .write()
  }

  private List<Source> consumerSources = [
    Source.java(
      '''\
      package com.example;
        
      import org.apache.commons.collections4.Bag;
      import org.apache.commons.collections4.bag.HashBag;
      
      public class Test {
        public void compute() {
          Bag<String> bag = new HashBag<>();
        }
      }
      '''
    )
      .withPath('com.example', 'Test')
      .withSourceSet('test')
      .build()
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice = []
  private final Set<Advice> producerAdvice = [
    Advice.ofRemove(moduleCoordinates(oldCommonsCollectionsApi), 'api')
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', consumerAdvice),
    projectAdviceForDependencies(':producer', producerAdvice),
  ]
}
