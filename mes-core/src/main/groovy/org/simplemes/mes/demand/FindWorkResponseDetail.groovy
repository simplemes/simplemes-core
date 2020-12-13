package org.simplemes.mes.demand

import groovy.transform.ToString
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.mes.floor.domain.WorkCenter

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * The response from the WorkListService.findWork() service method.
 * This represents a single unit in queue or in work.  This can be an order and/or LSN.
 * <p/>
 * This is a POGO used by the {@link org.simplemes.mes.demand.service.WorkListService#findWork(org.simplemes.mes.demand.FindWorkRequest)} method.
 */
@ToString(includeNames = true, includePackage = false)
class FindWorkResponseDetail {

  /**
   * The unique ID of this row.  The lowest element for the row will be used (LSNOperState, OrderOperState, LNS then Order).
   * This will be consistent for later requests.  Suitable for use
   * in identifying the selected records.
   */
  def id

  /**
   * The order that is in work or queue.  This is a simple string to reduce the size of this object.
   */
  String order

  /**
   * The ID of the order that is in work or queue.  This can be used to find the original record.
   */
  def orderID

  /**
   * The LSN that is in work or queue.   This is a simple string to reduce the size of this object.
   */
  String lsn = ''

  /**
   * The ID of the LSN that is in work or queue.  This can be used to find the original record.
   */
  def lsnID

  /**
   * The operation this qty is at.  (0 means not at an operation)
   */
  int operationSequence = 0

  /**
   * The qty in queue.
   */
  BigDecimal qtyInQueue

  /**
   * The qty in queue.
   */
  BigDecimal qtyInWork

  /**
   * The qty done.
   */
  BigDecimal qtyDone

  /**
   * The date the qty was queued.
   */
  Date dateQtyQueued

  /**
   * The date the qty was started.
   */
  Date dateQtyStarted

  /**
   * The date the qty was first queued.
   */
  Date dateFirstQueued

  /**
   * The date the qty was first queued.
   */
  Date dateFirstStarted

  /**
   * The work center the unit is in.
   */
  WorkCenter workCenter

  /**
   * True if the unit is in work.
   */
  boolean inWork

  /**
   * True if the unit is in queue.
   */
  boolean inQueue

  /**
   * Empty constructor.
   */
  FindWorkResponseDetail() {
    init()
  }

  /**
   * Map constructor.  Provides the standard Groovy map constructor.
   * Supports use of SQL column-name versions of the fields (e.g. 'qty_in_queue').
   * <h3>Column Names Supported</h3>
   * <ul>
   *   <li><b>qty_in_queue</b></li>
   *   <li><b>qty_in_work</b></li>
   *   <li><b>sequence</b> - operationSequence</li>
   *   <li><b>uuid</b> - The record's UUID.</li>
   *   <li><b>lsn</b></li>
   *   <li><b>order</b></li>
   *   <li><b>lsn_id</b></li>
   *   <li><b>order_id</b></li>
   * </ul>
   * @param options The options map.
   */
  FindWorkResponseDetail(Map options) {
    options?.each { k, v ->
      // Maybe it is a column name
      def name = NameUtils.convertFromColumnName(k as String)

      // Handle some special cases
      switch (k) {
        case 'sequence':
          name = 'operationSequence'
          break;
        case 'order_id':
          name = 'orderID'
          break;
        case 'ordr':
          name = 'order'
          break;
        case 'lsn_id':
          name = 'lsnID'
          break;
        case 'uuid':
          name = 'id'
          break;
      }

      if (this.hasProperty(name)) {
        this[name] = v
      } else {
        // Try name as a field
        this[k as String] = v
      }
    }
    init()
  }
/*
                                               'dateQtyQueued', 'dateQtyStarted', 'dateFirstQueued', 'dateFirstStarted']
 */

  /**
   * Finishes the common logic for most of the copy constructors.
   * Mainly sets the inWork/Queue flags based on the quantities.
   */
  def init() {
    inQueue = (qtyInQueue)
    inWork = (qtyInWork)
  }

}
