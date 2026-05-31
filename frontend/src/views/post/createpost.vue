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
        <el-form class="main-card" :model="postForm" label-position="top">
          <div v-if="isViewMode && loadedPost?.user" class="author-strip" @click="openUserProfile(loadedPost.user)">
            <img :src="defaultAvatar" alt="Post author" />
            <div>
              <span>Posted by</span>
              <strong>{{ loadedPost.user.fullName || 'Unknown user' }}</strong>
            </div>
          </div>

          <el-form-item label="Related Event">
            <el-autocomplete
              v-model="relatedEventKeyword"
              size="large"
              value-key="title"
              placeholder="Search an event to associate with this post"
              :disabled="isViewMode"
              :fetch-suggestions="searchRelatedEvents"
              clearable
              @input="handleRelatedEventInput"
              @select="selectRelatedEvent"
              @clear="clearRelatedEvent"
            />
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
              accept="image/jpeg,image/png"
              :disabled="isViewMode"
              :on-change="validatePostImage"
              :on-remove="handleImageRemove"
            >
              <el-icon v-if="!isViewMode"><Plus /></el-icon>
            </el-upload>
          </div>
        </el-form>

        <aside class="side-panel">
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
            <el-button
              v-if="canManageLoadedPost"
              size="large"
              type="primary"
              class="publish-button"
              @click="goEditPost"
            >
              Edit Post
            </el-button>
          </section>

          <section v-if="isViewMode" class="comments-card" v-loading="commentsLoading">
            <div class="section-heading">
              <div>
                <h2>Comments</h2>
                <p>Community comments for this post.</p>
              </div>
            </div>
            <div class="comment-list">
              <article v-for="comment in comments" :key="comment.id" class="comment-item">
                <button v-if="comment.user?.id" class="comment-author" @click="openUserProfile(comment.user)">
                  <span class="comment-avatar">{{ getInitials(comment.user?.fullName) }}</span>
                  <strong>{{ comment.user?.fullName || 'Anonymous' }}</strong>
                </button>
                <strong v-else>{{ comment.user?.fullName || 'Anonymous' }}</strong>
                <p>{{ comment.comment || comment.content || 'No comment text' }}</p>
              </article>
              <el-empty v-if="!commentsLoading && comments.length === 0" description="No comments yet" />
            </div>
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
  getPost,
  getPostImages,
  getPostReviews,
  updatePost,
  uploadPostImage
} from '@/api/post'
import { getEventDetail } from '@/api/events/detail'
import { searchEvent } from '@/api/events'
import useUserStore from '@/store/modules/user'
import { canManagePostForUser } from '@/utils/accessControl'
import {
  Plus,
  Reading
} from '@element-plus/icons-vue'

// 路由参数决定页面模式：无 id 为创建，有 id 为编辑，mode=view 为只读查看
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const postId = computed(() => route.query.id)
const isEditMode = computed(() => Boolean(postId.value))
const isViewMode = computed(() => route.query.mode === 'view')
const loading = ref(false)
const submitting = ref(false)
const loadedPost = ref(null)
const comments = ref([])
const commentsLoading = ref(false)
const relatedEventKeyword = ref('')
const canManageLoadedPost = computed(() => {
  if (!loadedPost.value) {
    return !isEditMode.value
  }

  return canManagePostForUser(userStore.userInfo, loadedPost.value)
})

// 帖子表单数据，对应 POST /api/posts 和 PUT /api/posts/{id}
const postForm = ref({
  eventId: '',
  title: '',
  content: ''
})

// el-upload 使用的图片列表；已有图片会带 persisted/imageId，新上传图片会带 raw
const imageList = ref([])
const maxPostImageSizeBytes = 2 * 1024 * 1024

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
    content: post.content || post.body || post.description || ''
  }
  relatedEventKeyword.value = post.event?.title || post.eventTitle || post.eventName || ''
}

function validatePostImage(file) {
  const rawFile = file.raw || file
  const allowedTypes = ['image/jpeg', 'image/png']
  if (!allowedTypes.includes(rawFile.type)) {
    ElMessage.error('Post images must be JPEG or PNG. GIF and other animated images are not supported.')
    imageList.value = imageList.value.filter((item) => item.uid !== file.uid)
    return false
  }

  if (rawFile.size > maxPostImageSizeBytes) {
    ElMessage.error('Post image is too large. Please upload a JPEG or PNG under 2 MB.')
    imageList.value = imageList.value.filter((item) => item.uid !== file.uid)
    return false
  }

  return true
}

async function searchRelatedEvents(query, callback) {
  const keyword = String(query || '').trim()
  if (!keyword) {
    callback([])
    return
  }

  try {
    const events = extractList(await searchEvent({ keyword, limit: 8 }))
    callback(events.map((event) => ({
      ...event,
      title: event.title || event.name || 'Untitled event',
      value: event.title || event.name || 'Untitled event'
    })))
  } catch (error) {
    console.error('Failed to search related events:', error)
    callback([])
  }
}

function selectRelatedEvent(event) {
  postForm.value.eventId = event.id || ''
  relatedEventKeyword.value = event.title || event.name || ''
}

function handleRelatedEventInput() {
  postForm.value.eventId = ''
}

function clearRelatedEvent() {
  postForm.value.eventId = ''
  relatedEventKeyword.value = ''
}

// 提交前校验必填项
function validateForm() {
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
  const payload = {
    title: postForm.value.title.trim(),
    content: postForm.value.content.trim(),
    status
  }

  if (postForm.value.eventId.trim()) {
    payload.eventId = postForm.value.eventId.trim()
  }

  return payload
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

// 编辑/查看模式下加载帖子详情和已有图片
async function loadPostForEdit() {
  if (!postId.value) {
    return
  }

  const post = await hydrateRelatedEvent(await getPost(postId.value))
  loadedPost.value = post

  if (!isViewMode.value && !canManageLoadedPost.value) {
    ElMessage.warning('You do not have permission to edit this post')
    router.replace({ name: 'MainPost' })
    return
  }

  fillPostForm(post)
  imageList.value = normalizeImages(extractList(await getPostImages(postId.value)))
  if (isViewMode.value) {
    await loadPostComments()
  }
}

async function hydrateRelatedEvent(post) {
  const relatedEventId = post?.event?.id || post?.eventId || post?.relatedEventId
  if (!relatedEventId || post?.event?.organizer) {
    return post
  }

  try {
    return {
      ...post,
      event: await getEventDetail(relatedEventId)
    }
  } catch (error) {
    console.error('Failed to load related event for post:', relatedEventId, error)
    return post
  }
}

// 保存帖子主流程：校验表单 -> 创建/更新 post -> 上传新增图片 -> 返回列表页
async function savePost(status) {
  if (isEditMode.value && !canManageLoadedPost.value) {
    ElMessage.warning('You do not have permission to edit this post')
    return
  }

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

    let hasImageUploadError = false
    if (savedPostId) {
      try {
        await uploadNewImages(savedPostId)
      } catch (error) {
        hasImageUploadError = true
        console.error('Failed to upload post images:', error)
      }
    }

    if (hasImageUploadError) {
      ElMessage.warning('Post saved, but image upload failed. Please check Supabase Storage policy.')
    } else {
      ElMessage.success(status === 'DRAFT' ? 'Draft saved' : 'Post published')
    }
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

function openUserProfile(user) {
  if (user?.id) {
    router.push(`/users/${user.id}`)
  }
}

function getInitials(name) {
  return String(name || '?').trim().slice(0, 1).toUpperCase() || '?'
}

async function loadPostComments() {
  if (!postId.value) return
  commentsLoading.value = true
  try {
    comments.value = extractList(await getPostReviews(postId.value))
  } catch (error) {
    console.error('Failed to load post comments:', error)
    comments.value = []
  } finally {
    commentsLoading.value = false
  }
}

// 从只读查看模式切换到编辑模式
function goEditPost() {
  if (!canManageLoadedPost.value) {
    ElMessage.warning('You do not have permission to edit this post')
    return
  }

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

// 页面初始化：如果带 id，则加载帖子详情和已有图片
onMounted(async () => {
  loading.value = true

  try {
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

.author-strip {
  margin-bottom: 22px;
  padding: 12px;
  display: inline-grid;
  grid-template-columns: 44px auto;
  gap: 12px;
  align-items: center;
  border: 1px solid #dce7f6;
  border-radius: 8px;
  background: #f8fbff;
  cursor: pointer;
}

.author-strip img {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
}

.author-strip span,
.author-strip strong {
  display: block;
}

.author-strip span {
  color: #73809c;
  font-size: 13px;
}

.author-strip strong {
  color: #0969f6;
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
.tip-card h3 {
  margin: 0;
  color: #0d1f4f;
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

.comments-card {
  padding: 20px;
  border: 1px solid #e0e8f6;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 12px 28px rgba(26, 54, 100, 0.09);
}

.comment-list {
  display: grid;
  gap: 12px;
}

.comment-item {
  padding: 12px;
  border: 1px solid #dde7f5;
  border-radius: 8px;
  background: #f8fbff;
}

.comment-author {
  margin: 0 0 8px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 0;
  background: transparent;
  color: #0969f6;
  cursor: pointer;
}

.comment-avatar {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #e8f1ff;
  color: #0969f6;
  font-weight: 800;
}

.comment-item p {
  margin: 0;
  color: #415178;
  line-height: 1.55;
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
