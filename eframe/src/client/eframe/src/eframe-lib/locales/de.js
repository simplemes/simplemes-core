/*
 * Copyright (c) Michael Houston 2021. All rights reserved.
 */

/**
 * Provides the getMessages() function to define the strings for a given locale.
 */
export default {
  getLocale() {
    return 'de'
  },
  getMessages() {
    return {
      label: {
        category: 'Kategorie',
        fields: 'Felder',
        title: 'Titel',
      }

    }

  }
}