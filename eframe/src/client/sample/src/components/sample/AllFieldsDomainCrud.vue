<template>
  <CrudTable :columns="columns" :service="service" :domainClassName="domainClassName"/>
</template>

<script>

import CrudTable from '@/eframe-lib/web/CrudTable';
import AllFieldsDomainService from '@/components/sample/AllFieldsDomainService'

export default {
  components: {
    CrudTable
  },
  data() {
    return {
      columns: [
        {field: 'name', header: this.$t('label.name'), sort: true},
        {field: 'title', header: this.$t('label.title'), sort: true},
        {field: 'qty', header: this.$t('label.qty'), sort: true},
      ],
      service: AllFieldsDomainService,
      domainClassName: 'sample.domain.AllFieldsDomain',
    }
  },
}

</script>

