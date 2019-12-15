package org.simplemes.eframe.dashboard.domain

//import grails.gorm.annotation.Entity

/**
 * Defines a single dashboard panel generic type.  The sub-classes implement the content or a splitter to display
 * multiple panels.
 */
//@Entity
abstract class AbstractDashboardPanel {
  /**
   * The parent dashboard this is a child of. <b>Required.</b>
   */
  DashboardConfig dashboardConfig

  /**
   * This is a child of a parent Dashboard.
   */
  static belongsTo = [dashboardConfig: DashboardConfig]

  /**
   * The panel's index in the dashboard's list of panels. (Set automatically during validation).
   */
  Integer panelIndex

  /**
   * The index of the parent panel (a splitter) that this panel is located in.
   */
  int parentPanelIndex = -1

  /**
   * Internal constraints.
   */
  static constraints = {
    panelIndex(nullable: false)
  }

  /**
   * Internal mappings.
   */
  static mapping = {
    table 'dashboard_panel'
  }

  /**
   * The primary keys for this object.
   */
  static keys = ['dashboardConfig', 'panelIndex']


}
