package org.simplemes.mes.demand.service

import groovy.util.logging.Slf4j
import io.micronaut.data.model.Pageable
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.domain.SQLUtils
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
    findWorkDetails(Order, findWorkRequest).each { Map map ->
      details << new FindWorkResponseDetail(map)
    }
    totalAvailable += findWorkTotalCount(Order, findWorkRequest)

    //  B Orders with a routing.
    long timeB = System.currentTimeMillis()
    findWorkDetails(OrderOperState, findWorkRequest).each { Map map ->
      details << new FindWorkResponseDetail(map)
    }
    totalAvailable += findWorkTotalCount(OrderOperState, findWorkRequest)

    //  C LSNs with no routing.
    long timeC = System.currentTimeMillis()
    findWorkDetails(LSN, findWorkRequest).each { Map map ->
      details << new FindWorkResponseDetail(map)
    }
    totalAvailable += findWorkTotalCount(LSN, findWorkRequest)

    //  D LSNs with a routing.
    long timeD = System.currentTimeMillis()
    findWorkDetails(LSNOperState, findWorkRequest).each { Map map ->
      details << new FindWorkResponseDetail(map)
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
  protected List findWorkDetails(Class domainClass, FindWorkRequest findWorkRequest) {
    return findWorkInternal(domainClass, findWorkRequest, false)
  }

  /**
   * Internal method to find the total number of work records available for a single object.
   * @param domainClass The class to search for work.
   * @param findWorkRequest Defines the request restrictions for the search.
   * @return The count.
   */
  protected int findWorkTotalCount(Class domainClass, FindWorkRequest findWorkRequest) {
    def list = findWorkInternal(domainClass, findWorkRequest, true)
    return list[0].count as int
  }

  /**
   * Internal method to find the work records or a count.
   * @param domainClass The class to search for work.
   * @param findWorkRequest Defines the request restrictions for the search.
   * @param needCount If true, then returns a list for the count (element 0).
   * @return The raw SQL list (of Maps).
   */
  protected List findWorkInternal(Class domainClass, FindWorkRequest findWorkRequest, boolean needCount = false) {
    def list = []
    if (domainClass == Order) {
      list = findWorkWithSQLForOrder(findWorkRequest, needCount)
    } else if (domainClass == OrderOperState) {
      list = findWorkWithSQLForOrderRouting(findWorkRequest, needCount)
    } else if (domainClass == LSN) {
      list = findWorkWithSQLForLSN(findWorkRequest, needCount)
    } else if (domainClass == LSNOperState) {
      list = findWorkWithSQLForLSNRouting(findWorkRequest, needCount)
    }
    return list
  }


  /**
   * Internal method to find the work for Orders without routing.  Builds a dynamic SQL query for the right filter
   * and tables.
   * @param findWorkRequest Defines the request restrictions for the search.
   * @param needCount Set to true for count(*) variant (<b>Default</b>: false).
   * @return The list of matching records.
   */
  protected List findWorkWithSQLForOrder(FindWorkRequest findWorkRequest, boolean needCount = false) {
    log.trace('findWorkWithSQLForOrder():  request={}', findWorkRequest)

    def columnList
    def orderBy = ''
    if (needCount) {
      columnList = "COUNT (*)"
    } else {
      columnList = "m.uuid,m.uuid as order_id,m.ordr,${buildStandardColumns()}"
      orderBy = 'ORDER BY m.date_first_queued ASC'
    }
    def whereClause = buildWhereClause(findWorkRequest, findWorkRequest.filter ? 'm.ordr' : '')

    def sql = "SELECT $columnList FROM ordr m $whereClause $orderBy"

    return SQLUtils.instance.executeQuery(sql, Map, buildQueryParameters(findWorkRequest, needCount))
  }

  /**
   * Internal method to find the work for Orders with routing.  Builds a dynamic SQL query for the right filter
   * and tables.
   * @param findWorkRequest Defines the request restrictions for the search.
   * @param needCount Set to true for count(*) variant (<b>Default</b>: false).
   * @return The list of matching records.
   */
  protected List findWorkWithSQLForOrderRouting(FindWorkRequest findWorkRequest, boolean needCount = false) {
    log.trace('findWorkWithSQLForOrderRouting():  request={}', findWorkRequest)

    def columnList
    def orderBy = ''
    if (needCount) {
      columnList = "COUNT (*)"
    } else {
      columnList = "m.uuid,m.order_id,o.ordr,m.sequence,${buildStandardColumns()}"
      orderBy = 'ORDER BY m.date_first_queued ASC'
    }
    def whereClause = buildWhereClause(findWorkRequest, findWorkRequest.filter ? 'o.ordr' : '')

    def sql = "SELECT $columnList FROM order_oper_state m INNER JOIN ordr o ON m.order_id=o.uuid $whereClause $orderBy"
    return SQLUtils.instance.executeQuery(sql, Map, buildQueryParameters(findWorkRequest, needCount))
  }

  /**
   * Internal method to find the work for LSNs without routing.  Builds a dynamic SQL query for the right filter
   * and tables.
   * @param findWorkRequest Defines the request restrictions for the search.
   * @param needCount Set to true for count(*) variant (<b>Default</b>: false).
   * @return The list of matching records.
   */
  protected List findWorkWithSQLForLSN(FindWorkRequest findWorkRequest, boolean needCount = false) {
    log.trace('findWorkWithSQLForLSN():  request={}', findWorkRequest)

    def columnList
    def orderBy = ''
    if (needCount) {
      columnList = "COUNT (*)"
    } else {
      columnList = "m.uuid,m.order_id as order_id,m.uuid as lsn_id,m.lsn,o.ordr,${buildStandardColumns()}"
      orderBy = 'ORDER BY m.date_first_queued ASC'
    }
    def whereClause = buildWhereClause(findWorkRequest, findWorkRequest.filter ? 'm.lsn' : '')

    def sql = "SELECT $columnList FROM lsn m INNER JOIN ordr o ON m.order_id=o.uuid $whereClause $orderBy"
    return SQLUtils.instance.executeQuery(sql, Map, buildQueryParameters(findWorkRequest, needCount))
  }

  /**
   * Internal method to find the work for LSNs without routing.  Builds a dynamic SQL query for the right filter
   * and tables.
   * @param findWorkRequest Defines the request restrictions for the search.
   * @param needCount Set to true for count(*) variant (<b>Default</b>: false).
   * @return The list of matching records.
   */
  protected List findWorkWithSQLForLSNRouting(FindWorkRequest findWorkRequest, boolean needCount = false) {
    log.trace('findWorkWithSQLForLSNRouting():  request={}', findWorkRequest)

    def columnList
    def orderBy = ''
    if (needCount) {
      columnList = "COUNT (*)"
    } else {
      columnList = "m.uuid,l.order_id as order_id,l.uuid as lsn_id,o.ordr,l.lsn,m.sequence,${buildStandardColumns()}"
      orderBy = 'ORDER BY m.date_first_queued ASC'
    }
    def whereClause = buildWhereClause(findWorkRequest, findWorkRequest.filter ? 'l.lsn' : '')

    def sql = "SELECT $columnList FROM lsn_oper_state m INNER JOIN lsn l ON m.lsn_id=l.uuid INNER JOIN ordr o ON l.order_id=o.uuid $whereClause $orderBy"

    return SQLUtils.instance.executeQuery(sql, Map, buildQueryParameters(findWorkRequest, needCount))
  }

  /**
   * The comma-delimited list of columns for all 4 queries.
   */
  static final String standardWorkStateColumns = 'm.qty_in_queue,m.qty_in_work,m.qty_done,m.date_qty_queued,m.date_qty_started,m.date_first_queued,m.date_first_started'
  /**
   * Builds the standard columns for work queries.
   * @return The columns list.
   */
  String buildStandardColumns() {
    return standardWorkStateColumns
  }

  /**
   * Builds the where clause based on the given filter criteria.
   * @param findWorkRequest The request with filter criteria.
   * @param likeColumnName The name of the like column (if needed for the filter option).
   * @return The WHERE clause.
   */
  String buildWhereClause(FindWorkRequest findWorkRequest, String likeColumnName = null) {
    def qtyCheck = findWorkRequest.findInQueue || findWorkRequest.findInWork
    if (qtyCheck || findWorkRequest.filter) {
      def sb = new StringBuilder()
      sb << (qtyCheck ? "(" : '')
      if (findWorkRequest.findInQueue) {
        sb << " m.qty_in_queue>0.0"
      }
      if (findWorkRequest.findInWork) {
        if (sb && findWorkRequest.findInQueue) {
          sb << " OR "
        }
        sb << "m.qty_in_work>0.0"
      }
      sb << (qtyCheck ? ")" : '')
      if (findWorkRequest.filter) {
        if (sb) {
          sb << " AND "
        }
        sb << "$likeColumnName ilike ?"
      }
      return "WHERE $sb"
    }

    return ''
  }

  /**
   * Builds a list of parameters for the find work request.  Adds filter and row count limits (if needed).
   * @param findWorkRequest The request.
   * @param needCount Set to true for count(*) variant (<b>Default</b>: false).
   * @return The parameters for the SQL.
   */
  Object[] buildQueryParameters(FindWorkRequest findWorkRequest, Boolean needCount) {
    List args = []
    if (!needCount) {
      args << Pageable.from(findWorkRequest.from, findWorkRequest.max) as Object
    }
    if (findWorkRequest.filter) {
      args << "${findWorkRequest.filter}%"
    }
    return args as Object[]
  }
}


