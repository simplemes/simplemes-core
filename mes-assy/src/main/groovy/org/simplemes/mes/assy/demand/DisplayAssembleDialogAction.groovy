package org.simplemes.mes.assy.demand

import groovy.transform.ToString
import org.simplemes.mes.system.ScanAction

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The client suggested action triggered when a component is scanned, but is missing assembly data.
 */
@ToString(includeNames = true, includePackage = false)
class DisplayAssembleDialogAction extends ScanAction {
  /**
   * The action.type for this action.
   */
  static final String TYPE_DISPLAY_ASSEMBLE_DIALOG = 'DISPLAY_ASSEMBLE_DIALOG'

  /**
   * The order the component is to be assembled into.
   */
  String order

  /**
   * The LSN the component is to be assembled into.
   */
  String lsn

  /**
   * The component (product) that is to be assembled.
   */
  String component

  /**
   * The bomSequence for the component that is to be assembled.
   */
  Integer bomSequence

  /**
   * The flex type to display the data entry fields for.
   */
  String assemblyData

  /**
   * The UUID of the flex type to display the data entry fields for.
   */
  String assemblyDataUuid

  /**
   * The name of the first assembly data field.  Used for the display focus logic.
   */
  String firstAssemblyDataField

  /**
   * The recommended client action.
   */
  @Override
  String getType() {
    return TYPE_DISPLAY_ASSEMBLE_DIALOG
  }


}
