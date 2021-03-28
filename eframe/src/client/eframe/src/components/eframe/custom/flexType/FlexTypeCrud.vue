<template>
  <CrudTable :columns="columns" :service="service" storageKey="flexTypeCrudList"/>
</template>

<script>

import CrudTable from '@/components/eframe/web/CrudTable';
import FlexTypeService from '@/components/eframe/custom/flexType/FlexTypeService'

export default {
  components: {
    CrudTable
  },
  data() {
    return {
      columns: [
        {field: 'flexType', header: this.$t('label.flexType'), sort: true},
        {field: 'category', header: this.$t('label.category'), sort: true},
        {field: 'title', header: this.$t('label.title'), sort: true},
        {field: 'fieldSummary', header: this.$t('label.fields')},
      ],
      service: FlexTypeService
    }
  },
}

</script>

