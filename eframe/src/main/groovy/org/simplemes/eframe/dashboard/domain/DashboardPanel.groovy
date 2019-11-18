package org.simplemes.eframe.dashboard.domain

import grails.gorm.annotation.Entity
import org.simplemes.eframe.misc.FieldSizes

/**
 * Defines a single dashboard panel that displays contents.
 */
@Entity
class DashboardPanel extends AbstractDashboardPanel {

  /**
   * The default URL/URI displayed in this panel.
   */
  String defaultURL

  /**
   * The default size for this panel (<b>optional</b>).
   */
  BigDecimal defaultSize

  /**
   * The panel name for this panel.  This is a fairly static value that is used to reference
   * this panel in Javascript calls.  Typically, this is assigned by the DashboardConfig, but can be manually
   * set if needed.
   */
  String panel

  static constraints = {
    defaultURL(maxSize: FieldSizes.MAX_URL_LENGTH, blank: true, nullable: true)
    defaultSize(nullable: true)
    panel(maxSize: FieldSizes.MAX_KEY_LENGTH, blank: false, nullable: false)
  }

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
    return "Panel{" +
      "panelIndex=" + panelIndex +
      ", parentPanelIndex=" + parentPanelIndex +
      ", defaultURL='" + defaultURL + "\'" +
      ", defaultSize=" + defaultSize +
      ", panel='" + panel + "\'" +
      '}'
  }


}
