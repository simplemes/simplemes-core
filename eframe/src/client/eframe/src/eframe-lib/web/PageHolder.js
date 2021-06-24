/**
 * Standard page services.  Helps with common layout/error handling and other common behaviors.
 */
'use strict';

module.exports = function (component, domainService) {
  const topComponent = component;


  /**
   * The top vue component.
   */
  this.vue = topComponent

  /**
   * Domain Service being used.
   */
  this.domainService = domainService

  /**
   * Handles the common errors.  Supports axios errors.
   * @param error The error.
   * @param context Optional context for the error message.
   */
  this.handleError = function (error, context = '') {
    console.log(error)
    let s = error
    if ('message' in error) {
      s = error.message + ': ' + context
    }
    if (error.response && error.response.data) {
      console.log(error.response.data);
      s = error.response.data.message.text
    }

    topComponent.$toast.add({severity: 'error', summary: topComponent.$t('title.error'), detail: s, life: 9000})
  }

}

