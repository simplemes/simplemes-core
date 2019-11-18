package org.simplemes.eframe.security

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines some useful constants for role assignments.  Includes the String constants for Roles.
 */
class Roles {

  /**
   * The admin role, provide access to general administration actions.
   */
  static final String ADMIN = 'ADMIN'

  /**
   * The admin role, provide access to general administration actions.
   */
  static final ADMIN_AND_ALL = ['ADMIN', 'CUSTOMIZER', 'DESIGNER', 'MANAGER']

  /**
   * CUSTOMIZER -Views/Updates non-GUI customization features.
   *
   */
  static final String CUSTOMIZER = 'CUSTOMIZER'

  /**
   * DESIGNER - Views/Updates framework display features (e.g. List columns, Custom Definition features, etc).
   */
  static final String DESIGNER = 'DESIGNER'

  /**
   * MANAGER - Views/Updates most framework manager features (User Definition).
   */
  static final String MANAGER = 'MANAGER'
}
