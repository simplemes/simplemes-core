/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.eframe.domain.validate.ValidationError
import org.simplemes.eframe.misc.FieldSizes

import javax.persistence.Column
import javax.persistence.OneToMany

/**
 * Defines the basic dashboard configuration.  This provides the framework to execute application-level pages (activities)
 * for configurable dashboards.  This class defines the persistent dashboard configuration for pre-defined dashboards
 * and user-configurable dashboards.
 */
@Slf4j
@MappedEntity
@DomainEntity
@EqualsAndHashCode(includes = ['dashboard'])
@ToString(includePackage = false, includeNames = true, excludes = ['dateCreated', 'dateUpdated'])
class DashboardConfig {

  /**
   * The default category assigned if none are defined in the dashboard configuration.
   */
  public static final String DEFAULT_CATEGORY = 'NONE'

  /**
   * A flexible category name.  This is used to group types of dashboards for easy user interaction.
   * Typical categories can be 'MANAGER' or 'SUPERVISOR' for manager and supervisor style dashboards.
   * This is not part of the keys for this object.
   * (<b>Default:</b>'NONE').  <b>Required.</b>
   */
  @Column(length = FieldSizes.MAX_CODE_LENGTH, nullable = false)
  String category = DEFAULT_CATEGORY

  /**
   * The dashboard's name (primary key).
   * <b>Required.</b>
   */
  @Column(length = FieldSizes.MAX_CODE_LENGTH, nullable = false)
  String dashboard

  /**
   * The title (short) of this dashboard.  Used for many display purposes.
   */
  @Column(length = FieldSizes.MAX_TITLE_LENGTH, nullable = true)
  String title

  /**
   * If true, then this is the default configuration for a given category.
   * During the save validation, this domain class will ensure that only one dashboard config is marked as the default.
   */
  Boolean defaultConfig = true

  /**
   * A list of display panels configured for this dashboard.
   * <b>Required.</b>
   */
  @OneToMany(mappedBy = "dashboardConfig")
  List<DashboardPanel> dashboardPanels

  /**
   * A list of display panels configured for this dashboard.
   * <b>Required.</b>
   */
  @OneToMany(mappedBy = "dashboardConfig")
  List<DashboardPanelSplitter> splitterPanels

  /**
   * A list of buttons display in the dashboard.
   * <b>Optional.</b>
   */
  @OneToMany(mappedBy = "dashboardConfig")
  List<DashboardButton> buttons


  @DateCreated
  @SuppressWarnings("unused")
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateCreated

  @DateUpdated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  @SuppressWarnings("unused")
  Date dateUpdated

  Integer version = 0

  @Id @AutoPopulated
  @MappedProperty(type = DataType.UUID)
  UUID uuid


  @SuppressWarnings("unused")
  static keys = ['dashboard']

  @SuppressWarnings("unused")
  def beforeSave() {
    clearOtherDefaultDashboardsIfNeeded()
  }

  /**
   * Called before validate happens.
   */
  @SuppressWarnings(["GroovyAssignabilityCheck"])
  def beforeValidate() {
    char startChar = 'A' - 1
    // Find highest single character already in use for a panel name
    for (int i = 0; i < getDashboardPanels().size(); i++) {
      if (getDashboardPanels()[i] instanceof DashboardPanel && getDashboardPanels()[i].panel) {
        char c = getDashboardPanels()[i].panel[0]
        c++  // WIll start at next highest character
        if (c > startChar) {
          startChar = c
        }
      }
    }

    // Set the panels' index to match the array index in panels and assign a panel name (if none)
    for (int i = 0; i < dashboardPanels?.size(); i++) {
      def panel = dashboardPanels[i]
      if (panel.panelIndex == null) {
        panel.panelIndex = i
      }
      if (!panel.panel) {
        panel.panel = startChar
        startChar++
      }
    }

    // Set the buttons' index to match the array index in buttons
    for (int i = 0; i < getButtons().size(); i++) {
      if (buttons[i].sequence == null) {
        buttons[i].sequence = (i + 1) * 10
      }
    }

    // Now, sort on the sequence
    buttons.sort { it.sequence }
  }

  /**
   * Validates the config.
   * @return A list of errors (may be empty).
   */
  List<ValidationError> validate() {
    beforeValidate()

    List res1 = validatePanels()
    List res2 = validateButtons()

    res1.addAll(res2)


    if (dashboardPanels?.size() <= 0) {
      //error.200.message=The list value ({0}) must have at least one entry in it.
      res1 << new ValidationError(200, 'panels')
    }

    return res1
  }

  /**
   * Internal method to clear the other dashboards if the default flag is set on this dashboard.
   */
  @SuppressWarnings('UnnecessaryQualifiedReference')
  protected clearOtherDefaultDashboardsIfNeeded() {
    // Make sure all other configs in this category are marked as not default if this one is the default.
    if (defaultConfig) {
      //noinspection UnnecessaryQualifiedReference
      def list = DashboardConfig.findAllByCategoryAndDefaultConfig(category, true)
      for (otherDashboardConfig in list) {
        if (otherDashboardConfig != this && otherDashboardConfig.uuid != this.uuid) {
          otherDashboardConfig.defaultConfig = false
          assert otherDashboardConfig.save()
        }
      }
    }
  }

  /**
   * Validate that the panels have valid panel (names) and parent panel references.
   * @param panels The list of panels.
   */
  List<ValidationError> validatePanels() {
    def res = []
    List<String> panelNames = getDashboardPanels().collect { it instanceof DashboardPanel ? it.panel : '' }
    for (s in panelNames) {
      if (s != '') {
        if (panelNames.count(s) != 1) {
          //error.203.message=The panel must be unique for each panel.  Panel {1} is used on {2} panels
          res << new ValidationError(203, 'dashboardPanels', s, panelNames.count(s))
        }
      }
    }

    // Make sure all splitters have exactly 2 children.
    Map<Integer, Integer> childCounts = [:]
    for (panel in getDashboardPanels()) {
      if (panel.parentPanelIndex >= 0) {
        //println "parent = ${panel.parentPanelIndex} map = ${splitters}"
        def childCount = childCounts[panel.parentPanelIndex] ?: 0
        childCount++
        childCounts[panel.parentPanelIndex] = childCount
      }
    }
    for (panel in getSplitterPanels()) {
      if (panel.parentPanelIndex >= 0) {
        //println "parent = ${panel.parentPanelIndex} map = ${splitters}"
        def childCount = childCounts[panel.parentPanelIndex] ?: 0
        childCount++
        childCounts[panel.parentPanelIndex] = childCount
      }
    }

    // Make sure all panels
    for (panel in getDashboardPanels()) {
      if (panel.parentPanelIndex >= 0) {
        if (childCounts[panel.parentPanelIndex] != 2) {
          //error.205.message=The splitter panel {1} must have exactly 2 child panels. It has {2} panels.
          res << new ValidationError(205, 'dashboardPanels', panel.parentPanelIndex, childCounts[panel.parentPanelIndex])
        }
      }
    }
    return res
  }

  /**
   * Validate that the buttons are valid.  Checks the button activities for valid panel (names).
   * @param buttons The list of buttons.
   * @param config The rest of the dashboard config.
   */
  List<ValidationError> validateButtons() {
    def res = []
    for (button in getButtons()) {
      def panel = getDashboardPanels().find { it instanceof DashboardPanel && it.panel == button.panel }
      if (!panel) {
        //error.204.message=The button {1} references an invalid panel {2}
        res << new ValidationError(204, 'buttons', button, button.panel)
      }
    }
    return res
  }

  /**
   * Load initial user and all roles available.
   */
  @SuppressWarnings('UnnecessaryQualifiedReference')
  static initialDataLoad() {
    // Load some test dashboard configs, but only for eframe work.
/*
    if (DashboardConfig.count() == 0 && Holders.configuration.appName == 'EFrame') {
      DashboardConfig dashboardConfig

      dashboardConfig = new DashboardConfig(dashboard: 'SUPERVISOR_DEFAULT', category: 'SUPERVISOR', title: 'Supervisor')
      dashboardConfig.splitterPanels << (new DashboardPanelSplitter(panelIndex: 0, vertical: false))
      dashboardConfig.dashboardPanels << (new DashboardPanel(panelIndex: 1, defaultURL: '/test/dashboard/page?view=sample/dashboard/wcSelection', parentPanelIndex: 0))
      dashboardConfig.dashboardPanels << (new DashboardPanel(panelIndex: 2, defaultURL: '/order/orderWorkList', parentPanelIndex: 0))
      assert dashboardConfig.save()

      dashboardConfig = new DashboardConfig(dashboard: 'OPERATOR_DEFAULT', category: 'OPERATOR', title: 'Operator')
      dashboardConfig.splitterPanels << (new DashboardPanelSplitter(panelIndex: 0, vertical: false))
      dashboardConfig.dashboardPanels << (new DashboardPanel(panelIndex: 1, parentPanelIndex: 0,
                                                             defaultURL: '/test/dashboard/page?view=sample/dashboard/wcSelection'))
      dashboardConfig.dashboardPanels << (new DashboardPanel(panelIndex: 2, parentPanelIndex: 0,
                                                             defaultURL: '/test/dashboard/page?view=sample/dashboard/workList'))
      def button1 = new DashboardButton(label: 'pass.label', url: '/dashSample/display?page=pass', panel: 'A',
                                        title: 'pass.title', size: 1.5, buttonID: 'PASS')
      def button2 = new DashboardButton(label: 'Complete', url: '/test/dashboard/page?view=sample/dashboard/complete', panel: 'B',
                                        buttonID: 'COMPLETE')
      def button3 = new DashboardButton(label: 'Log Failure', url: '/test/dashboard/page?view=sample/dashboard/logFailure', panel: 'B',
                                        css: 'caution-button', buttonID: 'FAIL')
      def button4 = new DashboardButton(label: 'Reports', url: '/dashSample/display?page=fail', panel: 'B',
                                        buttonID: 'REPORTS')
      dashboardConfig.buttons << (button1)
      dashboardConfig.buttons << (button2)
      dashboardConfig.buttons << (button3)
      dashboardConfig.buttons << (button4)
      assert dashboardConfig.save()

      dashboardConfig = new DashboardConfig(dashboard: 'MANAGER_DEFAULT', category: 'MANAGER', title: 'Manager')
      dashboardConfig.splitterPanels << (new DashboardPanelSplitter(panelIndex: 0, vertical: false))
      dashboardConfig.dashboardPanels << (new DashboardPanel(panelIndex: 1, defaultURL: '/test/dashboard/page?view=sample/dashboard/wcSelection', parentPanelIndex: 0))
      dashboardConfig.splitterPanels << (new DashboardPanelSplitter(panelIndex: 2, vertical: true, parentPanelIndex: 0))
      dashboardConfig.dashboardPanels << (new DashboardPanel(panelIndex: 3, defaultURL: '/test/dashboard/page?view=sample/dashboard/workList', parentPanelIndex: 2))
      dashboardConfig.dashboardPanels << (new DashboardPanel(panelIndex: 6, defaultURL: '/test/dashboard/page?view=sample/dashboard/workList', parentPanelIndex: 2))
      dashboardConfig.save()

      //noinspection UnnecessaryQualifiedReference
      log.warn("Created ${DashboardConfig.count()} default dashboards.")
    }
*/

    return null // No real initial data loaded, yet.
  }

  /**
   * Build human readable version of this dashboard's hierarchy.
   * @return The hierarchy
   */
  String hierarchyToString() {
    if (getSplitterPanels().size() < 1) {
      return "Panel${getDashboardPanels()[0].panel}[0]"
    }
    return hierarchyToStringInternal(0, 0)
  }

  /**
   * Internal, recursive dashboard hierarchy builder.
   * @param splitterIndex The splitter to display.
   * @param level The indention level.
   * @return The hierarchy for this splitter.
   */
  private String hierarchyToStringInternal(int splitterIndex, int level) {
    StringBuilder sb = new StringBuilder()
    def padding = '  ' * level
    sb << "${padding}Splitter[$splitterIndex] ${getSplitterPanels()[splitterIndex].vertical ? 'Vertical' : 'Horizontal'}\n"
    for (panel in getDashboardPanels()) {
      if (panel.parentPanelIndex == splitterIndex) {
        sb << "${padding}  Panel${panel.panel}[${panel.panelIndex}] ${panel.defaultURL ?: ''}\n"
      }
    }
    // Now, dump the child splitters (if any)
    for (int i = 0; i < getSplitterPanels().size(); i++) {
      if (getSplitterPanels()[i].parentPanelIndex == splitterIndex) {
        sb << hierarchyToStringInternal(i, level + 1)
      }
    }
    return sb.toString()
  }

}
