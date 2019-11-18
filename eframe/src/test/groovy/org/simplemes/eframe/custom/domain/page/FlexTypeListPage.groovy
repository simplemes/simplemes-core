package org.simplemes.eframe.custom.domain.page

import org.simplemes.eframe.test.page.AbstractPage
import org.simplemes.eframe.test.page.DefinitionListModule

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the GEB page elements for the FlexType list page used for GUI-level testing.
 *
 */
@SuppressWarnings("GroovyUnusedDeclaration")
class FlexTypeListPage extends AbstractPage {
  static url = "flexType/index"
  static at = { title.startsWith("Flex Type List") }

  static content = {
    userGrid { module(new DefinitionListModule(field: 'flexType')) }
  }

}

