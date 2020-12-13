package org.simplemes.mes.demand

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.simplemes.eframe.web.ui.UIDefaults
import org.simplemes.mes.floor.domain.WorkCenter


/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * The request argument for the WorkListService.findWork() service method.  This provides the arguments
 * for the findWork() method along with key options.
 * <p/>
 * This is a POGO used by the {@link org.simplemes.mes.demand.service.WorkListService#findWork(org.simplemes.mes.demand.FindWorkRequest)} method.
 */
@ToString(includeNames = true, includePackage = false)
@EqualsAndHashCode
class FindWorkRequest {

  /**
   * The work center to limit the work for.
   */
  WorkCenter workCenter

  /**
   * A filter value for suggest filtering on the client.  Looks for Orders/LSN's that start with the given value.
   */
  String filter

  /**
   * If true, then the findWork() method should return active work.
   */
  boolean findInWork = true

  /**
   * If true, then the findWork() method should return work that is in queue.
   */
  boolean findInQueue = true

  /**
   * The maximum number of rows to retrieve.  (<b>Default:</b> Limits.DEFAULT_PAGE_SIZE, upper limit of 100 or as
   * configured by ConfigSettingUtils.maxRowLimit).
   */
  int max = UIDefaults.PAGE_SIZE

  /**
   * The starting page to retrieve from.  (<b>Default:</b> 0).  Page size is max above.
   */
  int from = 0

  // TODO: Support sorting in findWork and controller.  Currently sorts by dateFirstQueued.  Maybe add queue Time to display.

}
