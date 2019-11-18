package org.simplemes.eframe.web.ui.webix.freemarker

import asset.pipeline.AssetPipelineConfigHolder
import asset.pipeline.micronaut.AssetPipelineService
import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.MockBean
import org.simplemes.eframe.test.UnitTestUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class AssetMarkerSpec extends BaseMarkerSpecification {

  /**
   * True if the original asset pipeline manifest needs to be restored.
   */
  boolean needToRestoreManifest = false
  /**
   * Holds the original asset pipeline manifest since we have mocked it.
   */
  Properties originalManifest

  // can be called multiple times
  /**
   * Mocks a manifest for the Asset Pipeline.  Creates a dummy manifest and
   * adds the given file/value pair to the manifest.
   * Can be called multiple times.
   * @param fileName The file to create the manifest entry for.
   * @param manifestValue The manifest value.
   */
  void mockManifest(String fileName, String manifestValue) {
    if (!needToRestoreManifest) {
      needToRestoreManifest = true
      originalManifest = AssetPipelineConfigHolder.manifest
      AssetPipelineConfigHolder.manifest = new Properties()
    }
    AssetPipelineConfigHolder.manifest.put(fileName, manifestValue)
  }


  void cleanup() {
    if (needToRestoreManifest) {
      needToRestoreManifest = false
      AssetPipelineConfigHolder.manifest = originalManifest
    }
  }

  def "verify that the asset marker finds the .jar version in the manifest"() {
    given: 'a mocked asset pipeline bean'
    new MockBean(this, AssetPipelineService).install()

    and: 'a fake manifest'
    mockManifest("webix.js", "webix-3434343.js")

    expect: 'the asset is converted to the correct value'
    execute(source: "<@efAsset uri='$fileName'/>") == results

    where:
    fileName               | results
    "/assets/webix.js"     | "/assets/webix-3434343.js"
    "/assets/webix.not.js" | "/assets/webix.not.js"
  }

  def "verify that the asset marker handles null uri gracefully"() {
    given: 'a mocked asset pipeline bean'
    new MockBean(this, AssetPipelineService).install()

    and: 'a fake manifest'
    mockManifest("webix.js", "webix-3434343.js")

    when: 'the asset is converted to the correct value'
    execute(source: source)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['uri', 'efAsset'])

    where:
    source               | _
    "<@efAsset uri=''/>" | _
    "<@efAsset/>"        | _
  }

}
