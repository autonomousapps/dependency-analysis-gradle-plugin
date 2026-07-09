// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class FindRuntimeDepsTaskTest {

  @Test fun `detects ComponentScan with string package`() {
    val source = """
      package com.appiancorp.ae.config;
      import org.springframework.context.annotation.ComponentScan;
      @ComponentScan("com.appiancorp.security.auth.maintenance.controller")
      public class MainConfig {}
    """.trimIndent()

    val classes = mutableSetOf<String>()
    val packages = mutableSetOf<String>()
    FindRuntimeDepsTask.scanSourceFileContent(source, classes, packages)

    assertTrue(packages.contains("com.appiancorp.security.auth.maintenance.controller"))
  }

  @Test fun `detects ComponentScan with basePackageClasses`() {
    val source = """
      package com.appiancorp.ae.config;
      import org.springframework.context.annotation.ComponentScan;
      import com.appiancorp.rules.xray.RulesXraySpringConfig;
      @ComponentScan(basePackageClasses = RulesXraySpringConfig.class)
      public class MainConfig {}
    """.trimIndent()

    val classes = mutableSetOf<String>()
    val packages = mutableSetOf<String>()
    FindRuntimeDepsTask.scanSourceFileContent(source, classes, packages)

    assertTrue(packages.contains("com.appiancorp.rules.xray"))
  }

  @Test fun `detects Bean return type`() {
    val source = """
      package com.appiancorp.ae.config;
      import org.springframework.context.annotation.Bean;
      import com.appiancorp.objectstorage.ObjectStorageClientManager;
      public class StorageConfig {
        @Bean
        public ObjectStorageClientManager objectStorageClientManager() {
          return new ObjectStorageClientManager();
        }
      }
    """.trimIndent()

    val classes = mutableSetOf<String>()
    val packages = mutableSetOf<String>()
    FindRuntimeDepsTask.scanSourceFileContent(source, classes, packages)

    assertTrue(classes.contains("com.appiancorp.objectstorage.ObjectStorageClientManager"))
  }

  @Test fun `detects Import annotation`() {
    val source = """
      package com.appiancorp.ae.config;
      import org.springframework.context.annotation.Import;
      import com.appiancorp.kafka.KafkaSpringConfig;
      @Import({KafkaSpringConfig.class})
      public class MainConfig {}
    """.trimIndent()

    val classes = mutableSetOf<String>()
    val packages = mutableSetOf<String>()
    FindRuntimeDepsTask.scanSourceFileContent(source, classes, packages)

    assertTrue(classes.contains("com.appiancorp.kafka.KafkaSpringConfig"))
  }

  @Test fun `detects Liquibase customChange in XML`() {
    val xml = """
      <databaseChangeLog>
        <changeSet id="1" author="dev">
          <customChange class="com.appiancorp.enduserreporting.persistence.migration.AddPhqUsersToSsaRolemapMigration"/>
        </changeSet>
      </databaseChangeLog>
    """.trimIndent()

    val classes = mutableSetOf<String>()
    FindRuntimeDepsTask.scanResourceFileContent(xml, classes)

    assertTrue(classes.contains("com.appiancorp.enduserreporting.persistence.migration.AddPhqUsersToSsaRolemapMigration"))
  }

  @Test fun `detects Spring XML bean class`() {
    val xml = """
      <beans>
        <bean class="com.appiancorp.portal.PortalService" id="portalService"/>
      </beans>
    """.trimIndent()

    val classes = mutableSetOf<String>()
    FindRuntimeDepsTask.scanResourceFileContent(xml, classes)

    assertTrue(classes.contains("com.appiancorp.portal.PortalService"))
  }

  @Test fun `detects Liquibase class in YAML`() {
    val yaml = """
      databaseChangeLog:
        - changeSet:
            id: 1
            changes:
              - customChange:
                  class: com.appiancorp.migration.FooMigration
    """.trimIndent()

    val classes = mutableSetOf<String>()
    FindRuntimeDepsTask.scanResourceFileContent(yaml, classes)

    assertTrue(classes.contains("com.appiancorp.migration.FooMigration"))
  }

  @Test fun `does not detect non-matching content`() {
    val source = """
      package com.example;
      public class PlainClass {
        public void doStuff() {}
      }
    """.trimIndent()

    val classes = mutableSetOf<String>()
    val packages = mutableSetOf<String>()
    FindRuntimeDepsTask.scanSourceFileContent(source, classes, packages)

    assertTrue(classes.isEmpty())
    assertTrue(packages.isEmpty())
  }
}
