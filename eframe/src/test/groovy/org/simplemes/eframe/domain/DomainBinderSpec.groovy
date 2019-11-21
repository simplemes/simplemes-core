package org.simplemes.eframe.domain

import ch.qos.logback.classic.Level
import grails.gorm.transactions.Rollback
import org.simplemes.eframe.custom.domain.FieldExtension
import org.simplemes.eframe.data.format.BigDecimalFieldFormat
import org.simplemes.eframe.data.format.DateFieldFormat
import org.simplemes.eframe.data.format.IntegerFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.system.DisabledStatus
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.web.report.ReportTimeIntervalEnum
import sample.domain.AllFieldsDomain
import sample.domain.Order
import sample.domain.OrderLine
import sample.domain.RMA
import sample.domain.SampleChild
import sample.domain.SampleParent

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class DomainBinderSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [JSON]

  @SuppressWarnings("unused")
  static dirtyDomains = [SampleParent, AllFieldsDomain, OrderLine, Order]

  @SuppressWarnings("GrEqualsBetweenInconvertibleTypes")
  def "verify that simple one level binding on a domain works with non-string values"() {
    given: 'a domain object to bind to'
    def o = new AllFieldsDomain()

    when: 'the object params are bound'
    DomainBinder.build().bind(o, [name              : 'ABC', title: 'abc', qty: 12.2, count: 247, enabled: true,
                                  dateTime          : new Date(UnitTestUtils.SAMPLE_TIME_MS),
                                  reportTimeInterval: ReportTimeIntervalEnum.YESTERDAY,
                                  status            : DisabledStatus.instance])

    then: 'the object has the right values'
    o.name == 'ABC'
    o.title == 'abc'
    o.qty == 12.2
    o.count == 247
    o.enabled
    o.dateTime == new Date(UnitTestUtils.SAMPLE_TIME_MS)
    o.reportTimeInterval == ReportTimeIntervalEnum.YESTERDAY
    o.status == DisabledStatus.instance
  }

  def "verify that simple one level binding on a domain works with string values"() {
    given: 'a domain object to bind to'
    def o = new AllFieldsDomain()

    when: 'the object params are bound'
    DomainBinder.build().bind(o, [name              : 'ABC', title: 'abc', qty: 12.2, count: 247, enabled: true,
                                  dateTime          : new Date(UnitTestUtils.SAMPLE_TIME_MS),
                                  reportTimeInterval: ReportTimeIntervalEnum.YESTERDAY.toString(),
                                  status            : DisabledStatus.instance.id])

    then: 'the object has the right values'
    o.name == 'ABC'
    o.title == 'abc'
    o.qty == 12.2
    o.count == 247
    o.enabled
    o.dateTime == new Date(UnitTestUtils.SAMPLE_TIME_MS)
    //noinspection GrEqualsBetweenInconvertibleTypes
    o.status == DisabledStatus.instance
  }

  def "verify that simple one level binding works with string values"() {
    given: 'a simple POGO with several field types'
    def src = """
    package sample
    class SampleClass {
      String name 
      String title
      BigDecimal qty
      Integer count
      Boolean enabled
      boolean primary
      Date dateTime
    }
    """
    def o = CompilerTestUtils.compileSource(src).newInstance()

    when: 'the string params are bound'
    DomainBinder.build().bind(o, [name    : 'ABC', title: 'abc', qty: '12.2', count: '247', enabled: 'true',
                                  dateTime: UnitTestUtils.SAMPLE_ISO_TIME_STRING])

    then: 'the object has the right values'
    o.name == 'ABC'
    o.title == 'abc'
    o.qty == 12.2
    o.count == 247
    o.enabled
    o.dateTime == new Date(UnitTestUtils.SAMPLE_TIME_MS)
  }

  def "verify that using the wrong non-string type for a value fails gracefully"() {
    given: 'a simple POGO with several field types'
    def src = """
    package sample
    class SampleClass {
      BigDecimal qty
    }
    """
    def o = CompilerTestUtils.compileSource(src).newInstance()

    when: 'the params are bound'
    DomainBinder.build().bind(o, [qty: 237L])

    then: 'the correct exception is thrown'
    // "Invalid value type ${value.getClass()}, value: ${value}. Expected String or ${propertyClass}. Property $key in object $object"
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['237', 'long', 'BigDecimal', 'qty'])
  }

  def "verify that null values work correctly"() {
    given: 'a simple POGO with several field types'
    def src = """
    package sample
    class SampleClass {
      String name 
      String title
      BigDecimal qty
      Integer count
      Boolean enabled
      Date dateTime
    }
    """
    def o = CompilerTestUtils.compileSource(src).newInstance()

    and: 'the values are set to non-nulls'
    o.name = 'ABC'
    o.title = 'abc'
    o.qty = 12.2
    o.count = 247
    o.enabled = true
    o.dateTime = new Date()

    when: 'the params are bound'
    DomainBinder.build().bind(o, [name: null, title: null, qty: null, count: null, enabled: null, dateTime: null])

    then: 'the correct values are set'
    o.name == null
    o.title == null
    o.qty == null
    o.count == null
    o.enabled == null
    o.dateTime == null
  }

  def "verify that blank values work correctly"() {
    given: 'a simple POGO with several field types'
    def src = """
    package sample
    class SampleClass {
      String name 
      String title
      BigDecimal qty
      Integer count
      Boolean enabled
      Date dateTime
    }
    """
    def o = CompilerTestUtils.compileSource(src).newInstance()

    and: 'the values are set to non-nulls'
    o.name = 'ABC'
    o.title = 'abc'
    o.qty = 12.2
    o.count = 247
    o.enabled = true
    o.dateTime = new Date()

    when: 'the params are bound'
    DomainBinder.build().bind(o, [name: '', title: '', qty: '', count: '', enabled: '', dateTime: ''])

    then: 'the correct values are set'
    o.name == ''
    o.title == ''
    o.qty == null
    o.count == null
    o.enabled == null
    o.dateTime == null
  }

  def "verify that ui mode works on bind"() {
    given: 'a simple POGO with several field types'
    def src = """
    package sample
    import org.simplemes.eframe.date.DateOnly
    //import grails.gorm.annotation.Entity
    
    //@Entity
    class SampleClass {
      Date dateTime
      DateOnly dueDate
      boolean enabled
    }
    """
    def o = CompilerTestUtils.compileSource(src).newInstance()

    and: 'the locale is set'
    GlobalUtils.defaultLocale = locale

    when: 'the params are bound using a locale-independent date format'
    def date = new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS)
    def dueDate = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
    DomainBinder.build().bind(o, [dateTime: DateFieldFormat.instance.formatForm(date, locale, null),
                                  dueDate : UnitTestUtils.SAMPLE_ISO_DATE_ONLY_STRING, enabled: '1'], true)

    then: 'the correct values are set'
    o.dueDate == dueDate
    o.dateTime == date
    o.enabled

    where:
    locale         | _
    Locale.US      | _
    Locale.GERMANY | _
  }

  def "verify that bind gracefully handles parse errors in the top-level object"() {
    given: 'a simple Domain class with a date field'
    def src = """
    package sample
    import grails.gorm.annotation.Entity
    
    @Entity
    class SampleClass {
      Date dateTime
    }
    """
    def o = CompilerTestUtils.compileSource(src).newInstance()

    when: 'the params are bound using a locale-independent date format'
    DomainBinder.build().bind(o, [dateTime: 'gibberish'], true)

    then: 'the domains errors list is populated correctly'
    o.errors
    def errorsByField = GlobalUtils.lookupValidationErrors(o.errors)
    errorsByField['dateTime'][0].contains('gibberish')
  }

  def "verify that bind gracefully handles parse errors in child records"() {
    given: 'a domain object to bind to'
    def o = new SampleParent()

    when: 'the object params are bound'
    DomainBinder.build().bind(o, [name                        : 'ABC',
                                  'sampleChildren[0].key'     : 'ABC',
                                  'sampleChildren[0].title'   : 'abc',
                                  'sampleChildren[0].dateTime': 'gibberish'], true)

    then: 'the domains errors list is populated correctly'
    o.errors
    def errorsByField = GlobalUtils.lookupValidationErrors(o.errors)
    errorsByField['sampleChildren.dateTime'][0].contains('gibberish')
  }

  def "verify that unknown inputs are ignored"() {
    given: 'a mock appender for one level only'
    def mockAppender = MockAppender.mock(DomainBinder, Level.WARN)

    and: 'a domain object to bind to'
    def o = new AllFieldsDomain()

    when: 'the object params are bound'
    DomainBinder.build().bind(o, [name: 'ABC', title: 'abc', gibberish: 'abc'])

    then: 'the object has the right values'
    o.name == 'ABC'
    o.title == 'abc'

    then: 'the warning message is written'
    UnitTestUtils.assertContainsAllIgnoreCase(mockAppender.message, ['WARN', 'ignoring', 'gibberish'])
  }

  @Rollback
  def "verify that one level child bindings works on create"() {
    given: 'a domain object to bind to'
    def o = new SampleParent()

    when: 'the object params are bound'
    DomainBinder.build().bind(o, [name                        : 'ABC',
                                  'sampleChildren[0].key'     : 'ABC',
                                  'sampleChildren[0].title'   : 'abc',
                                  'sampleChildren[0].sequence': '247'], true)

    and: 'the top-level record is saved'
    o.save()

    then: 'the child object has the right values'
    o.sampleChildren.size() == 1
    SampleChild child = (SampleChild) o.sampleChildren[0]
    child.key == 'ABC'
    child.title == 'abc'
    child.sequence == 247

    and: 'the record can be read from the DB'
    def record = SampleParent.findByName('ABC')
    record.sampleChildren.size() == 1
    SampleChild child2 = (SampleChild) record.sampleChildren[0]
    child2.key == 'ABC'
    child2.title == 'abc'
    child2.sequence == 247
  }

  @Rollback
  def "verify that one level child bindings works on create - multiple rows in wrong order"() {
    given: 'a domain object to bind to'
    def o = new SampleParent()

    when: 'the object params are bound'
    def map = [name                   : 'ABC',
               'sampleChildren[2].key': 'PDQ', 'sampleChildren[1].sequence': '247',
               'sampleChildren[0].key': 'XYZ', 'sampleChildren[0].sequence': '147',
               'sampleChildren[1].key': 'ABC', 'sampleChildren[2].sequence': '347',
    ]
    DomainBinder.build().bind(o, map, true)

    and: 'the top-level record is saved'
    o.save()

    then: 'the child object has the right values'
    o.sampleChildren.size() == 3
    SampleChild child1 = (SampleChild) o.sampleChildren[0]
    child1.key == 'XYZ'
    child1.sequence == 147
    SampleChild child2 = (SampleChild) o.sampleChildren[1]
    child2.key == 'ABC'
    child2.sequence == 247
    SampleChild child3 = (SampleChild) o.sampleChildren[2]
    child3.key == 'PDQ'
    child3.sequence == 347

    and: 'the record can be read from the DB'
    def record = SampleParent.findByName('ABC')
    record.sampleChildren.size() == 3
    SampleChild child1a = (SampleChild) record.sampleChildren[0]
    child1a.key == 'XYZ'
    child1a.sequence == 147
    SampleChild child2a = (SampleChild) record.sampleChildren[1]
    child2a.key == 'ABC'
    child2a.sequence == 247
    SampleChild child3a = (SampleChild) record.sampleChildren[2]
    child3a.key == 'PDQ'
    child3a.sequence == 347
  }

  def "verify that one level child bindings works on single row update - commit test too"() {
    given: 'a mock appender for one level only'
    def mockAppender = MockAppender.mock(DomainBinder, Level.WARN)

    and: 'a saved domain object to bind to'
    def record = null
    def child = null
    def originalChildVersion = null
    SampleParent.withTransaction {
      child = new SampleChild(key: 'k1', title: 'title1', sequence: 147)
      record = new SampleParent(name: 'ABC')
      record.addToSampleChildren(child)
      record.save()
      originalChildVersion = child.version
    }

    when: 'the object params are bound'
    SampleParent.withTransaction {
      DomainBinder.build().bind(record, [name                        : 'ABC',
                                         'sampleChildren[0].id'      : "gibberish",
                                         'sampleChildren[0]._dbId'   : "$child.id",
                                         'sampleChildren[0].key'     : 'k2',
                                         'sampleChildren[0].title'   : 'title2',
                                         'sampleChildren[0].sequence': '247'], true)
    }

    and: 'the top-level record is saved'
    SampleParent.withTransaction {
      record.save()
    }

    then: 'the record can be read from the DB'
    SampleParent.withTransaction {
      def record1 = SampleParent.findByName('ABC')
      assert record1.sampleChildren.size() == 1
      SampleChild child1 = (SampleChild) record1.sampleChildren[0]
      assert child1.key == 'k2'
      assert child1.title == 'title2'
      assert child1.sequence == 247

      // and the record ID is unchanged and the version number was changed - means hibernate really saved the record
      assert child1.id == child.id
      assert child1.version != originalChildVersion
      true
    }

    then: 'no log message is written for the ID parameter'
    !mockAppender.message
  }

  @Rollback
  def "verify that one level child bindings works when adding rows"() {
    given: 'a saved domain object to bind to'
    def child = new SampleChild(key: 'k1', title: 'title1', sequence: 147)
    def record = new SampleParent(name: 'ABC')
    record.addToSampleChildren(child)
    record.save()

    // Mixed up order of the parameters is intentional.  Tests the index sorting for the rows.
    when: 'the object params are bound'
    DomainBinder.build().bind(record, [name                        : 'ABC',
                                       'sampleChildren[1].key'     : 'k3',
                                       'sampleChildren[1].title'   : 'title3',
                                       'sampleChildren[0]._dbId'   : "$child.id".toString(),
                                       'sampleChildren[0].key'     : 'k2',
                                       'sampleChildren[0].title'   : 'title2',
                                       'sampleChildren[0].sequence': '247',
                                       'sampleChildren[1].sequence': '347'], true)

    and: 'the top-level record is saved'
    record.save()

    then: 'the record can be read from the DB'
    def record1 = SampleParent.findByName('ABC')
    record1.sampleChildren.size() == 2
    SampleChild child1 = (SampleChild) record1.sampleChildren[0]
    child1.key == 'k2'
    child1.title == 'title2'
    child1.sequence == 247
    child1.id == child.id

    SampleChild child2 = (SampleChild) record1.sampleChildren[1]
    child2.key == 'k3'
    child2.title == 'title3'
    child2.sequence == 347
    child2.id != child.id
  }

  def "verify that one level child bindings works when removing rows"() {
    given: 'a saved domain object to bind to'
    def child1 = null
    def child3 = null
    def record = null
    SampleParent.withTransaction {
      child1 = new SampleChild(key: 'k1', title: 'title1', sequence: 147)
      def child2 = new SampleChild(key: 'k2', title: 'title2', sequence: 247)
      child3 = new SampleChild(key: 'k3', title: 'title3', sequence: 347)
      record = new SampleParent(name: 'ABC')
      record.addToSampleChildren(child1)
      record.addToSampleChildren(child2)
      record.addToSampleChildren(child3)
      record.save()
    }

    // Mixed up order of the parameters is intentional.  Tests the index sorting for the rows.
    when: 'the object params are bound'
    SampleParent.withTransaction {
      DomainBinder.build().bind(record, [name                        : 'ABC',
                                         'sampleChildren[0]._dbId'   : "$child1.id",
                                         'sampleChildren[0].key'     : 'k2a',
                                         'sampleChildren[0].title'   : 'title2a',
                                         'sampleChildren[0].sequence': '2247',
                                         'sampleChildren[1]._dbId'   : "$child3.id",
                                         'sampleChildren[1].key'     : 'k3a',
                                         'sampleChildren[1].title'   : 'title3a',
                                         'sampleChildren[1].sequence': '2347',
      ], true)
      record.save()
    }

    then: 'the record can be read from the DB'
    SampleParent.withTransaction {
      def record1 = SampleParent.findByName('ABC')
      assert record1.sampleChildren.size() == 2
      SampleChild child1a = (SampleChild) record1.sampleChildren[0]
      assert child1a.key == 'k2a'
      assert child1a.title == 'title2a'
      assert child1a.sequence == 2247

      SampleChild child2a = (SampleChild) record1.sampleChildren[1]
      assert child2a.key == 'k3a'
      assert child2a.title == 'title3a'
      assert child2a.sequence == 2347

      assert SampleChild.count() == 2
      true
    }
  }

  @Rollback
  def "verify that one level child bindings works with sparse list"() {
    def record = new SampleParent()
    // Mixed up order of the parameters is intentional.  Tests the index sorting for the rows.
    when: 'the object params are bound'
    DomainBinder.build().bind(record, [name                        : 'ABC',
                                       'sampleChildren[2].key'     : 'k3',
                                       'sampleChildren[2].title'   : 'title3',
                                       'sampleChildren[0].key'     : 'k2',
                                       'sampleChildren[0].title'   : 'title2',
                                       'sampleChildren[0].sequence': '247',
                                       'sampleChildren[2].sequence': '347'], true)

    and: 'the top-level record is saved'
    record.save()

    then: 'the record can be read from the DB'
    def record1 = SampleParent.findByName('ABC')
    record1.sampleChildren.size() == 2
    SampleChild child1 = (SampleChild) record1.sampleChildren[0]
    child1.key == 'k2'
    child1.title == 'title2'
    child1.sequence == 247

    SampleChild child2 = (SampleChild) record1.sampleChildren[1]
    child2.key == 'k3'
    child2.title == 'title3'
    child2.sequence == 347
  }

  @Rollback
  def "verify that one level child bindings works with map list input"() {
    when: 'the object params are bound'
    def record = new SampleParent()
    DomainBinder.build().bind(record, [name: 'ABC', sampleChildren: [[key: 'C1', sequence: 111]]])

    and: 'the top-level record is saved'
    record.save()

    then: 'the record can be read from the DB'
    def record1 = SampleParent.findByName('ABC')
    record1.sampleChildren.size() == 1
    SampleChild child1 = (SampleChild) record1.sampleChildren[0]
    child1.key == 'C1'
    child1.sequence == 111
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that bind works with domain reference list to foreign domains"() {
    given: 'some domain records'
    List<AllFieldsDomain> afd = null
    AllFieldsDomain.withTransaction {
      afd = DataGenerator.generate {
        domain AllFieldsDomain
        count 3
      }
    }
    //setTraceLogLevel(DomainBinder)

    when: 'the object params are bound and saved'
    SampleParent.withTransaction {
      def o = new SampleParent(name: 'ABC')
      DomainBinder.build().bind(o, [name              : 'ABC',
                                    'allFieldsDomains': "${afd[0].id},${afd[2].id}".toString()])
      o.save()
    }

    then: 'the foreign records are saved'
    SampleParent.withTransaction {
      def sampleParent = SampleParent.findByName('ABC')
      assert sampleParent.allFieldsDomains.size() == 2
      assert sampleParent.allFieldsDomains[0] == afd[0]
      assert sampleParent.allFieldsDomains[1] == afd[2]
      true
    }

  }

  @Rollback
  def "verify that bind supports custom fields - API mode"() {
    given: 'a mocked FieldDefinitions for the domain'
    new FieldExtension(fieldName: 'abc', domainClassName: SampleParent.name,
                       fieldFormat: BigDecimalFieldFormat.instance).save()

    when: 'the object params are bound'
    def record = new SampleParent()
    DomainBinder.build().bind(record, [name: 'ABC', abc: '1.2'])

    then: 'the custom field is in the domain record'
    record.getFieldValue('abc') == 1.2
  }

  @Rollback
  def "verify that bind supports custom fields - UI mode"() {
    given: 'a mocked FieldDefinitions for the domain'
    new FieldExtension(fieldName: 'abc', domainClassName: SampleParent.name,
                       fieldFormat: format.instance).save()

    and: 'the locale is set'
    GlobalUtils.defaultLocale = locale

    when: 'the object params are bound'
    def record = new SampleParent()
    DomainBinder.build().bind(record, [name: 'ABC', abc: value], true)

    then: 'the custom field is in the domain record'
    record.getFieldValue('abc') == result


    where:
    format                | locale         | value   | result
    BigDecimalFieldFormat | Locale.US      | '1.2'   | 1.2
    BigDecimalFieldFormat | Locale.GERMANY | '1,2'   | 1.2
    BigDecimalFieldFormat | Locale.US      | null    | null
    IntegerFieldFormat    | Locale.US      | '1,004' | 1004
  }

  def "verify that custom child list bindings works on create - UI Mode"() {
    given: 'a domain object to bind to'

    when: 'the object params are bound'
    def order = null
    Order.withTransaction {
      order = new Order()
      DomainBinder.build().bind(order, [order                   : 'ABC',
                                        'orderLines[0].sequence': '1',
                                        'orderLines[0].qty'     : '1.2',
                                        'orderLines[0].product' : 'PROD1',
                                        'orderLines[1].sequence': '2',
                                        'orderLines[1].qty'     : '2.2',
                                        'orderLines[1].product' : 'PROD2'], true)

      and: 'the top-level record is saved'
      order.save()
    }

    then: 'the records are correct in the DB'
    Order.withTransaction {
      order = Order.findByOrder('ABC')
      List<OrderLine> orderLines = order.getFieldValue('orderLines')
      assert orderLines.size() == 2
      assert orderLines[0].sequence == 1
      assert orderLines[0].qty == 1.2
      assert orderLines[0].product == 'PROD1'
      assert orderLines[1].sequence == 2
      assert orderLines[1].qty == 2.2
      assert orderLines[1].product == 'PROD2'
      true
    }
  }

  def "verify that custom child list bindings works on create - API mode"() {
    when: 'the object params are bound'
    def order = null
    Order.withTransaction {
      order = new Order()
      DomainBinder.build().bind(order, [order     : 'ABC',
                                        orderLines: [
                                          [sequence: 1, product: 'PROD1', qty: 12.2],
                                          [sequence: 2, product: 'PROD2', qty: 22.2]
                                        ]])

      and: 'the top-level record is saved'
      order.save()
    }

    then: 'the records are correct in the DB'
    Order.withTransaction {
      order = Order.findByOrder('ABC')
      List<OrderLine> orderLines = order.getFieldValue('orderLines')
      assert orderLines.size() == 2
      assert orderLines[0].sequence == 1
      assert orderLines[0].qty == 12.2
      assert orderLines[0].product == 'PROD1'
      assert orderLines[1].sequence == 2
      assert orderLines[1].qty == 22.2
      assert orderLines[1].product == 'PROD2'
      true
    }
  }

  def "verify that custom child list bindings works on update - API mode"() {
    given: 'a domain object to bind to'
    def order = null
    Order.withTransaction {
      order = new Order(order: 'M1001')
      def orderLines = []
      orderLines << new OrderLine(sequence: 1, product: 'PROD1')
      orderLines << new OrderLine(sequence: 2, product: 'PROD2')
      orderLines << new OrderLine(sequence: 3, product: 'PROD3')
      order.setFieldValue('orderLines', orderLines)
      order.save()
    }

    when: 'the object params are bound'
    Order.withTransaction {
      order = new Order()
      DomainBinder.build().bind(order, [order     : 'ABC',
                                        orderLines: [
                                          [sequence: 11, product: 'PROD1A', qty: 32.2],
                                          [sequence: 12, product: 'PROD2A', qty: 42.2]
                                        ]])

      and: 'the top-level record is saved'
      order.save()
    }

    then: 'the records are correct in the DB'
    Order.withTransaction {
      order = Order.findByOrder('ABC')
      List<OrderLine> orderLines = order.getFieldValue('orderLines')
      assert orderLines.size() == 2
      assert orderLines[0].sequence == 11
      assert orderLines[0].qty == 32.2
      assert orderLines[0].product == 'PROD1A'
      assert orderLines[1].sequence == 12
      assert orderLines[1].qty == 42.2
      assert orderLines[1].product == 'PROD2A'
      true
    }
  }

  @Rollback
  def "verify that bind supports Configurable Type field"() {
    given: 'a default flex type'
    def flexType = DataGenerator.buildFlexType()

    when: 'the object params are bound'
    def rma = new RMA()
    DomainBinder.build().bind(rma, [rma: 'ABC', rmaType: flexType.id.toString(), rmaType_FIELD1: 'custom value 1'])

    then: 'the domain has the right values'
    rma.getRmaTypeValue('FIELD1') == 'custom value 1'
  }

  // remove foreign domain from list.
  // foreign add row to list


  // test grandChild
  // test field types -domainRef

}