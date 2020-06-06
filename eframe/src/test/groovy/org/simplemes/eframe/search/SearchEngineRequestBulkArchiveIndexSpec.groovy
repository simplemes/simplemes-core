/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import ch.qos.logback.classic.Level
import org.simplemes.eframe.application.EFrameConfiguration
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.archive.FileArchiver
import org.simplemes.eframe.archive.domain.ArchiveLog
import org.simplemes.eframe.misc.FileFactory
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.MockFileFactory
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.SampleParent

/**
 * Tests.
 */
class SearchEngineRequestBulkArchiveIndexSpec extends BaseSpecification {

  @SuppressWarnings('unused')
  static specNeeds = SERVER

  void cleanup() {
    // Reset to File archiver settings to the default.
    FileFactory.instance = new FileFactory()
    Holders.configuration.archive = new EFrameConfiguration.Archive()
    SearchHelper.instance = new SearchHelper()
  }

  /**
   * Builds some mock archive file objects with the contents for the nObjects records.  Creates
   * a Parent record archive for each one.  Removes the ArchiveLog records.
   * @param nObjects The number of Parent objects to create.
   */
  Tuple2<List<String>, List> buildMockArchives(int nObjects) {
    Holders.configuration.archive.topFolder = '../archives'
    // Need to use the Mocks to simulate the archiving of some records.
    // We can then grab the XML and use it for simulated unarchive actions later.
    def stringWriter = new StringWriter()
    def mockFileFactory = new MockFileFactory(stringWriter)
    FileFactory.instance = mockFileFactory
    Map contentsMap = [:]
    mockFileFactory.simulatedContents = contentsMap
    def refList = []
    def objectList = []
    for (i in 1..nObjects) {
      stringWriter = new StringWriter()
      mockFileFactory.stringWriter = stringWriter
      def parent = new SampleParent(name: "ABC_$i").save()
      def archiver = new FileArchiver()
      archiver.archive(parent)
      archiver.close()
      contentsMap["../archives/ref${i}.arc"] = stringWriter.toString()
      refList << "ref${i}.arc"
      objectList << parent
    }
    // Go ahead and delete the records created since the archive does not flush it and the indexing will be triggered for them.
    SampleParent.list().each { it.delete() }
    ArchiveLog.list().each { it.delete() }

    mockFileFactory.simulatedFiles = ['../archives'     : ['../archives/2018'],
                                      '../archives/2018': refList]
    return new Tuple2(refList, objectList)
  }

  @Rollback
  def "verify that the run method indexes multiple objects"() {
    given: 'a number of archived domains to process'
    def (List refList, objects) = buildMockArchives(3)

    and: 'a mock client that simulates the index action'
    def mockSearchEngineClient = new MockSearchEngineClient()
    SearchHelper.instance.searchEngineClient = mockSearchEngineClient

    and: 'a mock appender is used for the error logging'
    def mockAppender = MockAppender.mock(SearchEngineRequestBulkIndex, Level.ERROR)

    when: 'an attempt is made to index an object'
    new SearchEngineRequestBulkArchiveIndex(refList).run()

    then: 'no message is logged'
    mockAppender.messages.size() == 0

    and: 'the bulk action is performed'
    //println "objects = ${objects*.name}"
    mockSearchEngineClient.verify([action: 'bulkIndex', object: objects])

    and: 'no domain records are created as a side-effect'
    SampleParent.list().size() == 0
  }

  @Rollback
  def "verify that the run detects a missing response one one item"() {
    given: 'a number of archived domains to process'
    //noinspection GroovyUnusedAssignment
    def (List refList, objects) = buildMockArchives(3)

    and: 'a mock client that simulates the index action with just one response'
    def bulkResults = [[index: [_index: 'good', result: 'created']]]
    SearchHelper.instance.searchEngineClient = new MockSearchEngineClient(indexObjectResults: [items: bulkResults])

    and: 'a mock appender is used for the error logging'
    def mockAppender = MockAppender.mock(SearchEngineRequestBulkArchiveIndex, Level.ERROR)

    when: 'an attempt is made to index an object'
    new SearchEngineRequestBulkArchiveIndex(refList).run()

    then: 'a message is logged'
    mockAppender.assertMessageIsValid(['index', 'not', 'created', 'found 1', 'expected 3'])
  }

  @Rollback
  def "verify that the run detects an error in the response"() {
    given: 'a number of archived domains to process'
    //noinspection GroovyUnusedAssignment
    def (List refList, objects) = buildMockArchives(3)

    and: 'a mock client that simulates the index action with just one response'
    SearchHelper.instance.searchEngineClient = new MockSearchEngineClient(bulkIndexResult: ['created', 'failedOne', 'updated'])

    and: 'a mock appender is used for the error logging'
    def mockAppender = MockAppender.mock(SearchEngineRequestBulkArchiveIndex, Level.ERROR)

    when: 'an attempt is made to index an object'
    new SearchEngineRequestBulkArchiveIndex(refList).run()

    then: 'a message is logged'
    mockAppender.assertMessageIsValid(['index', 'not', 'created', 'failedOne'])
  }

  @Rollback
  def "verify that the run finishes and notifies the SearchHelper it finished with no errors"() {
    given: 'an archived domain to process'
    def (List refList, objects) = buildMockArchives(1)

    and: 'a mock client that simulates the index action'
    def searchHelper = Mock(SearchHelper)
    SearchHelper.instance = searchHelper

    when: 'an attempt is made to index the object'
    new SearchEngineRequestBulkArchiveIndex(refList).run()

    then: 'the search helper was notified'
    1 * searchHelper.finishedBulkRequest(0)
    1 * searchHelper.bulkIndex(objects, refList) >> [items: [[index: [result: 'created']]]]
    _ * searchHelper.isSearchable(_) >> true
  }

  @Rollback
  def "verify that the run detects a failed index and notifies the SearchHelper it finished"() {
    given: 'an archived domain to process'
    def (List refList, objects) = buildMockArchives(1)

    and: 'a mock client that simulates the index action'
    def searchHelper = Mock(SearchHelper)
    SearchHelper.instance = searchHelper

    and: 'a mock appender to reduce output to console'
    MockAppender.mock(SearchEngineRequestBulkArchiveIndex, Level.ERROR)

    when: 'an attempt is made to index the object'
    new SearchEngineRequestBulkArchiveIndex(refList).run()

    then: 'the search helper was notified'
    1 * searchHelper.finishedBulkRequest(1)
    1 * searchHelper.bulkIndex(objects, refList) >> [items: [[index: [result: 'failed']]]]
    _ * searchHelper.isSearchable(_) >> true
  }

  @Rollback
  def "verify that the run detects an exception and notifies the SearchHelper and logs the content as a TRACE log message"() {
    given: 'an archived domain to process'
    def (List refList, objects) = buildMockArchives(1)

    and: 'a mock client that simulates the index action'
    def searchHelper = Mock(SearchHelper)
    SearchHelper.instance = searchHelper

    and: 'a mock appender to capture the log message'
    def mockAppender = MockAppender.mock(SearchEngineRequestBulkArchiveIndex, Level.TRACE)

    when: 'an attempt is made to index an object'
    new SearchEngineRequestBulkArchiveIndex(refList).run()

    then: 'the exception is re-thrown'
    def ex = thrown(Exception)
    ex.toString().contains('bad')

    and: 'the search helper was notified'
    1 * searchHelper.finishedBulkRequest(1)
    1 * searchHelper.bulkIndex(objects, refList) >> { throw new IllegalArgumentException('bad exception') }
    _ * searchHelper.isSearchable(objects[0].class) >> true
    _ * searchHelper.getSearchDomainSettings(objects[0].class) >> new SearchDomainSettings()

    and: 'the exception is logged'
    mockAppender.assertMessageIsValid(['bad exception', 'parent', 'abc_1', '"_index"'])
  }

}
