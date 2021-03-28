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
        category: 'Category',
        fields: 'Fields',
        flexType: 'Flex Type',
        title: 'Title',
        searchStatusGreen: 'Green',

        _notInOtherLanguages: 'Not',  // Do not add to the other languages.

      }
    }
  }
}