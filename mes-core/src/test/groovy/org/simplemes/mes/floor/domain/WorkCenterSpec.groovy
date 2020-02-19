package org.simplemes.mes.floor.domain


import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.floor.WorkCenterDisabledStatus
import org.simplemes.mes.floor.WorkCenterStatus
import org.simplemes.mes.misc.FieldSizes

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class WorkCenterSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  @Override
  void checkForLeftoverRecords() {
    // TODO: Remove when all repos are defined.
    println "checkForLeftoverRecords DISABLED"
  }

  def "test standard constraints"() {
    expect: 'the constraints are enforced'
    DomainTester.test {
      domain WorkCenter
      requiredValues workCenter: 'ABC'
      maxSize 'workCenter', FieldSizes.MAX_CODE_LENGTH
      maxSize 'title', FieldSizes.MAX_TITLE_LENGTH
      notNullCheck 'workCenter'
      //fieldOrderCheck false
    }
  }

  @SuppressWarnings("SpellCheckingInspection")
  @Rollback
  def "test unicode values work"() {

    /**
     * A legal Unicode string suitable for key Unit Tests (uppercase, no spaces).
     * String is: "ABC" followed by Greek A, Accent A, Russian A and 2 Chinese/Japanese/Korean chars.
     */
    def UNICODE_KEY_TEST_STRING = "ABC\u0391\u00C0\u0410\u4E10\u4F11"

    given: 'a domain object with bad key field'
    WorkCenter workCenter = new WorkCenter()
    workCenter.workCenter = UNICODE_KEY_TEST_STRING
    workCenter.title = UNICODE_KEY_TEST_STRING

    when: 'the record is saved'
    workCenter.save()

    then: 'the unicode values can be retrieved'
    workCenter.workCenter == UNICODE_KEY_TEST_STRING
    workCenter.title == UNICODE_KEY_TEST_STRING
  }

  @Rollback
  def "test default status is used when null"() {
    given: 'a domain object with no status'
    def workCenter = new WorkCenter(workCenter: 'WC234')

    when: 'the record is saved'
    workCenter.save()

    then: 'the default status is used'
    workCenter.overallStatus == WorkCenterStatus.default
  }

  @Rollback
  def "test a provided status is used instead of the default status"() {
    given: 'a domain object with a status'
    new WorkCenter(workCenter: 'WC1', overallStatus: WorkCenterDisabledStatus.instance).save()

    when: 'the record is re-read'
    def workCenter = WorkCenter.findByWorkCenter('WC1')

    then: 'the provided status is used'
    workCenter.overallStatus == WorkCenterDisabledStatus.instance
  }
}
