<script setup lang="ts">
// ============================================
// 后台 - 通知中心(数据来自后端 /notification/*, 已读不可逆)
// 交互规则: 点击通知 -> dialog 展示内容并置为已读; 已读/删除状态后端持久化
// ============================================
defineOptions({ name: 'AdminNotifications' })

import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import announcementApi, { type Announcement } from '@/api/modules/announcement'
import notificationApi from '@/api/modules/notification'
import { emitNotifyReadChanged, loadNotices, type Notice, type NotifyType } from '@/utils/notification'

const notices = ref<Notice[]>([])
const noticesLoading = ref(false)

const dialogVisible = ref(false)
const currentNotice = ref<Notice | null>(null)
const activeSection = ref<'notice' | 'announcement'>('notice')
const announcements = ref<Announcement[]>([])
const annLoading = ref(false)
const annTotal = ref(0)
const annPageNum = ref(1)
const annPageSize = ref(20)

const publishVisible = ref(false)
const publishSubmitting = ref(false)
const publishFormRef = ref<FormInstance>()
const publishForm = reactive({
  title: '',
  content: '',
})
const publishRules: FormRules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
}

const typeMeta: Record<NotifyType, { label: string; tag: 'primary' | 'info' | 'danger' }> = {
  borrow: { label: '借阅', tag: 'primary' },
  system: { label: '系统', tag: 'info' },
  overdue: { label: '逾期', tag: 'danger' },
}

const unreadCount = computed(() => notices.value.filter((n) => !n.read).length)

// 通知列表(全部展示, 未读以圆点区分)
const filteredNotices = computed(() => notices.value)

/** 公告时间格式化: 2026-09-15T10:00:00 -> 2026-09-15 10:00 */
function formatTime(t?: string): string {
  if (!t) return '—'
  return t.replace('T', ' ').slice(0, 16)
}

// 加载本人通知列表
async function loadNoticeList() {
  noticesLoading.value = true
  try {
    notices.value = await loadNotices()
  } catch {
    notices.value = []
  } finally {
    noticesLoading.value = false
  }
}

// 打开通知详情: 置为已读(后端持久化, 不可还原未读), 并通知顶部角标同步
async function openNotice(item: Notice) {
  if (!item.read) {
    item.read = true
    try {
      await notificationApi.markRead(item.id)
    } catch {
      // 后端标记失败不阻塞查看
    }
    emitNotifyReadChanged()
  }
  currentNotice.value = item
  dialogVisible.value = true
}

// 全部已读: 单向置为已读(不可逆), 并通知顶部角标同步
async function markAllRead() {
  try {
    await notificationApi.markAllRead()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
    return
  }
  notices.value.forEach((n) => (n.read = true))
  emitNotifyReadChanged()
}

// 单条删除: 后端删除(不可恢复), 通知顶部角标同步
async function removeNotice(item: Notice) {
  try {
    await notificationApi.remove(item.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
    return
  }
  notices.value = notices.value.filter((n) => n.id !== item.id)
  emitNotifyReadChanged()
  ElMessage.success('通知已删除')
}

// 一键清空: 后端清空本人全部通知(不可恢复)
async function clearAll() {
  try {
    await ElMessageBox.confirm('确定清空全部通知吗？删除后不可恢复。', '清空通知', {
      confirmButtonText: '全部清空',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await notificationApi.clear()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '清空失败')
    return
  }
  notices.value = []
  emitNotifyReadChanged()
  ElMessage.success('已清空全部通知')
}

// ===== 公告加载/发布/删除 =====
async function loadAnnouncements() {
  annLoading.value = true
  try {
    const res = await announcementApi.page({ pageNum: annPageNum.value, pageSize: annPageSize.value })
    announcements.value = res.data?.list ?? []
    annTotal.value = res.data?.total ?? 0
  } catch (error) {
    announcements.value = []
    annTotal.value = 0
    ElMessage.error(error instanceof Error ? error.message : '公告加载失败')
  } finally {
    annLoading.value = false
  }
}

function openPublish() {
  publishForm.title = ''
  publishForm.content = ''
  publishVisible.value = true
}

async function handlePublishSubmit() {
  if (!publishFormRef.value) return
  const valid = await publishFormRef.value.validate().catch(() => false)
  if (!valid) return
  publishSubmitting.value = true
  try {
    await announcementApi.create({ title: publishForm.title.trim(), content: publishForm.content.trim() })
    ElMessage.success('公告发布成功，读者端已可见')
    publishVisible.value = false
    annPageNum.value = 1
    void loadAnnouncements()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发布失败')
  } finally {
    publishSubmitting.value = false
  }
}

async function removeAnnouncement(item: Announcement) {
  try {
    await ElMessageBox.confirm(`确定删除公告《${item.title}》吗？`, '删除公告', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await announcementApi.remove(item.id)
    ElMessage.success('公告已删除')
    void loadAnnouncements()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

// ===== 公告详情 dialog =====
const announcementDialogVisible = ref(false)
const currentAnnouncement = ref<Announcement | null>(null)

function openAnnouncement(item: Announcement) {
  currentAnnouncement.value = item
  announcementDialogVisible.value = true
}

// ===== 置顶 / 取消置顶(可多条置顶) =====
const pinningId = ref<number | null>(null)

async function togglePin(item: Announcement) {
  pinningId.value = item.id
  try {
    if (item.isPinned) {
      await announcementApi.unpin(item.id)
      ElMessage.success('已取消置顶')
    } else {
      await announcementApi.pin(item.id)
      ElMessage.success('已置顶')
    }
    void loadAnnouncements()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  } finally {
    pinningId.value = null
  }
}

function handleAnnPageChange(page: number) {
  annPageNum.value = page
  void loadAnnouncements()
}

onMounted(() => {
  void loadNoticeList()
  void loadAnnouncements()
})
</script>

<template>
  <div class="page-container notify-page">
    <h2 class="page-title">通知中心</h2>

    <!-- 分区: 通知 / 公告 -->
    <div class="card toolbar">
      <el-tabs v-model="activeSection" class="notify-tabs">
        <el-tab-pane label="通知" name="notice">
          <template #label>
            <span class="tab-label">
              通知
              <span v-if="unreadCount" class="tab-badge">{{ unreadCount }}</span>
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="公告" name="announcement" />
      </el-tabs>
      <div class="toolbar-right">
        <template v-if="activeSection === 'notice'">
          <el-button type="primary" :disabled="unreadCount === 0" @click="markAllRead">
            全部已读
          </el-button>
          <el-button type="danger" plain :disabled="filteredNotices.length === 0" @click="clearAll">
            清空全部
          </el-button>
        </template>
        <template v-else>
          <el-button type="primary" @click="openPublish">发布公告</el-button>
        </template>
      </div>
    </div>

    <!-- 通知列表: 点击打开详情 dialog, 阅读后即已读且不可还原 -->
    <template v-if="activeSection === 'notice'">
      <div v-loading="noticesLoading" class="notice-list">
        <div
          v-for="item in filteredNotices"
          :key="item.id"
          class="notice-item"
          :class="{ unread: !item.read }"
          @click="openNotice(item)"
        >
          <span class="dot" :class="{ off: item.read }"></span>
          <div class="notice-body">
            <div class="notice-head">
              <span class="notice-title">{{ item.title }}</span>
              <el-tag :type="typeMeta[item.type].tag" size="small" effect="plain">
                {{ typeMeta[item.type].label }}
              </el-tag>
            </div>
            <p class="notice-content">{{ item.content }}</p>
          </div>
          <span class="notice-time">{{ item.time }}</span>
          <el-button
            class="notice-delete"
            link
            type="danger"
            @click.stop="removeNotice(item)"
          >
            删除
          </el-button>
        </div>

        <el-empty v-if="filteredNotices.length === 0" description="暂无未读通知" :image-size="80" />
      </div>
    </template>

    <!-- 公告列表: 管理端发布/置顶/删除, 点击查看详情 -->
    <template v-else>
      <div v-loading="annLoading" class="notice-list">
        <div
          v-for="item in announcements"
          :key="item.id"
          class="notice-item announcement-item"
          :class="{ pinned: item.isPinned }"
          @click="openAnnouncement(item)"
        >
          <div class="notice-body">
            <div class="notice-head">
              <span class="notice-title">{{ item.title }}</span>
              <el-tag v-if="item.isPinned" type="warning" size="small" effect="dark">置顶</el-tag>
              <el-tag type="primary" size="small" effect="plain">公告</el-tag>
            </div>
            <p class="notice-content">{{ item.content }}</p>
            <div class="announcement-meta">
              <span>发布人：{{ item.publisherName || '—' }}</span>
              <span>{{ formatTime(item.createTime) }}</span>
            </div>
          </div>
          <div class="ann-actions" @click.stop>
            <el-button
              link
              :type="item.isPinned ? 'warning' : 'primary'"
              :loading="pinningId === item.id"
              @click="togglePin(item)"
            >
              {{ item.isPinned ? '取消置顶' : '置顶' }}
            </el-button>
            <el-button link type="danger" @click="removeAnnouncement(item)">删除</el-button>
          </div>
        </div>

        <el-empty v-if="!annLoading && announcements.length === 0" description="暂无公告，点击右上角发布" :image-size="80" />
      </div>
      <div v-if="annTotal > 0" class="pager">
        <el-pagination
          :current-page="annPageNum"
          :page-size="annPageSize"
          :total="annTotal"
          background
          layout="total, prev, pager, next"
          @current-change="handleAnnPageChange"
        />
      </div>
    </template>

    <!-- 发布公告 dialog -->
    <el-dialog
      v-model="publishVisible"
      width="560px"
      title="发布公告"
      class="notice-dialog"
      :close-on-click-modal="false"
    >
      <el-form ref="publishFormRef" :model="publishForm" :rules="publishRules" label-width="70px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="publishForm.title" maxlength="200" show-word-limit placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="publishForm.content"
            type="textarea"
            :rows="6"
            maxlength="2000"
            show-word-limit
            placeholder="请输入公告内容，发布后读者端首页立即可见"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishSubmitting" @click="handlePublishSubmit">
          发布
        </el-button>
      </template>
    </el-dialog>

    <!-- 通知详情 dialog -->
    <el-dialog
      v-model="dialogVisible"
      width="520px"
      :title="currentNotice ? currentNotice.title : ''"
      class="notice-dialog"
    >
      <div v-if="currentNotice" class="dialog-body">
        <div class="dialog-meta">
          <el-tag :type="typeMeta[currentNotice.type].tag" size="small" effect="plain">
            {{ typeMeta[currentNotice.type].label }}
          </el-tag>
          <span class="dialog-time">{{ currentNotice.time }}</span>
        </div>
        <p class="dialog-content">{{ currentNotice.content }}</p>
      </div>
    </el-dialog>

    <!-- 公告详情 dialog: 点击公告列表打开查看全文 -->
    <el-dialog
      v-model="announcementDialogVisible"
      width="560px"
      :title="currentAnnouncement ? currentAnnouncement.title : ''"
      class="notice-dialog"
    >
      <div v-if="currentAnnouncement" class="dialog-body">
        <div class="dialog-meta">
          <el-tag v-if="currentAnnouncement.isPinned" type="warning" size="small" effect="dark">置顶</el-tag>
          <el-tag type="primary" size="small" effect="plain">公告</el-tag>
          <span class="dialog-time">
            发布人：{{ currentAnnouncement.publisherName || '—' }} · {{ formatTime(currentAnnouncement.createTime) }}
          </span>
        </div>
        <p class="dialog-content">{{ currentAnnouncement.content }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.notify-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 8px 20px 0;
}

.notify-tabs {
  flex: 1;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: var(--radius-full);
  background: var(--brand-primary);
  color: #fff;
  font-size: 11px;
  line-height: 1;
}

.toolbar-right {
  flex-shrink: 0;
  padding-bottom: 12px;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notice-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  background: var(--bg-card);
  border-radius: $radius-lg;
  box-shadow: var(--shadow-card);
  padding: 18px 20px;
  cursor: pointer;
  transition:
    box-shadow 0.25s ease,
    transform 0.25s ease;

  &:hover {
    box-shadow: var(--shadow-hover);
    transform: translateY(-2px);
  }
}

.dot {
  flex-shrink: 0;
  width: 9px;
  height: 9px;
  margin-top: 6px;
  border-radius: var(--radius-full);
  background: var(--brand-primary);

  &.off {
    background: var(--border-base);
  }
}

.notice-body {
  flex: 1;
  min-width: 0;
}

.notice-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.notice-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.notice-item:not(.unread) .notice-title {
  font-weight: 500;
  color: var(--text-regular);
}

.notice-content {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-secondary);

  // 列表内最多两行, 完整内容在 dialog 中查看
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notice-time {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--text-placeholder);
  white-space: nowrap;
}

.notice-delete {
  flex-shrink: 0;
  align-self: center;
  opacity: 0;
  transition: opacity 0.2s ease;

  .notice-item:hover & {
    opacity: 1;
  }
}

.announcement-item {
  cursor: pointer;

  &:hover {
    transform: translateY(-2px);
  }
}

.ann-actions {
  flex-shrink: 0;
  align-self: center;
  display: flex;
  gap: 4px;
}

.announcement-meta {
  display: flex;
  gap: 20px;
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-placeholder);
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 4px;
}

// 详情 dialog 排版
.dialog-body {
  padding: 4px 0 8px;
}

.dialog-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.dialog-time {
  font-size: 13px;
  color: var(--text-placeholder);
}

.dialog-content {
  margin: 0;
  font-size: 15px;
  line-height: 1.8;
  color: var(--text-primary);
  white-space: pre-line;
}
</style>
