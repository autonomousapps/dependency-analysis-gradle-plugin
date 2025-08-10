// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project

final class ConstantsProject {

  final static class Java extends AbstractProject {

    final GradleProject gradleProject
    private final libProject = project('api', ':lib')

    Java() {
      this.gradleProject = build()
    }

    private GradleProject build() {
      return newGradleProjectBuilder()
      // consumer
        .withSubproject('proj') { s ->
          s.sources = [SOURCE_CONSUMER]
          s.withBuildScript { bs ->
            bs.plugins = javaLibrary
            bs.dependencies = [libProject]
          }
        }
      // producer
        .withSubproject('lib') { s ->
          s.sources = [SOURCE_PRODUCER]
          s.withBuildScript { bs ->
            bs.plugins = javaLibrary
          }
        }
        .write()
    }

    private static final Source SOURCE_CONSUMER = new Source(
      SourceType.JAVA, 'Main', 'com/example',
      """\
      package com.example;
      
      import com.example.library.Library;
      
      public class Main {
        void useConstant() {
          System.out.println(Library.CONSTANT);
        }
      }""".stripIndent()
    )

    private static final Source SOURCE_PRODUCER = new Source(
      SourceType.JAVA, 'Library', 'com/example/library',
      """\
      package com.example.library;
      
      public class Library {
        public static final String CONSTANT = "magic";
      }
      """.stripIndent()
    )

    Set<ProjectAdvice> actualBuildHealth() {
      return actualProjectAdvice(gradleProject)
    }

    private final Set<Advice> projAdvice = [
      Advice.ofChange(projectCoordinates(libProject), libProject.configuration, 'implementation')
    ]

    final Set<ProjectAdvice> expectedBuildHealth = [
      emptyProjectAdviceFor(':lib'),
      projectAdviceForDependencies(':proj', projAdvice)
    ]
  }

  final static class JavaNested extends AbstractProject {

    final GradleProject gradleProject
    private final libProject = project('implementation', ':producer')

    JavaNested() {
      this.gradleProject = build()
    }

    private GradleProject build() {
      return newGradleProjectBuilder()
        .withSubproject('consumer') { s ->
          s.sources = SOURCES_CONSUMER
          s.withBuildScript { bs ->
            bs.plugins = kotlin
            bs.dependencies(libProject)
          }
        }
        .withSubproject('producer') { s ->
          s.sources = SOURCES_PRODUCER
          s.withBuildScript { bs ->
            bs.plugins = javaLibrary
          }
        }
        .write()
    }

    private static final List<Source> SOURCES_CONSUMER = [
      Source.kotlin(
        '''\
          package com.example
          
          import com.example.library.Library.Inner
      
          public class Main {
            fun useConstant() {
              println(Inner.CONSTANT)
              println(Inner.INT_CONST)
              println(Inner.FLOAT_CONST)
              println(Inner.LONG_CONST)
              println(Inner.DOUBLE_CONST)
            }
          }'''.stripIndent()
      )
        .withPath('com.example', 'Main')
        .build()
    ]

    private static final List<Source> SOURCES_PRODUCER = [
      Source.java(
        '''\
          package com.example.library;
          
          public class Library {
            public static class Inner {
              public static final String CONSTANT = "magic"; 
              public static final int INT_CONST = 9;
              public static final float FLOAT_CONST = 4.2f;
              public static final long LONG_CONST = 11;
              public static final double DOUBLE_CONST = 3.14d;
              // A constant reference to a reference type (including arrays) other than strings triggers different 
              // heuristics. Null values do too (even for strings).
              //public static final String NULL_CONST = null;
              //public static final Object NULL_CONST = null;
              //public static final int[] DOUBLE_ARR_CONST = new int[0];
              //public static final String[] STRING_ARR_CONST = new String[0];
              //public static final Class<?> CLASS_CONST = String.class;
            }
          }'''.stripIndent()
      )
        .withPath('com.example.library', 'Library')
        .build()
    ]

    Set<ProjectAdvice> actualBuildHealth() {
      return actualProjectAdvice(gradleProject)
    }

    final Set<ProjectAdvice> expectedBuildHealth = [
      emptyProjectAdviceFor(':consumer'),
      emptyProjectAdviceFor(':producer'),
    ]
  }

  final static class TopLevel extends AbstractProject {

    final GradleProject gradleProject
    private final libProject = project('api', ':lib')

    TopLevel() {
      this.gradleProject = build()
    }

    private GradleProject build() {
      return newGradleProjectBuilder()
      // consumer
        .withSubproject('proj') { s ->
          s.sources = [SOURCE_CONSUMER]
          s.withBuildScript { bs ->
            bs.plugins = kotlin
            bs.dependencies = [libProject]
          }
        }
      // producer
        .withSubproject('lib') { s ->
          s.sources = [SOURCE_PRODUCER]
          s.withBuildScript { bs ->
            bs.plugins = kotlin
          }
        }
        .write()
    }

    private static final Source SOURCE_CONSUMER = new Source(
      SourceType.KOTLIN, 'Main', 'com/example',
      """\
      package com.example
      
      import com.example.library.CONSTANT
      
      class Main {        
        fun useConstant() {
          println(CONSTANT)
        }
      }""".stripIndent()
    )

    private static final Source SOURCE_PRODUCER = new Source(
      SourceType.KOTLIN, 'Lib', 'com/example/library',
      """\
      package com.example.library
      
      const val CONSTANT = "magic"
      """.stripIndent()
    )

    Set<ProjectAdvice> actualBuildHealth() {
      return actualProjectAdvice(gradleProject)
    }

    private final Set<Advice> projAdvice = [
      Advice.ofChange(projectCoordinates(libProject), libProject.configuration, 'implementation')
    ]

    final Set<ProjectAdvice> expectedBuildHealth = [
      emptyProjectAdviceFor(':lib'),
      projectAdviceForDependencies(':proj', projAdvice)
    ]
  }

  final static class Nested extends AbstractProject {

    final GradleProject gradleProject

    Nested() {
      this.gradleProject = build()
    }

    private GradleProject build() {
      return newGradleProjectBuilder()
        .withSubproject('consumer') { s ->
          s.sources = consumerSources
          s.withBuildScript { bs ->
            bs.plugins = kotlin
            bs.dependencies = [project('implementation', ':producer')]
          }
        }
        .withSubproject('producer') { s ->
          s.sources = producerSources
          s.withBuildScript { bs ->
            bs.plugins = kotlin
          }
        }
        .write()
    }

    private static final List<Source> consumerSources = [new Source(
      SourceType.KOTLIN, 'Main', 'com/example/consumer',
      """\
        package com.example.consumer
        
        import com.example.producer.A.B.C
        
        class Main {        
          fun useConstant() {
            println(C)
          }
        }""".stripIndent()
    )]

    private static final List<Source> producerSources = [new Source(
      SourceType.KOTLIN, 'A', 'com/example/producer',
      """\
        package com.example.producer
        
        object A {
          object B {
            const val C = "magic"
          }
        }""".stripIndent()
    )]

    Set<ProjectAdvice> actualBuildHealth() {
      return actualProjectAdvice(gradleProject)
    }

    final Set<ProjectAdvice> expectedBuildHealth = [
      emptyProjectAdviceFor(':consumer'),
      emptyProjectAdviceFor(':producer')
    ]
  }

  final static class CompanionObject extends AbstractProject {

    final GradleProject gradleProject

    CompanionObject() {
      this.gradleProject = build()
    }

    private GradleProject build() {
      return newGradleProjectBuilder()
        .withSubproject('consumer') { s ->
          s.sources = consumerSources
          s.withBuildScript { bs ->
            bs.plugins = kotlin
            bs.dependencies = [project('implementation', ':producer')]
          }
        }
        .withSubproject('producer') { s ->
          s.sources = producerSources
          s.withBuildScript { bs ->
            bs.plugins = kotlin
          }
        }
        .write()
    }

    private static final List<Source> consumerSources = [new Source(
      SourceType.KOTLIN, 'Main', 'com/example/consumer',
      """\
        package com.example.consumer
        
        import com.example.producer.Producer.Companion.CONSTANT
        
        class Main {        
          fun useConstant() {
            println(CONSTANT)
          }
        }""".stripIndent()
    )]

    private static final List<Source> producerSources = [new Source(
      SourceType.KOTLIN, 'Producer', 'com/example/producer',
      """\
        package com.example.producer
        
        class Producer {
          companion object {
            const val CONSTANT = "magic"
          }
        }""".stripIndent()
    )]

    Set<ProjectAdvice> actualBuildHealth() {
      return actualProjectAdvice(gradleProject)
    }

    final Set<ProjectAdvice> expectedBuildHealth = [
      emptyProjectAdviceFor(':consumer'),
      emptyProjectAdviceFor(':producer')
    ]
  }
}
