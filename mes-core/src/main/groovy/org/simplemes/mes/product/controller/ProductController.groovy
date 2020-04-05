package org.simplemes.mes.product.controller

import groovy.util.logging.Slf4j
import io.micronaut.http.annotation.Controller
import io.micronaut.security.annotation.Secured
import org.simplemes.eframe.controller.BaseCrudRestController
import org.simplemes.eframe.web.task.TaskMenuItem

/**
 * Handles HTTP requests for the Product object.
 *
 * <h3>Logging</h3>
 * The logging for this class that can be enabled:
 * <ul>
 *   <li><b>debug</b> - Debugging information. Typically includes inputs and outputs. </li>
 * </ul>
 *
 */
@Slf4j
@Secured("ENGINEER")
@Controller("/product")
class ProductController extends BaseCrudRestController {

  /**
   * Defines the entry(s) in the main Task Menu.
   */
  @SuppressWarnings("unused")
  def taskMenuItems = [new TaskMenuItem(folder: 'productDef:600', name: 'product', uri: '/product', displayOrder: 610)]

}
