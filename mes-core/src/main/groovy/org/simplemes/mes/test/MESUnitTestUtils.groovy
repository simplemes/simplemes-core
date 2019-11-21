package org.simplemes.mes.test

import org.simplemes.mes.demand.LSNTrackingOption
import org.simplemes.mes.demand.OrderReleaseRequest
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.LSNSequence
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.demand.domain.OrderSequence
import org.simplemes.mes.demand.service.OrderService
import org.simplemes.mes.numbering.domain.CodeSequence
import org.simplemes.mes.product.domain.MasterRouting
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.product.domain.ProductRouting
import org.simplemes.mes.product.domain.RoutingOperation

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Convenience utilities for MES unit tests.  Used to create common data structures.
 * Can also be used for GUI/Integration tests with proper transaction settings.
 */
class MESUnitTestUtils {

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
    List<Integer> operations = (List<Integer>) options.operations ?: [1, 2, 3]
    def pr = new ProductRouting()
    for (sequence in operations) {
      def operation = new RoutingOperation(sequence: sequence, title: "Oper $sequence")
      pr.addToOperations(operation)
    }
    LSNTrackingOption lsnTrackingOption = (LSNTrackingOption) options.lsnTrackingOption ?: LSNTrackingOption.ORDER_ONLY
    def lsnSequence = null
    if (lsnTrackingOption == LSNTrackingOption.LSN_ONLY) {
      lsnSequence = LSNSequence.findDefaultSequence()
    }

    def ID = options.product ?: 'PC'
    Product product = new Product(product: ID, lsnTrackingOption: lsnTrackingOption, lsnSequence: lsnSequence,
                                  productRouting: pr).save()
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
      def operation = new RoutingOperation(sequence: sequence, title: "Oper $sequence")
      masterRouting.addToOperations(operation)
    }
    return masterRouting.save()
  }

  /**
   * Create and release orders, optionally on a routing with or without LSNs.
   *
   * <h3>Options</h3>
   * The options can be:
   * <ul>
   *   <li><b>id</b> - A semi-unique ID to help identify records created by this method (<b>Default</b>: '') </li>
   *   <li><b>nOrders</b> - The number of orders to create (<b>Default</b>: 1) </li>
   *   <li><b>operations</b> - A List of operation sequences (Integers).  This list is sorted in reverse order before save().  (<b>Default</b>: No routing)</li>
   *   <li><b>masterRouting</b> - The name of the master routing to create for this order.  Will use the unique ID in the key.  (<b>Default</b>: No routing)</li>
   *   <li><b>qty</b> - The number of pieces to build for each order (<b>Default</b>: 1.0) </li>
   *   <li><b>lotSize</b> - The size of each LSN created (if configured for LSNs) (<b>Default</b>: 1.0) </li>
   *   <li><b>lsnTrackingOption</b> - Configures the LSN option (<b>Default</b>: LSNTrackingOption.ORDER_ONLY) </li>
   *   <li><b>lsnSequence</b> - The LSN Sequence to use to generate the LSN names (<b>Default</b>: default LSN Sequence) </li>
   *   <li><b>lsns</b> - A list of LSN names (Strings) to use for the order. LSNs are created using this and the ID and the order # (if needed).  (<b>Default</b>: default LSN Sequence) </li>
   * </ul>
   * <b>Note:</b> If the lsnTrackingOption is LSNTrackingOption.LSN_ONLY, then an LSNSequence is used for the product.
   *
   * <b>Note:</b> The records created by this utility should be cleaned up using this option (for sub-classes of the test specs).
   * <pre>
   *   static domainClassesToClearOnEachRun = [ActionLog, Order, Product]
   * </pre>
   *
   * @param options See options above. (<b>Optional</b>)
   * @return The order(s) created and released.
   */
  static List<Order> releaseOrders(Map options = [:]) {
    loadOrderInitialDataNeeded(options)
    setLoginUser()

    List<Order> list = []
    def nOrders = options.nOrders ?: 1
    def id = options.id ? "-${options.id}" : ''
    def lotSize = (BigDecimal) options.lotSize ?: 1.0
    def qty = (BigDecimal) options.qty ?: 1.0
    def lsnTrackingOption = (LSNTrackingOption) options.lsnTrackingOption ?: LSNTrackingOption.ORDER_ONLY
    def lsnSequence = (LSNSequence) options.lsnSequence

    // Create the product we need
    List<Integer> operations = (List<Integer>) options.operations ?: []
    def pr = null
    def mpr = null
    if (operations) {
      if (options.masterRouting) {
        // A master routing is needed.
        operations.sort() { a, b -> b <=> a }
        mpr = new MasterRouting(routing: "${options.masterRouting}$id")
        for (sequence in operations) {
          def operation = new RoutingOperation(sequence: sequence, title: "Oper $sequence $id")
          mpr.addToOperations(operation)
        }
        mpr.save()
      } else {
        // A product-specific routing is needed.
        // Reverse the order to uncover any sorting issues in the test programs or app code.
        operations.sort() { a, b -> b <=> a }
        pr = new ProductRouting()
        for (sequence in operations) {
          def operation = new RoutingOperation(sequence: sequence, title: "Oper $sequence $id")
          pr.addToOperations(operation)
        }
      }
    }
    Product product = null

    if (operations || lsnTrackingOption != LSNTrackingOption.ORDER_ONLY) {
      // Only need a product for a routing and non-default LSN Tracking Options.
      product = new Product(product: "PC$id", lotSize: lotSize,
                            lsnTrackingOption: lsnTrackingOption,
                            lsnSequence: lsnSequence)
      product.productRouting = pr
      product.masterRouting = mpr
      product.save()
    }
    def orderService = new OrderService()
    def seq = 1000
    (1..nOrders).each {
      def order = new Order(order: "M$seq$id", product: product, qtyToBuild: qty).save()
      for (lsnBase in options?.lsns) {
        def lsnSuffix = nOrders > 1 ? "$nOrders" : ''
        order.addToLsns(new LSN(lsn: "$lsnBase$id$lsnSuffix"))
      }
      orderService.release(new OrderReleaseRequest(order))
      seq++
      list << order
    }

    clearLoginUser()
    return list
  }

  /**
   * Create and release a single order, optionally on a routing with or without LSNs.
   * This is a convenience function that calls {@link #releaseOrders(java.util.Map)}.
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
   * Resets the currentSequence for thie given CodeSequence to the given sequence.  This is used to clean up after tests
   * @param sequenceClass The sequence to reset all sequences for.
   * @param newSequence The new current sequence to use.  (<b>Default:</b> 1000)
   */
  static resetSequence(Class sequenceClass, long newSequence = 1000) {
    List<CodeSequence> sequences = sequenceClass.list()
    for (sequence in sequences) {
      sequence.currentSequence = newSequence
      // Flush to avoid the dreaded DuplicateKeyException that happens with @Rollback and existing Sequence records.
      sequence.save(flush: true)
    }
  }

  static void setLoginUser() {
  }

  static void clearLoginUser() {
  }


}