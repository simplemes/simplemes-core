package org.simplemes.mes.demand.service

import groovy.util.logging.Slf4j
import org.simplemes.eframe.application.Holders
import org.simplemes.mes.demand.FindWorkRequest
import org.simplemes.mes.demand.FindWorkResponse
import org.simplemes.mes.demand.FindWorkResponseDetail
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.LSNOperState
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.demand.domain.OrderOperState

import javax.inject.Singleton

/**
 * Defines Services for list work that is available (in queue) or in work.  Can be restricted to work
 * in a work center or global.  This can show Orders and/or LSNs.
 * <p/>
 * This Service is part of the <b>Stable API</b>.
 *
 *  <h3>Logging</h3>
 * The logging for this class that can be enabled:
 * <ul>
 *   <li><b>debug</b> - Debugging information. Typically includes inputs and outputs. </li>
 *   <li><b>info</b> - Performance timing. </li>
 * </ul>
 */

@Slf4j
@Singleton
class WorkListService {

  /**
   * Finds the active/queued work available based on the request restrictions.
   *
   * @param findWorkRequest Defines the request restrictions for the search.  Null allowed.
   * @return The list of elements that are in work (Orders/LSNs).
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  FindWorkResponse findWork(FindWorkRequest findWorkRequest) {
    findWorkRequest = findWorkRequest ?: new FindWorkRequest()
    def details = []
    long totalAvailable = 0
    findWorkRequest.max = Math.min(findWorkRequest.max, Holders.configuration.maxRowLimit)
    log.debug('findWork() request: {}', findWorkRequest)

    // This method needs to perform up to 4 queries find all of the desired work.
    // The values will be added to the result list as-is to simplify the paging logic.
    // Also, most customers will not use all 4 approaches.
    // The 4 scenarios are:
    //  A Orders with no routing.
    //  B Orders with a routing.
    //  C LSNs with no routing.
    //  D LSNs with a routing.

    //  A Orders with no routing.
    long timeA = System.currentTimeMillis()
    findWorkDetails(Order, findWorkRequest).each { Order order ->
      details << new FindWorkResponseDetail(order)
    }
    totalAvailable += findWorkTotalCount(Order, findWorkRequest)

    //  B Orders with a routing.
    long timeB = System.currentTimeMillis()
    findWorkDetails(OrderOperState, findWorkRequest).each { OrderOperState orderOperState ->
      details << new FindWorkResponseDetail(orderOperState)
    }
    totalAvailable += findWorkTotalCount(OrderOperState, findWorkRequest)

    //  C LSNs with no routing.
    long timeC = System.currentTimeMillis()
    findWorkDetails(LSN, findWorkRequest).each { LSN lsn ->
      details << new FindWorkResponseDetail(lsn)
    }
    totalAvailable += findWorkTotalCount(LSN, findWorkRequest)

    //  D LSNs with a routing.
    long timeD = System.currentTimeMillis()
    findWorkDetails(LSNOperState, findWorkRequest).each { LSNOperState lsnOperState ->
      details << new FindWorkResponseDetail(lsnOperState)
    }
    totalAvailable += findWorkTotalCount(LSNOperState, findWorkRequest)

    if (log.infoEnabled) {
      long endTime = System.currentTimeMillis()
      log.info('findWork queries: time: {} - {}/{}/{}/{} (ms) found: {} for request: {}',
               (endTime - timeA), (timeB - timeA), (timeC - timeB), (timeD - timeC), (endTime - timeD),
               details.size(), findWorkRequest)
    }

    return new FindWorkResponse(totalAvailable: totalAvailable, list: details)
  }

  /**
   * Internal method to find the work for a single object.
   * @param domainClass The class to search for work.
   * @param findWorkRequest Defines the request restrictions for the search.
   * @return The list of matching records.
   */
  private List findWorkDetails(Class domainClass, FindWorkRequest findWorkRequest) {
    def criteria = domainClass.createCriteria()
    List list = criteria.list {
      maxResults(findWorkRequest.max)
      firstResult(findWorkRequest.offset)
      or {
        if (findWorkRequest.findInQueue) {
          gt("qtyInQueue", 0.0)
        }
        if (findWorkRequest.findInWork) {

          gt("qtyInWork", 0.0)
        }
      }
      order("dateFirstQueued", "asc")
    }
    return list
  }

  /**
   * Internal method to find the total number of work records available for a single object.
   * @param domainClass The class to search for work.
   * @param findWorkRequest Defines the request restrictions for the search.
   * @return The count.
   */
  private int findWorkTotalCount(Class domainClass, FindWorkRequest findWorkRequest) {
    def countCriteria = domainClass.createCriteria()
    return countCriteria.count {
      or {
        if (findWorkRequest.findInQueue) {
          gt("qtyInQueue", 0.0)
        }
        if (findWorkRequest.findInWork) {

          gt("qtyInWork", 0.0)
        }
      }
    }
  }


}


