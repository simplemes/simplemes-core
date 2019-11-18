package org.simplemes.eframe.controller

import groovy.json.JsonSlurper
import io.micronaut.http.HttpStatus
import org.simplemes.eframe.test.BaseAPISpecification
import sample.domain.AllFieldsDomain

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class BaseCrudRestControllerAPISpec extends BaseAPISpecification {

  static dirtyDomains = [AllFieldsDomain]

  def "verify that get works in a live server"() {
    given: 'a test record'
    def record = null
    AllFieldsDomain.withTransaction {
      record = new AllFieldsDomain(name: 'ABC', title: 'abc').save()
    }

    when: 'the get is triggered'
    login()
    def s = sendRequest(uri: "/allFieldsDomain/crud/${record.id}")

    then: 'the JSON is valid'
    def json = new JsonSlurper().parseText(s)
    json.name == 'ABC'
    json.title == 'abc'
  }

  def "verify that post works in a live server"() {
    given: 'the source JSON'
    def src = """
      {
        "name" : "ABC-021",
        "title" : "abc-001",
        "qty" : 1221.00,
        "count" : 3333,
        "enabled" : false,
        "dateTime" : "2018-11-13T12:41:30.000-0500",
        "dueDate" : "2010-07-05"
      }
      """

    when:
    login()
    def s = sendRequest(uri: "/allFieldsDomain/crud", method: 'post', content: src)

    then: 'the record is created in the DB'
    def record = null
    AllFieldsDomain.withTransaction {
      record = AllFieldsDomain.findByName('ABC-021')
    }
    record != null

    then: 'the JSON is valid'
    def json = new JsonSlurper().parseText(s)
    json.name == 'ABC-021'
    json.id == record.id
  }

  def "verify that put works in a live server"() {
    given: 'a record to update'
    def record1 = null
    AllFieldsDomain.withTransaction {
      record1 = new AllFieldsDomain(name: 'ABC-021').save()
    }

    and: 'the source JSON'
    def src = """
      {
        "name" : "ABC-021",
        "title" : "abc-001",
        "qty" : 1221.00,
        "count" : 3333,
        "enabled" : false,
        "dateTime" : "2018-11-13T12:41:30.000-0500",
        "dueDate" : "2010-07-05"
      }
      """

    when:
    login()
    def s = sendRequest(uri: "/allFieldsDomain/crud/${record1.id}", method: 'put', content: src)

    then: 'the record is created in the DB'
    def record = null
    AllFieldsDomain.withTransaction {
      record = AllFieldsDomain.findByName('ABC-021')
      true
    }
    record != null
    record.title == 'abc-001'

    then: 'the JSON is valid'
    def json = new JsonSlurper().parseText(s)
    json.name == 'ABC-021'
    json.id == record.id
  }

  def "verify that delete works in a live server"() {
    given: 'a record to delete'
    def record1 = null
    AllFieldsDomain.withTransaction {
      record1 = new AllFieldsDomain(name: 'ABC-021').save()
    }

    when:
    login()
    sendRequest(uri: "/allFieldsDomain/crud/${record1.id}", method: 'delete', status: HttpStatus.NO_CONTENT)

    then: 'the record is created in the DB'
    def record = null
    AllFieldsDomain.withTransaction {
      record = AllFieldsDomain.findByName('ABC-021')
      true
    }
    record == null
  }
}
