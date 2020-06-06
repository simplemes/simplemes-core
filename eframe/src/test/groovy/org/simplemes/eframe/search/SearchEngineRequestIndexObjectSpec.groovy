/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import ch.qos.logback.classic.Level
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.SampleParent

/**
 * Tests.
 */
class SearchEngineRequestIndexObjectSpec extends BaseSpecification {

  @SuppressWarnings('unused')
  static specNeeds = SERVER


  @Rollback
  def "verify that the run method logs errors when the response indicates the index was not updated or created"() {
    given: 'a mock client is created that returns a bad result'
    SearchHelper.instance.searchEngineClient = new MockSearchEngineClient(indexObjectResults: [result: "noop"])

    and: 'a domain object'
    def parent = new SampleParent(name: 'ABC').save()

    and: 'a mock appender is used for the stack trace logging'
    def mockAppender = MockAppender.mock(SearchEngineRequestIndexObject, Level.ERROR)

    when: 'an attempt is made to index an object'
    new SearchEngineRequestIndexObject(parent).run()

    then: 'the error result was logged since this is meant to be run in a background thread'
    mockAppender.assertMessageIsValid(['index', 'not', 'noop'])
  }

  @Rollback
  def "verify that the run method does not log an error when the index was created"() {
    given: 'a mock client is created that returns a bad result'
    SearchHelper.instance.searchEngineClient = new MockSearchEngineClient(indexObjectResults: [result: 'created'])

    and: 'a domain object'
    def parent = new SampleParent(name: 'ABC').save()

    and: 'a mock appender is used for the error logging'
    def mockAppender = MockAppender.mock(SearchEngineRequestIndexObject, Level.ERROR)

    when: 'an attempt is made to index an object'
    new SearchEngineRequestIndexObject(parent).run()

    then: 'no message is logged'
    mockAppender.messages.size() == 0
  }

  @Rollback
  def "verify that the run method does not log an error when the index was updated"() {
    given: 'a mock client is created that returns a bad result'
    SearchHelper.instance.searchEngineClient = new MockSearchEngineClient(indexObjectResults: [result: 'updated'])

    and: 'a domain object'
    def parent = new SampleParent(name: 'ABC').save()

    and: 'a mock appender is used for the error logging'
    def mockAppender = MockAppender.mock(SearchEngineRequestIndexObject, Level.ERROR)

    when: 'an attempt is made to index an object'
    new SearchEngineRequestIndexObject(parent).run()

    then: 'no message is logged'
    mockAppender.messages.size() == 0
  }

}
