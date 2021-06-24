/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.controller

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import io.micronaut.http.annotation.Put
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.reactivex.Flowable
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.custom.ExtensibleFieldHelper
import org.simplemes.eframe.custom.FieldHolderMap
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.domain.validate.ValidationError
import org.simplemes.eframe.exception.MessageHolder
import org.simplemes.eframe.exception.ValidationException
import org.simplemes.eframe.search.SearchHit
import org.simplemes.eframe.search.service.SearchService
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.web.asset.WebClientAssetService

import javax.annotation.Nullable
import java.security.Principal

/**
 * Defines common JSON/REST API behavior for controllers.  Also provides access to the client index page needed
 * for the CRUD-style GUI and the list() method.
 * <p/>
 * This provides these endpoints for the controller:
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{domain}/ <b>(GET - index)</b> - Returns the index.html from the /client sub-modules.</li>
 *   <li>{domain}/list <b>(GET)</b> - Lists the domain records.  Supports paging, filtering and sorting.</li>
 *   <li>{domain}/crud/id <b>(GET)</b> - Read a single record by ID or primary key.</li>
 *   <li>{domain}/crud (<b>POST)</b> - Create single record.</li>
 *   <li>{domain}/crud/id <b>(PUT)</b> - Update a single record.</li>
 *   <li>{domain}/crud/id <b>(DELETE)</b> - Delete a single record.</li>
 * </ul>
 * CRUD UI access is via the {@link BaseCrudController}
 */
@Slf4j
abstract class BaseCrudController2 extends BaseController {
  // TODO: Replace BaseCrudController


  WebClientAssetService webClientAssetService

  /**
   * The service that handles retrieves the client content.
   */
  WebClientAssetService getWebClientAssetService() {
    if (!webClientAssetService) {
      webClientAssetService = Holders.getBean(WebClientAssetService)
    }
    return webClientAssetService
  }

  /**
   * Displays the index page.  Requires a view  '{domain}/index' for the given domain.
   * <p>
   * <b>Note:</b> This method secured by a secondary check on the real controllers' @Secured setting.
   * @param principal The user logged in.
   * @return The model/view to display.
   */
  @Secured(SecurityRule.IS_ANONYMOUS)
  @Produces(MediaType.TEXT_HTML)
  @Get("/")
  @ExecuteOn(TaskExecutors.IO)
  Flowable<MutableHttpResponse<?>> index(HttpRequest request, @Nullable Principal principal) {
    def res = SecurityUtils.instance.checkRoleFromSubClass(this, principal)
    if (res) {
      return Flowable.just(res)
    }
    def indexView = getIndexView()

    return getWebClientAssetService().handleAsset(indexView, request, null)
  }

  /**
   * Determines the uri of the index page.  This is provided by the client sub-module, so it generally looks like
   * this: '/client/eframe/flexType/index.html'.
   * @return The resulting view path.
   */
  abstract String getIndexView()

  /**
   * Returns a list (JSON formatted) from the controller's domain for use by the index page.
   * Supports sorting and paging.<p>
   * <b>Note:</b> This method can use the configurable test delay.
   * <p>
   * <b>Note:</b> This method secured by a secondary check on the real controllers' @Secured setting.
   * @param request The request.
   * @param principal The user logged in.
   * @return The data for the list.
   */
  @Secured(SecurityRule.IS_ANONYMOUS)
  @Get("/list")
  HttpResponse list(HttpRequest request, @Nullable Principal principal) {
    def res = SecurityUtils.instance.checkRoleFromSubClass(this, principal)
    if (res) {
      return res
    }
    Class clazz = getDomain()

    // Calculate the criteria for the result set.
    def params = ControllerUtils.instance.convertToMap(request.parameters)
    def (from, max) = ControllerUtils.instance.calculateFromAndSizeForList(params)
    def (sortField, sortDir) = ControllerUtils.instance.calculateSortingForList(params)
    // Use some defaults if no sorting specified.
    sortField = sortField ?: DomainUtils.instance.getPrimaryKeyField(clazz)
    sortDir = sortDir ?: 'asc'
    //def sortDirection = (sortDir == 'asc') ? Sort.Order.Direction.ASC : Sort.Order.Direction.DESC
    def json = null
    def totalCount = 0
    def data = []
    String search = params.search
    log.debug('List(max: {}, from: {}, sort: {}, order: {}, search: {}) : ', max, from, sortField, sortDir, search)
    clazz.withTransaction {
      // Let the search service use the search engine or the DB depending on the configuration.
      def service = Holders.getBean(SearchService)
      def searchResult = service.domainSearch(clazz, search, params)
      totalCount = searchResult.totalHits
      for (SearchHit hit in searchResult.hits) {
        if (hit.object) {
          data << hit.object
        }
      }
      log.debug('list(): {}', data)
      json = Holders.objectMapper.writeValueAsString([data: data, pos: from * max, total_count: totalCount, sort: sortField, sortDir: sortDir])
      log.trace('list()-JSON: {}', json)
    }
    ControllerUtils.instance.delayForTesting('BaseCrudController.list()')
    return HttpResponse.status(HttpStatus.OK).body(json)
  }


  /**
   * Gets a single domain object and returns it as a JSON content.
   * <p>
   * <b>Note:</b> This method secured by a secondary check on the real controllers' @Secured setting.
   * @param x The ID (or key field value) for the record to return.
   * @param principal The user logged in.
   * @return The domain as JSON.
   */
  @SuppressWarnings("unused")
  @Secured(SecurityRule.IS_ANONYMOUS)
  @Produces(MediaType.APPLICATION_JSON)
  @Get("/crud/{x}")
  HttpResponse restGet(@PathVariable(name = 'x') String x, @Nullable Principal principal) {
    def securityRes = SecurityUtils.instance.checkRoleFromSubClass(this, principal)
    if (securityRes) {
      return securityRes
    }
    def _domain = domain
    def res = null
    _domain.withTransaction {
      def record = DomainUtils.instance.findDomainRecord(_domain, x)
      if (record) {
        res = Holders.objectMapper.writeValueAsString(record)
      }
    }
    log.debug('restGet() id = {}, res = {}', x, res)
    if (res) {
      return HttpResponse.status(HttpStatus.OK).body(res)
    } else {
      return HttpResponse.status(HttpStatus.NOT_FOUND)
    }
  }

  /**
   * The domain class used for this controller.
   */
  private Class _domain

  /**
   * Returns the expected domain needed for this controller.
   * @return The domain class.
   */
  Class getDomain() {
    if (!_domain) {
      _domain = ControllerUtils.instance.getDomainClass(this)
      if (!_domain) {
        throw new IllegalArgumentException("Could not determine domain for ${this}")
      }
    }
    //println "_domain = $_domain"

    return _domain
  }

  /**
   * Returns the Root URI path for this controller. This comes directly from the
   * @Controller annotation.* @return The path.
   */
  String getRootPath() {
    ControllerUtils.instance.getRootPath(this.class)
  }

  /**
   * Deletes a single top-level domain object.
   * <p>
   * <b>Note:</b> This method secured by a secondary check on the real controllers' @Secured setting.
   * @param x The ID (or key field value) for the record to delete.
   * @param principal The user logged in.
   * @return NOT_FOUND if record not found.  Otherwise, NO_CONTENT for a good a good delete.
   */
  @SuppressWarnings("unused")
  @Secured(SecurityRule.IS_ANONYMOUS)
  @Delete("/crud/{x}")
  HttpResponse restDelete(@PathVariable(name = 'x') String x, @Nullable Principal principal) {
    def securityRes = SecurityUtils.instance.checkRoleFromSubClass(this, principal)
    if (securityRes) {
      return securityRes
    }
    def _domain = domain
    def res = null
    _domain.withTransaction {
      def record = DomainUtils.instance.findDomainRecord(_domain, x)
      if (record) {
        // Now delete any related records (not true child records)
        for (o in DomainUtils.instance.findRelatedRecords(record)) {
          o.delete()
          log.debug('restDelete() related {} uuid = {}', o.class.simpleName, (Object) o.uuid)
        }
        // Delete after the related records in case of a referential integrity check.
        record.delete()
        res = HttpResponse.status(HttpStatus.NO_CONTENT)
        log.debug('restDelete() id = {}', x)
      }
    }

    if (res) {
      return res
    } else {
      return HttpResponse.status(HttpStatus.NOT_FOUND)
    }
  }

  /**
   * Creates a single top-level domain object.  This returns the created object in JSON so the caller can access the
   * record's ID.
   * <p>
   * <b>Note:</b> This method secured by a secondary check on the real controllers' @Secured setting.
   * @param request The HTTP Request.  The Body is the JSON needed to create the record.
   * @param principal The user logged in.
   * @return The domain as JSON.
   */
  @SuppressWarnings("unused")
  @Secured(SecurityRule.IS_ANONYMOUS)
  @Produces(MediaType.APPLICATION_JSON)
  @Post("/crud")
  HttpResponse restPost(HttpRequest request, @Nullable Principal principal) {
    def securityRes = SecurityUtils.instance.checkRoleFromSubClass(this, principal)
    if (securityRes) {
      return securityRes
    }
    def _domain = domain
    String body = null
    if (request.body) {
      body = request.body.get()
    }

    if (!body) {
      def msg = new MessageHolder(text: "Empty body in request")
      return HttpResponse.status(HttpStatus.BAD_REQUEST).body(Holders.objectMapper.writeValueAsString(msg))
    }

    HttpResponse res = null
    _domain.withTransaction { status ->
      log.trace('restPost() body = "{}"', body)
      def record = Holders.objectMapper.readValue(body, _domain)
      // Force a null UUID to make sure the record is created.
      record.uuid = null
      try {
        record.save()
        def s = Holders.objectMapper.writeValueAsString(record)
        res = HttpResponse.status(HttpStatus.OK).body(s)
      } catch (ValidationException e) {
        def msg = new MessageHolder(e.errors as List<ValidationError>)
        def s = Holders.objectMapper.writeValueAsString(msg)
        res = HttpResponse.status(HttpStatus.BAD_REQUEST).body(s)
        status.setRollbackOnly()
      }
    }
    log.debug('restPost() res = {}', res)
    return res
  }

  /**
   * Updates a single top-level domain object.
   * <p>
   * <b>Note:</b> This method secured by a secondary check on the real controllers' @Secured setting.
   * @param x The ID (or key field value) for the record to return.
   * @param request The HTTP Request.  The Body is the JSON needed to create the record.
   * @param principal The user logged in.
   * @return The domain as JSON.
   */
  @SuppressWarnings("unused")
  @Secured(SecurityRule.IS_ANONYMOUS)
  @Produces(MediaType.APPLICATION_JSON)
  @Put("/crud/{x}")
  HttpResponse restPut(@PathVariable(name = 'x') String x, HttpRequest request, @Nullable Principal principal) {
    def securityRes = SecurityUtils.instance.checkRoleFromSubClass(this, principal)
    if (securityRes) {
      return securityRes
    }
    def _domain = domain
    def res = null
    String body = null
    if (request.body) {
      body = request.body.get()
    }

    if (!body) {
      def msg = new MessageHolder(text: "Empty body in request")
      return HttpResponse.status(HttpStatus.BAD_REQUEST).body(Holders.objectMapper.writeValueAsString(msg))
    }

    _domain.withTransaction { status ->
      def record = DomainUtils.instance.findDomainRecord(_domain, x)
      DomainUtils.instance.loadChildRecords(record)  // Force the child records to be loaded for updates.
      def originalUUID = record?.uuid
      if (record) {
        // Preserve the older custom fields on update.
        def originalCustomFields = null
        def domainClass = record.getClass()
        def hasExtensibleFields = ExtensibleFieldHelper.instance.hasExtensibleFields(domainClass)
        if (hasExtensibleFields) {
          originalCustomFields = ExtensibleFieldHelper.instance.getExtensibleFieldMap(record) ?: new FieldHolderMap()
        }
        Holders.objectMapper.readerForUpdating(record).readValue(body)
        if (hasExtensibleFields) {
          // Record now has the new values from the JSON.  We need to merge it into the original map
          // to preserve the original settings and history.
          def updatedCustomFields = ExtensibleFieldHelper.instance.getExtensibleFieldMap(record)
          if (updatedCustomFields) {
            // Some custom data was provided in the JSON, so merge it
            originalCustomFields.mergeMap(updatedCustomFields, record)
          }
          // And force the updated fields into the the record.
          ExtensibleFieldHelper.instance.setExtensibleFieldMap(record, originalCustomFields)
        }
        // Restore the original UUID in case the JSON changed it.
        record.uuid = originalUUID
        try {
          record.save()
          def s = Holders.objectMapper.writeValueAsString(record)
          res = HttpResponse.status(HttpStatus.OK).body(s)
        } catch (ValidationException e) {
          def msg = new MessageHolder(e.errors as List<ValidationError>)
          def s = Holders.objectMapper.writeValueAsString(msg)
          res = HttpResponse.status(HttpStatus.BAD_REQUEST).body(s)
          status.setRollbackOnly()
        }
      }
    }

    log.debug('restPut() id = {}, res = {}', x, res)
    if (res) {
      return res
    } else {
      return HttpResponse.status(HttpStatus.NOT_FOUND)
    }
  }

}
