<template>
  <div class="write-review-page">
    <main class="review-shell">
      <section class="page-heading">
        <el-button class="back-icon" @click="cancelReview">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <h1>Write Review</h1>
      </section>

      <section class="review-card" v-loading="loading">
        <div class="event-summary">
          <img :src="event.image" :alt="event.title" class="event-image" @error="handleEventImageError" />

          <div class="event-copy">
            <h2>{{ event.title }}</h2>
            <div class="event-meta">
              <p>
                <el-icon><Calendar /></el-icon>
                <span>{{ event.date }}</span>
              </p>
              <p>
                <el-icon><Clock /></el-icon>
                <span>{{ event.time }}</span>
              </p>
              <p>
                <el-icon><Location /></el-icon>
                <span>{{ event.location }}</span>
              </p>
            </div>
          </div>
        </div>

        <section class="rating-section">
          <h3>Rating Section</h3>
          <el-rate
            v-model="rating"
            :max="5"
            size="large"
            void-color="#2f75f6"
            :colors="['#2f75f6', '#2f75f6', '#2f75f6']"
          />
          <p>Tap a star to rate</p>
        </section>

        <el-divider />

        <section class="review-section">
          <div class="section-header">
            <h3>Review Text</h3>
            <span>{{ reviewText.length }} / 1000</span>
          </div>

          <el-input
            v-model="reviewText"
            type="textarea"
            :maxlength="1000"
            :rows="7"
            resize="none"
            placeholder="Share your experience about the activity, accessibility, organization, and suggestions."
          />
        </section>

        <div class="action-row">
          <el-button size="large" class="cancel-button" @click="cancelReview">
            Cancel
          </el-button>
          <el-button
            size="large"
            type="primary"
            class="submit-button"
            :loading="submitting"
            @click="submitReview"
          >
            Submit Review
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  createEventReview,
  getCurrentUserProfile,
  getEventDetail,
  getEventImages,
  getRegistrationsByUser
} from '@/api/events/WriteReview'
import { ArrowLeft, ArrowRight, Calendar, Clock, Location } from '@element-plus/icons-vue'
import fallbackEventImage from '@/assets/images/login-background.jpg'
import useUserStore from '@/store/modules/user'
import { canWriteReview } from '@/utils/accessControl'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const eventId = computed(() => route.query.eventId || route.query.id)

const rating = ref(0)
const reviewText = ref('')
const loading = ref(false)
const submitting = ref(false)
const rawEvent = ref(null)
const currentRegistration = ref(null)
const event = ref({
  title: 'Loading event...',
  date: 'Date TBA',
  time: 'Time TBA',
  location: 'Location TBA',
  image: fallbackEventImage
})

function cancelReview() {
  router.push({
    path: '/product/eventDetails',
    query: eventId.value ? { id: eventId.value } : {}
  })
}

async function submitReview() {
  if (!canWriteReview(rawEvent.value, currentRegistration.value, userStore.userInfo)) {
    ElMessage.warning('You can review only confirmed events you attended after they have ended.')
    return
  }

  if (!rating.value) {
    ElMessage.error('Please select a rating')
    return
  }

  if (!reviewText.value.trim()) {
    ElMessage.error('Please enter your review')
    return
  }

  submitting.value = true

  try {
    await createEventReview(eventId.value, {
      rating: rating.value,
      comment: reviewText.value.trim()
    })
    ElMessage.success('Review submitted')
    cancelReview()
  } catch (error) {
    console.error('Failed to submit review:', error)
  } finally {
    submitting.value = false
  }
}

async function loadPage() {
  if (!eventId.value) {
    ElMessage.error('Missing event id')
    cancelReview()
    return
  }

  loading.value = true

  try {
    await loadEvent()
    await loadCurrentRegistration()
  } catch (error) {
    console.error('Failed to load review page:', error)
  } finally {
    loading.value = false
  }
}

async function loadEvent() {
  const detail = await getEventDetail(eventId.value)
  rawEvent.value = detail
  const images = await getEventImages(eventId.value).catch(() => [])
  const imageUrl = detail.coverImageUrl || images?.[0]?.imageUrl || detail.imageUrls?.[0]

  event.value = {
    title: detail.title || 'Untitled event',
    date: formatEventDate(detail.startTime),
    time: formatEventTimeRange(detail.startTime, detail.endTime),
    location: formatLocation(detail.location),
    image: normalizeImageUrl(imageUrl) || fallbackEventImage
  }
}

async function loadCurrentRegistration() {
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
      return String(registrationEventId || '') === String(eventId.value || '')
    }) || null
  } catch (error) {
    console.error('Failed to load review eligibility:', error)
  }
}

function formatLocation(location) {
  if (!location) {
    return 'Location TBA'
  }

  return [location.name, location.address, location.city, location.country].filter(Boolean).join(', ')
    || 'Location TBA'
}

function formatEventDate(value) {
  if (!value) {
    return 'Date TBA'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return 'Date TBA'
  }

  return date.toLocaleDateString('en-US', {
    weekday: 'short',
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

function formatEventTimeRange(startValue, endValue) {
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

function normalizeImageUrl(value) {
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

function handleEventImageError(event) {
  if (event.target.src !== fallbackEventImage) {
    event.target.src = fallbackEventImage
  }
}

onMounted(loadPage)
</script>

<style scoped lang="scss">
.write-review-page {
  min-height: 100vh;
  background: #f7faff;
  color: #10234f;
  font-family: Inter, "Segoe UI", Arial, sans-serif;
}

.review-shell {
  padding: 34px 44px 48px;
}

.page-heading {
  width: min(900px, 100%);
  margin: 0 auto 20px;
  display: flex;
  align-items: center;
  gap: 28px;
}

.back-icon {
  width: 48px;
  height: 48px;
  padding: 0;
  border-color: #d6dfef;
  border-radius: 10px;
  color: #0f66e9;
  background: #fff;
  font-size: 22px;
}

.page-heading h1 {
  margin: 0;
  color: #0d1f4f;
  font-size: 38px;
  line-height: 1.15;
  font-weight: 800;
  letter-spacing: 0;
}

.review-card {
  width: min(900px, 100%);
  margin: 0 auto;
  padding: 26px 28px 24px;
  background: rgba(255, 255, 255, 0.97);
  border: 1px solid #e0e8f6;
  border-radius: 16px;
  box-shadow: 0 18px 38px rgba(26, 54, 100, 0.1);
}

.event-summary {
  padding: 22px;
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
  gap: 26px;
  align-items: center;
  border-radius: 14px;
  background: #f5f9ff;
}

.event-image {
  width: 100%;
  height: 108px;
  border-radius: 10px;
  object-fit: cover;
}

.event-copy h2 {
  margin: 0 0 18px;
  color: #071a47;
  font-size: 24px;
  line-height: 1.25;
  font-weight: 800;
}

.event-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px 22px;
}

.event-meta p {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #2a3b62;
  font-size: 15px;
}

.event-meta .el-icon {
  color: #0f66e9;
  font-size: 18px;
}

.rating-section {
  margin-top: 28px;
}

.rating-section h3,
.review-section h3,
.upload-section h3 {
  margin: 0 0 14px;
  color: #071a47;
  font-size: 19px;
  font-weight: 800;
}

.rating-section :deep(.el-rate) {
  height: 44px;
}

.rating-section :deep(.el-rate__icon) {
  margin-right: 20px;
  font-size: 36px;
}

.rating-section p {
  margin: 6px 0 0;
  color: #7886a4;
  font-size: 14px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.section-header span {
  color: #7886a4;
  font-size: 13px;
}

.review-section :deep(.el-textarea__inner) {
  min-height: 160px;
  padding: 18px;
  border-radius: 12px;
  color: #26365f;
  font-size: 15px;
  line-height: 1.7;
  box-shadow: 0 0 0 1px #d6dfef inset;
}

.upload-section {
  margin-top: 24px;
}

.upload-section h3 span {
  color: #7c89a5;
  font-weight: 500;
}

.upload-section > p {
  margin: -6px 0 14px;
  color: #7886a4;
  font-size: 14px;
}

.photo-upload :deep(.el-upload--picture-card),
.photo-upload :deep(.el-upload-list__item) {
  width: 148px;
  height: 92px;
  border-radius: 12px;
  background: #f8fbff;
  border-color: #bcd0ef;
}

.photo-upload :deep(.el-upload--picture-card .el-icon) {
  color: #0f66e9;
  font-size: 25px;
}

.upload-note {
  margin: 8px 0 0;
  color: #8a96af;
  font-size: 13px;
}

.action-row {
  margin-top: 24px;
  padding-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 20px;
  border-top: 1px solid #e1e8f3;
}

.cancel-button,
.submit-button {
  width: 210px;
  height: 52px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 700;
}

.cancel-button {
  border-color: #2f75f6;
  color: #0f66e9;
  background: #fff;
}

.submit-button {
  box-shadow: 0 10px 22px rgba(47, 117, 246, 0.22);
}

@media (max-width: 820px) {
  .review-shell {
    padding: 28px 24px 40px;
  }

  .event-summary {
    grid-template-columns: 1fr;
  }

  .event-image {
    height: 180px;
  }

  .action-row {
    display: grid;
    grid-template-columns: 1fr;
  }

  .cancel-button,
  .submit-button {
    width: 100%;
  }
}

@media (max-width: 560px) {
  .review-shell {
    padding: 24px 16px 32px;
  }

  .page-heading {
    gap: 16px;
  }

  .page-heading h1 {
    font-size: 32px;
  }

  .review-card {
    padding: 20px;
  }
}
</style>
