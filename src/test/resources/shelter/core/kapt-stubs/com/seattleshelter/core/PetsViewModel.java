package com.seattleshelter.core;

import java.lang.System;

@kotlin.Metadata(mv = {1, 1, 15}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000R\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0004\u0018\u0000 \u001b2\u00020\u0001:\u0001\u001bB\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0014\u0010\u0013\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u00120\u0014H\u0002J\u0012\u0010\u0015\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u00120\u0016J\u000e\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u000e\u001a\u00020\u000fJ\b\u0010\u0019\u001a\u00020\u0018H\u0002J\b\u0010\u001a\u001a\u00020\u0018H\u0002R\u000e\u0010\u0007\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000R\u001a\u0010\n\u001a\u000e\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\r0\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0010\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u00120\u0011X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001c"}, d2 = {"Lcom/seattleshelter/core/PetsViewModel;", "Lcom/seattleshelter/core/base/DisposingViewModel;", "petDao", "Lcom/seattleshelter/db/daos/PetDao;", "petsService", "Lcom/seattleshelter/core/api/PetsService;", "(Lcom/seattleshelter/db/daos/PetDao;Lcom/seattleshelter/core/api/PetsService;)V", "dbSubscription", "Lio/reactivex/disposables/Disposable;", "netSubscription", "ordering", "Lkotlin/Function1;", "Lcom/seattleshelter/entities/Pet;", "", "petType", "Lcom/seattleshelter/entities/PetType;", "petsListData", "Landroidx/lifecycle/MutableLiveData;", "", "getPetsList", "Lio/reactivex/Single;", "petsList", "Landroidx/lifecycle/LiveData;", "setPetType", "", "subscribeToApi", "subscribeToDb", "Companion", "core_debug"})
public final class PetsViewModel extends com.seattleshelter.core.base.DisposingViewModel {
    private com.seattleshelter.entities.PetType petType;
    private final androidx.lifecycle.MutableLiveData<java.util.List<com.seattleshelter.entities.Pet>> petsListData = null;
    private io.reactivex.disposables.Disposable netSubscription;
    private io.reactivex.disposables.Disposable dbSubscription;
    private kotlin.jvm.functions.Function1<? super com.seattleshelter.entities.Pet, java.lang.String> ordering;
    private final com.seattleshelter.db.daos.PetDao petDao = null;
    private final com.seattleshelter.core.api.PetsService petsService = null;
    private static final java.lang.String TAG = "PetsViewModel";
    public static final com.seattleshelter.core.PetsViewModel.Companion Companion = null;
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.lifecycle.LiveData<java.util.List<com.seattleshelter.entities.Pet>> petsList() {
        return null;
    }
    
    public final void setPetType(@org.jetbrains.annotations.NotNull()
    com.seattleshelter.entities.PetType petType) {
    }
    
    private final void subscribeToDb() {
    }
    
    private final void subscribeToApi() {
    }
    
    private final io.reactivex.Single<java.util.List<com.seattleshelter.entities.Pet>> getPetsList() {
        return null;
    }
    
    public PetsViewModel(@org.jetbrains.annotations.NotNull()
    com.seattleshelter.db.daos.PetDao petDao, @org.jetbrains.annotations.NotNull()
    com.seattleshelter.core.api.PetsService petsService) {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 1, 15}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/seattleshelter/core/PetsViewModel$Companion;", "", "()V", "TAG", "", "core_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}