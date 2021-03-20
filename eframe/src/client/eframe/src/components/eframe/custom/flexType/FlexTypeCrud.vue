<template>
  <CrudTable :columns="columns" :service="service"/>
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
        {field: 'flexType', header: this.$t('label.flexType')},
        {field: 'category', header: this.$t('label.category')},
        {field: 'title', header: this.$t('label.title')},
        {field: 'fieldSummary', header: this.$t('label.fields')},
      ],
      service: FlexTypeService
    }
  },
}

</script>

