/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test.page

import geb.Module

/**
 * Defines the GEB page elements for a standard editable text field.
 * <p>
 * <h4>Example Page Definition:</h4>
 * <pre>
 *   static content = &#123;
 *     defaultFlexType &#123; module(new BooleanFieldModule(field: 'defaultFlexType')) &#125;
 *   &#125;
 * </pre>
 *
 * <p>
 * <h4>Example Test Spec Usage:</h4>
 * <pre>
 *   defaultFlexType.setValue(true)
 *   defaultFlexType.value == true
 * </pre>
 *
 * <h4>This contains these elements:</h4>
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
