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

import javax.persistence.ManyToOne

/**
 * Defines a single dashboard panel splitter.  These splitters have two or more children panels that have the actual display
 * contents.
 */
@MappedEntity
@DomainEntity
@EqualsAndHashCode(includes = ["dashboardConfig", "panelIndex"])
@ToString(includePackage = false, includeNames = true, excludes = ['dashboardConfig'])
class DashboardPanelSplitter {
  /**
   * The parent dashboard this is a child of. <b>Required.</b>
   */
  @SuppressWarnings('unused')
  @ManyToOne
  @MappedProperty(type = DataType.UUID)
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
   * If true, then this splitter is split vertically.
   */
  boolean vertical = false

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
    return "Splitter{" +
      ", panelIndex=" + panelIndex +
      ", parentPanelIndex=" + parentPanelIndex +
      ", vertical=" + vertical +
      "uuid=" + uuid +
      '}'
  }

}
