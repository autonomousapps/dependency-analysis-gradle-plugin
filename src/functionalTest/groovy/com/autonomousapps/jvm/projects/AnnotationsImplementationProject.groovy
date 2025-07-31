package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.project
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.junit

final class AnnotationsImplementationProject extends AbstractProject {

  final GradleProject gradleProject
  private final boolean visible

  AnnotationsImplementationProject(boolean visible = true) {
    this.visible = visible
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('consumer') { s ->
        s.sources = [SOURCE_CONSUMER]
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies(
            project('testImplementation', ':testrunner'),
            project('testCompileOnly', ':annotation-with-class-and-enum'),
            project('testCompileOnly', ':annotation'),
            project(visible ? 'testImplementation' : 'testCompileOnly', ':class'),
            project('testCompileOnly', ':member-annotation'),
            project('testCompileOnly', ':enum'),
            junit('testImplementation'),
          )
        }
      }
      .withSubproject('testrunner') { s ->
        s.sources = SOURCE_TEST_RUNNER
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies(
            junit('api'),
          )
        }
      }
      .withSubproject('annotation-with-class-and-enum') { s ->
        s.sources = annotationWithClassAndEnumSource()
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }
      .withSubproject('annotation') { s ->
        s.sources = annotationSource()
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies(
            project('api', ':enum'),
            project('api', ':member-annotation'),
          )
        }
      }
      .withSubproject('class') { s ->
        s.sources = SOURCE_CLASS
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }
      .withSubproject('enum') { s ->
        s.sources = SOURCE_ENUM
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }
      .withSubproject('member-annotation') { s ->
        s.sources = SOURCE_MEMBER_ANNOTATION
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }
      .write()
  }

  private static final Source SOURCE_CONSUMER = Source.java(
    '''\
      package com.example.test;
      
      import org.junit.runner.RunWith;

      @TypeAnno(clazz = AnnoClass.class, enumVal = AnnoEnum.FOO, anno = @MemberAnno)
      @TypeAnno2(clazz = AnnoClass2.class, enumVal = AnnoEnum2.FOO, anno = @MemberAnno2)
      @RunWith(MyTestRunner.class)
      public class TestSuite {}
    '''
  )
    .withPath('com.example.consumer', 'TestSuite')
    .withSourceSet('test')
    .build()

  private static final List<Source> SOURCE_TEST_RUNNER = [
    Source.java(
      '''\
        // Deliberate in the same package as the consumer, to avoid import-based detection heuristics
        package com.example.test;

        import org.junit.runner.Description;
        import org.junit.runner.Runner;
        import org.junit.runner.notification.RunNotifier;

        public class MyTestRunner extends Runner {
            public Description getDescription() {
              throw new IllegalStateException("not implemented");
            }

            public void run(RunNotifier notifier) {}
        }
      '''
    )
      .withPath('com.example.test', 'MyTestRunner')
      .build(),
  ]

  private annotationWithClassAndEnumSource() {
    return [
      Source.java(
        """\
          // Deliberate in the same package as the consumer, to avoid import-based detection heuristics
          package com.example.test;
  
          import java.lang.annotation.*;
          
          @Target(ElementType.TYPE)
          @Retention(${retention()})
          public @interface TypeAnno {
              Class<?> clazz();
              AnnoEnum enumVal();
              MemberAnno anno();
          }
        """
      )
        .withPath('com.example.test', 'TypeAnno')
        .build(),
      Source.java(
        '''\
          // Deliberate in the same package as the consumer, to avoid import-based detection heuristics
          package com.example.test;
  
          public class AnnoClass {
          }
        '''
      )
        .withPath('com.example.test', 'AnnoClass')
        .build(),
      Source.java(
        '''\
          // Deliberate in the same package as the consumer, to avoid import-based detection heuristics
          package com.example.test;
  
          public enum AnnoEnum {
            FOO
          }
        '''
      )
        .withPath('com.example.test', 'AnnoEnum')
        .build(),
      Source.java(
        """\
        // Deliberate in the same package as the consumer, to avoid import-based detection heuristics
        package com.example.test;

        import java.lang.annotation.*;
        
        @Target({})
        public @interface MemberAnno {
        }
      """
      )
        .withPath('com.example.test', 'MemberAnno')
        .build(),
    ]
  }

  private annotationSource() {
    return [
      Source.java(
        """\
          // Deliberate in the same package as the consumer, to avoid import-based detection heuristics
          package com.example.test;
  
          import java.lang.annotation.*;
          
          @Target(ElementType.TYPE)
          @Retention(${retention()})
          public @interface TypeAnno2 {
              Class<?> clazz();
              AnnoEnum2 enumVal();
              MemberAnno2 anno();
          }
        """
      )
        .withPath('com.example.test', 'TypeAnno2')
        .build(),
    ]
  }

  private static final List<Source> SOURCE_CLASS = [
    Source.java(
      '''\
        // Deliberate in the same package as the consumer, to avoid import-based detection heuristics
        package com.example.test;

        public class AnnoClass2 {
        }
      '''
    )
      .withPath('com.example.test', 'AnnoClass2')
      .build(),
  ]

  private static final List<Source> SOURCE_ENUM = [
    Source.java(
      '''\
        // Deliberate in the same package as the consumer, to avoid import-based detection heuristics
        package com.example.test;

        public enum AnnoEnum2 {
          FOO
        }
      '''
    )
      .withPath('com.example.test', 'AnnoEnum2')
      .build(),
  ]

  private static final List<Source> SOURCE_MEMBER_ANNOTATION = [
    Source.java(
      """\
        // Deliberate in the same package as the consumer, to avoid import-based detection heuristics
        package com.example.test;

        import java.lang.annotation.*;
        
        @Target({})
        public @interface MemberAnno2 {
        }
      """
    )
      .withPath('com.example.test', 'MemberAnno2')
      .build(),
  ]

  private retention() {
    if (visible) {
      return 'RetentionPolicy.RUNTIME'
    } else {
      return 'RetentionPolicy.CLASS'
    }
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':consumer'),
    emptyProjectAdviceFor(':testrunner'),
    emptyProjectAdviceFor(':annotation-with-class-and-enum'),
    emptyProjectAdviceFor(':annotation'),
    emptyProjectAdviceFor(':class'),
    emptyProjectAdviceFor(':enum'),
    emptyProjectAdviceFor(':member-annotation'),
  ]
}
