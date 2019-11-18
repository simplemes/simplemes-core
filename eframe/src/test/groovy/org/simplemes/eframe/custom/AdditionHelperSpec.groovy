package org.simplemes.eframe.custom

import ch.qos.logback.classic.Level
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.MockAdditionHelper
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.UnitTestUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class AdditionHelperSpec extends BaseSpecification {

  def "verify that getAdditions works"() {
    when: 'the additions are read from the efBootstrap.yml file'
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

  def "verify that getAdditions gracefully fails when the addition class is not found"() {
    given: 'a simulated file with a bad class name'
    // Note: Do not change the formatting this .yml file.  It is needed for the .yml format.
    def s = """---\neframe:\n  additions: [org.Gibberish]\n"""
    def inputStream = new ByteArrayInputStream(s.bytes)

    and: 'a mock appender for Info level only'
    def mockAppender = MockAppender.mock(AdditionHelper, Level.ERROR)

    when: 'the additions are read with a bad file'
    def additions = new AdditionHelper().getAdditions(inputStream, 'SourceGibberish')

    then: 'there are no additions found'
    !additions

    then: 'the right log message is written'
    UnitTestUtils.assertContainsAllIgnoreCase(mockAppender.message, ['SourceGibberish', 'org.Gibberish'])
  }

}
