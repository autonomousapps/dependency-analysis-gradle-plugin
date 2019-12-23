package com.seattleshelter.core;

import java.lang.System;

@kotlin.Metadata(mv = {1, 1, 15}, bv = {1, 0, 3}, k = 1, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\t\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u000e\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0018\u0002\n\u0000\u0018\u00002\u00020\u0001B!\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00070\u0005\u00a2\u0006\u0002\u0010\bJ\u0010\u0010#\u001a\u00020\u00072\u0006\u0010$\u001a\u00020%H\u0007R\u001b\u0010\t\u001a\u00020\n8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\r\u0010\u000e\u001a\u0004\b\u000b\u0010\fR\u001b\u0010\u000f\u001a\u00020\n8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0011\u0010\u000e\u001a\u0004\b\u0010\u0010\fR\u001b\u0010\u0012\u001a\u00020\n8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0014\u0010\u000e\u001a\u0004\b\u0013\u0010\fR\u001a\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00070\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0015\u001a\u00020\n8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0017\u0010\u000e\u001a\u0004\b\u0016\u0010\fR\u001b\u0010\u0018\u001a\u00020\u00198BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u001c\u0010\u000e\u001a\u0004\b\u001a\u0010\u001bR\u001b\u0010\u001d\u001a\u00020\n8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u001f\u0010\u000e\u001a\u0004\b\u001e\u0010\fR\u001b\u0010 \u001a\u00020\n8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\"\u0010\u000e\u001a\u0004\b!\u0010\f\u00a8\u0006&"}, d2 = {"Lcom/seattleshelter/core/PetViewHolder;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "itemView", "Landroid/view/View;", "clickListener", "Lkotlin/Function1;", "", "", "(Landroid/view/View;Lkotlin/jvm/functions/Function1;)V", "adoptionPending", "Landroid/widget/TextView;", "getAdoptionPending", "()Landroid/widget/TextView;", "adoptionPending$delegate", "Lkotlin/Lazy;", "age", "getAge", "age$delegate", "breed", "getBreed", "breed$delegate", "foster", "getFoster", "foster$delegate", "imageView", "Landroid/widget/ImageView;", "getImageView", "()Landroid/widget/ImageView;", "imageView$delegate", "name", "getName", "name$delegate", "sex", "getSex", "sex$delegate", "bind", "pet", "Lcom/seattleshelter/entities/Pet;", "core_debug"})
public final class PetViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
    private final kotlin.Lazy name$delegate = null;
    private final kotlin.Lazy sex$delegate = null;
    private final kotlin.Lazy breed$delegate = null;
    private final kotlin.Lazy age$delegate = null;
    private final kotlin.Lazy foster$delegate = null;
    private final kotlin.Lazy adoptionPending$delegate = null;
    private final kotlin.Lazy imageView$delegate = null;
    private final kotlin.jvm.functions.Function1<java.lang.Long, kotlin.Unit> clickListener = null;
    
    private final android.widget.TextView getName() {
        return null;
    }
    
    private final android.widget.TextView getSex() {
        return null;
    }
    
    private final android.widget.TextView getBreed() {
        return null;
    }
    
    private final android.widget.TextView getAge() {
        return null;
    }
    
    private final android.widget.TextView getFoster() {
        return null;
    }
    
    private final android.widget.TextView getAdoptionPending() {
        return null;
    }
    
    private final android.widget.ImageView getImageView() {
        return null;
    }
    
    @android.annotation.SuppressLint(value = {"SetTextI18n"})
    public final void bind(@org.jetbrains.annotations.NotNull()
    com.seattleshelter.entities.Pet pet) {
    }
    
    public PetViewHolder(@org.jetbrains.annotations.NotNull()
    android.view.View itemView, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Long, kotlin.Unit> clickListener) {
        super(null);
    }
}