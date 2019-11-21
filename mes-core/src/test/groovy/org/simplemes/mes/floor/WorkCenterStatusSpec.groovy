package org.simplemes.mes.floor

import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class WorkCenterStatusSpec extends BaseSpecification {

  def "verify that sub-classes have the correct methods - isEnabled"() {
    expect: 'the method finds the correct value'
    clazz.instance.enabled == enabled

    where:
    clazz                    | enabled
    WorkCenterEnabledStatus  | true
    WorkCenterDisabledStatus | false
  }

  def "verify that valueOf works for core statuses"() {
    expect: 'the method finds the correct value'
    WorkCenterStatus.valueOf(id) == results

    where:
    id                          | results
    WorkCenterEnabledStatus.ID  | WorkCenterEnabledStatus.instance
    WorkCenterDisabledStatus.ID | WorkCenterDisabledStatus.instance
    null                        | null
    'bad'                       | null
  }

  def "verify that toStringLocalized works for core statuses"() {
    expect: 'the method finds the correct value'
    def i18nKey = "workCenterStatus.${clazz.instance.id}.label"
    clazz.instance.toStringLocalized() == lookup(i18nKey)

    and: 'the label is in the messages.properties file'
    !clazz.instance.toStringLocalized().contains('.label')

    where:
    clazz                    | _
    WorkCenterEnabledStatus  | _
    WorkCenterDisabledStatus | _
  }

  def "verify that getValidValues works"() {
    given: 'the core valid values'
    def coreValues = [WorkCenterEnabledStatus, WorkCenterDisabledStatus]*.instance

    expect: 'the method has all of the expected values'
    WorkCenterStatus.getValidValues(null).containsAll(coreValues)
  }

}
