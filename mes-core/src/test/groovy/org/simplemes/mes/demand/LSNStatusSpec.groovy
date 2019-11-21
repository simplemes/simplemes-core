package org.simplemes.mes.demand


import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class LSNStatusSpec extends BaseSpecification {

  def "verify that sub-classes have the correct methods - isWorkable and isDone"() {
    expect: 'the method finds the correct value'
    clazz.instance.workable == workable
    clazz.instance.done == done
    clazz.instance.scrapped == scrapped

    where:
    clazz             | workable | done  | scrapped
    LSNReadyStatus    | true     | false | false
    LSNDoneStatus     | false    | true  | false
    LSNHoldStatus     | false    | false | false
    LSNScrappedStatus | false    | false | true
  }

  def "verify that valueOf works for core statuses"() {
    expect: 'the method finds the correct value'
    LSNStatus.valueOf(id) == results

    where:
    id                   | results
    LSNReadyStatus.ID    | LSNReadyStatus.instance
    LSNDoneStatus.ID     | LSNDoneStatus.instance
    LSNHoldStatus.ID     | LSNHoldStatus.instance
    LSNScrappedStatus.ID | LSNScrappedStatus.instance
    null                 | null
    'bad'                | null
  }

  def "verify that toStringLocalized works for core statuses"() {
    expect: 'the method finds the correct value'
    def i18nKey = "lsnStatus.${clazz.instance.id}.label"
    clazz.instance.toStringLocalized() == lookup(i18nKey)

    and: 'the label is in the messages.properties file'
    !clazz.instance.toStringLocalized().contains('.label')

    where:
    clazz             | _
    LSNReadyStatus    | _
    LSNDoneStatus     | _
    LSNHoldStatus     | _
    LSNScrappedStatus | _
  }

  def "verify that getValidValues works"() {
    given: 'the core valid values'
    def coreValues = [LSNReadyStatus, LSNHoldStatus, LSNDoneStatus, LSNScrappedStatus]*.instance

    expect: 'the method has all of the expected values'
    LSNStatus.getValidValues(null).containsAll(coreValues)
  }

}
