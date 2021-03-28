package org.simplemes.eframe.custom.domain.page

import org.simplemes.eframe.test.page.AbstractPage
import org.simplemes.eframe.test.page.CrudListModule

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the GEB page elements for the domain object's CRUD page used for GUI-level testing.
 *
 */
@SuppressWarnings("GroovyUnusedDeclaration")
class FlexTypeCrudPage extends AbstractPage {
  static url = "/flexType"
  static at = { title.contains(lookup("label.flexType")) }

  /**
   * Must wait for Ajax.
   */
  @Override
  boolean getWaitForAjaxOnLoad() {
    return true
  }

  static content = {
    crudList { module(new CrudListModule()) }
  }

}

