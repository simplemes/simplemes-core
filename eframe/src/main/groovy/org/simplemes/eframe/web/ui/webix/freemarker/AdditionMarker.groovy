/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker


import org.simplemes.eframe.custom.AdditionHelper

/**
 * Provides the efAddition Freemarker marker implementation.
 * This is used to generate page content from the additions provided by modules.
 * This is used for Javascript/CSS assets.
 *
 * <h3>Attributes</h3>
 * The Attributes include:
 * <ul>
 *   <li><b>assets</b> - If true, then the assets from all active additions will be output to load the assets on the client.  </li>
 * </ul>

 */
class AdditionMarker extends BaseMarker {
  /**
   * Copy Constructor.
   */
  AdditionMarker(BaseMarker baseMarker) {
    super(baseMarker)
  }

  /**
   * Executes the directive, with the values passed by the setValues() method.
   */
  @Override
  void execute() {
    def processAssets = Boolean.valueOf(parameters.assets)
    if (processAssets) {
      for (addition in AdditionHelper.instance.additions) {
        for (asset in addition.assets) {
          if (asset.page == markerContext.view) {
            if (asset.script) {
              def assetPath = AssetMarker.getAssetPath(asset.script, this)
              write("""<script src="$assetPath" type="text/javascript"></script>\n""")
            }
            if (asset.css) {
              def assetPath = AssetMarker.getAssetPath(asset.css, this)
              write("""<link rel="stylesheet" href="$assetPath" type="text/css"/>\n""")
            }
          }
        }
      }
    }
  }

}
