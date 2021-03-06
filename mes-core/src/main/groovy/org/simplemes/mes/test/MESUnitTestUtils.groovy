package org.simplemes.mes.test

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.mes.demand.LSNTrackingOption
import org.simplemes.mes.demand.OrderReleaseRequest
import org.simplemes.mes.demand.WorkStateTrait
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.LSNSequence
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.demand.domain.OrderSequence
import org.simplemes.mes.demand.service.OrderService
import org.simplemes.mes.numbering.CodeSequenceTrait
import org.simplemes.mes.product.domain.MasterOperation
import org.simplemes.mes.product.domain.MasterRouting
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.product.domain.ProductOperation

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Convenience utilities for MES unit tests.  Used to create common data structures.
 * Can also be used for GUI/Integration tests with proper transaction settings.
 * Currently, you should use the static methods.  The internal instance methods are used
 * to allow sub-classing.
 */
class MESUnitTestUtils {

  /**
   * A singleton, used to allow sub-classes to provide additional features.
   */
  static MESUnitTestUtils instance = new MESUnitTestUtils()


  /**
   * Build a product with a simple 3 operation routing for use in an product.
   *
   * <h3>Options</h3>
   * The options can be:
   * <ul>
   *   <li><b>product</b> - The product to create.  (<b>Default</b>: 'PC')</li>
   *   <li><b>operations</b> - A List of operation sequences (Integers).  (<b>Default</b>: [1,2,3])</li>
   *   <li><b>lsnTrackingOption</b> - Configures the LSN option (<b>Default</b>: LSNTrackingOption.ORDER_ONLY) </li>
   * </ul>
   * <b>Note:</b> If the lsnTrackingOption is LSNTrackingOption.LSN_ONLY, then default LSNSequence is used for the product.
   * @param options The options used to build the product.  Supports operations(List of sequences (integers))
   * @return The product.
   */
  static Product buildSimpleProductWithRouting(Map options = [:]) {
    LSNTrackingOption lsnTrackingOption = (LSNTrackingOption) options.lsnTrackingOption ?: LSNTrackingOption.ORDER_ONLY
    def lsnSequence = null
    if (lsnTrackingOption == LSNTrackingOption.LSN_ONLY) {
      lsnSequence = LSNSequence.findDefaultSequence()
    }

    def id = options.product ?: 'PC'
    Product product = new Product(product: id, lsnTrackingOption: lsnTrackingOption, lsnSequence: lsnSequence,)

    List<Integer> operations = (List<Integer>) options.operations ?: [1, 2, 3]
    for (sequence in operations) {
      def operation = new ProductOperation(sequence: sequence, title: "Oper $sequence")
      product.operations << operation
    }
    product.save()

    return product
  }

  /**
   * Build a simple master routing the given operations.
   *
   * <h3>Options</h3>
   * The options can be:
   * <ul>
   *   <li><b>operations</b> - A List of operation sequences (Integers).  (<b>Default</b>: [1,2,3])</li>
   *   <li><b>id</b> - A semi-unique ID to help identify records created by this method (<b>Default</b>: '') </li>
   * </ul>
   * @param options The options used to build the routing.
   * @return The master routing.
   */
  static MasterRouting buildMasterRouting(Map options = [:]) {
    def id = options.id ? "-${options.id}" : ''
    List<Integer> operations = (List<Integer>) options.operations ?: [1, 2, 3]
    def masterRouting = new MasterRouting(routing: "ROUTING$id")
    for (sequence in operations) {
      def operation = new MasterOperation(sequence: sequence, title: "Oper $sequence")
      masterRouting.operations << operation
    }
    return masterRouting.save()
  }

  /**
   * Create and release a single order, optionally on a routing with or without LSNs.
   * This is a convenience function that calls {@link #releaseOrders(java.util.Map)}.
   * <p>
   * <b>This method creates a <b>transaction</b> (if needed).</b>.
   *
   * @param options See for the {@link #releaseOrders(java.util.Map)} for details.  (<b>Optional</b>)
   * @return The order(s) created and released.
   */
  static Order releaseOrder(Map options = [:]) {
    options.nOrders = 1
    def list = releaseOrders(options)
    return list[0]
  }

  /**
   * Create and release orders, optionally on a routing with or without LSNs.
   * <p>
   * <b>This method creates a <b>transaction</b> (if needed).</b>.
   *
   * <h3>Options</h3>
   * The options can be:
   * <ul>
   *   <li><b>id</b> - A semi-unique ID to help identify records created by this method (<b>Default</b>: '') </li>
   *   <li><b>nOrders</b> - The number of orders to create (<b>Default</b>: 1) </li>
   *   <li><b>operations</b> - A List of operation sequences (Integers).  This list is sorted in reverse order before save().  (<b>Default</b>: No routing)</li>
   *   <li><b>masterRouting</b> - The name of the master routing to create for this order.  Will use the unique ID in the key.  (<b>Default</b>: No routing)</li>
   *   <li><b>productName</b> - The name of the product for the order. (<b>Default</b>: No product unless Operations or LSN tracking is set)</li>
   *   <li><b>qty</b> - The number of pieces to build for each order (<b>Default</b>: 1.0) </li>
   *   <li><b>qtyInWork</b> - The number of pieces to move to in work (<b>Default</b>: 0.0).  Allows easy testing of inWork orders. </li>
   *   <li><b>qtyDone</b> - The number of pieces to move to qtyDone (<b>Default</b>: 0.0).  Allows easy testing of completed orders. </li>
   *   <li><b>lotSize</b> - The size of each LSN created (if configured for LSNs) (<b>Default</b>: 1.0) </li>
   *   <li><b>lsnTrackingOption</b> - Configures the LSN option (<b>Default</b>: LSNTrackingOption.ORDER_ONLY) </li>
   *   <li><b>lsnSequence</b> - The LSN Sequence to use to generate the LSN names (<b>Default</b>: default LSN Sequence) </li>
   *   <li><b>lsns</b> - A list of LSN names (Strings) to use for the order. LSNs are created using this and the order # (if nOrders>1).  (<b>Default</b>: default LSN Sequence) </li>
   *   <li><b>spreadQueuedDates</b> - If true, then the dateFirstQueued for all created objects will be spread from a date 14 days ago (+1 second for each).  (<b>Default</b>: false) </li>
   *   <li><b>orderSequenceStart</b> - The sequence number for the first order created (<b>Default</b>: 1000) </li>
   * </ul>
   * <b>Note:</b> If the lsnTrackingOption is LSNTrackingOption.LSN_ONLY, then an LSNSequence is used for the product.
   * <p>
   * <b>Note:</b> The records created by this utility should be cleaned up using this option (for sub-classes of the test specs).
   * <pre>
   *   static dirtyDomains = [ActionLog, Order, Product]
   * </pre>
   *
   * @param options See options above. (<b>Optional</b>)
   * @return The order(s) created and released.
   */
  static List<Order> releaseOrders(Map options = [:]) {
    return instance.releaseOrdersInternal(options)
  }

  /**
   * Internal create and release orders.  This is used as a non-static method to allow sub-classes to alter the
   * objects created.
   * <p>
   * <b>This method creates a <b>transaction</b> (if needed).</b>.
   *
   * @param options See for the {@link #releaseOrders(java.util.Map)} for details.  (<b>Optional</b>)
   * @return The order(s) created and released.
   */
  @SuppressWarnings('AbcMetric')
  protected List<Order> releaseOrdersInternal(Map options = [:]) {
    List<Order> list = []
    Order.withTransaction {
      loadOrderInitialDataNeeded(options)
      setLoginUser()

      try {
        def nOrders = options.nOrders ?: 1
        def id = options.id ? "-${options.id}" : ''
        def lotSize = (BigDecimal) options.lotSize ?: 1.0
        def qty = (BigDecimal) options.qty ?: 1.0
        def lsnTrackingOption = (LSNTrackingOption) options.lsnTrackingOption ?: LSNTrackingOption.ORDER_ONLY
        def lsnSequence = (LSNSequence) options.lsnSequence
        def productName = options?.productName

        // Create the product we need
        List<Integer> operationSequences = (List<Integer>) options.operations ?: []
        def mpr = null
        if (operationSequences) {
          // Reverse the order to uncover any sorting issues in the test programs or app code.
          operationSequences.sort { a, b -> b <=> a }
          if (options.masterRouting) {
            // A master routing is needed.
            mpr = new MasterRouting(routing: "${options.masterRouting}$id")
            for (sequence in operationSequences) {
              def operation = new MasterOperation(sequence: sequence, title: "Oper $sequence $id")
              adjustOperation(operation,options)
              mpr.operations << operation
            }
            mpr.save()
          }
        }
        Product product = null

        if (operationSequences || lsnTrackingOption != LSNTrackingOption.ORDER_ONLY|| productName) {
          // Only need a product for a routing and non-default LSN Tracking Options.
          productName = productName ?:  "PC$id"
          product = new Product(product: productName, lotSize: lotSize,
                                lsnTrackingOption: lsnTrackingOption,
                                lsnSequence: lsnSequence)
          if (operationSequences) {
            // A product-specific routing is needed.
            for (sequence in operationSequences) {
              def operation = new ProductOperation(sequence: sequence, title: "Oper $sequence $id")
              adjustOperation(operation,options)
              product.operations << operation
            }

          }
          product.masterRouting = mpr
          adjustProduct(product,options)
          product.save()
        }
        def orderService = Holders.getBean(OrderService)
        def seq = options.orderSequenceStart ?: 1000
        (1..nOrders).each {
          def order = new Order(order: "M$seq$id", product: product, qtyToBuild: qty)
          adjustOrder(order,options)
          order.save()
          for (lsnBase in options?.lsns) {
            def lsnSuffix = nOrders > 1 ? "$nOrders" : ''
            order.lsns << new LSN(lsn: "$lsnBase$lsnSuffix")
          }
          orderService.release(new OrderReleaseRequest(order))
          seq++
          list << order
          forceQtyInWork(order, options?.qtyInWork as BigDecimal)
          forceQtyDone(order, options?.qtyDone as BigDecimal)
        }
        if (options.spreadQueuedDates) {
          spreadDates(list)
        }
      } finally {
        clearLoginUser()
      }
    }

    return list
  }

  /**
   * Forces the qty to in work for the given order (and LSNs if any).
   * @param order The order.
   * @param qtyInWork The qty to take from inQueue and move to in work.  Only moved if the qtyInQueue is large enough.
   */
  protected void forceQtyInWork(Order order, BigDecimal qtyInWork) {
    if (!qtyInWork) {
      return
    }
    if (order.qtyInQueue >= qtyInWork) {
      order.qtyInWork += qtyInWork
      order.qtyInQueue -= qtyInWork
    }
    for (lsn in order.lsns) {
      if (lsn.qtyInQueue >= qtyInWork) {
        lsn.qtyInWork += qtyInWork
        lsn.qtyInQueue -= qtyInWork
      }
    }
    // force a second save
    order.save()
  }

  /**
   * Forces the qty to done for the given order (and LSNs if any).
   * @param order The order.
   * @param qtyDone The qty to take from inQueue and move to done.  Only moved if the qtyInQueue is large enough.
   */
  protected void forceQtyDone(Order order, BigDecimal qtyDone) {
    if (!qtyDone) {
      return
    }
    if (order.qtyInQueue >= qtyDone) {
      order.qtyDone += qtyDone
      order.qtyInQueue -= qtyDone
    }
    for (lsn in order.lsns) {
      if (lsn.qtyInQueue >= qtyDone) {
        lsn.qtyDone += qtyDone
        lsn.qtyInQueue -= qtyDone
      }
    }
    // force a second save
    order.save()
  }

  /**
   * Utility method to set the dates queued/started to a known value for a list of orders/LSNs.
   * Processes all LSNs in the orders for all possible routing steps.
   * @param orders The orders to set the dates on.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  protected void spreadDates(List<Order> orders) {
    def timeStamp = System.currentTimeMillis() - DateUtils.MILLIS_PER_DAY * 14
    for (order in orders) {
      timeStamp = setDates(order, timeStamp)
      for (orderOperState in order.operationStates) {
        timeStamp = setDates(orderOperState, timeStamp)
      }
      for (lsn in order.lsns) {
        timeStamp = setDates(lsn, timeStamp)
        for (lsnOperState in lsn.operationStates) {
          timeStamp = setDates(lsnOperState, timeStamp)
        }
      }
      order.save()
    }
  }

  /**
   * Allows sub-classes to adjust the Product before it is saved.
   * @param product The un-saved product.
   * @param options the options for the order release.
   */
  @SuppressWarnings("unused")
  protected void adjustProduct(Product product,Map options) {
  }

  /**
   * Allows sub-classes to adjust the ProductOperation before it is saved.
   * @param operation The un-saved operation.
   * @param options the options for the order release.
   */
  @SuppressWarnings("unused")
  protected void adjustOperation(ProductOperation operation,Map options) {
  }

  /**
   * Allows sub-classes to adjust the MasterOperation before it is saved.
   * @param operation The un-saved operation.
   * @param options the options for the order release.
   */
  @SuppressWarnings("unused")
  protected void adjustOperation(MasterOperation operation,Map options) {
  }

  /**
   * Allows sub-classes to adjust the Order before it is saved.
   * @param product The un-saved order.
   * @param options the options for the order release.
   */
  @SuppressWarnings("unused")
  protected void adjustOrder(Order order,Map options) {
  }

  /**
   * Sets the date queued/started (if already set) to the given timestamp and then increments the timestamp.
   * @param workState The state object to update.
   * @param timeStamp The time to set the dates to.
   * @return The updated timestamp.
   */
  protected long setDates(WorkStateTrait workState, long timeStamp) {
    if (workState.dateFirstQueued) {
      workState.dateFirstQueued = new Date(timeStamp)
      timeStamp += 1001
    }
    if (workState.dateFirstStarted) {
      workState.dateFirstStarted = new Date(timeStamp)
      timeStamp += 1001
    }
    return timeStamp
  }


  /**
   * Loads any initial data needed for order release.  Always loads the OrderStatus.
   * @param options Uses the lsnTrackingOption to decide if LSN Sequence and Status must be loaded.
   */
  static loadOrderInitialDataNeeded(Map options) {
    if (options?.lsnTrackingOption == LSNTrackingOption.LSN_ONLY) {
      if (!options?.lsns) {
        LSNSequence.initialDataLoad()
      }
    }
  }

  /**
   * Resets all of the codeSequences for the test database.
   */
  static resetCodeSequences() {
    resetSequence(LSNSequence)
    resetSequence(OrderSequence)
  }

  /**
   * Resets the currentSequence for thie given CodeSequenceTrait to the given sequence.  This is used to clean up after tests
   * @param sequenceClass The sequence to reset all sequences for.
   * @param newSequence The new current sequence to use.  (<b>Default:</b> 1000)
   */
  static resetSequence(Class sequenceClass, long newSequence = 1000) {
    List<CodeSequenceTrait> sequences = sequenceClass.list()
    for (sequence in sequences) {
      sequence.currentSequence = newSequence
      // Flush to avoid the dreaded DuplicateKeyException that happens with @Rollback and existing Sequence records.
      sequence.save()
    }
  }

  static String originalUserOverride = null

  /**
   * Sets the current user for order release and other actions.
   */
  static void setLoginUser() {
    originalUserOverride = SecurityUtils.currentUserOverride
    SecurityUtils.currentUserOverride = SecurityUtils.TEST_USER
  }

  /**
   * Resets the user override to the value we found on setLoginUser().
   */
  static void clearLoginUser() {
    SecurityUtils.currentUserOverride = originalUserOverride
    originalUserOverride = null

  }


}
