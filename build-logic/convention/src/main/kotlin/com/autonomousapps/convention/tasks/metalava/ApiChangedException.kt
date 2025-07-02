package com.autonomousapps.convention.tasks.metalava

import java.lang.RuntimeException

public class ApiChangedException(msg: String, cause: Throwable) : RuntimeException(msg, cause)
