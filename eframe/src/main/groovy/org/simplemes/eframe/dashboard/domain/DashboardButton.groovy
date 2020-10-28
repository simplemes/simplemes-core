/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard.domain


import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.eframe.misc.FieldSizes

import javax.annotation.Nullable
import javax.persistence.Column
import javax.persistence.ManyToOne

/**
 * This domain class defines a single configurable button on a dashboard.  This allows
 * the user to configure buttons to their needs.
 * These buttons are displayed one after another in the appropriate area
 * of the page.  The buttons have configurable labels, styling, title (tooltip) and display sequence.
 */
@MappedEntity
@DomainEntity
@EqualsAndHashCode(includes = ["dashboardConfig", "sequence", "buttonID"])
@ToString(includePackage = false, includeNames = true, excludes = ['dashboardConfig'])
class DashboardButton {
  /**
   * The parent dashboard this button is a child of. <b>Required.</b>
   */
  @ManyToOne
  @MappedProperty(type = DataType.UUID)
  DashboardConfig dashboardConfig

  /**
   * A unique sequence that controls the display order of the button.  (Defaults to the order the records are stored
   * in the DashboardConfig). The DashboardConfig parent will set this value.
   */
  Integer sequence

  /**
   * The HTML element ID used for the button.  This can be used to access the button within the page.
   */
  @Column(length = FieldSizes.MAX_CODE_LENGTH, nullable = false)
  String buttonID

  /**
   * The title (tooltip) of this button.
   * This value can be used as the key in a lookup into the .properties files for globalization.
   */
  @Column(length = FieldSizes.MAX_TITLE_LENGTH, nullable = true)
  String title

  /**
   * The button label used for this button.
   * This value can be used as the key in a lookup into the .properties files for globalization.
   * <p/>
   * This field is used to group multiple activities on a single button.
   */
  @Column(length = FieldSizes.MAX_TITLE_LENGTH, nullable = false)
  String label

  /**
   * The URL/URI to display in a panel.
   */
  @Column(length = FieldSizes.MAX_URL_LENGTH, nullable = false)
  String url

  /**
   * The panel (name) to display the activity in.  (<b>Required</b>.  No default provided.)
   * Must exist in the dashboard.
   */
  @Column(length = FieldSizes.MAX_CODE_LENGTH, nullable = false)
  String panel

  /**
   * The relative size of this button (1.0 - standard size, the default).
   */
  @Nullable BigDecimal size = 1.0

  /**
   * Additional CSS class(es) to add to the button.
   */
  @Column(length = FieldSizes.MAX_SINGLE_LINE_LENGTH, nullable = true)
  String css

  @Id @AutoPopulated
  @MappedProperty(type = DataType.UUID)
  UUID uuid

  /**
   * The primary keys for this object.
   */
  @SuppressWarnings("unused")
  static keys = ['dashboardConfig', 'sequence']

  /**
   * Defines the order the fields are shown in the edit/show/etc GUIs.
   */
  @SuppressWarnings("unused")
  static fieldOrder = ['sequence', 'label', 'buttonID', 'title', 'size', 'css', 'url', 'panel']

  /**
   * Build human readable version of this object.
   * @return
   */
  @Override
  String toString() {
    return label + "($buttonID ${sequence})"
  }

}
