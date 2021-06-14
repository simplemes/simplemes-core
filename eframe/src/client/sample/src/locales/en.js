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
        allFieldsDomain: 'All Fields',
        allFieldsDomains: 'All Fields',
        dueDate: 'Due',
        moreNotes: 'More Notes',
        order: 'Order',
        reportTimeInterval: 'Interval',
        sampleChildren: 'Children',
      },
    }
  }
}