package org.simplemes.mes.system

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * The suggested client dashboard button press action.
 * This action is recommended based on the barcode scanned.
 */
@ToString(includeNames = true, includePackage = false)
@EqualsAndHashCode(callSuper = true)
class ButtonPressAction extends ScanAction {
  /**
   * The action.type for this action.
   */
  static final String TYPE_BUTTON_PRESS = 'BUTTON_PRESS'

  /**
   * The button ID of the dashboard button to press.
   */
  String button

  /**
   * The recommended client action.
   */
  @Override
  String getType() {
    return TYPE_BUTTON_PRESS
  }
}
