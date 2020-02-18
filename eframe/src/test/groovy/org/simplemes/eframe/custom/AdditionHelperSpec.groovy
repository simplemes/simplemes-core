/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom


import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.MockAdditionHelper

/**
 * Tests.
 */
class AdditionHelperSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  def "verify that getAdditions works"() {
    when: 'the additions are found in the bean context - normal runtime mode'
    def additions = new AdditionHelper().additions

    then: 'the addition contains the internal addition'
    def internal = additions.find { it.class == InternalAddition }
    internal != null
  }

  def "verify that addition can be mocked for testing with locally compiled additions"() {
    given: 'a simple mocked addition'
    def src = """
    package sample
    
    import org.simplemes.eframe.system.BasicStatus
    
    class TestAddition {
      static addedOptions = [BasicStatus:[SampleBasicStatus]]
    }
    """

    def clazz = CompilerTestUtils.compileSource(src)
    new MockAdditionHelper(this, [clazz]).install()

    expect: 'the list of additions is created'
    AdditionHelper.instance.additions.find { it.class == clazz }
  }

}
