package org.simplemes.mes.demand


import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.LSNOperState
import org.simplemes.mes.demand.domain.Order

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Test the trait, using a concrete class (LSNOperState).
 */
class WorkStateTraitSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  /**
   * Builds an LSNOperState record with the given settings.  Builds this oper state object inside of an LSN/Order.
   * Also saves the order.
   * @param options The LSNOperState options.
   * @return The LSNOperState record.
   */
  LSNOperState buildLSNOperState(Map options) {
    def order = new Order(order: 'ABC')
    def lsn = new LSN(lsn: 'SN001')
    order.lsns << lsn

    def lsnOperState = new LSNOperState(options)
    lsn.operationStates << lsnOperState
    order.save()

    return lsnOperState
  }

  @Rollback
  def "test queueing a qty"() {
    given: 'a work state object'
    def lsnOperState = buildLSNOperState(qtyInQueue: 0.0)
    assert lsnOperState.dateFirstQueued == null

    when: 'a qty is queued'
    lsnOperState.queueQty(0.2)

    then: 'right qty is queued'
    lsnOperState.qtyInQueue == 0.2

    and: 'the dateFirstQueued is set'
    UnitTestUtils.dateIsCloseToNow(lsnOperState.dateFirstQueued)
    UnitTestUtils.dateIsCloseToNow(lsnOperState.dateQtyQueued)
  }

  @Rollback
  def "test queueing a qty with a specific date"() {
    given: 'a work state object'
    def lsnOperState = buildLSNOperState(qtyInQueue: 0.0)
    assert lsnOperState.dateFirstQueued == null

    and: 'the date to queue it'
    def dateTime = new Date() - 1

    when: 'a qty is queued'
    lsnOperState.queueQty(0.2, dateTime)

    then: 'right qty is queued'
    lsnOperState.qtyInQueue == 0.2

    and: 'the dateFirstQueued is set'
    UnitTestUtils.compareDates(lsnOperState.dateFirstQueued, dateTime)
    UnitTestUtils.compareDates(lsnOperState.dateQtyQueued, dateTime)
  }

  @Rollback
  def "test queueing a second leaves the dateQtyQueued unchanged"() {
    given: 'a work state object'
    def lsnOperState = buildLSNOperState(qtyInQueue: 0.0)
    assert lsnOperState.dateFirstQueued == null

    and: 'the date to queue the qty'
    def firstDateTime = new Date() - 2
    def secondDateTime = new Date() - 1

    when: 'a qty is queued'
    lsnOperState.queueQty(0.2, firstDateTime)

    and: 'a second qty is queued'
    lsnOperState.queueQty(0.3, secondDateTime)

    then: 'right qty is queued'
    lsnOperState.qtyInQueue == 0.5

    and: 'the dates are correct set'
    UnitTestUtils.compareDates(lsnOperState.dateFirstQueued, firstDateTime)
    UnitTestUtils.compareDates(lsnOperState.dateQtyQueued, firstDateTime)
  }

  @Rollback
  def "test queueing a negative qty"() {
    given: 'a work state object'
    def lsnOperState = buildLSNOperState(qtyInQueue: 0.0)

    when: 'a qty is queued'
    lsnOperState.queueQty(-0.2)

    then: 'should fail with proper message'
    def e = thrown(BusinessException)
    UnitTestUtils.allParamsHaveValues(e)
    UnitTestUtils.assertContainsAllIgnoreCase(e.toString(), ['-0.2', 'greater'])
    e.code == 3002
  }

  @Rollback
  def "test first start"() {
    given: 'a work state object'
    def lsnOperState = buildLSNOperState(qtyInQueue: 1.2)
    assert lsnOperState.dateFirstStarted == null

    when: 'a qty is started'
    lsnOperState.startQty(0.2)

    then: 'right qty is in queue'
    lsnOperState.qtyInQueue == 1.0

    and: 'right qty is in work'
    lsnOperState.qtyInWork == 0.2

    and: 'the dates are set'
    UnitTestUtils.dateIsCloseToNow(lsnOperState.dateFirstStarted)
    UnitTestUtils.dateIsCloseToNow(lsnOperState.dateQtyStarted)
  }

  @Rollback
  def "test start with dateTime passed in"() {
    given: 'a work state object'
    def lsnOperState = buildLSNOperState(qtyInQueue: 0.5)
    assert lsnOperState.dateFirstStarted == null

    and: 'the start dateTime'
    def dateTime = new Date() - 1

    when: 'a qty is started'
    lsnOperState.startQty(0.2, dateTime)

    then: 'the dates are set'
    UnitTestUtils.compareDates(lsnOperState.dateFirstStarted, dateTime)
    UnitTestUtils.compareDates(lsnOperState.dateQtyStarted, dateTime)

    and: 'the queued date is cleared since all qty is started'
    lsnOperState.dateQtyQueued == null
  }

  @Rollback
  def "test a second start leaves the dates unchanged"() {
    given: 'a work state object'
    def lsnOperState = buildLSNOperState(qtyInQueue: 1.2)
    assert lsnOperState.dateFirstStarted == null

    and: 'the date to queue the qty'
    def firstDateTime = new Date() - 2
    def secondDateTime = new Date() - 1

    when: 'a qty is started'
    lsnOperState.dateQtyQueued = firstDateTime
    lsnOperState.startQty(0.2, firstDateTime)

    then: 'the queued date is still set'
    lsnOperState.dateQtyQueued != null

    when: 'a second qty is started'
    lsnOperState.startQty(0.3, secondDateTime)

    then: 'right qty is in work'
    lsnOperState.qtyInWork == 0.5

    and: 'the dates are set'
    UnitTestUtils.compareDates(lsnOperState.dateFirstStarted, firstDateTime)
    UnitTestUtils.compareDates(lsnOperState.dateQtyStarted, firstDateTime)
  }

  @Rollback
  def "test starting a negative qty"() {
    given: 'a work state object'
    def lsnOperState = buildLSNOperState(qtyInQueue: 2.2)

    when: 'a qty is started'
    lsnOperState.startQty(-1.2)

    then: 'should fail with proper message'
    def e = thrown(BusinessException)
    UnitTestUtils.allParamsHaveValues(e)
    UnitTestUtils.assertContainsAllIgnoreCase(e.toString(), ['-1.2', 'greater'])
    e.code == 3002
  }

  @Rollback
  def "test starting with not enough qty in queue"() {
    given: 'a work state object'
    def lsnOperState = buildLSNOperState(qtyInQueue: 2.2, sequence: 13)

    when: 'a qty is started'
    lsnOperState.startQty(3.2)

    then: 'should fail with proper message'
    def e = thrown(BusinessException)
    UnitTestUtils.allParamsHaveValues(e)
    // error.3003.message=Quantity to start ({0}) must be less than or equal to the quantity in queue ({1}) at {2}
    UnitTestUtils.assertContainsAllIgnoreCase(e.toString(), ['2.2', '3.2', 'quantity', 'queue', lsnOperState.toString()])
    e.code == 3003
  }

  @Rollback
  def "test partial complete"() {
    given: 'a work state object'
    def lsnOperState = buildLSNOperState(qtyInWork: 1.2)
    lsnOperState.dateQtyStarted = new Date() - 1

    when: 'a qty is completed'
    lsnOperState.completeQty(0.2)

    then: 'right qty is in work'
    lsnOperState.qtyInWork == 1.0

    and: 'the start date is not cleared'
    lsnOperState.dateQtyStarted != null
  }

  @Rollback
  def "test full complete"() {
    given: 'a work state object'
    def lsnOperState = buildLSNOperState(qtyInWork: 1.2)
    lsnOperState.dateQtyStarted = new Date() - 1

    when: 'a qty is completed'
    lsnOperState.completeQty(1.2)

    then: 'right qty is in work'
    lsnOperState.qtyInWork == 0.0

    and: 'the start date is cleared since it is no longer in work'
    lsnOperState.dateQtyStarted == null
  }

  @Rollback
  def "test completing a negative qty"() {
    given: 'a work state object'
    def lsnOperState = buildLSNOperState(qtyInWork: 2.2)

    when: 'a negative qty is completed'
    lsnOperState.startQty(-1.2)

    then: 'should fail with proper message'
    def e = thrown(BusinessException)
    UnitTestUtils.allParamsHaveValues(e)
    UnitTestUtils.assertContainsAllIgnoreCase(e.toString(), ['-1.2', 'greater'])
    e.code == 3002
  }

  @Rollback
  def "test completing with not enough qty in work"() {
    given: 'a work state object'
    def lsnOperState = buildLSNOperState(qtyInWork: 2.2, sequence: 13)

    when: 'too much qty is completed'
    lsnOperState.completeQty(3.2)

    then: 'should fail with proper message'
    def e = thrown(BusinessException)
    UnitTestUtils.allParamsHaveValues(e)
    // error.3008.message=Quantity to complete ({0}) must be less than or equal to the quantity in work ({1}) at {2}
    UnitTestUtils.assertContainsAllIgnoreCase(e.toString(), ['2.2', '3.2', 'quantity', 'work', lsnOperState.toString()])
    e.code == 3008
  }

  @Rollback
  def "test first reverseStart"() {
    given: 'a work state object'
    def lsnOperState = buildLSNOperState(qtyInQueue: 1.2)

    and: 'the dateTimes for the start/reverse'
    def startDateTime = new Date() - 2
    def dateTime = new Date() - 1

    and: 'a qty is started'
    lsnOperState.startQty(0.2, startDateTime)

    when: 'the start is reversed'
    def res = lsnOperState.reverseStartQty(0.2, dateTime)

    then: 'right qty is in queue'
    lsnOperState.qtyInQueue == 1.2

    and: 'right qty is in work'
    lsnOperState.qtyInWork == 0.0

    and: 'the returned qty is correct'
    res == 0.2

    and: 'the date/time queued is correct'
    UnitTestUtils.compareDates(lsnOperState.dateQtyQueued, dateTime)

    and: 'the dateQtyStarted is cleared'
    lsnOperState.dateQtyStarted == null
  }

  @Rollback
  def "verify that reverseComplete works - full qtyDone is reversed"() {
    given: 'a work state object'
    def lsnOperState = buildLSNOperState(qtyDone: 1.2)

    def dateTime = new Date(UnitTestUtils.SAMPLE_TIME_MS)

    when: 'the complete is reversed'
    def res = lsnOperState.reverseCompleteQty(1.2, dateTime)

    then: 'right qty is in queue'
    lsnOperState.qtyInQueue == 1.2

    and: 'right qty is in work'
    lsnOperState.qtyInWork == 0.0

    and: 'the returned qty is correct'
    res == 1.2

    and: 'the date/time queued is correct'
    UnitTestUtils.compareDates(lsnOperState.dateQtyQueued, dateTime)
  }

  @Rollback
  def "verify that reverseComplete works - partial qtyDone is reversed with qty in work"() {
    given: 'a work state object'
    def dateTime = new Date(UnitTestUtils.SAMPLE_TIME_MS)
    def lsnOperState = buildLSNOperState(qtyDone: 1.2, qtyInWork: 1.0, dateQtyQueued: null)

    when: 'the complete is reversed'
    def res = lsnOperState.reverseCompleteQty(0.2, dateTime)

    then: 'right qty is in queue'
    lsnOperState.qtyInQueue == 0.2

    and: 'right qty is in work'
    lsnOperState.qtyInWork == 1.0

    and: 'right qty is left as done'
    lsnOperState.qtyDone == 1.0

    and: 'the returned qty is correct'
    res == 0.2

    and: 'the date/time queued is correct'
    UnitTestUtils.compareDates(lsnOperState.dateQtyQueued, dateTime)
  }

  @Rollback
  def "verify that reverseComplete works - partial qtyDone with some other qty in queue already"() {
    given: 'a work state object'
    def dateTime = new Date(UnitTestUtils.SAMPLE_TIME_MS)
    def lsnOperState = buildLSNOperState(qtyDone: 1.2, qtyInQueue: 1.0, dateQtyQueued: dateTime)

    when: 'the complete is reversed'
    def res = lsnOperState.reverseCompleteQty(0.2, new Date(UnitTestUtils.SAMPLE_TIME_MS - 1000))

    then: 'right qty is in queue'
    lsnOperState.qtyInQueue == 1.2

    and: 'right qty is left as done'
    lsnOperState.qtyDone == 1.0

    and: 'the returned qty is correct'
    res == 0.2

    and: 'the date/time queued is correct'
    UnitTestUtils.compareDates(lsnOperState.dateQtyQueued, dateTime)
  }

  @Rollback
  def "verify that reverseComplete gracefully detects qty less than 0"() {
    given: 'a work state object'
    def lsnOperState = buildLSNOperState(qtyDone: 1.2)

    when: 'the complete is reversed'
    lsnOperState.reverseCompleteQty(-0.2)

    then: 'the right exception is thrown'
    // error.3007.message=Quantity to process ({0}) must be greater than 0
    def ex = thrown(BusinessException)
    UnitTestUtils.assertExceptionIsValid(ex, ['-0.2'])
    ex.code == 3007
  }

  @Rollback
  def "verify that reverseComplete gracefully detects qty less than qtyDone"() {
    given: 'a work state object'
    def lsnOperState = buildLSNOperState(qtyDone: 1.2)

    when: 'the complete is reversed'
    lsnOperState.reverseCompleteQty(2.2)

    then: 'the right exception is thrown'
    //error.3016.message=Quantity to process ({0}) must be less than or equal to the quantity done ({1}) for {2}
    def ex = thrown(BusinessException)
    UnitTestUtils.assertExceptionIsValid(ex, ['2.2', '1.2', lsnOperState.lsn.lsn])
    ex.code == 3016
  }

}
