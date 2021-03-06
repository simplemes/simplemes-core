A standard dialog for performing CRUD-style maintenance on single domain records.  Suitable for use in CRUDTable components.

<!--
  - Copyright (c) Michael Houston 2021. All rights reserved.
  -->

<template>
  <Dialog v-model:visible="dialogVisible" :breakpoints="{'960px': '95vw', '640px': '100vw'}" :style="{width: '90vw'}"
          :header="mode=='add' ? $t('title.add') : $t('title.edit')" :modal="true" :maximizable="true"
          :autoZIndex="true" :baseZIndex="90">
    <div class="p-fluid p-formgrid p-grid p-ai-center">
      <StandardField v-for="field in fields.top" :key="field.fieldName" :field="field" :record="record" ref="keyField"/>
      <div class="p-col-12"></div>
      <StandardField v-for="field in fields.bottom" :key="field.fieldName" :field="field" :record="record"/>
      <TabView v-if="fields.tabs.length>0" class="p-col-12">
        <TabPanel v-for="tab in fields.tabs" :header="$t(tab.tabLabel)" :key="tab.tab">
          <div class="p-fluid p-formgrid p-grid p-ai-center">
            <StandardField v-for="field in tab.fields" :key="field.fieldName" :field="field" :record="record"/>
          </div>
        </TabPanel>
      </TabView>
    </div>

    <template #footer>
      <Button icon="pi pi-times" :label="$t('label.cancel')" id="CancelButton" class="p-button-text"
              @click="cancelDialog"/>
      <Button icon="pi pi-check" :label="$t('label.save')" id="SaveButton" class="p-button-text" @click="saveDialog"/>
    </template>
  </Dialog>
</template>

<script>

import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import TabView from 'primevue/tabview'
import TabPanel from 'primevue/tabpanel'

import DomainService from "../domain/DomainService"
import ServiceUtils from "../domain/ServiceUtils"
import StandardField from "./StandardField"


export default {
  name: 'CrudDialog',
  components: {
    StandardField, Button, Dialog, TabView, TabPanel,
  },
  props: {
    domainClassName: {
      type: String,
      required: true
    },
    service: {
      type: Object,
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
      fixMissingChildLists(this.$data.record, this.$data.fields)
      ServiceUtils.fixFieldTypesForEdit(this.$data.record, this.$data.fields)
      let primaryKeyField = this.$data.fields.top[0].fieldName
      setTimeout(function () {
        const element = document.getElementById(primaryKeyField)
        if (element) {
          element.focus()
        }
      }, 50)

    },
    saveDialog() {
      //console.log("saving: " + JSON.stringify(this.$data.record) + " with "+this.service);
      this.service.save(this.$data.record, this.$data.fields, () => {
        this.dialogVisible = false
        this.$emit('updatedRecord', {record: this.$data.record})
        const s = this.$t('message.saved', {record: this.service.buildLabel(this.$data.record, true)})

        this.$toast.add({severity: 'success', summary: this.$t('title.saved'), detail: s, life: 9000})
      })
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
  emits: {
    updatedRecord: null
  },
  created() {
    // Load the fields needed for the dialog.
    DomainService.getDisplayFields(this.domainClassName, (data) => {
      this.fields = data
      //console.log("DomainService data: "+JSON.stringify(data));
    });
  },
}

/**
 * Makes sure the record to be edited has an empty array for all child lists that are undefined (missing).
 * @param theRecord The record for the dialog.
 * @param fieldDefs The fields.
 */
function fixMissingChildLists(theRecord, fieldDefs) {
  let fields = DomainService._flattenFieldList(fieldDefs)
  for (let field of fields) {
    if (field.fieldFormat == DomainService.fieldFormats.CHILD_LIST) {
      if (!theRecord[field.fieldName]) {
        theRecord[field.fieldName] = []
      }
    }
  }
}


</script>

