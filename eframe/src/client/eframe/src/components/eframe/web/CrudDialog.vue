A standard dialog for performing CRUD-style maintenance on single domain records.  Suitable for use in CRUDTable components.

<template>
  <Dialog v-model:visible="dialogVisible" :breakpoints="{'960px': '95vw', '640px': '100vw'}" :style="{width: '80vw'}"
          :header="mode=='add' ? $t('title.add') : $t('title.edit')" :modal="true" :maximizable="true">
    <div class="p-fluid p-formgrid p-grid p-ai-center">
      <StandardField v-for="field in fields.top" :key="field.fieldName" :field="field" :record="record"/>
      <div class="p-col-12"></div>
      <StandardField v-for="field in fields.bottom" :key="field.fieldName" :field="field" :record="record"/>
    </div>

    <template #footer>
      <Button icon="pi pi-times" :label="$t('label.cancel')" class="p-button-text" @click="cancelDialog"/>
      <Button icon="pi pi-check" :label="$t('label.save')" class="p-button-text" @click="saveDialog"/>
    </template>
  </Dialog>
</template>

<script>

import Button from 'primevue/button'
import Dialog from 'primevue/dialog'

import DomainService from "@/components/eframe/domain/DomainService"
import StandardField from "@/components/eframe/domain/StandardField"


export default {
  name: 'CrudDialog',
  components: {
    StandardField, Button, Dialog,
  },
  props: {
    service: Object,
    domainClassName: {
      type: String,
      required: true
    },
  },

  data() {
    return {
      record: {},
      dialogVisible: false,
      fields: {},
    }
  },
  methods: {
    cancelDialog() {
      this.dialogVisible = false
    },
    openDialog(recordValue) {
      this.dialogVisible = true
      this.$data.record = recordValue
    },
    saveDialog() {
      console.log("saving: " + JSON.stringify(this.$data.record));
      this.dialogVisible = false
    },
  },
  computed: {
    mode: {
      get() {
        if (this.$data.record) {
          return this.$data.record.uuid ? 'edit' : 'add'
        }
        return 'add'
      }
    }
  },
  created() {
    // Load the fields needed for the dialog.
    DomainService.getDisplayFields(this.domainClassName, (data) => {
      this.fields = data
      //console.log("DomainService data: "+JSON.stringify(data));
    });
  },
}


</script>

