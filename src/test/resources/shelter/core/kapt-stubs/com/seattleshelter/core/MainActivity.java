package com.seattleshelter.core;

import java.lang.System;

@kotlin.Metadata(mv = {1, 1, 15}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0002\u0018\u0000 42\u00020\u0001:\u00014B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0016\u0010$\u001a\u00020%2\f\u0010&\u001a\b\u0012\u0004\u0012\u00020(0\'H\u0002J\u0010\u0010)\u001a\u00020%2\u0006\u0010*\u001a\u00020+H\u0002J\b\u0010,\u001a\u00020%H\u0002J\b\u0010-\u001a\u00020%H\u0002J\u0012\u0010.\u001a\u00020%2\b\u0010/\u001a\u0004\u0018\u000100H\u0014J\u0010\u00101\u001a\u00020%2\u0006\u00102\u001a\u000203H\u0002R\u001b\u0010\u0003\u001a\u00020\u00048BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0007\u0010\b\u001a\u0004\b\u0005\u0010\u0006R#\u0010\t\u001a\n \u000b*\u0004\u0018\u00010\n0\n8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000e\u0010\b\u001a\u0004\b\f\u0010\rR#\u0010\u000f\u001a\n \u000b*\u0004\u0018\u00010\u00100\u00108BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0013\u0010\b\u001a\u0004\b\u0011\u0010\u0012R#\u0010\u0014\u001a\n \u000b*\u0004\u0018\u00010\u00150\u00158BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0018\u0010\b\u001a\u0004\b\u0016\u0010\u0017R\u001b\u0010\u0019\u001a\u00020\u001a8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u001d\u0010\b\u001a\u0004\b\u001b\u0010\u001cR\u001e\u0010\u001e\u001a\u00020\u001f8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b \u0010!\"\u0004\b\"\u0010#\u00a8\u00065"}, d2 = {"Lcom/seattleshelter/core/MainActivity;", "Lcom/seattleshelter/core/base/BaseActivity;", "()V", "petAdapter", "Lcom/seattleshelter/core/PetAdapter;", "getPetAdapter", "()Lcom/seattleshelter/core/PetAdapter;", "petAdapter$delegate", "Lkotlin/Lazy;", "petsSpinner", "Landroid/widget/Spinner;", "kotlin.jvm.PlatformType", "getPetsSpinner", "()Landroid/widget/Spinner;", "petsSpinner$delegate", "progressBar", "Landroidx/core/widget/ContentLoadingProgressBar;", "getProgressBar", "()Landroidx/core/widget/ContentLoadingProgressBar;", "progressBar$delegate", "recyclerView", "Landroidx/recyclerview/widget/RecyclerView;", "getRecyclerView", "()Landroidx/recyclerview/widget/RecyclerView;", "recyclerView$delegate", "viewModel", "Lcom/seattleshelter/core/PetsViewModel;", "getViewModel", "()Lcom/seattleshelter/core/PetsViewModel;", "viewModel$delegate", "vmFactory", "Lcom/seattleshelter/core/MainActivityViewModelFactory;", "getVmFactory", "()Lcom/seattleshelter/core/MainActivityViewModelFactory;", "setVmFactory", "(Lcom/seattleshelter/core/MainActivityViewModelFactory;)V", "handlePets", "", "petsList", "", "Lcom/seattleshelter/entities/Pet;", "handleProgressBar", "isPetsListEmpty", "", "initRecyclerView", "initSpinner", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "openPetDetails", "id", "", "Companion", "core_debug"})
public final class MainActivity extends com.seattleshelter.core.base.BaseActivity {
    @org.jetbrains.annotations.NotNull()
    @javax.inject.Inject()
    public com.seattleshelter.core.MainActivityViewModelFactory vmFactory;
    private final kotlin.Lazy viewModel$delegate = null;
    private final kotlin.Lazy petAdapter$delegate = null;
    private final kotlin.Lazy recyclerView$delegate = null;
    private final kotlin.Lazy petsSpinner$delegate = null;
    private final kotlin.Lazy progressBar$delegate = null;
    private static final java.lang.String TAG = "MainActivity";
    public static final com.seattleshelter.core.MainActivity.Companion Companion = null;
    
    @org.jetbrains.annotations.NotNull()
    public final com.seattleshelter.core.MainActivityViewModelFactory getVmFactory() {
        return null;
    }
    
    public final void setVmFactory(@org.jetbrains.annotations.NotNull()
    com.seattleshelter.core.MainActivityViewModelFactory p0) {
    }
    
    private final com.seattleshelter.core.PetsViewModel getViewModel() {
        return null;
    }
    
    private final com.seattleshelter.core.PetAdapter getPetAdapter() {
        return null;
    }
    
    private final androidx.recyclerview.widget.RecyclerView getRecyclerView() {
        return null;
    }
    
    private final android.widget.Spinner getPetsSpinner() {
        return null;
    }
    
    private final androidx.core.widget.ContentLoadingProgressBar getProgressBar() {
        return null;
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void initSpinner() {
    }
    
    private final void initRecyclerView() {
    }
    
    private final void handlePets(java.util.List<com.seattleshelter.entities.Pet> petsList) {
    }
    
    private final void handleProgressBar(boolean isPetsListEmpty) {
    }
    
    private final void openPetDetails(long id) {
    }
    
    public MainActivity() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 1, 15}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/seattleshelter/core/MainActivity$Companion;", "", "()V", "TAG", "", "core_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}