package com.autonomousapps.internal.utils

import com.google.common.truth.Truth
import org.junit.jupiter.api.Test
import java.io.File

internal class FilesTest {

  @Test fun `can get relative path`() {
    // Given an absolute path
    val original = File("/Users/me/my-project/foo/bar/baz/build/classes/kotlin/main/com/example/MyClass.class")

    // When we relativize it
    val actual = Files.relativize(original, "build/classes/kotlin/main/")

    // Then we get
    Truth.assertThat(actual).isEqualTo("com/example/MyClass.class")
  }

  @Test fun `can get package path`() {
    // Given an absolute path
    val original = File("/Users/me/my-project/foo/bar/baz/build/classes/kotlin/main/com/example/MyClass.class")

    // When we relativize it
    val actual = Files.asPackagePath(original)

    // Then we get
    Truth.assertThat(actual).isEqualTo("com/example/MyClass.class")
  }
}
