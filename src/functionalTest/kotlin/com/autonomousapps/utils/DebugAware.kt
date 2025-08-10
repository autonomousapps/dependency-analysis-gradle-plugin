// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.utils

import java.lang.management.ManagementFactory

object DebugAware {
  // Ensure this value is `true` when `--debug-jvm` is passed to Gradle, and false otherwise
  // https://gradle-community.slack.com/archives/CA7UM03V3/p1612641895194900?thread_ts=1612572503.188500&cid=CA7UM03V3
  @JvmStatic
  fun isDebug() = ManagementFactory.getRuntimeMXBean()
    .inputArguments.toString().indexOf("-agentlib:jdwp") > 0
}
