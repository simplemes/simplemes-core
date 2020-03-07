/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.pogo

import groovy.transform.ToString

/**
 * The sample response from the WorkListService.findWork() service method.
 * This contains header data and a list of detail rows that represent the unit of work.
 * <p/>
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