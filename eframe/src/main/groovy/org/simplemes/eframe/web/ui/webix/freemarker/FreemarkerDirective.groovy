package org.simplemes.eframe.web.ui.webix.freemarker

import freemarker.core.Environment
import freemarker.template.TemplateDirectiveBody
import freemarker.template.TemplateDirectiveModel
import freemarker.template.TemplateException
import freemarker.template.TemplateModel

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The main entry point for freemarker directives.  This is a thread-safe entry-point for the marker directives.
 * It will create a new marker instance for the given marker class and execute it.
 */
class FreemarkerDirective implements TemplateDirectiveModel {

  /**
   * The marker implementation class.
   */
  Class<MarkerInterface> markerClass

  /**
   * Basic constructor.
   * @param markerClass The marker class that handles the freemarker directive.
   */
  FreemarkerDirective(Class markerClass) {
    this.markerClass = markerClass
  }

  /**
   * Executes this user-defined directive; called by FreeMarker when the user-defined
   * directive is called in the template.
   *
   */
  @Override
  void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body) throws TemplateException, IOException {
    def o = markerClass.newInstance()
    o.setValues(env, params, loopVars, body)
    o.execute()
  }
}
