package org.simplemes.eframe.controller

import io.micronaut.http.HttpStatus
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.test.BaseAPISpecification
import sample.domain.AllFieldsDomain
import sample.domain.SampleParent

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests the controller CRUD actions.
 */
class BaseCrudControllerAPISpec extends BaseAPISpecification {

  static dirtyDomains = [SampleParent, User]

  def "verify that index works in a live server"() {
    when: 'the page is read'
    println "env = ${System.getenv()}"   // TODO: Remove
    login()
    def res = sendRequest(uri: "/sample", method: 'get')

    then: 'the page is correct'
    res.contains('parentGrid')
  }

  // TODO: Remove
  def "verify that index works in a live server - delete"() {
    when: 'the page is read'
    login()
    def res = sendRequest(uri: "/sample", method: 'get')

    then: 'the page is correct'
    res.contains('parentGrid')
  }

  def "verify that index prevents access without the correct role in a live server"() {
    given: 'a user without the MANAGER Role'
    User.withTransaction {
      new User(userName: 'TEST', password: 'TEST').save()
    }

    when: 'the page is read'
    login('TEST', 'TEST')
    sendRequest(uri: "/sample", method: 'get', status: HttpStatus.FORBIDDEN)

    then: 'not error is found'
    notThrown(Throwable)
  }

  def "verify that delete works in a live server with related records"() {
    given: 'a record to delete'
    def record1 = null
    SampleParent.withTransaction {
      record1 = new SampleParent(name: 'SAMPLE', title: 'Sample').save()
    }

    when:
    login()
    sendRequest(uri: "/sample/delete", method: 'post',
                content: [id: record1.id.toString()], status: HttpStatus.FOUND)

    then: 'the record is removed from the DB'
    AllFieldsDomain.withTransaction {
      assert SampleParent.count() == 0
      assert AllFieldsDomain.count() == 0
      true
    }
  }

}
