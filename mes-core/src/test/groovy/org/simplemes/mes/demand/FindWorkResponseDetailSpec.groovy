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
class FindWorkResponseDetailSpec extends BaseSpecification {

  //@SuppressWarnings("unused")
  //static dirtyDomains = [ActionLog, ProductionLog, Order, Product]

  def "test common constructor status tests"() {
    given: 'a detail record'
    def detail = new FindWorkResponseDetail(qtyInQueue: qtyInQueue, qtyInWork: qtyInWork)

    when: 'the status tests are made'
    detail.init()

    then: 'the status flags are set correctly'
    detail.inQueue == inQueue
    detail.inWork == inWork

    where: 'various combinations of queue and in work are used'
    qtyInQueue | qtyInWork | inQueue | inWork
    13.2       | 14.2      | true    | true
    23.2       | 0.0       | true    | false
    0.0        | 34.2      | false   | true
    0.0        | 0.0       | false   | false
  }

  def "test map constructor with just order column names"() {
    given: 'some values to set'
    def map = [uuid    : UUID.randomUUID(),
               order_id: UUID.randomUUID(),
               ordr    : 'M1001']
    when: 'the detail is created from the map with column names'
    def detail = new FindWorkResponseDetail(map)

    then: 'the qty/operation is copied'
    detail.order == map.ordr
    detail.orderID == map.order_id
    detail.id == map.uuid
  }

  def "test map constructor with all column names"() {
    given: 'some values to set'
    def date = new Date()
    def map = [qty_in_queue   : 23.7,
               qty_in_work    : 33.7,
               qty_done       : 13.7,
               sequence       : 237,
               uuid           : UUID.randomUUID(),
               lsn            : 'SN001',
               lsn_id         : UUID.randomUUID(),
               ordr           : 'M1001',
               order_id       : UUID.randomUUID(),
               dateQtyQueued  : date, dateQtyStarted: new Date(date.time + 1000),
               dateFirstQueued: new Date(date.time + 2000), dateFirstStarted: new Date(date.time + 3000)]
    when: 'the detail is created from the map with column names'
    def detail = new FindWorkResponseDetail(map)

    then: 'the qty/operation is copied'
    detail.order == map.ordr
    detail.orderID == map.order_id
    detail.lsn == map.lsn
    detail.lsnID == map.lsn_id
    detail.qtyInQueue == map.qty_in_queue
    detail.qtyInWork == map.qty_in_work
    detail.qtyDone == map.qty_done
    detail.operationSequence == map.sequence
    detail.id == map.uuid
    detail.dateQtyQueued == map.dateQtyQueued
    detail.dateQtyStarted == map.dateQtyStarted
    detail.dateFirstQueued == map.dateFirstQueued
    detail.dateFirstStarted == map.dateFirstStarted

    and: 'the inWork/inQueue flags work'
    detail.inQueue
    detail.inWork
  }

}
