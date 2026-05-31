<template>
  <div class="post-detail-page">
    <main class="post-detail-shell" v-loading="loading">
      <section class="post-hero">
        <el-button class="back-button" @click="backToPosts">
          <el-icon><ArrowLeft /></el-icon>
          Back to Posts
        </el-button>

        <div v-if="post.user" class="author-strip" @click="openUserProfile(post.user)">
          <img :src="getAvatar(post.user)" alt="Post author" @error="handleAvatarError" />
          <div>
            <span>Posted by</span>
            <strong>{{ post.user.fullName || 'Unknown user' }}</strong>
          </div>
        </div>

        <h1>{{ post.title || 'Untitled Post' }}</h1>
        <div class="post-meta">
          <span>{{ formatDate(post.createdAt || post.updatedAt) }}</span>
          <el-tag v-if="post.status" effect="plain">{{ post.status }}</el-tag>
          <button v-if="relatedEvent?.id" class="event-link" @click="openEvent">
            <el-icon><Calendar /></el-icon>
            Related Event: {{ relatedEvent.title || 'Untitled event' }}
          </button>
        </div>
      </section>

      <section class="content-grid">
        <article class="post-card">
          <div v-if="images.length" class="image-grid">
            <img
              v-for="image in images"
              :key="image.id || image.imageUrl"
              :src="normalizeImageUrl(image.imageUrl || image.url || image.publicUrl || image.path)"
              :alt="post.title || 'Post image'"
            />
          </div>
          <div v-else class="image-placeholder">
            <img :src="fallbackPostImage" :alt="post.title || 'Post image'" />
          </div>

          <p class="post-content">{{ post.content || 'No post content.' }}</p>

          <div class="post-actions">
            <el-button v-if="canManageLoadedPost" type="primary" @click="editPost">
              <el-icon><EditPen /></el-icon>
              Edit Post
            </el-button>
          </div>
        </article>

        <aside class="comments-card">
          <div class="comments-header">
            <h2>Comments</h2>
            <span>{{ comments.length }}</span>
          </div>

          <div class="comment-composer">
            <el-rate v-model="commentForm.rating" />
            <el-input
              v-model="commentForm.comment"
              type="textarea"
              :rows="3"
              maxlength="1000"
              show-word-limit
              resize="none"
              placeholder="Write a comment"
            />
            <el-button type="primary" :loading="submittingComment" @click="submitComment">
              Submit Comment
            </el-button>
          </div>

          <div class="comment-list" v-loading="commentsLoading">
            <article v-for="comment in comments" :key="comment.id" class="comment-item">
              <div class="comment-top">
                <button v-if="comment.user?.id" class="comment-author" @click="openUserProfile(comment.user)">
                  <span class="comment-avatar">{{ getInitials(comment.user?.fullName) }}</span>
                  <strong>{{ comment.user?.fullName || 'Anonymous' }}</strong>
                </button>
                <strong v-else>{{ comment.user?.fullName || 'Anonymous' }}</strong>
                <el-rate :model-value="Number(comment.rating || 0)" disabled />
              </div>
              <p>{{ comment.comment || comment.content || 'No comment text' }}</p>
              <div v-if="canManageComment(comment)" class="comment-actions">
                <el-button size="small" text type="danger" @click="removeComment(comment)">
                  Delete
                </el-button>
              </div>
            </article>
            <el-empty v-if="!commentsLoading && comments.length === 0" description="No comments yet" />
          </div>
        </aside>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createPostReview,
  deletePostReview,
  getPost,
  getPostImages,
  getPostReviews
} from '@/api/post'
import { getEventDetail } from '@/api/events/detail'
import useUserStore from '@/store/modules/user'
import { canManagePostComment, canManagePostForUser } from '@/utils/accessControl'
import fallbackPostImage from '@/assets/images/login-background.jpg'
import defaultAvatar from '@/assets/images/profile.jpg'
import {
  ArrowLeft,
  Calendar,
  EditPen
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const postId = computed(() => route.query.id || route.params.id)
const loading = ref(false)
const commentsLoading = ref(false)
const submittingComment = ref(false)
const post = ref({})
const relatedEvent = ref(null)
const images = ref([])
const comments = ref([])
const commentForm = ref({
  rating: 5,
  comment: ''
})

const canManageLoadedPost = computed(() => canManagePostForUser(userStore.userInfo, post.value))

function extractList(res) {
  if (Array.isArray(res)) return res
  return res?.rows || res?.data || res?.list || res?.content || []
}

function normalizeImageUrl(value) {
  const url = String(value || '').trim()
  if (!url) return ''
  if (/^(https?:|data:|blob:)/i.test(url)) return url
  if (url.startsWith('//')) return `${window.location.protocol}${url}`
  if (url.startsWith('/')) return url
  const supabaseUrl = import.meta.env.VITE_SUPABASE_URL
  return supabaseUrl
    ? `${supabaseUrl.replace(/\/$/, '')}/storage/v1/object/public/${url.replace(/^\/+/, '')}`
    : url
}

function getAvatar(user) {
  return normalizeImageUrl(user?.photo) || defaultAvatar
}

function handleAvatarError(event) {
  event.target.src = defaultAvatar
}

function getInitials(name) {
  return String(name || '?').trim().slice(0, 1).toUpperCase() || '?'
}

function formatDate(value) {
  const date = new Date(value)
  if (!value || Number.isNaN(date.getTime())) return 'Date unavailable'
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

function backToPosts() {
  router.push({ name: 'MainPost' })
}

function editPost() {
  router.push({ name: 'CreatePost', query: { id: postId.value } })
}

function openUserProfile(user) {
  if (user?.id) {
    router.push(`/users/${user.id}`)
  }
}

function openEvent() {
  if (relatedEvent.value?.id) {
    router.push({ path: '/product/eventDetails', query: { id: relatedEvent.value.id } })
  }
}

function canManageComment(comment) {
  return canManagePostComment(comment, post.value, relatedEvent.value, userStore.userInfo)
}

async function loadComments() {
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

async function submitComment() {
  if (!postId.value) {
    ElMessage.error('Missing post id')
    return
  }
  if (!commentForm.value.comment.trim()) {
    ElMessage.warning('Please enter your comment')
    return
  }

  submittingComment.value = true
  try {
    await createPostReview(postId.value, {
      rating: commentForm.value.rating,
      comment: commentForm.value.comment.trim()
    })
    commentForm.value.rating = 5
    commentForm.value.comment = ''
    await loadComments()
    ElMessage.success('Comment submitted')
  } catch (error) {
    console.error('Failed to submit post comment:', error)
  } finally {
    submittingComment.value = false
  }
}

async function removeComment(comment) {
  if (!canManageComment(comment)) {
    ElMessage.warning('You do not have permission to delete this comment')
    return
  }

  try {
    await ElMessageBox.confirm('Delete this comment?', 'Delete Comment', {
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel',
      type: 'warning'
    })
    await deletePostReview(postId.value, comment.id)
    comments.value = comments.value.filter((item) => item.id !== comment.id)
    ElMessage.success('Comment deleted')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to delete post comment:', error)
    }
  }
}

async function loadPostDetail() {
  if (!postId.value) {
    ElMessage.error('Missing post id')
    return
  }

  loading.value = true
  try {
    if (!userStore.userInfo) {
      await userStore.getInfo()
    }
    const loadedPost = await getPost(postId.value)
    post.value = loadedPost || {}
    images.value = extractList(await getPostImages(postId.value))

    const eventId = loadedPost?.event?.id || loadedPost?.eventId || loadedPost?.relatedEventId
    if (loadedPost?.event) {
      relatedEvent.value = loadedPost.event
    } else if (eventId) {
      relatedEvent.value = await getEventDetail(eventId)
    }
    await loadComments()
  } catch (error) {
    console.error('Failed to load post detail:', error)
    ElMessage.error('Failed to load post detail')
  } finally {
    loading.value = false
  }
}

onMounted(loadPostDetail)
</script>

<style scoped lang="scss">
.post-detail-page {
  min-height: 100vh;
  background: #f7faff;
  color: #10234f;
}

.post-detail-shell {
  padding: 34px 44px;
}

.post-hero {
  margin-bottom: 24px;
}

.back-button {
  margin-bottom: 18px;
}

.author-strip {
  width: fit-content;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.author-strip img {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
}

.author-strip span,
.post-meta {
  color: #63739b;
}

.post-hero h1 {
  margin: 0;
  font-size: 42px;
  line-height: 1.15;
  letter-spacing: 0;
}

.post-meta {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.event-link {
  padding: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 0;
  background: transparent;
  color: #0f66e9;
  cursor: pointer;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 24px;
}

.post-card,
.comments-card {
  background: #fff;
  border: 1px solid #e0e8f6;
  border-radius: 8px;
  box-shadow: 0 10px 24px rgba(26, 54, 100, 0.08);
}

.post-card {
  padding: 22px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
  margin-bottom: 22px;
}

.image-grid img,
.image-placeholder img {
  width: 100%;
  aspect-ratio: 16 / 9;
  object-fit: cover;
  border-radius: 8px;
}

.post-content {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #263b67;
  font-size: 16px;
}

.post-actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.comments-card {
  padding: 20px;
  align-self: start;
}

.comments-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.comments-header h2 {
  margin: 0;
}

.comment-composer {
  display: grid;
  gap: 12px;
  margin-bottom: 18px;
  padding: 14px;
  border: 1px solid #e0e8f6;
  border-radius: 8px;
  background: #f8fbff;
}

.comment-composer .el-button {
  justify-self: end;
}

.comment-list {
  display: grid;
  gap: 14px;
}

.comment-item {
  padding-bottom: 14px;
  border-bottom: 1px solid #edf2fb;
}

.comment-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.comment-author {
  padding: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.comment-avatar {
  width: 32px;
  height: 32px;
  display: inline-grid;
  place-items: center;
  border-radius: 50%;
  background: #eaf2ff;
  color: #0f66e9;
}

.comment-actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 980px) {
  .post-detail-shell {
    padding: 22px;
  }

  .content-grid {
    grid-template-columns: 1fr;
  }

  .post-hero h1 {
    font-size: 32px;
  }
}
</style>
