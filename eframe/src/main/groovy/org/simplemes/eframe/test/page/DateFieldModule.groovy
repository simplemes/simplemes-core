/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test.page

import geb.Module

/**
 * Defines the GEB page elements for a standard editable text field.  This contains these elements:
 * <p>
 * <h4>Example Page Definition:</h4>
 * <pre>
 *   static content = &#123;
 *     startDateTime &#123; module(new DateFieldModule(field: 'startDateTime')) &#125;
 *   &#125;
 * </pre>
 *
 * <p>
 * <h4>Example Test Spec Usage:</h4>
 * <pre>
 *   startDateTime.input.value() == DateUtils.formatDate(start)
 *   startDateTime.input.value(DateUtils.formatDate(newStart))
 * </pre>
 *
 * <h4>This contains these elements:</h4>
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
    label { $('label', for: field).text() }
    input { $("input#${field}") }
  }

}
