/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import ch.qos.logback.classic.Level
import org.simplemes.eframe.application.EFrameConfiguration
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.archive.FileArchiver
import org.simplemes.eframe.archive.domain.ArchiveLog
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.domain.annotation.DomainEntityHelper
import org.simplemes.eframe.domain.annotation.DomainEntityInterface
import org.simplemes.eframe.misc.FileFactory
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.MockFileFactory
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.eframe.web.ui.UIDefaults
import sample.domain.AllFieldsDomain
import sample.domain.RMA
import sample.domain.SampleChild
import sample.domain.SampleParent
import spock.lang.Shared

import java.util.concurrent.FutureTask

/**
 * Tests.
 */
@SuppressWarnings(["ClassSize", "UnnecessaryGetter"])
class SearchHelperSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [ArchiveLog, SampleParent, RMA]

  /**
   * The hosts as set in the config file.  Used to restore after the test modifies them.
   */
  @Shared
  def configuredHosts

  /**
   * The string writer to contain the mocked file contents.
   */
  StringWriter stringWriter


  def setup() {
    configuredHosts = Holders.configuration.search.hosts
    // Make sure the file archiver doesn't use the real file system.
    FileFactory.instance = new MockFileFactory(stringWriter)
    FileFactory.instance = new MockFileFactory()
  }

  def cleanup() {
    Holders.configuration.search = new EFrameConfiguration.Search()
    // Make sure the executor pool is the default
    SearchEnginePoolExecutor.pool = null
    // Reset to File archiver settings to the default. 
    Holders.configuration.archive = new EFrameConfiguration.Archive()
    FileFactory.instance = new FileFactory()
    // Reset to default search helper
    SearchHelper.instance = new SearchHelper()
  }

  /**
   * Build some dummy ArchiveLog records
   * @param count The number of records to build.
   */
  List<ArchiveLog> buildArchiveLogRecords(int count) {
    def res = []
    for (i in 1..count) {
      def uuid = UUID.randomUUID()
      res << new ArchiveLog(recordUUID: uuid, className: 'sample.Dummy', keyValue: "ARCHIVE $i",
                            archiveReference: "folder/ref$i").save()
    }
    return res
  }

  /**
   * Build a list of AllFieldsDomain records with a large number of entries for sorting/paging tests.
   * @param nEntries The number of entries.
   */
  private List buildAllFieldsDomainRecords(int nEntries) {
    def list = []
    for (int i = nEntries; i > 0; i--) {       // Count backwards to get the elements in reverse order.
      def counter = sprintf('%03d', i)
      def qty = (i % 2)
      list << new AllFieldsDomain(name: "ABC${counter}", title: "name${counter}", count: i, dueDate: new DateOnly(),
                                  dateTime: new Date(), qty: qty).save()
    }
    return list
  }

  /**
   * Build a list of SampleParent records with a large number of entries for sorting/paging tests.
   * @param nEntries The number of entries.
   */
  private List buildSampleParentRecords(int nEntries) {
    def list = []
    for (int i = nEntries; i > 0; i--) {       // Count backwards to get the elements in reverse order.
      def counter = sprintf('%03d', i)
      list << new SampleParent(name: "ABC${counter}", title: "name${counter}").save()
    }
    return list
  }

  /**
   * Builds some mock archive file objects with the contents for the nObjects records.  Creates
   * a Parent record archive for each one.  Removes the ArchiveLog records.
   * @param nObjects The number of Parent objects to create.
   */
  def buildMockArchives(int nObjects) {
    Holders.configuration.archive.topFolder = '../archives'
    // Need to use the Mocks to simulate the archiving of some records.
    // We can then grab the XML and use it for simulated unarchive actions later.
    def stringWriter = new StringWriter()
    def mockFileFactory = new MockFileFactory(stringWriter)
    FileFactory.instance = mockFileFactory
    Map contentsMap = [:]
    mockFileFactory.simulatedContents = contentsMap
    def refList = []
    for (i in 1..nObjects) {
      stringWriter = new StringWriter()
      mockFileFactory.stringWriter = stringWriter
      def parent = new SampleParent(name: "ABC_$i").save()
      def archiver = new FileArchiver()
      archiver.archive(parent)
      archiver.close()
      contentsMap["ref${i}.arc"] = stringWriter.toString()
      refList << "ref${i}.arc"
    }
    // Go ahead and delete the records created since the archive does not flush it and the indexing will be triggered for them.
    SampleParent.list().each { it.delete() }
    ArchiveLog.list().each { it.delete() }

    mockFileFactory.simulatedFiles = ['../archives'     : ['../archives/2018'],
                                      '../archives/2018': refList]
  }

  def "verify that the determineHosts works with single host in the configuration"() {
    given: 'a configuration with a single host'
    Holders.configuration.search.hosts = [[host: 'a-host', port: 9200, protocol: 'http']]

    when: 'the host are read from the config'
    def hosts = new SearchHelper().determineHosts()

    then: 'the right host is returned'
    hosts.size() == 1
    hosts[0].hostName == 'a-host'
    hosts[0].schemeName == 'http'
    hosts[0].port == 9200
  }

  def "verify that the determineHosts works with multiple hosts in the configuration"() {
    given: 'a configuration with a single host'
    Holders.configuration.search.hosts = [
      [host: 'a-host', port: 9200, protocol: 'http'],
      [host: 'b-host', port: 9300, protocol: 'https']]

    when: 'the host are read from the config'
    def hosts = new SearchHelper().determineHosts()

    then: 'the right hosts are returned'
    hosts.size() == 2
    hosts[0].hostName == 'a-host'
    hosts[0].schemeName == 'http'
    hosts[0].port == 9200
    hosts[1].hostName == 'b-host'
    hosts[1].schemeName == 'https'
    hosts[1].port == 9300
  }

  def "verify that the determineHosts provides the default port and protocol"() {
    given: 'a configuration with a single host'
    Holders.configuration.search.hosts = [[host: 'a-host']]

    when: 'the host are read from the config'
    def hosts = new SearchHelper().determineHosts()

    then: 'the right host is returned'
    hosts.size() == 1
    hosts[0].hostName == 'a-host'
    hosts[0].schemeName == 'http'
    hosts[0].port == 9200
  }

  def "verify that the determineHosts ignores entries with no host and logs a warning"() {
    given: 'a configuration with a missing host'
    Holders.configuration.search.hosts = [[port: 9200, otherStuff: 'gibberish']]

    and: 'a mock appender for WARN logging'
    def mockAppender = MockAppender.mock(SearchHelper, Level.WARN)

    when: 'the host are read from the config'
    def hosts = new SearchHelper().determineHosts()

    then: 'no host is returned'
    hosts.size() == 0

    and: 'a warning is logged'
    mockAppender.assertMessageIsValid(['determineHosts', 'host', 'not', 'found', 'otherStuff'])
  }

  def "verify that the determineHosts logs an info message when no config is found meaning that external search is disabled"() {
    given: 'a configuration with no external hosts'
    Holders.configuration.search.hosts = []

    and: 'a mock appender for logging'
    def mockAppender = MockAppender.mock(SearchHelper, Level.INFO)

    and: 'a helper'
    def searchHelper = new SearchHelper()

    when: 'the host are read from the config'
    def hosts = searchHelper.determineHosts()

    then: 'no host is returned'
    hosts.size() == 0

    and: 'the right message is logged'
    mockAppender.assertMessageIsValid(['determineHosts', 'no', 'host', 'found', 'engine', 'disabled'])

    and: 'the search feature is disabled'
    searchHelper.searchDisabled
  }

  def "verify that the determineHosts logs an info message when a null config is found"() {
    given: 'a configuration with no external hosts'
    Holders.configuration.search.hosts = null

    and: 'a mock appender for logging'
    def mockAppender = MockAppender.mock(SearchHelper, Level.INFO)

    when: 'the host are read from the config'
    def hosts = new SearchHelper().determineHosts()

    then: 'no host is returned'
    hosts.size() == 0

    and: 'the right message is logged'
    mockAppender.assertMessageIsValid(['determineHosts', 'no', 'host', 'found', 'engine', 'disabled'])
  }

  def "verify that getStatus is delegated to the SearchEngineClient"() {
    given: 'a helper with a mock client'
    def searchHelper = new SearchHelper(searchEngineClient: new MockSearchEngineClient())

    when: 'the mock status is returned'
    def res = searchHelper.status

    then: 'the mock value is returned'
    res.status == 'green'
  }

  def "verify that getStatus adds any bulk index request details to the status"() {
    given: 'a mock for the execution pool'
    def queueList = [new SearchEngineRequestBulkIndex([]), new SearchEngineRequestBulkIndex([]), new SearchEngineRequestBulkIndex([])]
    def queue = Mock(SearchEngineRequestQueue)
    def pool = Mock(SearchEnginePoolExecutor)
    SearchEnginePoolExecutor.pool = pool
    1 * pool.queue >> queue
    //1 * queue.size() >> 9
    queue.iterator() >> queueList.iterator()

    and: 'a helper with the current status set for a bulk request in progress'
    def searchHelper = new SearchHelper()
    searchHelper.bulkIndexStart = System.currentTimeMillis() - 10 * 1000
    searchHelper.bulkIndexEnd = 0
    searchHelper.bulkIndexErrorCount = 4
    searchHelper.bulkIndexStatus = SearchHelper.BULK_INDEX_STATUS_IN_PROGRESS
    searchHelper.bulkIndexRequestCount = 9

    when: 'the mock status is returned'
    def searchStatus = new SearchStatus()
    searchHelper.addBulkIndexStatus(searchStatus)

    then: 'the correct status is set'
    searchStatus.pendingBulkRequests == 3
    searchStatus.totalBulkRequests == 9
    searchStatus.bulkIndexErrorCount == 4
    searchStatus.bulkIndexStatus == SearchHelper.BULK_INDEX_STATUS_IN_PROGRESS
    searchStatus.bulkIndexStart == searchHelper.bulkIndexStart
    searchStatus.bulkIndexEnd == searchHelper.bulkIndexEnd
  }

  def "verify that getStatus sets the configured flag when no hosts are defined"() {
    given: 'no hosts are defined in the configuration'
    Holders.configuration.search.hosts = []

    and: 'a mock appender for WARN logging to reduce console output during tests'
    MockAppender.mock(SearchHelper, Level.WARN)

    when: 'the status is returned'
    def searchStatus = new SearchHelper().status

    then: 'the configured flag is set to not configured'
    !searchStatus.configured
  }

  @Rollback
  def "verify that indexObject is delegated to the SearchEngineClient"() {
    given: 'a saved domain object'
    def parent = new SampleParent(name: 'ABC').save()

    and: 'a helper with a mock client'
    def mockSearchEngineClient = new MockSearchEngineClient()
    def searchHelper = new SearchHelper(searchEngineClient: mockSearchEngineClient)

    when: 'the mock status is returned'
    def res = searchHelper.indexObject(parent)

    then: 'the mock value is returned'
    res.result == 'created'

    and: 'the right remove method was called'
    mockSearchEngineClient.verify([action: 'indexObject', object: parent])
  }

  @Rollback
  def "verify that removeObjectFromIndex is delegated to the SearchEngineClient"() {
    given: 'a saved domain object'
    def parent = new SampleParent(name: 'ABC').save()

    and: 'a helper with a mock client'
    def mockSearchEngineClient = new MockSearchEngineClient()
    def searchHelper = new SearchHelper(searchEngineClient: mockSearchEngineClient)

    when: 'the mock status is returned'
    def res = searchHelper.removeObjectFromIndex(parent)

    then: 'the mock value is returned'
    res.result == 'deleted'

    and: 'the right remove method was called'
    mockSearchEngineClient.verify([action: 'removeObjectFromIndex', object: parent])
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  @Rollback
  def "verify that bulkIndex is delegated to the SearchEngineClient"() {
    given: 'a saved domain object list'
    def list = [new SampleParent(name: 'ABC').save()]

    and: 'a helper with a mock client'
    def searchHelper = new SearchHelper(searchEngineClient: new MockSearchEngineClient())

    when: 'the mock status is returned'
    def res = searchHelper.bulkIndex(list)

    then: 'the mock value is returned'
    res.items[0].index.result == 'created'
  }

  @Rollback
  def "verify that bulkIndex is delegated to the SearchEngineClient - with index suffix"() {
    given: 'a saved domain object list'
    def list = [new SampleParent(name: 'ABC').save()]
    def refList = ['ref1.arc']

    and: 'a helper with a mock client'
    def searchHelper = new SearchHelper(searchEngineClient: new MockSearchEngineClient())

    when: 'the mock status is returned'
    def res = searchHelper.bulkIndex(list, refList)

    then: 'the index suffix was passed to the client'
    res.archiveReferenceList == refList
  }

  def "verify that globalSearch is delegated to the SearchEngineClient"() {
    given: 'a helper with a mock client'
    def searchHelper = new SearchHelper(searchEngineClient: new MockSearchEngineClient(expectedQueryString: 'queryABC'))

    when: 'the mock status is returned'
    def res = searchHelper.globalSearch('queryABC')

    then: 'original query is returned'
    res.query == 'queryABC'
  }

  def "verify that globalSearch is delegated to the SearchEngineClient - with optional params"() {
    given: 'a helper with a mock client'
    def params = [offset: 12, max: 13]
    def searchHelper = new SearchHelper(searchEngineClient: new MockSearchEngineClient(expectedParams: params))

    when: 'the mock status is returned'
    def res = searchHelper.globalSearch('queryABC', [offset: 12, max: 13])

    then: 'original query is returned'
    res.query == 'queryABC'
  }

  def "verify that isSearchable works on domains with and without the searchable"() {
    given: 'a domain compiled with the given searchable property'
    def src = """
      package sample

      import org.simplemes.eframe.domain.annotation.*
      import com.fasterxml.jackson.annotation.JsonFilter
      
      @JsonFilter("searchableFilter")
      @DomainEntity
      class TestClass {
        $searchable
        UUID uuid
      }
    """
    def domainClass = CompilerTestUtils.compileSource(src)

    expect: 'the method determines the correct search ability for various domains and sub-classes'
    SearchHelper.instance.isSearchable(domainClass) == results

    where:
    searchable                                            | results
    'static searchable=true'                              | true
    'static searchable=false'                             | false
    'static searchable=[exclude:["title","releaseDate"]]' | true
    ''                                                    | false
  }

  def "verify that getSearchSettings works on domain classes"() {
    given: 'a domain compiled with the given searchable property'
    def src = """
      package sample.domain

      import org.simplemes.eframe.domain.annotation.*
      import com.fasterxml.jackson.annotation.JsonFilter
      
      @DomainEntity
      @JsonFilter("searchableFilter")
      class TestClass {
        $searchable
        UUID uuid
      }
    """
    def domainClass = CompilerTestUtils.compileSource(src)

    expect: 'the method determines the correct search ability for various domains and sub-classes'
    SearchHelper.instance.getSearchDomainSettings(domainClass) == settings

    where:
    searchable                                                  | settings
    'static searchable=[exclude: ["title","releaseDate"]]'      | new SearchDomainSettings(exclude: ["title", "releaseDate"])
    'static searchable=[parent:SampleParent, searchable:false]' | new SearchDomainSettings(parent: SampleParent, searchable: false)
    'static searchable=true'                                    | new SearchDomainSettings(searchable: true)
    'static searchable=false'                                   | new SearchDomainSettings(searchable: false)
  }

  def "verify that handlePersistenceChange will queue an index request correctly"() {
    given: 'a mock client is created'
    def mockSearchEngineClient = new MockSearchEngineClient()
    SearchHelper.instance.searchEngineClient = mockSearchEngineClient

    and: 'the search executor pool is running'
    SearchEnginePoolExecutor.startPool()

    when: 'the domain is saved'
    def object = null
    SampleParent.withTransaction {
      object = new SampleParent(name: 'ABC').save()
      object = SampleParent.findByUuid(object.uuid)  // Re-read to make sure the objects are exactly the same.
    }

    and: 'the mock is reset to detect the new index request'
    SearchEnginePoolExecutor.waitForIdle()
    mockSearchEngineClient = new MockSearchEngineClient()
    SearchHelper.instance.searchEngineClient = mockSearchEngineClient

    and: 'the handler is called directly to simulate the normal persistence handler logic'
    SearchHelper.instance.handlePersistenceChange((DomainEntityInterface) object)
    SearchEnginePoolExecutor.waitForIdle()

    then: 'the correct action was performed'
    mockSearchEngineClient.verify([action: 'indexObject', object: object])
  }

  def "verify that handlePersistenceChange queues the parent object"() {
    given: 'a mock client is created'
    def mockSearchEngineClient = new MockSearchEngineClient()
    SearchHelper.instance.searchEngineClient = mockSearchEngineClient

    and: 'the search executor pool is running'
    SearchEnginePoolExecutor.startPool()

    and: 'a parent domain that is already saved'
    def sampleParent = null
    SampleParent.withTransaction {
      sampleParent = new SampleParent(name: 'ABC').save()
      sampleParent = SampleParent.findByUuid(sampleParent.uuid)
      // Re-read to make sure the objects are exactly the same.
    }

    when: 'a child record is saved'
    def object = null
    SampleParent.withTransaction {
      def child = new SampleChild(sampleParent: sampleParent, key: 'XYZ').save()
      object = SampleChild.findByUuid(child.uuid)
    }

    and: 'the mock is reset to detect the new index request'
    SearchEnginePoolExecutor.waitForIdle()
    mockSearchEngineClient = new MockSearchEngineClient()
    SearchHelper.instance.searchEngineClient = mockSearchEngineClient

    and: 'the handler is called directly to simulate the normal persistence handler logic'
    SearchHelper.instance.handlePersistenceChange((DomainEntityInterface) object)
    SearchEnginePoolExecutor.waitForIdle()

    then: 'the correct action was performed'
    mockSearchEngineClient.verify([action: 'indexObject', object: SampleParent.findByUuid(sampleParent.uuid)])

    and: 'only the parent is indexed'
    mockSearchEngineClient.actions.size() == 1
  }

  def "verify that handlePersistenceChange will index only the parent when saving both"() {
    given: 'a mock client with a simulated status'
    def mockSearchEngineClient = new MockSearchEngineClient()
    SearchHelper.instance.searchEngineClient = mockSearchEngineClient
    SearchEnginePoolExecutor.waitForIdle()

    and: 'the search executor pool is running'
    SearchEnginePoolExecutor.startPool()

    when: 'the parent is saved with the children'
    SampleParent sampleParent = null
    SampleParent.withTransaction {
      sampleParent = new SampleParent(name: 'ABC')
      sampleParent.sampleChildren << new SampleChild(key: 'XYZ')
      sampleParent.save()
    }
    SearchEnginePoolExecutor.waitForIdle()

    then: 'the correct action was performed'
    mockSearchEngineClient.actions.object.size() == 1
  }

  def "verify that handlePersistenceDelete will remove a object from the index correctly for delete events"() {
    given: 'a mock client is created'
    def mockSearchEngineClient = new MockSearchEngineClient()
    SearchHelper.instance.searchEngineClient = mockSearchEngineClient

    and: 'the search executor pool is running'
    SearchEnginePoolExecutor.startPool()

    when: 'the handler is called directly to simulate the normal persistence handler logic'
    RMA.withTransaction {
      // Save the element here in a txn so we can use the where clause.
      object.save()
    }
    SearchHelper.instance.handlePersistenceDelete((DomainEntityInterface) object)
    SearchEnginePoolExecutor.waitForIdle()

    then: 'the correct action was performed'
    if (action) {
      mockSearchEngineClient.verify([action: 'removeObjectFromIndex', object: object])
    } else {
      mockSearchEngineClient.verify([:])
    }

    where:
    object                        | action
    new SampleParent(name: 'ABC') | 'removeObjectFromIndex'
    new RMA(rma: 'ABC')           | null                     // No index action happened on non-searchable
  }

  def "verify that startBulkIndexRequest begins the bulk index request by deleting all of the current indices"() {
    given: 'a mock client is created'
    def mockSearchEngineClient = new MockSearchEngineClient()
    SearchHelper.instance.searchEngineClient = mockSearchEngineClient

    when: 'bulk index is started'
    SearchHelper.instance.startBulkIndexRequest(true)

    then: 'the bulk delete was called'
    mockSearchEngineClient.verify([action: 'deleteAllIndices'])
  }

  def "verify that startBulkIndexRequest does not delete the indices with the option set to false"() {
    given: 'a mock client is created'
    def mockSearchEngineClient = new MockSearchEngineClient()
    SearchHelper.instance.searchEngineClient = mockSearchEngineClient

    when: 'bulk index is started'
    SearchHelper.instance.startBulkIndexRequest(false)

    then: 'the bulk delete was NOT called'
    mockSearchEngineClient.verify(null)
  }

  def "verify that startBulkIndexRequest works - with transaction created when calling the method"() {
    given: 'a large number of domain records to index'
    SearchHelper.instance = Mock(SearchHelper)
    SearchEnginePoolExecutor.pool = Mock(SearchEnginePoolExecutor)
    SampleParent.withTransaction {
      buildArchiveLogRecords(65)
      buildSampleParentRecords(45)
    }
    SearchHelper.instance = new SearchHelper()

    and: 'a mock for the execution pool that will keep track of the request submitted'
    def requests = []
    def pool = Mock(SearchEnginePoolExecutor)
    SearchEnginePoolExecutor.pool = pool
    3 * pool.submit(_) >> { SearchEngineRequestInterface request ->
      requests << request
      return new FutureTask(request, null)
    }

    and: 'the initial counters are set as if from a previous run'
    SearchHelper.instance.bulkIndexRequestCount = 999
    SearchHelper.instance.bulkIndexErrorCount = 999
    SearchHelper.instance.bulkIndexEnd = 999

    when: 'bulk index is started'
    SampleParent.withTransaction {
      SearchHelper.instance.startBulkIndexRequest(false)
    }

    then: 'the start time is set (to within 1 sec of now)'
    Math.abs(SearchHelper.instance.bulkIndexStart - System.currentTimeMillis()) < 1000

    and: 'the requests are queued'
    requests.size() == 3

    and: 'the current counters and state is set correctly'
    SearchHelper.instance.bulkIndexRequestCount == 3
    SearchHelper.instance.bulkIndexErrorCount == 0
    SearchHelper.instance.bulkIndexFinishedCount == 0
    SearchHelper.instance.bulkIndexEnd == 0
    SearchHelper.instance.bulkIndexStatus == SearchHelper.BULK_INDEX_STATUS_IN_PROGRESS

    and: 'the archiveLog has 2 batches in the list with the right number of unique IDs'
    def archiveLogRequests = requests.findAll { it.domainClass == ArchiveLog }
    archiveLogRequests.size() == 2

    and: 'the ArchiveLog requests have the right number of unique IDs'
    def archiveIds = []
    for (SearchEngineRequestBulkIndex request in (archiveLogRequests as List<SearchEngineRequestBulkIndex>)) {
      archiveIds.addAll(request.domainIDs)
    }
    archiveIds.size() == 65
    archiveIds.clone().unique().size() == archiveIds.size()

    and: 'the Parent has 1 batch in the list with the right number of unique IDs'
    def parentRequests = requests.findAll { it.domainClass == SampleParent }
    parentRequests.size() == 1

    and: 'the Parent requests have the right number of unique IDs'
    def parentIds = []
    for (SearchEngineRequestBulkIndex request in (parentRequests as List<SearchEngineRequestBulkIndex>)) {
      parentIds.addAll(request.domainIDs)
    }
    parentIds.size() == 45
    parentIds.clone().unique().size() == parentIds.size()
  }

  @Rollback
  def "verify that startBulkIndexRequest logs the performance info message"() {
    given: 'a large number of domain records to index'
    buildArchiveLogRecords(15)

    and: 'a mock for the execution pool for the submit method'
    def pool = Mock(SearchEnginePoolExecutor)
    SearchEnginePoolExecutor.pool = pool
    1 * pool.submit(_) >> { SearchEngineRequestInterface request ->
      return new FutureTask(request, null)
    }

    and: 'a mock appender for INFO logging'
    def mockAppender = MockAppender.mock(SearchHelper, Level.INFO)

    when: 'bulk index is started'
    SearchHelper.instance.startBulkIndexRequest(false)

    then: 'the right message is logged'
    mockAppender.assertMessageIsValid(['INFO', 'elapsed'])
  }

  @Rollback
  def "verify that startBulkIndexRequest logs the inputs/outputs for debug message"() {
    given: 'a large number of domain records to index'
    buildArchiveLogRecords(15)

    and: 'a mock for the execution pool for the submit method'
    def pool = Mock(SearchEnginePoolExecutor)
    SearchEnginePoolExecutor.pool = pool
    1 * pool.submit(_) >> { SearchEngineRequestInterface request ->
      return new FutureTask(request, null)
    }

    and: 'a mock appender for the level to test'
    def mockAppender = MockAppender.mock(SearchHelper, Level.DEBUG)

    when: 'bulk index is started'
    SearchHelper.instance.startBulkIndexRequest(false)

    then: 'the right message is logged for the batchSize'
    mockAppender.assertMessageIsValid(['DEBUG', 'building', 'max'], 0)

    and: 'the detail batch message is logged'
    mockAppender.assertMessageIsValid(['DEBUG', 'Created', ArchiveLog.simpleName, '15 objects'], 1)
  }

  @Rollback
  def "verify that startBulkIndexRequest logs the ID details for the trace message"() {
    given: 'a large number of domain records to index'
    def list = buildArchiveLogRecords(15)

    and: 'a mock for the execution pool for the submit method'
    def pool = Mock(SearchEnginePoolExecutor)
    SearchEnginePoolExecutor.pool = pool
    1 * pool.submit(_) >> { SearchEngineRequestInterface request ->
      return new FutureTask(request, null)
    }

    and: 'a mock appender for the level to test'
    def mockAppender = MockAppender.mock(SearchHelper, Level.TRACE)

    when: 'bulk index is started'
    SearchHelper.instance.startBulkIndexRequest(false)

    then: 'the right message is logged for the batchSize'
    mockAppender.assertMessageIsValid(['TRACE', 'IDs', 'indexed'])
    for (archiveLog in list) {
      mockAppender.message.contains(list.uuid.toString())
    }
  }

  @Rollback
  def "verify that startBulkIndexRequest works with archive files"() {
    given: 'some domains to simulate the archive files for - makes 3 batches'
    buildMockArchives(7)
    Holders.configuration.search.bulkBatchSize = 3

    and: 'a mock for the execution pool that will keep track of the request submitted'
    def requests = []
    def pool = Mock(SearchEnginePoolExecutor)
    SearchEnginePoolExecutor.pool = pool
    3 * pool.submit(*_) >> { SearchEngineRequestInterface request ->
      requests << request
      return new FutureTask(request, null)
    }

    when: 'bulk index is started'
    SearchHelper.instance.startBulkIndexRequest(false)

    then: 'the requests are queued'
    requests.size() == 3

    and: 'the request have the right references in the right batches'
    requests[0].archiveRefs == ['2018/ref1.arc', '2018/ref2.arc', '2018/ref3.arc']
    requests[1].archiveRefs == ['2018/ref4.arc', '2018/ref5.arc', '2018/ref6.arc']
    requests[2].archiveRefs == ['2018/ref7.arc']
  }

  @Rollback
  def "verify that startBulkIndexRequest for archives logs the summary as a debug message"() {
    given: 'some domains to simulate the archive files for - makes 1 batches'
    buildMockArchives(3)

    and: 'a mock for the execution pool that will keep track of the request submitted'
    def requests = []
    def pool = Mock(SearchEnginePoolExecutor)
    SearchEnginePoolExecutor.pool = pool
    1 * pool.submit(_) >> { SearchEngineRequestInterface request ->
      requests << request
      return new FutureTask(request, null)
    }

    and: 'a mock appender for the level to test'
    def mockAppender = MockAppender.mock(SearchHelper, Level.DEBUG)

    when: 'bulk index is started'
    SearchHelper.instance.startBulkIndexRequest(false)

    then: 'the summary is logged'
    def message = mockAppender.messages.find { it.contains('build.Archive.Requests') }
    UnitTestUtils.assertContainsAllIgnoreCase(message, ['Created', '3 archived'])
  }

  @Rollback
  def "verify that startBulkIndexRequest for archives logs the detail references as a trace message"() {
    given: 'some domains to simulate the archive files for - makes 1 batches'
    buildMockArchives(3)

    and: 'a mock for the execution pool that will keep track of the request submitted'
    def requests = []
    def pool = Mock(SearchEnginePoolExecutor)
    SearchEnginePoolExecutor.pool = pool
    1 * pool.submit(_) >> { SearchEngineRequestInterface request ->
      requests << request
      return new FutureTask(request, null)
    }

    and: 'a mock appender for the level to test'
    def mockAppender = MockAppender.mock(SearchHelper, Level.TRACE)

    when: 'bulk index is started'
    SearchHelper.instance.startBulkIndexRequest(false)

    then: 'the summary is logged'
    mockAppender.assertMessageIsValid(['references', 'ref1.arc', 'ref2.arc', 'ref3.arc'])
  }

  def "verify that finishedBulkRequest handles the updates correctly"() {
    given: 'a search instance'
    def searchHelper = new SearchHelper()

    when: 'the finished method is called with no errors'
    searchHelper.finishedBulkRequest(0)

    then: 'the counts are correct'
    searchHelper.bulkIndexFinishedCount == 1
    searchHelper.bulkIndexErrorCount == 0

    when: 'the finished method is called with 1 error'
    searchHelper.finishedBulkRequest(1)

    then: 'the counts are correct'
    searchHelper.bulkIndexFinishedCount == 2
    searchHelper.bulkIndexErrorCount == 1

    when: 'the finished method is called with multiple error'
    searchHelper.finishedBulkRequest(5)

    then: 'the counts are correct'
    searchHelper.bulkIndexFinishedCount == 3
    searchHelper.bulkIndexErrorCount == 6
  }

  def "verify that clearStatistics clears the current stats"() {
    given: 'a helper with some stats'
    def searchHelper = new SearchHelper()
    searchHelper.finishedRequest()
    searchHelper.finishedRequest()
    searchHelper.requestFailed()
    searchHelper.bulkIndexStatus = 'inProgress'
    searchHelper.bulkIndexFinishedCount = 237
    searchHelper.bulkIndexErrorCount = 437
    searchHelper.bulkIndexEnd = 637
    searchHelper.bulkIndexStart = 737
    searchHelper.bulkIndexRequestCount = 837

    when: 'the stats are cleared'
    searchHelper.clearStatistics()

    then: 'the values are reset'
    searchHelper.finishedRequestCount == 0
    searchHelper.failureCount == 0
    searchHelper.bulkIndexStatus == ''
    searchHelper.bulkIndexFinishedCount == 0
    searchHelper.bulkIndexErrorCount == 0
    searchHelper.bulkIndexEnd == 0
    searchHelper.bulkIndexStart == 0
    searchHelper.bulkIndexRequestCount == 0
  }

  def "verify that isDomainSearchable detects a non-searchable domain"() {
    given: 'a domain compiled with the given searchable property'
    def src = """
      package sample

      import org.simplemes.eframe.domain.annotation.*
      import com.fasterxml.jackson.annotation.JsonFilter
      
      @DomainEntity
      @JsonFilter("searchableFilter")
      class TestClass {
        $searchable
        UUID uuid
      }
    """
    def domainClass = CompilerTestUtils.compileSource(src)

    and: 'a configuration with a single host'
    Holders.configuration.search.hosts = [[host: 'a-host', port: 9200, protocol: 'http']]

    expect: 'isDomainSearchable works'
    SearchHelper.instance.isDomainSearchable(domainClass) == results

    where:
    searchable                                              | results
    'static searchable=true'                                | true
    'static searchable=false'                               | false
    'static searchable=[exclude: ["title","releaseDate"]]' | true
    ''                                                      | false
  }

  @Rollback
  def "verify that domainSearch on a searchable domain works with a search engine"() {
    given: 'a configuration with a single host to avoid the fallback code'
    Holders.configuration.search.hosts = [[host: 'a-host']]

    and: 'some domain objects'
    def parents = buildSampleParentRecords(2)

    and: 'a mock client with a simulated response'
    def searchResult = new SearchResult([totalHits: 10, elapsedTime: 237], parents)
    def mockSearchEngineClient = new MockSearchEngineClient(domainSearchResult: searchResult,
                                                            expectedDomainClass: SampleParent,
                                                            expectedQueryString: 'queryABC')
    def searchHelper = new SearchHelper(searchEngineClient: mockSearchEngineClient)
    when: 'the mock status is returned'
    def res = searchHelper.domainSearch(SampleParent, 'queryABC')

    then: 'original query is returned'
    res.query == 'queryABC'

    and: 'the result is as expected'
    res.totalHits == 10
    res.hits.size() == 2
    res.hits[0].object == parents[0]
    res.hits[1].object == parents[1]
  }

  @Rollback
  def "verify that domainSearchInDB works with a domain that is not searchable by the search engine"() {
    given: 'some domain objects'
    def list = buildSampleParentRecords(5)

    when: 'the search is performed'
    def res = new SearchHelper().domainSearchInDB(SampleParent, '')

    then: 'original query is returned'
    res.query == ''

    and: 'the result is as expected'
    res.totalHits == list.size()
    res.hits.size() == list.size()
    for (i in 0..(list.size() - 1)) {
      res.hits[i] == list[i]
    }
  }

  @Rollback
  def "verify that domainSearchInDB handles sorting"() {
    given: 'some domain objects'
    def list = buildSampleParentRecords(20)

    and: 'the params for sorting'
    def params = [sort: 'name', order: 'asc']

    when: 'the search is performed'
    def res = new SearchHelper().domainSearchInDB(SampleParent, 'ABC', params)

    then: 'original query is returned'
    res.query == 'ABC'

    and: 'the result is as expected'
    res.totalHits == list.size()
    res.hits.size() == UIDefaults.PAGE_SIZE
    res.hits[0].object.name == 'ABC001'
  }

  @Rollback
  def "verify that domainSearchInDB handles paging"() {
    given: 'some domain objects'
    def list = buildSampleParentRecords(10)

    and: 'the params for sorting'
    def params = [sort: 'name', order: 'asc', from: '1', size: '3']

    when: 'the search is performed'
    def res = new SearchHelper().domainSearchInDB(SampleParent, 'ABC', params)

    then: 'original query is returned'
    res.query == 'ABC'

    and: 'the result is as expected'
    res.totalHits == list.size()
    res.hits.size() == 3
    res.hits[0].object.name == 'ABC004'
  }

  @Rollback
  def "verify that domainSearchInDB handles filtering"() {
    given: 'some domain objects'
    buildSampleParentRecords(5)

    when: 'the search is performed'
    def res = new SearchHelper().domainSearchInDB(SampleParent, 'ABC003')

    then: 'original query is returned'
    res.query == 'ABC003'

    and: 'the result is as expected'
    res.totalHits == 1
    res.hits.size() == 1
    res.hits[0].object.name == 'ABC003'
  }

  @Rollback
  def "verify that domainSearchInDB fails on non-grails class"() {
    when: 'the search is performed'
    new SearchHelper().domainSearchInDB(String, 'ABC003')

    then: 'an exception is triggered'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['String', 'not', 'domain'])
  }

  @Rollback
  def "verify that domainSearchInDB handles post-processing closure and additionalProperties options"() {
    given: 'some domain objects'
    buildAllFieldsDomainRecords(1)

    and: 'a closure to post-process them'
    // Use a closure to adjust the returned record(s)
    def closure = {
      it.notes = 'newNotes'
      it.transientField = 'transient_value'
    }
    def options = [postProcessor: closure, additionalProperties: ['transientField']]

    when: 'the search is performed'
    def res = new SearchHelper().domainSearchInDB(AllFieldsDomain, 'ABC001', [options: options])

    then: 'original query is returned'
    res.hits[0].object.name == 'ABC001'
    res.hits[0].object.notes == 'newNotes'
    res.hits[0].object.transientField == 'transient_value'
  }

  @Rollback
  def "verify that domainSearchInDB logs the performance info message"() {
    given: 'some domain objects'
    buildAllFieldsDomainRecords(10)

    and: 'a mock appender for INFO logging'
    def mockAppender = MockAppender.mock(SearchHelper, Level.INFO)

    when: 'the search is performed'
    new SearchHelper().domainSearchInDB(AllFieldsDomain, 'ABC')

    then: 'the right message is logged'
    mockAppender.assertMessageIsValid(['INFO', 'elapsed', 'AllFieldsDomain', 'records', 'of 10'])
  }

  @Rollback
  def "verify that domainSearch uses fallback when search engine is not available"() {
    given: 'some domain objects'
    buildSampleParentRecords(1)

    and: 'simulate a production environment for the holders check.'
    Holders.simulateProductionEnvironment = true

    and: 'a mock appender for WARN logging'
    def mockAppender = MockAppender.mock(SearchHelper, Level.WARN)

    when: 'the search is performed without a mock search engine'
    def res = new SearchHelper().domainSearch(SampleParent, 'ABC')

    then: 'the records are found'
    res.hits.size() == 1

    and: 'a warning is logged'
    mockAppender.assertMessageIsValid(['eframe.search.hosts', 'application.yml'])
  }

  def "verify that isSimpleQueryString detects the supported cases"() {
    expect: 'the simple/complex test is made correctly'
    SearchHelper.instance.isSimpleQueryString(query) == result

    where:
    query         | result
    'abc'         | true
    'abc('        | false
    'abc)'        | false
    'abc"'        | false
    "abc'"        | false
    'abc or xyz'  | false
    'abc OR xyz'  | false
    'abc and xyz' | false
    'abc AND xyz' | false
  }

  def "verify that clearRequestSent clears the flag"() {
    given: 'a domain with sent flag set'
    def sampleParent = new SampleParent() as DomainEntityInterface
    DomainEntityHelper.instance.setDomainSettingValue(sampleParent, SearchHelper.SETTINGS_SEARCH_REQUEST_SENT, true)
    assert DomainEntityHelper.instance.getDomainSettingValue(sampleParent, SearchHelper.SETTINGS_SEARCH_REQUEST_SENT)

    when: 'the flagged is cleared'
    SearchHelper.instance.clearRequestSent(sampleParent)

    then: 'the setting is cleared'
    DomainEntityHelper.instance.getDomainSettingValue(sampleParent, SearchHelper.SETTINGS_SEARCH_REQUEST_SENT) == false
  }

  def "verify that getDomainClassForIndex handles the supported cases"() {
    expect: 'the method works'
    SearchHelper.instance.getDomainClassForIndex(query) == result

    where:
    query               | result
    'gibberish'         | null
    'rma'               | RMA
    'rma-arc'           | RMA
    'sample-parent'     | SampleParent
    'sample-parent-arc' | SampleParent
  }

  def "verify that getIndexNameForDomain handles the supported cases"() {
    expect: 'the method works'
    SearchHelper.instance.getIndexNameForDomain(domainClass) == result

    where:
    domainClass  | result
    null         | null
    SampleParent | 'sample-parent'
  }

}

