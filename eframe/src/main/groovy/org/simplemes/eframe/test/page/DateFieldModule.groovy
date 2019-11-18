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
 *   <li><b>input</b> - The input field itself.</li>
 *   <li><b>invalid</b> - True if the input field is marked as invalid (css makes it appear red).</li>
 * </ul>
 *
 */
@SuppressWarnings(["GroovyAssignabilityCheck", "GroovyUnusedDeclaration"])
class DateFieldModule extends Module {
  String field

  static content = {
    label { $('div.webix_el_label', view_id: "${field}Label").text() }
    input { $('div.webix_el_datepicker', view_id: "${field}").find('input') }
    invalid { $('div.webix_el_datepicker', view_id: "${field}").classes().contains('webix_invalid') }
  }

}
