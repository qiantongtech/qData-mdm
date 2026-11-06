<template>
  <!-- Bring! No.  -->
  <el-form-item v-bind="$attrs">
    <template #default="scope">
      <div class="default-wrap">
        <slot name="default" v-bind="scope || {}" />
        <div v-if="isString && props.tip" class="tip-content">
          <el-icon> <info-filled /> </el-icon>
          <span v-html="props.tip"></span>
        </div>
      </div>
    </template>

    <template #label="scope">
      <div class="label-wrap">
        <slot name="label" v-bind="scope">
          {{ scope.label }}
        </slot>
        <el-tooltip
          v-if="!isString"
          v-bind="props.tip"
          :effect="props.tip.effect || 'light'"
          :placement="props.tip.placement || 'top'"
        >
          <el-icon class="tip-icon"> <info-filled /> </el-icon>
          <template v-if="props.tip.custom" #content>
            <div v-html="props.tip.content" class="tip-content"></div>
          </template>
        </el-tooltip>
      </div>
    </template>
  </el-form-item>
</template>

<script setup name="QtFromItem">
import { computed } from 'vue';

const props = defineProps({
  tip: {
    type: [String, Object],
    default: ''
  }
});

const isString = computed(() => {
  return typeof props.tip === 'string';
});
</script>

<style lang="scss" scoped>
.default-wrap {
  position: relative;
  width: 100%;

  .tip-content {
    display: flex;
    align-items: center;
    gap: 2px;
    padding-top: 4px;
    color: #888;
    font-size: 12px;
    line-height: 1.5;
    white-space: wrap;
  }
}

.label-wrap {
  display: flex;
  align-items: center;
  gap: 2px;

  .el-icon {
    color: #888;
  }
}

::v-deep(.el-form-item__error) {
  padding-top: 6px;
}

.el-form-item.is-error {
  padding-bottom: 16px;

  .tip-content {
    display: none;
  }
}
</style>
