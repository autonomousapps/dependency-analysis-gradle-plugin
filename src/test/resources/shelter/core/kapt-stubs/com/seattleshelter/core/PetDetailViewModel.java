package com.seattleshelter.core;

import java.lang.System;

@kotlin.Metadata(mv = {1, 1, 15}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\b\n\u0000\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\t0\rJ\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000b0\rJ\u000e\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0012R\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u000b0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lcom/seattleshelter/core/PetDetailViewModel;", "Lcom/seattleshelter/core/base/DisposingViewModel;", "petsService", "Lcom/seattleshelter/core/api/PetsService;", "id", "", "(Lcom/seattleshelter/core/api/PetsService;J)V", "_avatarUrl", "Landroidx/lifecycle/MutableLiveData;", "", "_petDetails", "Lcom/seattleshelter/entities/PetDetails;", "avatarUrl", "Landroidx/lifecycle/LiveData;", "catDetails", "onPictureSelected", "", "picNum", "", "core_debug"})
public final class PetDetailViewModel extends com.seattleshelter.core.base.DisposingViewModel {
    private final androidx.lifecycle.MutableLiveData<java.lang.String> _avatarUrl = null;
    private final androidx.lifecycle.MutableLiveData<com.seattleshelter.entities.PetDetails> _petDetails = null;
    private final com.seattleshelter.core.api.PetsService petsService = null;
    private final long id = 0L;
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.lifecycle.LiveData<com.seattleshelter.entities.PetDetails> catDetails() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.lifecycle.LiveData<java.lang.String> avatarUrl() {
        return null;
    }
    
    /**
     * 0-based.
     */
    public final void onPictureSelected(int picNum) {
    }
    
    public PetDetailViewModel(@org.jetbrains.annotations.NotNull()
    com.seattleshelter.core.api.PetsService petsService, long id) {
        super();
    }
}