import InMemoriam from 'in-memoriam';

const cache = new InMemoriam(50, 60000);

export default {
  buildLabel(record, includeType = false) {
    // Builds the Label for the given object.  If
    let s = record.flexType
    if (includeType) {
      s = window.$page.vue.$t('label.flexType') + ' ' + s
    }
                  // TODO: Move boiler plate to common file.  See https://github.com/primefaces/primevue/blob/master/src/components/utils/ObjectUtils.js
    return s
  },
  find(uuid, successFunction) {
    if (cache.get(uuid)) {
      successFunction(cache.get(uuid))
      console.log("cache: " + JSON.stringify(cache.stats));
      return
    }
    const url = '/flexType/crud/' + uuid;
    window.$page.vue.axios.get(url).then((response) => {
      successFunction(response.data)
      cache.set(uuid, response.data)
    }).catch((error) => {
      window.$page.handleError(error, url)
    })
  }
  ,
// List for crud-style pages.
  list(options, successFunction, errorFunction) {
    const url = '/flexType/list';

    window.$page.vue.axios.get(url, {params: options}).then((response) => {
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
  ,
// Deletes the record.
  delete(object, successFunction, errorFunction) {
    // Determine if this is a new record or existing.
    const url = '/flexType/crud/' + object.uuid

    window.$page.vue.axios.delete(url, object).then((response) => {
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
  ,
// Updates/creates the record as needed.
  save(object, successFunction, errorFunction) {
    // Determine if this is a new record or existing.
    let url = '/flexType/crud'
    let saveFunction = window.$page.vue.axios.post
    if (object.uuid) {
      url = '/flexType/crud/' + object.uuid
      saveFunction = window.$page.vue.axios.put
    }

    saveFunction(url, object).then((response) => {
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
  ,
}
;