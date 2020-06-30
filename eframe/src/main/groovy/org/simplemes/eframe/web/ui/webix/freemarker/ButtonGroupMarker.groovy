/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker


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
      {
        cols: [
          {width: tk.pw("15%")},
          ${content}
          {}
        ]
      },
    """
    write(res)
  }


}
