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
class OrderStatusSpec extends BaseSpecification {

  def "verify that sub-classes have the correct methods - isWorkable and isDone"() {
    expect: 'the method finds the correct value'
    clazz.instance.workable == workable
    clazz.instance.done == done

    where:
    clazz              | workable | done
    OrderCreatedStatus | false    | false
    OrderReadyStatus   | true     | false
    OrderDoneStatus    | false    | true
    OrderHoldStatus    | false    | false
  }

  def "verify that valueOf works for core statuses"() {
    expect: 'the method finds the correct value'
    OrderStatus.valueOf(id) == results

    where:
    id                    | results
    OrderCreatedStatus.ID | OrderCreatedStatus.instance
    OrderReadyStatus.ID   | OrderReadyStatus.instance
    OrderDoneStatus.ID    | OrderDoneStatus.instance
    OrderHoldStatus.ID    | OrderHoldStatus.instance
    null                  | null
    'bad'                 | null
  }

  def "verify that toStringLocalized works for core statuses"() {
    expect: 'the method finds the correct value'
    def i18nKey = "orderStatus.${clazz.instance.id}.label"
    clazz.instance.toStringLocalized() == lookup(i18nKey)

    and: 'the label is in the messages.properties file'
    !clazz.instance.toStringLocalized().contains('.label')

    where:
    clazz              | _
    OrderCreatedStatus | _
    OrderReadyStatus   | _
    OrderDoneStatus    | _
    OrderHoldStatus    | _
  }

  def "verify that the default status is correct"() {
    expect:
    OrderStatus.default == OrderReadyStatus.instance

    and: 'toString works'
    OrderReadyStatus.instance.toString()
  }

  def "verify that getValidValues works"() {
    given: 'the core valid values'
    def coreValues = [OrderCreatedStatus, OrderReadyStatus, OrderHoldStatus, OrderDoneStatus]*.instance

    expect: 'the method has all of the expected values'
    OrderStatus.getValidValues(null).containsAll(coreValues)
  }

}
