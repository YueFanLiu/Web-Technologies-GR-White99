<template>
  <el-container class="event-detail-container">
    <!-- 左侧主内容区 -->
    <el-aside width="60%" class="main-content">
      <!-- 面包屑导航 -->
      <div class="breadcrumb-wrapper">
        <el-breadcrumb separator=">">
          <el-breadcrumb-item :to="{ path: '/' }">Home</el-breadcrumb-item>
          <el-breadcrumb-item>Activity Detail</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <!-- 标题 -->
      <h1 class="event-title">{{ eventDetail.title }}</h1>

      <!-- 活动主图 -->
      <div class="event-image-wrapper">
        <img
          :src="mainEventImage"
          :alt="eventDetail.title || 'Event image'"
          class="event-image"
          @error="handleEventImageError"
        />
      </div>

      <!-- 活动信息卡片 -->
      <div class="event-info-card">
        <h2 class="event-name">{{ eventDetail.title }}</h2>

        <div class="event-meta">
          <div class="meta-item">
            <el-icon>
              <Calendar/>
            </el-icon>
            <span>{{ formattedEventDate }}</span>
          </div>

          <div class="meta-item">
            <el-icon>
              <Clock/>
            </el-icon>
            <span>{{ formattedEventTime }}</span>
          </div>

          <div class="meta-item">
            <el-icon>
              <Location/>
            </el-icon>
            <span>{{ eventLocationName }}</span>
          </div>

          <div class="meta-item">
            <el-icon>
              <Location/>
            </el-icon>
            <span>{{ eventLocationAddress }}</span>
          </div>
        </div>

        <!-- 地图 -->
        <div class="map-wrapper">
          <img
              src="https://picsum.photos/id/1025/600/400"
              alt="Map"
              class="map-image"
          />
        </div>

        <!-- 活动描述 -->
        <div class="description-section">
          <h3>Description</h3>
          <p class="description-text">
            {{ eventDetail.description }}
          </p>
          <div class="event-facts">
            <el-tag v-if="eventDetail.category" type="info">{{ eventDetail.category }}</el-tag>
            <el-tag v-if="eventDetail.status" type="success">{{ eventDetail.status }}</el-tag>
            <el-tag v-if="eventDetail.id" type="warning">{{ eventDetail.isVirtual ? 'Virtual Event' : 'In-person Event' }}</el-tag>
          </div>
        </div>
      </div>
    </el-aside>

    <!-- 右侧边栏 -->
    <el-aside width="40%" class="sidebar">
      <!-- 预订卡片 -->
      <div class="booking-card">
        <div class="price-row">
          <div class="price-info">
            <span class="price">{{ formattedPrice }} per Child</span>
          </div>
          <span class="price-label">Price per Child</span>
        </div>

        <div class="quantity-row">
          <el-button circle size="small" @click="decreaseCount">-</el-button>
          <span class="count">{{ childCount }}</span>
          <el-button circle size="small" @click="increaseCount">+</el-button>
          <el-button type="primary" class="book-btn" @click="goBookActivity">Book Now</el-button>
        </div>

        <div class="spots-left">
          <el-icon>
            <Warning/>
          </el-icon>
          <span>{{ spotsLeft }} spots left</span>
        </div>
      </div>

      <!-- 无障碍信息卡片 -->
      <div class="accessibility-card">
        <h3>Accessibility Info</h3>
        <div class="accessibility-tags">
          <el-tag
            v-for="tag in accessibilityTags"
            :key="tag"
            type="success"
            class="access-tag"
          >
            <el-icon><Lock/></el-icon>
            {{ tag }}
          </el-tag>
          <el-tag
            v-if="accessibilityTags.length === 0"
            type="info"
            class="access-tag"
          >
            Accessibility info unavailable
          </el-tag>
        </div>
        <p v-if="accessibilityInfo.notes" class="access-notes">{{ accessibilityInfo.notes }}</p>
      </div>

      <!-- 评论区 -->
      <div class="reviews-card">
        <div class="reviews-header">
          <h3>Reviews</h3>
          <div class="reviews-actions">
            <div class="rating-info">
              <el-icon>
                <Star/>
              </el-icon>
              <span class="rating">{{ eventDetail.averageRating || 0 }}</span>
              <span class="review-count">{{ reviewCount }} reviews</span>
            </div>
            <el-button v-if="canReviewEvent" type="primary" size="small" @click="goWriteReview">
              Write Review
            </el-button>
          </div>
        </div>

        <div class="review-item" v-for="review in eventReviews" :key="review.id">
          <div class="reviewer-info">
            <img
              :src="normalizeImageUrl(review.user?.photo || review.userAvatar) || fallbackAvatarImage"
              :alt="review.user?.fullName || 'Reviewer'"
              class="reviewer-avatar"
              @error="handleAvatarImageError"
            />
            <div class="reviewer-name">{{ review.user?.fullName || 'Anonymous' }}</div>
            <div class="review-stars">
              <el-icon v-for="i in getReviewStars(review.rating)" :key="i" color="#f7ba2a">
                <Star/>
              </el-icon>
            </div>
          </div>
          <p class="review-text">{{ review.comment || review.content }}</p>
          <div v-if="canManageReview(review)" class="review-actions">
            <el-button size="small" text type="primary" @click="editReview(review)">Edit</el-button>
            <el-button size="small" text type="danger" @click="removeReview(review)">Delete</el-button>
          </div>
        </div>
        <p v-if="eventReviews.length === 0" class="review-text">No reviews yet.</p>
      </div>
    </el-aside>
  </el-container>
</template>

<script setup>
import {ref, computed, onMounted} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage, ElMessageBox} from 'element-plus'
import {
  deleteEventReview,
  getEventDetail,
  getEventImages,
  getEventReviews,
  getEventRegistrations,
  getCurrentUserProfile,
  getRegistrationsByUser,
  getLocationAccessibility,
  updateEventReview,
} from '@/api/events/detail'
import {
  Calendar,
  Clock,
  Location,
  Lock,
  Warning,
  Star,
} from '@element-plus/icons-vue'
import fallbackEventImage from '@/assets/images/login-background.jpg'
import fallbackAvatarImage from '@/assets/images/profile.jpg'
import useUserStore from '@/store/modules/user'
import {
  canManageActivityForUser,
  canManageEventReview,
  canWriteReview
} from '@/utils/accessControl'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const eventId = route.query.id
const loading = ref(false)
const eventDetail = ref({})

const eventImages = ref([])
const eventReviews = ref([])
const eventRegistrations = ref([])
const currentRegistration = ref(null)
const accessibilityInfo = ref({})
const accessibilityTags = ref([])

// Swagger 中活动图片有两个来源：
// 1. GET /api/events/{id} 返回的 coverImageUrl / imageUrls
// 2. GET /api/events/{eventId}/images 返回的 ImageResponse[]，字段为 imageUrl
const mainEventImage = computed(() => {
  return normalizeImageUrl(eventDetail.value.coverImageUrl)
    || getImageUrl(eventImages.value[0])
    || normalizeImageUrl(eventDetail.value.imageUrls?.[0])
    || fallbackEventImage
})

const formattedEventDate = computed(() => {
  return formatEventDate(eventDetail.value.startTime)
})

const formattedEventTime = computed(() => {
  return formatEventTimeRange(eventDetail.value.startTime, eventDetail.value.endTime)
})

const formattedPrice = computed(() => {
  const price = Number(eventDetail.value.price || 0)
  return price === 0 ? 'Free' : `$${price.toFixed(2)}`
})

const eventLocationName = computed(() => {
  const location = eventDetail.value.location
  return location?.name || location?.city || 'Location TBA'
})

const eventLocationAddress = computed(() => {
  const location = eventDetail.value.location
  return [location?.address, location?.city, location?.country].filter(Boolean).join(', ') || 'Address TBA'
})

const activeRegistrationsCount = computed(() => {
  return eventRegistrations.value.filter(isActiveRegistration).length
})

const spotsLeft = computed(() => {
  const capacity = Number(eventDetail.value.capacity || 0)
  return Math.max(capacity - activeRegistrationsCount.value, 0)
})

const reviewCount = computed(() => {
  return eventDetail.value.reviewCount ?? eventReviews.value.length
})

const canReviewEvent = computed(() => {
  return canWriteReview(eventDetail.value, currentRegistration.value, userStore.userInfo)
})

// 儿童数量
const childCount = ref(1)

const fetchEventDetail = () => {
  if (!eventId) {
    ElMessage.error('Missing event id')
    return
  }

  loading.value = true

  getEventDetail(eventId).then(res => {
    console.log('event detail:', res)
    eventDetail.value = res

    fetchLocationAccessibility(res.location?.id)
    fetchCurrentUserRegistration(res)

    if (canManageActivityForUser(userStore.userInfo, res)) {
      fetchEventRegistrations()
    } else {
      eventRegistrations.value = []
    }
  }).catch(error => {
    console.error('Failed to load event detail:', error)
  }).finally(() => {
    loading.value = false
  })
}

const increaseCount = () => {
  childCount.value++
}

const decreaseCount = () => {
  if (childCount.value > 1) {
    childCount.value--
  }
}

const goBookActivity = () => {
  router.push({
    path: '/product/bookActivity',
    query: {
      eventId,
      quantity: childCount.value
    }
  })
}

const goWriteReview = () => {
  if (!eventId) {
    ElMessage.error('Missing event id')
    return
  }

  if (!canReviewEvent.value) {
    ElMessage.warning('You can review only confirmed events you attended after they have ended.')
    return
  }

  router.push({
    path: '/product/writeReview',
    query: { eventId }
  })
}

onMounted(() => {
  fetchEventDetail()
  fetchEventImages()
  fetchEventReviews()
})

const fetchEventImages = () => {
  if (!eventId) {
    return
  }

  getEventImages(eventId).then(res => {
    console.log('event images:', res)
    eventImages.value = Array.isArray(res) ? res : []
  }).catch(error => {
    console.error('Failed to load event images:', error)
  })
}

const getImageUrl = (image) => {
  return normalizeImageUrl(image?.imageUrl || image?.url || image?.publicUrl || image?.path)
}

const normalizeImageUrl = (value) => {
  const url = String(value || '').trim()

  if (!url) {
    return ''
  }

  if (/^(https?:|data:|blob:)/i.test(url)) {
    return url
  }

  if (url.startsWith('//')) {
    return `${window.location.protocol}${url}`
  }

  if (url.startsWith('/')) {
    return url
  }

  const supabaseUrl = import.meta.env.VITE_SUPABASE_URL
  if (supabaseUrl) {
    return `${supabaseUrl.replace(/\/$/, '')}/storage/v1/object/public/${url.replace(/^\/+/, '')}`
  }

  return url
}

const handleEventImageError = (event) => {
  if (event.target.src !== fallbackEventImage) {
    event.target.src = fallbackEventImage
  }
}

const handleAvatarImageError = (event) => {
  if (event.target.src !== fallbackAvatarImage) {
    event.target.src = fallbackAvatarImage
  }
}

const fetchEventReviews = () => {
  if (!eventId) {
    return
  }

  getEventReviews(eventId).then(res => {
    console.log('event reviews:', res)
    eventReviews.value = Array.isArray(res) ? res : []
  }).catch(error => {
    console.error('Failed to load event reviews:', error)
  })
}

const fetchEventRegistrations = () => {
  if (!eventId) {
    return
  }

  getEventRegistrations(eventId).then(res => {
    console.log('event registrations:', res)
    eventRegistrations.value = Array.isArray(res) ? res : []
  }).catch(error => {
    console.error('Failed to load event registrations:', error)
  })
}

const fetchLocationAccessibility = (locationId) => {
  if (!locationId) {
    return
  }

  getLocationAccessibility(locationId).then(res => {
    console.log('location accessibility:', res)
    accessibilityInfo.value = res || {}
    accessibilityTags.value = [
      res?.wheelchairAccessible ? 'Wheelchair Accessible' : null,
      res?.hasElevator ? 'Elevator' : null,
      res?.accessibleToilet ? 'Accessible Toilet' : null,
      res?.quietEnvironment ? 'Quiet Environment' : null,
      res?.stepFreeAccess ? 'Step-free Access' : null
    ].filter(Boolean)
  }).catch(error => {
    console.error('Failed to load location accessibility:', error)
  })
}

const fetchCurrentUserRegistration = async (event) => {
  currentRegistration.value = null

  try {
    if (!userStore.userInfo) {
      await userStore.getInfo()
    }

    const profile = userStore.userInfo || await getCurrentUserProfile()
    const userId = profile?.id || profile?.userId || profile?.user?.id || profile?.profile?.id
    if (!userId) {
      return
    }

    const registrations = await getRegistrationsByUser(userId)
    const list = Array.isArray(registrations)
      ? registrations
      : registrations?.rows || registrations?.data || registrations?.list || registrations?.content || []
    currentRegistration.value = list.find((registration) => {
      const registrationEventId = registration.event?.id || registration.eventId
      return String(registrationEventId || '') === String(event?.id || eventId || '')
    }) || null
  } catch (error) {
    console.error('Failed to load current user registration:', error)
  }
}

const canManageReview = (review) => {
  return canManageEventReview(review, eventDetail.value, userStore.userInfo)
}

const editReview = async (review) => {
  if (!canManageReview(review)) {
    ElMessage.warning('You do not have permission to perform this action.')
    return
  }

  try {
    const { value } = await ElMessageBox.prompt('Update your review text', 'Edit Review', {
      confirmButtonText: 'Save',
      cancelButtonText: 'Cancel',
      inputType: 'textarea',
      inputValue: review.comment || review.content || '',
      inputValidator: (value) => Boolean(String(value || '').trim()),
      inputErrorMessage: 'Please enter a review'
    })

    const updated = await updateEventReview(eventId, review.id, {
      rating: review.rating || 1,
      comment: value.trim()
    })
    eventReviews.value = eventReviews.value.map((item) => item.id === review.id ? updated : item)
    ElMessage.success('Review updated')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to update review:', error)
    }
  }
}

const removeReview = async (review) => {
  if (!canManageReview(review)) {
    ElMessage.warning('You do not have permission to perform this action.')
    return
  }

  try {
    await ElMessageBox.confirm('Delete this review?', 'Delete Review', {
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel',
      type: 'warning'
    })
    await deleteEventReview(eventId, review.id)
    eventReviews.value = eventReviews.value.filter((item) => item.id !== review.id)
    ElMessage.success('Review deleted')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to delete review:', error)
    }
  }
}

const formatEventDate = (value) => {
  if (!value) {
    return 'Date TBA'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return 'Date TBA'
  }

  return date.toLocaleDateString('en-US', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

const formatEventTimeRange = (startValue, endValue) => {
  if (!startValue) {
    return 'Time TBA'
  }

  const start = new Date(startValue)
  if (Number.isNaN(start.getTime())) {
    return 'Time TBA'
  }

  const options = { hour: 'numeric', minute: '2-digit' }
  const startText = start.toLocaleTimeString('en-US', options)
  if (!endValue) {
    return startText
  }

  const end = new Date(endValue)
  return Number.isNaN(end.getTime())
    ? startText
    : `${startText} - ${end.toLocaleTimeString('en-US', options)}`
}

const isActiveRegistration = (registration) => {
  const status = String(registration.status || '').toUpperCase()
  return !['CANCELLED', 'CANCELED', 'REJECTED'].includes(status)
}

const getReviewStars = (rating) => {
  const value = Number(rating || 0)
  return Math.max(0, Math.min(5, Math.round(value)))
}

</script>

<style scoped>
.event-detail-container {
  background-color: #f8faff;
  padding: 20px;
  min-height: 100vh;
}

/* 面包屑 */
.breadcrumb-wrapper {
  margin-bottom: 16px;
  color: #666;
}

/* 标题 */
.event-title {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin: 16px 0;
}

/* 活动主图 */
.event-image-wrapper {
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 24px;
}

.event-image {
  width: 100%;
  height: auto;
  display: block;
}

/* 左侧信息卡片 */
.event-info-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.event-name {
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 16px;
}

.event-meta {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #555;
}

.map-wrapper {
  margin: 20px 0;
}

.map-image {
  width: 100%;
  border-radius: 8px;
}

.description-section h3 {
  font-size: 20px;
  margin-bottom: 12px;
}

.description-text {
  color: #555;
  line-height: 1.6;
}

.event-facts {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

/* 右侧边栏 */
.sidebar {
  padding-left: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 预订卡片 */
.booking-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.price {
  font-size: 18px;
  font-weight: 600;
}

.price-label {
  color: #666;
}

.quantity-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.count {
  font-size: 18px;
  font-weight: 500;
  width: 30px;
  text-align: center;
}

.book-btn {
  margin-left: auto;
  padding: 12px 32px;
  font-size: 16px;
}

.spots-left {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #e6a23c;
  font-size: 14px;
}

/* 无障碍信息卡片 */
.accessibility-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.accessibility-card h3 {
  margin-bottom: 16px;
}

.accessibility-tags {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.access-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.access-notes {
  margin: 14px 0 0;
  color: #555;
  line-height: 1.6;
  font-size: 14px;
}

/* 评论卡片 */
.reviews-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.reviews-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.reviews-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.rating-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.rating {
  font-weight: 600;
}

.review-count {
  color: #666;
  font-size: 14px;
}

.review-item {
  margin-bottom: 16px;
}

.reviewer-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.reviewer-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
}

.reviewer-name {
  font-weight: 500;
}

.review-stars {
  display: flex;
  gap: 2px;
}

.review-text {
  color: #555;
  font-size: 14px;
}

.review-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
</style>
