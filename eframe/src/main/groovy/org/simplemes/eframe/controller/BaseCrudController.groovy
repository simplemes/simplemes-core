/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.controller

import groovy.util.logging.Slf4j
import io.micronaut.core.io.Writable
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.views.ViewsRenderer
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.domain.DomainBinder
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.exception.MessageHolder
import org.simplemes.eframe.exception.ValidationException
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.search.SearchHit
import org.simplemes.eframe.search.service.SearchService
import org.simplemes.eframe.security.SecurityUtils

import javax.annotation.Nullable
import java.security.Principal

/**
 * A base class for normal CRUD GUI controller actions such as index, list, edit, etc.
 */
@Slf4j
abstract class BaseCrudController extends BaseController {

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
  HttpResponse index(@Nullable Principal principal) {
    def res = SecurityUtils.instance.checkRoleFromSubClass(this, principal)
    if (res) {
      return res
    }
    def modelAndView = new StandardModelAndView(getView('index'), principal, this)
    log.debug('index(): {}', modelAndView)
    def renderer = Holders.applicationContext.getBean(ViewsRenderer)
    Writable writable = renderer.render(modelAndView.view.get(), modelAndView.model.get())
    return HttpResponse.status(HttpStatus.OK).body(writable)
  }

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
    }
    ControllerUtils.instance.delayForTesting('BaseCrudController.list()')
    return HttpResponse.status(HttpStatus.OK).body(json)
  }

  /**
   * Displays the show page.  Requires the view '{domain}/show.hbs' for the given domain.
   * <p>
   * <b>Note:</b> This method secured by a secondary check on the real controllers' @Secured setting.
   * @param id The domain record ID to show.
   * @param principal The user logged in.
   * @return The response.
   */
  @Secured(SecurityRule.IS_ANONYMOUS)
  @Produces(MediaType.TEXT_HTML)
  @Get("/show/{id}")
  HttpResponse show(@PathVariable(name = 'id') String id, @Nullable Principal principal) {
    def res = SecurityUtils.instance.checkRoleFromSubClass(this, principal)
    if (res) {
      return res
    }
    def modelAndView = new StandardModelAndView(getView('show'), principal, this)

    // Store the domain record in the model for the view/markers.
    def domainClass = ControllerUtils.instance.getDomainClass(this)

    domainClass.withTransaction {
      def name = NameUtils.lowercaseFirstLetter(domainClass.simpleName)
      def record = DomainUtils.instance.findDomainRecord(domainClass, id)
      modelAndView.model.get().put(name, record)
    }

    log.debug('show(): {}', modelAndView)
    def renderer = Holders.applicationContext.getBean(ViewsRenderer)
    Writable writable = renderer.render(modelAndView.view.get(), modelAndView.model.get())
    return HttpResponse.status(HttpStatus.OK).body(writable)
  }

  /**
   * Displays the create page.  Requires the view '{domain}/create.hbs' for the given domain.
   * <p>
   * <b>Note:</b> This method secured by a secondary check on the real controllers' @Secured setting.
   * @param principal The user logged in.
   * @return The response.
   */
  @Secured(SecurityRule.IS_ANONYMOUS)
  @Produces(MediaType.TEXT_HTML)
  @Get("/create")
  HttpResponse create(@Nullable Principal principal) {
    def res = SecurityUtils.instance.checkRoleFromSubClass(this, principal)
    if (res) {
      return res
    }
    def modelAndView = new StandardModelAndView(getView('create'), principal, this)

    // Store the domain record in the model for the view/markers.
    def domainClass = ControllerUtils.instance.getDomainClass(this)

    def name = NameUtils.lowercaseFirstLetter(domainClass.simpleName)
    modelAndView.model.get().put(name, domainClass.getConstructor().newInstance())

    log.debug('create(): {}', modelAndView)
    def renderer = Holders.applicationContext.getBean(ViewsRenderer)
    Writable writable = renderer.render(modelAndView.view.get(), modelAndView.model.get())
    return HttpResponse.status(HttpStatus.OK).body(writable)
  }


  /**
   * Accepts the save from a create page and either creates the record or re-displays with errors shown.
   * <p>
   * <b>Note:</b> This method can use the configurable test delay.
   * @param request The request.
   * @param bodyParams The form values, as a the body (a Map).
   * @param principal The user logged in.
   * @return The show page for success, the create page for failure.
   */
  @SuppressWarnings(["GroovyAssignabilityCheck", "unused"])
  @Secured(SecurityRule.IS_ANONYMOUS)
  @Produces(MediaType.TEXT_HTML)
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Post("/create")
  HttpResponse createPost(HttpRequest request, @Body bodyParams, @Nullable Principal principal) {
    def res = SecurityUtils.instance.checkRoleFromSubClass(this, principal)
    if (res) {
      return res
    }
    // Attempt to save record created from the request body (form fields).
    def domainClass = ControllerUtils.instance.getDomainClass(this)
    def name = NameUtils.lowercaseFirstLetter(domainClass.simpleName)

    def errors
    def record = domainClass.getConstructor().newInstance()
    domainClass.withTransaction {
      try {
        DomainBinder.build().bind(record, bodyParams, true)
        bindEvent(record, bodyParams)
        log.debug('Creating {}', record)
        record.save()
      } catch (ValidationException e) {
        errors = e.errors
      }
    }

    ControllerUtils.instance.delayForTesting('BaseCrudController.createPost()')

    if (errors) {
      def modelAndView = new StandardModelAndView(getView('create'), principal, this)
      modelAndView.model.get().put(ControllerUtils.MODEL_KEY_DOMAIN_ERRORS, errors)
      modelAndView[StandardModelAndView.MESSAGES] = new MessageHolder(errors)
      def renderer = Holders.applicationContext.getBean(ViewsRenderer)

      // Store the domain record in the model for the view/markers.
      modelAndView.model.get().put(name, record)

      Writable writable = renderer.render(modelAndView.view.get(), modelAndView.model.get())
      return HttpResponse.status(HttpStatus.OK).body(writable)
    } else {
      return HttpResponse.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, "${rootPath}/show/${record.uuid}")
    }
  }

  /**
   * Displays the edit page.  Requires the view '{domain}/create.hbs' for the given domain.
   * <p>
   * <b>Note:</b> This method can use the configurable test delay.
   * @param principal The user logged in.
   * @return The model/view to display.
   */
  @Secured(SecurityRule.IS_ANONYMOUS)
  @Produces(MediaType.TEXT_HTML)
  @Get("/edit/{id}")
  HttpResponse edit(@PathVariable(name = 'id') String id, @Nullable Principal principal) {
    def res = SecurityUtils.instance.checkRoleFromSubClass(this, principal)
    if (res) {
      return res
    }
    def modelAndView = new StandardModelAndView(getView('edit'), principal, this)

    // Store the domain record in the model for the view/markers.
    def domainClass = ControllerUtils.instance.getDomainClass(this)

    domainClass.withTransaction {
      def name = NameUtils.lowercaseFirstLetter(domainClass.simpleName)
      def record = DomainUtils.instance.findDomainRecord(domainClass, id)
      modelAndView.model.get().put(name, record)
    }

    log.debug('edit(): {}', modelAndView)
    def renderer = Holders.applicationContext.getBean(ViewsRenderer)
    Writable writable = renderer.render(modelAndView.view.get(), modelAndView.model.get())
    return HttpResponse.status(HttpStatus.OK).body(writable)
  }

  /**
   * Accepts the save from a edit page and either updates the record or re-displays with errors shown.
   * <p>
   * <b>Note:</b> This method can use the configurable test delay.
   * <p>
   * <b>Note:</b> This method can use the configurable test delay.
   * @param request The request.
   * @param bodyParams The form values, as a the body (a Map).  Should also contain the record ID.
   * @param principal The user logged in.
   * @return The model/view for the good case (show) or fail (create).
   */
  @SuppressWarnings("unused")
  @Secured(SecurityRule.IS_ANONYMOUS)
  @Produces(MediaType.TEXT_HTML)
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Post("/edit")
  HttpResponse editPost(HttpRequest request, @Body bodyParams, @Nullable Principal principal) {
    def res = SecurityUtils.instance.checkRoleFromSubClass(this, principal)
    if (res) {
      return res
    }
    // Attempt to save record created from the request body (form fields).
    def domainClass = ControllerUtils.instance.getDomainClass(this)
    def name = NameUtils.lowercaseFirstLetter(domainClass.simpleName)
    def id = bodyParams.id

    def errors
    def record = DomainUtils.instance.findDomainRecord(domainClass, id)
    domainClass.withTransaction {
      try {
        DomainBinder.build().bind(record, (Map) bodyParams, true)
        bindEvent(record, bodyParams)
        log.debug('Updating {}', record)
        record.save()
      } catch (ValidationException e) {
        errors = e.errors
      }
    }

    ControllerUtils.instance.delayForTesting('BaseCrudController.editPost()')

    if (errors) {
      def modelAndView = new StandardModelAndView(getView('edit'), principal, this)
      modelAndView.model.get().put(ControllerUtils.MODEL_KEY_DOMAIN_ERRORS, errors)
      modelAndView[StandardModelAndView.MESSAGES] = new MessageHolder(errors)
      def renderer = Holders.applicationContext.getBean(ViewsRenderer)

      // Store the domain record in the model for the view/markers.
      modelAndView.model.get().put(name, record)

      Writable writable = renderer.render(modelAndView.view.get(), modelAndView.model.get())
      return HttpResponse.status(HttpStatus.OK).body(writable)
    } else {
      return HttpResponse.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, "$rootPath/show/${record.uuid}")
    }
  }


  /**
   * Deletes a single top-level domain object.
   * <p>
   * <b>Note:</b> This method can use the configurable test delay.
   * <p>
   * <b>Note:</b> This method secured by a secondary check on the real controllers' @Secured setting.
   * @param request The request.
   * @param bodyParams The form values, as a the body (a Map).  Should also contain the record ID.
   * @param principal The user logged in.
   * @return The model/view for the good case (list page) or fail (show page).
   */
  @Secured(SecurityRule.IS_ANONYMOUS)
  @SuppressWarnings("unused")
  @Produces(MediaType.TEXT_HTML)
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Post("/delete")
  HttpResponse delete(HttpRequest request, @Body bodyParams, @Nullable Principal principal) {
    def res = SecurityUtils.instance.checkRoleFromSubClass(this, principal)
    if (res) {
      return res
    }

    // Attempt to delete record from the body parameter id.
    def domainClass = ControllerUtils.instance.getDomainClass(this)
    def name = NameUtils.lowercaseFirstLetter(domainClass.simpleName)
    def id = (String) bodyParams.id

    def error = false
    def record = null
    domainClass.withTransaction {
      record = DomainUtils.instance.findDomainRecord(domainClass, id)
      if (record) {
        // Now delete any related records (not true child records)
        for (o in DomainUtils.instance.findRelatedRecords(record)) {
          o.delete()
          log.debug('delete() related {} id = {}', o.class.simpleName, (Object) o.uuid)
        }
        // Delete after the related records in case of a referential integrity check.
        record.delete()
        log.debug('delete() {} id = {}', record.class.simpleName, id)
      } else {
        error = true
      }
    }
    ControllerUtils.instance.delayForTesting('BaseCrudController.delete()')

    if (error) {
      //error.105.message=Could not delete record {0}, record not found.
      def msg = GlobalUtils.lookup('error.105.message', null, id)
      return HttpResponse.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, "/$name?_error=$msg")
    } else {
      //deleted.message=Deleted {0} "{1}".
      def domainName = GlobalUtils.lookup("${NameUtils.lowercaseFirstLetter(domain.simpleName)}.label")
      //def s = TextUtils.escape(TypeUtils.toShortString(record))
      def s = TypeUtils.toShortString(record)
      def msg = URLEncoder.encode(GlobalUtils.lookup('deleted.message', null, domainName, s), "UTF-8")
      return HttpResponse.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, "$rootPath?_info=$msg")
    }
  }

  /**
   * An overridable method that can do additional binding from HTTP params to the domain object.
   * @param record The domain record.
   * @param params The HTTP parameters/body (as a Map).
   */
  void bindEvent(Object record, params) {
  }

  /**
   * Determines the view to display for the given method.  This can be overridden in your controller class to
   * use a different naming scheme.<p>
   * This default naming scheme uses the method name and domain name to make the path.  For example, the User index()
   * method will use the view 'user/index'.
   * @param methodName The method that needs the view (e.g. 'index').
   * @return The resulting view path.
   */
  String getView(String methodName) {
    Class clazz = getDomain()
    return "${NameUtils.toDomainName(clazz)}/$methodName"
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

}
