/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security.controller

import groovy.util.logging.Slf4j
import io.micronaut.http.annotation.Controller
import io.micronaut.security.annotation.Secured
import org.simplemes.eframe.controller.BaseCrudController
import org.simplemes.eframe.domain.validate.ValidationError
import org.simplemes.eframe.exception.ValidationException
import org.simplemes.eframe.web.task.TaskMenuItem

/**
 * Handles the controller actions for the User objects.  Includes CRUD actions on the user.
 */
@Slf4j
@Secured("ADMIN")
@Controller("/user")
class UserController extends BaseCrudController {
  /**
   * Defines the entry(s) in the main Task Menu.
   */
  @SuppressWarnings("unused")
  def taskMenuItems = [new TaskMenuItem(folder: 'admin:7000', name: 'user', uri: '/user', displayOrder: 7050, clientRootActivity: true)]

  /**
   * An overridable method that can do additional binding from HTTP params to the domain object.
   * @param record The domain record.
   * @param params The HTTP parameters/body (as a Map).
   */
  @Override
  void bindEvent(Object record, Object params) {
    if (params._pwNew) {
      if (params._pwNew != params._pwConfirm) {
        //error.207.message=Password match fail on {1}
        throw new ValidationException([new ValidationError(207, 'password', record.userName)], record)
      } else {
        record.password = params._pwNew
      }
    }
  }
}