/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test.page
/**
 * The general base class for all CRUD list pages tested in the framework.
 * Adds the common elements from the normal CRUD page, including dialog elements.
 */
@SuppressWarnings("GroovyUnusedDeclaration")
class AbstractCrudPage extends AbstractPage {

  /**
   * The page content available for this page.  
   */
  static content = {
    saveButton { $('button#SaveButton') }
    cancelButton { $('button#CancelButton') }
  }

}
