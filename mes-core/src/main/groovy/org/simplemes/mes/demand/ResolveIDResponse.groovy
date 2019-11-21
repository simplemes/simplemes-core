package org.simplemes.mes.demand

import groovy.transform.ToString
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * The response from the resolveID method.  This method searches a prioritized list of objects for a matching
 * ID to determine what object it relates to.  This typically resolves to one object.
 * <p/>
 * This is a POGO returned by the {@link org.simplemes.mes.demand.service.ResolveService#resolveID} method.
 */
@ToString(includeNames = true, includePackage = false)
class ResolveIDResponse {

  /**
   * If true, then a matching object was found.
   */
  boolean resolved = true

  /**
   * The ID input string.
   */
  String barcode

  /**
   * The order if the ID matches it.
   */
  Order order

  /**
   * The LSN, if the ID matches it.
   */
  LSN lsn

}
