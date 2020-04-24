package org.simplemes.mes.assy.demand

import groovy.transform.ToString
import org.simplemes.mes.system.ScanAction

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The action/event triggered when a component has been assembled by a scan.
 * The client side action is to publish the ORDER_COMPONENT_STATUS_CHANGED event on the client dashboard.
 */
@ToString(includeNames = true, includePackage = false)
class AssembledComponentAction extends ScanAction {
  /**
   * The action.type for this action.
   */
  static final String TYPE_ORDER_COMPONENT_STATUS_CHANGED = 'ORDER_COMPONENT_STATUS_CHANGED'

  /**
   * The order the component was assembled into.
   */
  String order

  /**
   * The LSN the component was assembled into.
   */
  String lsn

  /**
   * The component (product) that was assembled.
   */
  String component

  /**
   * The bomSequence for the component that was assembled.
   */
  Integer bomSequence

  /**
   * The recommended client action.
   */
  @Override
  String getType() {
    return TYPE_ORDER_COMPONENT_STATUS_CHANGED
  }


}
