package com.seattleshelter.db;

import java.lang.System;

@androidx.room.TypeConverters(value = {com.seattleshelter.db.Converters.class})
@androidx.room.Database(entities = {com.seattleshelter.db.entities.Pet.class}, version = 1, exportSchema = false)
@kotlin.Metadata(mv = {1, 1, 15}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\'\u0018\u0000 \u00052\u00020\u0001:\u0001\u0005B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H&\u00a8\u0006\u0006"}, d2 = {"Lcom/seattleshelter/db/ShelterDatabase;", "Landroidx/room/RoomDatabase;", "()V", "petDao", "Lcom/seattleshelter/db/daos/PetDao;", "Companion", "db_debug"})
public abstract class ShelterDatabase extends androidx.room.RoomDatabase {
    private static final java.lang.String DATABASE_NAME = "shelter-database";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String TABLE_PETS = "pets";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String TABLE_PET_DETAILS = "pet_details";
    public static final com.seattleshelter.db.ShelterDatabase.Companion Companion = null;
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.seattleshelter.db.daos.PetDao petDao();
    
    public ShelterDatabase() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 1, 15}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nH\u0002J\u000e\u0010\u000b\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0080T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0080T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\f"}, d2 = {"Lcom/seattleshelter/db/ShelterDatabase$Companion;", "", "()V", "DATABASE_NAME", "", "TABLE_PETS", "TABLE_PET_DETAILS", "buildDatabase", "Lcom/seattleshelter/db/ShelterDatabase;", "context", "Landroid/content/Context;", "init", "db_debug"})
    public static final class Companion {
        
        @org.jetbrains.annotations.NotNull()
        public final com.seattleshelter.db.ShelterDatabase init(@org.jetbrains.annotations.NotNull()
        android.content.Context context) {
            return null;
        }
        
        private final com.seattleshelter.db.ShelterDatabase buildDatabase(android.content.Context context) {
            return null;
        }
        
        private Companion() {
            super();
        }
    }
}