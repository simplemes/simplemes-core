package org.simplemes.eframe.reports

import groovy.transform.ToString
import net.sf.jasperreports.engine.JRParameter
import org.simplemes.eframe.data.SimpleFieldDefinition
import org.simplemes.eframe.data.format.BasicFieldFormat
import org.simplemes.eframe.data.format.DateFieldFormat
import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.data.format.FieldFormatInterface
import org.simplemes.eframe.date.ISODate
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.ArgumentUtils

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * Defines a single report parameter field to allow user input of parameters.
 */
@ToString(includePackage = false, includeNames = true, includeSuper = true, includes = ['effectiveValue'])
class ReportFieldDefinition extends SimpleFieldDefinition {

  /**
   * The report details holding POGO used to build this field.
   */
  Report reportDetails

  /**
   * The effective value.  Can be set for Unit Test purposes.
   */
  Object effectiveValue

  /**
   * Constructor for a report parameter definition.
   * @param options The field definition values.
   */
  ReportFieldDefinition(Map options) {
    super(options)
  }

  /**
   * Constructor for use with Report Engine values.
   * @param parameter The parameter from the report engine.
   * @param reportDetails The compiled report details to fill in with data.
   */
  ReportFieldDefinition(JRParameter parameter, Report reportDetails) {
    this([:])
    ArgumentUtils.checkMissing(parameter, 'parameter')
    ArgumentUtils.checkMissing(reportDetails, 'reportDetails')

    name = parameter.name
    format = (FieldFormatInterface) BasicFieldFormat.findByType(parameter.valueClass)
    if (name == ReportEngine.REPORT_TIME_INTERVAL_NAME) {
      format = EnumFieldFormat.instance
      type = ReportTimeIntervalEnum
      referenceType = ReportTimeIntervalEnum
    }
    label = GlobalUtils.lookup(name + '.label') - '.label'

    this.reportDetails = reportDetails
  }

  /**
   * Gets the effective value for the report.  Uses the report parameters or the default value
   * from the compiled report.
   *
   * @return The value of the field.
   */
  Object getEffectiveValue() {
    if (effectiveValue == null) {
      effectiveValue = reportDetails?.params?.get(name)
      if (effectiveValue == null) {
        def defaultParameters = reportDetails?.defaultParameters
        effectiveValue = defaultParameters?.get(name)
      }
      if (format == DateFieldFormat.instance && effectiveValue instanceof String) {
        effectiveValue = ISODate.parse(effectiveValue)
      }
    }
    return effectiveValue
  }

  /**
   * Gets the given field value from the given object (domain or POGO depending on sub-class).
   *
   * @param domainOrPogoObject The domain or POGO object the field is stored in.
   * @return The value of the field.
   */
  @Override
  Object getFieldValue(Object domainOrPogoObject) {
    if (domainOrPogoObject) {
      def value = domainOrPogoObject[name]
      if (value instanceof String) {
        value = format.decode(value, this)
      }
      return value
    }
    return null
  }

  /**
   * Sets the given field value in the given object (domain or POGO depending on sub-class).
   *
   * @param pogo The POGO object the field is to be stored in.
   * @param value The field value.
   */
  @Override
  void setFieldValue(Object pogo, Object value) {
  }

  /**
   * Returns human readable form of this object.
   * @return human readable string.
   */
/*
  @Override
  String toString() {
    return "ReportFieldDefinition{" + super.toString() + "," +
      "effectiveValue='" + getEffectiveValue() + '\'' +
      '}'
  }
*/

}
