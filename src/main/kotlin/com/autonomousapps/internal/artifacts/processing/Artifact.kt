// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.artifacts.processing

import com.autonomousapps.model.internal.KtFile

internal interface Artifact {
  fun hasKotlinClasses(): Boolean

  fun ktFiles(): Set<KtFile>

  fun asSequenceOfClassFiles(): Sequence<ClassFile>
}
