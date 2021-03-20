/**
 * Standard page services.  Helps with common layout/error handling and other common behaviors.
 */
'use strict';

module.exports = function (component) {
  const topComponent = component;


  /**
   * The top vue component.
   */
  this.vue = topComponent

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

    topComponent.$toast.add({severity: 'error', summary: 'Metric', detail: s, life: 3000})
  }

}

