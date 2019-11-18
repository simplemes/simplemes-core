package org.simplemes.eframe.data

import ch.qos.logback.classic.Level
import org.simplemes.eframe.custom.AdditionConfiguration
import org.simplemes.eframe.data.format.LongFieldFormat
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.UnitTestUtils

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *  Tests.
 */
class AdditionFieldDefinitionSpec extends BaseSpecification {

  def "verify that the basic constructor works with field values"() {
    given: 'an addition configuration'
    def src = """
    package sample
    
    import org.simplemes.eframe.custom.Addition
    import org.simplemes.eframe.custom.BaseAddition
    import org.simplemes.eframe.custom.AdditionConfiguration
    import org.simplemes.eframe.data.format.LongFieldFormat
    import sample.domain.SampleParent
    
    class SimpleAddition extends BaseAddition {
      AdditionConfiguration addition = Addition.configure {
        field {
          name 'custom1'
          domain SampleParent
          format LongFieldFormat
          maxLength 237
        }
      }
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)
    AdditionConfiguration addition = clazz.newInstance().addition

    when: 'the constructor is used'
    def fieldDefinition = new AdditionFieldDefinition(addition.fields[0], addition)

    then: 'the field definition is correct'
    fieldDefinition.name == 'custom1'
    fieldDefinition.format == LongFieldFormat.instance
    fieldDefinition.maxLength == 237
    fieldDefinition.label == 'custom1'

    and: 'the addition name is set'
    fieldDefinition.additionName == 'SimpleAddition'
  }

  def "verify that the basic constructor works with a field label"() {
    given: 'an addition configuration'
    def src = """
    package sample
    
    import org.simplemes.eframe.custom.Addition
    import org.simplemes.eframe.custom.BaseAddition
    import org.simplemes.eframe.custom.AdditionConfiguration
    import org.simplemes.eframe.data.format.LongFieldFormat
    import sample.domain.SampleParent
    
    class SimpleAddition extends BaseAddition {
      AdditionConfiguration addition = Addition.configure {
        field {
          name 'custom1'
          domain SampleParent
          label 'new custom1'
        }
      }
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)
    AdditionConfiguration addition = clazz.newInstance().addition

    when: 'the constructor is used'
    def fieldDefinition = new AdditionFieldDefinition(addition.fields[0], addition)

    then: 'the field definition is correct'
    fieldDefinition.label == 'new custom1'
  }

  def "verify that the guiHints are parsed correctly"() {
    given: 'an addition configuration'
    def src = """
    package sample
    
    import org.simplemes.eframe.custom.Addition
    import org.simplemes.eframe.custom.BaseAddition
    import org.simplemes.eframe.custom.AdditionConfiguration
    import org.simplemes.eframe.data.format.LongFieldFormat
    import sample.domain.SampleParent
    
    class SimpleAddition extends BaseAddition {
      AdditionConfiguration addition = Addition.configure {
        field {
          name 'custom1'
          domain SampleParent
          label 'new custom1'
          guiHints=''' required='true' ''' 
        }
      }
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)
    AdditionConfiguration addition = clazz.newInstance().addition

    when: 'the constructor is used'
    def fieldDefinition = new AdditionFieldDefinition(addition.fields[0], addition)

    then: 'the hints are correct'
    fieldDefinition.guiHints == [required: 'true']
  }

  def "verify that the guiHints parsing errors are logged"() {
    given: 'an addition configuration'
    def src = """
    package sample
    
    import org.simplemes.eframe.custom.Addition
    import org.simplemes.eframe.custom.BaseAddition
    import org.simplemes.eframe.custom.AdditionConfiguration
    import org.simplemes.eframe.data.format.LongFieldFormat
    import sample.domain.SampleParent
    
    class SimpleAddition extends BaseAddition {
      AdditionConfiguration addition = Addition.configure {
        field {
          name 'custom1'
          domain SampleParent
          guiHints=''' required=' ''' 
        }
      }
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)
    AdditionConfiguration addition = clazz.newInstance().addition

    and: 'a mock appender for Error level only'
    def mockAppender = MockAppender.mock(AdditionFieldDefinition, Level.ERROR)

    when: 'the constructor is used'
    //noinspection GroovyResultOfObjectAllocationIgnored
    new AdditionFieldDefinition(addition.fields[0], addition)

    then: 'the parsing issue is logged'
    //      log.error('Error parsing guiHints for field {}, addition {} : {}',name, additionName,ex)
    UnitTestUtils.assertContainsAllIgnoreCase(mockAppender.message, ['ERROR', 'parsing', 'guiHints',
                                                                     'custom1', 'SimpleAddition',
                                                                     'IllegalArgumentException'])
  }

}
