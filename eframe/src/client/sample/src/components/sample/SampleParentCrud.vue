<template>
  <CrudTable :columns="columns" :service="service" :domainClassName="domainClassName"/>
</template>

<script>

import CrudTable from '@/eframe-lib/web/CrudTable';
import SampleParentService from '@/components/sample/SampleParentService'

export default {
  components: {
    CrudTable
  },
  data() {
    return {
      columns: [
        {field: 'name', header: this.$t('label.name'), sort: true},
        {field: 'title', header: this.$t('label.title'), sort: true},
      ],
      service: SampleParentService,
      domainClassName: 'sample.domain.SampleParent',
    }
  },
}

</script>

