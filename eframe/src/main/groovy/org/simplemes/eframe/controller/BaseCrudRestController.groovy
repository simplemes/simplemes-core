/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.controller

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.exception.MessageHolder
import org.simplemes.eframe.exception.ValidationException
import org.simplemes.eframe.security.SecurityUtils

import javax.annotation.Nullable
import java.security.Principal

/**
 * Defines common JSON/REST API behavior for controllers.  This provides these endpoints for the controller:
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{domain}/crud/id <b>(GET)</b> - Read a single record by ID or primary key.</li>
 *   <li>{domain}/crud (<b>POST)</b> - Create single record.</li>
 *   <li>{domain}/crud/id <b>(PUT)</b> - Update a single record.</li>
 *   <li>{domain}/crud/id <b>(DELETE)</b> - Delete a single record.</li>
 * </ul>
 * CRUD UI access is via the {@link BaseCrudController}
 */
@Slf4j
abstract class BaseCrudRestController extends BaseCrudController {

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
          o.delete(flush: true)
          log.debug('restDelete() related {} id = {}', o.class.simpleName, (Object) o.id)
        }
        // Delete after the related records in case of a referential integrity check.
        record.delete(flush: true)
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
    _domain.withTransaction {
      def record = Holders.objectMapper.readValue(body, _domain)
      try {
        record.save()
        def s = Holders.objectMapper.writeValueAsString(record)
        res = HttpResponse.status(HttpStatus.OK).body(s)
      } catch (ValidationException e) {
        def msg = new MessageHolder(e.errors)
        def s = Holders.objectMapper.writeValueAsString(msg)
        res = HttpResponse.status(HttpStatus.BAD_REQUEST).body(s)
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

    _domain.withTransaction {
      def record = DomainUtils.instance.findDomainRecord(_domain, x)
      if (record) {
        Holders.objectMapper.readerForUpdating(record).readValue(body)
        DomainUtils.instance.fixChildParentReferences(record)
        if (!record.validate()) {
          // Failed, so return error messages.
          def msg = DomainUtils.instance.getValidationMessages(record)
          //println "map = $map"
          def s = Holders.objectMapper.writeValueAsString(msg)
          res = HttpResponse.status(HttpStatus.BAD_REQUEST).body(s)
        } else {
          record.save()
          def s = Holders.objectMapper.writeValueAsString(record)
          res = HttpResponse.status(HttpStatus.OK).body(s)
        }
      }
    }
    log.debug('restGet() id = {}, res = {}', x, res)
    if (res) {
      return res
    } else {
      return HttpResponse.status(HttpStatus.NOT_FOUND)
    }
  }

}
