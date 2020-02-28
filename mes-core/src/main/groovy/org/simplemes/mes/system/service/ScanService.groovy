package org.simplemes.mes.system.service

import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.mes.demand.ResolveIDRequest
import org.simplemes.mes.demand.StartRequest
import org.simplemes.mes.demand.WorkableInterface
import org.simplemes.mes.demand.service.OrderService
import org.simplemes.mes.demand.service.ResolveService
import org.simplemes.mes.demand.service.WorkService
import org.simplemes.mes.system.ButtonPressAction
import org.simplemes.mes.system.OrderLSNChangeAction
import org.simplemes.mes.system.RefreshOrderStatusAction
import org.simplemes.mes.system.ScanRequestInterface
import org.simplemes.mes.system.ScanResponse
import org.simplemes.mes.system.ScanResponseInterface

import javax.inject.Inject
import javax.transaction.Transactional

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * This service provided methods to handle user barcode scans in a shop floor dashboard.
 * It determines what was scanned and processes the scan to perform the desired action on the object scanned.
 * <p/>
 * See the User Guide for details on this service.
 *
 *
 */
class ScanService {

  /**
   * The resolve service used to find Orders/LSNs
   */
  @Inject
  ResolveService resolveService

  /**
   * The order service used to find Orders/LSNs states.
   */
  @Inject
  OrderService orderService

  /**
   * The work service used to start/complete orders/LSNs.
   */
  @Inject
  WorkService workService

  /**
   * The logical element name for a GUI button.  This is used by internal logic for determining what to do with a scan.
   * The barcode will contain the prefix 'BTN'.
   */
  public static final String BARCODE_BUTTON = 'BUTTON'

  /**
   * The mapping between barcode prefixes and the logical meaning of those prefixes.  Maps 'BTN' to 'BUTTON' for clarify.
   */
  private final Map<String, String> defaultBarcodePrefixMapping = ['BTN': BARCODE_BUTTON]

  /**
   * Handles the scan from the user.  This resolves the ID and will sometimes process the ID.
   * <p/>
   * <b>Note:</b> This method uses the normal Java interface pattern for arguments and return values.  This is done because
   *              the optional modules will most likely replace the core objects with sub-classes.
   * @param scanRequest The scan request to resolve.
   * @return The response of the scan processing.  Includes the resolve results, client actions and any messages from the actions
   *         performed by this service.
   */
  @Transactional
  ScanResponseInterface scan(ScanRequestInterface scanRequest) {
    ArgumentUtils.checkMissing(scanRequest, 'scanRequest')
    ArgumentUtils.checkMissing(scanRequest.barcode, 'scanRequest.barcode')
    def response = new ScanResponse(scanRequest)

    def parsedScan = parseScan(scanRequest)
    response.parsedBarcode = parsedScan
    if (parsedScan[BARCODE_BUTTON]) {
      response.scanActions << new ButtonPressAction(button: parsedScan[BARCODE_BUTTON])
      response.resolved = true
      return response
    }

    // Check for Order/LSN matches
    def resolveIDResponse = resolveService.resolveID(new ResolveIDRequest(barcode: scanRequest.barcode))
    if (resolveIDResponse.resolved) {
      if (resolveIDResponse.lsn || resolveIDResponse.order) {
        response.resolved = true
        response.order = resolveIDResponse.order
        response.lsn = resolveIDResponse.lsn

        // Now, try to start the order/lsn if it is in queue
        if (response.lsn) {
          // TODO: Support LSN
        } else if (response.order) {
          processOrder(response)
        }

      }
    }


    return response
  }

  /**
   * Attempts to parse the scanned data, looking for specific markers for specific field types (e.g. button,
   * order, lsn, product, etc).
   * @param scanRequest The original barcode scan request.
   * @return A map containing the parsed name/value pairs found in the barcode (if any).
   */
  @Transactional
  Map parseScan(ScanRequestInterface scanRequest) {
    def res = [:]
    String barcode = scanRequest.barcode
    // Handle internal format first
    if (barcode && barcode[0] == '^') {
      def values = barcode.tokenize('^')
      // Process each pair
      for (int i = 0; (i + 1) < values.size(); i += 2) {
        def prefix = values[i]
        // Fallback to prefix from the barcode if not in the mapping list. The caller will deal with it.
        def map = getBarcodePrefixMapping()
        def key = map[prefix] ?: prefix

        res[key] = values[i + 1]
      }
    }

    return res
  }

  /**
   * Attempts to execute the automatic actions for an order when scanned.
   * This includes starting the order if in queue.
   * @param scanResponse The response with the order.  Updated by this method when an action is performed.
   */
  @Transactional
  void processOrder(ScanResponseInterface scanResponse) {
    // Check the order for in queue status
    def order = scanResponse.order
    List<WorkableInterface> workables = orderService.determineQtyStates(order)
    if (!workables) {
      // Not in queue/work, so no action possible
      return
    }

    // Use the first workable for now.  Later, support filter by Work Center.
    WorkableInterface workable = workables[0]
    BigDecimal qtyInQueue = workable.qtyInQueue
    BigDecimal qtyInWork = workable.qtyInWork
    if (workable.qtyInQueue) {
      // Attempt a start
      def startRequest = new StartRequest(barcode: scanResponse.barcode, order: scanResponse.order,
                                          operationSequence: scanResponse.operationSequence)
      def startResponse = workService.start(startRequest)

      // Add the message to the response
      def msg = GlobalUtils.lookup('started.message', startResponse.order.order, startResponse.qty)
      scanResponse.messageHolder.addInfo(text: msg)

      // Let the client know it should refresh the order status.
      scanResponse.scanActions << new RefreshOrderStatusAction(order: order.order)
      qtyInQueue -= startResponse.qty
      qtyInWork += startResponse.qty

      // Copy any undo actions from the start
      for (undoAction in startResponse) {
        scanResponse.undoActions.addAll(undoAction.undoActions)
      }
      //} else if (workable.qtyInWork) {
    }

    // Let the client know the order might have changed.
    scanResponse.scanActions << new OrderLSNChangeAction(order: order.order,
                                                         qtyInQueue: qtyInQueue, qtyInWork: qtyInWork)

  }

  /**
   * Returns the barcode prefix mapping list.  This maps a literal barcode prefix value (e.g. 'BTN') to
   * the standard name (e.g. 'BUTTON').  The standard name is used in most parsing logic decisions (e.g. which action
   * to perform).
   * <p>
   * This method is designed to be extended with modules.  You may modify the defaultBarcodePrefixMapping
   * to add module-specific mappings.
   *
   * @return The map.  The key of the map is the literal barcode prefix and the value is the standard name.
   */
  Map<String, String> getBarcodePrefixMapping() {
    return defaultBarcodePrefixMapping
  }

}
