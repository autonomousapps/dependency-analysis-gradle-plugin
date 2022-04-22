package com.example.sandbox; 

import dagger.Module;
import dagger.Provides;
        
@Module
public abstract class MyModule {  
  @Provides public static String provideString() {
    return "magic";
  }
}
