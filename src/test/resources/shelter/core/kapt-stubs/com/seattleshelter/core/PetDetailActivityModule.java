package com.seattleshelter.core;

import java.lang.System;

@kotlin.Metadata(mv = {1, 1, 15}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u00c7\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0007\u00a8\u0006\u0007"}, d2 = {"Lcom/seattleshelter/core/PetDetailActivityModule;", "", "()V", "provideId", "", "activity", "Lcom/seattleshelter/core/PetDetailActivity;", "core_debug"})
@dagger.Module()
public final class PetDetailActivityModule {
    public static final com.seattleshelter.core.PetDetailActivityModule INSTANCE = null;
    
    @com.seattleshelter.core.di.Id()
    @dagger.Provides()
    public static final long provideId(@org.jetbrains.annotations.NotNull()
    com.seattleshelter.core.PetDetailActivity activity) {
        return 0L;
    }
    
    private PetDetailActivityModule() {
        super();
    }
}