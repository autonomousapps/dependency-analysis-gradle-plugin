package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.kit.Dependency.project

class AbiUsedClassProject extends AbstractProject {

  final GradleProject gradleProject

  AbiUsedClassProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    // consumer
    builder.withSubproject('proj') { s ->
      s.sources = [SOURCE_CONSUMER]
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.dependencies = [
          project('api', ':lib')
        ]
      }
    }
    // producer
    builder.withSubproject('core') { s ->
      s.sources = [SOURCE_CORE_PRODUCER]
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.dependencies = []
      }
    }
    builder.withSubproject('lib') { s ->
      s.sources = [SOURCE_LIB_PRODUCER]
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.dependencies = [
          project('api', ':core')
        ]
      }
    }


    def project = builder.build()
    project.writer().write()
    return project
  }

  private static final Source SOURCE_CONSUMER = new Source(
    SourceType.JAVA, "WebImpl", "",
    """\
      public class WebImpl {        
        
        private final WebPermission user;
        
        public WebImpl(WebPermission user) {
          this.user = user;
        }
        
        public boolean canChangePassword() {
          return user.isAdmin();
        }
      }
     """.stripIndent()
  )

  private static final Source SOURCE_CORE_PRODUCER = new Source(
    SourceType.JAVA, "IPermission", "",
    """\
      public interface IPermission {
        String name();
        boolean isAdmin();
        default Object getLoggingObject() {
          return name();
        }
      }
     """.stripIndent()
  )

  private static final Source SOURCE_LIB_PRODUCER = new Source(
    SourceType.JAVA, "WebPermission", "",
    """\
      public enum WebPermission implements IPermission {
        ViewData(false),
        EditData(true),
        ;
        
        private final boolean superRole;
        
        WebPermission(boolean superRole) {
          this.superRole = superRole;
        }
        
        @Override
        public boolean isAdmin() {
          return superRole;
        }
      }
     """.stripIndent()
  )
}
