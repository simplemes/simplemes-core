/*
 * Copyright (c) Michael Houston 2021. All rights reserved.
 */

package org.simplemes.eframe.web.asset

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import org.reactivestreams.Publisher

/**
 * This filter provides access to the client (vue-based) assets.  This uses the client .jar files on the classpath
 * to provide the assets.
 * <p/>
 * See the <a href="https://simplemes.github.io/simplemes-core/eframe/guide.html">Framework Guide</a>
 * for details on the client-side GUI support.
 *
 */
@Slf4j
@Filter("/client/**")
class WebClientAssetFilter implements HttpServerFilter {

  protected WebClientAssetService webClientAssetService

  WebClientAssetFilter(WebClientAssetService webClientAssetService) {
    this.webClientAssetService = webClientAssetService
  }

  /**
   * Handles the request for the vue-based client asset requests on the '/client/**' path.
   * All vue-based clients are hosted from that public path.
   *
   * @param request The request
   * @param chain The chain
   * @return A{@link org.reactivestreams.Publisher} that emits a {@link io.micronaut.http.MutableHttpResponse}
   */
  @Override
  Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {

    String fileUri = request.getUri()
    if (fileUri.startsWith('/client/')) {
      fileUri = fileUri[1..-1]  // Strip the leading / since the classloader will not find it.
      return webClientAssetService.handleAsset(fileUri, request, chain)
    }
    //final String baseAssetUrl = "/" + assetMapping;

    return chain.proceed(request)


  }
}
