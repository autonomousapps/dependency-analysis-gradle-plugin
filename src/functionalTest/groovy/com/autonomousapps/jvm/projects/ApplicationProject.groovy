package com.autonomousapps.jvm.projects

import com.autonomousapps.advice.Advice
import com.autonomousapps.fixtures.jvm.Dependency
import com.autonomousapps.fixtures.jvm.JvmProject
import com.autonomousapps.fixtures.jvm.Plugin
import com.autonomousapps.fixtures.jvm.Source
import com.autonomousapps.fixtures.jvm.SourceType

import static com.autonomousapps.fixtures.jvm.Dependency.*

/**
 * This project has the `application` plugin applied. There should be no api dependencies, only
 * implementation.
 */
final class ApplicationProject {

  private final List<Plugin> plugins
  private final SourceType sourceType
  final JvmProject jvmProject

  ApplicationProject(
    List<Plugin> plugins = [Plugin.applicationPlugin()], SourceType sourceType = SourceType.JAVA
  ) {
    this.plugins = plugins
    this.sourceType = sourceType
    this.jvmProject = build()
  }

  private JvmProject build() {
    def builder = new JvmProject.Builder()

    def dependencies = [
      commonsMath("compile"),
      commonsIO("compile"),
      commonsCollections("compile")
    ]
    if (sourceType == SourceType.KOTLIN) {
      dependencies.add(kotlinStdlib("implementation"))
    }

    def source
    if (sourceType == SourceType.JAVA) {
      source = JAVA_SOURCE
    } else {
      source = KOTLIN_SOURCE
    }

    builder.addSubproject(plugins, dependencies, [source], 'main', '')

    def project = builder.build()
    project.writer().write()
    return project
  }

  private static final Source JAVA_SOURCE = new Source(
    SourceType.JAVA, "Main", "com/example",
    """\
      package com.example;
      
      import java.io.FileFilter;
      import org.apache.commons.io.filefilter.EmptyFileFilter; 
      import org.apache.commons.collections4.bag.HashBag;
      
      public class Main {
        public FileFilter f = EmptyFileFilter.EMPTY;
        
        public HashBag<String> getBag() {
          return new HashBag<String>();
        }
      }
     """.stripIndent()
  )

  private static final Source KOTLIN_SOURCE = new Source(
    SourceType.KOTLIN, "Main", "com/example",
    """\
      package com.example
      
      import java.io.FileFilter
      import org.apache.commons.io.filefilter.EmptyFileFilter
      import org.apache.commons.collections4.bag.HashBag
      
      class Main {
        val f: FileFilter = EmptyFileFilter.EMPTY
        
        fun getBag(): HashBag<String> {
          return HashBag<String>()
        }
      }
     """.stripIndent()
  )

  static final List<Advice> expectedAdvice = ApplicationAdvice.expectedAdvice
}
