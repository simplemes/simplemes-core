/*
 * Copyright (c) Michael Houston 2021. All rights reserved.
 */
import InMemoriam from 'in-memoriam'
import DomainService from "@/eframe-lib/domain/DomainService"

const cache = new InMemoriam(10, 10000)  // Cache the find results for a short time.


// Utilities for most client-side Servce's used by CrudTable and similar elements.
// noinspection JSValidateJSDoc
export default class ServiceUtils {

  /**
   * Builds the display label for confirmation dialogs and related elements.
   * @param keyValue The identifier for the object.  Usually the primary key field(s).
   * @param typeLabel The label to use as a prefix. (e.g. 'label.flexType' will return the localized label such as: 'Flex Type ABC').
   * @returns {string} The label.
   */
  static buildLabel(keyValue, typeLabel) {
    let res = keyValue
    if (typeLabel) {
      res = window.$page.vue.$t(typeLabel) + ' ' + res
    }
    return res
  }

  /**
   * Helper method to request the list of matching records for a normal CrudTable display.
   * @param uri The base uri for the request (e.g. '/flexType').  Must point to a controller that supports the list() method.
   * @param options The sorting/paging/filtering options.
   * @param successFunction Called when the request returns with a success.
   * @param errorFunction  Called when the request returns with an error.  By default, the error is displayed
   *                       using the page's error handler (e.g. Toast popup).
   * @returns {Promise<AxiosResponse<any>>}
   */
  static list(uri, options, successFunction, errorFunction) {
    const url = uri + '/list';

    return window.$page.vue.axios.get(url, {params: options}).then((response) => {
      if (successFunction) {
        successFunction(response.data)
      }
    }).catch((error) => {
      window.$page.handleError(error, url)
      if (errorFunction) {
        errorFunction(error, url)
      }
    })
  }

  /**
   * Helper method to retrieve a single record for the domain.
   * @param uri The base uri for the request (e.g. '/flexType').  Must point to a controller that supports the crud GET method.
   * @param uuid The record's UUID.
   * @param successFunction Called when the request returns with a success.
   * @param errorFunction  Called when the request returns with an error.  By default, the error is displayed
   *                       using the page's error handler (e.g. Toast popup).
   * @returns {Promise<AxiosResponse<any>>}
   */
  static find(uri, uuid, successFunction, errorFunction) {
    if (cache.get(uuid)) {
      successFunction(cache.get(uuid))
      console.log("cacheFind(): " + JSON.stringify(cache.stats));
      return
    }
    const url = uri + '/crud/' + uuid;
    return window.$page.vue.axios.get(url).then((response) => {
      successFunction(response.data)
      cache.set(uuid, response.data)
    }).catch((error) => {
      window.$page.handleError(error, url)
      if (errorFunction) {
        errorFunction(error, url)
      }
    })
  }

  /**
   * Helper method to save a single record for the domain.  Used by normal CrudTable pages.
   * Handles record creation and updates.
   * @param uri The base uri for the request (e.g. '/flexType').  Must point to a controller that supports the crud POST/PUT methods.
   * @param object The record to save.
   * @param fields The fields for this domain object.
   * @param successFunction Called when the request returns with a success.
   * @param errorFunction  Called when the request returns with an error.  By default, the error is displayed
   *                       using the page's error handler (e.g. Toast popup).
   * @returns {Promise<AxiosResponse<any>>}
   */
  static save(uri, object, fields, successFunction, errorFunction) {
    // Determine if this is a new record or existing.
    let url = uri + '/crud'
    var clonedObject = JSON.parse(JSON.stringify(object))
    this.fixFieldTypesForSave(clonedObject, fields)

    let saveFunction = window.$page.vue.axios.post
    if (clonedObject.uuid) {
      url = uri + '/crud/' + clonedObject.uuid
      saveFunction = window.$page.vue.axios.put
    }

    return saveFunction(url, clonedObject).then((response) => {
      if (successFunction) {
        successFunction(response.data)
      }
    }).catch((error) => {
      window.$page.handleError(error, url)
      if (errorFunction) {
        errorFunction(error, url)
      }
    })
  }

  /**
   * Helper method to delete a single record for the domain.  Used by normal CrudTable pages.
   * @param uri The base uri for the request (e.g. '/flexType').  Must point to a controller that supports the crud DELETE method.
   * @param object The record to delete.
   * @param successFunction Called when the request returns with a success.
   * @param errorFunction  Called when the request returns with an error.  By default, the error is displayed
   *                       using the page's error handler (e.g. Toast popup).
   * @returns {Promise<AxiosResponse<any>>}
   */
  static delete(uri, object, successFunction, errorFunction) {
    const url = uri + '/crud/' + object.uuid;

    return window.$page.vue.axios.delete(url, object).then((response) => {
      if (successFunction) {
        successFunction(response.data)
      }
    }).catch((error) => {
      window.$page.handleError(error, url)
      if (errorFunction) {
        errorFunction(error, url)
      }
    })
  }

  /**
   * Fixes the various field formats to match the format needed by the vue-based clients for display/edit.  This includes the
   * conversion of date/time strings to JS objects.
   * @param record The single record.
   * @param fields The flattened field list.
   */
  static fixFieldTypesForEdit(record, fields) {
    const flatFields = DomainService._flattenFieldList(fields)
    for (let field of flatFields) {
      if (field.fieldFormat == DomainService.fieldFormats.DATE_TIME || field.fieldFormat == DomainService.fieldFormats.DATE) {
        if (record[field.fieldName]) {
          record[field.fieldName] = this._parseISODate(record[field.fieldName])
          //console.log(field.fieldName + ": Converted from "+s+" to " + record[field.fieldName]);
        }
      }
    }
  }

  /**
   * Fixes the various field formats to match the format needed server-side saved.  This includes the
   * conversion of date/time strings to JS objects.
   * @param record The single record.
   * @param fields The flattened field list.
   */
  static fixFieldTypesForSave(record, fields) {
    const flatFields = DomainService._flattenFieldList(fields)
    for (let field of flatFields) {
      var value = record[field.fieldName];
      if (field.fieldFormat == DomainService.fieldFormats.DATE_TIME) {
        if ((typeof value == 'string')) {
          try {
            record[field.fieldName] = new Date(value)
          } catch (ignored) {
            console.log(ignored);
          }
        }
      } else if (field.fieldFormat == DomainService.fieldFormats.DATE) {
        if (value) {
          //record[field.fieldName] = this._parseISODate(record[field.fieldName])
          //console.log(field.fieldName + ": Converted for save from "+record[field.fieldName]+ " to "+s);
          record[field.fieldName] = this._formatISODate(value)
        }
      }
    }
  }

  /**
   * Formats the given date into an ISO date.
   *
   * @param date
   * @returns {String} The ISO form of the date.
   */
  static _formatISODate(date) {
    let s = date + ""

    let index = s.indexOf("T")
    if (index) {
      s = s.substr(0, index)
    }
    return s
  }

  /**
   * Parses an ISO date string into a real date.
   *
   * @param s The string.
   * @returns {Date}
   * @private
   */
  static _parseISODate(s) {
    // Convert +0000 to Z for IE parsing
    if (s.length == 10) {
      // Handle date only methods to avoid TZ offset issues with just dates.
      var fields = s.split('-');
      if (fields.length == 3) {
        var month = Number(fields[1]) - 1;
        return new Date(fields[0], month, fields[2]);
      }
    }
    return new Date(s);
  }

}