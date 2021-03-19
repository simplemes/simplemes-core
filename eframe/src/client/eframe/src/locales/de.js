/*
 * Copyright (c) Michael Houston 2021. All rights reserved.
 */

/**
 * Provides the getMessages() function to define the strings for a given locale.
 */
export default {
  getLocale() {
    return 'en'
  },
  getMessages() {
    return {
      label: {
        category: 'Kategorie',
        fields: 'Felder',
        flexType: 'Flex Typ',
        title: 'Titel',
        searchStatusGreen: 'Grün',
      }

    }

  }
}