<template>
  <div class="messages-page">
    <main class="messages-shell">
      <section class="page-heading">
        <div>
          <h1>Messages</h1>
          <p>Conversations about your activities.</p>
        </div>
        <el-button :loading="loading" @click="loadChats">
          <el-icon><Refresh /></el-icon>
          Refresh
        </el-button>
      </section>

      <el-alert
        v-if="loadFailed"
        class="load-alert"
        title="Messages are temporarily unavailable."
        type="info"
        show-icon
        :closable="false"
      />

      <section class="conversation-list" v-loading="loading">
        <article
          v-for="chat in conversations"
          :key="chat.id"
          class="conversation-item"
          @click="openChat(chat)"
        >
          <div class="conversation-main">
            <div class="title-row">
              <h2>{{ getConversationTitle(chat) }}</h2>
              <el-badge v-if="Number(chat.unreadCount || 0) > 0" :value="chat.unreadCount" />
            </div>
            <p class="event-title">{{ chat.event?.title || 'Direct conversation' }}</p>
            <p class="last-message">{{ getLastMessage(chat) }}</p>
          </div>
          <span class="time-text">{{ formatDate(chat.lastMessage?.createdAt || chat.updatedAt) }}</span>
        </article>

        <el-empty
          v-if="!loading && conversations.length === 0"
          description="No conversations yet"
        />
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { getChats } from '@/api/chat'
import useUserStore from '@/store/modules/user'
import { getUserId } from '@/utils/accessControl'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const loadFailed = ref(false)
const conversations = ref([])

function extractList(res) {
  const data = res?.data ?? res
  if (Array.isArray(data)) return data
  return data?.rows || data?.list || data?.content || []
}

function getOtherParticipants(chat) {
  const currentUserId = getUserId(userStore.userInfo)
  return (chat.participants || []).filter((user) => String(user.id || '') !== String(currentUserId || ''))
}

function getConversationTitle(chat) {
  const names = getOtherParticipants(chat).map((user) => user.fullName || 'Unknown user')
  return names.length ? names.join(', ') : 'Conversation'
}

function getLastMessage(chat) {
  const message = chat.lastMessage
  if (!message) return 'No messages yet'
  return message.content || 'Message unavailable'
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

function openChat(chat) {
  if (!chat.id) return
  router.push(`/messages/${chat.id}`)
}

async function loadChats() {
  loading.value = true
  loadFailed.value = false

  try {
    if (!userStore.userInfo) {
      await userStore.getInfo()
    }
    conversations.value = extractList(await getChats())
  } catch (error) {
    console.error('Failed to load chats:', error)
    conversations.value = []
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

onMounted(loadChats)
</script>

<style scoped lang="scss">
.messages-page {
  min-height: 100vh;
  background: #f7faff;
  color: #10234f;
}

.messages-shell {
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

.conversation-list {
  min-height: 260px;
  display: grid;
  gap: 12px;
}

.conversation-item {
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

.conversation-item:hover {
  border-color: #9fc0f7;
  background: #f8fbff;
}

.conversation-main {
  min-width: 0;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-row h2 {
  margin: 0;
  color: #10234f;
  font-size: 18px;
  line-height: 1.25;
}

.event-title,
.last-message {
  margin: 6px 0 0;
  color: #4f5d7c;
  font-size: 14px;
}

.last-message {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time-text {
  color: #73809c;
  font-size: 13px;
}

@media (max-width: 720px) {
  .messages-shell {
    padding: 28px 18px 36px;
  }

  .page-heading,
  .conversation-item {
    grid-template-columns: 1fr;
    align-items: flex-start;
  }
}
</style>
