// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.fixtures;

public class LambdaUsage {

  public void run() {
    var classUsedInLambda = new ClassUsedInLambda();
    Runnable r = classUsedInLambda::someMethod;
    r.run();
  }
}
