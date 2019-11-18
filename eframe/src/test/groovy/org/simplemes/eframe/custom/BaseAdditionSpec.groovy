package org.simplemes.eframe.custom


import org.simplemes.eframe.data.format.BasicFieldFormat
import org.simplemes.eframe.system.BasicStatus
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.MockDomainUtils
import sample.domain.SampleChild
import sample.domain.SampleParent

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class BaseAdditionSpec extends BaseSpecification {

  /**
   * Convenience class to build an addition.
   * @param contents The contents to use in the Addition.configure DSL.
   * @return The addition instance.
   */
  AdditionInterface buildAddition(String contents) {
    def src = """
    package sample
    
    import org.simplemes.eframe.custom.Addition
    import org.simplemes.eframe.custom.BaseAddition
    import org.simplemes.eframe.custom.AdditionConfiguration
    import org.simplemes.eframe.data.format.LongFieldFormat
    import sample.domain.SampleParent
    import sample.domain.SampleChild
    import org.simplemes.eframe.EFramePackage
    import org.simplemes.eframe.data.format.BasicFieldFormat
    import org.simplemes.eframe.system.BasicStatus
    
    class SimpleAddition extends BaseAddition {
      AdditionConfiguration addition = Addition.configure {
      $contents
      }
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    return (AdditionInterface) clazz.newInstance()
  }

  def "verify that getEncodedTypes works"() {
    given: 'the DSL content for the addition'
    def content = """
      encodedType BasicStatus
      encodedType BasicFieldFormat
    """

    when: 'the configuration is generated'
    AdditionInterface addition = buildAddition(content)

    then: 'the encoded types are correct'
    addition.encodedTypes == [BasicStatus, BasicFieldFormat]
  }

  def "verify that getTopLevelDomainClasses works"() {
    given: 'the DSL content for the addition'
    def content = """
      domainPackage SampleParent
      domainPackage SampleChild
    """

    when: 'the configuration is generated'
    AdditionInterface addition = buildAddition(content)

    then: 'the values are correct'
    addition.domainPackageClasses == [SampleParent, SampleChild]
  }

  def "verify that initialDataLoaders works"() {
    given: 'the DSL content for the addition'
    def content = """
      initialDataLoader SampleParent
      initialDataLoader SampleChild
    """

    when: 'the configuration is generated'
    AdditionInterface addition = buildAddition(content)

    then: 'the encoded types are correct'
    addition.initialDataLoaders == [SampleParent, SampleChild]
  }

  def "verify that getName works"() {
    given: 'the DSL content for the addition'
    def content = """
    """

    when: 'the configuration is generated'
    AdditionInterface addition = buildAddition(content)

    then: 'the values are correct'
    addition.name == 'SimpleAddition'
  }

  def "verify that getFields works"() {
    given: 'the DSL content for the addition'
    def content = """
      field { 
        domain SampleParent
        name 'custom_counter'
      }
      field { 
        domain SampleChild
        name 'custom_child_counter'
      }
    """

    and: 'the domains are mocked'
    new MockDomainUtils(this, [SampleParent, SampleChild]).install()

    when: 'the configuration is generated'
    AdditionInterface addition = buildAddition(content)

    then: 'the values are correct'
    addition.fields.size() == 2
    addition.fields[0].name == 'custom_counter'
    addition.fields[0].domainClass == SampleParent
    addition.fields[1].name == 'custom_child_counter'
    addition.fields[1].domainClass == SampleChild
  }


}
