package org.simplemes.eframe.domain.controller

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.http.annotation.QueryValue
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import org.simplemes.eframe.controller.BaseController
import org.simplemes.eframe.custom.ExtensibleFieldHelper
import org.simplemes.eframe.data.EncodedTypeInterface
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.data.format.ChildListFieldFormat
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.web.PanelUtils

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The controller for domain class configuration queries.   Provides Get access to domain class's
 * fields, including extensions and hints on GUI display order.
 */
@Slf4j
@Secured(SecurityRule.IS_AUTHENTICATED)
@Controller("/domain")
class DomainController extends BaseController {


  /**
   * Provides the domain class fields define, including field extensions.  Provides the fields displayed in
   * a format suitable for the client rendering.
   * @param request The user logged in.
   * @return The fields.
   */
  @SuppressWarnings('unused')
  @Secured(SecurityRule.IS_ANONYMOUS)
  @Produces(MediaType.APPLICATION_JSON)
  @Get("/displayFields")
  HttpResponse displayFields(HttpRequest request, @QueryValue String domain) {
    def domainClass = TypeUtils.loadClass(domain)
    def keyFields = DomainUtils.instance.getKeyFields(domainClass)
    def allFieldDefs = ExtensibleFieldHelper.instance.getEffectiveFieldDefinitions(domainClass)
    def panelsFound = false

    // Now, filter by the effective field order and sort into the right order.
    def fieldOrder = ExtensibleFieldHelper.instance.getEffectiveFieldOrder(domainClass)
    def fieldDefs = []
    for (field in fieldOrder) {
      if (!PanelUtils.isPanel(field)) {
        fieldDefs << allFieldDefs[field]
      } else {
        panelsFound = true
      }
    }

    def res = [:]

    // Build the key fields first for the 'top' section.
    res.top = []
    for (FieldDefinitionInterface fieldDef in fieldDefs) {
      if (keyFields.contains(fieldDef.name)) {
        res.top << buildFieldElement(fieldDef, domainClass)
      }
    }

    res.bottom = []
    res.tabs = []
    if (panelsFound) {
      // Build the tabbed panels for the fields
      def panels = PanelUtils.organizeFieldsIntoPanels(fieldOrder)
      panels.each { panelName, panelFields ->
        def panelMap = [tab: panelName, tabLabel: "label.${panelName}".toString(), fields: []]
        for (panelFieldName in panelFields) {
          if (!keyFields.contains(panelFieldName)) {
            panelMap.fields << buildFieldElement(allFieldDefs[panelFieldName], domainClass)
          }
        }
        res.tabs << panelMap
      }
    } else {
      // Build the other non-tab fields for the 'bottom' section.
      for (FieldDefinitionInterface fieldDef in fieldDefs) {
        if (!keyFields.contains(fieldDef.name)) {
          res.bottom << buildFieldElement(fieldDef, domainClass)
        }
      }
    }

    return HttpResponse.status(HttpStatus.OK).body(res)
  }

  /**
   * Builds the single field definition needed for the client.
   * @param fieldDef The field.
   * @param domainClass The domain class this field is part of.
   * @return A Map with the field definition for the client's use.
   */
  protected Map buildFieldElement(FieldDefinitionInterface fieldDef, Class domainClass) {
    def res = [:]
    //println "fieldDef = $fieldDef"
    res.fieldName = fieldDef.name
    res.fieldLabel = fieldDef.label
    res.fieldFormat = fieldDef.format.clientFormatType
    res.maxLength = fieldDef.maxLength
    def defaultValue = buildDefault(fieldDef, domainClass)
    if (defaultValue) {
      res.defaultValue = defaultValue
    }

    if (res.fieldFormat == ChildListFieldFormat.instance.clientFormatType) {
      // Need to expand a child list
      res.fields = expandChildElement(fieldDef)
    }

    def validValues = fieldDef.format.getValidValues(fieldDef)
    if (validValues) {
      res.validValues = []
      for (value in validValues) {
        res.validValues << [id: value.id, value: value.displayValue]
      }
      //println "validValues = ${TextUtils.prettyFormat(res.validValues)}"
    }
    def validValuesURI = fieldDef.format.getValidValuesURI(fieldDef)
    if (validValuesURI) {
      res.validValuesURI = validValuesURI
    }

    return res
  }

  /**
   * Expands the given field as a child list.
   * @param fieldDef The field.
   * @return The list of fields for the given child list.
   */
  protected List expandChildElement(FieldDefinitionInterface fieldDef) {
    def res = []

    def domainClass = fieldDef.referenceType
    def fieldDefs = ExtensibleFieldHelper.instance.getEffectiveFieldDefinitions(domainClass)
    def fields = ExtensibleFieldHelper.instance.getEffectiveFieldOrder(domainClass)
    for (field in fields) {
      def childFieldDef = fieldDefs[field]
      if (childFieldDef) {
        res << buildFieldElement(childFieldDef, domainClass)
      }

    }

    return res
  }

  /**
   * Builds the default value for the given field (if any).
   * @param fieldDef The field.
   * @param domainClass The domain class this field is part of.
   * @return The default value string (generally a Javascript expression).
   */
  protected String buildDefault(FieldDefinitionInterface fieldDef, Class domainClass) {
    // Try the explicit clientDefaults element first.
    def clientDefaults = TypeUtils.getStaticProperty(domainClass, 'clientDefaults')
    if (clientDefaults && clientDefaults[fieldDef.name]) {
      return clientDefaults[fieldDef.name]
    }

    // Now, try the domain field defaults.
    def domainObject = domainClass.getConstructor().newInstance()
    def value = domainObject[fieldDef.name]
    if (value) {
      return buildJavascriptForDefault(fieldDef, value)
    }

    return null
  }

  /**
   * Builds the JS-legal form of the given object for use as a default.
   * This provides a string that is legal for the JS eval() method.
   * @param fieldDef The field.
   * @param value The object value.
   * @return The default value string (generally a Javascript expression).
   */
  @SuppressWarnings('unused')
  protected String buildJavascriptForDefault(FieldDefinitionInterface fieldDef, Object value) {
    if (value.getClass().isEnum()) {
      return "'$value.id'"
    } else if (value instanceof EncodedTypeInterface) {
      return "'$value.id'"
    } else if (value instanceof Number || value instanceof Boolean) {
      return "$value"
    }
    return "'$value'"
  }

}
