<template>
  <div class="public-profile-page">
    <main class="profile-shell">
      <el-alert
        v-if="errorMessage"
        class="page-alert"
        :title="errorMessage"
        type="error"
        show-icon
        :closable="false"
      />

      <section class="profile-header" v-loading="loading">
        <img :src="avatarUrl" :alt="profile.fullName || 'User avatar'" class="profile-avatar" @error="handleAvatarError" />
        <div class="profile-main">
          <div class="title-row">
            <div>
              <h1>{{ profile.fullName || 'Unknown user' }}</h1>
              <el-tag v-if="profile.role" effect="plain">{{ roleDisplayLabel(profile.role) }}</el-tag>
            </div>
            <div class="action-row">
              <el-button v-if="isSelf" type="primary" @click="goMyProfile">Edit Profile</el-button>
              <template v-else>
                <el-button
                  :type="friendButtonType"
                  :loading="friendActionLoading"
                  :disabled="friendButtonDisabled"
                  @click="handleFriendAction"
                >
                  {{ friendButtonText }}
                </el-button>
                <el-tooltip
                  :disabled="canMessage"
                  content="You need to add this user as a friend before messaging."
                  placement="top"
                >
                  <span>
                    <el-button
                      type="primary"
                      :loading="messageLoading"
                      :disabled="!canMessage"
                      @click="openMessage"
                    >
                      Message
                    </el-button>
                  </span>
                </el-tooltip>
              </template>
            </div>
          </div>
          <p class="profile-note">{{ profile.email || profile.phone ? 'Contact details are available to you.' : 'Public community profile' }}</p>
          <dl class="profile-fields">
            <div v-if="profile.email">
              <dt>Email</dt>
              <dd>{{ profile.email }}</dd>
            </div>
            <div v-if="profile.phone">
              <dt>Phone</dt>
              <dd>{{ profile.phone }}</dd>
            </div>
          </dl>
        </div>
      </section>

      <section class="content-grid">
        <article class="section-card">
          <div class="section-heading">
            <h2>Posts</h2>
            <el-button text :loading="postsLoading" @click="loadPosts">Refresh</el-button>
          </div>
          <div v-loading="postsLoading" class="item-list">
            <button v-for="post in posts" :key="post.id" class="content-item" @click="openPost(post)">
              <strong>{{ post.title || 'Untitled Post' }}</strong>
              <span>{{ formatDate(post.updatedAt || post.createdAt) }}</span>
            </button>
            <el-empty v-if="!postsLoading && posts.length === 0" description="No public posts yet" />
          </div>
        </article>

        <article class="section-card">
          <div class="section-heading">
            <h2>Activities</h2>
          </div>
          <el-empty description="No activity summary available from the current API" />
        </article>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import defaultAvatar from '@/assets/images/profile.jpg'
import { getPublicUserProfile } from '@/api/user'
import {
  acceptFriendRequest,
  cancelFriendRequest,
  getFriends,
  getIncomingFriendRequests,
  getOutgoingFriendRequests,
  sendFriendRequest
} from '@/api/friend'
import { getChatId, getOrCreateDirectChat } from '@/api/chat'
import { getPostsByUser } from '@/api/post'
import useUserStore from '@/store/modules/user'
import { getUserId, roleDisplayLabel } from '@/utils/accessControl'
import { getToken } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const profile = ref({})
const friends = ref([])
const incomingRequests = ref([])
const outgoingRequests = ref([])
const posts = ref([])
const loading = ref(false)
const postsLoading = ref(false)
const friendActionLoading = ref(false)
const messageLoading = ref(false)
const errorMessage = ref('')

const userId = computed(() => route.params.id)
const currentUserId = computed(() => getUserId(userStore.userInfo))
const isSelf = computed(() => Boolean(userId.value && currentUserId.value && String(userId.value) === String(currentUserId.value)))
const outgoingRequest = computed(() => outgoingRequests.value.find((request) => sameUser(request.addressee, userId.value)))
const incomingRequest = computed(() => incomingRequests.value.find((request) => sameUser(request.requester, userId.value)))
const isFriend = computed(() => friends.value.some((friend) => sameUser(friend.user, userId.value)))
const canMessage = computed(() => isFriend.value && !isSelf.value)

const avatarUrl = computed(() => normalizePhotoUrl(profile.value.photo))
const friendButtonText = computed(() => {
  if (!getToken()) return 'Add Friend'
  if (isFriend.value) return 'Friends'
  if (outgoingRequest.value) return 'Cancel Request'
  if (incomingRequest.value) return 'Accept Request'
  return 'Add Friend'
})
const friendButtonType = computed(() => {
  if (isFriend.value) return 'success'
  if (outgoingRequest.value) return 'warning'
  return 'primary'
})
const friendButtonDisabled = computed(() => isFriend.value)

function sameUser(user, id) {
  return Boolean(user?.id && id && String(user.id) === String(id))
}

function extractList(res) {
  const data = res?.data ?? res
  if (Array.isArray(data)) return data
  return data?.rows || data?.list || data?.content || []
}

function normalizePhotoUrl(photo) {
  const value = String(photo || '').trim()
  if (!value) return defaultAvatar
  if (/^(https?:|data:|blob:)/i.test(value)) return value
  if (value.startsWith('/')) return value
  return `${import.meta.env.VITE_APP_BASE_API || ''}${value}`
}

function handleAvatarError(event) {
  event.target.src = defaultAvatar
}

function formatDate(value) {
  const date = new Date(value)
  if (!value || Number.isNaN(date.getTime())) return ''
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}

async function ensureCurrentUser() {
  if (getToken() && !userStore.userInfo) {
    await userStore.getInfo()
  }
}

async function loadRelationship() {
  if (!getToken() || isSelf.value) {
    friends.value = []
    incomingRequests.value = []
    outgoingRequests.value = []
    return
  }

  const [friendRes, incomingRes, outgoingRes] = await Promise.all([
    getFriends(),
    getIncomingFriendRequests(),
    getOutgoingFriendRequests()
  ])
  friends.value = extractList(friendRes)
  incomingRequests.value = extractList(incomingRes)
  outgoingRequests.value = extractList(outgoingRes)
}

async function loadProfile() {
  if (!userId.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    await ensureCurrentUser()
    profile.value = isSelf.value && userStore.userInfo
      ? userStore.userInfo
      : await getPublicUserProfile(userId.value)
    await loadRelationship()
  } catch (error) {
    console.error('Failed to load public profile:', error)
    errorMessage.value = error?.response?.data?.message || error?.message || 'Failed to load user profile'
  } finally {
    loading.value = false
  }
}

async function loadPosts() {
  if (!userId.value) return
  postsLoading.value = true
  try {
    posts.value = extractList(await getPostsByUser(userId.value)).filter((post) => {
      return String(post.status || '').trim().toUpperCase() === 'PUBLISHED'
    })
  } catch (error) {
    console.error('Failed to load user posts:', error)
    posts.value = []
  } finally {
    postsLoading.value = false
  }
}

async function handleFriendAction() {
  if (!getToken()) {
    ElMessage.warning('Please log in first.')
    router.push('/login')
    return
  }
  if (isFriend.value) return

  friendActionLoading.value = true
  try {
    if (outgoingRequest.value) {
      await ElMessageBox.confirm('Cancel this friend request?', 'Cancel Request', {
        confirmButtonText: 'Cancel Request',
        cancelButtonText: 'Keep',
        type: 'warning'
      })
      await cancelFriendRequest(outgoingRequest.value.id)
      ElMessage.success('Friend request cancelled')
    } else if (incomingRequest.value) {
      await acceptFriendRequest(incomingRequest.value.id)
      ElMessage.success('Friend request accepted')
    } else {
      await sendFriendRequest(userId.value)
      ElMessage.success('Friend request sent')
    }
    await loadRelationship()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Friend action failed:', error)
    }
  } finally {
    friendActionLoading.value = false
  }
}

async function openMessage() {
  if (!getToken()) {
    ElMessage.warning('Please log in first.')
    router.push('/login')
    return
  }
  if (!canMessage.value) {
    ElMessage.warning('You need to add this user as a friend before messaging.')
    return
  }

  messageLoading.value = true
  try {
    const chat = await getOrCreateDirectChat(userId.value)
    const chatId = getChatId(chat)
    if (chatId) {
      router.push(`/messages/${chatId}`)
    }
  } catch (error) {
    console.error('Failed to open direct chat:', error)
  } finally {
    messageLoading.value = false
  }
}

function goMyProfile() {
  router.push('/user/profile')
}

function openPost(post) {
  router.push({ name: 'PostDetails', query: { id: post.id } })
}

watch(userId, () => {
  loadProfile()
  loadPosts()
})

onMounted(() => {
  loadProfile()
  loadPosts()
})
</script>

<style scoped lang="scss">
.public-profile-page {
  min-height: 100vh;
  background: #f7faff;
  color: #10234f;
}

.profile-shell {
  padding: 34px 44px 48px;
}

.page-alert {
  margin-bottom: 16px;
}

.profile-header,
.section-card {
  border: 1px solid #e0e8f6;
  border-radius: 8px;
  background: #fff;
}

.profile-header {
  min-height: 220px;
  padding: 28px;
  display: grid;
  grid-template-columns: 132px minmax(0, 1fr);
  gap: 28px;
  align-items: center;
}

.profile-avatar {
  width: 132px;
  height: 132px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #dbe6f5;
}

.title-row {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.title-row h1 {
  margin: 0 0 12px;
  color: #0d1f4f;
  font-size: 38px;
  line-height: 1.15;
  font-weight: 800;
}

.action-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.profile-note {
  margin: 18px 0;
  color: #4f5d7c;
}

.profile-fields {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin: 0;
}

.profile-fields div {
  min-width: 200px;
}

.profile-fields dt {
  color: #73809c;
  font-size: 13px;
}

.profile-fields dd {
  margin: 4px 0 0;
  font-weight: 700;
}

.content-grid {
  margin-top: 20px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 20px;
}

.section-card {
  min-height: 260px;
  padding: 20px;
}

.section-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-heading h2 {
  margin: 0;
  font-size: 22px;
}

.item-list {
  min-height: 170px;
  display: grid;
  gap: 10px;
}

.content-item {
  width: 100%;
  padding: 14px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #dde7f5;
  border-radius: 8px;
  background: #f8fbff;
  color: #10234f;
  text-align: left;
  cursor: pointer;
}

@media (max-width: 780px) {
  .profile-shell {
    padding: 24px 16px 36px;
  }

  .profile-header,
  .content-grid,
  .title-row {
    grid-template-columns: 1fr;
    display: grid;
  }

  .action-row {
    flex-wrap: wrap;
  }
}
</style>
