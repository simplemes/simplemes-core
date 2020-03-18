/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test

import groovy.util.logging.Slf4j
import org.simplemes.eframe.dashboard.controller.DashboardTestController
import org.simplemes.eframe.dashboard.domain.DashboardConfig
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.test.page.DashboardPage

/**
 * This is the common Spock specification base class for testing Dashboard related activities.
 * It provides a way to define dashboard activities in memory for a specific test to avoid creating
 * dozens of test activities as .ftl files.
 * <p/>
 * This base class provides creation of simple one/two panel dashboards with memory-based content and buttons.
 * This allows you to test dashboard functions with minimal dependencies on other elements.
 * <p/>
 * This class depends on the main {@link org.simplemes.eframe.dashboard.controller.DashboardController} and
 * {@link DashboardTestController} to provide the page displays needed for the
 * dashboard itself and the content (test controller).<p/>
 * For example:
 * <pre>
 *   def page1 = """ ... """
 *   buildDashboard(defaults: [page1,page2], buttons: [page3,page4])
 *   displayDashboard()
 *   clickButton(0)  // Displays page3 in the Panel 'B'.
 * </pre>
 * <p>
 * <b>Note</b>: The activity content should be wrapped in <script></script> tags.  If not, it will be treated
 * as HTML test to render in the page.
 * <p>
 */
@Slf4j
class BaseDashboardSpecification extends BaseGUISpecification {
  // Note: Some of this base class is tested in the DashboardJSFormGUISpec.

  /**
   * The panel content used to generate only dashboard buttons.
   */
  public static final String BUTTON_PANEL = '''
    <script>
      <#assign panel = "${params._panel}"/>
      <#assign variable = "${params._variable}"/>
      <@efForm id="logFailure" dashboard='buttonHolder'>
        <@efField field="order" label="Order/LSN" value="M1008" width=20 labelWidth='35%'>
          <@efButton type='undo' id="undoButton" tooltip='undo.title' click='dashboard.undoAction();'/>
        </@efField>
      </@efForm>
    </script>
  '''

  @SuppressWarnings("unused")
  static dirtyDomains = [DashboardConfig]

  @Override
  void cleanup() {
    DashboardTestController.clearMemoryPages()
  }

  /**
   * Builds a test dashboard for use by this GUI tester.  Builds a single or two panel dashboard with dynamic content.
   * Supports buttons to display additional dynamic activities in the last panel.
   * <h3>Options</h3>
   * <ul>
   *   <li><b>defaults</b> - The default activity content for the panels. (<b>Required</b>: Array of Strings - 1, 3 or 3 elements).
   *    </li>
   *   <li><b>buttons</b> - The buttons activity content for the dashboard.
   *     (<b>Optional</b>). Array of Strings or Maps with DashboardButton fields (label, url, panel, title, css, size, buttonID). </li>
   * </ul>
   *
   * <b>Note:</b> If the URL's (defaults or buttons values) start with a '/', then it assumed to be a URL, not the activity content.
   * @param options The options.
   * @return The dashboard name.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  String buildDashboard(Map options) {
    // Build the default pages needed.
    def defaults = []
    ArgumentUtils.checkMissing(options.defaults, 'options.defaults')
    if (options.defaults.size() > 3) {
      throw new IllegalArgumentException('options.defaults must be a string List of size 1..3')
    }
    def buttonActionPanel = 'A'  // The panel each button will display in
    if (options.defaults.size() == 1) {
      defaults << buildActivity((String) options.defaults[0], '0')
    } else if (options.defaults.size() == 2) {
      // A two panel horizontal dashboard
      defaults << 'horizontal0'
      defaults << buildActivity((String) options.defaults[0], '0')
      defaults << buildActivity((String) options.defaults[1], '1')
      buttonActionPanel = 'B'
    } else {
      // A three panel dashboard with the main horizontal splitter and a vertical sub-splitter.
      defaults << 'horizontal0'
      defaults << buildActivity((String) options.defaults[0], '0')
      defaults << 'vertical2'
      defaults << buildActivity((String) options.defaults[1], '1')
      defaults << buildActivity((String) options.defaults[2], '3')
      buttonActionPanel = 'B'
    }

    // Build the buttons.
    def buttons = []
    if (options.buttons) {
      def id = 0
      for (buttonContent in options.buttons) {
        if (buttonContent instanceof Map) {
          //def buttons = [[label: 'b10', url: '/page10', panel: 'A', title: 'title10', css: 'caution-button', size: 1.2, buttonID: 'B10']]
          buttonContent.url = buildActivity((String) buttonContent.url, "B$id")
          if (!buttonContent.label) {
            buttonContent.label = "B$id"
          }
          buttons << buttonContent
        } else {
          // A simple string is the content itself.
          //def buttons = [[label: 'b10', url: '/page10', panel: 'A', title: 'title10', css: 'caution-button', size: 1.2, buttonID: 'B10']]
          def content = buildActivity((String) buttonContent, "B$id")
          buttons << [label: "B${id}", url: content, panel: buttonActionPanel, buttonID: "B$id"]
        }
        id++
      }
    }

    DashboardConfig.withTransaction {
      def cfg = DashboardUnitTestUtils.buildDashboardConfig('_TEST', defaults, buttons)
      log.trace('buildDashboard() cfg: {}', cfg)
    }
    return '_TEST'
  }

  /**
   * Builds the correct activity URL/content reference for the
   * @param activity The activity content.  Can be the content or a URI.
   */
  protected String buildActivity(String activity, String page) {
    if (activity.startsWith('/')) {
      return activity
    }
    // See if this might be simple text (no <script> tags).
    if (!activity.contains("<script>") && !activity.contains("<@efForm")) {
      // Probably just plain text to display, so convert to a .display = expression with a simple template element.
      activity = """_\${params._panel}.display = {view: "template", template: "$activity"};  // Server """
    }

    // Needs a dynamic activity defined for this request.
    DashboardTestController.setMemoryPages(page, activity)
    return "/test/dashboard/memory?page=$page"
  }

  /**
   * Builds the text for simple panel.
   * @param options The options.  Supports: text=Simple Text in the panel, finished: the finished button ID.
   * @return The panel content, suitable as the default or as a button activity content.
   */
  String buildSimplePanel(Map options) {
    def finishedText = ""
    if (options.finished) {
      finishedText = """ 
        <@efButtonGroup>
          <@efButton id="${options.finished}" label="Finish" click="dashboard.finished('\${panel}');"/>
        </@efButtonGroup>
      """
    }
    def extraText = ""
    if (options.text) {
      extraText = """,{view: "template", template: "$options.text"} """
    }
    return """
      <#assign panel = "\${params._panel}"/>
      <@efForm id="FINISH_\${panel}" dashboard=true>
        $extraText
        $finishedText
      </@efForm>
    """
  }

  /**
   * Navigates to the dashboard page with the given dashboard.
   * @param dashboard The dashboard config to navigate to (<b>Default</b>: '_TEST').
   */
  void displayDashboard(String dashboard = '_TEST') {
    displayDashboard([dashboard: dashboard])
  }

  /**
   * Navigates to the dashboard page with the given options.
   * <h3>Options</h3>
   * <ul>
   *   <li><b>dashboard</b> - The dashboard to display (<b>Default</b>: '_TEST'). </li>
   *   <li><b>(other options)</b> - Passed as URL parameters to the dashboard. </li>
   * </ul>

   * @param options Supported options: dashboard - the dasboard nameThe dashboard config to navigate to (<b>Default</b>: '_TEST').
   */
  void displayDashboard(Map options) {
    options = options ?: [:]
    if (!options.dashboard) {
      options.dashboard = '_TEST'
    }

    login()
    to options, DashboardPage
  }


  /**
   * Clicks a dashboard-defined button with the given view integer button number.  This
   * corresponds to the index in the buttons list passed to the {@link #buildDashboard(java.util.Map)} method.
   * @param id The ID of the button (0 = first button in the button list).
   */
  void clickButton(Integer id) {
    clickButton("B$id")
  }
}
