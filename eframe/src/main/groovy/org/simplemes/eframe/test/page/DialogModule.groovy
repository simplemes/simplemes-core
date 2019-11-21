package org.simplemes.eframe.test.page

import geb.Module

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the GEB page elements for a standard framework dialog.  The default index is '0'.
 * Defines these elements:
 * <ul>
 *   <li><b>exists</b> - True if the dialog is displayed.</li>
 *   <li><b>title</b> - The text of the dialog's title.</li>
 *   <li><b>okButton</b> - The text of the dialog's Ok button.</li>
 *   <li><b>cancelButton</b> - The text of the dialog's cancel button.</li>
 *   <li><b>header</b> - The dialog's header bar.</li>
 *   <li><b>closeButton</b> - The dialog header's close window button.</li>
 *   <li><b>resizeHandle</b> - The dialog's resize handle.</li>
 *   <li><b>body</b> - The dialog body content.</li>
 * </ul>
 *
 */
@SuppressWarnings(["GroovyAssignabilityCheck", "GroovyUnusedDeclaration"])
class DialogModule extends Module {
  def index = 0

  static content = {
    view { $('div', view_id: "dialog${index}") }

    exists(required: false) { $('div.webix_window', view_id: "dialog${index}").text() != null }
    okButton { $('div.webix_el_button', view_id: "dialog${index}-ok").find('button') }
    cancelButton { $('div.webix_el_button', view_id: "dialog${index}-cancel").find('button') }

    title {
      $('div.webix_window', view_id: "dialog${index}").find('div.webix_win_head').find('div.webix_el_label').text()
    }
    header { $('div.webix_window', view_id: "dialog${index}").find('div.webix_win_head') }
    closeButton { $('div.webix_window', view_id: "dialog${index}").find('button').find('span.wxi-close') }
    resizeHandle { $('div.webix_window', view_id: "dialog${index}").find('div.webix_resize_handle') }

    templateContent { $('div.webix_window', view_id: "dialog${index}").find('div.webix_template') }

    body { $('div.webix_win_body', view_id: "dialog${index}") }
  }

}