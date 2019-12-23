package com.seattleshelter.db.daos;

import java.lang.System;

@androidx.room.Dao()
@kotlin.Metadata(mv = {1, 1, 15}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0006\b\'\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0015\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H!\u00a2\u0006\u0002\b\u0007J\u001c\u0010\b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000b0\n0\t2\u0006\u0010\u0005\u001a\u00020\u0006H\'J\u001b\u0010\f\u001a\u00020\u00042\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000b0\nH!\u00a2\u0006\u0002\b\u000eJ\u0016\u0010\u000f\u001a\u00020\u00042\f\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u000b0\nH\u0017\u00a8\u0006\u0011"}, d2 = {"Lcom/seattleshelter/db/daos/PetDao;", "", "()V", "deleteAllPetsFor", "", "petType", "Lcom/seattleshelter/entities/PetType;", "deleteAllPetsFor$db_debug", "getPetsFor", "Lio/reactivex/Flowable;", "", "Lcom/seattleshelter/db/entities/Pet;", "insertPets", "cat", "insertPets$db_debug", "replacePets", "pets", "db_debug"})
public abstract class PetDao {
    
    @org.jetbrains.annotations.NotNull()
    @androidx.room.Query(value = "SELECT * FROM pets WHERE petType = :petType")
    public abstract io.reactivex.Flowable<java.util.List<com.seattleshelter.db.entities.Pet>> getPetsFor(@org.jetbrains.annotations.NotNull()
    com.seattleshelter.entities.PetType petType);
    
    @androidx.room.Transaction()
    public void replacePets(@org.jetbrains.annotations.NotNull()
    java.util.List<com.seattleshelter.db.entities.Pet> pets) {
    }
    
    @androidx.room.Query(value = "DELETE FROM pets WHERE petType = :petType")
    public abstract void deleteAllPetsFor$db_debug(@org.jetbrains.annotations.NotNull()
    com.seattleshelter.entities.PetType petType);
    
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    public abstract void insertPets$db_debug(@org.jetbrains.annotations.NotNull()
    java.util.List<com.seattleshelter.db.entities.Pet> cat);
    
    public PetDao() {
        super();
    }
}