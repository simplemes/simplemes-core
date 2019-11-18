package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.JavascriptUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The tool bar widget.  Produces the toolkit elements needed to build a generic button.
 * This supports a link-style button or regular HTML button element.
 *
 * <h3>Parameters</h3>
 * This widget supports these parameters:
 * <ul>
 *   <li><b>id</b> - The view ID for the button.  This should be unique. </li>
 *   <li><b>label</b> - The label for the button.  This is looked-up in the messages.properties file. </li>
 *   <li><b>tooltip</b> - The tooltip for the button.  (<b>Default</b>: label - '.label'+ '.tooltip')  </li>
 *   <li><b>icon</b> - The icon to display in the button.  (e.g. 'fa-th-list' for font awesome icons)  </li>
 *   <li><b>type</b> - The toolkit button type (<b>Default:</b> 'icon' or 'htmlbutton' for link buttons).  </li>
 *   <li><b>click</b> - The javascript to execute when the button is clicked.  Can be a function call or just the function name.  Ignored if a 'link' is given.</li>
 *   <li><b>link</b> - The link to navigate to when the button is clicked.</li>
 *   <li><b>width</b> - The width of the button.  (<b>Default</b>: 'tk.pw('10%')')  </li>
 *   <li><b>height</b> - The height of the button (<b>Default</b>: Toolkit default - 1em)  </li>
 *   <li><b>css</b> - Extra CSS style(s) for the button. </li>
 *   <li><b>subMenus</b> - The sub-menus for a menu button. The subMenus support these parameters: id (<b>Required</b>),label,click  </li>
 * </ul>
 */
class ButtonWidget extends BaseWidget {

  // Some globals used for this button build methods.
  // These are the script values, which usually include the prefix (e.g. 'id: "anID"'

  String idS = ''
  String labelS = ''
  String iconS = ''
  String iconEntryS = ''
  String tooltipS = ''
  String widthS = "width: tk.pw('10%'),"
  String heightS = ""

  // Some values without the prefix (e.g. 'anID').
  String label

  /**
   * Basic constructor.
   * @param widgetContext The widget context this widget is operating in.  Includes URI, parameters, marker etc.
   */
  ButtonWidget(WidgetContext widgetContext) {
    super(widgetContext)

    // Initialize values common to std button and sub-menu buttons.
    if (widgetContext.parameters.id) {
      idS = """id: "$widgetContext.parameters.id", """
    }

    if (widgetContext.parameters.icon) {
      iconEntryS = "fas ${widgetContext.parameters.icon}"
      iconS = """icon: "$iconEntryS", """
    }

    def tooltip = ''
    if (widgetContext.parameters.label) {
      (label, tooltip) = GlobalUtils.lookupLabelAndTooltip((String) widgetContext.parameters.label,
                                                           (String) widgetContext.parameters.tooltip)
    } else if (widgetContext.parameters.tooltip) {
      // Just a tooltip, with no label case
      tooltip = lookup((String) widgetContext.parameters.tooltip)
    }

    if (label) {
      labelS = """label: "${label}", """
    }

    if (tooltip) {
      tooltipS = """tooltip: "${tooltip}" """
    }

    if (widgetContext.parameters.width) {
      widthS = "width: ${widgetContext.parameters.width},"
    } else if (label && !iconS) {
      // No specific width, so use the label width if no icon is given.
      widthS = """width: tk.pw("${label.size()}em"),"""
    }
  }


  /**
   * Builds the text for the UI elements.
   * @return The UI page text.
   */
  @Override
  CharSequence build() {
    if (widgetContext.parameters.subMenus) {
      return buildMenuButton()
    } else {
      return buildButton()
    }
  }

  /**
   * Builds the text for the UI elements.
   * @return The UI page text.
   */
  String buildButton() {

    def click = ''
    if (widgetContext.parameters.click) {
      def clickHandler = widgetContext.parameters.click
      click = """click: "${JavascriptUtils.escapeForJavascript((String) clickHandler)}", """
    }

    def type = widgetContext.parameters.type ?: 'icon'

    // Override the label to use an HTML link if desired
    def css = ''
    if (widgetContext.parameters.link) {
      type = 'htmlbutton'
      def href = widgetContext.parameters.link
      def a = """<a href="$href" class="toolbar-link" tabindex="-1">"""
      def span1 = """<span class="webix_icon $iconEntryS"></span>"""
      labelS = """label: '$a$span1<span class="toolbar-span"> $label</span></a>',"""
      css = 'no-border'

      // And add a click handler to allow us to take the <a> tag out of the TAB navigation order.
      click = """click: "window.location='$href'", """
    }

    if (widgetContext.parameters.css) {
      if (css) {
        css += " "
      }
      css += "${widgetContext.parameters.css}"
    }
    def cssS = ''
    if (css) {
      cssS = """,css: "$css" """
    }

    // Check for height override.
    if (widgetContext.parameters.height) {
      def h = widgetContext.parameters.height
      heightS = """inputHeight: $h, height: $h,"""
    }


    def s = """{view: "button", $idS $widthS$heightS type: "$type", $iconS $click $labelS $tooltipS $cssS}"""
    return s
  }

  /**
   * Build a menu-style button.
   * @return Returns the view as a menu-style button with sub-menus.
   */
  String buildMenuButton() {
    def subMenus = new StringBuilder()

    // Supports id, label, tooltip, click.
    for (subMenu in widgetContext.parameters.subMenus) {
      if (subMenus) {
        subMenus << ",\n"
      }
      def subID = subMenu.id
      ArgumentUtils.checkMissing(subID, 'subMenu.id')
      def (label, tooltip) = GlobalUtils.lookupLabelAndTooltip(subMenu.label, subMenu.tooltip)
      def tooltipS2 = ''
      if (tooltip) {
        tooltipS2 = """tooltip: "${tooltip}" """
      }

      subMenus << """ {id: "$subID", value: "${label}", ${tooltipS2}} """
    }

    def clickHandlers = new StringBuilder()
    for (subMenu in widgetContext.parameters.subMenus) {
      if (clickHandlers) {
        clickHandlers << "\n"
      }
      def subID = subMenu.id
      def click = subMenu.click
      if (!click.contains('(')) {
        // Turn the click function name into a function call.
        click += "()"
      }
      clickHandlers << """ if (id=="$subID") {$click};  """
    }

    def s = """
      { view: "menu", $idS css: 'toolbar-with-submenu', openAction: "click", type: {subsign: true},
        data: [
          {id: "${id}Menu", value: "${label}",${tooltipS} submenu: [
            ${subMenus}
          ]}
        ],
        on: {
          onMenuItemClick: function (id) {
            $clickHandlers
          }
        }
      }
    """
    return s
  }


}
