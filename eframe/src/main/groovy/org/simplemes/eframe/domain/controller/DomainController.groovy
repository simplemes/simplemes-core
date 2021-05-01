package org.simplemes.eframe.domain.controller

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.controller.BaseController

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
  @Secured(SecurityRule.IS_ANONYMOUS)
  @Produces(MediaType.APPLICATION_JSON)
  @Get("/displayFields")
  HttpResponse displayFields(HttpRequest request) {
    def res = Holders.objectMapper.writeValueAsString(record)
    println "res = $res"
    return HttpResponse.status(HttpStatus.OK).body(res)
  }
  /*
    // top is always given (key fields).
    // Either bottom or tabs is given.

    top: [
      {
        fieldName: 'name',
        fieldLabel: 'label.name',
        fieldFormat: 'S',
        fieldDefault: '',
        maxLength: 20,
      },
    ],
    bottom: [
      {
        fieldName: 'name',
        fieldLabel: 'label.name',
        fieldFormat: 'S',
        fieldDefault: '',
        maxLength: 20,
      },
    ],
    tabs: [
      {
        tab: 'MAIN',
        tabLabel: 'label.main',
        fields: [
          fieldName: 'name',
          fieldLabel: 'label.name',
          fieldFormat: 'S',
          fieldDefault: '',
          maxLength: 20,
        ]
      },
      {
        tab: 'DETAILS',
        tabLabel: 'Details',
        fields: [
          fieldName: 'name',
          fieldLabel: 'Name',
          fieldFormat: 'S',
          fieldDefault: ''
          maxLength: 20,
        ]
      }
    ]

   */

}
