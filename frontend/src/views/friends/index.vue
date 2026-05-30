<template>
  <div class="friends-page">
    <main class="friends-shell">
      <section class="page-heading">
        <div>
          <h1>Friends</h1>
          <p>Manage friend requests and start private conversations.</p>
        </div>
        <el-button :loading="loading" @click="loadAll">Refresh</el-button>
      </section>

      <section class="search-card">
        <el-input
          v-model="keyword"
          size="large"
          clearable
          placeholder="Search users by name"
          @keyup.enter="searchPeople"
        />
        <el-button type="primary" size="large" :loading="searching" @click="searchPeople">Search</el-button>
      </section>

      <section v-if="searchResults.length" class="section-card">
        <h2>Add Friends</h2>
        <div class="user-grid">
          <article v-for="user in searchResults" :key="user.id" class="user-card">
            <UserMiniCard :user="user" />
            <el-button :disabled="isSelf(user) || relationFor(user).disabled" @click="addFriend(user)">
              {{ relationFor(user).label }}
            </el-button>
          </article>
        </div>
      </section>

      <section class="section-card" v-loading="loading">
        <h2>My Friends</h2>
        <div class="user-grid">
          <article v-for="friend in friends" :key="friend.user?.id" class="user-card">
            <UserMiniCard :user="friend.user" />
            <div class="button-row">
              <el-button @click="viewProfile(friend.user)">View Profile</el-button>
              <el-button type="primary" :loading="messageLoadingId === friend.user?.id" @click="messageFriend(friend.user)">Message</el-button>
              <el-button type="danger" plain @click="deleteFriend(friend.user)">Remove Friend</el-button>
            </div>
          </article>
        </div>
        <el-empty v-if="!loading && friends.length === 0" description="No friends yet" />
      </section>

      <section class="requests-grid">
        <article class="section-card">
          <h2>Received Requests</h2>
          <div class="request-list">
            <div v-for="request in incomingRequests" :key="request.id" class="request-item">
              <UserMiniCard :user="request.requester" />
              <div class="button-row">
                <el-button type="primary" @click="acceptRequest(request)">Accept</el-button>
                <el-button type="danger" plain @click="rejectRequest(request)">Reject</el-button>
              </div>
            </div>
            <el-empty v-if="incomingRequests.length === 0" description="No received requests" />
          </div>
        </article>

        <article class="section-card">
          <h2>Sent Requests</h2>
          <div class="request-list">
            <div v-for="request in outgoingRequests" :key="request.id" class="request-item">
              <UserMiniCard :user="request.addressee" />
              <el-button type="warning" plain @click="cancelRequest(request)">Cancel Request</el-button>
            </div>
            <el-empty v-if="outgoingRequests.length === 0" description="No sent requests" />
          </div>
        </article>
      </section>
    </main>
  </div>
</template>

<script setup>
import { defineComponent, h, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import defaultAvatar from '@/assets/images/profile.jpg'
import { searchUsers } from '@/api/user'
import {
  acceptFriendRequest,
  cancelFriendRequest,
  getFriends,
  getIncomingFriendRequests,
  getOutgoingFriendRequests,
  rejectFriendRequest,
  removeFriend,
  sendFriendRequest
} from '@/api/friend'
import { createDirectChat } from '@/api/chat'
import useUserStore from '@/store/modules/user'
import { getUserId } from '@/utils/accessControl'

const UserMiniCard = defineComponent({
  props: {
    user: {
      type: Object,
      default: () => ({})
    }
  },
  setup(props) {
    const photo = () => normalizePhotoUrl(props.user?.photo)
    return () => h('div', { class: 'mini-card' }, [
      h('img', {
        src: photo(),
        alt: props.user?.fullName || 'User avatar',
        onError: (event) => { event.target.src = defaultAvatar }
      }),
      h('div', [
        h('strong', props.user?.fullName || 'Unknown user'),
        props.user?.role ? h('span', props.user.role) : null
      ])
    ])
  }
})

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const searching = ref(false)
const keyword = ref('')
const friends = ref([])
const incomingRequests = ref([])
const outgoingRequests = ref([])
const searchResults = ref([])
const messageLoadingId = ref('')

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

function isSameUser(user, id) {
  return Boolean(user?.id && id && String(user.id) === String(id))
}

function isSelf(user) {
  return isSameUser(user, getUserId(userStore.userInfo))
}

function relationFor(user) {
  if (isSelf(user)) return { label: 'You', disabled: true }
  if (friends.value.some((friend) => isSameUser(friend.user, user.id))) return { label: 'Friends', disabled: true }
  if (outgoingRequests.value.some((request) => isSameUser(request.addressee, user.id))) return { label: 'Request Sent', disabled: true }
  if (incomingRequests.value.some((request) => isSameUser(request.requester, user.id))) return { label: 'Respond in Received', disabled: true }
  return { label: 'Add Friend', disabled: false }
}

async function loadAll() {
  loading.value = true
  try {
    if (!userStore.userInfo) {
      await userStore.getInfo()
    }
    const [friendsRes, incomingRes, outgoingRes] = await Promise.all([
      getFriends(),
      getIncomingFriendRequests(),
      getOutgoingFriendRequests()
    ])
    friends.value = extractList(friendsRes)
    incomingRequests.value = extractList(incomingRes)
    outgoingRequests.value = extractList(outgoingRes)
  } catch (error) {
    console.error('Failed to load friends:', error)
  } finally {
    loading.value = false
  }
}

async function searchPeople() {
  searching.value = true
  try {
    searchResults.value = extractList(await searchUsers(keyword.value.trim(), 20))
  } catch (error) {
    console.error('Failed to search users:', error)
    searchResults.value = []
  } finally {
    searching.value = false
  }
}

async function addFriend(user) {
  try {
    await sendFriendRequest(user.id)
    ElMessage.success('Friend request sent')
    await loadAll()
  } catch (error) {
    console.error('Failed to send friend request:', error)
  }
}

async function acceptRequest(request) {
  await acceptFriendRequest(request.id)
  ElMessage.success('Friend request accepted')
  await loadAll()
}

async function rejectRequest(request) {
  try {
    await ElMessageBox.confirm('Reject this friend request?', 'Reject Request', {
      confirmButtonText: 'Reject',
      cancelButtonText: 'Cancel',
      type: 'warning'
    })
    await rejectFriendRequest(request.id)
    ElMessage.success('Friend request rejected')
    await loadAll()
  } catch (error) {
    if (error !== 'cancel') console.error('Failed to reject friend request:', error)
  }
}

async function cancelRequest(request) {
  try {
    await ElMessageBox.confirm('Cancel this friend request?', 'Cancel Request', {
      confirmButtonText: 'Cancel Request',
      cancelButtonText: 'Keep',
      type: 'warning'
    })
    await cancelFriendRequest(request.id)
    ElMessage.success('Friend request cancelled')
    await loadAll()
  } catch (error) {
    if (error !== 'cancel') console.error('Failed to cancel friend request:', error)
  }
}

async function deleteFriend(user) {
  try {
    await ElMessageBox.confirm(`Remove ${user.fullName || 'this user'} from your friends?`, 'Remove Friend', {
      confirmButtonText: 'Remove',
      cancelButtonText: 'Cancel',
      type: 'warning'
    })
    await removeFriend(user.id)
    ElMessage.success('Friend removed')
    await loadAll()
  } catch (error) {
    if (error !== 'cancel') console.error('Failed to remove friend:', error)
  }
}

async function messageFriend(user) {
  messageLoadingId.value = user.id
  try {
    const chat = await createDirectChat(user.id)
    if (chat?.id) {
      router.push(`/messages/${chat.id}`)
    }
  } catch (error) {
    console.error('Failed to open chat:', error)
  } finally {
    messageLoadingId.value = ''
  }
}

function viewProfile(user) {
  router.push(`/users/${user.id}`)
}

onMounted(loadAll)
</script>

<style scoped lang="scss">
.friends-page {
  min-height: 100vh;
  background: #f7faff;
  color: #10234f;
}

.friends-shell {
  padding: 34px 44px 48px;
}

.page-heading {
  margin-bottom: 22px;
  display: flex;
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
}

.search-card,
.section-card {
  border: 1px solid #e0e8f6;
  border-radius: 8px;
  background: #fff;
}

.search-card {
  margin-bottom: 18px;
  padding: 18px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 132px;
  gap: 12px;
}

.section-card {
  margin-bottom: 18px;
  padding: 20px;
}

.section-card h2 {
  margin: 0 0 16px;
  font-size: 22px;
}

.user-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.user-card,
.request-item {
  padding: 14px;
  display: grid;
  gap: 12px;
  border: 1px solid #dde7f5;
  border-radius: 8px;
  background: #f8fbff;
}

.request-list {
  display: grid;
  gap: 12px;
}

.requests-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.button-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

:deep(.mini-card) {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

:deep(.mini-card img) {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  object-fit: cover;
}

:deep(.mini-card strong),
:deep(.mini-card span) {
  display: block;
}

:deep(.mini-card span) {
  margin-top: 4px;
  color: #73809c;
  font-size: 13px;
}

@media (max-width: 760px) {
  .friends-shell {
    padding: 24px 16px 36px;
  }

  .page-heading,
  .search-card,
  .requests-grid {
    grid-template-columns: 1fr;
    display: grid;
  }
}
</style>
