/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.reports

import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.custom.ExtensibleFieldHelper
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.data.annotation.ExtensibleFieldHolder
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.TextUtils

import javax.annotation.Nullable

/**
 * Report utils for use with the Report Engine.  
 */
class ReportUtils {


  /**
   * Formats the extensible fields from the json field in a format suitable for web pages.
   * <p>
   * <b>Note</b>: If a flexTypeUuid is not given, then the field ID's will be used instead of the labels.
   *
   * @param fields The fields (as a JSON string).
   * @param flexTypeUuid The UUID of the Flex Type for this set of fields.
   * @param flexTypeName The same of the flex type field (e.g. 'assemblyDataType').
   * @param highlightFieldNames If true, then the field name is highlighted (with HTML <b>bold</b> elements)..
   * @param maxLength The max length of the values to display.
   * @return The formatted values.
   */
  static String formatFields(String fieldJson, String flexTypeUuid, String flexTypeName, Boolean highlightFieldNames = true, Integer maxLength = 100) {
    if (!fieldJson) {
      return ''
    }

    StringBuilder sb = new StringBuilder()
    Map options = [highlight: highlightFieldNames, maxLength: maxLength]

    // Make a dummy Map to simulate an object with this field type
    def flexType = FlexType.findByUuid(UUID.fromString(flexTypeUuid))
    def object = new DummyFieldHolder()
    object.assemblyDataType = flexType
    object.fields = fieldJson

    flexType?.fields?.sort { a, b -> a.sequence <=> b.sequence }
    for (field in flexType?.fields) {
      def label = GlobalUtils.lookup(field.fieldLabel ?: field.fieldName)
      def value = ExtensibleFieldHelper.instance.getFieldValue(object, field.fieldName)
      def valueString = field.fieldFormat.format(value, null, null)
      TextUtils.addFieldForDisplay(sb, label, valueString, options)
    }

    return sb.toString()
  }
}

class DummyFieldHolder {

  // Test support for configurable fields in a POGO.
  FlexType assemblyDataType

  @Nullable
  @ExtensibleFieldHolder
  @MappedProperty(type = DataType.JSON)
  String fields

}
