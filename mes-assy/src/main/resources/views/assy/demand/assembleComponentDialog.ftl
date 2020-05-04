<@efPreloadMessages codes="assemble.label,cancel.label"/>

<@efForm id="assembleComponent" dashboard="true">
    <@efField field="OrderAssembledComponent.qty" id="qty" modelName="componentModel"/>
    <@efField field="OrderAssembledComponent.assemblyData" id="assemblyData" modelName="componentModel" _combo@readOnly="true"/>
</@efForm>
