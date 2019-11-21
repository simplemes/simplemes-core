package org.simplemes.mes.demand

import groovy.transform.ToString

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * The request argument for resolveID method.  This searches a prioritized list of objects for a matching
 * ID to determine what object it relates to.  This request contains optional context information (e.g. Order/LSN) that
 * might help resolve the ID in some cases.
 * <p/>
 * This is a POGO used by the {@link org.simplemes.mes.demand.service.ResolveService#resolveID} method.
 */
@ToString(includeNames = true, includePackage = false)
class ResolveIDRequest {

  /**
   * A generic user or barcode input string.  This is the ID that will be resolved to a specific object.
   */
  String barcode

}
