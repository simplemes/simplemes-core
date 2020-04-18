package org.simplemes.mes.assy.demand

import org.simplemes.eframe.i18n.GlobalUtils

/*
 * Copyright Michael Houston 2016. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *  Defines the overall state of a given BOM component in relation to the order/LSN.  This is used to quickly tell if the
 *  BOM component is fully assembled, partially assembled or empty.
 *  <p>
 *  This is not persisted, but used in query APIs.
 */
//@SuppressWarnings('SerializableClassMustDefineSerialVersionUID')
enum OrderComponentStateEnum {
  /**
   * The component is not assembled.
   */
  EMPTY('EMPTY'),

  /**
   * The component is partially assembled.
   */
  PARTIAL('PARTIAL'),

  /**
   * The component is over-assembled.
   */
  OVER('OVER'),

  /**
   * The component is fully assembled.
   */
  FULL('FULL')

  /**
   * The standardized field type associated with this field format.
   */
  final String id

  /**
   * Convenience constructor.
   * @param id The stored value in the DB.
   */
  OrderComponentStateEnum(String id) {
    this.id = id
  }

  /**
   * The short, internal (DB) ID used for this status.
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
    return GlobalUtils.lookup("orderComponentState.${name()}.label", locale)
  }

}