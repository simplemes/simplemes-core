package org.simplemes.eframe.test.page

import geb.Module

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the GEB page elements for a standard read-only text field.  This contains these elements:
 * <ul>
 *   <li><b>label</b> - The label text.</li>
 *   <li><b>value</b> - The field text.</li>
 * </ul>
 *
 */
@SuppressWarnings(["GroovyAssignabilityCheck", "GroovyUnusedDeclaration"])
class ReadOnlyFieldModule extends Module {
  String field

  static content = {
    label { $('div.webix_el_label', view_id: "${field}Label").text() }
    value { $('div.webix_el_label', view_id: "${field}").text() }
  }

}
