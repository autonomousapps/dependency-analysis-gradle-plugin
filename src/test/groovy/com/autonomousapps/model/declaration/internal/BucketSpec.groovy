// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.declaration.internal

import com.autonomousapps.ProjectType
import com.autonomousapps.model.internal.declaration.Bucket
import com.autonomousapps.model.internal.declaration.ConfigurationNames
import spock.lang.Specification

class BucketSpec extends Specification {

  private Set<String> supportedSourceSetNames = ["main", "test", "debug", "release", "releaseFlavor", "debugFlavor"]
  private ConfigurationNames configurationNames = new ConfigurationNames(ProjectType.JVM, supportedSourceSetNames)

  def "can compute bucket from configuration (#configurationName => #bucket)"() {
    expect:
    Bucket.of(configurationName, configurationNames) == bucket

    where:
    configurationName                  | bucket
    'implementation'                   | Bucket.IMPL
    'debugImplementation'              | Bucket.IMPL
    'releaseFlavorImplementation'      | Bucket.IMPL

    'api'                              | Bucket.API
    'releaseApi'                       | Bucket.API
    'debugFlavorApi'                   | Bucket.API

    'compileOnly'                      | Bucket.COMPILE_ONLY
    'debugCompileOnly'                 | Bucket.COMPILE_ONLY
    'releaseFlavorCompileOnly'         | Bucket.COMPILE_ONLY

    'runtimeOnly'                      | Bucket.RUNTIME_ONLY
    'releaseRuntimeOnly'               | Bucket.RUNTIME_ONLY
    'flavorDebugRuntimeOnly'           | Bucket.RUNTIME_ONLY

    'annotationProcessor'              | Bucket.ANNOTATION_PROCESSOR
    'debugAnnotationProcessor'         | Bucket.ANNOTATION_PROCESSOR
    'releaseFlavorAnnotationProcessor' | Bucket.ANNOTATION_PROCESSOR

    'kapt'                             | Bucket.ANNOTATION_PROCESSOR
    'kaptRelease'                      | Bucket.ANNOTATION_PROCESSOR
    'kaptDebugFlavor'                  | Bucket.ANNOTATION_PROCESSOR

    'testImplementation'               | Bucket.IMPL
    'testApi'                          | Bucket.API
    'testCompileOnly'                  | Bucket.COMPILE_ONLY
    'testRuntimeOnly'                  | Bucket.RUNTIME_ONLY
  }

  def "throws when no matching bucket found (#configurationName)"() {
    when:
    Bucket.of(configurationName, configurationNames)

    then:
    thrown(IllegalArgumentException)

    where:
    configurationName << [
      'debugKapt', 'annotationProcessorRelease',
      'implementationDebug', 'apiRelease'
    ]
  }
}
