package sample.page

import org.simplemes.eframe.test.page.AbstractCrudPage
import org.simplemes.eframe.test.page.BooleanFieldModule
import org.simplemes.eframe.test.page.ComboboxModule
import org.simplemes.eframe.test.page.CrudListModule
import org.simplemes.eframe.test.page.DateFieldModule
import org.simplemes.eframe.test.page.PanelModule
import org.simplemes.eframe.test.page.TextFieldModule

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
class AllFieldsDomainCrudPage extends AbstractCrudPage {
  static url = "/allFieldsDomain"
  static at = { title.contains("All Fields Domain") }

  /**
   * Must wait for Ajax.
   */
  @Override
  boolean getWaitForAjaxOnLoad() {
    return true
  }

  static content = {
    crudList { module(new CrudListModule()) }

    // The Create/Edit dialog elements.
    mainPanel { module(new PanelModule(index: 0)) }
    detailsPanel { module(new PanelModule(index: 1)) }

    // Main panel fields
    name { module(new TextFieldModule(field: 'name')) }
    titleField { module(new TextFieldModule(field: 'title')) }
    qty { module(new TextFieldModule(field: 'qty')) }
    count { module(new TextFieldModule(field: 'count')) }
    enabled { module(new BooleanFieldModule(field: 'enabled')) }
    dueDate { module(new DateFieldModule(field: 'dueDate')) }
    dateTime { module(new DateFieldModule(field: 'dateTime')) }

    // Details panel fields
    notes { module(new TextFieldModule(field: 'notes')) }
    transientField(required: false) { module(new TextFieldModule(field: 'transientField')) }
    reportTimeInterval { module(new ComboboxModule(field: 'reportTimeInterval')) }
    order { module(new ComboboxModule(field: 'order')) }
    status { module(new ComboboxModule(field: 'status')) }

  }

}

