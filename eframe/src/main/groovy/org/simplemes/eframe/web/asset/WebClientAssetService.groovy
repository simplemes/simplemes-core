/*
 * Copyright (c) Michael Houston 2021. All rights reserved.
 */

package org.simplemes.eframe.web.asset

import groovy.util.logging.Slf4j
import io.micronaut.core.naming.NameUtils
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.filter.ServerFilterChain
import io.micronaut.http.server.types.files.StreamedFile
import io.reactivex.Flowable

import javax.inject.Singleton

/**
 * Handles requests for assets from the client (vue-based).  This uses the /client/module folder on the classpath
 * to provide the assets.
 * <p/>
 * See the <a href="https://simplemes.github.io/simplemes-core/eframe/guide.html">Framework Guide</a>
 * for details on the client-side GUI support.
 *
 */
@Slf4j
@Singleton
class WebClientAssetService {

  /**
   * Handles the request from the client for web assets.
   * @param filename
   * @param request
   * @param chain
   * @return
   */
  Flowable<MutableHttpResponse<?>> handleAsset(String filename, HttpRequest<?> request, ServerFilterChain chain) {
    // TODO: Tests?
    def resource = WebClientAssetService.classLoader.getResource(filename)
    if (!resource) {
      // Try as a .html case.
      resource = WebClientAssetService.classLoader.getResource(filename + '.html')
      if (resource) {
        filename += ".html"
      }
    }
    def gzipResource = WebClientAssetService.classLoader.getResource(filename + ".gz")
    def webClientAsset = new WebClientAsset(exists: resource != null, resource: resource)
    if (gzipResource) {
      webClientAsset.gzipExists = true
      webClientAsset.gzipResource = gzipResource
    }
    def assetFlowable = Flowable.fromCallable(() -> webClientAsset)
    def etagHeader = filename
    MediaType contentType = MediaType.forExtension(NameUtils.extension(filename)).orElseGet(() -> null)
    MediaType requestContentType = contentType != null ? contentType : MediaType.forExtension("html").get()

    String acceptEncoding = request.getHeaders().get("Accept-Encoding")
    String encoding = request.getParameters().getFirst("encoding").orElseGet(() -> request.getCharacterEncoding().toString())

    return assetFlowable.switchMap(asset -> {
      if (asset.exists) {
        final Boolean gzip = acceptEncoding != null && acceptEncoding.contains("gzip") && asset.gzipExists
        String ifNoneMatch = request.getHeaders().get("If-None-Match")
        if (ifNoneMatch != null && ifNoneMatch == etagHeader) {
          log.debug("{} not modified.  Details: {}", filename, asset)
          return Flowable.fromCallable(() -> HttpResponse.notModified())
        } else {
          log.debug("{} found.  Returning: {}", filename, asset)
          return Flowable.fromCallable(() -> {
            URLConnection urlCon = asset.gzipExists ? asset.getGzipResource().openConnection() : asset.getResource().openConnection()
            StreamedFile streamedFile = new StreamedFile(urlCon.getInputStream(), contentType, urlCon.getLastModified(), urlCon.getContentLength())
            MutableHttpResponse<StreamedFile> response = HttpResponse.ok(streamedFile)
            if (gzip) {
              response.header("Content-Encoding", "gzip")
            }
            if (encoding) {
              response.characterEncoding(encoding)
            }
            response.contentType(requestContentType)
            //response.header("ETag", etagHeader)
            response.header("Vary", "Accept-Encoding")
            response.header("Cache-Control", "public, max-age=31536000")
            return response
          })
        }

      } else {
        return chain.proceed(request)
      }
    })
  }


}
