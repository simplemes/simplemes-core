/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom

import ch.qos.logback.classic.Level
import org.simplemes.eframe.custom.domain.FieldExtension
import org.simplemes.eframe.custom.domain.FieldGUIExtension
import org.simplemes.eframe.custom.domain.FlexField
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.custom.gui.FieldInsertAdjustment
import org.simplemes.eframe.data.format.BigDecimalFieldFormat
import org.simplemes.eframe.data.format.BooleanFieldFormat
import org.simplemes.eframe.data.format.DateFieldFormat
import org.simplemes.eframe.data.format.DateOnlyFieldFormat
import org.simplemes.eframe.data.format.DomainReferenceFieldFormat
import org.simplemes.eframe.data.format.EncodedTypeFieldFormat
import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.data.format.IntegerFieldFormat
import org.simplemes.eframe.data.format.LongFieldFormat
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.domain.annotation.DomainEntityInterface
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.system.BasicStatus
import org.simplemes.eframe.system.DisabledStatus
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.MockAdditionHelper
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.eframe.web.report.ReportTimeIntervalEnum
import sample.domain.AllFieldsDomain
import sample.domain.CustomOrderComponent
import sample.domain.Order
import sample.domain.OrderLine
import sample.domain.RMA
import sample.domain.SampleParent
import sample.pogo.SamplePOGO

/**
 * Tests.
 */
class ExtensibleFieldHelperSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [CustomOrderComponent, OrderLine, Order, FieldGUIExtension, FieldExtension, FlexType, RMA]

  @Rollback
  def "verify that getEffectiveFieldDefinitions finds custom fields"() {
    given: 'a custom field on a domain'
    new FieldExtension(fieldName: 'abc', domainClassName: SampleParent.name, fieldFormat: DateFieldFormat.instance).save()

    when: 'the effective fields are found'
    def fieldDefs = ExtensibleFieldHelper.instance.getEffectiveFieldDefinitions(SampleParent)

    then: 'the custom field is in the list'
    fieldDefs['abc'].type == Date

    and: 'the core fields are in the list too'
    fieldDefs['name'].type == String
  }

  @Rollback
  def "verify that getEffectiveFieldDefinitions logs the fields added - trace logging"() {
    given: 'a custom field on a domain'
    new FieldExtension(fieldName: 'xyzzy', domainClassName: SampleParent.name, fieldFormat: DateFieldFormat.instance).save()

    and: 'a mock appender for trace level only'
    def mockAppender = MockAppender.mock(ExtensibleFieldHelper, Level.DEBUG)

    when: 'the effective fields are found'
    ExtensibleFieldHelper.instance.getEffectiveFieldDefinitions(SampleParent)

    then: 'the log message is written'
    UnitTestUtils.assertContainsAllIgnoreCase(mockAppender.message, ['DEBUG', 'add', 'xyzzy', SampleParent.name])
  }

  @Rollback
  def "verify that getEffectiveFieldDefinitions finds fields added by additions"() {
    given: 'a simple mocked addition with a custom field'
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
        field {
          domain SampleParent
          name 'custom1'
          format LongFieldFormat
        }
      }
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)
    new MockAdditionHelper(this, [clazz]).install()

    when: 'the effective fields are found'
    def fieldDefs = ExtensibleFieldHelper.instance.getEffectiveFieldDefinitions(SampleParent)

    then: 'the custom field is in the list'
    fieldDefs['custom1'].type == Long

    and: 'the core fields are in the list too'
    fieldDefs['name'].type == String
  }

  @Rollback
  def "verify that getEffectiveFieldOrder finds custom fields GUI definitions"() {
    given: 'a custom field GUI extension on a domain'
    def originalFieldOrder = DomainUtils.instance.getStaticFieldOrder(SampleParent)
    def adj1 = new FieldInsertAdjustment(fieldName: 'custom1', afterFieldName: 'title')
    def extension = new FieldGUIExtension(domainName: SampleParent.name, adjustments: [adj1])
    extension.save()

    when: 'the effective field order is found'
    def fieldOrder = ExtensibleFieldHelper.instance.getEffectiveFieldOrder(SampleParent)

    then: 'the custom field is in the list in the right place'
    fieldOrder.size() == originalFieldOrder.size() + 1
    (fieldOrder.indexOf('title') + 1) == fieldOrder.indexOf('custom1')

    and: 'the original list is not modified'
    originalFieldOrder == DomainUtils.instance.getStaticFieldOrder(SampleParent)
  }

  @Rollback
  def "verify that getEffectiveFieldOrder supports optional fieldOrder passed in"() {
    given: 'a custom field GUI extension on a domain'
    def originalFieldOrder = ['name', 'title']
    def adj1 = new FieldInsertAdjustment(fieldName: 'custom1', afterFieldName: 'title')
    def extension = new FieldGUIExtension(domainName: SampleParent.name, adjustments: [adj1])
    extension.save()

    when: 'the effective field order is found'
    def fieldOrder = ExtensibleFieldHelper.instance.getEffectiveFieldOrder(SampleParent, originalFieldOrder)

    then: 'the custom field is in the list in the right place'
    fieldOrder.size() == originalFieldOrder.size() + 1
    (fieldOrder.indexOf('title') + 1) == fieldOrder.indexOf('custom1')

    and: 'the original list is not modified'
    originalFieldOrder.size() == 2
  }

  def "verify that getEffectiveFieldOrder works outside of a database transaction"() {
    when: 'the effective field order is found'
    ExtensibleFieldHelper.instance.getEffectiveFieldOrder(SampleParent)

    then: 'no exception is thrown'
    notThrown(Exception)
  }

  @Rollback
  def "verify that getEffectiveFieldOrder finds field order changes made by additions"() {
    given: 'a simple mocked addition with a custom field'
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
        field {
          domain SampleParent
          name 'custom1'
          fieldOrder { name 'custom1'; after 'title' }
        }
      }
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)
    new MockAdditionHelper(this, [clazz]).install()

    when: 'the effective fields are found'
    def fieldOrder = ExtensibleFieldHelper.instance.getEffectiveFieldOrder(SampleParent)

    then: 'the custom field is in the fieldOrder list in the right place'
    fieldOrder.indexOf('title') < fieldOrder.indexOf('custom1')
  }


  def "verify that set and getFieldValue handles simple cases"() {
    given: 'a domain object with a custom field'
    def src = """
      import org.simplemes.eframe.data.annotation.*
      import org.simplemes.eframe.domain.annotation.DomainEntity
      @DomainEntity
      class TestClass {
        @ExtensibleFieldHolder String customFields
        UUID uuid 
      }
    """
    def object = CompilerTestUtils.compileSource(src).newInstance()

    when: 'the value is set'
    ExtensibleFieldHelper.instance.setFieldValue(object, 'field1', 'ABC')

    then: 'the value can be read'
    ExtensibleFieldHelper.instance.getFieldValue(object as DomainEntityInterface, 'field1') == 'ABC'
  }

  def "verify that set and getFieldValue handles supported field types"() {
    given: 'a domain object with a custom field'
    def src = """
      import org.simplemes.eframe.data.annotation.*
      import org.simplemes.eframe.domain.annotation.DomainEntity
      @DomainEntity
      class TestClass {
        @ExtensibleFieldHolder String customFields
        UUID uuid 
      }
    """
    def object = CompilerTestUtils.compileSource(src).newInstance()

    and: 'a field extension'
    FieldExtension.withTransaction {
      new FieldExtension(fieldName: 'field1', domainClassName: 'TestClass',
                         fieldFormat: format.instance, valueClassName: valueClass?.name).save()

    }

    expect: 'the value is set and get works'
    ExtensibleFieldHelper.instance.setFieldValue(object, 'field1', value)
    ExtensibleFieldHelper.instance.getFieldValue(object, 'field1') == value

    where:
    value                              | format                 | valueClass
    'ABC'                              | StringFieldFormat      | null
    'A"BC'                             | StringFieldFormat      | null
    "A'BC"                             | StringFieldFormat      | null
    true                               | BooleanFieldFormat     | null
    false                              | BooleanFieldFormat     | null
    237                                | IntegerFieldFormat     | null
    23527L                             | LongFieldFormat        | null
    1.2                                | BigDecimalFieldFormat  | null
    new Date()                         | DateFieldFormat        | null
    new DateOnly()                     | DateOnlyFieldFormat    | null
    DisabledStatus.instance            | EncodedTypeFieldFormat | BasicStatus
    ReportTimeIntervalEnum.LAST_7_DAYS | EnumFieldFormat        | ReportTimeIntervalEnum
  }

  @Rollback
  def "verify that set and getFieldValue handles domain reference"() {
    given: 'a domain object with a custom field'
    def object = new SampleParent()

    and: 'a domain record'
    def (AllFieldsDomain allFieldsDomain) = DataGenerator.generate {
      domain AllFieldsDomain
    }

    and: 'a field extension'
    FieldExtension.withTransaction {
      new FieldExtension(fieldName: 'field1', domainClassName: SampleParent.name,
                         fieldFormat: DomainReferenceFieldFormat.instance,
                         valueClassName: AllFieldsDomain.name).save()
    }

    expect: 'the value is set and get works'
    ExtensibleFieldHelper.instance.setFieldValue(object, 'field1', allFieldsDomain)
    ExtensibleFieldHelper.instance.getFieldValue(object, 'field1') == allFieldsDomain
  }

  @Rollback
  def "verify that setFieldValue logs the value"() {
    given: 'a domain object with a custom field'
    def object = new SampleParent()

    and: 'a field extension'
    FieldExtension.withTransaction {
      new FieldExtension(fieldName: 'field1', domainClassName: SampleParent.name).save()
    }

    and: 'a mock appender for trace level only'
    def mockAppender = MockAppender.mock(ExtensibleFieldHelper, Level.TRACE)

    when: 'the value is set'
    ExtensibleFieldHelper.instance.setFieldValue(object, 'field1', 'XYZZY')

    then: 'the log message is written'
    //      log.trace('setFieldValue(): {} value = {} to object {}',fieldName, value, object)
    UnitTestUtils.assertContainsAllIgnoreCase(mockAppender.message, ['TRACE', 'setFieldValue', 'XYZZY', 'field1', object.toString()])
  }

  @Rollback
  def "verify that getFieldValue logs the value"() {
    given: 'a domain object with a custom field'
    def object = new SampleParent()

    and: 'a field extension'
    FieldExtension.withTransaction {
      new FieldExtension(fieldName: 'field1', domainClassName: SampleParent.name).save()
    }

    when: 'the value is set'
    ExtensibleFieldHelper.instance.setFieldValue(object, 'field1', 'XYZZY')

    and: 'a mock appender for trace level only'
    def mockAppender = MockAppender.mock(ExtensibleFieldHelper, Level.TRACE)
    ExtensibleFieldHelper.instance.getFieldValue(object, 'field1')

    then: 'the log message is written'
    //log.trace('getFieldValue(): {} value = {} from object {}',fieldName, value, object)
    UnitTestUtils.assertContainsAllIgnoreCase(mockAppender.message, ['TRACE', 'getFieldValue', 'XYZZY', 'field1', object.toString()])
  }

  @Rollback
  def "verify that set and getFieldValue supports a prefix for the field name"() {
    given: 'a domain object with a custom field'
    def object = new SampleParent()

    and: 'a field extension'
    FieldExtension.withTransaction {
      new FieldExtension(fieldName: 'field1', domainClassName: SampleParent.name,
                         fieldFormat: StringFieldFormat.instance).save()
    }

    expect: 'the value is set and get works'
    ExtensibleFieldHelper.instance.setFieldValue(object, 'dummyType_field1', 'XYZZY')
    ExtensibleFieldHelper.instance.getFieldValue(object, 'dummyType_field1') == 'XYZZY'

    and: 'the field is stored with the prefix in the JSON'
    object[ExtensibleFieldHelper.instance.getCustomHolderFieldName(SampleParent)].contains('"dummyType_field1"')

    and: 'getFieldValue with no prefix does not find the prefixed field'
    ExtensibleFieldHelper.instance.getFieldValue(object, 'field1') == null
  }

  def "verify that set and getFieldValue round-trip performance is acceptable"() {
    given: 'a domain object with a custom field'
    def src = """
      import org.simplemes.eframe.data.annotation.*
      import javax.persistence.Column
      import org.simplemes.eframe.domain.annotation.DomainEntity
      @DomainEntity
      class TestClass {
        @ExtensibleFieldHolder 
        @Column(nullable = true, length = 5000)
        String customFields
        UUID uuid
      }
    """
    def object = (DomainEntityInterface) CompilerTestUtils.compileSource(src).newInstance()
    def nFields = 100
    def nIterations = 1000

    and: 'a number of custom fields are added to the holder'
    for (i in 1..nFields) {
      ExtensibleFieldHelper.instance.setFieldValue(object, "field$i", '1234567890')
    }

    when: 'the value is set'
    def start = System.currentTimeMillis()
    for (i in 1..nIterations) {
      ExtensibleFieldHelper.instance.setFieldValue(object, "field50", "ABC_XYZ")
      ExtensibleFieldHelper.instance.getFieldValue(object, "field50") == 'ABC_XYZ'
    }

    then: 'the value can be read'
    def elapsed = System.currentTimeMillis() - start
    def rate = elapsed / nIterations
    rate < 2.5  // Real value is 0.05 ms/iteration. 0.5 works fine on desktop.  1.6 on S server.
  }

  def "verify that setFieldValue fails when there is not enough size - Default Size case"() {
    given: 'a domain object with a custom field'
    def src = """
      import org.simplemes.eframe.data.annotation.*
      import javax.persistence.Column
      import org.simplemes.eframe.domain.annotation.DomainEntity
      @DomainEntity
      class TestClass {
        @ExtensibleFieldHolder 
        String customFields
        UUID uuid
      }
    """
    def object = CompilerTestUtils.compileSource(src).newInstance()
    def nFields = 100

    when: 'too much is stored'
    for (i in 1..nFields) {
      ExtensibleFieldHelper.instance.setFieldValue(object, "field$i", '1234567890')
    }

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['TestClass', 'field', TypeUtils.toShortString(object)])
  }

  def "verify that setFieldValue fails when there is not enough size - custom Size case"() {
    given: 'a domain object with a custom field'
    def src = """
      import org.simplemes.eframe.data.annotation.*
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import javax.persistence.Column
      @DomainEntity
      class TestClass {
        @ExtensibleFieldHolder 
        @Column(nullable = true, length = 437)
        String customFields
        UUID uuid
      }
    """
    def object = CompilerTestUtils.compileSource(src).newInstance()
    def nFields = 100

    when: 'too much is stored'
    for (i in 1..nFields) {
      ExtensibleFieldHelper.instance.setFieldValue(object, "field$i", '1234567890')
    }

    then: 'the right exception is thrown'
    def ex = thrown(BusinessException)
    ex.code == 130
    UnitTestUtils.assertExceptionIsValid(ex, ['TestClass', 'field', "437"])
  }

  def "verify that setFieldValue fails when the annotation is not used on the domain"() {
    given: 'a domain object without the annotation'
    def src = """
      class TestClass {
      }
    """
    def object = CompilerTestUtils.compileSource(src).newInstance()

    when: 'a custom value is stored'
    ExtensibleFieldHelper.instance.setFieldValue(object, "field1", '1234567890')

    then: 'the right exception is thrown'
    def ex = thrown(BusinessException)
    ex.code == 131
    UnitTestUtils.assertExceptionIsValid(ex, ['TestClass'])
  }

  def "verify that getFieldValue fails when the annotation is not used on the domain"() {
    given: 'a domain object without the annotation'
    def src = """
      class TestClass {
      }
    """
    def object = CompilerTestUtils.compileSource(src).newInstance()

    when: 'a custom value is stored'
    ExtensibleFieldHelper.instance.getFieldValue(object, "field1")

    then: 'the right exception is thrown'
    def ex = thrown(BusinessException)
    ex.code == 131
    UnitTestUtils.assertExceptionIsValid(ex, ['TestClass'])
  }

  def "verify that set and getFieldValue handles non-standard field name case"() {
    given: 'a domain object with a custom field'
    def src = """
      import org.simplemes.eframe.data.annotation.*
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import javax.persistence.Column
      @DomainEntity
      class TestClass {
        @ExtensibleFieldHolder 
        String testCustom
        UUID uuid
      }
    """
    def object = CompilerTestUtils.compileSource(src).newInstance()

    when: 'the value is set'
    ExtensibleFieldHelper.instance.setFieldValue(object, 'field1', 'ABC')

    then: 'the value can be read'
    ExtensibleFieldHelper.instance.getFieldValue(object, 'field1') == 'ABC'

    and: 'it is stored in the right custom field'
    object.testCustom.contains('ABC')
  }

  @Rollback
  def "verify that set and getFieldValue handles prefix option"() {
    given: 'a flex type with a field'
    def flexType = DataGenerator.buildFlexType()

    when: 'the value is set'
    def rma = new RMA(rma: 'ABC')
    rma.rmaType = flexType
    rma.setRmaTypeValue('FIELD1', 'XYZ')

    then: 'the field can be retrieved'
    rma.getRmaTypeValue('FIELD1') == 'XYZ'

    and: 'the value is stored correctly in the custom fields'
    rma.getRmaTypeValue('FIELD1') == 'XYZ'
  }

  @Rollback
  def "verify that getFieldValue handles custom child list scenario"() {
    given: 'a parent record'
    def (Order order) = DataGenerator.generate {
      domain Order
    }

    and: 'the child custom records'
    DataGenerator.generate {
      domain CustomOrderComponent
      values order: order, sequence: 0, product: 'PRODUCT-$i', qty: 1.2
    }

    when: 'the value is read'
    def order2 = Order.findByUuid(order.uuid)
    List list = (List) ExtensibleFieldHelper.instance.getFieldValue(order2, 'customComponents')

    then: 'the value is correct'
    list.size() == 1
    list[0].product == 'PRODUCT-001'
  }

  @Rollback
  def "verify that setFieldValue will update the custom child records when the parent is saved"() {
    when: 'the parent record and custom child records are saved'
    def order = new Order(order: 'M1001')
    def customComponents = []
    customComponents << new CustomOrderComponent(sequence: 1, product: 'PROD1')
    customComponents << new CustomOrderComponent(sequence: 2, product: 'PROD2')
    customComponents << new CustomOrderComponent(sequence: 3, product: 'PROD3')
    order.setFieldValue('customComponents', customComponents)
    order.save()

    and: 'the top-level domain is re-read'
    order = Order.findByOrder('M1001')
    List<CustomOrderComponent> list = (List) ExtensibleFieldHelper.instance.getFieldValue(order, 'customComponents')

    then: 'the values are correct'
    list.size() == 3
    list[0].sequence == 1
    list[0].product == 'PROD1'
    list[1].sequence == 2
    list[1].product == 'PROD2'
    list[2].sequence == 3
    list[2].product == 'PROD3'

    and: 'the records are in the database'
    def dbList = CustomOrderComponent.findAllByOrder(order)
    dbList.size() == 3
    dbList[0].sequence == 1
    dbList[0].product == 'PROD1'
    dbList[1].sequence == 2
    dbList[1].product == 'PROD2'
    dbList[2].sequence == 3
    dbList[2].product == 'PROD3'
  }

  @Rollback
  def "verify that updating the domain will update and add custom child records when the parent is saved"() {
    given: 'the saved parent record and custom child records'
    def order = new Order(order: 'M1001')
    def customComponents = []
    customComponents << new CustomOrderComponent(sequence: 1, product: 'PROD1')
    customComponents << new CustomOrderComponent(sequence: 2, product: 'PROD2')
    customComponents << new CustomOrderComponent(sequence: 3, product: 'PROD3')
    order.setFieldValue('customComponents', customComponents)
    order.save()

    when: 'the parent record and custom child records are updated'
    order = Order.findByOrder('M1001')
    customComponents = (List) order.getFieldValue('customComponents')
    customComponents[0].product = 'PROD1A'
    customComponents[1].product = 'PROD2A'
    customComponents[2].product = 'PROD3A'
    customComponents << new CustomOrderComponent(sequence: 4, product: 'PROD4')
    order.save()

    and: 'the domain is re-read is re-read'
    order = Order.findByOrder('M1001')
    List list = (List) ExtensibleFieldHelper.instance.getFieldValue(order, 'customComponents')

    then: 'the values are correct'
    list.size() == 4
    list[0].sequence == 1
    list[0].product == 'PROD1A'
    list[1].sequence == 2
    list[1].product == 'PROD2A'
    list[2].sequence == 3
    list[2].product == 'PROD3A'
    list[3].sequence == 4
    list[3].product == 'PROD4'

    and: 'the records are in the database'
    def dbList = CustomOrderComponent.findAllByOrder(order)
    dbList.size() == 4
    dbList[0].sequence == 1
    dbList[0].product == 'PROD1A'
    dbList[1].sequence == 2
    dbList[1].product == 'PROD2A'
    dbList[2].sequence == 3
    dbList[2].product == 'PROD3A'
    dbList[3].sequence == 4
    dbList[3].product == 'PROD4'
  }

  def "verify that updating the domain will remove custom child records when the parent is saved and transactions are used"() {
    given: 'the saved parent record and custom child records'
    def order = null
    Order.withTransaction {
      order = new Order(order: 'M1001')
      def customComponents = []
      customComponents << new CustomOrderComponent(sequence: 1, product: 'PROD1')
      customComponents << new CustomOrderComponent(sequence: 2, product: 'PROD2')
      customComponents << new CustomOrderComponent(sequence: 3, product: 'PROD3')
      order.setFieldValue('customComponents', customComponents)
      order.save()
    }

    when: 'the parent record is changed and the custom child record is removed and one is added'
    Order.withTransaction {
      order = Order.findByOrder('M1001')
      def orderComponents = (List<CustomOrderComponent>) order.getFieldValue('customComponents')
      orderComponents[0].product = 'PROD1A'
      orderComponents.remove(1)
      orderComponents << new CustomOrderComponent(sequence: 4, product: 'PROD4')
      order.save()
    }

    and: 'the order line list is read'
    List list = null
    Order.withTransaction {
      order = Order.findByOrder('M1001')
      list = (List) ExtensibleFieldHelper.instance.getFieldValue(order, 'customComponents')
    }

    then: 'the values are correct'
    list.size() == 3
    list[0].sequence == 1
    list[0].product == 'PROD1A'
    list[1].sequence == 3
    list[1].product == 'PROD3'
    list[2].sequence == 4
    list[2].product == 'PROD4'

    and: 'the records are in the database'
    OrderLine.withTransaction {
      def customOrderComponents = CustomOrderComponent.findAllByOrder(order)
      assert customOrderComponents.size() == 3
      assert customOrderComponents[0].sequence == 1
      assert customOrderComponents[0].product == 'PROD1A'
      assert customOrderComponents[1].sequence == 3
      assert customOrderComponents[1].product == 'PROD3'
      assert customOrderComponents[2].sequence == 4
      assert customOrderComponents[2].product == 'PROD4'
      true
    }
  }

  @Rollback
  def "verify that addConfigurableTypeFields adds the configurable fields to the field definitions"() {
    given: 'a flex type with a field'
    def flexType = new FlexType(flexType: 'XYZ')
    flexType.fields << new FlexField(flexType: flexType, fieldName: 'Field1')
    flexType.save()

    and: 'a domain with a configurable type'
    def rma = new RMA(rma: 'ABC')
    rma.rmaType = flexType
    rma.save()

    when: 'the field definitions are added'
    def originalFD = ExtensibleFieldHelper.instance.getEffectiveFieldDefinitions(RMA)
    def newFD = ExtensibleFieldHelper.instance.addConfigurableTypeFields(originalFD, rma)

    then: 'the new list is different from the original'
    originalFD.size() != newFD.size()

    and: 'the flex field is added to the field defs'
    def fieldDefinition = newFD.rmaType_Field1
    fieldDefinition
    fieldDefinition instanceof ConfigurableTypeFieldDefinition
  }

  @Rollback
  def "verify that getFieldValue supports the configurable field elements"() {
    given: 'a flex type with a field'
    def flexType = new FlexType(flexType: 'XYZ')
    flexType.fields << new FlexField(flexType: flexType, fieldName: 'Field1',
                                     fieldFormat: DateOnlyFieldFormat.instance)
    flexType.save()

    and: 'a domain with a configurable type'
    def rma = new RMA(rma: 'ABC')
    rma.rmaType = flexType
    rma.setRmaTypeValue('Field1', new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS))
    rma.save()

    expect: 'the field value is retrieved'
    rma.getRmaTypeValue('Field1') == new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
  }

  @Rollback
  def "verify that getFieldValue works on a POGO class"() {
    given: 'a flex type with a field'
    def flexType = DataGenerator.buildFlexType()

    when: 'a POGO with the extensible field holder has a value set'
    def pogo = new SamplePOGO(assemblyData: flexType)
    pogo.setAssemblyDataValue('FIELD1', 'XYZZY')

    then: 'the field value is retrieved'
    pogo.getAssemblyDataValue('FIELD1') == 'XYZZY'
  }

  def "verify that formatConfigurableTypeValues handles supported cases"() {
    given: 'a domain object with the given field values'
    def object = null
    RMA.withTransaction {
      def flexType = DataGenerator.buildFlexType(fieldCount: fieldCount)
      object = new RMA(rmaType: flexType)
      for (int i = (fieldCount - 1); i >= 0; i--) {
        // Add the field values in reverse order
        object.setRmaTypeValue("FIELD${i + 1}", values[i])
      }
    }

    expect: 'the method works'
    ExtensibleFieldHelper.instance.formatConfigurableTypeValues('rmaType', object, options) == result

    where:
    fieldCount | values                | options           | result
    3          | ['AAA', 'BBB', 'CCC'] | null              | 'FIELD1: AAA FIELD2: BBB FIELD3: CCC'
    3          | ['AAA', 'BBB', 'CCC'] | [highlight: true] | '<b>FIELD1</b>: AAA <b>FIELD2</b>: BBB <b>FIELD3</b>: CCC'
    3          | ['AAA', 'BBB', 'CCC'] | [newLine: true]   | 'FIELD1: AAA <br>FIELD2: BBB <br>FIELD3: CCC'
    2          | ['AAA', 'BBB']        | [maxLength: 10]   | 'FIELD1: AAA ...'
  }

  @Rollback
  def "verify that formatConfigurableTypeValues handles localized labels"() {
    given: 'a domain object with the given field values'
    def flexType = DataGenerator.buildFlexType(fieldLabel: 'order.label')
    def object = new RMA(rmaType: flexType)
    object.setRmaTypeValue("FIELD1", 'XYZ')

    expect: 'the method works'
    ExtensibleFieldHelper.instance.formatConfigurableTypeValues('rmaType', object) == "${lookup('order.label')}: XYZ"
  }

  @Rollback
  def "verify that formatConfigurableTypeValues supports POGOs"() {
    given: 'a POGO object with the given field values'
    def flexType = DataGenerator.buildFlexType(fieldLabel: 'order.label')
    def object = new SamplePOGO(assemblyData: flexType)
    object.setAssemblyDataValue("FIELD1", 'XYZ')

    expect: 'the method works'
    ExtensibleFieldHelper.instance.formatConfigurableTypeValues('assemblyData', object) == "${lookup('order.label')}: XYZ"
  }

  def "verify that getCustomHolderFieldName works for a class"() {
    expect: 'method works'
    ExtensibleFieldHelper.instance.getCustomHolderFieldName(clazz) == results

    where:
    clazz                 | results
    SampleParent          | 'customFields'
    FieldInsertAdjustment | null
  }

  def "verify that hasExtensibleFields works for a class"() {
    expect: 'method works'
    ExtensibleFieldHelper.instance.hasExtensibleFields(clazz) == results

    where:
    clazz                 | results
    SampleParent          | true
    FieldInsertAdjustment | false
  }


  @Rollback
  def "verify that getCustomChildLists works for custom child lists"() {
    given: 'a domain record with custom child records'
    def order = new Order(order: 'ABC')
    def comp1 = new CustomOrderComponent(order: order, sequence: 123).save()
    def comps = [comp1]
    order.setFieldValue('customComponents', comps)
    order.save()

    when: 'the list is returned'
    List<Map<String, Object>> list = ExtensibleFieldHelper.getCustomChildLists(order as DomainEntityInterface)

    then: 'the custom fields are correct'
    list.size() >= 1
    def map = list.find { it.fieldName == 'customComponents' }
    map.fieldName == 'customComponents'
    map.childClass == CustomOrderComponent
    map.parentFieldName == 'order'
    map.list == comps
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  @Rollback
  def "verify that getCustomChildLists gracefully handles non-extensible domains"() {
    expect: 'a null list'
    ExtensibleFieldHelper.getCustomChildLists(new FlexType()) == null
  }


  // test     ExtensibleFieldHelper.instance.formatCustomValues(object, options)

}
