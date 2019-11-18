package org.simplemes.eframe.web.ui.webix.freemarker

import groovy.util.logging.Slf4j
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.dashboard.domain.AbstractDashboardPanel
import org.simplemes.eframe.dashboard.domain.DashboardConfig
import org.simplemes.eframe.dashboard.domain.DashboardPanel
import org.simplemes.eframe.dashboard.domain.DashboardPanelSplitter
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.misc.JavascriptUtils
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.security.SecurityUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Provides the implementation of the &lt;ef:dashboard/&gt; tag.
 * This tag is used to display configurable dashboard with application and user-defined elements on a page.
 *
 * <pre>
 *
 * &lt;@efDashboard category="MANAGER"/&gt;
 * </pre>
 * <p/>
 *
 * <h3>Logging</h3>
 * The logging for this class can be enabled:
 * <ul>
 *   <li><b>trace</b> - Logs full details of dashboard settings and panel hierarchy when one is displayed. </li>
 * </ul>
 */
@Slf4j
class DashboardMarker extends BaseMarker {
  // TODO: DashboardE2EGUISpec
  /**
   * The dashboard config being worked.
   */
  DashboardConfig dashboardConfig

  /**
   * The basic HTML ID for the dashboard.  All splitters and panels use this prefix.
   */
  String baseHTMLID

  /**
   * The basic element name used for the dashboard preferences (splitter setting).
   */
  String baseElement

  /**
   * The prefix to add to the dashboard name for the preferences element name(s).
   */
  public static final String ELEMENT_PREFIX = '_d'

  /**
   * The data model name that will hold the activity parameters from the controller.
   */
  public static final String ACTIVITY_PARAMETERS_NAME = '_DashboardActParams'

  /**
   * The category needed for the dashboard editor.
   */
  String categoryForEditor

  /**
   * The preferences for this page.
   */
  PreferenceHolder preferenceHolder

  /**
   * Executes the directive, with the values passed by the setValues() method.
   */
  @Override
  void execute() {
    try {
      configureDashboard()

      if (dashboardConfig) {
        writeDashboard()
      }
    } catch (BusinessException e) {
      writeError(e)
    }
  }

  void writeError(Exception e) {
    def sb = new StringBuilder()
    sb << """
      <script>
        ef.displayMessage({error: '${e.toString()}'});
      </script>
    """

    write(sb.toString())
  }

  /**
   * Read and configure the dashboard for display.
   */
  void configureDashboard() {
    def dashboardName = parameters.dashboard
    def category = parameters.category ?: 'NONE'

    // TODO: Consider moving this DB access to a service with Preferences.
    DashboardConfig.withTransaction() {
      if (dashboardName) {
        // Caller wants a specific dashboard
        dashboardConfig = DashboardConfig.findByDashboard(dashboardName)
        if (dashboardConfig == null) {
          //error.121.message=No dashboard {0} found.
          throw new BusinessException(121, [dashboardName])
        } else {
          categoryForEditor = dashboardConfig.category
        }
      } else {
        // Use the category
        dashboardConfig = DashboardConfig.findByCategoryAndDefaultConfig(category, true)
        if (dashboardConfig == null) {
          //error.112.message=No default dashboard for category {0} found.
          throw new BusinessException(112, [category])
        }
        categoryForEditor = category
      }
      DomainUtils.instance.resolveProxies(dashboardConfig)
      loadSplitterSizes()
    }
    log.trace('configureDashboard() {}', dashboardConfig)
    if (log.traceEnabled) {
      log.trace("configureDashboard() \n {} ${dashboardConfig?.hierarchyToString()}")
    }
    baseHTMLID = "${NameUtils.convertToHTMLID(dashboardConfig?.dashboard)}"
    baseElement = "$ELEMENT_PREFIX$baseHTMLID"
  }


  /**
   * Writes the dashboard itself.
   */
  void writeDashboard() {
    def rowsOrCols = 'rows'
    if (dashboardConfig.panels[0] instanceof DashboardPanelSplitter) {
      if (dashboardConfig.panels[0].vertical) {
        rowsOrCols = 'cols'
      }
    }

    def s = """
    <div id="$baseHTMLID"></div>
    <script>
      ${buildCommunicationVariables()}
      ${buildButtonDefinitions()}
      ${buildPanelDefinitions()}
      ${buildAdditionalActivityParameters()}
      eframe.loadDialogPreferences();
      dashboard.currentDashboard="${dashboardConfig.dashboard}";
      dashboard.currentCategory="${categoryForEditor}";
    
      webix.ui({
        container: '$baseHTMLID',
        type: "space", margin: 4, id: "_$baseHTMLID", width: tk.pw("100%"), height: tk.ph("93%"), $rowsOrCols: [
          ${buildPanels()}
        ]
      });
    </script>
    """
    write(s)
  }

  /**
   * Builds the Javascript variables used to communicate with activities (e.g. _A, _B, etc).
   * @return The script to create the variables.
   */
  String buildCommunicationVariables() {
    def sb = new StringBuilder()
    for (panel in dashboardConfig.panels) {
      if (panel instanceof DashboardPanel) {
        sb << "var _${panel.panel}={};\n"
      }
    }

    return sb.toString()
  }

  /**
   * Writes the panel definitions so the dashboard.js functions can operate on them.
   */
  String buildPanelDefinitions() {
    def sb = new StringBuilder()
    sb << """dashboard._definePanelsAndLoad({"""
    int index = 0
    boolean first = true
    for (panel in dashboardConfig.panels) {
      if (panel instanceof DashboardPanel) {
        if (!first) {
          sb << """,\n"""
        }
        def defaultURL = ''
        if (panel.defaultURL) {
          defaultURL = "defaultURL: \"${panel.defaultURL}\""
        }
        sb << """ "${panel.panel}": {${defaultURL}}"""
        index++
        first = false
      }

    }
    sb << """});\n"""

    return sb.toString()
  }

  /**
   * Writes the button definitions so the dashboard.js functions can display them.
   */
  String buildButtonDefinitions() {
    if (!dashboardConfig.buttons) {
      return ''
    }
    def sb = new StringBuilder()
    sb << """dashboard._defineButtons(["""

    // Group the buttons with the same labels together.
    List<Map> buttons = []
    for (button in dashboardConfig.buttons) {
      def buttonIndex = buttons.findIndexOf { b -> b.label == button.label }
      if (buttonIndex < 0) {
        // Need a new button entry.
        buttonIndex = buttons.size()
        buttons[buttonIndex] = [:]
        buttons[buttonIndex].label = button.label
        buttons[buttonIndex].buttonID = button.buttonID
        buttons[buttonIndex].title = button.title
        buttons[buttonIndex].css = button.css
        buttons[buttonIndex].size = button.size
        buttons[buttonIndex].activities = []
      }
      // Add one more activity.
      buttons[buttonIndex].activities << [url: button.url, panel: button.panel]
    }

    boolean first = true
    for (Map button in buttons) {
      if (!first) {
        sb << """,\n"""
      }
      sb << """{label: "${lookup((String) button.label)}","""
      if (button.buttonID) {
        sb << """id: "${button.buttonID}","""
      }
      if (button.title) {
        sb << """title: "${lookup((String) button.title)}","""
      }
      if (button.css) {
        sb << """css: "${button.css}","""
      }
      sb << """size: ${button.size},"""
      sb << """activities: """
      sb << buildButtonActivities((List) button.activities)
      sb << """}"""

      first = false
    }
    sb << """]);\n"""

    return sb.toString()
  }

  /**
   * Writes the button activities so the dashboard.js functions can display them.
   * The activities have the properties: url and panel.
   */
  String buildButtonActivities(List<Map> activities) {
    def sb = new StringBuilder()
    sb << """["""
    boolean first = true
    for (activity in activities) {
      if (!first) {
        sb << """, """
      }
      sb << """{url: "${activity.url}", panel: "${activity.panel}"}"""
      first = false
    }
    sb << """]"""

    return sb.toString()
  }

  /**
   * Builds the panels needed for the dashboard.
   */
  String buildPanels() {
    if (dashboardConfig?.panels?.size() == 1) {
      return buildSinglePanel()
    } else {
      return buildOneSplitterPanel(dashboardConfig.panels[0].panelIndex)
    }
  }

  /**
   * Builds the single panel scenario (no splitters).
   */
  String buildSinglePanel() {
    def s = """
      {view: "form", id: "PanelA", height: tk.ph("93%"), type: "clean", borderless: true, elements: 
        [
          {view: "template", id: "ContentA", template: " "}
        ]
      }
    """
    return s
  }

  /**
   * Builds one level of splitters.  Supports recursively nested panels.
   * @param panelId The panel ID to generate.
   */
  String buildOneSplitterPanel(int panelId) {
    // Find the two child panels for the given panel
    DashboardPanelSplitter panel = (DashboardPanelSplitter) dashboardConfig.panels.find() { it.panelIndex == panelId }
    def panels = dashboardConfig.panels.findAll() { it.parentPanelIndex == panelId }
    if (panels.size() != 2) {
      //error.98.message=Invalid Value {0}. {1} should be {2}.
      throw new BusinessException(98, [panels.size(), 'panels.size()', '2'])
    }
    panels.sort { a, b -> a.panelIndex <=> b.panelIndex }

    def resizeId = "resizer${panelId}"

    def s = """  
      ${buildSplitterPanelMember(panels[0], true, panel.vertical)},
      {view: "resizer", id: "$resizeId"},
      ${buildSplitterPanelMember(panels[1], false, panel.vertical)}
    """
    return s
  }

  /**
   * Builds one of the splitter's panels.  Calls the buildOneSplitterPanel() when the a sub-splitter is found.
   * @param panel The panel to generate.  Can be a splitter or a content panel.
   * @param topElement True if this is the top element of a splitter panel.  Used to define the position of the splitter
   *        bar and the resize handler logic.
   */
  String buildSplitterPanelMember(AbstractDashboardPanel panel, boolean topElement, Boolean vertical) {
    if (panel instanceof DashboardPanelSplitter) {
      def panelName = "Splitter${panel.panelIndex}"
      def panels = dashboardConfig.panels.findAll() { it.parentPanelIndex == panel.panelIndex }
      panels.sort { a, b -> a.panelIndex <=> b.panelIndex }
      def rowsOrCols = panel.vertical ? 'cols' : 'rows'
      return """
        {
          id: "Panel${panelName}", type: "space", margin: 0,paddingX: 0,paddingY: 0, $rowsOrCols: 
            [
              ${buildSplitterPanelMember((AbstractDashboardPanel) panels[0], true, panel.vertical)}
            , {view: "resizer", id: "resizer${panel.panelIndex}"},
              ${buildSplitterPanelMember((AbstractDashboardPanel) panels[1], false, panel.vertical)}
            ] 
        }
      """
    } else {
      def sizeText = ''
      def resizeHandlerText = ''
      if (topElement) {
        def resizeId = "resizer${panel.parentPanelIndex}"
        def panelSize = getReSizerPreference(resizeId)
        if (panelSize) {
          def size = vertical ? "width: tk.pw(" : "height: tk.ph("
          sizeText = """,${size}"${panelSize}%")"""
        }
        resizeHandlerText = """
            on: {
              onViewResize: function () {
                dashboard._splitterResized("${baseElement}","${panel.panel}","${resizeId}",${vertical});
              }
            },
          """
      }

      def s = """
        {
          view: "form", id: "Panel${panel.panel}"${
        sizeText
      }, type: "clean", margin: 2,paddingX: 2,paddingY: 2, borderless: true, $resizeHandlerText elements: [
            {view: "template", id: "Content${panel.panel}", template: ""}] 
        }
      """
      return s
    }
  }

  /**
   * Finds the user's preference for the panel size (from last resize).
   * @param resizerId The first panel for the splitter.
   * @return The size (in percent).  Can be null.
   */
  BigDecimal getReSizerPreference(String resizerId) {
    preferenceHolder.element = baseElement
    def splitterPref = preferenceHolder[resizerId]
    return splitterPref?.size
  }

  /**
   * Loads the user preferences for the dashboard page.
   */
  void loadSplitterSizes() {
    // Now, write the default size for each panel
    // Load the preferences (if any) for this page/splitter.
    def pageParam = ControllerUtils.instance.determineBaseURI(markerContext?.uri)

    preferenceHolder = PreferenceHolder.find {
      page pageParam
      user SecurityUtils.currentUserName
    }
  }

  /**
   * Build the Javascript to create additional activity parameters needed fto be passed from the dashboard URI to the
   * activity URIs.
   * @return The Javascript.
   */
  String buildAdditionalActivityParameters() {
    def sb = new StringBuilder()
    def activityParams = unwrap(environment.dataModel?.get(ACTIVITY_PARAMETERS_NAME))


    if (activityParams) {
      activityParams.each { k, v ->
        sb << """dashboard._addActivityParameter("${k}","${JavascriptUtils.escapeForJavascript(v)}");\n"""
      }
    }
    return sb.toString()
  }

}

