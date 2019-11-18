package org.simplemes.eframe.dashboard.domain

import grails.gorm.annotation.Entity

/**
 * Defines a single dashboard panel splitter.  These splitters have two or more children panels that have the actual display
 * contents.
 */
@Entity
class DashboardPanelSplitter extends AbstractDashboardPanel {
  /**
   * If true, then this splitter is split vertically.
   */
  boolean vertical = false

  /**
   * The primary keys for this object.
   */
  static keys = ['dashboardConfig', 'panelIndex']

  /**
   * Build human readable version of this object.
   * @return
   */
  @Override
  String toString() {
    return "Splitter{" +
      "id=" + id +
      ", panelIndex=" + panelIndex +
      ", parentPanelIndex=" + parentPanelIndex +
      ", vertical=" + vertical +
      '}'
  }

}
