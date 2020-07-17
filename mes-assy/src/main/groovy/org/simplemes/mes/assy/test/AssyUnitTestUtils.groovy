package org.simplemes.mes.assy.test

import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.mes.assy.demand.AssembledComponentStateEnum
import org.simplemes.mes.assy.demand.domain.OrderAssembledComponent
import org.simplemes.mes.assy.demand.domain.OrderBOMComponent
import org.simplemes.mes.assy.product.domain.ProductComponent
import org.simplemes.mes.assy.system.service.ScanAssyService
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.system.ScanRequest
import org.simplemes.mes.system.ScanResponse
import org.simplemes.mes.test.MESUnitTestUtils

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Convenience utilities for MES Assy module unit tests.  Used to create common data structures.
 * Can also be used for GUI/Integration tests with proper transaction settings.
 * <p>
 * <b>Note:</b> These methods should only be used in Tests.
 */
class AssyUnitTestUtils extends MESUnitTestUtils {

  /**
   * A singleton, used to allow sub-classes to provide additional features.
   */
  static AssyUnitTestUtils instance = new AssyUnitTestUtils()

  /**
   * Create and release orders with product components with or without LSNs.
   * <p>
   * <b>This method creates a <b>transaction</b> (if needed).</b>.
   *
   * @param options See for the {@link #releaseOrder(java.util.Map)} for details.  (<b>Optional</b>)
   * @return The order(s) created and released.
   */
  static List<Order> releaseOrders(Map options = [:]) {
    if (options.components) {
      // Force a product for the base-class creation.
      options.productName = options.product ?: "PC"
    }
    return instance.releaseOrdersInternal(options)
  }

  /**
   * Create and release a single order, optionally on a routing with or without LSNs.
   * This is a convenience function that calls {@link #releaseOrders(java.util.Map)}.
   * <p>
   * <b>This method creates a <b>transaction</b> (if needed).</b>.
   * <h3>Options</h3>
   * See {@link MESUnitTestUtils#releaseOrders(java.util.Map)} for other options.
   * The options can be:
   * <ul>
   *   <li><b>components</b> - A List of component names (Strings). Sequence and Qty are incremented  (<b>Default</b>: No components or product used). </li>
   *   <li><b>assemblyDataType</b> - The FlexType to use for assembly data for all components. Requires the addition to be loaded: <code>ArtefactTstUtils.addAdditionArtefactsForTest([AssemblyAddition])</code></li>
   *   <li><b>reverseSequences</b> - If true, then will assign operation sequences by counting down.  </li>
   *   <li><b>product</b> - The top-level product to built by the order (<b>Default</b>: PC) </li>
   * </ul>
   *
   * <b>Note:</b> For integration tests, the records created by this utility should be cleaned up using this option
   * (for sub-classes of the test specs).
   * <pre>
   *   static dirtyDomains = [ActionLog, Order, Product, MasterRouting]
   * </pre>
   *
   *
   * @param options See options above. (<b>Optional</b>)
   * @return The order(s) created and released.
   */
  static Order releaseOrder(Map options = [:]) {
    options.nOrders = 1
    def list = releaseOrders(options)
    return list[0]
  }


  /**
   * Allows sub-classes to adjust the Product before it is saved.
   * @param product The un-saved product.
   * @param options the options for the order release.
   */
  @Override
  protected void adjustProduct(Product product, Map options) {
    List components = options.components as List
    if (components) {
      // Make the sequences and qty increment in the desired direction.
      def sequence = 10
      def sequenceIncrement = 10
      def componentQty = 1.0
      def componentIncrement = 1.0
      if (options.reverseSequences && components?.size() > 0) {
        sequence = components.size() * 10
        sequenceIncrement = -10
        componentQty = components.size() * 1.0
        componentIncrement = -1.0
      }
      // Build the component records for the product
      def assemblyDataType = options?.assemblyDataType
      def id = options?.id ?: ''
      for (componentName in components) {
        // See if the component already has a Product record
        def comp = "$componentName$id".toString()
        def component = Product.findByProduct(comp)
        if (!component) {
          component = new Product(product: comp, title: comp.toLowerCase())
          if (assemblyDataType) {
            component.assemblyDataType = assemblyDataType
          }
          component.save()
        }
        product.components << new ProductComponent(component: component, sequence: sequence, qty: componentQty)
        sequence += sequenceIncrement
        componentQty += componentIncrement
      }
    }

  }

  /**
   * Assembles a single component onto the order/LSN.  This is designed to be used only in tests.
   * <p>
   * <b>This method creates a <b>transaction</b> (if needed).</b>.
   *
   * <h3>Options</h3>
   * The options can be:
   * <ul>
   *   <li><b>sequence</b> - The component sequence.  If not given, then non-BOM is assumed (<b>Optional</b>) </li>
   *   <li><b>bomSequence</b> - The BOM record component sequence to assemble.  If not given, then the sequence is used (<b>Optional</b>) </li>
   *   <li><b>qty</b> - The number of pieces to assemble
   *                    If no qty is specified, then the BOM component QTY is multiplied by the LSN.qty or the Order.qtyToBuild
   *                    (<b>Optional</b>) </li>
   *   <li><b>lsn</b> - The LSN to assemble the component for (<b>Optional</b>).</li>
   *   <li><b>component</b> - The component to assemble.  Required for non-BOM components. </li>
   *   <li><b>assemblyDataType</b> - The assembly data flex type to use (<b>Optional</b>).</li>
   *   <li><b>assemblyDataValues</b> - The assembly data values to use for the component. A Map of name/value pairs (<b>Optional</b>).</li>
   *   <li><b>removed</b> - If true, then the component will be marked as removed.</li>
   *
   * </ul>
   *
   * @param order The order to assemble a component for.
   * @param options The options.  See above.
   * @return The assembled component record.
   */
  static OrderAssembledComponent assembleComponent(Order order, Map options) {
    def orderAssembledComponent = null
    try {
      OrderAssembledComponent.withTransaction {
        setLoginUser()
        def highestSequence = order.assembledComponents*.sequence.max() ?: 0
        def assembledComponentSequence = highestSequence + 10
        def currentComponentWithMaxSequence = order.assembledComponents?.max() { it.sequence }
        def sequence = currentComponentWithMaxSequence?.sequence ?: 0
        sequence = options.sequence ?: sequence + 1
        def bomSequence = options.bomSequence ?: options.sequence ?: sequence
        //ArgumentUtils.checkMissing(sequence, 'options.sequence')
        ArgumentUtils.checkMissing(order, 'order')

        def orderComponent = order.components.find() { it.sequence == bomSequence } as OrderBOMComponent
        def buildQty = options.lsn?.qty ?: order.qtyToBuild

        orderAssembledComponent = new OrderAssembledComponent()
        orderAssembledComponent.component = (Product) (orderComponent?.component ?: options.component)
        orderAssembledComponent.userName = SecurityUtils.currentUserName
        orderAssembledComponent.qty = (BigDecimal) (options.qty ?: buildQty * (orderComponent?.qty ?: 1.0))
        orderAssembledComponent.bomSequence = orderComponent?.sequence ?: 0
        orderAssembledComponent.lsn = options.lsn as LSN
        orderAssembledComponent.assemblyData = options.assemblyDataType as FlexType
        orderAssembledComponent.sequence = (int) assembledComponentSequence
        if (options.removed) {
          orderAssembledComponent.removedByUserName = SecurityUtils.currentUserName
          orderAssembledComponent.removedDate = new Date()
          orderAssembledComponent.state = AssembledComponentStateEnum.REMOVED
        }
        def assemblyDataValues = options.assemblyDataValues
        assemblyDataValues?.each() { key, value ->
          orderAssembledComponent.setAssemblyDataValue(key, value)
        }
        //println "orderAssembledComponent = $orderAssembledComponent"
        order.assembledComponents << orderAssembledComponent
        order.save()
      }
    } finally {
      clearLoginUser()
    }
    return orderAssembledComponent

  }

  /**
   * Builds a parsed, but not resolved scan response/request for the given order with a LOT
   * @param options Contains elements for the response: order/lsn, component and optional lot, vendor.
   * @return A scan request and scan response.
   */
  static Tuple2<ScanRequest, ScanResponse> buildRequestAndResponse(Map<String, Object> options) {
    def order = options.order as Order
    def lsn = options.lsn as LSN
    if (lsn) {
      order = lsn.order
    }
    def product = options.component
    def productBarcode = ''
    if (product) {
      productBarcode = "^PRD^$product"
    }
    def lotBarcode = ''
    if (options.lot) {
      lotBarcode = "^LOT^$options.lot"
    }
    def barcode = options.barcode ?: "$productBarcode$lotBarcode"
    def scanRequest = new ScanRequest(barcode: barcode, order: order, lsn: lsn)
    def scanResponse = new ScanResponse(scanRequest)
    def parsedBarcode = [:]
    if (product) {
      parsedBarcode[ScanAssyService.BARCODE_PRODUCT] = product
    }
    if (options.lot) {
      parsedBarcode[ScanAssyService.BARCODE_LOT] = options.lot
    }
    if (options.vendor) {
      parsedBarcode[ScanAssyService.BARCODE_VENDOR] = options.vendor
    }
    scanResponse.parsedBarcode = parsedBarcode
    return new Tuple2(scanRequest, scanResponse)
  }

}
