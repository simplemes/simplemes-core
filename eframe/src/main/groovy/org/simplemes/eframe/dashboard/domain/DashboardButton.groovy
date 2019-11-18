package org.simplemes.eframe.dashboard.domain

import grails.gorm.annotation.Entity
import org.simplemes.eframe.misc.FieldSizes

/**
 * This domain class defines a single configurable button on a dashboard.  This allows
 * the user to configure buttons to their needs.
 * These buttons are displayed one after another in the appropriate area
 * of the page.  The buttons have configurable labels, styling, title (tooltip) and display sequence.
 */
@Entity
class DashboardButton {
  /**
   * The parent dashboard this button is a child of. <b>Required.</b>
   */
  DashboardConfig dashboardConfig

  /**
   * A unique sequence that controls the display order of the button.  (Defaults to the order the records are stored
   * in the DashboardConfig). The DashboardConfig parent will set this value.
   */
  Integer sequence

  /**
   * The HTML element ID used for the button.  This can be used to access the button within the page.
   */
  String buttonID

  /**
   * The title (tooltip) of this button.
   * This value can be used as the key in a lookup into the .properties files for globalization.
   */
  String title

  /**
   * The button label used for this button.
   * This value can be used as the key in a lookup into the .properties files for globalization.
   * <p/>
   * This field is used to group multiple activities on a single button.
   */
  String label

  /**
   * The URL/URI to display in a panel.
   */
  String url

  /**
   * The panel (name) to display the activity in.  (<b>Required</b>.  No default provided.)
   * Must exist in the dashboard.
   */
  String panel

  /**
   * The relative size of this button (1.0 - standard size, the default).
   */
  BigDecimal size = 1.0

  /**
   * Additional CSS class(es) to add to the button.
   */
  String css

  /**
   * The primary keys for this object.
   */
  static keys = ['dashboardConfig', 'sequence']

  /**
   * This is a child of a parent Dashboard.
   */
  @SuppressWarnings("unused")
  static belongsTo = [dashboardConfig: DashboardConfig]

  static constraints = {
    dashboardConfig(nullable: false)
    buttonID(maxSize: FieldSizes.MAX_CODE_LENGTH, blank: true, nullable: true)
    label(maxSize: FieldSizes.MAX_TITLE_LENGTH, blank: false, nullable: false)
    title(maxSize: FieldSizes.MAX_TITLE_LENGTH, blank: true, nullable: true)
    css(maxSize: FieldSizes.MAX_SINGLE_LINE_LENGTH, blank: true, nullable: true)
    size(scale: FieldSizes.STANDARD_DECIMAL_SCALE, nullable: true)
    url(maxSize: FieldSizes.MAX_URL_LENGTH, blank: false, nullable: false)
    panel(maxSize: FieldSizes.MAX_KEY_LENGTH, blank: false, nullable: false)
  }

  /**
   * Defines the order the fields are shown in the edit/show/etc GUIs.
   */
  static fieldOrder = ['sequence', 'label', 'buttonID', 'title', 'size', 'css', 'url', 'panel']

  /**
   * Build human readable version of this object.
   * @return
   */
  @Override
  String toString() {
    return label + "($buttonID ${sequence})"
  }

  /**
   * Build a full human readable version of this object.
   * @return
   */
/*
  String toFullString() {
    return "Button{" +
      "sequence=" + sequence +
      ", label='" + label + "\'" +
      ", buttonID='" + buttonID + "\'" +
      ", title='" + title + "\'" +
      ", style='" + style + "\'" +
      ", url='" + url + "\'" +
      ", panel='" + panel + "\'" +
      '}'
  }
*/

}
