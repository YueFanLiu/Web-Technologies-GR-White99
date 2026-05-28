<template>
  <div class="posts-page">
    <main class="posts-shell">
      <section class="hero-row">
        <div>
          <h1>Posts</h1>
          <p>{{ props.managerMode ? 'Manage and organize your posts.' : 'Browse community posts.' }}</p>
        </div>
        <el-button
          v-if="props.managerMode && canCreatePost"
          type="primary"
          class="create-button"
          @click="goCreatePost"
        >
          <el-icon><Plus /></el-icon>
          Create New Post
        </el-button>
      </section>

      <section class="content-grid">
        <aside class="side-panel">
          <div class="tabs-card">
            <button
              v-for="tab in tabs"
              :key="tab.value"
              class="post-tab"
              :class="{ active: activeTab === tab.value }"
              @click="activeTab = tab.value"
            >
              <span class="tab-label">
                <el-icon><Document /></el-icon>
                {{ tab.label }}
              </span>
              <span class="tab-count">{{ countByStatus(tab.value) }}</span>
            </button>
          </div>

          <div class="tip-card">
            <div class="tip-icon">
              <el-icon><Reading /></el-icon>
            </div>
            <div>
              <h3>Tip</h3>
              <p>Publish your posts to share important updates and insights with the community.</p>
            </div>
          </div>
        </aside>

        <section class="posts-card">
          <div class="list-toolbar">
            <el-input
              v-model="keyword"
              class="post-search"
              size="large"
              clearable
              :placeholder="props.managerMode ? 'Search your posts' : 'Search posts'"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <div class="sort-control">
              <span>Sort by:</span>
              <el-select v-model="sortBy" size="large">
                <el-option label="Most Recent" value="recent" />
                <el-option label="Oldest" value="oldest" />
              </el-select>
            </div>
          </div>

          <div class="post-list" v-loading="loading">
            <article v-for="post in filteredPosts" :key="post.id" class="post-card">
              <img :src="post.cover" :alt="post.title" class="post-cover" />

              <div class="post-body">
                <h2>{{ post.title }}</h2>
                <p class="related-event">
                  <el-icon><Calendar /></el-icon>
                  {{ post.relatedEvent }}
                </p>
                <p class="summary">{{ post.summary }}</p>
              </div>

              <div class="post-date">
                <span>{{ post.dateLabel }}</span>
                <strong>{{ post.date }}</strong>
              </div>

              <div class="post-actions">
                <el-tag
                  class="status-tag"
                  :class="post.status === 'Published' ? 'published' : 'draft'"
                  effect="plain"
                >
                  <span class="status-dot"></span>
                  {{ post.status }}
                </el-tag>

                <div class="button-row">
                  <el-button @click="viewPost(post)">
                    <el-icon><View /></el-icon>
                    View
                  </el-button>
                  <el-button v-if="props.managerMode && canManagePost(post)" @click="editPost(post)">
                    <el-icon><EditPen /></el-icon>
                    Edit
                  </el-button>
                  <el-button
                    v-if="props.managerMode && canManagePost(post)"
                    class="delete-button"
                    @click="removePost(post)"
                  >
                    <el-icon><Delete /></el-icon>
                    Delete
                  </el-button>
                </div>
              </div>
            </article>

            <el-empty v-if="filteredPosts.length === 0" description="No posts found" />
          </div>
        </section>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deletePost,
  getPostImages,
  getPostsByUser,
  listPosts
} from '@/api/post'
import { getEventDetail } from '@/api/events/detail'
import useUserStore from '@/store/modules/user'
import { canManagePostForUser, getUserId, isAdminRole, isOrganizerRole } from '@/utils/accessControl'
import {
  Calendar,
  Delete,
  Document,
  EditPen,
  Plus,
  Reading,
  Search,
  View
} from '@element-plus/icons-vue'

const props = defineProps({
  managerMode: {
    type: Boolean,
    default: false
  }
})

const activeTab = ref(props.managerMode ? 'All' : 'Published')
const sortBy = ref('recent')
const keyword = ref('')
const loading = ref(false)
const router = useRouter()
const userStore = useUserStore()
const canCreatePost = computed(() => Boolean(getUserId(userStore.userInfo)))

function canManagePost(post) {
  return canManagePostForUser(userStore.userInfo, post)
}

// 宸︿晶鐘舵€佺瓫閫夋爮閰嶇疆
const tabs = [
  { label: 'All Posts', value: 'All' },
  { label: 'Published', value: 'Published' },
  { label: 'Draft', value: 'Draft' }
]

// 褰撳墠鐢ㄦ埛鐨勫笘瀛愬垪琛紝椤甸潰鍔犺浇鍚庣敱鍚庣鎺ュ彛濉厖
const posts = ref([])
const eventDetailCache = new Map()

// 鏍规嵁鐘舵€併€佸叧閿瘝鍜屾帓搴忔柟寮忕敓鎴愭渶缁堝睍绀虹殑甯栧瓙鍒楄〃
const filteredPosts = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  const result = posts.value.filter((post) => {
    const matchesStatus = activeTab.value === 'All' || post.status === activeTab.value
    const matchesKeyword = !text || [post.title, post.relatedEvent, post.summary]
      .filter(Boolean)
      .some((value) => value.toLowerCase().includes(text))

    return matchesStatus && matchesKeyword
  })

  return [...result].sort((a, b) => {
    const left = a.sortTime || 0
    const right = b.sortTime || 0
    return sortBy.value === 'oldest' ? left - right : right - left
  })
})

// 缁熻鏌愪釜鐘舵€佷笅鐨勫笘瀛愭暟閲忥紝鐢ㄤ簬宸︿晶 tab 瑙掓爣
function countByStatus(status) {
  if (status === 'All') {
    return posts.value.length
  }

  return posts.value.filter((post) => post.status === status).length
}

function goCreatePost() {
  if (!props.managerMode || !canCreatePost.value) {
    return
  }

  router.push({ name: 'CreatePost' })
}

// 璺宠浆鍒板彧璇绘煡鐪嬫ā寮忥紝createpost.vue 浼氭牴鎹?mode=view 绂佺敤琛ㄥ崟
function viewPost(post) {
  router.push({ name: 'CreatePost', query: { id: post.id, mode: 'view' } })
}

// 璺宠浆鍒扮紪杈戞ā寮忥紝createpost.vue 浼氭牴鎹?id 鍔犺浇甯栧瓙璇︽儏
function editPost(post) {
  if (!props.managerMode || !canManagePost(post)) {
    ElMessage.warning('You do not have permission to edit this post')
    return
  }

  router.push({ name: 'CreatePost', query: { id: post.id } })
}

// 鍒犻櫎甯栧瓙锛氬厛寮瑰嚭纭妗嗭紝纭鍚庤皟鐢?DELETE /api/posts/{id}
function removePost(post) {
  if (!props.managerMode || !canManagePost(post)) {
    ElMessage.warning('You do not have permission to delete this post')
    return
  }

  ElMessageBox.confirm(`Delete "${post.title}"?`, 'Delete Post', {
    confirmButtonText: 'Delete',
    cancelButtonText: 'Cancel',
    type: 'warning'
  }).then(() => {
    return deletePost(post.id)
  }).then(() => {
    ElMessage.success('Post deleted')
    posts.value = posts.value.filter((item) => item.id !== post.id)
  }).catch((error) => {
    if (error !== 'cancel') {
      console.error('Failed to delete post:', error)
    }
  })
}

// 鍏煎涓嶅悓鍚庣鍒楄〃鍝嶅簲鏍煎紡锛岀粺涓€杞垚鏁扮粍
function extractList(res) {
  if (Array.isArray(res)) {
    return res
  }

  return res?.rows || res?.data || res?.list || res?.content || []
}

function formatDate(value) {
  if (!value) {
    return 'Date unavailable'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return 'Date unavailable'
  }

  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric'
  })
}

// 灏嗗悗绔姸鎬佺粺涓€鎴愰〉闈娇鐢ㄧ殑 Published / Draft
function getStatus(value) {
  const status = String(value || 'Published').toLowerCase()
  return status === 'draft' ? 'Draft' : 'Published'
}

// 浠庡笘瀛愬浘鐗囧垪琛ㄤ腑鍙栫涓€寮犱綔涓哄皝闈㈠浘
function getFirstImageUrl(images) {
  const image = images?.[0]
  return image?.imageUrl || image?.url || image?.publicUrl || image?.path || ''
}

// 鎸?Swagger 鐨?PostResponse.event / PostResponse.location 鐢熸垚鍏宠仈瀵硅薄灞曠ず鏂囨湰
function getRelatedTarget(post) {
  if (post.event) {
    return post.event.title || post.event.name || `Event #${post.event.id}`
  }

  if (post.location) {
    return post.location.name || post.location.city || `Location #${post.location.id}`
  }

  return post.eventTitle || post.eventName || post.locationName || (post.eventId ? `Event #${post.eventId}` : 'No related event')
}

// 鎽樿鏉ヨ嚜 content锛屽垪琛ㄩ噷闄愬埗闀垮害锛岄伩鍏嶉暱鏂囨湰鎾戝紑鍗＄墖
function getSummary(post) {
  const text = post.summary || post.excerpt || post.content || ''
  return text.length > 140 ? `${text.slice(0, 140)}...` : text
}

// 灏嗗悗绔?post 鍘熷鏁版嵁鏁寸悊鎴愰〉闈㈠崱鐗囬渶瑕佺殑鏁版嵁缁撴瀯
function normalizePost(post, images = []) {
  const createdAt = post.createdAt || post.createTime || post.createdTime
  const updatedAt = post.updatedAt || post.updateTime || post.updatedTime || createdAt
  const publishedAt = post.publishedAt || post.publishTime || updatedAt
  const status = getStatus(post.status)

  return {
    id: post.id,
    title: post.title || 'Untitled Post',
    relatedEvent: getRelatedTarget(post),
    dateLabel: status === 'Draft' ? 'Updated on' : 'Published on',
    date: formatDate(status === 'Draft' ? updatedAt : publishedAt),
    summary: getSummary(post),
    status,
    raw: post,
    user: post.user || null,
    userId: post.userId || post.user?.id || post.authorId || post.createdBy,
    event: post.event || null,
    eventId: post.event?.id || post.eventId || post.relatedEventId,
    sortTime: new Date(updatedAt || publishedAt || createdAt || 0).getTime(),
    cover: getFirstImageUrl(images) || post.coverImageUrl || post.imageUrl || 'https://picsum.photos/id/1083/420/260'
  }
}

async function hydrateManageEvent(post) {
  const relatedEventId = post.event?.id || post.eventId || post.relatedEventId
  if (!props.managerMode || !relatedEventId || post.event?.organizer) {
    return post
  }

  try {
    if (!eventDetailCache.has(relatedEventId)) {
      eventDetailCache.set(relatedEventId, await getEventDetail(relatedEventId))
    }
    return {
      ...post,
      event: eventDetailCache.get(relatedEventId)
    }
  } catch (error) {
    console.error('Failed to load related event for post:', relatedEventId, error)
    return post
  }
}

async function normalizePostList(list) {
  return Promise.all(list.map(async (post) => {
    const hydratedPost = await hydrateManageEvent(post)
    try {
      const images = extractList(await getPostImages(hydratedPost.id))
      return normalizePost(hydratedPost, images)
    } catch (error) {
      console.error('Failed to load post images:', error)
      return normalizePost(hydratedPost)
    }
  }))
}

async function loadPosts() {
  loading.value = true

  try {
    if (!props.managerMode) {
      const res = await listPosts({ status: 'PUBLISHED', limit: 1000 })
      posts.value = await normalizePostList(extractList(res))
      return
    }

    if (!userStore.userInfo) {
      await userStore.getInfo()
    }
    const userId = getUserId(userStore.userInfo)

    if (!userId) {
      posts.value = []
      ElMessage.error('Unable to load current user profile')
      return
    }

    const res = isAdminRole(userStore.userInfo?.role) || isOrganizerRole(userStore.userInfo?.role)
      ? await listPosts({ limit: 1000 })
      : await getPostsByUser(userId)
    const list = extractList(res)
    const normalizedPosts = await normalizePostList(list)
    posts.value = isAdminRole(userStore.userInfo?.role)
      ? normalizedPosts
      : normalizedPosts.filter((post) => canManagePostForUser(userStore.userInfo, post))
  } catch (error) {
    console.error('Failed to load posts:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadPosts()
})
</script>

<style scoped lang="scss">
.posts-page {
  min-height: 100vh;
  background: #f7faff;
  color: #10234f;
  font-family: Inter, "Segoe UI", Arial, sans-serif;
}

.posts-shell {
  padding: 36px 44px;
}

.hero-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.hero-row h1 {
  margin: 0;
  font-size: 44px;
  line-height: 1.15;
  font-weight: 800;
  color: #0d1f4f;
  letter-spacing: 0;
}

.hero-row p {
  margin: 12px 0 0;
  color: #1d3264;
  font-size: 18px;
}

.create-button {
  height: 50px;
  padding: 0 24px;
  border-radius: 10px;
  font-size: 16px;
  box-shadow: 0 10px 20px rgba(47, 117, 246, 0.22);
}

.content-grid {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 28px;
}

.side-panel,
.posts-card {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #e0e8f6;
  border-radius: 14px;
  box-shadow: 0 12px 28px rgba(26, 54, 100, 0.09);
}

.side-panel {
  min-height: 720px;
  padding: 28px 22px;
}

.tabs-card {
  padding: 0 0 28px;
  border-bottom: 1px solid #dce5f4;
}

.post-tab {
  width: 100%;
  min-height: 64px;
  padding: 0 16px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 0;
  border-radius: 12px;
  background: transparent;
  color: #0d1f4f;
  cursor: pointer;
  font-size: 17px;
}

.post-tab.active {
  background: #eef6ff;
  box-shadow: inset 0 0 0 1px #d6e7ff;
  color: #0969f6;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 14px;
}

.tab-label .el-icon {
  font-size: 24px;
}

.tab-count {
  min-width: 34px;
  height: 34px;
  padding: 0 8px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: #eaf1fb;
  color: #0d62e9;
  font-size: 15px;
  font-weight: 700;
}

.tip-card {
  margin-top: 36px;
  padding: 20px;
  min-height: 170px;
  display: flex;
  gap: 16px;
  border: 1px solid #dbe8fb;
  border-radius: 12px;
  background: #f1f7ff;
}

.tip-icon {
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #2f75f6;
  color: #fff;
  font-size: 20px;
}

.tip-card h3 {
  margin: 6px 0 14px;
  font-size: 16px;
  font-weight: 700;
  color: #0f66e9;
}

.tip-card p {
  margin: 0;
  color: #415178;
  font-size: 15px;
  line-height: 1.65;
}

.posts-card {
  padding: 20px 24px 12px;
}

.list-toolbar {
  height: 54px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 10px;
}

.post-search {
  max-width: 360px;
}

.post-search :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #d6dfef inset;
}

.sort-control {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #26365f;
  font-size: 14px;
}

.sort-control :deep(.el-select) {
  width: 156px;
}

.sort-control :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #d6dfef inset;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.post-card {
  min-height: 154px;
  padding: 16px;
  display: grid;
  grid-template-columns: 200px minmax(260px, 1fr) 150px 330px;
  gap: 24px;
  align-items: center;
  border: 1px solid #dde7f5;
  border-radius: 14px;
  background: #fff;
}

.post-cover {
  width: 200px;
  height: 120px;
  border-radius: 10px;
  object-fit: cover;
}

.post-body h2 {
  margin: 0 0 10px;
  color: #0d1f4f;
  font-size: 20px;
  line-height: 1.2;
  font-weight: 800;
  letter-spacing: 0;
}

.related-event {
  margin: 0 0 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #0969f6;
  font-size: 14px;
}

.summary {
  margin: 0;
  max-width: 420px;
  color: #667091;
  font-size: 15px;
  line-height: 1.6;
}

.post-date {
  align-self: start;
  padding-top: 22px;
  color: #647092;
  font-size: 14px;
}

.post-date span,
.post-date strong {
  display: block;
  font-weight: 400;
  line-height: 1.7;
}

.post-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 32px;
}

.status-tag {
  height: 32px;
  min-width: 112px;
  justify-content: center;
  border-radius: 9px;
  font-size: 14px;
}

.status-tag :deep(.el-tag__content) {
  display: flex;
  align-items: center;
  gap: 10px;
}

.status-tag.published {
  border-color: #c9edd7;
  background: #e8f9ee;
  color: #10833f;
}

.status-tag.draft {
  border-color: #ffe1ad;
  background: #fff7e8;
  color: #f18a00;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.button-row {
  display: flex;
  gap: 16px;
}

.button-row .el-button {
  width: 104px;
  height: 44px;
  border-radius: 9px;
  border-color: #d5deef;
  color: #0d1f4f;
  font-size: 14px;
}

.button-row .delete-button {
  color: #f3222d;
}

@media (max-width: 1280px) {
  .posts-shell {
    padding: 30px 24px;
  }

  .content-grid {
    grid-template-columns: 280px minmax(0, 1fr);
  }

  .post-card {
    grid-template-columns: 180px minmax(220px, 1fr) 120px;
  }

  .post-cover {
    width: 180px;
  }

  .post-actions {
    grid-column: 2 / 4;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
    gap: 18px;
  }
}

@media (max-width: 980px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .side-panel {
    min-height: auto;
  }
}

@media (max-width: 720px) {
  .posts-shell {
    padding: 24px 16px;
  }

  .hero-row {
    align-items: flex-start;
    flex-direction: column;
    gap: 18px;
  }

  .hero-row h1 {
    font-size: 36px;
  }

  .post-card {
    grid-template-columns: 1fr;
  }

  .post-cover {
    width: 100%;
    height: 180px;
  }

  .post-date {
    padding-top: 0;
  }

  .post-actions {
    grid-column: auto;
    align-items: stretch;
    flex-direction: column;
  }

  .button-row {
    flex-wrap: wrap;
  }

  .button-row .el-button {
    flex: 1 1 120px;
  }
}
</style>

