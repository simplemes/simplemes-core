/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test

import ch.qos.logback.classic.Level
import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.web.report.ReportTimeIntervalEnum
import sample.domain.AllFieldsDomain
import sample.domain.Order
import sample.domain.SampleChild
import sample.domain.SampleParent
import spock.lang.IgnoreIf

/**
 * Tests the CRUD GUI Tester with most of the options.
 */
@IgnoreIf({ !sys['geb.env'] })
class CRUDGUITesterSpec extends BaseGUISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [AllFieldsDomain, Order, SampleChild, SampleParent]

  def "verify that the standard GUI definition pages work"() {
    given: 'some dates'
    def date = new Date()
    def dateOnly = new DateOnly()

    and: 'another domain record to reference'
    def (Order order) = (List) DataGenerator.generate {
      domain Order
    }

    expect: 'the test is run'
    CRUDGUITester.test {
      tester this
      domain AllFieldsDomain
      recordParams name: 'ABC', title: 'abc', qty: 0.0, count: 237, enabled: true, dueDate: dateOnly, dateTime: date,
                   notes: 'some notes', reportTimeInterval: ReportTimeIntervalEnum.LAST_7_DAYS,
                   order: order
      minimalParams name: 'XYZ'
      readOnlyFields 'displayOnlyText'
    }
  }

  def "verify that the tester works with enum field values"() {
    given: 'some dates'
    def date = new Date()
    def dateOnly = new DateOnly()

    and: 'another domain record to reference'
    def (Order order) = (List) DataGenerator.generate {
      domain Order
    }

    expect: 'the test is run'
    CRUDGUITester.test {
      tester this
      domain AllFieldsDomain
      enableListTests false
      enableCreateTests false
      enableShowTests false
      recordParams name: 'ABC', title: 'abc', qty: 0.0, count: 237, enabled: true, dueDate: dateOnly, dateTime: date,
                   notes: 'some notes', reportTimeInterval: ReportTimeIntervalEnum.LAST_7_DAYS,
                   order: order
      minimalParams name: 'XYZ', reportTimeInterval: ReportTimeIntervalEnum.LAST_6_MONTHS
      readOnlyFields 'displayOnlyText'
    }
  }

  def "verify that all tests can be disabled"() {
    expect: 'the test is run with all tests disabled'
    def t = CRUDGUITester.test {
      tester this
      enableListTests false
      enableShowTests false
      enableCreateTests false
      enableEditTests false
      domain AllFieldsDomain
      recordParams name: 'ABC', title: 'abc'
      minimalParams name: 'XYZ'
    }
    t.testCount == 0
  }

  def "verify that the HTMLBaseID and column list can be used in list page"() {
    given: 'a column list from the domain fieldOrder'
    def columns = DomainUtils.instance.getStaticFieldOrder(AllFieldsDomain).join(',')

    and: 'a mock appender for the Warn level only - eats the log message for this test'
    MockAppender.mock(CRUDGUITester, Level.WARN)

    expect: 'the test is run'
    CRUDGUITester.test {
      tester this
      htmlIDBase 'allFieldsDomain'
      dashDOption 'list'
      listColumns columns
      domain AllFieldsDomain
      recordParams name: 'ABC', title: 'abc'
      minimalParams name: 'XYZ'
    }
  }

  def "verify that the field list can be used in show page"() {
    given: 'a mock appender for the Warn level only - eats the log message for this test'
    MockAppender.mock(CRUDGUITester, Level.WARN)

    expect: 'the test is run with an error in the non-displayed field'
    CRUDGUITester.test {
      tester this
      dashDOption 'show'
      htmlIDBase 'allFieldsDomain'
      showFields "name,count"
      domain AllFieldsDomain
      recordParams name: 'ABC', title: 'XYZZY', count: 237   // title field will fail, if tested.
      minimalParams name: 'XYZ'
    }
  }

  def "verify that the field list can be used in create page"() {
    given: 'a mock appender for the Warn level only - eats the log message for this test'
    MockAppender.mock(CRUDGUITester, Level.WARN)

    expect: 'the test is run with an error in the non-displayed field'
    CRUDGUITester.test {
      tester this
      dashDOption 'create'
      htmlIDBase 'allFieldsDomain'
      createFields "name,count"
      domain AllFieldsDomain
      recordParams name: 'ABC', title: 'XYZZY', count: 237     // title field will fail, if tested.
      minimalParams name: 'XYZ'
    }
  }

  def "verify that the readOnly field list can be used with a create page test"() {
    given: 'a mock appender for the Warn level only - eats the log message for this test'
    MockAppender.mock(CRUDGUITester, Level.WARN)

    expect: 'the test is run with an error in the non-displayed field'
    CRUDGUITester.test {
      tester this
      dashDOption 'create'
      htmlIDBase 'allFieldsDomain'
      domain AllFieldsDomain
      recordParams name: 'ABC'
      minimalParams name: 'XYZ'
      readOnlyFields 'displayOnlyText'
    }
  }

  def "verify that the field list can be used in edit page"() {
    given: 'a mock appender for the Warn level only - eats the log message for this test'
    MockAppender.mock(CRUDGUITester, Level.WARN)

    expect: 'the test is run with an error in the non-displayed field'
    CRUDGUITester.test {
      tester this
      dashDOption 'edit'
      htmlIDBase 'allFieldsDomain'
      editFields "name,count"
      domain AllFieldsDomain
      recordParams name: 'ABC', title: 'XYZZY', count: 237     // title field will fail, if it is actually tested.
      minimalParams name: 'XYZ'
    }
  }

  def "verify that the bad value is detected in show page"() {
    given: 'a mock appender for the Warn level only - eats the log message for this test'
    MockAppender.mock(CRUDGUITester, Level.WARN)

    when: 'the test is run'
    CRUDGUITester.test {
      tester this
      dashDOption 'show'
      htmlIDBase 'allFieldsDomain'
      showFields "name,title"
      domain AllFieldsDomain
      recordParams name: 'ABC', title: 'XYZZY'
      minimalParams name: 'XYZ'
    }

    then: 'the right exception is thrown'
    def ex = thrown(Throwable)
    UnitTestUtils.assertExceptionIsValid(ex, ['title', 'XYZZY'])
  }

  def "verify that the D option for list works"() {
    given: 'a mock appender for the Warn level only'
    def mockAppender = MockAppender.mock(CRUDGUITester, Level.WARN)

    when: 'the test is run with one test enabled'
    def t = CRUDGUITester.test {
      tester this
      domain AllFieldsDomain
      dashDOption 'list'
      recordParams name: 'ABC', title: 'abc'
      minimalParams name: 'XYZ'
    }

    then: 'only one test is run'
    t.testCount == 1

    and: 'the log message is written'
    UnitTestUtils.assertContainsAllIgnoreCase(mockAppender.message, ['WARN', 'testOnly', 'list'])
  }

  def "verify that the D option for show works"() {
    given: 'a mock appender for the Warn level only'
    def mockAppender = MockAppender.mock(CRUDGUITester, Level.WARN)

    when: 'the test is run with one test enabled'
    def t = CRUDGUITester.test {
      tester this
      domain AllFieldsDomain
      dashDOption 'show'
      recordParams name: 'ABC', title: 'abc'
      minimalParams name: 'XYZ'
    }

    then: 'one test is run'
    t.testCount == 1

    and: 'the log message is written'
    UnitTestUtils.assertContainsAllIgnoreCase(mockAppender.message, ['WARN', 'testOnly', 'show'])
  }

  def "verify that the D option for create works"() {
    given: 'a mock appender for the Warn level only'
    def mockAppender = MockAppender.mock(CRUDGUITester, Level.WARN)

    when: 'the test is run with one test enabled'
    def t = CRUDGUITester.test {
      tester this
      domain AllFieldsDomain
      dashDOption 'create'
      recordParams name: 'ABC', title: 'abc'
      minimalParams name: 'XYZ'
      readOnlyFields 'displayOnlyText'
    }

    then: 'one test is run'
    t.testCount == 1

    and: 'the log message is written'
    UnitTestUtils.assertContainsAllIgnoreCase(mockAppender.message, ['WARN', 'testOnly', 'create'])
  }

  def "verify that the D option for edit works"() {
    given: 'a mock appender for the Warn level only'
    def mockAppender = MockAppender.mock(CRUDGUITester, Level.WARN)

    when: 'the test is run with one test enabled'
    def t = CRUDGUITester.test {
      tester this
      domain AllFieldsDomain
      dashDOption 'edit'
      recordParams name: 'ABC', title: 'abc'
      minimalParams name: 'XYZ'
      readOnlyFields 'displayOnlyText'
    }

    then: 'one test is run'
    t.testCount == 1

    and: 'the log message is written'
    UnitTestUtils.assertContainsAllIgnoreCase(mockAppender.message, ['WARN', 'testOnly', 'edit'])
  }

  /**
   * A place to store a value for closure testing.
   */
  static String _closureResult

  def "verify that the create closure can be run"() {
    given: 'a mock appender for the Warn level only - eats the log message for this test'
    MockAppender.mock(CRUDGUITester, Level.WARN)

    when: 'the test is run with the closure to set a value'
    CRUDGUITester.test {
      tester this
      dashDOption 'create'
      htmlIDBase 'allFieldsDomain'
      editFields "name,count"
      domain AllFieldsDomain
      recordParams name: 'ABC', count: 237
      minimalParams name: 'XYZ'
      readOnlyFields 'displayOnlyText'
      createClosure {
        _closureResult = getInputField("name").value()
      }
    }

    then: 'the closure was able to pull data from the GUI'
    _closureResult == 'ABC'
  }


  def "verify that the edit closure can be run"() {
    given: 'a mock appender for the Warn level only - eats the log message for this test'
    MockAppender.mock(CRUDGUITester, Level.WARN)

    when: 'the test is run with the closure to set a value'
    CRUDGUITester.test {
      tester this
      dashDOption 'edit'
      htmlIDBase 'allFieldsDomain'
      editFields "name,count"
      domain AllFieldsDomain
      recordParams name: 'ABC1', count: 237
      minimalParams name: 'XYZ'
      editClosure {
        _closureResult = getInputField("name").value()
      }
    }

    then: 'the closure was able to pull data from the GUI'
    _closureResult == 'ABC1'
  }

  def "verify that inline grid can be populated for create"() {
    given: 'a mock appender for the Warn level only - eats the log message for this test'
    MockAppender.mock(CRUDGUITester, Level.WARN)

    and: 'some values for the record'
    def dueDate = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
    def dateTime = new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS)

    when: 'the test is run with the closure to set a value'
    CRUDGUITester.test {
      tester this
      dashDOption 'create'
      editFields "name,sampleChildren"
      domain SampleParent
      recordParams name: 'ABC', sampleChildren: [[key               : 'C1', sequence: 111, title: 'title-c1', qty: 12.2, enabled: true,
                                                  dueDate           : dueDate, dateTime: dateTime,
                                                  format            : EnumFieldFormat.instance,
                                                  reportTimeInterval: ReportTimeIntervalEnum.LAST_30_DAYS]]
      minimalParams name: 'XYZ'
      unlabeledFields "sampleChildren"
    }

    then: 'the test passes'
    notThrown(Exception)
  }

  def "verify that inline grid can be populated with multiple rows for create"() {
    given: 'a mock appender for the Warn level only - eats the log message for this test'
    MockAppender.mock(CRUDGUITester, Level.WARN)

    when: 'the test is run with the closure to set a value'
    CRUDGUITester.test {
      tester this
      dashDOption 'create'
      editFields "name,sampleChildren"
      domain SampleParent
      recordParams name: 'ABC', sampleChildren: [[key: 'C1'],
                                                 [key: 'C2'],
                                                 [key: 'C3']]
      minimalParams name: 'XYZ'
      unlabeledFields "sampleChildren"
    }

    then: 'the test passes'
    notThrown(Exception)
  }

  def "verify that inline grid can be populated for edit"() {
    given: 'a mock appender for the Warn level only - eats the log message for this test'
    MockAppender.mock(CRUDGUITester, Level.WARN)

    when: 'the test is run with the closure to set a value'
    CRUDGUITester.test {
      tester this
      dashDOption 'edit'
      editFields "name,sampleChildren"
      domain SampleParent
      recordParams name: 'ABC', sampleChildren: [[key: 'C1', sequence: 111]]
      minimalParams name: 'XYZ'
      unlabeledFields "sampleChildren"
    }

    then: 'the test passes'
    notThrown(Exception)
  }

  def "verify that inline grid values are correct for show case"() {
    given: 'a mock appender for the Warn level only - eats the log message for this test'
    MockAppender.mock(CRUDGUITester, Level.WARN)
    //setTraceLogLevel(CRUDGUITester)

    when: 'the test is run with the closure to set a value'
    CRUDGUITester.test {
      tester this
      dashDOption 'show'
      editFields "name,sampleChildren"
      domain SampleParent
      recordParams name: 'ABC', sampleChildren: [[key: 'C1', sequence: 111]]
      minimalParams name: 'XYZ'
      unlabeledFields "sampleChildren"
    }

    then: 'the test passes'
    notThrown(Exception)
  }

  // inline grid edit mode (update existing record?
  // check create/edit with readOnly fields.
  // test gridWidget grid name option.
}
