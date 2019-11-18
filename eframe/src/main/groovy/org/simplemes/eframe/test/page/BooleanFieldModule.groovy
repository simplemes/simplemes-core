package org.simplemes.eframe.test.page

import geb.Module

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the GEB page elements for a standard editable text field.  This contains these elements:
 * <ul>
 *   <li><b>label</b> - The label text.</li>
 *   <li><b>input</b> - The input checkbox itself.  Can be clicked.</li>
 * </ul>
 *
 */
@SuppressWarnings(["GroovyAssignabilityCheck", "GroovyUnusedDeclaration"])
class BooleanFieldModule extends Module {
  String field

  static content = {
    label { $('div.webix_el_label', view_id: "${field}Label").text() }
    input { $('div.webix_el_checkbox', view_id: "${field}").find('button') }
  }

  void setValue(Object setValue) {
    if (getValue() != setValue) {
      input.click()
    }
  }

  Boolean getValue() {
    return input.@'aria-checked' == 'true'
  }

}
