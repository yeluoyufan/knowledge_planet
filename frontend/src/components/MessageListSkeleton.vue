<template>
  <div class="skeleton-list">
    <el-skeleton v-for="i in count" :key="i" animated>
      <template #template>
        <div class="item">
          <div class="left">
            <el-skeleton-item variant="circle" style="width: 40px; height: 40px" />
          </div>
          <div class="right">
            <div class="top">
              <el-skeleton-item variant="text" style="width: 160px" />
              <el-skeleton-item variant="text" style="width: 120px" />
            </div>
            <el-skeleton-item variant="text" style="width: 82%" />
            <el-skeleton-item variant="text" style="width: 58%" />
          </div>
        </div>
      </template>
    </el-skeleton>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ count?: number }>(), { count: 6 })
</script>

<style scoped lang="scss">
.skeleton-list {
  padding: 0 20px;
}

.item {
  padding: 20px 0;
  border-bottom: 1px solid #f5f5f5;
  display: flex;
  gap: 15px;
}

.left {
  flex-shrink: 0;
}

.right {
  flex: 1;
  min-width: 0;
}

.top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}
</style>

