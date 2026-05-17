<template>
  <div class="create-post-page">
    <main class="create-post-shell">
      <section class="hero-row">
        <div>
          <p class="eyebrow">Share your experience</p>
          <h1>{{ pageTitle }}</h1>
          <p>{{ pageDescription }}</p>
        </div>
      </section>

      <section class="editor-grid" v-loading="loading">
        <el-form ref="postFormRef" class="main-card" :model="postForm" label-position="top">
          <el-form-item label="Related Event" required>
            <el-select
              v-model="postForm.eventId"
              size="large"
              placeholder="Choose an event you attended"
              :disabled="isViewMode"
            >
              <el-option
                v-for="event in events"
                :key="event.id"
                :label="event.name"
                :value="event.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="Post Title" required>
            <el-input
              v-model="postForm.title"
              size="large"
              placeholder="Give your post a clear, friendly title"
              :disabled="isViewMode"
            />
          </el-form-item>

          <el-form-item label="Content / Experience" required>
            <el-input
              v-model="postForm.content"
              type="textarea"
              :rows="11"
              resize="none"
              placeholder="Share your experience, suggestions, accessibility notes, or moments that stood out."
              :disabled="isViewMode"
            />
          </el-form-item>

          <div v-if="!isViewMode || imageList.length" class="upload-section">
            <div class="section-heading">
              <div>
                <h2>Upload Images</h2>
                <p>Add a few photos to make your post more useful and personal.</p>
              </div>
            </div>

            <el-upload
              v-model:file-list="imageList"
              class="image-uploader"
              action="#"
              list-type="picture-card"
              :auto-upload="false"
              :limit="4"
              :disabled="isViewMode"
              :on-remove="handleImageRemove"
            >
              <el-icon v-if="!isViewMode"><Plus /></el-icon>
            </el-upload>
          </div>
        </el-form>

        <aside class="side-panel">
          <section class="side-card">
            <h2>Accessibility Tags</h2>
            <p class="side-note">Choose the details that best describe your visit.</p>

            <el-checkbox-group v-model="postForm.tags" class="tag-list" :disabled="isViewMode">
              <el-checkbox-button
                v-for="tag in accessibilityTags"
                :key="tag"
                :label="tag"
              >
                {{ tag }}
              </el-checkbox-button>
            </el-checkbox-group>
          </section>

          <section class="tip-card">
            <div class="tip-icon">
              <el-icon><Reading /></el-icon>
            </div>
            <div>
              <h3>Tips</h3>
              <p>Focus on what helped, what was difficult, and what future visitors should know before they go.</p>
            </div>
          </section>

          <section v-if="!isViewMode" class="action-card">
            <el-button size="large" class="draft-button" :loading="submitting" @click="saveDraft">
              Save Draft
            </el-button>
            <el-button size="large" type="primary" class="publish-button" :loading="submitting" @click="publishPost">
              Publish Post
            </el-button>
          </section>
          <section v-else class="action-card">
            <el-button size="large" class="draft-button" @click="backToPosts">
              Back to Posts
            </el-button>
            <el-button size="large" type="primary" class="publish-button" @click="goEditPost">
              Edit Post
            </el-button>
          </section>
        </aside>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  createPost,
  deletePostImage,
  getCurrentUserProfile,
  getPost,
  getPostImages,
  getRegistrationsByUser,
  updatePost,
  uploadPostImage
} from '@/api/post'
import { getEventDetail } from '@/api/events/detail'
import {
  Plus,
  Reading
} from '@element-plus/icons-vue'

// 路由参数决定页面模式：无 id 为创建，有 id 为编辑，mode=view 为只读查看
const route = useRoute()
const router = useRouter()
const postFormRef = ref(null)
const postId = computed(() => route.query.id)
const isEditMode = computed(() => Boolean(postId.value))
const isViewMode = computed(() => route.query.mode === 'view')
const loading = ref(false)
const submitting = ref(false)

// 帖子表单数据，对应 POST /api/posts 和 PUT /api/posts/{id}
const postForm = ref({
  eventId: '',
  title: '',
  content: '',
  tags: []
})

// el-upload 使用的图片列表；已有图片会带 persisted/imageId，新上传图片会带 raw
const imageList = ref([])
const currentUserId = ref('')

// 当前用户参加过的活动列表，用于 Related Event 下拉框
const events = ref([])

// 前端固定展示的无障碍标签，保存时随 post 一起提交
const accessibilityTags = [
  'Wheelchair Accessible',
  'Elevator Available',
  'Accessible Restroom',
  'Quiet / Low Noise',
  'Child Friendly',
  'Crowded'
]

// 根据创建/编辑/查看模式切换页面标题
const pageTitle = computed(() => {
  if (isViewMode.value) return 'View Post'
  return isEditMode.value ? 'Edit Post' : 'Create Post'
})

// 根据页面模式切换说明文案
const pageDescription = computed(() => {
  if (isViewMode.value) return 'Review your event experience post.'
  if (isEditMode.value) return 'Update your post and keep your event experience useful for others.'
  return 'Write about an event you joined and help others understand what the experience felt like.'
})

// 兼容不同后端列表响应格式，统一转成数组
function extractList(res) {
  if (Array.isArray(res)) {
    return res
  }

  return res?.rows || res?.data || res?.list || res?.content || []
}

// 从 /api/users/me 的返回里取当前用户 id，兼容多种字段结构
function getProfileId(profile) {
  return profile?.id || profile?.userId || profile?.user?.id || profile?.profile?.id
}

// 将后端 event 原始数据整理成下拉框需要的 id/name
function normalizeEvent(event) {
  return {
    id: event.id || event.eventId,
    name: event.title || event.name || event.eventTitle || `Event #${event.id || event.eventId}`
  }
}

// 从 registration 里取 eventId；如果后端已经返回 event 对象，也兼容 event.id
function getEventIdFromRegistration(registration) {
  return registration.eventId || registration.event?.id || registration.event?.eventId
}

// 将后端 tags 字段统一成数组；兼容数组、JSON 字符串和逗号分隔字符串
function normalizeTags(value) {
  if (Array.isArray(value)) {
    return value
  }

  if (typeof value === 'string' && value.trim()) {
    try {
      const parsed = JSON.parse(value)
      return Array.isArray(parsed) ? parsed : value.split(',').map((item) => item.trim()).filter(Boolean)
    } catch {
      return value.split(',').map((item) => item.trim()).filter(Boolean)
    }
  }

  return []
}

// 将后端图片列表转换成 el-upload 需要的 file-list 格式
function normalizeImages(images) {
  return images.map((image) => ({
    name: image.fileName || image.name || `image-${image.id}`,
    url: image.imageUrl || image.url || image.publicUrl || image.path,
    imageId: image.id,
    persisted: true
  })).filter((image) => image.url)
}

// 编辑/查看模式下，把帖子详情填回表单
function fillPostForm(post) {
  postForm.value = {
    eventId: post.eventId || post.event?.id || '',
    title: post.title || '',
    content: post.content || post.body || post.description || '',
    tags: normalizeTags(post.tags || post.accessibilityTags)
  }
}

// 提交前校验必填项
function validateForm() {
  if (!postForm.value.eventId) {
    ElMessage.error('Please choose a related event')
    return false
  }

  if (!postForm.value.title.trim()) {
    ElMessage.error('Please enter a post title')
    return false
  }

  if (!postForm.value.content.trim()) {
    ElMessage.error('Please enter post content')
    return false
  }

  return true
}

// 组装创建/更新帖子接口需要的请求体
function buildPayload(status) {
  return {
    eventId: postForm.value.eventId,
    title: postForm.value.title.trim(),
    content: postForm.value.content.trim(),
    tags: postForm.value.tags,
    status
  }
}

// 帖子创建或更新成功后，再把本次新增的图片逐张上传到 Post Images 接口
async function uploadNewImages(savedPostId) {
  const files = imageList.value.filter((file) => file.raw && !file.persisted)

  await Promise.all(files.map((file) => {
    const formData = new FormData()
    formData.append('file', file.raw)
    return uploadPostImage(savedPostId, formData)
  }))
}

// 加载当前用户参加过的活动：先查用户，再查用户报名记录，最后整理成活动下拉选项
async function loadEventsForCurrentUser() {
  const profile = await getCurrentUserProfile()
  const userId = getProfileId(profile)
  currentUserId.value = userId

  if (!userId) {
    ElMessage.error('Unable to load current user profile')
    return
  }

  const registrations = extractList(await getRegistrationsByUser(userId))
  const eventMap = new Map()

  registrations.forEach((registration) => {
    if (registration.event) {
      const event = normalizeEvent(registration.event)
      eventMap.set(event.id, event)
    }
  })

  const missingEventIds = registrations
    .map(getEventIdFromRegistration)
    .filter((eventId) => eventId && !eventMap.has(eventId))

  const missingEvents = await Promise.all(missingEventIds.map(async (eventId) => {
    try {
      return normalizeEvent(await getEventDetail(eventId))
    } catch (error) {
      console.error('Failed to load registered event:', error)
      return null
    }
  }))

  missingEvents.filter(Boolean).forEach((event) => {
    eventMap.set(event.id, event)
  })

  events.value = Array.from(eventMap.values())
}

// 编辑/查看模式下加载帖子详情和已有图片
async function loadPostForEdit() {
  if (!postId.value) {
    return
  }

  const post = await getPost(postId.value)
  fillPostForm(post)
  imageList.value = normalizeImages(extractList(await getPostImages(postId.value)))

  if (postForm.value.eventId && !events.value.some((event) => event.id === postForm.value.eventId)) {
    events.value.unshift(normalizeEvent(post.event || {
      id: postForm.value.eventId,
      title: post.eventTitle || post.eventName
    }))
  }
}

// 保存帖子主流程：校验表单 -> 创建/更新 post -> 上传新增图片 -> 返回列表页
async function savePost(status) {
  if (!validateForm()) {
    return
  }

  submitting.value = true

  try {
    const payload = buildPayload(status)
    const savedPost = isEditMode.value
      ? await updatePost(postId.value, payload)
      : await createPost(payload)

    const savedPostId = postId.value || savedPost?.id || savedPost?.postId

    if (savedPostId) {
      await uploadNewImages(savedPostId)
    }

    ElMessage.success(status === 'DRAFT' ? 'Draft saved' : 'Post published')
    router.push({ name: 'MainPost' })
  } catch (error) {
    console.error('Failed to save post:', error)
  } finally {
    submitting.value = false
  }
}

// 保存为草稿
function saveDraft() {
  savePost('DRAFT')
}

// 发布帖子
function publishPost() {
  savePost('PUBLISHED')
}

// 返回帖子列表页
function backToPosts() {
  router.push({ name: 'MainPost' })
}

// 从只读查看模式切换到编辑模式
function goEditPost() {
  router.push({ name: 'CreatePost', query: { id: postId.value } })
}

// 删除已有图片时，同步调用后端 Post Images 删除接口；新选择但未上传的图片只由 el-upload 本地移除
function handleImageRemove(file) {
  if (!file.persisted || !file.imageId || !postId.value || isViewMode.value) {
    return
  }

  deletePostImage(postId.value, file.imageId).catch((error) => {
    console.error('Failed to delete post image:', error)
  })
}

// 页面初始化：加载活动下拉；如果带 id，再加载帖子详情
onMounted(async () => {
  loading.value = true

  try {
    await loadEventsForCurrentUser()
    await loadPostForEdit()
  } catch (error) {
    console.error('Failed to load create post page:', error)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.create-post-page {
  min-height: 100vh;
  background: #f7faff;
  color: #10234f;
  font-family: Inter, "Segoe UI", Arial, sans-serif;
}

.create-post-shell {
  padding: 36px 44px 48px;
}

.hero-row {
  margin-bottom: 28px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #2f75f6;
  font-size: 15px;
  font-weight: 700;
}

.hero-row h1 {
  margin: 0;
  font-size: 44px;
  line-height: 1.15;
  font-weight: 800;
  color: #0d1f4f;
  letter-spacing: 0;
}

.hero-row p:last-child {
  max-width: 720px;
  margin: 12px 0 0;
  color: #1d3264;
  font-size: 18px;
  line-height: 1.6;
}

.editor-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 28px;
  align-items: start;
}

.main-card,
.side-card,
.tip-card,
.action-card {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #e0e8f6;
  border-radius: 14px;
  box-shadow: 0 12px 28px rgba(26, 54, 100, 0.09);
}

.main-card {
  padding: 28px;
}

.main-card :deep(.el-form-item) {
  margin-bottom: 24px;
}

.main-card :deep(.el-form-item__label) {
  margin-bottom: 8px;
  color: #0d1f4f;
  font-size: 15px;
  font-weight: 700;
}

.main-card :deep(.el-input__wrapper),
.main-card :deep(.el-textarea__inner) {
  border-radius: 12px;
  box-shadow: 0 0 0 1px #d6dfef inset;
}

.main-card :deep(.el-textarea__inner) {
  padding: 18px;
  color: #26365f;
  font-size: 15px;
  line-height: 1.7;
}

.upload-section {
  padding-top: 4px;
}

.section-heading {
  margin-bottom: 16px;
}

.section-heading h2,
.side-card h2 {
  margin: 0;
  color: #0d1f4f;
  font-size: 20px;
  font-weight: 800;
}

.section-heading p,
.side-note {
  margin: 8px 0 0;
  color: #667091;
  font-size: 14px;
  line-height: 1.5;
}

.image-uploader :deep(.el-upload--picture-card),
.image-uploader :deep(.el-upload-list__item) {
  width: 148px;
  height: 112px;
  border-radius: 14px;
  background: #f4f8ff;
  border-color: #cddbf0;
}

.image-uploader :deep(.el-upload--picture-card .el-icon) {
  color: #2f75f6;
  font-size: 28px;
}

.side-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.side-card {
  padding: 24px;
}

.tag-list {
  margin-top: 18px;
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.tag-list :deep(.el-checkbox-button__inner) {
  width: 100%;
  min-height: 46px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  border: 1px solid #dbe5f4;
  border-radius: 12px;
  color: #26365f;
  background: #fbfdff;
  font-size: 14px;
  text-align: left;
  box-shadow: none;
}

.tag-list :deep(.el-checkbox-button.is-checked .el-checkbox-button__inner) {
  border-color: #8db8ff;
  background: #eef6ff;
  color: #0969f6;
  box-shadow: none;
}

.tip-card {
  padding: 20px;
  display: flex;
  gap: 16px;
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
  margin: 6px 0 12px;
  color: #0f66e9;
  font-size: 16px;
  font-weight: 700;
}

.tip-card p {
  margin: 0;
  color: #415178;
  font-size: 15px;
  line-height: 1.65;
}

.action-card {
  padding: 20px;
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.draft-button,
.publish-button {
  height: 48px;
  border-radius: 10px;
  font-size: 16px;
}

.draft-button {
  border-color: #cfdced;
  color: #0d1f4f;
  background: #fff;
}

.publish-button {
  box-shadow: 0 10px 20px rgba(47, 117, 246, 0.22);
}

@media (max-width: 1180px) {
  .create-post-shell {
    padding: 30px 24px 40px;
  }

  .editor-grid {
    grid-template-columns: 1fr;
  }

  .side-panel {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .action-card {
    grid-column: 1 / -1;
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 720px) {
  .create-post-shell {
    padding: 24px 16px 32px;
  }

  .hero-row h1 {
    font-size: 36px;
  }

  .main-card {
    padding: 20px;
  }

  .side-panel,
  .action-card {
    grid-template-columns: 1fr;
  }

  .image-uploader :deep(.el-upload--picture-card),
  .image-uploader :deep(.el-upload-list__item) {
    width: 128px;
    height: 104px;
  }
}
</style>
