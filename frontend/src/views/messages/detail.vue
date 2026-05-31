<template>
  <div class="message-detail-page">
    <main class="message-shell">
      <section class="page-heading">
        <el-button class="back-button" @click="backToMessages">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <div>
          <h1>{{ conversationTitle }}</h1>
          <p>{{ eventTitle }}</p>
        </div>
        <el-button :loading="loading" @click="loadMessages">
          <el-icon><Refresh /></el-icon>
          Refresh
        </el-button>
      </section>

      <section class="messages-card" v-loading="loading">
        <el-alert
          v-if="loadFailed"
          class="load-alert"
          :title="loadErrorMessage"
          type="error"
          show-icon
          :closable="false"
        />

        <div ref="messageListRef" class="message-list">
          <article
            v-for="message in orderedMessages"
            :key="message.id"
            class="message-row"
            :class="{ mine: isMine(message) }"
          >
            <img
              v-if="!isMine(message)"
              class="message-avatar"
              :src="getAvatar(message.sender)"
              alt="Message sender"
              @error="handleAvatarError"
            />
            <div class="message-bubble">
              <strong>{{ message.sender?.fullName || 'Unknown user' }}</strong>
              <p>{{ message.content || 'Message unavailable' }}</p>
              <span>{{ formatDate(message.createdAt) }}</span>
            </div>
          </article>

          <el-empty
            v-if="!loading && orderedMessages.length === 0"
            description="No messages yet"
          />
        </div>

        <div class="composer">
          <el-input
            v-model="draft"
            type="textarea"
            :rows="3"
            resize="none"
            maxlength="1000"
            placeholder="Type a message"
            @keydown.enter.exact.prevent="submitMessage"
          />
          <el-button type="primary" :loading="sending" @click="submitMessage">
            Send
          </el-button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Refresh } from '@element-plus/icons-vue'
import { getChatMessages, getChats, markChatRead, sendChatMessage } from '@/api/chat'
import useUserStore from '@/store/modules/user'
import { getUserId } from '@/utils/accessControl'
import defaultAvatar from '@/assets/images/profile.jpg'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const chatId = computed(() => route.params.conversationId || route.params.chatId)
const loading = ref(false)
const sending = ref(false)
const loadFailed = ref(false)
const loadErrorMessage = ref('')
const messages = ref([])
const chat = ref(null)
const draft = ref('')
const messageListRef = ref(null)
let refreshTimer = null

const orderedMessages = computed(() => {
  return [...messages.value].sort((left, right) => {
    return new Date(left.createdAt || 0).getTime() - new Date(right.createdAt || 0).getTime()
  })
})

const conversationTitle = computed(() => {
  const currentUserId = getUserId(userStore.userInfo)
  const names = (chat.value?.participants || [])
    .filter((user) => String(user.id || '') !== String(currentUserId || ''))
    .map((user) => user.fullName || 'Unknown user')
  return names.length ? names.join(', ') : 'Conversation'
})

const eventTitle = computed(() => {
  return chat.value?.event?.title || 'Direct conversation'
})

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

function normalizeMessage(message) {
  const payload = message?.data ?? message
  return {
    ...payload,
    id: payload.id || payload.messageId || payload.message_id,
    conversationId: payload.conversationId || payload.conversation_id || chatId.value,
    sender: payload.sender || payload.user || payload.author || {},
    senderId: payload.senderId || payload.sender_id || payload.sender?.id || payload.user?.id || payload.author?.id || '',
    content: payload.content || payload.body || payload.message || '',
    messageType: payload.messageType || payload.message_type || payload.type || 'TEXT',
    createdAt: payload.createdAt || payload.created_at || payload.createTime || payload.time || '',
    editedAt: payload.editedAt || payload.edited_at || null,
    deletedAt: payload.deletedAt || payload.deleted_at || null
  }
}

function getErrorMessage(error, fallback) {
  return error?.response?.data?.message ||
    error?.response?.data?.msg ||
    error?.response?.data?.error ||
    error?.message ||
    fallback
}

function isMine(message) {
  const currentUserId = getUserId(userStore.userInfo)
  const senderId = message.sender?.id || message.senderId
  return String(senderId || '') === String(currentUserId || '')
}

function getAvatar(user) {
  return user?.photo || defaultAvatar
}

function handleAvatarError(event) {
  event.target.src = defaultAvatar
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

function backToMessages() {
  router.push('/messages')
}

async function loadChatMeta() {
  const chats = extractList(await getChats())
  chat.value = chats.find((item) => String(item.id || '') === String(chatId.value || '')) || null
}

async function loadMessages() {
  if (!chatId.value) {
    ElMessage.error('Missing chat id')
    backToMessages()
    return
  }

  loading.value = true
  loadFailed.value = false
  loadErrorMessage.value = ''

  try {
    if (!userStore.userInfo) {
      await userStore.getInfo()
    }
    await loadChatMeta()
    messages.value = extractList(await getChatMessages(chatId.value))
      .map(normalizeMessage)
    await markChatRead(chatId.value)
    window.dispatchEvent(new CustomEvent('app:unread-refresh'))
    await scrollToBottom()
  } catch (error) {
    messages.value = []
    loadFailed.value = true
    loadErrorMessage.value = getErrorMessage(error, 'Failed to load messages')
    console.error('Failed to load chat messages:', loadErrorMessage.value, error?.response?.data || error)
    ElMessage.error(loadErrorMessage.value)
  } finally {
    loading.value = false
  }
}

function mergeMessages(incomingMessages) {
  const existingIds = new Set(messages.value.map((message) => String(message.id || '')))
  const normalizedIncoming = incomingMessages.map(normalizeMessage)
  const newMessages = normalizedIncoming.filter((message) => message.id && !existingIds.has(String(message.id)))
  if (newMessages.length === 0) {
    return false
  }
  messages.value = [...messages.value, ...newMessages]
  return true
}

async function refreshMessagesQuietly() {
  if (!chatId.value || loading.value || sending.value || document.hidden) {
    return
  }

  try {
    const incomingMessages = extractList(await getChatMessages(chatId.value))
    if (mergeMessages(incomingMessages)) {
      await markChatRead(chatId.value)
      window.dispatchEvent(new CustomEvent('app:unread-refresh'))
      await scrollToBottom()
    }
  } catch (error) {
    console.warn('Failed to refresh chat messages:', error)
  }
}

function startMessageRefresh() {
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
  }
  refreshTimer = window.setInterval(refreshMessagesQuietly, 4000)
}

function stopMessageRefresh() {
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
    refreshTimer = null
  }
}

async function scrollToBottom() {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

async function submitMessage() {
  const content = draft.value.trim()
  if (!content || sending.value) {
    return
  }

  sending.value = true

  try {
    draft.value = ''
    await sendChatMessage(chatId.value, content)
    await loadMessages()
  } catch (error) {
    console.error('Failed to send message:', error)
  } finally {
    sending.value = false
  }
}

onMounted(async () => {
  await loadMessages()
  startMessageRefresh()
})

onBeforeUnmount(stopMessageRefresh)
</script>

<style scoped lang="scss">
.message-detail-page {
  min-height: 100vh;
  background: #f7faff;
  color: #10234f;
}

.message-shell {
  padding: 34px 44px 48px;
}

.page-heading {
  margin-bottom: 24px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
}

.back-button {
  width: 44px;
  height: 44px;
  padding: 0;
}

.page-heading h1 {
  margin: 0;
  color: #0d1f4f;
  font-size: 34px;
  line-height: 1.2;
  font-weight: 800;
}

.page-heading p {
  margin: 8px 0 0;
  color: #4f5d7c;
  font-size: 15px;
}

.messages-card {
  min-height: 560px;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  border: 1px solid #e0e8f6;
  border-radius: 8px;
  background: #fff;
}

.load-alert {
  margin: 16px 16px 0;
}

.message-list {
  min-height: 420px;
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: auto;
}

.message-row {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.message-row.mine {
  justify-content: flex-end;
}

.message-avatar {
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #d8e3f4;
}

.message-bubble {
  max-width: min(620px, 78%);
  padding: 12px 14px;
  border-radius: 8px;
  background: #eef5ff;
}

.message-row.mine .message-bubble {
  background: #0f66e9;
  color: #fff;
}

.message-bubble strong,
.message-bubble p,
.message-bubble span {
  display: block;
}

.message-bubble strong {
  margin-bottom: 6px;
  font-size: 13px;
}

.message-bubble p {
  margin: 0;
  line-height: 1.5;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.message-bubble span {
  margin-top: 8px;
  color: #6f7d98;
  font-size: 12px;
}

.message-row.mine .message-bubble span {
  color: rgba(255, 255, 255, 0.78);
}

.composer {
  padding: 16px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 120px;
  gap: 12px;
  border-top: 1px solid #e0e8f6;
}

@media (max-width: 720px) {
  .message-shell {
    padding: 28px 18px 36px;
  }

  .page-heading,
  .composer {
    grid-template-columns: 1fr;
  }

  .message-bubble {
    max-width: 92%;
  }
}
</style>
