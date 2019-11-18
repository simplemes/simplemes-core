package org.simplemes.eframe.web.ui.webix.freemarker


/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Provides the efButtonGroup Freemarker marker implementation.
 * This builds a button group with correct spacing for most displays.
 * This marker is normally wrapped around efButton elements.
 */
@SuppressWarnings("unused")
class ButtonGroupMarker extends BaseMarker {

  /**
   * Executes the directive, with the values passed by the setValues() method.
   */
  @Override
  void execute() {
    def content = renderContent()

    def res = """
      ,{
        cols: [
          {width: tk.pw("15%")}
          ${content}
          , {}
        ]
      }
    """
    write(res)
  }


}
