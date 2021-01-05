/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import ch.qos.logback.classic.Level
import org.simplemes.eframe.application.EFrameConfiguration
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.AllFieldsDomain
import sample.domain.CustomOrderComponent
import sample.domain.Order
import sample.domain.RMA
import sample.domain.SampleParent

/**
 * Tests for the client.  This does not test the real interaction with the external search engine.
 * The SearchEngineLiveSpec does that.
 */
class SearchEngineClientSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  void cleanup() {
    Holders.configuration.search = new EFrameConfiguration.Search()
  }

  def "verify that getStatus works"() {
    given: 'a search engine client with a mock rest client'
    def mockRestClient = new MockRestClient(method: 'GET', uri: "/_cluster/health",
                                            response: [status: 'unknown'])
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    expect:
    searchEngineClient.status.status == 'unknown'
  }

  def "verify that getStatus logs inputs and outputs as debug messages"() {
    given: 'a search engine client with a mock rest client'
    def mockRestClient = new MockRestClient(method: 'GET', uri: "/_cluster/health",
                                            response: [status: 'unknown'])
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    and: 'a mock logger'
    def mockAppender = MockAppender.mock(SearchEngineClient, Level.DEBUG)

    when: 'the object is indexed'
    searchEngineClient.status

    then: 'the inputs and results are logged'
    mockAppender.assertMessageIsValid(['DEBUG', 'GET', "/_cluster/health"], 0)
    mockAppender.assertMessageIsValid(['DEBUG', 'status', 'unknown'], 1)
  }

  def "verify that getStatus logs performance as info message"() {
    given: 'a search engine client with a mock rest client'
    def mockRestClient = new MockRestClient(method: 'GET', uri: "/_cluster/health",
                                            response: [status: 'unknown'])
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    and: 'a mock logger'
    def mockAppender = MockAppender.mock(SearchEngineClient, Level.INFO)

    when: 'the object is indexed'
    searchEngineClient.status

    then: 'the perf info is logged'
    mockAppender.assertMessageIsValid(['INFO', 'elapsed'])
  }

  @Rollback
  def "verify that buildURIForIndexRequest works with existing domain"() {
    given: 'a helper and a domain object'
    def parent = new SampleParent(name: 'ABC').save()

    expect: 'the URI to be correct'
    SearchEngineClient.buildURIForIndexRequest(parent) == "/sample-parent/_doc/${parent.uuid}"
  }

  @Rollback
  def "verify that buildURIForIndexRequest works with mixed case domain names"() {
    given: 'a helper and a domain object'
    def sampleParent = new SampleParent(name: 'ABC').save()

    expect: 'the URI to be correct'
    SearchEngineClient.buildURIForIndexRequest(sampleParent) == "/sample-parent/_doc/${sampleParent.uuid}"
  }

  def "verify that buildURIForIndexRequest fails with unsaved domain object"() {
    given: 'a helper and a domain object'
    def parent = new SampleParent(name: 'ABC')

    when: 'the method is called'
    SearchEngineClient.buildURIForIndexRequest(parent)

    then: 'it should fail with a clear message'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['uuid'])
  }

  def "verify that the getRestClient does not log too many messages when search is disabled"() {
    given: 'a configuration with no external hosts - search disabled and a mock appender'
    Holders.configuration.search.hosts = null
    def mockAppender = MockAppender.mock(SearchEngineClient, Level.INFO)

    and: 'a helper'
    def searchEngineClient = new SearchEngineClient()

    when: 'the method is called'
    def client = searchEngineClient.restClient

    then: 'no client is returned'
    !client

    and: 'just one log message is found'
    mockAppender.messages.size() == 1

    when: 'the method is called a second time'
    searchEngineClient.restClient

    then: 'no additional messages are logged'
    mockAppender.messages.size() == 1
  }

  @Rollback
  def "verify that indexObject works with simple object"() {
    given: 'a saved domain object'
    def parent = new SampleParent(name: 'ABC').save()

    and: 'a mock search rest client and response'
    def mockRestClient = new MockRestClient(method: 'PUT', uri: SearchEngineClient.buildURIForIndexRequest(parent),
                                            content: parent,
                                            response: [result: 'created', uuid: parent.uuid])
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    when: 'the object is indexed'
    def res = searchEngineClient.indexObject(parent)

    then: 'the result is correct'
    res._index == 'sample-parent'
    res._id == "$parent.uuid"
    res.result == "created"
  }

  @Rollback
  def "verify that indexObject logs inputs and outputs as debug messages"() {
    given: 'a saved domain object'
    def parent = new SampleParent(name: 'ABC').save()

    and: 'a mock search rest client and response'
    def mockRestClient = new MockRestClient(method: 'PUT', uri: SearchEngineClient.buildURIForIndexRequest(parent),
                                            content: parent,
                                            response: [result: 'created', id: parent.uuid])
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    and: 'a mock logger'
    def mockAppender = MockAppender.mock(SearchEngineClient, Level.DEBUG)

    when: 'the object is indexed'
    searchEngineClient.indexObject(parent)

    then: 'the inputs and results are logged'
    mockAppender.assertMessageIsValid(['DEBUG', 'PUT', SearchEngineClient.buildURIForIndexRequest(parent), '"name":"ABC"'], 0)
    mockAppender.assertMessageIsValid(['DEBUG', 'result', 'created'], 1)
  }

  @Rollback
  def "verify that indexObject logs performance info message"() {
    given: 'a saved domain object'
    def parent = new SampleParent(name: 'ABC').save()

    and: 'a mock search rest client and response'
    def mockRestClient = new MockRestClient(method: 'PUT', uri: SearchEngineClient.buildURIForIndexRequest(parent),
                                            content: parent,
                                            response: [result: 'created', id: parent.uuid])
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    and: 'a mock logger'
    def mockAppender = MockAppender.mock(SearchEngineClient, Level.INFO)

    when: 'the object is indexed'
    searchEngineClient.indexObject(parent)

    then: 'the perf info is logged'
    mockAppender.assertMessageIsValid(['INFO', 'elapsed'])
  }

  def "verify that indexObject fails with null domain object"() {
    given: 'a helper'
    def searchEngineClient = new SearchEngineClient()

    when: 'the method is called'
    searchEngineClient.indexObject(null)

    then: 'it should fail with a clear message'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['object'])
  }

  def "verify that globalSearch works with simple query string"() {
    given: 'a mock search rest client and response'
    def uuid1 = UUID.randomUUID()
    def uuid2 = UUID.randomUUID()
    def mockRestClient = new MockRestClient(method: 'GET', uri: '/_search?q=abc*',
                                            response: [took: '13', _index: 'sample-parent',
                                                       hits: [[code: 'abc1', uuid: uuid1], [code: 'abc2', uuid: uuid2]]])
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    when: 'the search is performed'
    def res = searchEngineClient.globalSearch('abc*')

    then: 'the result is correct'
    res.totalHits == 2
    res.elapsedTime == 13
    res.hits[0].uuid == uuid1
    res.hits[0].className == SampleParent.name
    res.hits[1].uuid == uuid2
    res.hits[1].className == SampleParent.name
  }

  def "verify that globalSearch passes from and size works with simple query string"() {
    given: 'a mock search rest client and response'
    def uuid1 = UUID.randomUUID()
    def uuid2 = UUID.randomUUID()
    def mockRestClient = new MockRestClient(method: 'GET', uri: '/_search?q=abc*&size=2&from=3',
                                            response: [className: SampleParent.name,
                                                       totalHits: 200,
                                                       hits     : [[code: 'abc4', uuid: uuid1], [code: 'abc5', uuid: uuid2]]])
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    when: 'the search is performed'
    def res = searchEngineClient.globalSearch('abc*', [size: 2, from: 3])

    then: 'the result are offset and limited'
    res.totalHits == 200
    res.from == 3
    res.size == 2
    res.hits[0].uuid == uuid1
    res.hits[1].uuid == uuid2
  }

  def "verify that globalSearch works with complex JSON search content body"() {
    given: 'a mock search rest client and response'
    def mockRestClient = new MockRestClient(method: 'GET', uri: '/_search',
                                            response: [hits: [[code: 'xyz1']]])
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    when: 'the search is performed'
    def res = searchEngineClient.globalSearch('{query: "xyz*"}')

    then: 'the result is correct'
    res.totalHits == 1
    res.hits[0].uuid
  }

  def "verify that globalSearch works with no results found"() {
    given: 'a mock search rest client and response'
    def mockRestClient = new MockRestClient(method: 'GET', uri: '/_search?q=xyz',
                                            response: [hits: []])
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    when: 'the search is performed'
    def res = searchEngineClient.globalSearch('xyz')

    then: 'the result is correct'
    res.totalHits == 0
  }

  def "verify that globalSearch logs inputs and outputs"() {
    given: 'a mock search rest client and response'
    def mockRestClient = new MockRestClient(method: 'GET', uri: '/_search?q=abc*',
                                            response: [hits: [[code: 'abc1'], [code: 'abc2']]])
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    and: 'a mock logger'
    def mockAppender = MockAppender.mock(SearchEngineClient, Level.DEBUG)

    when: 'the search is performed'
    searchEngineClient.globalSearch('abc*')

    then: 'the inputs and results are logged'
    mockAppender.assertMessageIsValid(['DEBUG', 'GET', '/_search?q=abc*'], 0)
    mockAppender.assertMessageIsValid(['DEBUG', 'result', 'abc1'], 1)
  }

  def "verify that globalSearch logs input JSON content"() {
    given: 'a mock search rest client and response'
    def mockRestClient = new MockRestClient(method: 'GET', uri: '/_search')
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    and: 'a mock logger'
    def mockAppender = MockAppender.mock(SearchEngineClient, Level.DEBUG)

    when: 'the search is performed'
    searchEngineClient.globalSearch('{query: "xyz*"}')

    then: 'the inputs and results are logged'
    mockAppender.assertMessageIsValid(['DEBUG', 'GET', '/_search', 'xyz*'], 0)
  }

  def "verify that globalSearch logs output with a length limited"() {
    given: 'a mock search rest client and response'
    def long1 = 'A' * 1950
    def long2 = 'B' * 50
    def mockRestClient = new MockRestClient(method: 'GET', uri: '/_search?q=abc*',
                                            response: [hits: [[code: long1], [code: long2], [code: 'chopped']]])
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    and: 'a mock logger'
    def mockAppender = MockAppender.mock(SearchEngineClient, Level.DEBUG)

    when: 'the search is performed'
    searchEngineClient.globalSearch('abc*')

    then: 'the inputs and results are logged with the end chopped off'
    mockAppender.messages[1].contains('AAA')
    !mockAppender.messages[1].contains('BBB')
    !mockAppender.messages[1].contains('chopped')
  }

  def "verify that globalSearch logs performance info"() {
    given: 'a mock search rest client and response'
    def mockRestClient = new MockRestClient(method: 'GET', uri: '/_search?q=abc*',
                                            response: [hits: [[code: 'abc1'], [code: 'abc2']]])
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    and: 'a mock logger'
    def mockAppender = MockAppender.mock(SearchEngineClient, Level.INFO)

    when: 'the search is performed'
    searchEngineClient.globalSearch('abc*')

    then: 'the perf info is logged'
    mockAppender.assertMessageIsValid(['INFO', 'elapsed'], 0)
  }

  @Rollback
  def "verify that buildBulkIndexContent works with a list of mixed objects"() {
    given: 'a saved domain object'
    def parent1 = new SampleParent(name: 'ABC1').save()
    def parent2 = new SampleParent(name: 'ABC2').save()
    def sampleParent3 = new SampleParent(name: 'XYZ3').save()
    def list = [parent2, sampleParent3, parent1]

    when: 'the list is converted to the bulk content'
    def s = SearchEngineClient.buildBulkIndexContent(list)

    then: 'the right number of rows is created in the string'
    // Parse the result into JSON rows
    def textRows = s.tokenize(SearchEngineClient.BULK_REQUEST_DELIMITER)
    def jsonRows = []
    for (json in textRows) {
      jsonRows << Holders.objectMapper.readValue(json, Map)
    }
    jsonRows.size() == 6  // Each object has two rows: an action row and the value row.

    and: 'each row pair is correct'
    jsonRows[0].index._index == 'sample-parent'
    jsonRows[0].index._id == parent2.uuid.toString()
    jsonRows[1].name == 'ABC2'
    jsonRows[2].index._index == 'sample-parent'
    jsonRows[2].index._id == sampleParent3.uuid.toString()
    jsonRows[3].name == 'XYZ3'
    jsonRows[4].index._index == 'sample-parent'
    jsonRows[4].index._id == parent1.uuid.toString()
    jsonRows[5].name == 'ABC1'
  }

  @Rollback
  def "verify that buildBulkIndexContent works with strings with embedded new lines"() {
    given: 'a saved domain object'
    def parent1 = new SampleParent(name: 'ABC1', notes: 'DEF\r\nXYZ').save()

    when: 'the list is converted to the bulk content'
    def s = SearchEngineClient.buildBulkIndexContent([parent1])
    //println "s = $s"

    then: 'the right number of rows is created in the string'
    // Parse the result into JSON rows
    def textRows = s.tokenize(SearchEngineClient.BULK_REQUEST_DELIMITER)
    def jsonRows = []
    for (json in textRows) {
      jsonRows << Holders.objectMapper.readValue(json, Map)
    }
    jsonRows.size() == 2  // Each object has two rows: an action row and the value row.

    and: 'the field with the newline can be parsed correctly'
    jsonRows[1].notes == 'DEF\r\nXYZ'
  }

  @Rollback
  def "verify that buildBulkIndexContent works with a list of archived objects"() {
    given: 'some domain objects'
    def parent1 = new SampleParent(name: 'ABC1').save()
    def parent2 = new SampleParent(name: 'ABC2').save()
    def list = [parent1, parent2]

    when: 'the list is converted to the bulk content'
    def s = SearchEngineClient.buildBulkIndexContent(list, ['ref1.arc', 'ref2.arc'])

    then: 'the right number of rows is created in the string'
    // Parse the result into JSON rows
    def textRows = s.tokenize(SearchEngineClient.BULK_REQUEST_DELIMITER)
    def jsonRows = []
    for (json in textRows) {
      jsonRows << Holders.objectMapper.readValue(json, Map)
    }
    jsonRows.size() == 4  // Each object has two rows: an action row and the value row.

    and: 'each row pair is correct'
    jsonRows[0].index._index == 'sample-parent-arc'
    jsonRows[1]._archiveReference == 'ref1.arc'
    jsonRows[2].index._index == 'sample-parent-arc'
    jsonRows[3]._archiveReference == 'ref2.arc'
  }

  @Rollback
  def "verify that buildBulkIndexContent fails if the archiveReference list is not the same size as the object list"() {
    given: 'some domain objects'
    def parent1 = new SampleParent(name: 'ABC1').save()
    def parent2 = new SampleParent(name: 'ABC2').save()
    def list = [parent1, parent2]

    when: 'the list is converted to the bulk content'
    SearchEngineClient.buildBulkIndexContent(list, ['ref1.arc', 'ref2.arc', 'ref3.arc'])

    then: 'an exception is triggered'
    def ex = thrown(Throwable)
    UnitTestUtils.assertExceptionIsValid(ex, ['reference', 'size'])
  }

  @Rollback
  def "verify that bulkIndex works with a list of objects"() {
    given: 'a saved domain object'
    def parent1 = new SampleParent(name: 'ABC1').save()
    def parent2 = new SampleParent(name: 'ABC2').save()
    def sampleParent3 = new SampleParent(name: 'XYZ3').save()
    def list = [parent2, sampleParent3, parent1]

    and: 'a mock search rest client and response'
    def mockRestClient = new MockRestClient(method: 'POST', uri: '/_bulk',
                                            content: SearchEngineClient.buildBulkIndexContent(list),
                                            response: [items: [parent2, sampleParent3, parent1]])
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    when: 'the object is indexed'
    def res = searchEngineClient.bulkIndex(list)

    then: 'indices are create/updated'
    List items = res.items as List
    items.size() == 3
    items[0].index._index == 'sample-parent'
    items[0].index._id == "$parent2.uuid"
    items[0].index.result == "created"
    items[1].index._index == 'sample-parent'
    items[1].index._id == "$sampleParent3.uuid"
    items[1].index.result == "created"
    items[2].index._index == 'sample-parent'
    items[2].index._id == "$parent1.uuid"
    items[2].index.result == "created"
  }

  @Rollback
  def "verify that bulkIndex logs inputs/outputs as debug with limited length contents/response"() {
    given: 'a number of saved domain objects'
    def list = []
    for (i in 1..10) {
      list << new SampleParent(name: 'A' * 20 + i, notes: 'B' * 255).save()
    }

    and: 'a mock logger'
    def mockAppender = MockAppender.mock(SearchEngineClient, Level.DEBUG)

    and: 'a mock search rest client and response'
    def mockRestClient = new MockRestClient(method: 'POST', uri: '/_bulk',
                                            content: SearchEngineClient.buildBulkIndexContent(list),
                                            response: [items: list])
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    when: 'the object is indexed'
    searchEngineClient.bulkIndex(list)

    then: 'the inputs and results are logged with some of it truncated'
    mockAppender.assertMessageIsValid(['DEBUG', 'POST', "/_bulk", "more chars"], 0)
    mockAppender.assertMessageIsValid(['DEBUG', 'took', 'more chars'], 1)
  }

  @Rollback
  def "verify that bulkIndex logs performance as INFO log message"() {
    given: 'a number of saved domain objects'
    def list = [new SampleParent(name: 'ABC').save()]

    and: 'a mock logger'
    def mockAppender = MockAppender.mock(SearchEngineClient, Level.INFO)

    and: 'a mock search rest client and response'
    def mockRestClient = new MockRestClient(method: 'POST', uri: '/_bulk',
                                            content: SearchEngineClient.buildBulkIndexContent(list),
                                            response: [items: list])
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    when: 'the object is indexed'
    searchEngineClient.bulkIndex(list)

    then: 'the inputs and results are logged with some of it truncated'
    mockAppender.assertMessageIsValid(['INFO', 'elapsed'])
  }

  def "verify that getStatus queries the search engine pool for the current number"() {
    given: 'a mock for the pool'
    def queue = Mock(SearchEngineRequestQueue)
    def pool = Mock(SearchEnginePoolExecutor)
    SearchEnginePoolExecutor.pool = pool
    1 * pool.queue >> queue
    1 * queue.size() >> 237

    and: 'mock clients'
    def mockRestClient = new MockRestClient(method: 'GET', uri: "/_cluster/health",
                                            response: [status: 'unknown'])
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    when: 'the status is read'
    def searchStatus = searchEngineClient.status

    then: 'the right queue size is returned'
    searchStatus.pendingRequests == 237

    cleanup:
    SearchEnginePoolExecutor.pool = null
  }

  def "verify that deleteAllIndices works"() {
    given: 'a mock search rest client and response'
    def mockRestClient = new MockRestClient(method: 'DELETE', uri: '/_all')
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    when: 'the indices are delete'
    def res = searchEngineClient.deleteAllIndices()

    then: 'the result is correct'
    res.acknowledged == true
  }

  def "verify that deleteAllIndices logs performance info message"() {
    given: 'a mock search rest client and response'
    def mockRestClient = new MockRestClient(method: 'DELETE', uri: '/_all')
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    and: 'a mock logger'
    def mockAppender = MockAppender.mock(SearchEngineClient, Level.INFO)

    when: 'the indices are delete'
    searchEngineClient.deleteAllIndices()

    then: 'the perf info is logged'
    mockAppender.assertMessageIsValid(['INFO', 'elapsed'])
  }

  @Rollback
  def "verify that removeObjectFromIndex works with simple object - deleted response"() {
    given: 'a saved domain object'
    def parent = new SampleParent(name: 'ABC').save()

    and: 'a mock search rest client and response'
    def mockRestClient = new MockRestClient(method: 'DELETE', uri: SearchEngineClient.buildURIForIndexRequest(parent),
                                            response: [result: 'deleted'])
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    when: 'the object is removed from the index'
    def res = searchEngineClient.removeObjectFromIndex(parent)

    then: 'the result is correct'
    res.result == "deleted"
  }

  @Rollback
  def "verify that removeObjectFromIndex works with simple object - not_found response"() {
    given: 'a saved domain object'
    def parent = new SampleParent(name: 'ABC').save()

    and: 'a mock search rest client and response'
    def mockRestClient = new MockRestClient(method: 'DELETE', uri: SearchEngineClient.buildURIForIndexRequest(parent),
                                            response: [result: 'not_found'])
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    when: 'the object is removed from the index'
    def res = searchEngineClient.removeObjectFromIndex(parent)

    then: 'the result is correct'
    res.result == "deleted"
  }

  @Rollback
  def "verify that removeObjectFromIndex logs inputs and outputs as debug messages"() {
    given: 'a saved domain object'
    def parent = new SampleParent(name: 'ABC').save()

    and: 'a mock search rest client and response'
    def mockRestClient = new MockRestClient(method: 'DELETE', uri: SearchEngineClient.buildURIForIndexRequest(parent),
                                            response: [result: 'deleted'])
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    and: 'a mock logger'
    def mockAppender = MockAppender.mock(SearchEngineClient, Level.DEBUG)

    when: 'the object is removed from the index'
    searchEngineClient.removeObjectFromIndex(parent)

    then: 'the inputs and results are logged'
    mockAppender.assertMessageIsValid(['DEBUG', 'DELETE', SearchEngineClient.buildURIForIndexRequest(parent)], 0)
    mockAppender.assertMessageIsValid(['DEBUG', 'result', 'deleted'], 1)
  }

  @Rollback
  def "verify that removeObjectFromIndex logs performance info message"() {
    given: 'a saved domain object'
    def parent = new SampleParent(name: 'ABC').save()

    and: 'a mock search rest client and response'
    def mockRestClient = new MockRestClient(method: 'DELETE', uri: SearchEngineClient.buildURIForIndexRequest(parent),
                                            response: [result: 'deleted'])
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    and: 'a mock logger'
    def mockAppender = MockAppender.mock(SearchEngineClient, Level.INFO)

    when: 'the object is removed from the index'
    searchEngineClient.removeObjectFromIndex(parent)

    then: 'the perf info is logged'
    mockAppender.assertMessageIsValid(['INFO', 'elapsed'])
  }

  def "verify that domainSearch works with simple query string"() {
    given: 'a mock search rest client and response'
    def uuid1 = UUID.randomUUID()
    def uuid2 = UUID.randomUUID()
    def mockRestClient = new MockRestClient(method: 'GET', uri: '/sample-parent/_search?q=abc*',
                                            response: [took: '13', _index: 'sample-parent',
                                                       hits: [[code: 'abc1', uuid: uuid1], [code: 'abc2', uuid: uuid2]]])
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    when: 'the search is performed'
    def res = searchEngineClient.domainSearch(SampleParent, 'abc*')

    then: 'the result is correct'
    res.totalHits == 2
    res.elapsedTime == 13
    res.hits[0].uuid == uuid1
    res.hits[0].className == SampleParent.name
    res.hits[1].uuid == uuid2
    res.hits[1].className == SampleParent.name
  }

  def "verify that domainSearch works with std toolkit paging"() {
    given: 'a mock search rest client and response'
    def uuid1 = UUID.randomUUID()
    def uuid2 = UUID.randomUUID()
    def mockRestClient = new MockRestClient(method: 'GET', uri: '/sample-parent/_search?q=abc*&size=13&from=23',
                                            response: [took: '13', _index: 'sample-parent',
                                                       hits: [[code: 'abc1', uuid: uuid1], [code: 'abc2', uuid: uuid2]]])
    def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)

    when: 'the search is performed'
    def res = searchEngineClient.domainSearch(SampleParent, 'abc*', [start: 23, count: 13])

    then: 'the result is correct'
    res.totalHits == 2
    res.elapsedTime == 13
    res.hits[0].uuid == uuid1
    res.hits[0].className == SampleParent.name
    res.hits[1].uuid == uuid2
    res.hits[1].className == SampleParent.name
  }

  def "verify that domainSearch fails when domain is not searchable"() {
    when: 'the search is performed'
    new SearchEngineClient().domainSearch(RMA, 'abc*')

    then: 'a valid exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['RMA', 'not', 'searchable'])
  }

  @Rollback
  def "verify that formatForIndex supports list of excluded fields"() {
    given: 'a saved domain object'
    def afd = new AllFieldsDomain(name: 'ABC1', enabled: true).save()

    when: 'the object is formatted'
    def s = new SearchEngineClient().formatForIndex(afd)
    //println "s = s"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"
    def json = Holders.objectMapper.readValue(s, Map)

    then: 'the excluded fields are not in the output'
    json.enabled == null

    and: 'the standard fields are excluded'
    json.dateCreated == null
    json.dateUpdated == null
    json.uuid == null
    json.version == null
    json._complexCustomFields == null
  }

  @Rollback
  def "verify that formatForIndex excludes standard fields when no exclude is used"() {
    given: 'a saved domain object'
    def parent = new SampleParent(name: 'ABC1').save()

    when: 'the object is formatted'
    def s = new SearchEngineClient().formatForIndex(parent)
    //println "s = s"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"
    def json = Holders.objectMapper.readValue(s, Map)

    then: 'the standard fields are excluded'
    json.dateCreated == null
    json.dateUpdated == null
    json.uuid == null
    json.version == null
    json._complexCustomFields == null
  }

  @Rollback
  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that formatForIndex does a deep format with custom child list"() {
    given: 'a domain object with custom values'
    def order = new Order(order: 'ABC')
    order.customComponents << new CustomOrderComponent(sequence: 10, product: 'A1')
    order.customComponents << new CustomOrderComponent(sequence: 20, product: 'A2')
    order.customComponents << new CustomOrderComponent(sequence: 30, product: 'A3')
    order.save()

    when: 'the object is formatted'
    def s = new SearchEngineClient().formatForIndex(order)
    //println "s = $s"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"
    def json = Holders.objectMapper.readValue(s, Map)

    then: 'the JSON contents are correct'
    json.order == "ABC"
    json.customComponents.size() == 3
    json.customComponents[0].uuid == order.customComponents[0].uuid.toString()
    json.customComponents[0].product == 'A1'
    json.customComponents[2].uuid == order.customComponents[2].uuid.toString()
    json.customComponents[2].product == 'A3'
  }

  @Rollback
  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that formatForIndex does a deep format with custom fields"() {
    given: 'a domain object with custom values'
    DataGenerator.buildCustomField(fieldName: 'custom1', domainClass: Order)
    def order = new Order(order: 'ABC')
    order.custom1 = 'XYZ'
    order.save()

    when: 'the object is formatted'
    def s = new SearchEngineClient().formatForIndex(order)
    //println "s = $s"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"
    def json = Holders.objectMapper.readValue(s, Map)

    then: 'the JSON contents are correct'
    json.order == "ABC"
    json._customFields.custom1 == 'XYZ'
  }


}
