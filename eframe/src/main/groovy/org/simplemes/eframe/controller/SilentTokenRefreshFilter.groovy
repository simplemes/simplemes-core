/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.controller

import com.nimbusds.jwt.JWTParser
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.FilterChain
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import io.micronaut.http.netty.cookies.NettyCookie
import io.micronaut.security.token.jwt.generator.AccessTokenConfiguration
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Publisher
import org.simplemes.eframe.application.Holders

/**
 *
 */
@Filter("/**")
class SilentTokenRefreshFilter implements HttpServerFilter {


  /**
   * Variation of the {@link #doFilter(HttpRequest, FilterChain)} method that accepts a {@link ServerFilterChain}
   * which allows to mutate the outgoing HTTP response.
   *
   * @param request The request
   * @param chain The chain
   * @return A{@link Publisher} that emits a {@link MutableHttpResponse}
   * @see #doFilter(HttpRequest, FilterChain)
   */
  @Override
  Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
    processRequest(request)
      .switchMap({ aBoolean -> chain.proceed(request) })
      .doOnNext({ res ->
        def accept = request.headers.get(HttpHeaders.ACCEPT) ?: ''
        def value = request.cookies?.get('JWT')?.getValue()
        if (value && accept.contains(MediaType.TEXT_HTML)) {
          // Find the access token timeout
          AccessTokenConfiguration config = Holders.getBean(AccessTokenConfiguration)

          def jwt = JWTParser.parse(value)
          def payload = (String) jwt.payload
          if (payload) {
            def json = Holders.objectMapper.readValue(payload, Map)
            def exp = Long.valueOf((String) json.exp)
            def delta = exp - System.currentTimeSeconds() - 60  // Refresh one minute early.
            if (delta > 0) {
              def cookie = new NettyCookie("JWT_SILENT_REFRESH", "${delta.toString()},$config.expiration")
              cookie.path("/")
              res.cookie(cookie)
            }
          }
        }
      })
  }

  /**
   * Creates the Flowable to process the HTTP request for the silent token refresh cookie.
   * @param request The request.
   * @return The flowable to process the request.
   */
  @SuppressWarnings('unused')
  Flowable<Boolean> processRequest(HttpRequest<?> request) {
    Flowable.fromCallable({ ->
      return true
    }).subscribeOn(Schedulers.io())
  }

}
