/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.domain

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.AllFieldsDomain
import sample.domain.Order
import sample.domain.SampleParent
import sample.service.OrderService

import javax.transaction.Transactional

/**
 * Tests for Micronaut Data compatibility.  Tests basic behaviors that might break
 * for new releases.
 */
class MicronautDataCompatibilitySpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  @Rollback
  def "verify that simple domain references can be updated"() {
    given: 'a domain with a reference'
    def afd1 = new AllFieldsDomain(name: 'ABC-01', title: 'orig').save()
    def p = new SampleParent(name: 'SAMPLE', title: 'Sample', allFieldsDomain: afd1)
    p.save()

    when: 'the record is changed and saved'
    p.title = 'new'
    p.save()

    then: 'the value is correct in the DB'
    def sampleParent2 = SampleParent.findByUuid(p.uuid)
    sampleParent2.title == 'new'
    sampleParent2.allFieldsDomain == afd1
  }

  void writeSome(boolean fail) {
    new SampleParent(name: 'SAMPLE1').save()
    new SampleParent(name: 'SAMPLE2').save()
    if (fail) {
      throw new BusinessException(code: 1)
    }
  }

  @Transactional
  void writeSomeAnnotation(boolean fail) {
    //println "orderService = ${Holders.applicationContext.getBean(OrderService)}"
    Holders.applicationContext.getBean(OrderService).save(new Order('M1001'))

    new SampleParent(name: 'SAMPLE1').save()
    new SampleParent(name: 'SAMPLE2').save()
    if (fail) {
      throw new BusinessException(code: 1)
    }
  }

  void writeSomeWithTransaction(boolean fail) {
    SampleParent.withTransaction {
      Holders.applicationContext.getBean(OrderService).save(new Order('M1001'))
      new SampleParent(name: 'SAMPLE1').save()
      new SampleParent(name: 'SAMPLE2').save()
      if (fail) {
        throw new BusinessException(code: 1)
      }
    }
  }


  def "verify that Transactional annotation works - no error"() {
    when: 'a transactional method is called and it works'
    writeSomeAnnotation(false)

    then: 'the records are in the DB'
    SampleParent.list().size() == 2

    cleanup:
    deleteAllRecords(SampleParent, false)
  }

  def "verify that witTransaction works - no error"() {
    when: 'a transactional method is called and it works'
    writeSomeWithTransaction(false)

    then: 'the records are in the DB'
    SampleParent.list().size() == 2

    cleanup:
    deleteAllRecords(SampleParent, false)
    deleteAllRecords(Order, false)
  }


}
