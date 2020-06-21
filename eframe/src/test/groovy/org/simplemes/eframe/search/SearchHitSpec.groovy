/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import ch.qos.logback.classic.Level
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.RMA
import sample.domain.SampleParent

/**
 * Tests.
 */
class SearchHitSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  def "verify that constructor handles a search hit from the JSON map"() {
    given: 'a response as a map'
    def uuid = UUID.randomUUID()
    def response = [_id: uuid, _index: 'sample-parent', _source: [name: 'ABC']]

    when: 'the result is built'
    def searchHit = new SearchHit(response)

    then: 'the values are copied correctly'
    searchHit.uuid == uuid
    searchHit.className == 'sample.domain.SampleParent'
  }

  @Rollback
  def "verify that search hit can read the domain class on demand"() {
    given: 'a domain object'
    def sampleParent = new SampleParent(name: 'ABC').save()

    and: 'a response as a map'
    def response = [_id: "${sampleParent.uuid}", _index: 'sample-parent', _source: [name: 'ABC']]

    when: 'the result is built and the object is read'
    def object = new SearchHit(response).object

    then: 'the record is correct'
    object.name == 'ABC'
  }

  @Rollback
  def "verify that search hit provides the correct link HREF and display value for a domain object"() {
    given: 'a domain object'
    def sampleParent = new SampleParent(name: 'ABC', title: 'xyzzy').save()

    and: 'a response as a map'
    def response = [_id: "${sampleParent.uuid}", _index: 'sample-parent', _source: [name: 'ABC']]

    when: 'the result is built'
    def hit = new SearchHit(response)

    then: 'the link HREF is correct'
    hit.link == "/sampleParent/show/${sampleParent.uuid}"

    and: 'the display value is correct'
    hit.displayValue.contains(TypeUtils.toShortString(sampleParent, true))
    hit.displayValue.contains('xyzzy')
    hit.displayValue.contains(SampleParent.simpleName)
  }

  @Rollback
  def "verify that search hit provides the correct link HREF works for all uppercase domain"() {
    given: 'a domain object'
    def rma = new RMA(rma: 'ABC').save()

    and: 'a response as a map'
    def response = [_id: "${rma.uuid}", _index: 'rma', _source: [rma: 'ABC']]

    when: 'the result is built'
    def hit = new SearchHit(response)

    then: 'the link HREF is correct'
    hit.link == "/rma/show/${rma.uuid}"
  }

  @Rollback
  def "verify that the link HREF and display value make sense when the record is not found"() {
    given: 'a domain object and a record that ID that is not indexed'
    def badID = UUID.randomUUID()

    and: 'a response as a map'
    def response = [_id: "${badID}", _index: 'sample-parent', _source: [name: 'ABC']]

    when: 'the result is built'
    def hit = new SearchHit(response)

    then: 'the link HREF is correct'
    hit.link == null

    and: 'the display value is correct'
    //searchMissingRecord.message=Search result ({0}:{1}) not found in database.
    hit.displayValue.contains(GlobalUtils.lookup('searchMissingRecord.message', SampleParent.simpleName, badID))
  }

  def "verify that getObject fails gracefully with unknown class - logs warning"() {
    given: 'a response with invalid class name'
    def response = [_id: UUID.randomUUID(), _index: 'gibberish', _source: [name: 'ABC']]

    and: 'a mock appender for WARN logging'
    def mockAppender = MockAppender.mock(SearchHit, Level.WARN)

    when: 'the result is built'
    def hit = new SearchHit(response)

    then: 'the link HREF is correct'
    hit.link == null

    and: 'the display value is correct'
    //searchUnknownClass.message=Invalid class {0} for search result.
    hit.displayValue.contains(GlobalUtils.lookup('searchUnknownClass.message', null, 'gibberish'))

    and: 'a warning is logged'
    mockAppender.assertMessageIsValid(['class', 'not', 'valid', 'gibberish'])
  }
}
