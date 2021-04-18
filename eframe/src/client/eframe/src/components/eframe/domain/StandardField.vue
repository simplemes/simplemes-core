StandardField run-time display component.  Supports multiple fieldFormat values.
Should be used inside the div: 'div class="p-fluid p-formgrid p-grid"'


<template>
  <div class="p-col-12">
    <div class="p-field p-grid" v-if="field.fieldFormat==='S'">
      <label :for="field.fieldName" class="p-col-12 p-mb-2 p-md-2 p-mb-md-0">{{ $t(field.fieldLabel) }}</label>
      <div class="p-col-12 " :class="textFieldClass(field)">
        <InputText v-bind:id="field.fieldName" :maxlength="field.maxLength" v-model="value"/>
      </div>
    </div>
    <div class="p-field" v-if="field.fieldFormat==='C'">
      <label :for="field.fieldName">{{ $t(field.fieldLabel) }}</label>
      <InlineGrid :storageKey="field.fieldName" :columns="field.fields"/>
    </div>
    <div class="p-field p-grid" v-if="field.fieldFormat==='B'">
      <label :for="field.fieldName" class="p-col-12 p-mb-2 p-md-2 p-mb-md-0">{{ $t(field.fieldLabel) }}</label>
      <div class="p-col-12 p-md-2">
        <Checkbox v-bind:id="field.fieldName" v-model="checkbox0" :binary="true"/>
      </div>
    </div>
    <div class="p-field " v-if="field.fieldFormat==='I'">
      <InputNumber :id="field.fieldName" v-model="value" locale="en-US" mode="decimal" style="width:14em"
                   :minFractionDigits="0" :maxFractionDigits="0"
                   showButtons buttonLayout="horizontal"
                   decrementButtonClass="p-button-danger" decrementButtonIcon="pi pi-minus"
                   incrementButtonClass="p-button-success" incrementButtonIcon="pi pi-plus"
                   :step="1.0" :placeholder="field.fieldLabel"
      />
    </div>
  </div>
</template>

<script>

import InputText from 'primevue/inputtext';
import InputNumber from 'primevue/inputnumber';
import Checkbox from 'primevue/checkbox';

import InlineGrid from '@/components/eframe/web/InlineGrid';

// InputText supports :placeholder="field.fieldLabel"
/*
Supported types:
☑ String - S
☑ Integer/int/long - I
BigDecimal - N
DateOnly - D
DateTime - T
☑ Boolean/boolean - B
⌧ Long/long - L
Domain Reference - R
List of Refs - Q
List of Children - C
List of Custom Children - K
Enumeration - E
EncodedType - Y
Configurable Type - G
*/

export default {
  data() {
    return {
      index: -1,
      checkbox0: true,
    }
  },
  props: ['field'],
  components: {
    InputText, InputNumber, Checkbox, InlineGrid
  },
  computed: {
    value: {
      get() {
        //console.log(this.field.fieldName+": "+JSON.stringify(this.$attrs.record));
        //console.log(" value:"+this.$attrs.record[this.field.fieldName]);

        return this.$attrs.record[this.field.fieldName]
      },
      set(value) {
        this.$attrs.record[this.field.fieldName] = value
        //this.$emit('update:modelValue', value)
      }
    }
  },
  methods: {
    // Determines the class(es) needed for the given field.
    textFieldClass(theField) {
      if (theField.maxLength < 30) {
        return "p-md-4"
      } else if (theField.maxLength < 80) {
        return "p-md-6"
      } else {
        return "p-md-8"
      }
    }
  },
  mounted() {
    //console.log("this.field: " + JSON.stringify(this.field));
    //console.log("this.record: " + JSON.stringify(this.record));
  }

}
</script>

