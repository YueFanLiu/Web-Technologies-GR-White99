<template>
  <div class="created-activities-page">
    <main class="created-shell">
      <section class="page-heading">
        <div>
          <h1>Created Activities</h1>
          <p>Activities you created and manage.</p>
        </div>
        <el-button v-if="canCreateActivity" type="primary" class="create-button" @click="createActivity">
          Create Activity
        </el-button>
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
          <strong>{{ countByStatus(tab.value) }}</strong>
        </button>
      </section>

      <section class="activity-card-list" v-loading="loading">
        <article v-for="activity in filteredActivities" :key="activity.id" class="activity-card">
          <img :src="activity.image" :alt="activity.title" class="activity-image" />

          <div class="activity-body">
            <div class="activity-title-row">
              <div>
                <h2>{{ activity.title }}</h2>
                <p>{{ activity.location }}</p>
              </div>
              <el-tag class="status-pill" :class="activity.status.toLowerCase()" effect="plain">
                {{ activity.statusLabel }}
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
                <el-icon><User /></el-icon>
                {{ activity.registered }} / {{ activity.capacity }} attendees
              </span>
            </div>

            <div class="activity-actions">
              <el-button type="primary" @click="manageActivity(activity)">Manage</el-button>
              <el-button @click="viewAttendees(activity)">View Attendees</el-button>
            </div>
          </div>
        </article>

        <el-empty
          v-if="!loading && filteredActivities.length === 0"
          :description="`No ${activeTab.toLowerCase()} activities found`"
        />
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar, Clock, User } from '@element-plus/icons-vue'
import {
  listEvent
} from '@/api/events/index'
import { getEventRegistrations } from '@/api/manager/manageActivity'
import useUserStore from '@/store/modules/user'
import { canCreateActivityForUser, canManageActivityForUser } from '@/utils/accessControl'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const activeTab = ref('PUBLISHED')
const activities = ref([])
const canCreateActivity = computed(() => canCreateActivityForUser(userStore.userInfo))

const tabs = [
  { label: 'Published', value: 'PUBLISHED' },
  { label: 'Draft', value: 'DRAFT' },
  { label: 'Cancelled', value: 'CANCELLED' }
]

const filteredActivities = computed(() => {
  return activities.value.filter((activity) => activity.status === activeTab.value)
})

function extractList(res) {
  const data = res?.data ?? res
  if (Array.isArray(data)) return data
  return data?.rows || data?.list || data?.content || []
}

function isCreatedByCurrentUser(event) {
  return canManageActivityForUser(userStore.userInfo, event)
}

function countByStatus(status) {
  return activities.value.filter((activity) => activity.status === status).length
}

function normalizeStatus(status) {
  const value = String(status || 'DRAFT').toUpperCase()
  if (value === 'PUBLISHED' || value === 'CANCELLED' || value === 'DRAFT') return value
  return 'DRAFT'
}

function statusLabel(status) {
  if (status === 'PUBLISHED') return 'Published'
  if (status === 'CANCELLED') return 'Cancelled'
  return 'Draft'
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

function fallbackImage(id) {
  return `https://picsum.photos/seed/created-${encodeURIComponent(id || 'activity')}/420/260`
}

async function registrationCount(eventId) {
  try {
    return extractList(await getEventRegistrations(eventId))
      .filter((registration) => String(registration.status || '').toUpperCase() !== 'CANCELLED')
      .length
  } catch (error) {
    return 0
  }
}

async function normalizeActivity(event) {
  const location = event.location || {}
  const status = normalizeStatus(event.status)

  return {
    id: event.id,
    title: event.title || 'Untitled activity',
    date: formatDate(event.startTime),
    time: formatTime(event.startTime, event.endTime),
    location: [location.name, location.address, location.city, location.country].filter(Boolean).join(', ') || 'Location TBA',
    image: event.coverImageUrl || event.imageUrls?.[0] || fallbackImage(event.id),
    capacity: Number(event.capacity || 0),
    registered: await registrationCount(event.id),
    status,
    statusLabel: statusLabel(status),
    organizerId: event.organizerId || event.organizer?.id || event.createdBy || event.userId
  }
}

async function loadActivities() {
  loading.value = true
  try {
    if (!userStore.userInfo) {
      await userStore.getInfo()
    }
    const events = extractList(await listEvent({ limit: 1000, upcomingOnly: false }))
      .filter(isCreatedByCurrentUser)

    activities.value = await Promise.all(events.map(normalizeActivity))
  } catch (error) {
    console.error('Failed to load created activities:', error)
    ElMessage.error('Failed to load created activities')
  } finally {
    loading.value = false
  }
}

function createActivity() {
  if (!canCreateActivity.value) {
    ElMessage.warning('You do not have permission to create activities')
    return
  }

  router.push('/manager/createActivity')
}

function manageActivity(activity) {
  router.push({
    path: '/manager/manageActivity',
    query: { id: activity.id }
  })
}

function viewAttendees(activity) {
  router.push({
    path: '/manager/attendeeList',
    query: { id: activity.id }
  })
}

onMounted(loadActivities)
</script>

<style scoped lang="scss">
.created-activities-page {
  min-height: 100vh;
  background: #f7faff;
  color: #10234f;
  font-family: Inter, "Segoe UI", Arial, sans-serif;
}

.created-shell {
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

.create-button {
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

.activity-card-list {
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

.status-pill.published {
  color: #16833d;
  background: #dcf6e3;
}

.status-pill.draft {
  color: #b7791f;
  background: #fff5df;
}

.status-pill.cancelled {
  color: #d92d3c;
  background: #ffe8ec;
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

@media (max-width: 820px) {
  .created-shell {
    padding: 28px 24px 40px;
  }

  .page-heading,
  .activity-title-row {
    align-items: flex-start;
    flex-direction: column;
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
  .created-shell {
    padding: 24px 16px 32px;
  }

  .page-heading h1 {
    font-size: 34px;
  }
}
</style>
