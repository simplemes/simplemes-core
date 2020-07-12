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
 * Defines a single dashboard panel that displays contents.
 */
@MappedEntity
@DomainEntity
@EqualsAndHashCode(includes = ["dashboardConfig", "panelIndex"])
@ToString(includePackage = false, includeNames = true, excludes = ['dashboardConfig'])
class DashboardPanel {

  /**
   * The parent dashboard this is a child of. <b>Required.</b>
   */
  @ManyToOne
  @MappedProperty(type = DataType.UUID)
  @SuppressWarnings('unused')
  DashboardConfig dashboardConfig

  /**
   * The panel's index in the dashboard's list of panels. (Set automatically during validation).
   */
  Integer panelIndex

  /**
   * The index of the parent panel (a splitter) that this panel is located in.
   */
  int parentPanelIndex = -1

  /**
   * The default URL/URI displayed in this panel.
   */
  @Column(length = FieldSizes.MAX_URL_LENGTH, nullable = true)
  String defaultURL

  /**
   * The default size for this panel (<b>optional</b>).
   */
  @Nullable BigDecimal defaultSize

  /**
   * The panel name for this panel.  This is a fairly static value that is used to reference
   * this panel in Javascript calls.  Typically, this is assigned by the DashboardConfig, but can be manually
   * set if needed.
   */
  @Column(length = FieldSizes.MAX_CODE_LENGTH, nullable = false)
  String panel

  @Id @AutoPopulated
  @MappedProperty(type = DataType.UUID)
  UUID uuid

  /**
   * The primary keys for this object.
   */
  @SuppressWarnings("unused")
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
