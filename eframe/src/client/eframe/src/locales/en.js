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
        flexType: 'Flex Type',
        searchStatusGreen: 'Green',
      },
      reportTimeIntervalEnum: {
        CUSTOM_RANGE: 'Custom Range',
        LAST_24_HOURS: 'Last 24 Hours',
        LAST_7_DAYS: 'Last 7 Days',
        LAST_30_DAYS: 'Last 30 Days',
        LAST_6_MONTHS: 'Last 6 Months',
        LAST_MONTH: 'Last Month',
        LAST_YEAR: 'Last Year',
        THIS_MONTH: 'This Month',
        THIS_YEAR: 'This Year',
        TODAY: 'Today',
        YESTERDAY: 'Yesterday',
      },
    }
  }
}