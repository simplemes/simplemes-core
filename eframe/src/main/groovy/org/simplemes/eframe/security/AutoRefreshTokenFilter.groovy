/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security

import groovy.util.logging.Slf4j
import io.micronaut.core.async.publisher.Publishers
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import io.micronaut.http.filter.ServerFilterPhase
import io.micronaut.security.handlers.LoginHandler
import io.netty.handler.codec.http.HttpHeaderNames
import org.reactivestreams.Publisher

/**
 * Checks all incoming requests and will trigger an auto-refresh of the access token if needed.
 * Works with {@link AutoRefreshAuthenticationFetcher} to update the JWT cookies on browser for this response.
 *
 */
@Slf4j
@Filter("/**")
class AutoRefreshTokenFilter implements HttpServerFilter {
  protected final LoginHandler loginHandler


  AutoRefreshTokenFilter(LoginHandler loginHandler) {
    this.loginHandler = loginHandler
  }

  /**
   * @return The order of the object. Defaults to zero (no order).
   */
  @Override
  int getOrder() {
    return ServerFilterPhase.LAST.order()
  }


  /**
   * Variation of the {@link #doFilter(HttpRequest, io.micronaut.http.filter.FilterChain)} method that accepts a {@link ServerFilterChain}
   * which allows to mutate the outgoing HTTP response.
   *
   * @param request The request
   * @param chain The chain
   * @return A{@link Publisher} that emits a {@link MutableHttpResponse}
   * @see #doFilter(HttpRequest, io.micronaut.http.filter.FilterChain)
   */
  @Override
  Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
    return Publishers.then(chain.proceed(request), (response) -> {
      if (!request.path.startsWith('/assets/')) {
        def replacementTokenResponse = request.getAttribute(AutoRefreshAuthenticationFetcher.AUTO_REFRESH_ATTRIBUTE).orElse(null) as ReplacementTokenResponse
        if (replacementTokenResponse) {
          log.debug('doFilter(): replacementTokenResponse = {}', replacementTokenResponse)
          def res = loginHandler.loginRefresh(replacementTokenResponse.userDetails, replacementTokenResponse.refreshToken, request)
          for (value in res.headers.getAll(HttpHeaderNames.SET_COOKIE)) {
            response.headers.add(HttpHeaderNames.SET_COOKIE, value)
            log.debug('doFilter(): added Cookie {}', value)
          }
        }
      }
    })

  }

}