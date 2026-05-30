<template>
  <div class="notifications-page">
    <main class="notifications-shell">
      <section class="page-heading">
        <div>
          <h1>Notifications</h1>
          <p>Recent updates about your activities and bookings.</p>
        </div>
        <el-button :loading="loading" @click="loadNotifications">
          <el-icon><Refresh /></el-icon>
          Refresh
        </el-button>
      </section>

      <el-alert
        v-if="loadFailed"
        class="load-alert"
        title="Notifications are temporarily unavailable."
        type="info"
        show-icon
        :closable="false"
      />

      <section class="notifications-card" v-loading="loading">
        <article
          v-for="notification in notifications"
          :key="notification.id"
          class="notification-item"
          :class="{ unread: !notification.isRead }"
          @click="openNotification(notification)"
        >
          <div class="notification-main">
            <h2>{{ notification.title }}</h2>
            <p>{{ notification.body }}</p>
          </div>
          <span>{{ formatDate(notification.createdAt) }}</span>
        </article>

        <el-empty
          v-if="!loading && notifications.length === 0"
          description="No notifications yet"
        />
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { listNotifications, markNotificationRead } from '@/api/notifications'

const router = useRouter()
const loading = ref(false)
const loadFailed = ref(false)
const notifications = ref([])

function extractList(res) {
  const data = res?.data ?? res
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.data)) return data.data
  if (Array.isArray(data?.rows)) return data.rows
  if (Array.isArray(data?.list)) return data.list
  if (Array.isArray(data?.content)) return data.content
  if (Array.isArray(data?.items)) return data.items
  return []
}

function readPayload(notification) {
  const payload = notification?.payload || notification?.metadata || notification?.data || {}
  if (payload && typeof payload === 'object' && !Array.isArray(payload)) {
    return payload
  }
  return {}
}

function normalizeNotification(notification) {
  const payload = readPayload(notification)
  const targetType = notification.targetType || notification.target_type || payload.targetType || ''
  const targetId = notification.targetId || notification.target_id || payload.targetId || ''
  const readAt = notification.readAt || notification.read_at || null
  const status = String(notification.status || '').toUpperCase()

  return {
    ...notification,
    id: notification.id || notification.notificationId || notification.notification_id,
    type: notification.type || payload.type || '',
    title: notification.title || notification.subject || 'Notification',
    body: notification.body || notification.message || notification.content || notification.description || 'No details available.',
    targetType,
    targetId,
    sourceType: notification.sourceType || notification.source_type || payload.sourceType || '',
    sourceId: notification.sourceId || notification.source_id || payload.sourceId || '',
    payload,
    readAt,
    isRead: Boolean(readAt || notification.read === true || notification.isRead === true || status === 'READ'),
    createdAt: notification.createdAt || notification.created_at || notification.createTime || notification.time || ''
  }
}

function normalizeType(value) {
  return String(value || '').trim().toUpperCase()
}

function notificationSnapshot(notification) {
  return {
    type: notification.type,
    targetType: notification.targetType,
    targetId: notification.targetId,
    sourceType: notification.sourceType,
    sourceId: notification.sourceId,
    payload: notification.payload
  }
}

function resolveEventId(notification) {
  const metadata = notification.payload || {}
  const targetType = normalizeType(notification.targetType)
  const sourceType = normalizeType(notification.sourceType)

  if (metadata.eventId) {
    return metadata.eventId
  }

  if (sourceType === 'EVENT' && notification.sourceId) {
    return notification.sourceId
  }

  if (targetType === 'EVENT' && notification.targetId) {
    return notification.targetId
  }

  return ''
}

function resolveConversationId(notification) {
  const metadata = notification.payload || {}
  const targetType = normalizeType(notification.targetType)

  if (metadata.conversationId) {
    return metadata.conversationId
  }

  if (['CHAT', 'CONVERSATION', 'CHAT_CONVERSATION'].includes(targetType) && notification.targetId) {
    return notification.targetId
  }

  return ''
}

function resolvePostId(notification) {
  const metadata = notification.payload || {}
  const targetType = normalizeType(notification.targetType)

  if (metadata.postId) {
    return metadata.postId
  }

  if (targetType === 'POST' && notification.targetId) {
    return notification.targetId
  }

  return ''
}

function isFriendRequestNotification(notification) {
  return normalizeType(notification.type) === 'FRIEND_REQUEST_RECEIVED' ||
    normalizeType(notification.targetType) === 'FRIEND_REQUEST' ||
    normalizeType(notification.sourceType) === 'FRIEND_REQUEST'
}

function getErrorMessage(error, fallback) {
  return error?.response?.data?.message ||
    error?.response?.data?.msg ||
    error?.response?.data?.error ||
    error?.message ||
    fallback
}

async function fetchNotifications() {
  try {
    return await listNotifications({ status: 'all', limit: 50 })
  } catch (error) {
    console.warn('GET /api/notifications with query params failed, retrying without params:', getErrorMessage(error, 'Request failed'), error?.response?.data || error)
    return listNotifications()
  }
}

function formatDate(value) {
  const date = new Date(value)
  if (!value || Number.isNaN(date.getTime())) return ''
  return date.toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit'
  })
}

function resolveTarget(notification) {
  const notificationType = normalizeType(notification.type)

  if (isFriendRequestNotification(notification)) {
    return {
      path: '/friends',
      query: {
        tab: 'received',
        requestId: notification.targetId || notification.sourceId || notification.payload?.requestId || ''
      }
    }
  }

  if (notificationType === 'CHAT_MESSAGE_RECEIVED') {
    const conversationId = resolveConversationId(notification)
    return conversationId ? { path: `/messages/${conversationId}` } : null
  }

  if ([
    'EVENT_REGISTRATION_STATUS_CHANGED',
    'EVENT_REGISTRATION_CREATED',
    'EVENT_UPDATED',
    'EVENT_CANCELLED',
    'EVENT_STARTS_IN_1_DAY',
    'EVENT_STARTS_IN_2_HOURS'
  ].includes(notificationType)) {
    const eventId = resolveEventId(notification)
    return eventId ? { path: '/product/eventDetails', query: { id: eventId } } : null
  }

  if (notificationType === 'POST_REVIEW_CREATED') {
    const postId = resolvePostId(notification)
    return postId ? { name: 'CreatePost', query: { id: postId, mode: 'view' } } : null
  }

  return null
}

async function openNotification(notification) {
  console.log('Opening notification:', notificationSnapshot(notification))

  if (notification.id && !notification.isRead) {
    markNotificationRead(notification.id)
      .then(() => {
        notifications.value = notifications.value.map((item) => {
          return item.id === notification.id ? { ...item, isRead: true, readAt: item.readAt || new Date().toISOString() } : item
        })
        window.dispatchEvent(new CustomEvent('app:unread-refresh'))
      })
      .catch((error) => {
        console.error('Failed to mark notification as read:', getErrorMessage(error, 'Failed to mark notification as read'), error)
      })
  }

  const target = resolveTarget(notification)
  if (target) {
    router.push(target).catch((error) => {
      console.error('Failed to navigate from notification:', error)
    })
    return
  }

  if ([
    'EVENT_REGISTRATION_STATUS_CHANGED',
    'EVENT_REGISTRATION_CREATED',
    'EVENT_UPDATED',
    'EVENT_CANCELLED',
    'EVENT_STARTS_IN_1_DAY',
    'EVENT_STARTS_IN_2_HOURS'
  ].includes(normalizeType(notification.type))) {
    ElMessage.warning('Unable to open event from this notification.')
    return
  }

  console.warn('Unsupported notification target:', notificationSnapshot(notification))
  ElMessage.warning('This notification type cannot be opened yet.')
}

async function loadNotifications() {
  loading.value = true
  loadFailed.value = false

  try {
    notifications.value = extractList(await fetchNotifications())
      .map(normalizeNotification)
  } catch (error) {
    notifications.value = []
    loadFailed.value = true
    const message = getErrorMessage(error, 'Failed to load notifications')
    console.error('Failed to load notifications:', message, error?.response?.data || error)
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

onMounted(loadNotifications)
</script>

<style scoped lang="scss">
.notifications-page {
  min-height: 100vh;
  background: #f7faff;
  color: #10234f;
}

.notifications-shell {
  padding: 34px 44px 48px;
}

.page-heading {
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.page-heading h1 {
  margin: 0;
  color: #0d1f4f;
  font-size: 42px;
  line-height: 1.15;
  font-weight: 800;
}

.page-heading p {
  margin: 10px 0 0;
  color: #4f5d7c;
  font-size: 16px;
}

.load-alert {
  margin-bottom: 16px;
}

.notifications-card {
  min-height: 260px;
  display: grid;
  gap: 12px;
}

.notification-item {
  padding: 18px 20px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 20px;
  align-items: center;
  border: 1px solid #e0e8f6;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
}

.notification-item.unread {
  border-color: #9fc0f7;
  background: #f2f7ff;
}

.notification-main {
  min-width: 0;
}

.notification-main h2 {
  margin: 0;
  color: #071a47;
  font-size: 18px;
  line-height: 1.3;
}

.notification-main p {
  margin: 8px 0 0;
  color: #4f5d7c;
  line-height: 1.5;
}

.notification-item > span {
  color: #667091;
  font-size: 14px;
  white-space: nowrap;
}

@media (max-width: 720px) {
  .notifications-shell {
    padding: 24px 16px 32px;
  }

  .page-heading,
  .notification-item {
    align-items: flex-start;
    grid-template-columns: 1fr;
  }

  .page-heading {
    flex-direction: column;
  }
}
</style>
