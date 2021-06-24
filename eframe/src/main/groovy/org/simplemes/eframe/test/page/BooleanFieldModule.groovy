/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test.page

import geb.Module
import geb.navigator.Navigator

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
    label { $('label', for: field).text() }
    parent { $('label', for: field).parent() }
  }

  void setValue(Object setValue) {
    if (getValue() != setValue) {
      parent.click()
    }
  }

  Navigator click() {
    //parent.click()
    parent.find('div.p-checkbox').click()
  }

  Boolean getValue() {
    return parent.find('div.p-checkbox-box', role: 'checkbox').@'aria-checked' == 'true'
  }

}
