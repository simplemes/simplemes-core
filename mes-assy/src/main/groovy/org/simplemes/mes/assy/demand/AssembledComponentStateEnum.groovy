package org.simplemes.mes.assy.demand

import org.simplemes.eframe.i18n.GlobalUtils

/*
 * Copyright Michael Houston 2016. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *  Defines the possible choices for a given order component.  Used to identify if a component
 *  has been removed from the order/LSN.
 */
@SuppressWarnings('SerializableClassMustDefineSerialVersionUID')
enum AssembledComponentStateEnum {
  /**
   * The component is current assembled on the order/LSN.
   */
  ASSEMBLED('A'),

  /**
   * The component has been removed from the order/LSN.
   */
  REMOVED('R')

  /**
   * The standardized field type associated with this field format.
   */
  final String id

  /**
   * Convenience constructor.
   * @param id The stored value in the DB.
   */
  AssembledComponentStateEnum(String id) {
    this.id = id
  }

  /**
   * The short, internal (DB) ID used for this value.
   * @return The ID.
   */
  String getId() {
    return id
  }

  /**
   * Build a human-readable version of this object.
   * @param locale The locale to display the enum display text.
   * @return The human-readable string.
   */
  String toStringLocalized(Locale locale = null) {
    return GlobalUtils.lookup("assembledComponentState.${name()}.label", null, locale)
  }

}