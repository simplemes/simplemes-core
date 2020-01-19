/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data


import org.simplemes.eframe.system.DisabledStatus
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.AllFieldsDomain

/**
 *  Tests.
 */
class EncodedTypeConvertersSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  @Rollback
  def "verify that converters work on round-trip to and from the DB"() {
    when: 'a record with a dateOnly is saved'
    def afd = new AllFieldsDomain(name: 'ABC')
    afd.status = DisabledStatus.instance
    afd.save()

    and: 'the record is re-read'
    def afd2 = AllFieldsDomain.findByUuid(afd.uuid)

    then: 'the read value has the correct date'
    //noinspection GrEqualsBetweenInconvertibleTypes
    afd2.status == DisabledStatus.instance
  }

}
