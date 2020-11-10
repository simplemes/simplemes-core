/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.asset

import asset.pipeline.AssetPipelineConfigHolder
import asset.pipeline.micronaut.AssetPipelineService
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Replaces
import org.simplemes.eframe.application.Holders

import javax.inject.Singleton

/**
 * A groovy implementation of the asset pipeline replacement.  This just delegates
 * to the .java class since the logic is not easily convertible to Groovy.
 * <p>
 * Micronaut does not handle beans defined in the Java tree along with the Groovy tree beans,
 * so we just make sure all beans are define in Groovy (never in Java).
 */
@Primary
@Singleton
@Replaces(AssetPipelineService)
class EFrameAssetPipelineServiceGroovy extends EFrameAssetPipelineService {
  EFrameAssetPipelineServiceGroovy() {
    super()
    // Force the .asscache file to the build/ folder in dev/test mode.
    if (Holders.environmentDev || Holders.environmentTest) {
      AssetPipelineConfigHolder.config.put("cacheLocation", "build/.asscache")
    }
  }
}
