package com.autonomousapps.kit

final class GradleProperties {

  final List<String> lines

  GradleProperties(List<String> lines) {
    this.lines = lines
  }

  static final String JVM_ARGS = """\
    # Try to prevent OOMs (Metaspace) in test daemons spawned by testkit tests
    org.gradle.jvmargs=-Dfile.encoding=UTF-8 -XX:+HeapDumpOnOutOfMemoryError -XX:GCTimeLimit=20 -XX:GCHeapFreeLimit=10 -XX:MaxMetaspaceSize=512m
  """.stripIndent()

  static final String USE_ANDROID_X = """\
    # Necessary for AGP 3.6+
    android.useAndroidX=true
  """.stripIndent()

  static final GradleProperties DEFAULT = new GradleProperties([JVM_ARGS])

  @Override
  String toString() {
    if (lines.isEmpty()) {
      return ''
    }
    return lines.join('\n')
  }
}
