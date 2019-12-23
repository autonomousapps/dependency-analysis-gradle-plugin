package com.seattleshelter.core.api;

import java.lang.System;

@kotlin.Metadata(mv = {1, 1, 15}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\t\b`\u0018\u00002\u00020\u0001J\u0018\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u0006H\'J\u0014\u0010\u0007\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\b0\u0003H\'J\u0018\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u0006H\'J\u0014\u0010\u000b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\b0\u0003H\'J\u0018\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u0006H\'J\u0014\u0010\r\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\b0\u0003H\'J\u0018\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u0006H\'J\u0014\u0010\u000f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\b0\u0003H\'J\u0018\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u0006H\'J\u0014\u0010\u0011\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\b0\u0003H\'\u00a8\u0006\u0012"}, d2 = {"Lcom/seattleshelter/core/api/InnerPetsService;", "", "getCatById", "Lio/reactivex/Single;", "Lcom/seattleshelter/entities/PetDetails;", "id", "", "getCatsList", "", "Lcom/seattleshelter/entities/Pet;", "getDogById", "getDogsList", "getRabbitById", "getRabbitsList", "getReptileById", "getReptilesList", "getSmallMammalById", "getSmallMammalsList", "core_debug"})
public abstract interface InnerPetsService {
    
    @org.jetbrains.annotations.NotNull()
    @retrofit2.http.GET(value = "cats")
    public abstract io.reactivex.Single<java.util.List<com.seattleshelter.entities.Pet>> getCatsList();
    
    @org.jetbrains.annotations.NotNull()
    @retrofit2.http.GET(value = "cats/{id}")
    public abstract io.reactivex.Single<com.seattleshelter.entities.PetDetails> getCatById(@retrofit2.http.Path(value = "id")
    long id);
    
    @org.jetbrains.annotations.NotNull()
    @retrofit2.http.GET(value = "dogs")
    public abstract io.reactivex.Single<java.util.List<com.seattleshelter.entities.Pet>> getDogsList();
    
    @org.jetbrains.annotations.NotNull()
    @retrofit2.http.GET(value = "dogs/{id}")
    public abstract io.reactivex.Single<com.seattleshelter.entities.PetDetails> getDogById(@retrofit2.http.Path(value = "id")
    long id);
    
    @org.jetbrains.annotations.NotNull()
    @retrofit2.http.GET(value = "rabbits")
    public abstract io.reactivex.Single<java.util.List<com.seattleshelter.entities.Pet>> getRabbitsList();
    
    @org.jetbrains.annotations.NotNull()
    @retrofit2.http.GET(value = "rabbits/{id}")
    public abstract io.reactivex.Single<com.seattleshelter.entities.PetDetails> getRabbitById(@retrofit2.http.Path(value = "id")
    long id);
    
    @org.jetbrains.annotations.NotNull()
    @retrofit2.http.GET(value = "smallmammals")
    public abstract io.reactivex.Single<java.util.List<com.seattleshelter.entities.Pet>> getSmallMammalsList();
    
    @org.jetbrains.annotations.NotNull()
    @retrofit2.http.GET(value = "smallmammals/{id}")
    public abstract io.reactivex.Single<com.seattleshelter.entities.PetDetails> getSmallMammalById(@retrofit2.http.Path(value = "id")
    long id);
    
    @org.jetbrains.annotations.NotNull()
    @retrofit2.http.GET(value = "reptiles")
    public abstract io.reactivex.Single<java.util.List<com.seattleshelter.entities.Pet>> getReptilesList();
    
    @org.jetbrains.annotations.NotNull()
    @retrofit2.http.GET(value = "reptiles/{id}")
    public abstract io.reactivex.Single<com.seattleshelter.entities.PetDetails> getReptileById(@retrofit2.http.Path(value = "id")
    long id);
}