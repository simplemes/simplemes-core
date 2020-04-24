package org.simplemes.mes.assy.demand

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * A simple POGO to hold the component report detail rows.  Mostly results from a search request.
 */
class ComponentReportDetail {
  /**
   * The object found by the search hit.  Usuaully the string format.
   */
  Object searchHit

  String _searchHitLink
  String _searchHitText

  /**
   * The date/time the object was last changed.
   */
  Date dateTime


}
