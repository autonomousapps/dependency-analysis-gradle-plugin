package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source

import static com.autonomousapps.kit.gradle.Dependency.project

final class BinaryIncompatibilityProject extends AbstractProject {

  final GradleProject gradleProject

  BinaryIncompatibilityProject(boolean extra = false) {
    this.gradleProject = build(extra)
  }

  private GradleProject build(boolean extra) {
    return newGradleProjectBuilder()
    // :consumer uses the Producer class.
    // This class is provided by both
    // 1. :producer-2, which is a direct dependency, and
    // 2. :producer-1, which is a transitive dependency (of :unused)
    // These classes have incompatible definitions. :consumer _requires_ the version provided by :producer-2.
      .withSubproject('consumer') { s ->
        s.sources = sourcesConsumer
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary + plugins.gradleDependenciesSorter
          bs.dependencies(
            project('implementation', ':unused'),
            project('implementation', ':producer-2'),
          )

          // If this is a direct dep, then we should suggest removing it due to binary-incompatibility
          if (extra) {
            bs.dependencies += project('implementation', ':producer-1')
          }
        }
      }
    // Used
      .withSubproject('producer-1') { s ->
        s.sources = sourcesProducer1
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }
    // Unused, except its transitive is
      .withSubproject('unused') { s ->
        s.sources = sourcesUnused
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies(
            project('api', ':producer-1'),
          )
        }
      }
    // Used?
      .withSubproject('producer-2') { s ->
        s.sources = sourcesProducer2
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }
      .write()
  }

  private static final List<Source> sourcesConsumer = [
    Source.java(
      '''\
        package com.example.consumer;

        import com.example.producer.Person;

        public class Consumer {
          private Person person = new Person("Emma", "Goldman");
          private String usesField = person.firstName;
          private String usesMethod = person.getLastName();
          
          private void usePerson() {
            String m = Person.MAGIC;
            System.out.println(person.firstName);
            System.out.println(person.lastName);
            System.out.println(person.getFirstName());
            System.out.println(person.getLastName());
            
            // Person::ugh takes and returns Object, here we pass Void and print a String.
            System.out.println(person.ugh(null));
          }
        }
      '''
    )
      .withPath('com.example.consumer', 'Consumer')
      .build(),
  ]

  private static final List<Source> sourcesProducer1 = [
    Source.java(
      '''\
        package com.example.producer;

        public class Person {
          
          private final String name;
          
          public Person(String name) {
            this.name = name;
          }
        }
      '''
    )
      .withPath('com.example.producer', 'Person')
      .build(),
  ]

  // Same class file as sourcesProducer1, incompatible definition
  private static final List<Source> sourcesProducer2 = [
    Source.java(
      '''\
        package com.example.producer;

        import java.util.ArrayList;
        import java.util.List;

        /**
         * This class contains fields with different visibilities so we can exercise our bytecode analysis thoroughly. 
         */
        public class Person {
          
          public static final String MAGIC = "42";
          
          public final String firstName;
          public final String lastName;
          
          protected int nameLength;
          
          String[] names = new String[2];
          
          private final boolean unused = false;
          
          public Person(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.nameLength = firstName.length() + lastName.length();
            
            this.names[0] = firstName;
            this.names[1] = lastName;
          }
          
          public String getFirstName() {
            return firstName;
          }
          
          public String getLastName() {
            return lastName;
          }
          
          protected int getNameLength() {
            return nameLength;
          }
          
          List<String> getNames() {
            List<String> list = new ArrayList<>();
            list.add(firstName);
            list.add(lastName);
            return list;
          }
          
          public Object ugh(Object anything) {
            return firstName;
          }
          
          private void notImplemented() {
            throw new RuntimeException("ohnoes");
          }
        }
      '''
    )
      .withPath('com.example.producer', 'Person')
      .build(),
  ]

  private static final List<Source> sourcesUnused = [
    Source.java(
      '''\
        package com.example.unused;

        import com.example.producer.Person;

        public class Unused {
          public Person producer;
        }
      '''
    )
      .withPath('com.example.unused', 'Unused')
      .build(),
  ]
}
