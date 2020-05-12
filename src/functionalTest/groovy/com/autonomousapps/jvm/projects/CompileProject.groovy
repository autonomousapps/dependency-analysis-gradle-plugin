package com.autonomousapps.jvm.projects

import com.autonomousapps.advice.Advice
import com.autonomousapps.fixtures.jvm.JvmProject
import com.autonomousapps.fixtures.jvm.Plugin
import com.autonomousapps.fixtures.jvm.Source
import com.autonomousapps.fixtures.jvm.SourceType

import static com.autonomousapps.fixtures.jvm.Dependency.*

/**
 * Includes three dependencies on the compile scope:
 * 1. commons-io
 * 2. commons-math
 * 3. commons-collections
 *
 * Commons-io is used and should be moved to implementation, commons-math should be removed
 * as unused, and commons-collections should be moved to api.
 */
final class CompileProject {
  final JvmProject jvmProject

  CompileProject() {
    this.jvmProject = build()
  }

  private static JvmProject build() {
    def builder = new JvmProject.Builder()

    def plugins = [Plugin.javaLibraryPlugin()]
    def dependencies = [
      commonsMath("compile"),
      commonsIO("compile"),
      commonsCollections("compile")
    ]
    def source = new Source(
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

    builder.addSubproject(plugins, dependencies, [source], 'main', '')

    def project = builder.build()
    project.writer().write()
    return project
  }

  static final List<Advice> expectedAdvice = CompileAdvice.expectedAdvice
}
