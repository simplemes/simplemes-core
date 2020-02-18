/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.controller

import groovy.json.JsonSlurper
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.date.ISODate
import org.simplemes.eframe.test.BaseAPISpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.MockSecurityUtils
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.eframe.web.report.ReportTimeIntervalEnum
import sample.controller.AllFieldsDomainController
import sample.controller.OrderController
import sample.controller.SampleParentController
import sample.domain.AllFieldsDomain
import sample.domain.CustomOrderComponent
import sample.domain.Order
import sample.domain.SampleChild
import sample.domain.SampleParent

/**
 * Tests.
 */
class BaseCrudRestControllerSpec extends BaseAPISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [SampleParent, AllFieldsDomain]

  Class<SampleParentController> buildSampleParentController() {
    def src = """package sample
      import org.simplemes.eframe.controller.BaseCrudRestController
      import groovy.util.logging.Slf4j
      import io.micronaut.http.HttpResponse
      import javax.annotation.Nullable
      import java.security.Principal
      import io.micronaut.security.annotation.Secured

      @Slf4j
      @Secured("isAnonymous()")
      class SampleParentController extends BaseCrudRestController {
      }
    """
    return CompilerTestUtils.compileSource(src)
  }

  Class<AllFieldsDomainController> buildAllFieldsDomainController() {
    def src = """package sample
      import org.simplemes.eframe.controller.BaseCrudRestController
      import groovy.util.logging.Slf4j
      import io.micronaut.http.HttpResponse
      import javax.annotation.Nullable
      import java.security.Principal
      import io.micronaut.security.annotation.Secured

      @Slf4j
      @Secured("isAnonymous()")
      class AllFieldsDomainController extends BaseCrudRestController {
      }
    """
    return CompilerTestUtils.compileSource(src)
  }

  Class<OrderController> buildOrderController() {
    def src = """package sample
      import org.simplemes.eframe.controller.BaseCrudRestController
      import groovy.util.logging.Slf4j
      import io.micronaut.http.HttpResponse
      import javax.annotation.Nullable
      import java.security.Principal
      import io.micronaut.security.annotation.Secured

      @Slf4j
      @Secured("isAnonymous()")
      class OrderController extends BaseCrudRestController {
      }
    """
    return CompilerTestUtils.compileSource(src)
  }


  @Rollback
  def "verify restGet works for basic case "() {
    given: 'a controller for the base class'
    Class clazz = buildAllFieldsDomainController()

    and: 'some test data is created'
    DataGenerator.generate {
      domain AllFieldsDomain
      values name: 'ABC', title: 'abc', reportTimeInterval: ReportTimeIntervalEnum.YESTERDAY
    }

    when: 'the get is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restGet('ABC', null)

    then: 'the JSON is valid'
    res.status() == HttpStatus.OK
    def json = new JsonSlurper().parseText((String) res.body())
    json.name == 'ABC'
    json.title == 'abc'
    json.reportTimeInterval == ReportTimeIntervalEnum.YESTERDAY.toString()
  }

  @Rollback
  def "verify restGet works with record ID"() {
    given: 'a controller for the base class for SampleParent'
    def controller = buildSampleParentController().newInstance()

    and: 'some test data is created'
    def record = new SampleParent(name: 'ABC1', title: 'abc').save()

    when: 'the get is called'
    HttpResponse res = controller.restGet(record.uuid.toString(), null)

    then: 'the JSON is valid'
    res.status() == HttpStatus.OK
    def json = new JsonSlurper().parseText((String) res.body())
    json.name == 'ABC1'
    json.title == 'abc'
  }

  @Rollback
  def "verify restGet works with records with child proxy records"() {
    given: 'a controller for the base class for SampleParent'
    Class clazz = buildSampleParentController()

    and: 'some test data is created'
    def record = null
    SampleParent.withTransaction {
      record = new SampleParent(name: 'ABC', title: 'abc')
      record.sampleChildren << (new SampleChild(key: 'C1'))
      record.sampleChildren << (new SampleChild(key: 'C2'))
      record.sampleChildren << (new SampleChild(key: 'C3'))
      record.save()
    }

    when: 'the get is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restGet(record.uuid.toString(), null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.body())}"

    then: 'the JSON is valid'
    res.status() == HttpStatus.OK
    def json = new JsonSlurper().parseText((String) res.body())
    json.name == 'ABC'
    json.title == 'abc'

    and: 'the children are valid'
    List children = json.sampleChildren
    children.size() == 3
    children[0].key == 'C1'
    children[1].key == 'C2'
    children[2].key == 'C3'
  }

  @Rollback
  def "verify restGet works with custom fields"() {
    given: 'a controller for the base class'
    Class clazz = buildAllFieldsDomainController()

    and: 'a custom field for the domain'
    DataGenerator.buildCustomField(fieldName: 'custom1', domainClass: AllFieldsDomain)

    and: 'some test data is created'
    def allFieldsDomain = new AllFieldsDomain(name: 'ABC')
    allFieldsDomain.setFieldValue('custom1', 'xyzzy')
    allFieldsDomain.save()

    when: 'the get is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restGet('ABC', null)

    then: 'the JSON is valid'
    res.status() == HttpStatus.OK
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.body())}"
    def json = new JsonSlurper().parseText((String) res.body())
    json.custom1 == 'xyzzy'
  }

  @Rollback
  def "verify restGet works with custom child list field added via addition"() {
    given: 'a controller for the base class'
    Class clazz = buildOrderController()

    and: 'some test data is created'
    def order = new Order(order: 'M1001')
    def customComponents = []
    customComponents << new CustomOrderComponent(sequence: 1, product: 'PROD1')
    customComponents << new CustomOrderComponent(sequence: 2, product: 'PROD2')
    customComponents << new CustomOrderComponent(sequence: 3, product: 'PROD3')
    order.setFieldValue('customComponents', customComponents)
    order.save()

    when: 'the get is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restGet('M1001', null)

    then: 'the JSON is valid'
    res.status() == HttpStatus.OK
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.body())}"

    def json = new JsonSlurper().parseText((String) res.body())
    List customComponents2 = json.customComponents
    customComponents2.size() == 3
    customComponents2[0].sequence == 1
    customComponents2[0].product == 'PROD1'
    customComponents2[1].sequence == 2
    customComponents2[1].product == 'PROD2'
    customComponents2[2].sequence == 3
    customComponents2[2].product == 'PROD3'
  }

  @Rollback
  def "verify restGet gracefully handles record not found case"() {
    given: 'a controller for the base class for SampleParent'
    Class clazz = buildSampleParentController()

    when: 'the get is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restGet('1111', null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.body())}"

    then: 'the response is correct'
    res.status() == HttpStatus.NOT_FOUND
  }

  @Rollback
  def "verify restGet gracefully handles ID is not a UUID"() {
    given: 'a controller for the base class for SampleParent'
    Class clazz = buildSampleParentController()

    when: 'the get is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restGet('gibberish', null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.body())}"

    then: 'the JSON is valid'
    res.status() == HttpStatus.NOT_FOUND
  }

  @Rollback
  def "verify restGet gracefully handles ID missing"() {
    given: 'a controller for the base class for SampleParent'
    Class clazz = buildSampleParentController()

    when: 'the get is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restGet('', null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.body())}"

    then: 'the JSON is valid'
    res.status() == HttpStatus.NOT_FOUND
  }

  @Rollback
  def "verify restGet checks for controller-level secured annotation and fails when user has wrong permissions"() {
    given: 'a controller'
    Object controller = buildAllFieldsDomainController().newInstance()

    and: 'a mocked security utils that will fail'
    new MockSecurityUtils(this, HttpStatus.FORBIDDEN).install()

    when: 'the list is called from the controller'
    def res = controller.restGet('1', null)

    then: 'the correct values are returned'
    res.status == HttpStatus.FORBIDDEN
  }


  @Rollback
  def "verify restPost can create simple record"() {
    given: 'a controller for the base class for a domain'
    Class clazz = buildAllFieldsDomainController()

    and: 'the source JSON'
    def src = """
      {
        "name" : "ABC-021",
        "title" : "abc-001",
        "qty" : 1221.00,
        "count" : 3333,
        "enabled" : false,
        "dateTime" : "2018-11-13T12:41:30.000-0500",
        "dueDate" : "2010-07-05",
        "reportTimeInterval" : "YESTERDAY"
      }
      """

    when: 'the post is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restPost(mockRequest([body: src]), null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.body())}"

    then: 'the JSON is valid'
    res.status() == HttpStatus.OK

    and: 'the record is created in the DB'
    AllFieldsDomain record = AllFieldsDomain.findByName('ABC-021')
    record.name == 'ABC-021'
    record.title == 'abc-001'
    record.qty == 1221.0
    record.count == 3333
    !record.enabled
    record.dateTime == ISODate.parse("2018-11-13T12:41:30.000-0500")
    record.dueDate == ISODate.parseDateOnly("2010-07-05")
    record.reportTimeInterval == ReportTimeIntervalEnum.YESTERDAY

    and: 'the JSON is valid'
    def json = new JsonSlurper().parseText((String) res.getBody().get())
    json.name == record.name
    json.title == record.title
    json.qty == record.qty
    json.count == record.count
    json.enabled == record.enabled
    ISODate.parseDateOnly((String) json.dueDate) == record.dueDate
    ISODate.parse((String) json.dateTime) == record.dateTime
    json.uuid == record.uuid.toString()
  }

  @Rollback
  def "verify restPost ignores the UUID passed in"() {
    given: 'a controller for the base class for a domain'
    Class clazz = buildAllFieldsDomainController()
    def uuid = UUID.fromString("bcf50818-cbd9-45d5-b6c0-146332e19045")

    and: 'the source JSON'
    def src = """
      {
        "name" : "ABC-021",
        "uuid" : "${uuid}"
      }
      """

    when: 'the post is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restPost(mockRequest([body: src]), null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.body())}"

    then: 'the JSON is valid'
    res.status() == HttpStatus.OK

    and: 'the record is created in the DB'
    AllFieldsDomain record = AllFieldsDomain.findByName('ABC-021')
    record.uuid != uuid
  }

  @Rollback
  def "verify restPost can create a record with child records"() {
    given: 'a controller for the base class for a domain'
    Class clazz = buildSampleParentController()

    and: 'the source JSON'
    def src = """
      {
        "name" : "ABC-021",
        "sampleChildren" : [
          {"key" : "C1"},
          {"key" : "C3"},
          {"key" : "C2"}
        ]
      }
      """


    when: 'the post is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restPost(mockRequest([body: src]), null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.body())}"

    then: 'the JSON is valid'
    res.status() == HttpStatus.OK

    and: 'the record is created in the DB'
    SampleParent record = SampleParent.findByName('ABC-021')
    record.name == 'ABC-021'

    and: 'the child records are correct'
    def children = record.sampleChildren
    children.size() == 3
    children[0].key == 'C1'
    children[1].key == 'C3'
    children[2].key == 'C2'

    and: 'the JSON is valid'
    def json = new JsonSlurper().parseText((String) res.getBody().get())
    json.name == record.name

    List jsonChildren = json.sampleChildren
    jsonChildren.size() == 3
    jsonChildren[0].key == 'C1'
    jsonChildren[1].key == 'C3'
    jsonChildren[2].key == 'C2'
  }

  @Rollback
  def "verify restPost can create a record with custom fields"() {
    given: 'a controller for the base class for a domain'
    Class clazz = buildAllFieldsDomainController()

    and: 'a custom field for the domain'
    DataGenerator.buildCustomField(fieldName: 'custom1', domainClass: AllFieldsDomain)

    and: 'the source JSON'
    def src = """
      {
        "name" : "ABC-021",
        "title" : "abc-001",
        "custom1" : "custom_abc"
      }
      """

    when: 'the post is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restPost(mockRequest([body: src]), null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.body())}"

    then: 'the JSON is valid'
    res.status() == HttpStatus.OK

    and: 'the record is created in the DB'
    AllFieldsDomain record = AllFieldsDomain.findByName('ABC-021')
    record.getFieldValue('custom1') == 'custom_abc'

    and: 'the JSON is valid'
    def record2 = Holders.objectMapper.readValue((String) res.getBody().get(), AllFieldsDomain)
    record2.getFieldValue('custom1') == 'custom_abc'
  }

  @Rollback
  def "verify restPost can create a record with custom child records"() {
    given: 'a controller for the base class for a domain'
    Class clazz = buildOrderController()

    and: 'the source JSON'
    def src = """
      {
        "order" : "ABC-021",
        "customComponents" : [
          {"sequence" : 1, "product" : "C1"},
          {"sequence" : 2, "product" : "C2"},
          {"sequence" : 3, "product" : "C3"}
        ]
      }
    """

    when: 'the post is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restPost(mockRequest([body: src]), null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.body())}"

    then: 'the JSON is valid'
    res.status() == HttpStatus.OK

    and: 'the record is created in the DB'
    Order record = Order.findByOrder('ABC-021')
    record.order == 'ABC-021'

    and: 'the child records are correct'
    List<CustomOrderComponent> customComponents = record.getFieldValue('customComponents') as List<CustomOrderComponent>
    customComponents.size() == 3
    customComponents[0].sequence == 1
    customComponents[0].product == 'C1'
    customComponents[1].sequence == 2
    customComponents[1].product == 'C2'
    customComponents[2].sequence == 3
    customComponents[2].product == 'C3'

    and: 'the JSON is valid'
    def json = new JsonSlurper().parseText((String) res.getBody().get())
    json.order == record.order

    List jsonChildren = json.customComponents
    jsonChildren.size() == 3
    jsonChildren[0].sequence == 1
    jsonChildren[0].product == 'C1'
    jsonChildren[1].sequence == 2
    jsonChildren[1].product == 'C2'
    jsonChildren[2].sequence == 3
    jsonChildren[2].product == 'C3'
  }

  @Rollback
  def "verify restPost fails with validation errors"() {
    given: 'a controller for the base class for a domain'
    Class clazz = buildAllFieldsDomainController()

    and: 'the source JSON'
    def src = """
      {
        "name" : "ABC-021",
        "count" : 1000001
      }
      """

    when: 'the post is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restPost(mockRequest([body: src]), null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.body())}"

    then: 'the create fails'
    res.status() == HttpStatus.BAD_REQUEST

    and: 'the JSON is has the correct messages'
    def json = new JsonSlurper().parseText((String) res.getBody().get())
    json.message.text.contains('count')
    json.message.text.contains('000')
  }

  @Rollback
  def "verify restPost fails with no request body"() {
    given: 'a controller for the base class for a domain'
    Class clazz = buildAllFieldsDomainController()

    when: 'the post is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restPost(mockRequest([:]), null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.body())}"

    then: 'the create fails'
    res.status() == HttpStatus.BAD_REQUEST

    and: 'the JSON is has the correct messages'
    def json = new JsonSlurper().parseText((String) res.getBody().get())
    UnitTestUtils.assertContainsAllIgnoreCase(json.message.text, ['empty', 'body'])
  }

  @Rollback
  def "verify restPost checks for controller-level secured annotation and fails when user has wrong permissions"() {
    given: 'a controller'
    Object controller = buildAllFieldsDomainController().newInstance()

    and: 'a mocked security utils that will fail'
    new MockSecurityUtils(this, HttpStatus.FORBIDDEN).install()

    when: 'the list is called from the controller'
    def res = controller.restPost(mockRequest(), null)

    then: 'the correct values are returned'
    res.status == HttpStatus.FORBIDDEN
  }

  @Rollback
  def "verify restPut can update a simple record"() {
    given: 'a controller for the base class for a domain'
    Class clazz = buildAllFieldsDomainController()

    and: 'an existing record'
    def record1 = new AllFieldsDomain(name: 'ABC-021').save()

    and: 'the source JSON'
    def src = """
      {
        "title" : "abc-001",
        "qty" : 1221.00,
        "count" : 3333,
        "enabled" : false,
        "dateTime" : "2018-11-13T12:41:30.000-0500",
        "dueDate" : "2010-07-05"
      }
      """

    when: 'the put is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restPut(record1.uuid.toString(), mockRequest([body: src]), null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.getBody().get())}"

    then: 'the JSON is valid'
    res.status() == HttpStatus.OK

    and: 'the record is updated in the DB'
    AllFieldsDomain record = AllFieldsDomain.findByName('ABC-021')
    record.name == 'ABC-021'
    record.title == 'abc-001'
    record.qty == 1221.0
    record.count == 3333
    !record.enabled
    record.dateTime == ISODate.parse("2018-11-13T12:41:30.000-0500")
    record.dueDate == ISODate.parseDateOnly("2010-07-05")
    record != null

    and: 'the JSON is valid'
    def json = new JsonSlurper().parseText((String) res.getBody().get())
    json.name == record.name
    json.title == record.title
    json.qty == record.qty
    json.count == record.count
    json.enabled == record.enabled
    ISODate.parseDateOnly((String) json.dueDate) == record.dueDate
    ISODate.parse((String) json.dateTime) == record.dateTime
    json.uuid == record.uuid.toString()
  }

  @Rollback
  def "verify restPut will ignore a UUID changed on update"() {
    given: 'a controller for the base class for a domain'
    Class clazz = buildAllFieldsDomainController()

    and: 'an existing record'
    def record1 = new AllFieldsDomain(name: 'ABC-021').save()

    and: 'the source JSON'
    def src = """
      {
        "title" : "abc-001",
        "uuid" : "bcf50818-cbd9-45d5-b6c0-146332e19045"
      }
      """

    when: 'the put is called to update by name'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restPut(record1.name, mockRequest([body: src]), null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.getBody().get())}"

    then: 'the JSON is valid'
    res.status() == HttpStatus.OK

    and: 'the record is updated in the DB'
    AllFieldsDomain record = AllFieldsDomain.findByName('ABC-021')
    record.title == 'abc-001'
    record.uuid == record1.uuid

    and: 'the JSON is valid'
    def json = new JsonSlurper().parseText((String) res.getBody().get())
    json.uuid == record.uuid.toString()
  }

  @Rollback
  def "verify restPut can update a simple record with child records added"() {
    given: 'a controller for the base class for a domain'
    Class clazz = buildSampleParentController()

    and: 'an existing record'
    def record1 = new SampleParent(name: 'ABC-021').save()

    and: 'the source JSON'
    def src = """
      {
        "title" : "abc-001",
        "sampleChildren" : [
          {"key": "C1"},
          {"key": "C3"},
          {"key": "C2"}
        ]
      }
      """

    when: 'the put is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restPut(record1.uuid.toString(), mockRequest([body: src]), null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.getBody().get())}"

    then: 'the JSON is valid'
    res.status() == HttpStatus.OK

    and: 'the record is updated in the DB'
    SampleParent record = SampleParent.findByName('ABC-021')
    record.name == 'ABC-021'
    record.title == 'abc-001'
    record.sampleChildren.size() == 3
    record.sampleChildren[0].key == 'C1'
    record.sampleChildren[1].key == 'C3'
    record.sampleChildren[2].key == 'C2'

    and: 'the JSON is valid'
    def json = new JsonSlurper().parseText((String) res.getBody().get())

    List jsonChildren = json.sampleChildren
    jsonChildren.size() == 3
    jsonChildren[0].key == 'C1'
    jsonChildren[1].key == 'C3'
    jsonChildren[2].key == 'C2'
  }

  @Rollback
  def "verify restPut can update a simple record with child records that are replaced"() {
    given: 'a controller for the base class for a domain'
    Class clazz = buildSampleParentController()

    and: 'an existing record'
    def record1 = new SampleParent(name: 'ABC-021')
    record1.sampleChildren << (new SampleChild(key: 'C1'))
    record1.sampleChildren << (new SampleChild(key: 'C2'))
    record1.save()

    and: 'the source JSON'
    def src = """
      {
        "title" : "abc-001",
        "sampleChildren" : [
          {"key": "C3"}
        ]
      }
      """

    when: 'the put is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restPut(record1.uuid.toString(), mockRequest([body: src]), null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.getBody().get())}"

    then: 'the JSON is valid'
    res.status() == HttpStatus.OK

    and: 'the record is updated in the DB'
    SampleParent record = SampleParent.findByName('ABC-021')
    record.name == 'ABC-021'
    record.title == 'abc-001'
    record.sampleChildren.size() == 1
    record.sampleChildren[0].key == 'C3'

    and: 'the JSON is valid'
    def json = new JsonSlurper().parseText((String) res.getBody().get())

    List jsonChildren = json.sampleChildren
    jsonChildren.size() == 1
    jsonChildren[0].key == 'C3'
  }

  @Rollback
  def "verify restPut can update a record with custom child records"() {
    given: 'a controller for the base class for a domain'
    Class clazz = buildOrderController()

    and: 'a domain object to update'
    def order = new Order(order: 'ABC')
    def customComponents = []
    customComponents << new CustomOrderComponent(sequence: 1, product: 'PROD1')
    customComponents << new CustomOrderComponent(sequence: 2, product: 'PROD2')
    customComponents << new CustomOrderComponent(sequence: 3, product: 'PROD3')
    order.setFieldValue('customComponents', customComponents)
    order.save()

    and: 'the source JSON'
    def src = """
      {
        "order" : "ABC",
        "customComponents" : [
          {"sequence" : 11, "product" : "C1A"},
          {"sequence" : 12, "product" : "C2A"},
          {"sequence" : 13, "product" : "C3A"}
        ]
      }
    """

    when: 'the post is called'
    def controller = clazz.newInstance()
    //HttpResponse res = controller.restPut(mockRequest([body: src]), null)
    HttpResponse res = controller.restPut(order.uuid.toString(), mockRequest([body: src]), null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.body())}"

    then: 'the JSON is valid'
    res.status() == HttpStatus.OK

    and: 'the record is created in the DB'
    Order record = Order.findByOrder('ABC')
    record.order == 'ABC'

    and: 'the child records are correct'
    List<CustomOrderComponent> customComponents2 = record.getFieldValue('customComponents') as List<CustomOrderComponent>
    customComponents2.size() == 3
    customComponents2[0].sequence == 11
    customComponents2[0].product == 'C1A'
    customComponents2[1].sequence == 12
    customComponents2[1].product == 'C2A'
    customComponents2[2].sequence == 13
    customComponents2[2].product == 'C3A'

    and: 'the JSON is valid'
    def json = new JsonSlurper().parseText((String) res.getBody().get())
    json.order == record.order

    List jsonChildren = json.customComponents
    jsonChildren.size() == 3
    jsonChildren[0].sequence == 11
    jsonChildren[0].product == 'C1A'
    jsonChildren[1].sequence == 12
    jsonChildren[1].product == 'C2A'
    jsonChildren[2].sequence == 13
    jsonChildren[2].product == 'C3A'
  }

  @Rollback
  def "verify restPut can update will leave child records un-touched if they are not in the update JSON"() {
    given: 'a controller for the base class for a domain'
    Class clazz = buildSampleParentController()

    and: 'an existing record'
    def record1 = new SampleParent(name: 'ABC-021')
    record1.sampleChildren << (new SampleChild(key: 'C1'))
    record1.sampleChildren << (new SampleChild(key: 'C2'))
    record1.save()

    and: 'the source JSON'
    def src = """
      {
        "title" : "abc-001"
      }
      """

    when: 'the put is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restPut(record1.uuid.toString(), mockRequest([body: src]), null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.getBody().get())}"

    then: 'the JSON is valid'
    res.status() == HttpStatus.OK

    and: 'the record is updated in the DB'
    SampleParent record = SampleParent.findByName('ABC-021')
    record.name == 'ABC-021'
    record.title == 'abc-001'
    record.sampleChildren.size() == 2
    record.sampleChildren[0].key == 'C1'
    record.sampleChildren[1].key == 'C2'

    and: 'the JSON is valid'
    def json = new JsonSlurper().parseText((String) res.getBody().get())

    List jsonChildren = json.sampleChildren
    jsonChildren.size() == 2
    jsonChildren[0].key == 'C1'
    jsonChildren[1].key == 'C2'
  }

  @Rollback
  def "verify restPut can update a record with custom fields"() {
    given: 'a controller for the base class for a domain'
    Class clazz = buildAllFieldsDomainController()

    and: 'an existing record'
    def record1 = new AllFieldsDomain(name: 'ABC-021')
    record1.setFieldValue('custom1', 'original')
    record1.save()

    and: 'custom field values'
    DataGenerator.buildCustomField(fieldName: 'custom1', domainClass: AllFieldsDomain)

    and: 'the source JSON'
    def src = """
      {
        "title" : "abc-001",
        "qty" : 1221.00,
        "custom1" : "xyzzy"
      }
      """

    when: 'the put is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restPut(record1.uuid.toString(), mockRequest([body: src]), null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.getBody().get())}"

    then: 'the JSON is valid'
    res.status() == HttpStatus.OK

    and: 'the record is created in the DB'
    AllFieldsDomain record = AllFieldsDomain.findByName('ABC-021')
    record.getFieldValue('custom1') == 'xyzzy'

    and: 'the JSON is valid'
    def record2 = Holders.objectMapper.readValue((String) res.getBody().get(), AllFieldsDomain)
    record2.getFieldValue('custom1') == 'xyzzy'
  }

  @Rollback
  def "verify restPut fails with validation errors"() {
    given: 'a controller for the base class for a domain'
    Class clazz = buildAllFieldsDomainController()

    and: 'a record to update'
    def record1 = new AllFieldsDomain(name: 'ABC-021').save()

    and: 'the source JSON'
    def src = """
      {
        "count" : 1000001
      }
      """

    when: 'the post is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restPut(record1.uuid.toString(), mockRequest([body: src]), null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.body())}"

    then: 'the create fails'
    res.status() == HttpStatus.BAD_REQUEST

    and: 'the JSON is has the correct messages'
    def json = new JsonSlurper().parseText((String) res.getBody().get())
    json.message.text.contains('count')
    json.message.text.contains('000')
  }

  @Rollback
  def "verify restPut fails with no request body"() {
    given: 'a controller for the base class for a domain'
    Class clazz = buildAllFieldsDomainController()

    when: 'the post is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restPut('', mockRequest([:]), null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.body())}"

    then: 'the update fails'
    res.status() == HttpStatus.BAD_REQUEST

    and: 'the JSON is has the correct messages'
    def json = new JsonSlurper().parseText((String) res.getBody().get())
    UnitTestUtils.assertContainsAllIgnoreCase(json.message.text, ['empty', 'body'])
  }


  @Rollback
  def "verify restPut fails with no record found"() {
    given: 'a controller for the base class for a domain'
    Class clazz = buildAllFieldsDomainController()

    when: 'the put is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restPut('', mockRequest([body: "{}"]), null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.body())}"

    then: 'the response is correct'
    res.status() == HttpStatus.NOT_FOUND
  }

  @Rollback
  def "verify restPut checks for controller-level secured annotation and fails when user has wrong permissions"() {
    given: 'a controller'
    Object controller = buildAllFieldsDomainController().newInstance()

    and: 'a mocked security utils that will fail'
    new MockSecurityUtils(this, HttpStatus.FORBIDDEN).install()

    when: 'the list is called from the controller'
    def res = controller.restPut('1', mockRequest(), null)

    then: 'the correct values are returned'
    res.status == HttpStatus.FORBIDDEN
  }

  @Rollback
  def "verify restDelete can delete a simple record"() {
    given: 'a controller for the base class for a domain'
    Class clazz = buildAllFieldsDomainController()

    and: 'an existing record'
    def record1 = new AllFieldsDomain(name: 'ABC-021').save()

    when: 'the put is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restDelete(record1.uuid.toString(), null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.getBody().get())}"

    then: 'the JSON is valid'
    res.status() == HttpStatus.NO_CONTENT

    and: 'the record is delete from the DB'
    AllFieldsDomain.findByName('ABC-021') == null
  }

  @Rollback
  def "verify restDelete can delete a record with children"() {
    given: 'a controller for the base class for a domain'
    Class clazz = buildSampleParentController()

    and: 'an existing record'
    def record1 = new SampleParent(name: 'ABC-021')
    record1.sampleChildren << (new SampleChild(key: 'C1'))
    record1.sampleChildren << (new SampleChild(key: 'C2'))
    record1.save()

    when: 'the put is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restDelete(record1.uuid.toString(), null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.getBody().get())}"

    then: 'the JSON is valid'
    res.status() == HttpStatus.NO_CONTENT

    and: 'the records are deleted from the DB'
    SampleParent.findByName('ABC-021') == null
    SampleChild.count() == 0
  }

  @Rollback
  def "verify restDelete can delete a record with related records"() {
    given: 'a controller for the base class for a domain'
    Class clazz = buildSampleParentController()

    and: 'an existing record'
    new AllFieldsDomain(name: 'SAMPLE').save()
    def sampleParent = new SampleParent(name: 'SAMPLE', title: 'Sample').save()

    when: 'the put is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restDelete(sampleParent.uuid.toString(), null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.getBody().get())}"

    then: 'the JSON is valid'
    res.status() == HttpStatus.NO_CONTENT

    and: 'the records are deleted from the DB'
    SampleParent.findByName('ABC-021') == null
    AllFieldsDomain.count() == 0
  }

  @Rollback
  def "verify restDelete fails with no record found"() {
    given: 'a controller for the base class for a domain'
    Class clazz = buildAllFieldsDomainController()

    when: 'the put is called'
    def controller = clazz.newInstance()
    HttpResponse res = controller.restDelete('', null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.body())}"

    then: 'the response is correct'
    res.status() == HttpStatus.NOT_FOUND
  }

  @Rollback
  def "verify restDelete checks for controller-level secured annotation and fails when user has wrong permissions"() {
    given: 'a controller'
    Object controller = buildAllFieldsDomainController().newInstance()

    and: 'a mocked security utils that will fail'
    new MockSecurityUtils(this, HttpStatus.FORBIDDEN).install()

    when: 'the list is called from the controller'
    def res = controller.restDelete('1', null)

    then: 'the correct values are returned'
    res.status == HttpStatus.FORBIDDEN
  }

  def "verify that get works in a live server"() {
    given: 'a test record'
    def record = null
    AllFieldsDomain.withTransaction {
      record = new AllFieldsDomain(name: 'ABC', title: 'abc').save()
    }

    when: 'the get is triggered'
    login()
    def s = sendRequest(uri: "/allFieldsDomain/crud/${record.uuid}")

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
    json.uuid == record.uuid.toString()
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
    def s = sendRequest(uri: "/allFieldsDomain/crud/${record1.uuid}", method: 'put', content: src)

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
    json.uuid == record.uuid.toString()
  }

  def "verify that delete works in a live server"() {
    given: 'a record to delete'
    def record1 = null
    AllFieldsDomain.withTransaction {
      record1 = new AllFieldsDomain(name: 'ABC-021').save()
    }

    when:
    login()
    sendRequest(uri: "/allFieldsDomain/crud/${record1.uuid}", method: 'delete', status: HttpStatus.NO_CONTENT)

    then: 'the record is created in the DB'
    def record = null
    AllFieldsDomain.withTransaction {
      record = AllFieldsDomain.findByName('ABC-021')
      true
    }
    record == null
  }
}
