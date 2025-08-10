// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.DoubleExclusionsProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class DoubleExclusionsSpec extends AbstractJvmSpec {

    def "project can declare exclusion twice (#gradleVersion)"() {
        given:
        def project = new DoubleExclusionsProject()
        gradleProject = project.gradleProject

        when:
        build(gradleVersion, gradleProject.rootDir, "buildHealth")

        then:
        assertThat(project.actualProjectAdvice().isEmpty())

        where:
        gradleVersion << gradleVersions()
    }
}
