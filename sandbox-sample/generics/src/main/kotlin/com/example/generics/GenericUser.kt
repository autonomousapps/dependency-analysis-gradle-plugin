package com.example.generics

import org.apache.commons.math.random.RandomData

abstract class GenericUser {
  abstract fun thing(): List<RandomData>
}
