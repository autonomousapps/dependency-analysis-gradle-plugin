package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.project

final class SamInterfacesProject extends AbstractProject {

  final GradleProject gradleProject

  SamInterfacesProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('kotlin-consumer') { c ->
        c.sources = kotlinConsumerSources
        c.withBuildScript { bs ->
          bs.plugins = kotlin
          bs.dependencies = [
            project('implementation', ':sam-interface-consumer'),
            project('implementation', ':sam-interface-producer'),
          ]
        }
      }
      .withSubproject('sam-interface-consumer') { c ->
        c.sources = samInterfaceConsumerSources
        c.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }
      .withSubproject('sam-interface-producer') { c ->
        c.sources = samInterfaceProducerSources
        c.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }
      .write()
  }

  private kotlinConsumerSources = [
    Source.kotlin(
      """\
      package com.example.consumer
            
      import com.example.consumer.java.SamInterfaceConsumer
            
      class Consumer {
        fun usesJavaProducer() {
          val consumer = SamInterfaceConsumer()
          consumer.setWhatever(com.example.producer.java.SamInterface { "magic!" })
        }
      }
      """
    )
      .withPath('com.example.consumer', 'Consumer')
      .build(),
  ]

  private samInterfaceConsumerSources = [
    Source.java(
      """\
      package com.example.consumer.java;
            
      public class SamInterfaceConsumer {
        public void setWhatever(Object whatever) {}
      }
      """
    )
      .withPath('com.example.consumer.java', 'SamInterfaceConsumer')
      .build(),
  ]

  private samInterfaceProducerSources = [
    Source.java(
      """\
      package com.example.producer.java;
      
      public interface SamInterface {
        String produce();
      }
      """
    )
      .withPath('com.example.producer.java', 'SamInterface')
      .build()
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }


  final Set<ProjectAdvice> expectedProjectAdvice = [
    emptyProjectAdviceFor(':kotlin-consumer'),
    emptyProjectAdviceFor(':sam-interface-consumer'),
    emptyProjectAdviceFor(':sam-interface-producer'),
  ]
}
