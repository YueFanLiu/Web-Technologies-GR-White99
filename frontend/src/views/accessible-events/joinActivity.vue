<template>
  <div class="joined-activities-page">
    <main class="joined-shell">
      <section class="page-heading">
        <div>
          <h1>Joined Activities</h1>
          <p>Activities you booked, attended, or saved for later.</p>
        </div>
        <div class="heading-actions">
          <el-button :loading="loading" @click="loadActivities">
            <el-icon><Refresh /></el-icon>
            Refresh
          </el-button>
          <el-button type="primary" class="browse-button" @click="browseActivities">
            Browse Activities
          </el-button>
        </div>
      </section>

      <section class="status-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          class="status-tab"
          :class="{ active: activeTab === tab.value }"
          type="button"
          @click="activeTab = tab.value"
        >
          <span>{{ tab.label }}</span>
          <strong>{{ countByTab(tab.value) }}</strong>
        </button>
      </section>

      <el-alert
        v-if="loadError"
        class="load-alert"
        :title="loadError"
        type="error"
        show-icon
        :closable="false"
      />

      <section class="activity-card-list" v-loading="loading">
        <article v-for="activity in filteredActivities" :key="activity.key" class="activity-card">
          <img
            :src="activity.image"
            :alt="activity.title"
            class="activity-image"
            @error="handleActivityImageError"
          />

          <div class="activity-body">
            <div class="activity-title-row">
              <div>
                <h2>{{ activity.title }}</h2>
                <p>{{ activity.location }}</p>
              </div>
              <el-tag class="status-pill" :class="activity.tab.toLowerCase()" effect="plain">
                {{ activity.tab }}
              </el-tag>
            </div>

            <div class="activity-meta">
              <span>
                <el-icon><Calendar /></el-icon>
                {{ activity.date }}
              </span>
              <span>
                <el-icon><Clock /></el-icon>
                {{ activity.time }}
              </span>
              <span>
                <el-icon><Tickets /></el-icon>
                {{ activity.statusLabel }}
              </span>
            </div>

            <div class="activity-actions">
              <el-button @click="viewDetails(activity)">View Details</el-button>
              <el-button
                v-if="activity.tab === 'Upcoming'"
                class="danger-button"
                :loading="cancellingId === activity.registrationId"
                @click="cancelBooking(activity)"
              >
                Cancel Booking
              </el-button>
              <el-button
                v-if="activity.tab === 'Past'"
                type="primary"
                @click="leaveReview(activity)"
              >
                Leave Review
              </el-button>
            </div>
          </div>
        </article>

        <el-empty
          v-if="!loading && filteredActivities.length === 0"
          :description="emptyDescription"
        >
          <el-button type="primary" @click="browseActivities">Browse Activities</el-button>
        </el-empty>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Clock, Refresh, Tickets } from '@element-plus/icons-vue'
import fallbackEventImage from '@/assets/images/login-background.jpg'
import {
  deleteRegistration,
  getCurrentUserProfile,
  getEventDetail,
  getRegistrationsByUser
} from '@/api/events/joinActivity'

const router = useRouter()
const loading = ref(false)
const cancellingId = ref('')
const activeTab = ref('Upcoming')
const activities = ref([])
const loadError = ref('')

const tabs = [
  { label: 'Upcoming', value: 'Upcoming' },
  { label: 'Past', value: 'Past' },
  { label: 'Saved', value: 'Saved' }
]

const filteredActivities = computed(() => {
  return activities.value.filter((activity) => activity.tab === activeTab.value)
})

const emptyDescription = computed(() => {
  return loadError.value || `No ${activeTab.value.toLowerCase()} activities found`
})

function unwrapResponse(res) {
  return res?.data ?? res
}

function extractList(res) {
  const data = unwrapResponse(res)
  if (Array.isArray(data)) return data
  return data?.rows || data?.list || data?.content || []
}

function countByTab(tab) {
  return activities.value.filter((activity) => activity.tab === tab).length
}

function formatDate(value) {
  const date = new Date(value)
  if (!value || Number.isNaN(date.getTime())) return 'Date TBA'
  return date.toLocaleDateString('en-US', {
    weekday: 'short',
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

function formatTime(startValue, endValue) {
  const start = new Date(startValue)
  const end = new Date(endValue)
  if (!startValue || Number.isNaN(start.getTime())) return 'Time TBA'

  const options = { hour: 'numeric', minute: '2-digit' }
  if (!endValue || Number.isNaN(end.getTime())) return start.toLocaleTimeString('en-US', options)
  return `${start.toLocaleTimeString('en-US', options)} - ${end.toLocaleTimeString('en-US', options)}`
}

function normalizeImageUrl(value) {
  const url = String(value || '').trim()
  if (!url) return ''
  if (/^(https?:|data:|blob:)/i.test(url)) return url
  if (url.startsWith('//')) return `${window.location.protocol}${url}`
  if (url.startsWith('/')) return url

  const supabaseUrl = import.meta.env.VITE_SUPABASE_URL
  if (supabaseUrl) {
    return `${supabaseUrl.replace(/\/$/, '')}/storage/v1/object/public/${url.replace(/^\/+/, '')}`
  }

  return url
}

function getActivityImage(event, registrationId) {
  return normalizeImageUrl(event.coverImageUrl)
    || normalizeImageUrl(event.imageUrls?.[0])
    || `https://picsum.photos/seed/joined-${encodeURIComponent(event.id || registrationId || 'activity')}/420/260`
}

function normalizeStatus(value) {
  return String(value || 'REGISTERED').trim().toUpperCase()
}

function getStatusLabel(status) {
  const labels = {
    REGISTERED: 'Registered',
    CONFIRMED: 'Confirmed',
    ATTENDED: 'Attended',
    COMPLETED: 'Completed',
    SAVED: 'Saved'
  }
  return labels[status] || status.charAt(0) + status.slice(1).toLowerCase()
}

function getActivityTab(status, startTime, endTime) {
  if (status === 'SAVED') return 'Saved'
  if (['ATTENDED', 'COMPLETED'].includes(status)) return 'Past'

  const compareTime = endTime || startTime
  const date = compareTime ? new Date(compareTime) : null
  return date && !Number.isNaN(date.getTime()) && date.getTime() < Date.now()
    ? 'Past'
    : 'Upcoming'
}

function getLocationText(location) {
  return [location?.name, location?.address, location?.city, location?.country]
    .filter(Boolean)
    .join(', ') || 'Location TBA'
}

function normalizeActivity(registration) {
  const event = registration.event || {}
  const startTime = event.startTime || registration.startTime
  const endTime = event.endTime || registration.endTime
  const status = normalizeStatus(registration.status)

  return {
    key: `${registration.id || event.id}-${status}`,
    registrationId: registration.id,
    eventId: event.id || registration.eventId,
    title: event.title || registration.eventTitle || 'Untitled activity',
    date: formatDate(startTime),
    time: formatTime(startTime, endTime),
    location: getLocationText(event.location || registration.location),
    image: getActivityImage(event, registration.id),
    status,
    statusLabel: getStatusLabel(status),
    tab: getActivityTab(status, startTime, endTime),
    raw: registration
  }
}

async function hydrateMissingEvents(items) {
  return Promise.all(items.map(async (registration) => {
    if (registration.event?.id || !registration.eventId) return registration

    try {
      const event = unwrapResponse(await getEventDetail(registration.eventId))
      return { ...registration, event }
    } catch (error) {
      console.error('Failed to load event for registration:', registration.eventId, error)
      return registration
    }
  }))
}

async function loadActivities() {
  loading.value = true
  loadError.value = ''

  try {
    const profile = unwrapResponse(await getCurrentUserProfile())
    const userId = profile?.id || profile?.userId || profile?.user?.id || profile?.profile?.id
    if (!userId) {
      activities.value = []
      loadError.value = 'Unable to load current user profile'
      return
    }

    const registrations = extractList(await getRegistrationsByUser(userId))
      .filter((registration) => !['CANCELLED', 'CANCELED', 'REJECTED'].includes(normalizeStatus(registration.status)))

    const hydratedRegistrations = await hydrateMissingEvents(registrations)
    activities.value = hydratedRegistrations.map(normalizeActivity)
  } catch (error) {
    console.error('Failed to load joined activities:', error)
    loadError.value = 'Failed to load joined activities'
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

function viewDetails(activity) {
  router.push({
    path: '/product/eventDetails',
    query: activity.eventId ? { id: activity.eventId } : {}
  })
}

async function cancelBooking(activity) {
  if (!activity.registrationId) {
    ElMessage.warning('Missing booking id')
    return
  }

  try {
    await ElMessageBox.confirm(`Cancel booking for "${activity.title}"?`, 'Cancel Booking', {
      confirmButtonText: 'Confirm',
      cancelButtonText: 'Keep Booking',
      type: 'warning'
    })

    cancellingId.value = activity.registrationId
    await deleteRegistration(activity.registrationId)
    activities.value = activities.value.filter((item) => item.registrationId !== activity.registrationId)
    ElMessage.success('Booking cancelled')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to cancel booking:', error)
      ElMessage.error('Failed to cancel booking')
    }
  } finally {
    cancellingId.value = ''
  }
}

function leaveReview(activity) {
  router.push({
    path: '/product/writeReview',
    query: activity.eventId ? { eventId: activity.eventId } : {}
  })
}

function browseActivities() {
  router.push('/product/mainEvent')
}

function handleActivityImageError(event) {
  if (event.target.src !== fallbackEventImage) {
    event.target.src = fallbackEventImage
  }
}

onMounted(loadActivities)
</script>

<style scoped lang="scss">
.joined-activities-page {
  min-height: 100vh;
  background: #f7faff;
  color: #10234f;
  font-family: Inter, "Segoe UI", Arial, sans-serif;
}

.joined-shell {
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
  letter-spacing: 0;
}

.page-heading p {
  margin: 10px 0 0;
  color: #4f5d7c;
  font-size: 16px;
}

.heading-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.heading-actions :deep(.el-button),
.browse-button {
  height: 44px;
  border-radius: 8px;
  font-weight: 700;
}

.status-tabs {
  margin-bottom: 22px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.status-tab {
  min-height: 74px;
  padding: 16px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid #d8e4f5;
  border-radius: 8px;
  color: #1d3264;
  background: #fff;
  cursor: pointer;
  font: inherit;
  font-weight: 700;
}

.status-tab strong {
  color: #0f66e9;
  font-size: 24px;
}

.status-tab.active {
  border-color: #409eff;
  background: #eef5ff;
}

.load-alert {
  margin-bottom: 16px;
}

.activity-card-list {
  min-height: 220px;
  display: grid;
  gap: 16px;
}

.activity-card {
  padding: 16px;
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 20px;
  border: 1px solid #e0e8f6;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(26, 54, 100, 0.07);
}

.activity-image {
  width: 100%;
  height: 150px;
  border-radius: 6px;
  object-fit: cover;
}

.activity-body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.activity-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.activity-title-row h2 {
  margin: 0;
  color: #071a47;
  font-size: 22px;
  line-height: 1.25;
  font-weight: 800;
}

.activity-title-row p {
  margin: 8px 0 0;
  color: #4f5d7c;
  font-size: 14px;
}

.status-pill {
  height: 32px;
  border: 0;
  border-radius: 8px;
  font-weight: 800;
}

.status-pill.upcoming {
  color: #0f66e9;
  background: #e8f1ff;
}

.status-pill.past {
  color: #637089;
  background: #eef1f5;
}

.status-pill.saved {
  color: #b7791f;
  background: #fff5df;
}

.activity-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 26px;
  color: #344466;
  font-size: 15px;
}

.activity-meta span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.activity-meta .el-icon {
  color: #0f66e9;
}

.activity-actions {
  margin-top: auto;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.activity-actions :deep(.el-button) {
  margin-left: 0;
  border-radius: 8px;
  font-weight: 700;
}

.danger-button {
  border-color: #ff8c99;
  color: #ec2d3d;
  background: #fff;
}

@media (max-width: 820px) {
  .joined-shell {
    padding: 28px 24px 40px;
  }

  .page-heading,
  .activity-title-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .heading-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .status-tabs,
  .activity-card {
    grid-template-columns: 1fr;
  }

  .activity-image {
    height: 210px;
  }
}

@media (max-width: 560px) {
  .joined-shell {
    padding: 24px 16px 32px;
  }

  .page-heading h1 {
    font-size: 34px;
  }
}
</style>
