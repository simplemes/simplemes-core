/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search.page

import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.test.page.AbstractPage
import org.simplemes.eframe.test.page.ButtonModule
import org.simplemes.eframe.test.page.ReadOnlyFieldModule

/**
 * Defines the GEB page elements for the Search Index page.
 *
 */
@SuppressWarnings(["GroovyUnusedDeclaration"])
class SearchAdminPage extends AbstractPage {
  static url = "/search/admin"
  static at = {
    title.contains(GlobalUtils.lookup('searchAdmin.title'))
  }

  static content = {
    status { module(new ReadOnlyFieldModule(field: 'status')) }
    pendingRequests { module(new ReadOnlyFieldModule(field: 'pendingRequests')) }
    finishedRequests { module(new ReadOnlyFieldModule(field: 'finishedRequests')) }
    failedRequests { module(new ReadOnlyFieldModule(field: 'failedRequests')) }

    bulkIndexSection(required: false) { $('div.webix_el_label', view_id: 'bulkIndexStatus').parent() }

    bulkIndexStatus { module(new ReadOnlyFieldModule(field: 'bulkIndexStatus')) }
    totalBulkRequests { module(new ReadOnlyFieldModule(field: 'totalBulkRequests')) }
    pendingBulkRequests { module(new ReadOnlyFieldModule(field: 'pendingBulkRequests')) }
    finishedBulkRequests { module(new ReadOnlyFieldModule(field: 'finishedBulkRequests')) }
    bulkIndexErrorCount { module(new ReadOnlyFieldModule(field: 'bulkIndexErrorCount')) }

    resetButton { module(new ButtonModule(id: 'searchResetCounters')) }
    rebuildAllButton { module(new ButtonModule(id: 'searchRebuildIndices')) }

  }


}

