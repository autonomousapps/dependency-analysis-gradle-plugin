package com.example.lib;

import java.util.List;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.math.random.RandomData;

abstract public class JavaLib {
  abstract public List<RandomData> getRandomData();

  public Bag<String> newBag() {
    return new HashBag<String>();
  }
}
