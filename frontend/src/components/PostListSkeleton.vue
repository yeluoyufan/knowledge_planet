<template>
  <div class="skeleton-list">
    <el-skeleton v-for="i in count" :key="i" animated>
      <template #template>
        <div class="item">
          <el-skeleton-item variant="h3" style="width: 62%" />
          <div class="tags">
            <el-skeleton-item variant="text" style="width: 100px" />
            <el-skeleton-item variant="text" style="width: 80px" />
            <el-skeleton-item variant="text" style="width: 70px" />
          </div>
          <el-skeleton-item variant="text" style="width: 92%" />
          <el-skeleton-item variant="text" style="width: 78%" />
          <div class="meta">
            <el-skeleton-item variant="circle" style="width: 20px; height: 20px" />
            <el-skeleton-item variant="text" style="width: 120px" />
            <el-skeleton-item variant="text" style="width: 160px" />
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
  padding: 6px 0 12px;
}

.item {
  padding: 20px 0;
  border-bottom: 1px solid #f0f0f0;
}

.tags {
  margin: 10px 0 12px;
  display: flex;
  gap: 10px;
}

.meta {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #cbd5e1;
}
</style>
