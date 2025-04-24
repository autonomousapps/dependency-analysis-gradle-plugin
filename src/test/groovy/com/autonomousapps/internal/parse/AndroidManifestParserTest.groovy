// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.parse

import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

final class AndroidManifestParserTest extends Specification {

  @TempDir
  Path dir

  def "can parse manifests that don't have a package declaration"() {
    given:
    def mainManifest = '''\
      <?xml version="1.0" encoding="utf-8"?>
      <manifest
        xmlns:android="http://schemas.android.com/apk/res/android"
        package="mutual.aid"
        />'''.stripIndent()
    def debugManifest = '''\
      <?xml version="1.0" encoding="utf-8"?>
      <manifest
        xmlns:android="http://schemas.android.com/apk/res/android" 
        />'''.stripIndent()
    def manifests = [
      newManifest('project/src/main/AndroidManifest.xml', mainManifest),
      newManifest('project/src/debug/AndroidManifest.xml', debugManifest)
    ]
    def projectDir = dir.resolve('project').toFile()
    def namespace = ''
    def parser = new AndroidManifestParser(projectDir, manifests, namespace)

    when:
    parser.explodedManifests

    then:
    noExceptionThrown()
  }

  private File newManifest(String path, String contents) {
    return dir.resolve(path).tap {
      Files.createDirectories(parent)
      write(contents)
    }.toFile()
  }
}
