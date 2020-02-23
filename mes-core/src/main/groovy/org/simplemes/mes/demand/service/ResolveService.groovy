package org.simplemes.mes.demand.service


import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.mes.demand.ResolveIDRequest
import org.simplemes.mes.demand.ResolveIDResponse
import org.simplemes.mes.demand.ResolveQuantityPreference
import org.simplemes.mes.demand.ResolveWorkableRequest
import org.simplemes.mes.demand.WorkableInterface
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.demand.domain.OrderOperState
import org.simplemes.mes.floor.domain.WorkCenter

import javax.inject.Singleton

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * This service provided methods to resolve identifiers into workable objects in SimpleMES.
 * This includes methods to find the appropriate workable unit and to find an order/LSN from
 * a single ID.
 * <p/>
 * See the User Guide for details on this service.
 *
 * Original Author: mph
 *
 */
@Singleton
class ResolveService {
  /**
   * Fills in the details need for most production requests.  This attempts to use the given input and fill in the
   * details such as operationSequence from the current records available.
   * @param request The request to resolve.
   * @param qtyPreference The resolve preference the caller needs.  (<b>Default:</b> ResolvePreference.QUEUE_OR_WORK)
   */
  void resolveProductionRequest(Object request, ResolveQuantityPreference qtyPreference = ResolveQuantityPreference.QUEUE_OR_WORK) {
    if (request == null) {
      throw new IllegalArgumentException('Missing request')
    }
    // Make sure the input request has the expected fields.
    ArgumentUtils.checkForProperties(request, ['lsn', 'order', 'barcode', 'qty', 'workCenter', 'operationSequence'])

    // Copy the request properties to local variables for easier use and IDE code completion.
    // These will be copied back to the request upon completion.
    String barcode = request.barcode
    Order order = request.order
    LSN lsn = request.lsn
    BigDecimal qty = request.qty
    WorkCenter workCenter = request.workCenter
    int operationSequence = request.operationSequence

    if (order == null && lsn == null) {
      // Need to find order and LSN
      if (barcode) {
        def lsns = LSN.findAllByLsn(barcode)
        if (lsns) {
          // Matches at least one LSN
          if (lsns.size() == 1) {
            // Just one found, so use it.
            lsn = lsns[0]
          } else {
            // error.3011.message=More than one LSN matches the input {0}.
            throw new BusinessException(3011, [barcode])
          }
        } else {
          // Try finding orders that match the barcode.
          def orders = Order.findAllByOrder(barcode)
          if (orders) {
            // Matches at least one Order
            if (orders.size() == 1) {
              // Just one found, so use it.
              order = orders[0]
              // No else needed since the Order has a unique constraint on it.
            }
          } else {
            // No matches found.
            // error.3012.message=No Orders or LSNs found for the input {0}.
            throw new BusinessException(3012, [barcode])
          }
        }
      }
    }

    if (lsn && !order) {
      // If we have an LSN but no order, use the LSN's order.
      order = lsn.order
    }

    // Now, see if we can determine the operationSequence
    if (!operationSequence && order?.operationStates) {
      // Has routing, so find the first state it is in queue with.
      if (lsn) {
        // Check the LSN for routing states
      } else {
        // Just an order, so see which operation is the 'current'
        for (op in order.operationStates) {
          // Let the preference enum figure out if this is an Ok entry.
          if (qtyPreference.areQuantitiesPreferred(op.qtyInQueue, op.qtyInWork)) {
            operationSequence = op.sequence
            break   // Stop after first found that is acceptable to the caller.
          }
        }
      }
    }

    // Now, copy the discovered data back to the input request (except barcode).
    request.order = order
    request.lsn = lsn
    request.qty = qty
    request.workCenter = workCenter
    request.operationSequence = operationSequence
  }

  /**
   * Determines the right level to process a given order and/or LSN.  This is based on the Product settings and the
   * current state of this order.  This finds where the in work, in queue and other quantities are handled for these cases.
   * <p/>
   * @param request The request to resolve.
   * @return A list of elements to that can be worked.   This is a list of orders or LSNs.
   */
  @SuppressWarnings("GrMethodMayBeStatic")
  //@Transactional
  WorkableInterface[] resolveWorkable(ResolveWorkableRequest request) {
    if (request == null) {
      //error.100.message=Missing parameter {0}.
      throw new BusinessException(100, ['request'])
    }
    if (request.order == null && request.lsn == null) {
      //error.100.message=Missing parameter {0}.
      throw new BusinessException(100, ['order/lsn'])
    }
    Order order = request.order ?: request.lsn.order

    // Fill in the Order if SFC is given.
    if (!request.order && request.lsn) {
      request.order = request.lsn.order
    }

    // Determine if we should resolve to the LSN level or just the order level.
    boolean useLSN = false
    //noinspection GroovyIfStatementWithIdenticalBranches
    if (request.order && !request.lsn) {
      // Just Order given, so figure out the right workable.
      if (!order.lsnTrackingOption.isOrderAllowed()) {
        // Find the first LSN(s) that have a qty in queue
        useLSN = true
      }
    } else if (!request.order && request.lsn) {
      // Just LSN given, so figure out the right stuff.
      def lsnTrackingOption = order?.lsnTrackingOption
      if (lsnTrackingOption.isLSNAllowed()) {
        useLSN = true
      }
    } else {
      // Both given, so try to resolve what the client wanted.
      if (order.lsnTrackingOption.isLSNAllowed()) {
        // Use lowest level option (LSN) when it is wide open
        useLSN = true
      }
    }

    // Now, see if we should check for operations instead of top-level workables.
    if (order?.operations) {
      if (useLSN) {
        // TODO: Find the LSN operation
      } else {
        // Find the right order operation.
        if (request.operationSequence) {
          // A specific operation was given, so use the order state that matches (if any)
          return [order.operationStates.find() { it.sequence == request.operationSequence }]
        } else {
          // No specific operation was given, so use first operation with a qtyInQueue or qtyInWork
          for (op in order.operationStates) {
            if (op.qtyInQueue > 0 || op.qtyInWork > 0) {
              return [op]
            }
          }
        }
      }
    } else {
      // No routing, so just resolve to top-level objects (LSN or Order)
      if (useLSN) {
        if (request.lsn) {
          // We have a specific LSN, so just use it.
          return [request.lsn]
        } else {
          // Find the first LSN(s) that have a qty in queue
          return order.resolveSpecificLSNs(1.0)
        }
      } else {
        return [order]
      }
    }
    return []

/*
    //noinspection GroovyIfStatementWithIdenticalBranches
    if (request.order && !request.lsn) {
      // Just Order given, so figure out the right workable.
      if (order.lsnTrackingOption.isOrderAllowed()) {
        return [order]
      } else {
        // Find the first LSN(s) that have a qty in queue
        return order.resolveSpecificLSNs(1.0)
      }
    } else if (!request.order && request.lsn) {
      // Just LSN given, so figure out the right stuff.
      if (order.lsnTrackingOption.isLSNAllowed()) {
        return [request.lsn]
      } else {
        return [order]
      }
    } else {
      // Both given, so try to resolve what the client wanted.
      if (order.lsnTrackingOption.isLSNAllowed()) {
        // Use lowest level option (LSN) when it is wide open
        return [request.lsn]
      } else {
        return [order]
      }
    }
*/
  }

  /**
   * Utility method to determine the order for a given workable.
   * @param w The workable.
   * @return The order (non-null for valid workables).
   */
  @SuppressWarnings("GrMethodMayBeStatic")
  Order determineOrderForWorkable(WorkableInterface w) {
    if (w instanceof LSN) {
      return w.order
    } else if (w instanceof OrderOperState) {
      return w.order
    } else {
      // Must be an order
      return w as Order
    }
  }

  /**
   * Utility method to determine the LSN for a given workable.
   * @param w The workable.
   * @return The LSN (null if not an LSN).
   */
  @SuppressWarnings("GrMethodMayBeStatic")
  LSN determineLSNForWorkable(WorkableInterface w) {
    if (w instanceof LSN) {
      return w
    } else {
      return null
    }
  }

  /**
   * Given a barcode-style input, this will resolve the given ID into a unique MES object.  It searches
   * for objects in a prioritized order (e.g. LSN, then Order, etc...) until it finds a match.
   * The context values are used to narrow down the results in some cases.
   *
   * @param request The request containing the ID and context to resolve.
   * @return A response object with the first object that matches.
   */
  ResolveIDResponse resolveID(ResolveIDRequest request) {
    ArgumentUtils.checkMissing(request, 'request')
    ArgumentUtils.checkMissing(request.barcode, 'request.barcode')
    def response = new ResolveIDResponse(barcode: request.barcode)

    def lsn = LSN.findByLsn(request.barcode)
    if (lsn) {
      response.lsn = lsn
      return response
    }

    def order = Order.findByOrder(request.barcode)
    if (order) {
      response.order = order
      return response
    }

    // Nothing found
    response.resolved = false
    return response
  }


}
