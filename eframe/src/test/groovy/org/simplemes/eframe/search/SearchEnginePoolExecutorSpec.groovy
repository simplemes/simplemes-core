/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import ch.qos.logback.classic.Level
import org.simplemes.eframe.application.EFrameConfiguration
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.SampleParent

/**
 * Tests.
 */
class SearchEnginePoolExecutorSpec extends BaseSpecification {

  @SuppressWarnings('unused')
  static specNeeds = SERVER

  def setup() {
    SearchEnginePoolExecutor.startPool()
  }

  void cleanup() {
    SearchEnginePoolExecutor.shutdownPool()
    Holders.configuration.search = new EFrameConfiguration.Search()
  }

  @Rollback
  def "verify that the pool can be started and can process events"() {
    given: 'a mock client is created and a domain object is saved'
    SearchHelper.instance.searchEngineClient = new MockSearchEngineClient()
    def sampleParent = new SampleParent(name: 'ABC').save()

    and: 'the search helper counts are reset'
    SearchEnginePoolExecutor.waitForIdle()
    SearchHelper.instance.resetCounts()

    when: 'a request is added'
    SearchEnginePoolExecutor.addRequest(new SearchEngineRequestIndexObject(sampleParent))
    SearchEnginePoolExecutor.waitForIdle()

    then: 'it is processed with no failures'
    SearchHelper.instance.failureCount == 0
    SearchHelper.instance.finishedRequestCount == 1
  }

  def "verify that the pool detects exceptions and logs the error message"() {
    given: 'a mock client is created '
    SearchHelper.instance.searchEngineClient = new MockSearchEngineClient()

    and: 'a mock appender is used for the stack trace logging'
    def mockAppender = MockAppender.mock(SearchEnginePoolExecutor, Level.ERROR)

    and: 'the search helper counts are reset'
    SearchHelper.instance.resetCounts()

    when: 'a request is added'
    SearchEnginePoolExecutor.addRequest(new MockSearchEngineRequest(exceptionMsg: 'An Exception was Triggered'))
    SearchEnginePoolExecutor.waitForIdle()

    then: 'it is processed with a failure'
    SearchHelper.instance.failureCount == 1
    SearchHelper.instance.finishedRequestCount == 1

    and: 'the exception was logged correctly'
    mockAppender.assertMessageIsValid(['An Exception was Triggered'])
  }

  def "verify that the pool counts the request processed"() {
    given: 'a mock client is created '
    SearchHelper.instance.searchEngineClient = new MockSearchEngineClient()

    and: 'the search helper counts are reset'
    SearchHelper.instance.resetCounts()

    when: 'multiple valid requests are added'
    SearchEnginePoolExecutor.addRequest(new MockSearchEngineRequest())
    SearchEnginePoolExecutor.addRequest(new MockSearchEngineRequest())
    SearchEnginePoolExecutor.addRequest(new MockSearchEngineRequest())
    SearchEnginePoolExecutor.waitForIdle()

    then: 'the request count is correct'
    SearchHelper.instance.finishedRequestCount == 3
  }

  def "verify that a large number of requests can be processed properly"() {
    given: 'a mock client is created '
    SearchHelper.instance.searchEngineClient = new MockSearchEngineClient()

    and: 'the search helper counts are reset'
    SearchHelper.instance.resetCounts()

    and: 'a number of request to generate is set'
    def count = 100

    when: 'multiple valid requests are added'
    def start = System.currentTimeMillis()
    for (i in 1..count) {
      SearchEnginePoolExecutor.addRequest(new MockSearchEngineRequest(sleep: 10))
    }
    SearchEnginePoolExecutor.waitForIdle()

    then: 'the request count is correct'
    SearchHelper.instance.finishedRequestCount == count

    and: 'the elapsed time indicates that they were handled in parallel'
    def elapsed = System.currentTimeMillis() - start
    elapsed < (count * 10 / 2)

    and: 'the run action took some amount of time'
    elapsed > (2 * 10)
  }

  def "verify that the determineThreadInitSize works with values from the configuration"() {
    given: 'a configuration with a the coreSize set'
    Holders.configuration.search.threadInitSize = 27

    expect: 'the method returns the correct size'
    SearchEnginePoolExecutor.pool.determineThreadInitSize() == 27
  }

  def "verify that the determineThreadMaxSize works with values from the configuration"() {
    given: 'a configuration with a the maxSize set'
    Holders.configuration.search.threadMaxSize = 37

    expect: 'the method returns the correct size'
    SearchEnginePoolExecutor.pool.determineThreadMaxSize() == 37
  }

}
