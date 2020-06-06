/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import ch.qos.logback.classic.Level
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.RMA
import sample.domain.SampleParent

/**
 * Tests.
 */
class SearchEngineRequestBulkIndexSpec extends BaseSpecification {

  @SuppressWarnings('unused')
  static specNeeds = SERVER

  void cleanup() {
    // Make sure we don't leave a mock search helper in place
    SearchHelper.instance = new SearchHelper()
  }

  /**
   * Build some dummy Parent records
   * @param count The number of records to build.
   * @return The list of parents created.
   */
  List buildParentRecords(int count) {
    def list = []
    for (i in 1..count) {
      list << new SampleParent(name: "ABC $i").save()
    }
    return list
  }

  @Rollback
  def "verify that the run method indexes multiple objects"() {
    given: 'a number of domains to process'
    def objects = buildParentRecords(20)

    and: 'a mock client that simulates the index action'
    def mockSearchEngineClient = new MockSearchEngineClient()
    SearchHelper.instance.searchEngineClient = mockSearchEngineClient

    and: 'a mock appender is used for the error logging'
    def mockAppender = MockAppender.mock(SearchEngineRequestBulkIndex, Level.ERROR)

    when: 'an attempt is made to index an object'
    new SearchEngineRequestBulkIndex(objects).run()

    then: 'no message is logged'
    mockAppender.messages.size() == 0

    and: 'the bulk action is performed'
    mockSearchEngineClient.verify([action: 'bulkIndex', object: objects])
  }

  @Rollback
  def "verify that the getDomains method returns the right objects"() {
    given: 'a number of domains to process'
    def objects = buildParentRecords(20)

    and: 'a mock client that simulates the index action'
    SearchHelper.instance.searchEngineClient = new MockSearchEngineClient()

    when: 'the request is created and the list of domains is re-created'
    def list = new SearchEngineRequestBulkIndex(objects).findRecords()

    then: 'the right records are returned'
    list.size() == objects.size()
    for (object in list) {
      assert list.contains(object)
    }
  }

  @Rollback
  @SuppressWarnings(["GroovyResultOfObjectAllocationIgnored", "UnusedObject"])
  def "verify that the constructor fails if list contains more than one domain"() {
    given: 'two different domains to process'
    def objects = buildParentRecords(20)
    objects << new RMA(rma: "XYZ").save()

    when: 'an attempt is made to build the request'
    new SearchEngineRequestBulkIndex(objects)

    then: 'an exception is generated with the right details'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['mixed', 'domain', SampleParent.name, RMA.name, 'ABC', 'XYZ'])
  }

  @Rollback
  def "verify that the run method logs an WARN message if an object is missing"() {
    given: 'a number of domains to process'
    def objects = buildParentRecords(20)

    and: 'one of the objects is delete'
    objects[4].delete()

    and: 'a mock appender is used for the stack trace logging'
    def mockAppender = MockAppender.mock(SearchEngineRequestBulkIndex, Level.WARN)

    when: 'an attempt is made to build the request'
    new SearchEngineRequestBulkIndex(objects).findRecords()

    then: 'the missing message is logged'
    mockAppender.assertMessageIsValid(['missing', objects[4].uuid.toString()])
  }

  @Rollback
  def "verify that the run detects a failed index on one item and logs it"() {
    given: 'a number of domains to process'
    def objects = buildParentRecords(20)

    and: 'a mock client that simulates the index action'
    SearchHelper.instance.searchEngineClient = new MockSearchEngineClient(bulkIndexResult: [null, null, 'failed'])

    and: 'a mock appender is used for the error logging'
    def mockAppender = MockAppender.mock(SearchEngineRequestBulkIndex, Level.ERROR)

    when: 'an attempt is made to index an object'
    new SearchEngineRequestBulkIndex(objects).run()

    then: 'a message is logged'
    mockAppender.assertMessageIsValid(['index', 'not', 'created', 'failed', 'SampleParent'])
  }

  @Rollback
  def "verify that the run detects a mismatched result count from the request count and logs it"() {
    given: 'a number of domains to process'
    def objects = buildParentRecords(3)

    and: 'a mock client that simulates the index action with the wrong number of results'
    def mockResults = [took: 30, errors: false, items: [[index: [result: 'created']]]]
    SearchHelper.instance.searchEngineClient = new MockSearchEngineClient(indexObjectResults: mockResults)

    and: 'a mock appender is used for the error logging'
    def mockAppender = MockAppender.mock(SearchEngineRequestBulkIndex, Level.ERROR)

    when: 'an attempt is made to index an object'
    new SearchEngineRequestBulkIndex(objects).run()

    then: 'a message is logged'
    mockAppender.assertMessageIsValid(['index', 'not', 'created', '1 results', 'expected 3'])
  }

  @Rollback
  def "verify that the run finishes and notifies the SearchHelper it finished with no errors"() {
    given: 'a number of domains to process'
    def objects = buildParentRecords(1)

    and: 'a mock client that simulates the index action'
    def searchHelper = Mock(SearchHelper)
    SearchHelper.instance = searchHelper

    when: 'an attempt is made to index an object'
    new SearchEngineRequestBulkIndex(objects).run()

    then: 'the search helper was notified'
    1 * searchHelper.finishedBulkRequest(0)
    1 * searchHelper.bulkIndex(_) >> [items: [[index: [result: 'created']]]]
  }

  @Rollback
  def "verify that the run detects a failed index and notifies the SearchHelper it finished"() {
    given: 'a number of domains to process'
    def objects = buildParentRecords(1)

    and: 'a mock client that simulates the index action'
    def searchHelper = Mock(SearchHelper)
    SearchHelper.instance = searchHelper

    and: 'a mock appender to reduce output to console'
    MockAppender.mock(SearchEngineRequestBulkIndex, Level.ERROR)

    when: 'an attempt is made to index an object'
    new SearchEngineRequestBulkIndex(objects).run()

    then: 'the search helper was notified'
    1 * searchHelper.finishedBulkRequest(1)
    1 * searchHelper.bulkIndex(_) >> [items: [[index: [result: 'failed']]]]
  }

  @Rollback
  def "verify that the run detects an exception notifies the SearchHelper it finished"() {
    given: 'a number of domains to process'
    def objects = buildParentRecords(1)

    and: 'a mock client that simulates the index action'
    def searchHelper = Mock(SearchHelper)
    SearchHelper.instance = searchHelper

    and: 'a mock appender to capture the log message'
    def mockAppender = MockAppender.mock(SearchEngineRequestBulkIndex, Level.TRACE)

    when: 'an attempt is made to index an object'
    new SearchEngineRequestBulkIndex(objects).run()

    then: 'the exception is re-thrown'
    def ex = thrown(Exception)
    ex.toString().contains('bad')

    and: 'the search helper was notified'
    1 * searchHelper.finishedBulkRequest(1)
    1 * searchHelper.bulkIndex(_) >> { throw new IllegalArgumentException('bad exception') }
    _ * searchHelper.isSearchable(objects[0].class) >> true
    _ * searchHelper.getSearchDomainSettings(objects[0].class) >> new SearchDomainSettings()

    and: 'the exception is logged'
    mockAppender.assertMessageIsValid(['bad exception', 'parent', 'abc 1', '"_index"'])
  }

}
