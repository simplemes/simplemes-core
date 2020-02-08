/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.controller

import groovy.util.logging.Slf4j
import io.micronaut.core.io.Writable
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import io.micronaut.security.annotation.Secured
import io.micronaut.views.ViewsRenderer
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.controller.BaseController
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.controller.StandardModelAndView
import org.simplemes.eframe.custom.domain.FieldExtension
import org.simplemes.eframe.custom.service.ExtensionService
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.domain.DomainBinder
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.exception.MessageHolder
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.JavascriptUtils
import org.simplemes.eframe.security.Roles

import javax.annotation.Nullable
import javax.inject.Inject
import java.security.Principal

/**
 * Controller to support GUI extension/customization.  Provides dialogs and server support for customizing GUIs and
 * domains.
 * <p>
 * All methods require the DESIGNER role.
 */
@Slf4j
@Secured(Roles.DESIGNER)
@Controller("/extension")
class ExtensionController extends BaseController {

  /**
   * The service that handles most preference actions.
   */
  @Inject
  ExtensionService extensionService

  /**
   * Serves up a page for the extension-related client functions. This is served from an .ftl file.
   *
   * <h3>Parameters</h3>
   * The HTTP Get parameters supported:
   * <ul>
   *   <li><b>dialog</b> - The dialog (.ftl file) to render for display (e.g. '/extension/editFieldDialog').
   *   </li>
   * </ul>
   * @param request The request.
   * @param principal
   * @return The page.
   */
  @Get("/dialog")
  @Produces('application/javascript')
  HttpResponse dialog(HttpRequest request, @Nullable Principal principal) {
    def params = ControllerUtils.instance.convertToMap(request.parameters)
    if (!params.dialog) {
      throw new IllegalArgumentException('Missing parameter dialog')
    }
    def modelAndView = new StandardModelAndView((String) params.dialog, principal, this)
    def renderer = Holders.applicationContext.getBean(ViewsRenderer)
    Writable writable = renderer.render(modelAndView.view.get(), modelAndView.model.get())
    return HttpResponse.status(HttpStatus.OK).body(writable)
  }


  /**
   * Serves the configuration dialog content for the definition GUI configuration editor.
   * This controller finds the current state and passes it to the .ftl file to generate
   * the dialog content.
   *
   * <h3>Parameters</h3>
   * The HTTP Get parameters supported:
   * <ul>
   *   <li><b>domainURL</b> - The URL that requested the config dialog.  This is used to find the core
   *                          domain class.
   *   </li>
   * </ul>
   *
   * <h3>Model(s) Provided</h3>
   * The model variables available to the .ftl file include:
   * <ul>
   *   <li><b>availableFields</b> - A Javascript formatted list of the fields that could be configured in the domain.
   *                                This includes the elements: name, label, type, custom (boolean).
   *   <li><b>configuredFields</b> - A Javascript formatted list of the fields already configured to be displayed by the GUI/domain.
   *                                This list has the same elements as the availableFields.
   *   </li>
   * </ul>
   * @param request The request.
   * @param principal
   * @return The page.
   */
  @Get("/configDialog")
  //@Produces('application/javascript')
  @Produces(MediaType.TEXT_HTML)
  StandardModelAndView configDialog(HttpRequest request, @Nullable Principal principal) {
    def params = ControllerUtils.instance.convertToMap(request.parameters)
    ArgumentUtils.checkMissing(params.domainURL, 'params.domainURL')
    def modelAndView = new StandardModelAndView('extension/configDialog', principal, this)
    def domainClass = ControllerUtils.instance.getDomainClass(params.domainURL)
    def (available, configuredFields) = extensionService.getExtensionConfiguration(domainClass)

    modelAndView.model.get().put('availableFields', JavascriptUtils.buildJavascriptObject((List) available))
    modelAndView.model.get().put('configuredFields', JavascriptUtils.buildJavascriptObject((List) configuredFields))

    return modelAndView
  }

  /**
   * Serves the custom field edit/add dialog content for the definition GUI configuration editor.
   * This controller finds the current state and passes it to the .ftl file to generate
   * the dialog content.
   *
   * <h3>Parameters</h3>
   * The HTTP Get parameters supported:
   * <ul>
   *   <li><b>domainURL</b> - The URL that requested the config dialog.  This is used to find the core
   *                          domain class.
   *   </li>
   * </ul>
   *
   * <h3>Model(s) Provided</h3>
   * The model variables available to the .ftl file include:
   * <ul>
   *   <li><b>id</b> - The original ID of the field (if in edit mode).
   *   </li>
   * </ul>
   * @param request The request.
   * @param principal
   * @return The page.
   */
  @Get("/editFieldDialog")
  @Produces(MediaType.TEXT_HTML)
  StandardModelAndView fieldDialog(HttpRequest request, @Nullable Principal principal) {
    def params = ControllerUtils.instance.convertToMap(request.parameters)
    ArgumentUtils.checkMissing(params.domainURL, 'params.domainURL')
    def modelAndView = new StandardModelAndView('extension/editFieldDialog', principal, this)

    def id = params.id
    if (id) {
      FieldExtension.withTransaction {
        // Store the existing record in the model for the view/markers.
        def record = FieldExtension.get(UUID.fromString((String) id))
        modelAndView.model.get().put(ControllerUtils.MODEL_KEY_DOMAIN_OBJECT, record)
      }
    } else {
      // Store the default record in the model for the view/markers.
      def record = new FieldExtension(fieldFormat: StringFieldFormat.instance, maxLength: 10)
      modelAndView.model.get().put(ControllerUtils.MODEL_KEY_DOMAIN_OBJECT, record)
    }

    return modelAndView
  }

  /**
   * Saves the GUI definition field order.
   *
   * <h3>Body</h3>
   * The request body is a JSON object with these elements:
   * <ul>
   *   <li><b>domainURL</b> - The URL that requested the config dialog.  This is used to find the core
   *                          domain class.
   *   <li><b>fields</b> - An array of field names as configure by the user.
   *   </li>
   * </ul>
   *
   * Returns a standard message response if save worked.  An error if not.
   * @param request The request.
   * @param principal
   * @return The message response.
   */
  @SuppressWarnings("unused")
  @Post("/saveFieldOrder")
  @Produces(MediaType.APPLICATION_JSON)
  HttpResponse saveFieldOrder(HttpRequest request, @Nullable Principal principal) {
    String body = request.body.get()
    log.debug("saveFieldOrder() body {}", body)
    def map = Holders.objectMapper.readValue(body, Map)

    ArgumentUtils.checkMissing(map.domainURL, 'body.domainURL')
    ArgumentUtils.checkMissing(map.fields, 'body.fields')

    def domainClass = ControllerUtils.instance.getDomainClass(map.domainURL)

    extensionService.saveFieldOrder(domainClass, (List) map.fields)

    def holder = new MessageHolder(level: MessageHolder.LEVEL_INFO, text: GlobalUtils.lookup('definitionEditor.saved.message'))
    return HttpResponse.status(HttpStatus.OK).body(Holders.objectMapper.writeValueAsString(holder))
  }

  /**
   * Saves the custom field definition.
   *
   * <h3>Body</h3>
   * The request body is a JSON object with these elements:
   * <ul>
   *   <li><b>domainURL</b> - The URL that requested the config dialog.  This is used to find the core
   *                          domain class.
   *   <li><b>FieldExtension properties</b> - All properties for a field extension record.
   *   </li>
   * </ul>
   *
   * <h3>Response</h3>
   * The Response contains a map with these fields:
   * <ul>
   *   <li><b>name</b> - The field name.
   *   <li><b>label</b> - The field label (localized).
   *   <li><b>type</b> - The field type (e.g. 'textField', 'dateField', etc).
   *   <li><b>custom</b> - True if the field is a custom field.
   * </ul>
   *
   * @param request The request.
   * @param principal
   * @return The field extension details.
   */
  @Post("/saveField")
  @Produces(MediaType.APPLICATION_JSON)
  HttpResponse saveField(HttpRequest request, @Nullable Principal principal) {
    String body = request.body.get()
    log.debug("saveField() body {}", body)
    def map = Holders.objectMapper.readValue(body, Map)
    String id = map.id
    map.remove('id')

    ArgumentUtils.checkMissing(map.domainURL, 'body.domainURL')
    def domainClass = ControllerUtils.instance.getDomainClass(map.domainURL)
    map.remove('domainURL')

    FieldExtension fieldExtension = null
    def errorResponse = null
    domainClass.withTransaction {
      if (id) {
        fieldExtension = FieldExtension.get(UUID.fromString(id))
        if (!fieldExtension) {
          //error.134.message=The record (id={0}) for domain {1} could not be found.
          throw new BusinessException(134, [id, domainClass.simpleName])
        }
      } else {
        fieldExtension = new FieldExtension(domainClassName: domainClass.name)
      }
      DomainBinder.build().bind(fieldExtension, map)
      errorResponse = checkForValidationErrors(fieldExtension)
      if (!errorResponse) {
        fieldExtension.save()
      }
    }
    if (errorResponse) {
      return errorResponse
    } else {
      def type = extensionService.determineFieldType(fieldExtension.fieldName, fieldExtension.fieldFormat)
      def label = fieldExtension.fieldLabel ? GlobalUtils.lookup(fieldExtension.fieldLabel) : fieldExtension.fieldName
      def res = [name: fieldExtension.fieldName, label: label, type: type, custom: true, recordID: fieldExtension.id]

      return HttpResponse.status(HttpStatus.OK).body(Holders.objectMapper.writeValueAsString(res))
    }
  }

  /**
   * Deletes the custom field definition and removes it from the field order.
   *
   * <h3>Body</h3>
   * The request body is a JSON object with these elements:
   * <ul>
   *   <li><b>id</b> - The ID of the FieldExtension record to delete.
   *   </li>
   * </ul>
   *
   * @param request The request.
   * @param principal
   * @return Status only.
   */
  @Post("/deleteField")
  @Produces(MediaType.APPLICATION_JSON)
  HttpResponse deleteField(HttpRequest request, @Nullable Principal principal) {
    String body = request.body.get()
    log.debug("deleteField() body {}", body)
    def map = Holders.objectMapper.readValue(body, Map)
    String id = map.id
    map.remove('id')

    ArgumentUtils.checkMissing(id, 'id')
    def count = extensionService.deleteField(id)
    if (count == 0) {
      //error.105.message=Could not delete record {0}, record not found.
      throw new BusinessException(105, [id])
    }
    return HttpResponse.status(HttpStatus.OK)
  }

}
