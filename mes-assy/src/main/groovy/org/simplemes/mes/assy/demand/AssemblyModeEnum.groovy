package org.simplemes.mes.assy.demand

import org.simplemes.eframe.i18n.GlobalUtils

/*
 * Copyright Michael Houston 2016. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *  Defines the possible choices for a given operation's assembly mode.
 */
@SuppressWarnings('SerializableClassMustDefineSerialVersionUID')
enum AssemblyModeEnum {
  /**
   * No automatic assembly processing at this operation.
   */
  NONE(null),

  /**
   * Auto-assemble from work center setup on start.
   */
  START('S'),

  /**
   * Auto-assemble from work center setup on complete.
   */
  COMPLETE('C'),

  /**
   * Auto-assemble from work center setup on start and complete.
   */
  START_COMPLETE('B')

  /**
   * The standardized field type associated with this field format.
   */
  final String id

  /**
   * Convenience constructor for an interval.
   * @param id The stored value in the DB.
   */
  AssemblyModeEnum(String id) {
    this.id = id
  }

  /**
   * The short, internal (DB) ID used for this interval.
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
    return GlobalUtils.lookup("assemblyMode.${name()}.label", locale)
  }

}