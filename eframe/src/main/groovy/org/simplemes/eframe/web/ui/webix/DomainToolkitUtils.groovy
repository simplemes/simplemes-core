/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix

import groovy.util.logging.Slf4j
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.data.format.BigDecimalFieldFormat
import org.simplemes.eframe.data.format.DateFieldFormat
import org.simplemes.eframe.data.format.DateOnlyFieldFormat
import org.simplemes.eframe.data.format.FieldFormatInterface
import org.simplemes.eframe.data.format.IntegerFieldFormat
import org.simplemes.eframe.data.format.LongFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.JavascriptUtils
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.web.PanelUtils
import org.simplemes.eframe.web.ui.webix.widget.BaseLabeledFieldWidget

import java.text.SimpleDateFormat

/**
 * This class provides utilities to create schemas and other data needed for toolkit actions.
 * For example, this will generate the table column definitions for a domain class.
 */
@Slf4j
class DomainToolkitUtils {
  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  static DomainToolkitUtils instance = new DomainToolkitUtils()

  /**
   * Builds the column definitions needed for a GUI toolkit-based grid/table.
   *
   * <h3>options</h3>
   * The valid entries for options include:
   * <ul>
   *   <li><b>columns</b> - The list of columns needed.</li>
   *   <li><b>widths</b> - A map of column widths to use for the columns.  Map<String, BigDecimal>.  The key is the column name, the value is the width (in percent). </li>
   *   <li><b>totalWidth</b> - The total column width (in percent) to spread over all columns if the widths are not given above (<b>Default:</b> 95.0).  </li>
   *   <li><b>keyHyperlink</b> - If true, then the key column will be created as a hyperlink to a show page (<b>Default:</b> true).  </li>
   *   <li><b>sort</b> - If true, sort on the client using the column type.  If 'server', then passes 'server' to the toolkit for server-side sorting. (<b>Default:</b> true).  </li>
   *   <li><b>displayValueForCombo</b> - If true, then display the _${filed}Display_ in the grid instead of the ID.  Used for definition lists.  (<b>Default:</b> false).  </li>
   * </ul>
   *
   * @param domainClass The domain class or POGO.
   * @param columns The list of columns to display.
   * @param options The options used to generate the column definition.  Typically passed in as-is from the marker.
   * @return The column javascript code, with correct types.
   */
  String buildTableColumns(Class domainClass, List<String> columns, Map options = null) {
    log.debug('buildTableColumns() options = {}', options)
    def path = options?.get('_controllerRoot') ?: "/${NameUtils.toDomainName(domainClass)}"
    StringBuilder sb = new StringBuilder()
    def widths = options?.widths
    log.debug('buildTableColumns() columns = {}, widths = {}', columns, widths)
    def fieldDefinitions = DomainUtils.instance.getFieldDefinitions(domainClass)
    for (fieldName in columns) {
      if (PanelUtils.isPanel(fieldName)) {
        // Ignore panels.
        continue
      }
      if (sb.size()) {
        sb << ',\n'
      }

      def keyHyperlink = (options?.keyHyperlink != null) ? options.keyHyperLink : true
      //println "field = $field"
      Class type = DomainUtils.instance.getFieldType(domainClass, fieldName) ?: String
      def fieldDef = fieldDefinitions[fieldName]
      def label = GlobalUtils.lookup("${fieldName}.label")
      def sort = ''
      if (options?.sort) {
        def sortType
        if (options.sort == 'server') {
          sortType = 'server'
        } else {
          // Use client sort
          sortType = getSortType(fieldDef?.format?.class)
        }
        sort = """,sort: "${sortType}" """
      }
      sb << """ {id: "${fieldName}", header: {text: "$label"},adjust: false $sort """
      if (isPrimaryKey(domainClass, fieldName) && keyHyperlink) {
        sb << """, template: "<a href='$path/show/#uuid#'>#$fieldName#</a>" """
      }

      def readOnly = false
      if (options?.get('_widgetContext')?.readOnly != null) {
        readOnly = options._widgetContext.readOnly
      }

      if (!readOnly) {
        // Write the editor if the caller explicitly asked for and editable widget
        def editor = fieldDef?.format?.getGridEditor(fieldDef) ?: 'text'
        sb << """, editor: "$editor" """
        // And build the list of options if needed.
        if (editor == 'combo') {
          sb << buildComboEditorOptions(fieldDef)
        }
      }

      // Add type-specific options
      if (type == String) {
        sb << """, format: webix.template.escape"""
      } else if (type == DateOnly) {
        sb << """, format: webix.i18n.dateFormatStr"""
      } else if (Date.isAssignableFrom(type)) {
        sb << """, format: webix.i18n.fullDateFormatStr"""
        //sb << """, format: '%m/%d/%Y'"""
      } else if (type == BigDecimal) {
        sb << """, format: webix.i18n.numberFormat"""
      } else if (type == Boolean || type == boolean) {
        def checkbox = readOnly ? 'readOnlyCheckbox' : 'checkbox'
        sb << """, template: "{common.$checkbox()}" """
      }

      // Check for combobox style columns that might need help on the client (readOnly case for definition grid).
      if (options?.displayValueForCombo) {
        def editor = fieldDef?.format?.getGridEditor(fieldDef) ?: 'text'
        if (editor == 'combo') {
          if (fieldDef.referenceType == fieldDef.type) {
            // Single reference to another domain, so make sure it displays the key field
            def key = DomainUtils.instance.getPrimaryKeyField(fieldDef.referenceType)
            sb << """,template:function(obj){return ef._getMemberSafely(obj,"$fieldName","$key");}"""
          } else {
            // Make sure the display value from the data source is displayed instead of the ID.
            sb << """,template:function(obj){return ef._getMemberSafely(obj,"_${fieldName}Display_");}"""
          }
        }
      }

      if (widths) {
        if (widths[fieldName]) {
          sb << """,width: tk.pw("${widths[fieldName]}%")"""
        }
      } else if (options?.totalWidth) {
        // Not user-defined widths, so just use a fixed percentage so all fields show up
        def totalWidth = options?.totalWidth ?: 95.0
        def percent = totalWidth / columns.size()
        sb << """,width: tk.pw("${percent}%")"""
      }

      sb << "}"
    }
    return sb.toString()
  }

  /**
   * Builds the combobox editor options (list of choices) and other options to display the choice list correctly.
   * @param fieldDef The field definition.
   * @return The Javascript for the options.
   */
  String buildComboEditorOptions(FieldDefinitionInterface fieldDef) {
    def sb = new StringBuilder()
    def choices = fieldDef.format.getValidValues(fieldDef)
    if (choices) {
      def cb = new StringBuilder()
      def maxValueWidth = 15
      for (choice in choices) {
        if (cb) {
          cb << ',\n'
        }
        def s = choice.toStringLocalized()
        maxValueWidth = Math.max(s.size(), maxValueWidth)
        cb << """{id: "${choice.id}", value: "${JavascriptUtils.escapeForJavascript(s)}"}"""
      }
      // now, add some options to the suggest box that popups.
      def calculateFieldWidth = BaseLabeledFieldWidget.adjustFieldCharacterWidth(maxValueWidth)
      sb << """,suggest: { fitMaster: false, width: tk.pw("${calculateFieldWidth}em")}"""

      sb << """, options: [${cb}] """
    }


    return sb.toString()
  }

  /**
   * Determines if the given field is one of the primary key fields.
   * @param domainClass The domain class.
   * @param name The name of the field.
   * @return True if the field is part of the primary key.
   */
  boolean isPrimaryKey(Class domainClass, String name) {
    return name == DomainUtils.instance.getPrimaryKeyField(domainClass)
  }

  /**
   * Determines the client sort type for the given field type.
   * @param fieldDef The field type.
   * @return The toolkit sort type.
   */
  String getSortType(Class<FieldFormatInterface> format) {
    switch (format) {
      case IntegerFieldFormat:
      case LongFieldFormat:
      case BigDecimalFieldFormat:
        return 'int'
      case DateOnlyFieldFormat:
      case DateFieldFormat:
        return 'date'
    }
    return 'text'

  }

  /**
   * Builds a data parser (scheme) needed for a GUI toolkit-based grid/table.
   * This is used to convert string JSON elements to proper types once the
   * data is received by the client.  Mainly used for dates.
   *
   * <h3>options</h3>
   * The valid entries for options include:
   * <ul>
   *   <li><b>columns</b> - The list of columns needed.</li>
   * </ul>
   *
   * @param domainClass The domain class.
   * @param columns The list of columns to display.
   * @param options The options used to generate the parser logic.  Typically passed in as-is from the marker.
   * @return The javascript scheme code with the correct parsing needed (if any).
   */
  @SuppressWarnings("unused")
  String buildTableDataParser(Class domainClass, List<String> columns, Map options = null) {
    def sb = new StringBuilder()
    // Check each column for the special type(s) that need parsing upon loading.  Mainly Dates.
    log.debug('buildTableDataParser() columns = {}', columns)
    for (fieldName in columns) {
      if (PanelUtils.isPanel(fieldName)) {
        // Ignore panels.
        continue
      }
      Class type = DomainUtils.instance.getFieldType(domainClass, fieldName) ?: String

      if (Date.isAssignableFrom(type) || DateOnly.isAssignableFrom(type)) {
        // We need to parse the date with some special handling of TZ for the gui toolkit.
        sb << """if (typeof obj.$fieldName == 'string') {
          obj.$fieldName = tk._parseISODate(obj.$fieldName,true);
        }
        """
      }
    }
    if (sb.size()) {
      return """scheme:{
        \$init:function(obj){
          ${sb.toString()}
        }
      },"""
    } else {
      return ''
    }
  }

  /**
   * Converts the given date format to a toolkit-style date/time format.
   * @param dateFormat The date format.  Must be a SimpleDateFormat instance.
   * @return The toolkit format.
   */
  String convertDateFormatToToolkit(SimpleDateFormat dateFormat) {
    // See https://docs.webix.com/mobile_calendar__date_format.html for webix details.
    def pattern = dateFormat.toPattern()
    //def orig = pattern
    pattern = pattern.replace('MM', '%m')
    pattern = pattern.replace('M', '%n')
    pattern = pattern.replace('dd', '%x')  // Temporarily convert to 'x' to avoid the next replacement
    pattern = pattern.replace('d', '%j')
    pattern = pattern.replace('%x', '%d')  // Revert back to the toolkit '%d'
    pattern = pattern.replace('yyyy', '%Y')
    pattern = pattern.replace('yy', '%y')
    pattern = pattern.replace('hh', '%G')
    pattern = pattern.replace('h', '%g')
    pattern = pattern.replace('HH', '%x') // Temporarily convert to 'x' to avoid the next replacement
    pattern = pattern.replace('H', '%G')
    pattern = pattern.replace('%x', '%H') // Revert back to the toolkit '%H'
    pattern = pattern.replace('mm', '%i')
    pattern = pattern.replace('ss', '%s')
    pattern = pattern.replace('a', '%A')

    //def sample = getDateFormat.format(new Date()+10)
    //println "converted $orig ($sample) to $pattern"

    return pattern
  }


}
