package org.simplemes.eframe.web.ui.webix.freemarker

import asset.pipeline.AssetPipelineConfigHolder
import asset.pipeline.micronaut.AssetPipelineService
import org.simplemes.eframe.application.Holders

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Provides the efAsset Freemarker marker implementation.
 * This converts a generic asset URI (e.g. '/assets/eframe.min.js') to a digest form that
 * can be cached easily by the browser.
 */
class AssetMarker extends BaseMarker {
  /**
   * Indicates if the manifest was loaded into the AssetPipelineConfigHolder.
   */
  private boolean manifestLoaded = false

  /**
   * Copy Constructor.
   */
  AssetMarker(BaseMarker baseMarker) {
    super(baseMarker)
  }

  /**
   * Executes the directive, with the values passed by the setValues() method.
   */
  @Override
  void execute() {
    write(adjustAssetPath((String) unwrap(parameters.uri)))
  }

  /**
   * Make sure the manifest is loaded into the AssetPipelineConfigHolder.
   */
  void loadManifest() {
    if (!manifestLoaded) {
      // Force the manifest to be loaded.  This is done by the AssetPipelineService constructor, we just make
      // sure the service is loaded.
      Holders.applicationContext.getBean(AssetPipelineService)
      manifestLoaded = true
    }
  }

  /**
   * Allows access to the asset logic for other markers to use.  Returns the corrected asset path
   * for the given input path for use by the given marker.
   * @param path The basic path.
   * @param marker The marker that needs the real path.
   * @return The real path.
   */
  String adjustAssetPath(String path) {
    loadManifest()
    String uri = path
    if (!path) {
      throw new MarkerException("efAsset must have a uri.", this)
    }

    // Hard-code for now, since the AssetPipelineFilter does not expose it or set it in the AssetPipelineConfigHolder.
    def baseAssetUrl = "/assets"

    if (uri.startsWith(baseAssetUrl)) {
      uri = uri[baseAssetUrl.length()..-1]
      if (uri.startsWith('/')) {
        uri = uri[1..-1]
      }
      def manifest = AssetPipelineConfigHolder.manifest
      if (manifest) {
        String manifestURI = manifest[uri]
        if (manifestURI) {
          return baseAssetUrl + '/' + manifestURI
        }
      }
    }
    return path ?: ""

  }


  /**
   * Allows access to the asset logic for other markers to use.  Returns the corrected asset path
   * for the given input path for use by the given marker.
   * @param path The basic path.
   * @param marker The marker that needs the real path.
   * @return The real path.
   */
  static CharSequence getAssetPath(String path, BaseMarker marker) {
    def assetMarker = new AssetMarker(marker)
    return assetMarker.adjustAssetPath(path)
  }
}
