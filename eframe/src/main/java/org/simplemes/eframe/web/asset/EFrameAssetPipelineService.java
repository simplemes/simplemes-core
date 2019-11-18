package org.simplemes.eframe.web.asset;

import asset.pipeline.AssetPipelineConfigHolder;
import asset.pipeline.fs.FileSystemAssetResolver;
import asset.pipeline.micronaut.AssetPipelineService;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.filter.ServerFilterChain;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Collection;

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
 */

/**
 * Development mode asset pipeline service.  Works in dev mode for local (file system) assets, but
 * allows production mode assets from the .jar files (via the asset pipeline manifest).
 * This means no need to restart the server when an asset changes.
 */
@SuppressWarnings("unused")
@Singleton
@Replaces(AssetPipelineService.class)
public class EFrameAssetPipelineService extends AssetPipelineService {
  private static final Logger LOG = LoggerFactory.getLogger(EFrameAssetPipelineService.class);

  public EFrameAssetPipelineService() {
    AssetPipelineConfigHolder.registerResolver(new FileSystemAssetResolver("application", "src/assets"));
  }

  /**
   * Determines if the we should force dev mode on the given asset.  Used to detect mixed-mode scenarios
   * when some assets come from a .jar file and some from locale file system in dev mode.
   *
   * @param fileName The file to check for dev mode.
   * @return True if in the file is not in the manifest and should trigger dev mode.
   */
  protected boolean shouldForceDevMode(String fileName) {
    if (fileName.startsWith("/")) {
      fileName = fileName.substring(1);
    }
    if (AssetPipelineConfigHolder.manifest != null) {
      // Check for digest version of the asset (contains a has that starts with '-').
      if (fileName.contains("-")) {
        // See if the digest version exists in the manifest.
        Collection values = AssetPipelineConfigHolder.manifest.values();
        if (values.contains(fileName)) {
          // Let the production mode logic handle the request.
          return false;
        }
      }

      // Trigger dev mode if the file is not in any manifest loaded for the asset pipeline.
      return AssetPipelineConfigHolder.manifest.get(fileName) == null;
    }

    return false;
  }

  /**
   * Handles the asset request, but falls back to dev mode if the asset is no in the manifest.
   */
  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Override
  public Flowable<MutableHttpResponse<?>> handleAsset(String filename, MediaType contentType, String encoding, HttpRequest<?> request, ServerFilterChain chain) {
    if (shouldForceDevMode(filename)) {
      // Not in manifest, so force dev mode.
      final String format = contentType != null ? contentType.toString() : null;
      LOG.debug("DevMode for " + filename);

      return super.handleAssetDevMode(filename, format, encoding, request).switchMap(contents -> {
        if (contents.isPresent()) {
          return Flowable.fromCallable(() -> {
            MutableHttpResponse<byte[]> response = HttpResponse.ok(contents.get());
            response.header("Cache-Control", "no-cache, no-store, must-revalidate");
            response.header("Pragma", "no-cache");
            response.header("Expires", "0");
            MediaType responseContentType = contentType != null ? contentType : MediaType.forExtension("html").get();
            response.contentType(responseContentType);
            response.contentLength(contents.get().length);

            return response;
          });
        } else {
          return chain.proceed(request);
        }
      });

    }

    return super.handleAsset(filename, contentType, encoding, request, chain);
  }
}
