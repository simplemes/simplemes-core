package org.simplemes.mes.demand.service

import groovy.transform.Synchronized
import groovy.util.logging.Slf4j
import io.micronaut.data.model.Pageable
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.archive.ArchiverFactoryInterface
import org.simplemes.eframe.archive.FileArchiver
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.mes.demand.LSNTrackingOption
import org.simplemes.mes.demand.OrderReleaseRequest
import org.simplemes.mes.demand.OrderReleaseResponse
import org.simplemes.mes.demand.WorkableInterface
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.LSNOperState
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.demand.domain.OrderOperState
import org.simplemes.mes.demand.domain.OrderOperation
import org.simplemes.mes.tracking.domain.ActionLog

import javax.inject.Singleton
import javax.transaction.Transactional

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * This service provided methods to manipulate orders in SimpleMES.  This contains the core business logic
 * for orders.  This provides methods to release an order for production and close an order when completed early.
 *
 * <h3>Logging</h3>
 * The logging for this class that can be enabled:
 * <ul>
 *   <li><b>debug</b> - Debugging information. Typically includes inputs and outputs. </li>
 * </ul>
 *
 */
@Slf4j
@Singleton
@SuppressWarnings("GrMethodMayBeStatic")
class OrderService {

  /**
   * The {@link org.simplemes.mes.tracking.domain.ActionLog} entry used for Release order actions.
   */
  public static final String ACTION_RELEASE_ORDER = "RELEASE_ORDER"

  /**
   * The {@link org.simplemes.mes.tracking.domain.ActionLog} entry used for Release LSN actions.
   */
  public static final String ACTION_RELEASE_LSN = "RELEASE_LSN"

  /**
   * The time the last log warning message was triggered.
   */
  long lastLogTime = 0  // This used to log when auto-deleting archive records.

  /**
   * Internal field used for performance testing support.
   */
  private static Long stableRowCount = null

  /**
   * Release the given order for production.  The order cannot be worked on the shop floor until it is released.
   * This method makes the quantity released available for production by queueing it the appropriate entry point (first
   * operation if there is a routing needed).<p>
   * <b>Note</b>: This method is part of the Stable API.
   * @param orderReleaseRequest The details of the release request (order and qty).
   * @param quantity The quantity to release.  If not given, then the entire un-released quantity will be released.
   */
  @SuppressWarnings("GrUnnecessaryDefModifier")
  //@Transactional
  OrderReleaseResponse release(OrderReleaseRequest orderReleaseRequest) {
    log.debug('release() params: {}', orderReleaseRequest)
    ArgumentUtils.checkMissing(orderReleaseRequest, 'orderReleaseRequest')
    Order order = orderReleaseRequest.order
    BigDecimal quantity = orderReleaseRequest.qty
    ArgumentUtils.checkMissing(order, 'order')
    def dateReleased = orderReleaseRequest.dateTime ?: new Date()

    validateRelease(order, quantity)

    quantity = quantity ?: order.qtyToBuild - order.qtyReleased

    // Figure out if we have a routing to use here.
    def routing = order.operations ? order : null
    if (!routing) {
      // No order routing already, so see if we should grab it from the product.
      routing = order?.product?.determineEffectiveRouting()
      if (routing) {
        order.operations = []
        for (operation in routing.operations) {
          order.operations << new OrderOperation(operation)
        }
      }
    }
    // Now, populate the LSNs (if needed)
    if (order.lsnTrackingOption == LSNTrackingOption.LSN_ONLY) {
      order.populateLSNs()
    }

    // Figure out where to place the qtyInQueue.
    if (routing) {
      // A routing is to be used, so create the operation state records.
      routing.operations.sort()
      if (order.lsns) {
        for (lsn in order.lsns) {
          for (oper in routing.operations) {
            lsn.operationStates << new LSNOperState(oper)
          }
          lsn.operationStates[0].queueQty(lsn.qty, dateReleased)
        }
      } else {
        // No LSNs, so we will just have order-level states
        for (oper in routing.operations) {
          order.operationStates << new OrderOperState(oper)
        }
        // Place released Qty in queue at first step.
        order.operationStates[0].queueQty(quantity, dateReleased)
      }
    } else {
      // No routing to be used, so use top-level qty's.
      if (order.product) {
        // If we have a product and LSNs are used, place the qty in queue there.
        if (order.product.lsnTrackingOption.isLSNAllowed() && order.lsns) {
          for (lsn in order.lsns) {
            lsn.queueQty(lsn.qty, dateReleased)
          }
        }
      } else {
        // No product, so handle LSNs if they exist.
        for (lsn in order.lsns) {
          lsn.queueQty(lsn.qty, dateReleased)
        }
      }
      if (!order.lsns) {
        // No LSNs, so
        order.queueQty(quantity, dateReleased)
      }
    }

    order.qtyReleased += quantity
    order.dateReleased = order.dateReleased ?: dateReleased

    order.save()   // Force slush for rollback unit tests.
    logReleaseActions(orderReleaseRequest, quantity)

    def response = new OrderReleaseResponse(order: order, qtyReleased: quantity)
    log.debug('release() returns: {}', response)
    return response
  }

  /**
   * Validate that the release can take place.
   * @param order The order to release.
   * @param quantity The quantity to release.  If not given, then the entire un-released quantity will be released.
   */
  private void validateRelease(Order order, BigDecimal quantity) {
    ArgumentUtils.checkMissing(order, 'order')
    if (!order.overallStatus.workable) {
      //error.3001.message=Order {0} cannot be worked.  It has a status of {1}.
      throw new BusinessException(3001, [order.order, order.overallStatus.toString()])
    }

    // Check for legal quantity to release
    def quantityAvailable = order.qtyToBuild - order.qtyReleased
    if (quantityAvailable <= 0) {
      //error.3005.message=Can''t release more quantity.  All of the quantity to build ({0}) has been released.
      throw new BusinessException(3005, [order.qtyToBuild])
    }
    if (quantity) {
      if (quantity <= 0) {
        //error.3002.message=Quantity ({0}) must be greater than 0
        throw new BusinessException(3002, [quantity])
      }
      if (quantity > quantityAvailable) {
        //error.3004.message=Quantity {0} can't be released.  A quantity of {1} is available.
        throw new BusinessException(3004, [quantity, quantityAvailable])
      }
    } else {
      // No quantity, so calculate it and use it for the release.
      quantity = quantityAvailable
    }

  }

  /**
   * Log the release actions for the order and any LSNs.
   * @param order The order released.
   * @param quantity The quantity released.
   */
  private void logReleaseActions(OrderReleaseRequest orderReleaseRequest, BigDecimal quantity) {
    def order = orderReleaseRequest.order
    if (order.lsns) {
      for (lsn in order.lsns) {
        ActionLog l = new ActionLog()
        l.action = ACTION_RELEASE_LSN
        l.dateTime = orderReleaseRequest.dateTime ?: new Date()
        l.qty = lsn.qty
        l.lsn = lsn
        l.order = order
        l.product = order.product
        l.save()
      }
    }
    ActionLog l = new ActionLog()
    l.action = ACTION_RELEASE_ORDER
    l.dateTime = orderReleaseRequest.dateTime ?: new Date()
    l.qty = quantity
    l.order = order
    l.product = order.product
    l.save()
  }

  /**
   * Deletes the given order and related records.  There is no confirmation for this delete.
   * It relies on the {@link Order#findRelatedRecords()}
   * @param order The order to delete.
   */
  @SuppressWarnings("GrUnnecessaryDefModifier")
  @Transactional
  void delete(Order order) {
    // Delete the related records first, to avoid
    def records = order.findRelatedRecords()
    for (record in records) {
      record.delete()
    }
    order.delete()
  }

  /**
   * Archive old, done orders based on the date that they were completed.  There are options
   * for the age (days) and the max number of orders to archive in one batch (transaction).  There is also an option to limit the number
   * batches archived at a time.  This last option is used to limit the slowdown from archiving during 'catch-up' scenarios when
   * archiving has not been run recently.  Values that exceed the maximum will be ignored and the maximum will be used.
   * <p/>
   * This method is typically called automatically once per day using the job scheduler.
   * <p>
   * <b>Note:</b> This method creates new transactions when archiving each batch of records.
   *              If called from within an existing transaction, then no new transactions will be created.
   * @param ageDays Any orders completed more than this number of days in the past are eligible for archive.
   *                If 0 (default), then the config setting <b>org.simplemes.eframe.archive.ageDays</b> is used.
   *                Fractions allowed. <b>Default:</b> 100
   * @param maxOrdersPerTxn The max number of orders to archive per transaction.  <b>Default:</b> 100. <b>Maximum:</b> 500.
   * @param maxTxns The max number of orders to archive per transaction.  <b>Default:</b> 5. <b>Maximum:</b> 50.
   * @return The list of archive references created by this archive process.
   */
  @SuppressWarnings("JavaIoPackageAccess")
  @Synchronized
  List<String> archiveOld(BigDecimal ageDays = 0, Long maxOrdersPerTxn = 100, Integer maxTxns = 5) {
    /*
      This method also has an undocumented archiving mode to support long-term performance testing.
      If you pass in all values with a value of -1, then all calls to this method will attempt to
      keep the number of order records stable.  The first call with (-1,-1,-1) will count the number
      of order records.  Later calls will attempt to archive a small number (50 max) until the original record
      count is reached.
      This is not to be used in production.
      This is normally called after the performance test is done with a test order and wants to clean up the records.
     */
    def res = []
    boolean stableMode = false

    if (ageDays == -1 && maxOrdersPerTxn == -1 && maxTxns == -1) {
      stableMode = true
      // Handle special case for performance testing.
      ageDays = 0.0001
      maxTxns = 1
      maxOrdersPerTxn = 50
      if (stableRowCount == null) {
        // Set the stable row count for later calls and do nothing else now.
        stableRowCount = Order.count()
        //println "stableRowCount = ${stableRowCount}"
        log.warn("Archive: Stable Archive Rows set to {}", stableRowCount)
        return res
      }
      // See how many orders we might want to archive.
      maxOrdersPerTxn = Order.count() - stableRowCount
      if (maxOrdersPerTxn <= 0) {
        // Do nothing since the rows have not grown.
        return res
      }
      maxOrdersPerTxn = Math.min(maxOrdersPerTxn, 50)  // No more than 50 at a time.
    } else {
      // Check the parameters
      ageDays = ageDays ?: Holders.configuration.archive.ageDays
      if (ageDays <= 0) {
        throw new IllegalArgumentException("ageDays must be greater than 0")
      }

      maxOrdersPerTxn = maxOrdersPerTxn ?: 100
      maxOrdersPerTxn = Math.min(maxOrdersPerTxn, 500)
      maxOrdersPerTxn = Math.max(maxOrdersPerTxn, 1)

      maxTxns = maxTxns ?: 5
      maxTxns = Math.min(maxTxns, 50)
      maxTxns = Math.max(maxTxns, 1)
    }

    Date archiveDate = DateUtils.subtractDays(new Date(), ageDays)

    int txnCount = 0
    while (txnCount < maxTxns) {
      // Process one batch of orders inside of its own txn.
      Order.withTransaction {
        Order[] list = Order.findAllByDateCompletedLessThan(archiveDate, Pageable.from((Integer) 0, (Integer) maxOrdersPerTxn))
        //println "list = ${list*.order}"
        if (list.size() <= 0) {
          // Since we are inside of a 'withTransaction' closure, we can't use break.  kludge an exit.
          txnCount = maxTxns + 1
        }
        for (order in list) {
          def factory = Holders.applicationContext.getBean(ArchiverFactoryInterface)
          def archiver = factory.archiver
          archiver.archive(order)
          def ref = archiver.close()
          res << ref
        }
      }
      txnCount++
    }
    if (stableMode) {
      // Clean up archive files to avoid out of file space errors on stability/performance tests.
      for (ref in res) {
        File f = new File(FileArchiver.makePathFromReference(ref))
        f.exists()
        f.delete()
        log.debug("Deleted archive file ${f}!!!!!!!!")
      }
      def timeSinceLastLogging = System.currentTimeMillis() - lastLogTime
      if (timeSinceLastLogging > DateUtils.MILLIS_PER_DAY) {
        // Log a warning once every day, just to to make sure nobody accidentally triggers this option.
        log.error("Automatically Deleting archive file!!!!!!!!!!!! stableRowCount = {}.  Triggered when: ageDays/maxOrdersPerTxn/maxTxns = -1.", stableRowCount)
        lastLogTime = System.currentTimeMillis()
      }
    }
    return res
  }

  /**
   * Finds all places the given order is in queue or in work.
   * @param order The order to query.
   * @return A list of workable objects where the order is in queue or in work.  Sorted on operation sequence if an operation object.
   */
  List<WorkableInterface> determineQtyStates(Order order) {
    def res = []

    if (order?.operationStates) {
      // Find all operation states than have a qty in queue/work
      for (operationState in order.operationStates) {
        if (operationState.qtyInQueue || operationState.qtyInWork) {
          res << operationState
        }
      }
      res.sort { a, b -> a.sequence <=> b.sequence }
    } else {
      // Find simple order, no routing state
      if (order.qtyInQueue || order.qtyInWork) {
        res << order
      }
    }

    return res
  }

  /**
   * Finds all places the given LSN is in queue or in work.
   * @param lsn The LSN to query.
   * @return A list of workable objects where the LSN is in queue or in work.  Sorted on operation sequence if an operation object.
   */
  List<WorkableInterface> determineQtyStates(LSN lsn) {
    def res = []

    if (lsn?.operationStates) {
      // Find all operation states than have a qty in queue/work
      for (operationState in lsn.operationStates) {
        if (operationState.qtyInQueue || operationState.qtyInWork) {
          res << operationState
        }
      }
      res.sort { a, b -> a.sequence <=> b.sequence }
    } else {
      // Find simple lsn, no routing state
      if (lsn.qtyInQueue || lsn.qtyInWork) {
        res << lsn
      }
    }

    return res
  }

}
