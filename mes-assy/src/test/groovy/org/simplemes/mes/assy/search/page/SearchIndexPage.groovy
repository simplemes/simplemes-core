/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.mes.assy.search.page

import org.simplemes.eframe.test.page.AbstractPage
import org.simplemes.eframe.test.page.ButtonModule

/**
 * Defines the GEB page elements for the Search Index page.  Cloned from eframe.
 *
 */
@SuppressWarnings("GroovyUnusedDeclaration")
class SearchIndexPage extends AbstractPage {
  static url = "/search"
  static at = { title.contains(lookup('search.title')) }

  static content = {
    searchField { $("#query") }
    searchButton { module(new ButtonModule(id: 'searchButton')) }
    searchResultsHeader(required: false) { $('div.search-result-header') }
    searchResultsFooter(required: false) { $('div.search-result-footer') }
    searchResults(required: false) { $('div.search-result-single') }

    pagination { $('#pagination') }
    pagerButtons { $('a.pager-button') }
  }


}

