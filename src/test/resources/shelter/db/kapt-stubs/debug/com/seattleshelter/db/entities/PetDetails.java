package com.seattleshelter.db.entities;

import java.lang.System;

@androidx.room.Entity(tableName = "pet_details")
@kotlin.Metadata(mv = {1, 1, 15}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010 \n\u0002\b\b\n\u0002\u0010\u000b\n\u0002\b\u001b\b\u0007\u0018\u00002\u00020\u0001B\u0091\u0001\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\f\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00070\t\u0012\b\u0010\n\u001a\u0004\u0018\u00010\u0007\u0012\u0006\u0010\u000b\u001a\u00020\u0007\u0012\b\u0010\f\u001a\u0004\u0018\u00010\u0007\u0012\b\u0010\r\u001a\u0004\u0018\u00010\u0007\u0012\b\u0010\u000e\u001a\u0004\u0018\u00010\u0007\u0012\b\u0010\u000f\u001a\u0004\u0018\u00010\u0007\u0012\b\u0010\u0010\u001a\u0004\u0018\u00010\u0007\u0012\b\u0010\u0011\u001a\u0004\u0018\u00010\u0012\u0012\u0006\u0010\u0013\u001a\u00020\u0012\u0012\u0006\u0010\u0014\u001a\u00020\u0012\u0012\u0006\u0010\u0015\u001a\u00020\u0012\u00a2\u0006\u0002\u0010\u0016R\u0011\u0010\u0015\u001a\u00020\u0012\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0018R\u0013\u0010\u000e\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u001aR\u0015\u0010\u0011\u001a\u0004\u0018\u00010\u0012\u00a2\u0006\n\n\u0002\u0010\u001d\u001a\u0004\b\u001b\u0010\u001cR\u0013\u0010\r\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001aR\u0013\u0010\u0010\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010\u001aR\u0011\u0010\u0013\u001a\u00020\u0012\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010\u0018R\u0011\u0010\u000b\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b!\u0010\u001aR\u0016\u0010\u0002\u001a\u00020\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010#R\u0011\u0010\u0014\u001a\u00020\u0012\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010\u0018R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b%\u0010\u001aR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010\'R\u0017\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00070\t\u00a2\u0006\b\n\u0000\u001a\u0004\b(\u0010)R\u0013\u0010\f\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b*\u0010\u001aR\u0013\u0010\u000f\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b+\u0010\u001aR\u0013\u0010\n\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b,\u0010\u001a\u00a8\u0006-"}, d2 = {"Lcom/seattleshelter/db/entities/PetDetails;", "", "id", "", "petType", "Lcom/seattleshelter/entities/PetType;", "name", "", "picUrls", "", "videoUrl", "description", "sex", "breed", "age", "size", "color", "altered", "", "declawed", "inFosterCare", "adoptionPending", "(JLcom/seattleshelter/entities/PetType;Ljava/lang/String;Ljava/util/List;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Boolean;ZZZ)V", "getAdoptionPending", "()Z", "getAge", "()Ljava/lang/String;", "getAltered", "()Ljava/lang/Boolean;", "Ljava/lang/Boolean;", "getBreed", "getColor", "getDeclawed", "getDescription", "getId", "()J", "getInFosterCare", "getName", "getPetType", "()Lcom/seattleshelter/entities/PetType;", "getPicUrls", "()Ljava/util/List;", "getSex", "getSize", "getVideoUrl", "db_debug"})
public final class PetDetails {
    @androidx.room.PrimaryKey()
    private final long id = 0L;
    @org.jetbrains.annotations.NotNull()
    private final com.seattleshelter.entities.PetType petType = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String name = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> picUrls = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String videoUrl = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String description = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String sex = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String breed = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String age = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String size = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String color = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Boolean altered = null;
    private final boolean declawed = false;
    private final boolean inFosterCare = false;
    private final boolean adoptionPending = false;
    
    public final long getId() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.seattleshelter.entities.PetType getPetType() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getName() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> getPicUrls() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getVideoUrl() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getDescription() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getSex() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getBreed() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getAge() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getSize() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getColor() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Boolean getAltered() {
        return null;
    }
    
    public final boolean getDeclawed() {
        return false;
    }
    
    public final boolean getInFosterCare() {
        return false;
    }
    
    public final boolean getAdoptionPending() {
        return false;
    }
    
    public PetDetails(long id, @org.jetbrains.annotations.NotNull()
    com.seattleshelter.entities.PetType petType, @org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> picUrls, @org.jetbrains.annotations.Nullable()
    java.lang.String videoUrl, @org.jetbrains.annotations.NotNull()
    java.lang.String description, @org.jetbrains.annotations.Nullable()
    java.lang.String sex, @org.jetbrains.annotations.Nullable()
    java.lang.String breed, @org.jetbrains.annotations.Nullable()
    java.lang.String age, @org.jetbrains.annotations.Nullable()
    java.lang.String size, @org.jetbrains.annotations.Nullable()
    java.lang.String color, @org.jetbrains.annotations.Nullable()
    java.lang.Boolean altered, boolean declawed, boolean inFosterCare, boolean adoptionPending) {
        super();
    }
}