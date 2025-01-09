// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package nexus

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response

class NexusOkHttpInterceptor(
  private val username: String, private val password: String
) : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val requestBuilder = chain.request().newBuilder()

    requestBuilder.addHeader("Accept", "application/json")
    requestBuilder.addHeader("Authorization", Credentials.basic(username, password))

    return chain.proceed(requestBuilder.build())
  }
}
