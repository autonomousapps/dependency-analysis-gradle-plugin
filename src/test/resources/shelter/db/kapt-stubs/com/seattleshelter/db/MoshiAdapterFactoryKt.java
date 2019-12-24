package com.seattleshelter.db;

import java.lang.System;

@kotlin.Metadata(mv = {1, 1, 15}, bv = {1, 0, 3}, k = 2, d1 = {"\u0000\u001e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\u001a\u0017\u0010\u0000\u001a\b\u0012\u0004\u0012\u0002H\u00020\u0001\"\u0006\b\u0000\u0010\u0002\u0018\u0001H\u0086\b\u001a\u0006\u0010\u0003\u001a\u00020\u0004\u001a\u0006\u0010\u0005\u001a\u00020\u0006\u001a\u001c\u0010\u0007\u001a\u0004\u0018\u0001H\u0002\"\u0006\b\u0000\u0010\u0002\u0018\u0001*\u00020\bH\u0086\b\u00a2\u0006\u0002\u0010\t\u001a\u001a\u0010\n\u001a\u00020\b\"\u0006\b\u0000\u0010\u0002\u0018\u0001*\u0002H\u0002H\u0086\b\u00a2\u0006\u0002\u0010\u000b\u00a8\u0006\f"}, d2 = {"getJsonAdapter", "Lcom/squareup/moshi/JsonAdapter;", "T", "getMoshi", "Lcom/squareup/moshi/Moshi;", "moshiConverterFactory", "Lretrofit2/converter/moshi/MoshiConverterFactory;", "fromJson", "", "(Ljava/lang/String;)Ljava/lang/Object;", "toJson", "(Ljava/lang/Object;)Ljava/lang/String;", "db_debug"})
public final class MoshiAdapterFactoryKt {
    
    /**
     * For use with Retrofit, to enable it to deserialize JSON responses using Moshi. Use it like so:
     *
     * ```
     * Retrofit retrofit = new Retrofit.Builder()
     *        .addConverterFactory(moshiConverterFactory())
     *        .build();
     * ```
     *
     * @return a [MoshiConverterFactory] for use by Retrofit.
     */
    @org.jetbrains.annotations.NotNull()
    public static final retrofit2.converter.moshi.MoshiConverterFactory moshiConverterFactory() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public static final com.squareup.moshi.Moshi getMoshi() {
        return null;
    }
}