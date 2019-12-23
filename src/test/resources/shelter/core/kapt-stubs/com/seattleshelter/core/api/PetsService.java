package com.seattleshelter.core.api;

import java.lang.System;

@kotlin.Metadata(mv = {1, 1, 15}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\t\bf\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\u0006\u0010\u0005\u001a\u00020\u0006H&J\u0014\u0010\u0007\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\b0\u0003H&J\u0016\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\u0006\u0010\u0005\u001a\u00020\u0006H&J\u0014\u0010\u000b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\b0\u0003H&J\u0016\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\u0006\u0010\u0005\u001a\u00020\u0006H&J\u0014\u0010\r\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\b0\u0003H&J\u0016\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\u0006\u0010\u0005\u001a\u00020\u0006H&J\u0014\u0010\u000f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\b0\u0003H&J\u0016\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\u0006\u0010\u0005\u001a\u00020\u0006H&J\u0014\u0010\u0011\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\b0\u0003H&\u00a8\u0006\u0012"}, d2 = {"Lcom/seattleshelter/core/api/PetsService;", "", "getCatById", "Lio/reactivex/Single;", "Lcom/seattleshelter/entities/PetDetails;", "id", "", "getCatsList", "", "Lcom/seattleshelter/entities/Pet;", "getDogById", "getDogsList", "getRabbitById", "getRabbitsList", "getReptileById", "getReptilesList", "getSmallMammalById", "getSmallMammalsList", "core_debug"})
public abstract interface PetsService {
    
    @org.jetbrains.annotations.NotNull()
    public abstract io.reactivex.Single<java.util.List<com.seattleshelter.entities.Pet>> getCatsList();
    
    @org.jetbrains.annotations.NotNull()
    public abstract io.reactivex.Single<com.seattleshelter.entities.PetDetails> getCatById(long id);
    
    @org.jetbrains.annotations.NotNull()
    public abstract io.reactivex.Single<java.util.List<com.seattleshelter.entities.Pet>> getDogsList();
    
    @org.jetbrains.annotations.NotNull()
    public abstract io.reactivex.Single<com.seattleshelter.entities.PetDetails> getDogById(long id);
    
    @org.jetbrains.annotations.NotNull()
    public abstract io.reactivex.Single<java.util.List<com.seattleshelter.entities.Pet>> getRabbitsList();
    
    @org.jetbrains.annotations.NotNull()
    public abstract io.reactivex.Single<com.seattleshelter.entities.PetDetails> getRabbitById(long id);
    
    @org.jetbrains.annotations.NotNull()
    public abstract io.reactivex.Single<java.util.List<com.seattleshelter.entities.Pet>> getSmallMammalsList();
    
    @org.jetbrains.annotations.NotNull()
    public abstract io.reactivex.Single<com.seattleshelter.entities.PetDetails> getSmallMammalById(long id);
    
    @org.jetbrains.annotations.NotNull()
    public abstract io.reactivex.Single<java.util.List<com.seattleshelter.entities.Pet>> getReptilesList();
    
    @org.jetbrains.annotations.NotNull()
    public abstract io.reactivex.Single<com.seattleshelter.entities.PetDetails> getReptileById(long id);
}