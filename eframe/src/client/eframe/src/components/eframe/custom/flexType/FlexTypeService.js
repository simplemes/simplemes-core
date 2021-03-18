import InMemoriam from 'in-memoriam';

const cache = new InMemoriam(50, 60000);

export default {
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
  },
  // List for crud-style pages.
  list(options, successFunction) {
    const url = '/flexType/list';

    window.$page.vue.axios.get(url, {params: options}).then((response) => {
      if (successFunction) {
        successFunction(response.data)
      }
    }).catch((error) => {
      window.$page.handleError(error, url)
    })
  },
};