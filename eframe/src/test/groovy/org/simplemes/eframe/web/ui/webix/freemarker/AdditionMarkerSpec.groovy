/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import asset.pipeline.AssetPipelineConfigHolder
import asset.pipeline.micronaut.AssetPipelineService
import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.MockAdditionHelper
import org.simplemes.eframe.test.MockBean

/**
 * Tests.
 */
class AdditionMarkerSpec extends BaseMarkerSpecification {

  /**
   * True if the original asset pipeline manifest needs to be restored.
   */
  boolean needToRestoreManifest = false
  /**
   * Holds the original asset pipeline manifest since we have mocked it.
   */
  Properties originalManifest

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

  def "verify that a script asset from addition can be generated - with asset pipeline manifest"() {
    given: 'a mocked asset pipeline bean'
    new MockBean(this, AssetPipelineService).install()

    and: 'a fake manifest'
    mockManifest("mes.js", "mes-3434343.js")

    and: 'a mocked addition with an asset'
    def src = """
    package sample
    
    import org.simplemes.eframe.custom.Addition
    import org.simplemes.eframe.custom.BaseAddition
    import org.simplemes.eframe.custom.AdditionConfiguration
    import org.simplemes.eframe.data.format.LongFieldFormat
    import sample.domain.SampleParent
    import org.simplemes.eframe.data.format.BasicFieldFormat
    import org.simplemes.eframe.system.BasicStatus
    
    class SimpleAddition extends BaseAddition {
      AdditionConfiguration addition = Addition.configure {
        asset {    
          page "dashboard/index1"
          script "/assets/mes.js"
        }
      }
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)
    new MockAdditionHelper(this, [clazz]).install()

    expect: 'the asset is converted to the correct value'
    def page = execute(source: "<@efAddition assets=true/>", view: 'dashboard/index1')
    page == '<script src="/assets/mes-3434343.js" type="text/javascript"></script>\n'
  }

  def "verify that a script asset from addition can be generated - no manifest"() {
    given: 'a mocked asset pipeline bean'
    new MockBean(this, AssetPipelineService).install()

    and: 'a mocked addition with an asset'
    def src = """
    package sample
    
    import org.simplemes.eframe.custom.Addition
    import org.simplemes.eframe.custom.BaseAddition
    import org.simplemes.eframe.custom.AdditionConfiguration
    import org.simplemes.eframe.data.format.LongFieldFormat
    import sample.domain.SampleParent
    import org.simplemes.eframe.data.format.BasicFieldFormat
    import org.simplemes.eframe.system.BasicStatus
    
    class SimpleAddition extends BaseAddition {
      AdditionConfiguration addition = Addition.configure {
        asset {    
          page "dashboard/index1"
          script "/assets/mes.js"
        }
      }
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)
    new MockAdditionHelper(this, [clazz]).install()

    expect: 'the asset is converted to the correct value'
    def page = execute(source: "<@efAddition assets=true/>", view: 'dashboard/index1')
    page == '<script src="/assets/mes.js" type="text/javascript"></script>\n'
  }

  def "verify that a css asset from addition can be generated - with asset pipeline manifest"() {
    given: 'a mocked asset pipeline bean'
    new MockBean(this, AssetPipelineService).install()

    and: 'a fake manifest'
    mockManifest("mes.css", "mes-3434343.css")

    and: 'a mocked addition with an asset'
    def src = """
    package sample
    
    import org.simplemes.eframe.custom.Addition
    import org.simplemes.eframe.custom.BaseAddition
    import org.simplemes.eframe.custom.AdditionConfiguration
    import org.simplemes.eframe.data.format.LongFieldFormat
    import sample.domain.SampleParent
    import org.simplemes.eframe.data.format.BasicFieldFormat
    import org.simplemes.eframe.system.BasicStatus
    
    class SimpleAddition extends BaseAddition {
      AdditionConfiguration addition = Addition.configure {
        asset {    
          page "dashboard/index1"
          css "/assets/mes.css"
        }
      }
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)
    new MockAdditionHelper(this, [clazz]).install()

    expect: 'the asset is converted to the correct value'
    def page = execute(source: "<@efAddition assets='true'/>", view: 'dashboard/index1')
    page == '<link rel="stylesheet" href="/assets/mes-3434343.css" type="text/css"/>\n'
  }

  def "verify that a css asset from addition can be generated - no manifest"() {
    given: 'a mocked asset pipeline bean'
    new MockBean(this, AssetPipelineService).install()

    and: 'a mocked addition with an asset'
    def src = """
    package sample
    
    import org.simplemes.eframe.custom.Addition
    import org.simplemes.eframe.custom.BaseAddition
    import org.simplemes.eframe.custom.AdditionConfiguration
    import org.simplemes.eframe.data.format.LongFieldFormat
    import sample.domain.SampleParent
    import org.simplemes.eframe.data.format.BasicFieldFormat
    import org.simplemes.eframe.system.BasicStatus
    
    class SimpleAddition extends BaseAddition {
      AdditionConfiguration addition = Addition.configure {
        asset {    
          page "dashboard/index1"
          css "/assets/mes.css"
        }
      }
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)
    new MockAdditionHelper(this, [clazz]).install()

    expect: 'the asset is converted to the correct value'
    def page = execute(source: "<@efAddition assets=true/>", view: 'dashboard/index1')
    page == '<link rel="stylesheet" href="/assets/mes.css" type="text/css"/>\n'
  }

  def "verify that a an asset from addition is ignored if the page does not match"() {
    given: 'a mocked asset pipeline bean'
    new MockBean(this, AssetPipelineService).install()

    and: 'a mocked addition with an asset'
    def src = """
    package sample
    
    import org.simplemes.eframe.custom.Addition
    import org.simplemes.eframe.custom.BaseAddition
    import org.simplemes.eframe.custom.AdditionConfiguration
    import org.simplemes.eframe.data.format.LongFieldFormat
    import sample.domain.SampleParent
    import org.simplemes.eframe.data.format.BasicFieldFormat
    import org.simplemes.eframe.system.BasicStatus
    
    class SimpleAddition extends BaseAddition {
      AdditionConfiguration addition = Addition.configure {
        asset {    
          page "dashboard/index1"
          css "/assets/mes.css"
        }
      }
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)
    new MockAdditionHelper(this, [clazz]).install()

    expect: 'the asset is converted to the correct value'
    def page = execute(source: "<@efAddition assets=true/>", view: 'wrong/index1')
    page == ''
  }

}
