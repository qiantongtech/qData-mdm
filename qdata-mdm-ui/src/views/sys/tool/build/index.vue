<template>
  <div>
    <qt-wrap :config="{ fullContent: false, actions: { show: false } }">
      {{ td('sys.tool.build.title') }}
      <svg-icon icon-class="build" />
    </qt-wrap>
  </div>
</template>

<script setup>

import useDefaultLang from "@/composables/useDefaultLang";
const { td } = useDefaultLang();

</script>
