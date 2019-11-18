package org.simplemes.eframe.web.ui.webix.widget
/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The tool bar widget.  Produces the toolkit elements needed for toolbars.  This is a configurable
 * toolbar.
 *
 * <h3>Parameters</h3>
 * This widget supports these parameters:
 * <ul>
 *   <li><b>id</b> - The ID of the widget (view_id) toolbar created. </li>
 *   <li><b>paddingY</b> - The paddingY to use for the toolbar (<b>Default:</b> "-2"). </li>
 * </ul>

 */
class ToolbarWidget extends BaseWidget {
  /**
   * Basic constructor.
   * @param widgetContext The widget context this widget is operating in.  Includes URI, parameters, marker etc.
   */
  ToolbarWidget(WidgetContext widgetContext) {
    super(widgetContext)
  }


  /**
   * Builds the string for the UI elements.
   * @return The UI page text.
   */
  @Override
  CharSequence build() {
    def id = ''
    if (widgetContext.parameters.id) {
      id = """id: "$widgetContext.parameters.id","""
    }

    def paddingY = widgetContext.parameters.paddingY ?: '-2'

    def buttonScript = new StringBuilder()
    for (button in widgetContext.parameters.buttons) {
      if (buttonScript) {
        buttonScript << ",\n"
      }

      def text
      if (button instanceof String) {
        // A special spacer to force the remainder of the buttons to be right-aligned.
        text = """{view: "label", template: "<span>$button</span>"}"""
      } else {
        def w = new WidgetContext((WidgetContext) widgetContext, (Map) button)
        text = new ButtonWidget(w).build().toString()
      }
      buttonScript << text
    }

    def s = """
      { view: "toolbar", $id paddingY: $paddingY,
        elements: [
          $buttonScript
        ]
      }
    """
    return s
  }


}
