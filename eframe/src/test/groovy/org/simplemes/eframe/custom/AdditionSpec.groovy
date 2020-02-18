/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom

import ch.qos.logback.classic.Level
import org.simplemes.eframe.data.format.BasicFieldFormat
import org.simplemes.eframe.data.format.LongFieldFormat
import org.simplemes.eframe.system.BasicStatus
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.MockDomainUtils
import org.simplemes.eframe.test.UnitTestUtils
import sample.domain.Order
import sample.domain.SampleChild
import sample.domain.SampleParent

/**
 * Tests.
 */
class AdditionSpec extends BaseSpecification {

  def setup() {
    new MockDomainUtils(this, [SampleParent, SampleChild]).install()
  }

  def "verify that field extension DSL works - top-level elements"() {
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
        encodedType BasicStatus
        encodedType BasicFieldFormat
        initialDataLoader SampleParent
      }
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the configuration is generated'
    AdditionConfiguration addition = clazz.newInstance().addition

    then: 'the top-level values are correct'
    addition.encodedTypes == [BasicStatus, BasicFieldFormat]
    addition.initialDataLoaders == [SampleParent]
    addition.name == 'SimpleAddition'

    and: 'the config passes validation'
    addition.validate()

    then: 'the configure closure can be used in a read program'
    def x = Addition.configure {
      encodedType BasicStatus
      initialDataLoader SampleParent
      field {
        domain SampleParent
        name 'count'
        format LongFieldFormat
        maxLength 23
        valueClass Order
        fieldOrder { name 'group:components'; after '' }
        fieldOrder { name 'components'; after 'group:components' }
        guiHints """componentsInlineGrid="true" """
      }
    }
    x.validate()
  }

  def "verify that field extension DSL works - field options"() {
    def src = """
    package sample
    
    import org.simplemes.eframe.custom.Addition
    import org.simplemes.eframe.custom.BaseAddition
    import org.simplemes.eframe.custom.AdditionConfiguration
    import org.simplemes.eframe.data.format.LongFieldFormat
    import sample.domain.SampleParent
    import sample.domain.SampleChild
    import org.simplemes.eframe.data.format.BasicFieldFormat
    import org.simplemes.eframe.system.BasicStatus
    
    class SimpleAddition extends BaseAddition {
      AdditionConfiguration addition = Addition.configure {
        field {
          domain SampleParent
          name 'code'
          label 'codeLabel'
          format LongFieldFormat
          valueClass String
          guiHints '''inlineGrid="true"
                      sequenceDefault="toolkit.findMaxGridValue(gridName, 'sequence')+10"/>'''
        }
        field {domain SampleChild; name 'count'}
      }
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the configuration is generated'
    AdditionConfiguration addition = clazz.newInstance().addition

    then: 'the field-level values are correct'
    addition.fields.size() == 2
    def field0 = addition.fields[0]
    field0.domainClass == SampleParent
    field0.name == 'code'
    field0.label == 'codeLabel'
    field0.format == LongFieldFormat.instance
    field0.valueClass == String
    field0.guiHints.contains('inlineGrid="true"')

    def field1 = addition.fields[1]
    field1.name == 'count'
    field1.domainClass == SampleChild

    and: 'the config passes validation'
    addition.validate()
  }

  def "verify that field extension DSL works - field order options"() {
    def src = """
    package sample
    
    import org.simplemes.eframe.custom.Addition
    import org.simplemes.eframe.custom.BaseAddition
    import org.simplemes.eframe.custom.AdditionConfiguration
    import org.simplemes.eframe.data.format.LongFieldFormat
    import sample.domain.SampleParent
    import sample.domain.SampleChild
    import org.simplemes.eframe.data.format.BasicFieldFormat
    import org.simplemes.eframe.system.BasicStatus
    
    class SimpleAddition extends BaseAddition {
      AdditionConfiguration addition = Addition.configure {
        field {
          domain SampleParent
          name 'code'
          fieldOrder { name 'group:abc' }
          fieldOrder { name 'priority'; after 'group:abc' }
        }
      }
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the configuration is generated'
    AdditionConfiguration addition = clazz.newInstance().addition

    then: 'the field order values are correct'
    addition.fields.size() == 1
    def field0 = addition.fields[0]
    field0.fieldOrderAdjustments.size() == 2
    field0.fieldOrderAdjustments[0].fieldName == 'group:abc'
    !field0.fieldOrderAdjustments[0].afterFieldName
    field0.fieldOrderAdjustments[1].fieldName == 'priority'
    field0.fieldOrderAdjustments[1].afterFieldName == 'group:abc'

    and: 'the config passes validation'
    addition.validate()
  }

  def "verify that field extension DSL detects invalid values"() {
    given: 'an addition class'
    def src = """
    package sample
    
    import org.simplemes.eframe.custom.Addition
    import org.simplemes.eframe.custom.BaseAddition
    import org.simplemes.eframe.custom.AdditionConfiguration
    import org.simplemes.eframe.data.format.LongFieldFormat
    import sample.domain.SampleParent
    import sample.domain.SampleChild
    import org.simplemes.eframe.domain.DomainUtils
    import org.simplemes.eframe.data.format.BasicFieldFormat
    import org.simplemes.eframe.system.BasicStatus
    
    class SimpleAddition extends BaseAddition {
      AdditionConfiguration addition = Addition.configure {
        $input
      }
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    and: 'a mock appender for Error level only'
    def mockAppender = MockAppender.mock(AdditionConfiguration, Level.ERROR)

    when: 'the configuration is validated'
    AdditionConfiguration addition = clazz.newInstance().addition

    then: 'the validation error is logged correctly'
    UnitTestUtils.assertContainsAllIgnoreCase(mockAppender.message, logContains)

    and: 'the message has the class name in the message'
    mockAppender.message.contains('SimpleAddition')

    and: 'the validate method returns false'
    !addition.validate()

    where:
    input                                                                | logContains
    'encodedType String'                                                 | ['encodedType', 'String', 'EncodedTypeInterface']
    'initialDataLoader String'                                           | ['initialDataLoader', 'String', 'method', 'initialDataLoad()']
    'field {domain SampleParent}'                                        | ['field', 'name']
    'field {name "abc"; domain SampleParent; fieldOrder {after "xyz"} }' | ['field', 'abc', 'name']
  }

  // test validations.  When enforced?
  /*
    fieldConfig has correct types: domainClass name format maxLength valueClass guiHints
   */
}
