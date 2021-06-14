/*
 * Copyright (c) Michael Houston 2021. All rights reserved.
 */

/**
 * Provides the getLocales() function to define all supported locales for this client.
 * Supports multiple source locales.  Combines the source locales messages into one locale.
 */

let locales = []

import {mergeDeep} from "./MergeDeep"

export default {
  /**
   * Add a single locale to the list.
   * @param locale
   */
  addLocale(locale) {
    locales[locales.length] = locale
  },
  getLocales() {
    let combined = {}
    for (let locale of locales) {
      let list = locale.getLocales()
      combined = mergeDeep(combined, list)
    }

    return combined
  },
}