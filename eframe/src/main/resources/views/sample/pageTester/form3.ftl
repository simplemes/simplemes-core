<#assign title>Form3</#assign>
<#include "../../includes/header.ftl" />
<#include "../../includes/definition.ftl" />

<@efPreloadMessages
codes="ok.label,cancel.label,cancel.tooltip,definitionEditor.title,definitionEditor.drag.label
         save.label,save.tooltip"/>
<script>

  setTimeout(efd._editorOpenConfigDialog, 500)

</script>
<#include "../../includes/footer.ftl" />

