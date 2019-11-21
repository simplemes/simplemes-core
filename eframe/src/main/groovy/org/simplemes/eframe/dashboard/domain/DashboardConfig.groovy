package org.simplemes.eframe.dashboard.domain

import grails.gorm.annotation.Entity
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.misc.FieldSizes

/**
 * Defines the basic dashboard configuration.  This provides the framework to execute application-level pages (activities)
 * for configurable dashboards.  This class defines the persistent dashboard configuration for pre-defined dashboards
 * and user-configurable dashboards.
 */
@Slf4j
@Entity
@EqualsAndHashCode(includes = ['dashboard'])
@ToString(includePackage = false, includeNames = true, excludes = ['dateCreated', 'lastUpdated', 'errors', 'dirtyPropertyNames', 'attached', 'dirty'])
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
  String category = DEFAULT_CATEGORY

  /**
   * The dashboard's name (primary key).
   * <b>Required.</b>
   */
  String dashboard

  /**
   * The title (short) of this dashboard.  Used for many display purposes.
   */
  String title

  /**
   * If true, then this is the default configuration for a given category.
   * During the save validation, this domain class will ensure that only one dashboard config is marked as the default.
   */
  boolean defaultConfig = true

  /**
   * The date this record was last updated.
   */
  Date lastUpdated

  /**
   * The date this record was created
   */
  @SuppressWarnings("unused")
  Date dateCreated

  /**
   * A list of display panels configured for this dashboard.
   * <b>Required.</b>
   */
  List<AbstractDashboardPanel> panels = []

  /**
   * A list of buttons display in the dashboard.
   * <b>Optional.</b>
   */
  List<DashboardButton> buttons = []

  /**
   * Internal definition for this domain.
   */
  @SuppressWarnings("unused")
  static hasMany = [panels: AbstractDashboardPanel, buttons: DashboardButton]

  @SuppressWarnings("unused")
  static keys = ['dashboard']

  @SuppressWarnings("unused")
  def beforeUpdate() {
    clearOtherDefaultDashboardsIfNeeded()
  }

  @SuppressWarnings("unused")
  def beforeInsert() {
    clearOtherDefaultDashboardsIfNeeded()
  }

  /**
   * Called before validate happens.
   */
  @SuppressWarnings(["unused", "GroovyAssignabilityCheck"])
  def beforeValidate() {
    char startChar = 'A' - 1
    // Find highest single character already in use for a panel name
    for (int i = 0; i < panels.size(); i++) {
      if (panels[i] instanceof DashboardPanel && panels[i].panel) {
        char c = panels[i].panel[0]
        c++  // WIll start at next highest character
        if (c > startChar) {
          startChar = c
        }
      }

    }

    // Set the panels' index to match the array index in panels and assign a panel name (if none)
    for (int i = 0; i < panels?.size(); i++) {
      if (panels[i].panelIndex == null) {
        panels[i].panelIndex = i
      }
      if (panels[i] instanceof DashboardPanel && !panels[i].panel) {
        panels[i].panel = startChar
        startChar++
      }
    }

    // Set the buttons' index to match the array index in buttons
    for (int i = 0; i < buttons.size(); i++) {
      if (buttons[i].sequence == null) {
        buttons[i].sequence = (i + 1) * 10
      }
    }

    // Now, sort on the sequence
    buttons.sort { it.sequence }
  }

  /**
   * Internal method to clear the other dashboards if the default flag is set on this dashboard.
   */
  @SuppressWarnings('UnnecessaryQualifiedReference')
  protected clearOtherDefaultDashboardsIfNeeded() {
    // Make sure all other configs in this category are marked as not default if this one is the default.
    // Must do this in a new session to avoid issues with recursive loop in Unit tests.
    DashboardConfig.withNewSession {
      DashboardConfig.withTransaction {
        if (defaultConfig) {
          // This qualified ref is needed for Unit Tests.  Something in Grails mockup forces this.
          //noinspection UnnecessaryQualifiedReference
          def list = DashboardConfig.findAllByCategoryAndDefaultConfig(category, true)
          for (otherDashboardConfig in list) {
            if (otherDashboardConfig != this && otherDashboardConfig.id != this.id) {
              otherDashboardConfig.defaultConfig = false
              assert otherDashboardConfig.save(flush: true)
            }
          }
        }
      }
    }
  }
  /**
   * Internal constraints for this domain.
   */
  @SuppressWarnings("unused")
  static constraints = {
    dashboard(nullable: false, blank: false, maxSize: FieldSizes.MAX_CODE_LENGTH, unique: true)
    category(nullable: false, blank: false, maxSize: FieldSizes.MAX_CODE_LENGTH)
    title(maxSize: FieldSizes.MAX_TITLE_LENGTH, nullable: true, blank: true)
    panels(minSize: 1, validator: { val -> validatePanels(val) })
    buttons(validator: { val, config -> validateButtons(val, config) })
  }

  /**
   * Validate that the panels have valid panel (names) and parent panel references.
   * @param panels The list of panels.
   */
  static validatePanels(List<AbstractDashboardPanel> panels) {
    List<String> panelNames = panels.collect { it instanceof DashboardPanel ? it.panel : '' }
    for (s in panelNames) {
      if (s != '') {
        if (panelNames.count(s) != 1) {
          return ['unique', s, panelNames.count(s)]
        }
      }
    }

    // Make sure all splitters have exactly 2 children.
    Map<Integer, Integer> childCounts = [:]
    for (panel in panels) {
      if (panel.parentPanelIndex >= 0) {
        //println "parent = ${panel.parentPanelIndex} map = ${splitters}"
        def childCount = childCounts[panel.parentPanelIndex] ?: 0
        childCount++
        childCounts[panel.parentPanelIndex] = childCount
      }
    }

    // Make sure all panels
    for (panel in panels) {
      if (panel.parentPanelIndex >= 0) {
        if (childCounts[panel.parentPanelIndex] != 2) {
          return ['wrongNumberOfPanels', panel.parentPanelIndex, childCounts[panel.parentPanelIndex]]
        }
      }
    }

/*


    // Find all splitters
    def splitters = []
    for (int i = 0; i < panels.size(); i++) {
      if (panels[i] instanceof DashboardPanelSplitter) {
        splitters[i] = [index: panels[i].panelIndex, childCount: 0]
      }
    }
    for (panel in panels) {
      if (panel.parentPanelIndex >= 0) {
        //println "parent = ${panel.parentPanelIndex} map = ${splitters}"
        splitters[panel.parentPanelIndex].childCount++
      }
    }
    for (int i = 0; i < panels.size(); i++) {
      if (panels[i] instanceof DashboardPanelSplitter) {
        if (childCounts[i].childCount != 2) {
          return ['wrongNumberOfPanels', i, splitters[i].childCount]
        }
      }
    }

*/
  }

  /**
   * Validate that the buttons are valid.  Checks the button activities for valid panel (names).
   * @param buttons The list of buttons.
   * @param config The rest of the dashboard config.
   */
  static validateButtons(List<DashboardButton> buttons, DashboardConfig config) {
    for (button in buttons) {
      def panel = config.panels.find { it instanceof DashboardPanel && it.panel == button.panel }
      if (!panel) {
        return ['invalidPanel', button, button.panel]
      }
    }
    return true
  }

  /**
   * Load initial user and all roles available.
   */
  @SuppressWarnings('UnnecessaryQualifiedReference')
  static initialDataLoad() {
    // Load some test dashboard configs, but only for eframe development.
    if (DashboardConfig.count() == 0 && Holders.configuration.appName == 'EFrame' && Holders.environmentDev) {
/*
      DashboardConfig dashboardConfig

      dashboardConfig = new DashboardConfig(dashboard: 'SUPERVISOR_DEFAULT', category: 'SUPERVISOR', title: 'Supervisor')
      dashboardConfig.addToPanels(new DashboardPanelSplitter(panelIndex: 0, vertical: false))
      dashboardConfig.addToPanels(new DashboardPanel(panelIndex: 1, defaultURL: '/parent/activityA', parentPanelIndex: 0))
      dashboardConfig.addToPanels(new DashboardPanel(panelIndex: 2, defaultURL: '/parent/activityB', parentPanelIndex: 0))
      assert dashboardConfig.save()

      dashboardConfig = new DashboardConfig(dashboard: 'OPERATOR_DEFAULT', category: 'OPERATOR', title: 'Operator')
      dashboardConfig.addToPanels(new DashboardPanelSplitter(panelIndex: 0, vertical: false))
      dashboardConfig.addToPanels(new DashboardPanel(panelIndex: 1, parentPanelIndex: 0,
                                                     defaultURL: '/sample/dashboard/page?view=sample/dashboard/wcSelection'))
      dashboardConfig.addToPanels(new DashboardPanel(panelIndex: 2, parentPanelIndex: 0,
                                                     defaultURL: '/sample/dashboard/page?view=sample/dashboard/workList'))
      def button1 = new DashboardButton(label: 'pass.label', url: '/dashSample/display?page=pass', panel: 'A',
                                        title: 'pass.title', size: 1.5, buttonID: 'PASS')
      def button2 = new DashboardButton(label: 'Complete', url: '/sample/dashboard/page?view=sample/dashboard/complete', panel: 'B',
                                        buttonID: 'COMPLETE')
      def button3 = new DashboardButton(label: 'Log Failure', url: '/sample/dashboard/page?view=sample/dashboard/logFailure', panel: 'B',
                                        css: 'caution-button', buttonID: 'FAIL')
      def button4 = new DashboardButton(label: 'Reports', url: '/dashSample/display?page=fail', panel: 'B',
                                        buttonID: 'REPORTS')
      dashboardConfig.addToButtons(button1)
      dashboardConfig.addToButtons(button2)
      dashboardConfig.addToButtons(button3)
      dashboardConfig.addToButtons(button4)
      assert dashboardConfig.save()

      dashboardConfig = new DashboardConfig(dashboard: 'MANAGER_DEFAULT', category: 'MANAGER', title: 'Manager')
      dashboardConfig.addToPanels(new DashboardPanelSplitter(panelIndex: 0, vertical: false))
      dashboardConfig.addToPanels(new DashboardPanel(panelIndex: 1, defaultURL: '/sample/dashboard/page?view=sample/dashboard/wcSelection', parentPanelIndex: 0))
      dashboardConfig.addToPanels(new DashboardPanelSplitter(panelIndex: 2, vertical: true, parentPanelIndex: 0))
      dashboardConfig.addToPanels(new DashboardPanel(panelIndex: 3, defaultURL: '/sample/dashboard/page?view=sample/dashboard/workList', parentPanelIndex: 2))
      dashboardConfig.addToPanels(new DashboardPanelSplitter(panelIndex: 4, vertical: true, parentPanelIndex: 2))
      dashboardConfig.addToPanels(new DashboardPanel(panelIndex: 5, defaultURL: '/sample/dashboard/page?view=sample/dashboard/workList', parentPanelIndex: 4))
      dashboardConfig.addToPanels(new DashboardPanel(panelIndex: 6, defaultURL: '/sample/dashboard/page?view=sample/dashboard/workList', parentPanelIndex: 4))
      dashboardConfig.save()

      //noinspection UnnecessaryQualifiedReference
      log.warn("Created ${DashboardConfig.count()} default dashboards.")
*/
    }

    return null // No real initial data loaded, yet.
  }

  /**
   * Build human readable version of this dashboard's hierarchy.
   * @return The hierarchy
   */
  String hierarchyToString() {
    if (panels.size() < 2) {
      return "Panel${panels[0].panel}[0]"
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
    sb << "${padding}Splitter[$splitterIndex] ${panels[splitterIndex].vertical ? 'Vertical' : 'Horizontal'}\n"
    for (int i = 0; i < panels.size(); i++) {
      if (panels[i] instanceof DashboardPanelSplitter) {
        if (panels[i].parentPanelIndex == splitterIndex) {
          sb << hierarchyToStringInternal(i, level + 1)
        }
      } else if (panels[i].parentPanelIndex == splitterIndex) {
        sb << "${padding}  Panel${panels[i].panel}[$i] ${panels[i].defaultURL ?: ''}\n"
      }
    }
    return sb.toString()
  }

}
