package org.simplemes.eframe.test

import groovy.transform.ToString
import io.micronaut.core.io.Writable
import io.micronaut.views.ViewsRenderer
import io.micronaut.views.freemarker.FreemarkerViewsRenderer

import javax.annotation.Nonnull
import javax.annotation.Nullable

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Builds a very simple mock/stub for the view renderer.  Defines a render() mock
 * method records the view/model used to call renderer.
 * This class restores the original instance during BaseSpecification.cleanup().
 * <pre>
 *   def mock = new MockRenderer(this).install()
 *   . . .
 *   mock.view == 'order/show'
 * </pre>
 * <p>
 * This will create a new instance of the Renderer and register it in a mock Bean context
 * using {@link MockBean}.  This mock instance has a model and view property that was used by the laster
 * render() call.
 */
@ToString(includePackage = false, includeNames = true)
class MockRenderer implements ViewsRenderer {

  /**
   * The test specification.
   */
  def baseSpec

  /**
   * The view name passed to the last call to the render method.
   */
  String view

  /**
   * The model passed to the last call to the render method.
   */
  Object model

  /**
   * Basic constructor - implementation argument version.
   *
   * @param baseSpec The test specification that needs the mock (usually this).
   */
  MockRenderer(BaseSpecification baseSpec) {
    //super({'abc'} as ViewsConfiguration, null, null)
    this.baseSpec = baseSpec
  }

  /**
   * Installs the mock.
   */
  MockRenderer install() {
    // Add this to a fake bean context.
    new MockBean(baseSpec, FreemarkerViewsRenderer, this).install()
    return this
  }

  /**
   * @param viewName view name to be render
   * @param data response body to render it with a view
   * @return A writable where the view will be written to.
   */
  @Override
  Writable render(@Nonnull String viewName, @Nullable Object data) {
    view = viewName
    model = data
    return null
  }

  /**
   * @param viewName view name to be render
   * @return true if a template can be found for the supplied view name.
   */
  @Override
  boolean exists(@Nonnull String viewName) {
    return false
  }
}
