/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.json

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.system.DisabledStatus
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.eframe.web.report.ReportTimeIntervalEnum
import sample.domain.AllFieldsDomain
import sample.domain.AllFieldsDomainRepository
import sample.domain.SampleChild
import sample.domain.SampleParent
import sample.domain.SampleParentRepository

/**
 * Tests the Persistence Aware Jackson additions.
 */
class EFrameJacksonModuleSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [JSON, SERVER]

  @SuppressWarnings("unused")
  static dirtyDomains = [SampleParent, AllFieldsDomain]

  @SuppressWarnings("GroovyAssignabilityCheck")
  @Rollback
  def "verify that the introspector will allow child records to be generated"() {
    given: 'a domain with a foreign reference'
    def afd1 = new AllFieldsDomain(name: 'ABC-01').save()
    def p = new SampleParent(name: 'SAMPLE', title: 'Sample', allFieldsDomain: afd1).save()
    p.sampleChildren << new SampleChild(sampleParent: p, key: '123').save()

    when: 'the entity is serialized to JSON'
    def s = new ObjectMapper().registerModule(new EFrameJacksonModule()).writeValueAsString(p)

    then: 'the correct JSON is created'
    def json = new JsonSlurper().parse(s.bytes)
    json.name == 'SAMPLE'

    and: 'the child records are in the list'
    json.sampleChildren.size() == 1
    json.sampleChildren[0].key == '123'
  }


  @Rollback
  def "verify that the round trip with foreign reference in domain object works"() {
    // TODO: Remove repo references?
    given: 'a simple domain'
    AllFieldsDomainRepository allFieldsDomainRepository = Holders.applicationContext.getBean(AllFieldsDomainRepository)
    //println "allFieldsDomainRepository = $allFieldsDomainRepository"
    def afd1 = new AllFieldsDomain(name: 'ABC-01', title: 'orig')
    allFieldsDomainRepository.save(afd1)
    SampleParentRepository sampleParentRepository = Holders.applicationContext.getBean(SampleParentRepository)
    def p = new SampleParent(name: 'SAMPLE', title: 'Sample', allFieldsDomain: afd1)
    sampleParentRepository.save(p)
    def originalID = p.uuid
    //println "p = $p.version"
    //println "p1 = $p.version, $p.id"

    when: 'the entity is serialized to JSON'
    def objectMapper = new ObjectMapper().registerModule(new EFrameJacksonModule())
    def s = objectMapper.writeValueAsString(p)
    //println "s = $s"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    and: 'the original record is deleted'
    p.delete()

    and: 'update the foreign record'
    afd1.title = 'new'
    afd1.save()

    and: 'the object is re-created from the JSON'
    def p2 = objectMapper.readValue(s, SampleParent)
    p2.uuid = null // TODO: Determine if we need to clear this to force insert.   A new _exists field?
    //println "p2 = $p2"
    p2.save()
    //println "p2 = $p2"

    then: 'the new record exists'
    p2.uuid != originalID
    p2.allFieldsDomain == afd1
    //println "p2 = $p2.version, $p2.id"

    and: 'no new records for the foreign domain is created'
    AllFieldsDomain.list().size() == 1
    def afd = AllFieldsDomain.findByName('ABC-01')
    //println "afd = ${afd.dirty}, $afd"
    //println "afd = ${AllFieldsDomain.list()}"
    afd.title == 'new'
  }

  @Rollback
  def "verify that foreign reference in simple format can be read and saved in the domain object"() {
    given: 'a simple domain'
    def afd = new AllFieldsDomain(name: 'ABC-01', title: 'orig').save()

    when: 'the entity is de-serialized to JSON'
    def objectMapper = new ObjectMapper().registerModule(new EFrameJacksonModule())
    def s = """
    {
      "name": "SAMPLE",
      "title": "Sample",
      "notes": null,
      "notDisplayed": null,
      "dateCreated": 1546379397946,
      "dateUpdated": 1546379397946,
      "allFieldsDomain": {
          "name": "SAMPLEX",
          "uuid": "${afd.uuid}"
      },
      "allFieldsDomains": null,
      "sampleChildren": [
          
      ],
      "uuid": "274e90ad-fb5b-4ec1-9c87-060914079edd"
    }"""
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    and: 'the object is created from the JSON'
    def p1 = objectMapper.readValue(s, SampleParent)
    p1.uuid = null // TODO: Determine if we need to clear this to force insert.   A new _exists field?
    p1.save()
    //println "p1 = $p1"

    then: 'the new record exists'
    def p2 = SampleParent.findByName('SAMPLE')
    p2.uuid != null
    p2.allFieldsDomain == afd
    //println "p2 = $p2.version, $p2.id"

    and: 'no new records for the foreign domain is created'
    AllFieldsDomain.count() == 1
  }

  @Rollback
  def "verify that a foreign reference is serialized to simple format - just key and ID"() {
    given: 'a simple domain'
    def afd = new AllFieldsDomain(name: 'ABC-01', title: 'orig').save()
    def p = new SampleParent(name: 'SAMPLE', allFieldsDomain: afd)

    when: 'the entity is de-serialized to JSON'
    def objectMapper = new ObjectMapper().registerModule(new EFrameJacksonModule())
    def s = objectMapper.writeValueAsString(p)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the correct JSON is created'
    def json = new JsonSlurper().parse(s.bytes)
    json.name == 'SAMPLE'

    and: 'the foreign reference is correct'
    json.allFieldsDomain.uuid == afd.uuid.toString()
    json.allFieldsDomain.name == afd.name
    json.allFieldsDomain.title == null
  }

  @Rollback
  def "verify that the round trip with domain object works"() {
    given: 'a simple domain'
    def p = new SampleParent(name: 'SAMPLE', title: 'Sample')
    p.save()
    def originalID = p.uuid

    when: 'the entity is serialized to JSON'
    def objectMapper = new ObjectMapper().registerModule(new EFrameJacksonModule())
    def s = objectMapper.writeValueAsString(p)
    //println "s = $s"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    and: 'the original record is deleted'
    p.delete()

    and: 'the object is re-created from the JSON'
    def p2 = objectMapper.readValue(s, SampleParent)
    p2.uuid = null // TODO: Determine if we need to clear this to force insert.   A new _exists field?
    p2.save()
    //println "p2 = $p2"

    then: 'the new record exists'
    p2.uuid != originalID
  }

  @Rollback
  def "verify that the enum serializer works"() {
    given: 'a domain with an enum'
    def afd = new AllFieldsDomain(name: 'ABC-01', reportTimeInterval: ReportTimeIntervalEnum.LAST_30_DAYS)

    when: 'the entity is serialized to JSON'
    def s = new ObjectMapper().registerModule(new EFrameJacksonModule()).writeValueAsString(afd)
    //println "s = $s"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the correct JSON is created'
    def json = new JsonSlurper().parse(s.bytes)
    json.reportTimeInterval == ReportTimeIntervalEnum.LAST_30_DAYS.toString()
    json._reportTimeIntervalDisplay_ == ReportTimeIntervalEnum.LAST_30_DAYS.toStringLocalized()
  }

  @Rollback
  def "verify that the enum serializer works - round trip"() {
    given: 'a domain with an enum'
    def afd = new AllFieldsDomain(name: 'ABC-01', reportTimeInterval: ReportTimeIntervalEnum.LAST_30_DAYS)

    when: 'the entity is serialized to JSON'
    def s = new ObjectMapper().registerModule(new EFrameJacksonModule()).writeValueAsString(afd)
    //println "s = $s"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    and: 'the value is de-serialized'
    AllFieldsDomain o = new ObjectMapper().registerModule(new EFrameJacksonModule()).readValue(s, AllFieldsDomain)

    then: 'status us set correctly'
    o.reportTimeInterval == ReportTimeIntervalEnum.LAST_30_DAYS
  }

  @Rollback
  def "verify that the encoded type serializer works"() {
    given: 'a domain with an enum'
    def afd = new AllFieldsDomain(name: 'ABC-01', status: DisabledStatus.instance)

    when: 'the entity is serialized to JSON'
    def s = new ObjectMapper().registerModule(new EFrameJacksonModule()).writeValueAsString(afd)
    //println "s = $s"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the correct JSON is created'
    def json = new JsonSlurper().parse(s.bytes)
    json.status == DisabledStatus.instance.id
    json._statusDisplay_ == DisabledStatus.instance.toStringLocalized()
  }

  @SuppressWarnings("GrEqualsBetweenInconvertibleTypes")
  @Rollback
  def "verify that the encoded type serializer works for round trips"() {
    given: 'a domain with an encoded type'
    def afd = new AllFieldsDomain(name: 'ABC-01', status: DisabledStatus.instance)

    when: 'the entity is serialized to JSON'
    def s = new ObjectMapper().registerModule(new EFrameJacksonModule()).writeValueAsString(afd)
    //println "s = $s"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    and: 'the value is de-serialized'
    AllFieldsDomain o = new ObjectMapper().registerModule(new EFrameJacksonModule()).readValue(s, AllFieldsDomain)

    then: 'status us set correctly'
    o.status == DisabledStatus.instance
  }

  @Rollback
  def "verify that serialize does not create an entry for the holder itself"() {
    given: 'a domain object with custom fields'
    buildCustomField(fieldName: 'custom1', domainClass: SampleParent)
    def (SampleParent sampleParent) = DataGenerator.generate {
      domain SampleParent
    }

    when: 'the JSON is created'
    sampleParent.setFieldValue('custom1', 'abc')
    def s = new ObjectMapper().registerModule(new EFrameJacksonModule()).writeValueAsString(sampleParent)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the JSON has the correct field name for the holder'
    !s.contains('"_customFields"')
    s.contains('"__customFields"')
  }


  // test POJO is left un-changed
  // test parent ref is removed
}
