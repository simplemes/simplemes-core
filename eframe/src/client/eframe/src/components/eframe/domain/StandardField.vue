StandardField run-time display component.  Supports multiple fieldFormat values.
Should be used inside the div: 'div class="p-fluid p-formgrid p-grid"'


<template>
  <div class="p-field p-col-12" :class="textFieldClass(field)">
    <div class="p-field" v-if="field.fieldFormat==='S'">
      <label :for="field.fieldName">{{ $t(field.fieldLabel) }}</label>
      <InputText v-bind:id="field.fieldName" :maxlength="field.maxLength"/>
    </div>
    <div class="p-field" v-if="field.fieldFormat==='C'">
      <label :for="field.fieldName">{{ $t(field.fieldLabel) }}</label>
      <InlineGrid :storageKey="field.fieldName" :columns="field.fields"/>
    </div>
    <div class="p-field-checkbox" v-if="field.fieldFormat==='B'">
      <Checkbox v-bind:id="field.fieldName"/>
      <label :for="field.fieldName">{{ $t(field.fieldLabel) }}</label>
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
      value: null,
      index: -1,
    }
  },
  props: ['field'],
  components: {
    InputText, InputNumber, Checkbox, InlineGrid
  },
  methods: {
    textFieldClass(theField) {
      if (theField.maxLength < 30) {
        return "p-md-4"
      } else if (theField.maxLength < 80) {
        return "p-md-6"
      }

      return ""
    }
  },
  mounted() {
    //console.log("this.field: " + JSON.stringify(this.field));

  }

}
</script>

