package org.simplemes.mes.assy.system.service

import groovy.util.logging.Slf4j
import org.simplemes.eframe.custom.domain.FlexField
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.mes.assy.demand.AddOrderAssembledComponentRequest
import org.simplemes.mes.assy.demand.AssembledComponentAction
import org.simplemes.mes.assy.demand.AssembledComponentStateEnum
import org.simplemes.mes.assy.demand.AssembledComponentUndoAction
import org.simplemes.mes.assy.demand.DisplayAssembleDialogAction
import org.simplemes.mes.assy.demand.domain.OrderAssembledComponent
import org.simplemes.mes.assy.demand.domain.OrderBOMComponent
import org.simplemes.mes.assy.demand.service.OrderAssyService
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.system.ScanRequestInterface
import org.simplemes.mes.system.ScanResponseInterface
import org.simplemes.mes.system.service.GetBarcodePrefixPoint
import org.simplemes.mes.system.service.ScanPoint

import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional

/**
 * This service provides business logic used for the Assembly module that
 * provides additions to the core Scan service.
 * <p>
 *
 */
@Slf4j
@Singleton
class ScanAssyService implements ScanPoint, GetBarcodePrefixPoint {

  @Inject
  OrderAssyService orderAssyService

  /**
   * The logical element name for a Product barcode element.  This is used by internal logic for determining what to do with a scan.
   * The barcode will contain the prefix 'PRD'.
   */
  public static final String BARCODE_PRODUCT = 'PRODUCT'

  /**
   * The logical element name for a Lot barcode element.  This is used by internal logic for determining what to do with a scan.
   * The barcode will contain the prefix 'LOT'.
   */
  public static final String BARCODE_LOT = 'LOT'

  /**
   * The logical element name for a Serial Number barcode element.  This is used by internal logic for determining what to do with a scan.
   * The barcode will contain the prefix 'SN'.
   */
  public static final String BARCODE_SERIAL = 'SERIAL'

  /**
   * The logical element name for a Vendor barcode element.  This is used by internal logic for determining what to do with a scan.
   * The barcode will contain the prefix 'VND'.
   */
  public static final String BARCODE_VENDOR = 'VENDOR'

  /**
   * Pre-scan extension method for the getBarcodePrefix() method.
   * <p>
   * <b>Note</b>: This module's method does nothing in the pre method, just the post method.
   */
  @Override
  void preGetBarcodePrefixMapping() {
  }

  /**
   * Post-scan extension method for the getBarcodePrefix() method.
   * @param coreResponse The response from the core method (or from other extensions that provide a response).
   * @return Return a new response if your extension needs to return a different value. Return null to use core response.
   *         You can also modify the coreResponse if it is a POGO that allows modification.
   */
  @Override
  Map<String, String> postGetBarcodePrefixMapping(Map<String, String> coreResponse) {
    if (!coreResponse.PRD) {
      coreResponse.PRD = BARCODE_PRODUCT
      coreResponse.LOT = BARCODE_LOT
      coreResponse.SN = BARCODE_SERIAL
      coreResponse.VND = BARCODE_VENDOR
      log.trace('postGetBarcodePrefixMapping: adding mappings.  Result = {}', coreResponse)
    }
    return null
  }

  /**
   * Pre-scan extension method for the scan() method.
   * <p>
   * <b>Note</b>: This module's method does nothing in the pre method, just the post method.
   * @param scanRequest
   */
  @Override
  void preScan(ScanRequestInterface scanRequest) {
  }

  /**
   * Post-scan extension method for the scan() method.
   * This extension adds ability to handle the scan request for component assembly.
   * @param coreResponse The response from the core method (or from other extensions that provide a response).
   * @param scanRequest The details of the request .
   * @return Return a new response if your extension needs to return a different value. Return null to use core response.
   *         You can also modify the coreResponse if it is a POGO that allows modification.
   */
  @Override
  @Transactional
  ScanResponseInterface postScan(ScanResponseInterface coreResponse, ScanRequestInterface scanRequest) {
    log.debug("scanPost: request = {}, passed in result = {}", scanRequest, coreResponse)
    //println "scanRequest = $scanRequest"
    if (coreResponse.resolved) {
      // Already resolved, so nothing to test.
      return null
    }

    if (!scanRequest.order) {
      return null
    }

    // Find the component to be assembled: if parsed, use it.  If not, then use the entire barcode as the product.
    def componentProduct = coreResponse?.parsedBarcode?."${BARCODE_PRODUCT}" ?: coreResponse?.barcode
    if (!componentProduct) {
      // No component info in the barcode, so do nothing.
      return null
    }

    assembleComponent(scanRequest, componentProduct, coreResponse)

    return null
  }


  /**
   * Adds a single component to the order/LSN.
   * @param scanRequest The input scan request.  Includes the parsed barcode data.
   * @param componentProduct The product for the component.
   * @param result The scan results from the core method.
   */
  @SuppressWarnings('GroovyAssignabilityCheck')
  protected void assembleComponent(ScanRequestInterface scanRequest, String componentProduct, ScanResponseInterface result) {
    def orderComponent = determineOrderComponent(scanRequest, componentProduct)
    def component
    def qty = 1.0
    if (orderComponent) {
      component = orderComponent.component
      qty = orderComponent.qty
    } else {
      // No BOM component matches, so try assembling it as a non-BOM component.
      component = Product.findByProduct(componentProduct)
      if (!component) {
        // Not a valid component, so do nothing.
        return
      }
    }
    //println "orderComponent = $orderComponent"

    // Assume the full qty was assembled.
    def addRequest = new AddOrderAssembledComponentRequest(order: scanRequest.order, lsn: scanRequest.lsn,
                                                           bomSequence: orderComponent?.sequence,
                                                           component: component, qty: qty)

    // Now, build the assembly data to collect.
    FlexType assemblyDataType = component.assemblyDataType
    if (assemblyDataType) {
      def fieldMatchCount = 0
      for (FlexField field in assemblyDataType.fields) {
        fieldMatchCount = fieldMatchCount + processBarcodeForFlexField(result, field.fieldName, addRequest)
      }
      addRequest.assemblyData = assemblyDataType

      // If there are no fields from a structured barcode, then force an assemble component dialog on the client.
      if (!fieldMatchCount) {
        result.scanActions << buildAssembleDialogAction(component, scanRequest, orderComponent)
        result.resolved = true
        return
      }

    }

    def orderAssembledComponent = orderAssyService.addComponent(addRequest) as OrderAssembledComponent
    def action = new AssembledComponentAction(order: scanRequest.order.order,
                                              lsn: scanRequest.lsn?.lsn,
                                              component: component.product,
                                              bomSequence: orderAssembledComponent.bomSequence)

    result.scanActions << action
    result.undoActions << new AssembledComponentUndoAction(orderAssembledComponent, scanRequest.order)
    log.trace('scanPost: action = {}', action)

    // Add the message to the response
    def destination = scanRequest.lsn?.lsn ?: scanRequest.order.order
    //assembledComponent.message=Assembled component {0} into {1}.
    def msg = GlobalUtils.lookup('assembledComponent.message', component.product, destination)
    result.messageHolder.addInfo(text: msg)

    // And now adjust the result to indicate we processed the scan.
    result.resolved = true
  }

  /**
   * Builds the client action that will display the assemble component dialog.
   * @param component The component being assembled.
   * @param scanRequest The original scan request.
   * @param orderComponent The BOM component from the order being assembled (<b>optional</b>).
   * @return The display dialog action.
   */
  private DisplayAssembleDialogAction buildAssembleDialogAction(Product component, ScanRequestInterface scanRequest, OrderBOMComponent orderComponent) {
    def firstField = ''
    if (component.assemblyDataType?.fields?.size()) {
      //noinspection GroovyAssignabilityCheck
      firstField = component.assemblyDataType.fields[0].fieldName
    }

    def action = new DisplayAssembleDialogAction(order: scanRequest.order.order,
                                                 lsn: scanRequest.lsn?.lsn,
                                                 component: component.product,
                                                 bomSequence: orderComponent?.sequence,
                                                 assemblyData: component.assemblyDataType?.flexType,
                                                 assemblyDataUuid: component.assemblyDataType?.uuid?.toString(),
                                                 firstAssemblyDataField: firstField)
    log.trace('scanPost: action = {}', action)
    return action
  }

  /**
   * Determines the component from the order/LSN that corresponds to the parsed barcode map.
   * @param scanRequest The request with the order/LSN to be checked for the scanned component.
   * @param componentProduct The product name to for the component.
   * @return The matching OrderBOMComponent.
   */
  OrderBOMComponent determineOrderComponent(ScanRequestInterface scanRequest, String componentProduct) {
    def order = scanRequest.order
    //println "order = $order, cp = $componentProduct"
    if (!(componentProduct && order)) {
      // Can't find it
      return null
    }
    // Assign to first BOM component that is not fully assembled.
    def orderComponents = findAllMatchingComponents(order, componentProduct)
    //println "   orderComponents = $orderComponents"
    def qtyToBuild = scanRequest.lsn?.qty ?: order.qtyToBuild
    //println "qtyToBuild = $qtyToBuild"
    for (orderComponent in orderComponents) {
      // First, count up how much of the required qty is assembled already.
      // Limit it to just the given LSN, if an LSN is given.
      def assembledComponents = order.assembledComponents.findAll() {
        it.bomSequence == orderComponent.sequence &&
          it.state == AssembledComponentStateEnum.ASSEMBLED &&
          (scanRequest.lsn == null || scanRequest.lsn == it.lsn)
      }
      //println "assembledComponents = $assembledComponents"
      def assembledQty = assembledComponents.qty.sum()
      //println "assembledQty = $assembledQty, needed qty = ${orderComponent.qty*qtyToBuild}"
      if (assembledQty < (orderComponent.qty * qtyToBuild)) {
        return orderComponent
      }
    }

    // All components are assembled, so let the caller over-assemble at the first component
    if (orderComponents) {
      //println "orderComponents = $orderComponents"
      return orderComponents[0]
    }

    // Still not found
    return null
  }

  /**
   * Finds all of the matching OrderComponents for the given product, sorted in ascending BOM
   * order.
   * @param order The order to be checked for the scanned component.
   * @param product The the product name to search for.
   * @return The matching OrderBOMComponent.
   */
  List<OrderBOMComponent> findAllMatchingComponents(Order order, String product) {
    def matches = order.components.findAll() { it.component.product == product }

    return matches.sort() { it.sequence } as List<OrderBOMComponent>
  }

  /**
   * Check the parsed barcode for the given flex field name (case insensitive) and adjust the addRequest to include
   * the value if the field is in the barcode.  This helps detect when a barcode like ^PRD^WHEEL-27^LOT^87929459
   * is used with a FlexType field = 'lot' or 'LOT' and will use the value ('87929459') in the add component
   * request.
   *
   * @param scanResponse The response being built for this scan.  Includes the barcode fields.
   * @param fieldName The flex type field name.
   * @param addRequest The add component request to add the field value to, if it matches.
   * @return 1 if the field was found.  Otherwise, 0.
   */
  int processBarcodeForFlexField(ScanResponseInterface scanResponse, String fieldName,
                                 AddOrderAssembledComponentRequest addRequest) {
    def fieldNameLC = fieldName.toLowerCase()
    if (scanResponse.parsedBarcode) {
      def foundCounter = 0
      scanResponse.parsedBarcode.each { k, v ->
        if (fieldNameLC == k.toLowerCase()) {
          addRequest.setFieldValue(fieldName, scanResponse?.parsedBarcode[(String) k])
          foundCounter++
        }
      }
      return foundCounter
    }
    return 0
  }
}
