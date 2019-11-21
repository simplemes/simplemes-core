package org.simplemes.mes.demand

import groovy.transform.ToString

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * The response from the WorkListService.findWork() service method.
 * This contains header data and a list of detail rows that represent the unit of work.
 * <p/>
 * This is a POGO used by the {@link org.simplemes.mes.demand.service.WorkListService#findWork(org.simplemes.mes.demand.FindWorkRequest)} method.
 * <p>
 * <b>Note:</b> This POGO is suitable as the input to a &lt;ef:list&gt; tag.
 */
@ToString(includeNames = true, includePackage = false)
class FindWorkResponse {

  /**
   * The total number of rows available that matches the search criteria.
   */
  int totalAvailable

  /**
   * The list of details rows found that match the search criteria.
   * This detail represents a single unit in queue or in work.  This can be an order and/or LSN.
   */
  List<FindWorkResponseDetail> list

}
