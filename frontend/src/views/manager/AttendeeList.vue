<template>
  <div class="attendee-list-page">
    <main class="attendee-shell" v-loading="loading">
      <section class="page-heading">
        <el-button class="back-icon" @click="backToManage">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <div>
          <h1>Attendee List</h1>
          <p>Manage Activity / {{ event.title }} / Attendees</p>
        </div>
      </section>

      <el-empty v-if="!eventId" description="No activity selected" />

      <section v-else class="overview-card">
        <img :src="event.image" :alt="event.title" class="event-image" />

        <div class="event-info">
          <div class="title-row">
            <h2>{{ event.title }}</h2>
            <el-tag class="published-tag" :class="event.status.toLowerCase()" effect="plain">
              {{ event.status }}
            </el-tag>
          </div>

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
            <p>
              <el-icon><User /></el-icon>
              <span>Registered: {{ registered }} / {{ capacity }}</span>
            </p>
          </div>
        </div>
      </section>

      <section v-if="eventId" class="summary-grid">
        <article class="summary-card">
          <span class="summary-icon blue">
            <el-icon><User /></el-icon>
          </span>
          <div>
            <p>Total Capacity</p>
            <strong>{{ capacity }}</strong>
          </div>
        </article>

        <article class="summary-card">
          <span class="summary-icon green">
            <el-icon><UserFilled /></el-icon>
          </span>
          <div>
            <p>Registered</p>
            <strong>{{ registered }}</strong>
          </div>
        </article>

        <article class="summary-card">
          <span class="summary-icon amber">
            <el-icon><Plus /></el-icon>
          </span>
          <div>
            <p>Remaining Spots</p>
            <strong>{{ remainingSpots }}</strong>
          </div>
        </article>
      </section>

      <section v-if="eventId" class="table-card">
        <div class="toolbar">
          <el-input
            v-model="searchText"
            size="large"
            class="search-input"
            placeholder="Search attendee"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <el-select v-model="statusFilter" size="large" class="status-filter">
            <el-option label="All Status" value="all" />
            <el-option label="Confirmed" value="CONFIRMED" />
            <el-option label="Pending" value="PENDING" />
            <el-option label="Cancelled" value="CANCELLED" />
          </el-select>
        </div>

        <el-table :data="filteredAttendees" class="attendee-table" empty-text="No attendees found">
          <el-table-column label="Attendee" min-width="220">
            <template #default="{ row }">
              <div class="attendee-cell">
                <img :src="row.avatar" :alt="row.name" />
                <span>{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="role" label="Role" min-width="150" />

          <el-table-column label="Registered At" min-width="190">
            <template #default="{ row }">
              <div class="ticket-cell">
                <span>{{ row.registeredDate }}</span>
                <small>{{ row.registeredTime }}</small>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="Status" width="150">
            <template #default="{ row }">
              <el-tag class="status-tag" :class="row.status.toLowerCase()" effect="plain">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="Actions" width="150" align="center">
            <template #default="{ row }">
              <div class="row-actions">
                <el-button :icon="View" aria-label="View attendee" @click="viewAttendee(row)" />
                <el-dropdown trigger="click" @command="(status) => changeStatus(row, status)">
                  <el-button :icon="Edit" aria-label="Change status" />
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="CONFIRMED">Confirm</el-dropdown-item>
                      <el-dropdown-item command="PENDING">Pending</el-dropdown-item>
                      <el-dropdown-item command="CANCELLED">Cancel</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
                <el-button :icon="Delete" class="remove-button" aria-label="Remove attendee" @click="removeAttendee(row)" />
              </div>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  Calendar,
  Clock,
  Delete,
  Edit,
  Location,
  Plus,
  Search,
  User,
  UserFilled,
  View
} from '@element-plus/icons-vue'
import {
  deleteRegistration,
  getEvent,
  getRegistration,
  getEventRegistrations,
  updateRegistration
} from '@/api/manager/AttendeeList'

const route = useRoute()
const router = useRouter()
const eventId = computed(() => route.query.id || route.query.eventId || route.query.activityId || '')

const loading = ref(false)
const searchText = ref('')
const statusFilter = ref('all')
const attendees = ref([])

const defaultEventImage = 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=760&q=80'
const defaultAvatar = 'https://ui-avatars.com/api/?background=eef5ff&color=0f66e9&name=Attendee'

const event = reactive({
  id: '',
  title: 'Selected activity',
  date: 'Date TBA',
  time: 'Time TBA',
  location: 'Location TBA',
  image: defaultEventImage,
  status: 'DRAFT',
  capacity: 0
})

const capacity = computed(() => event.capacity)
const registered = computed(() => attendees.value.filter((attendee) => attendee.status !== 'CANCELLED').length)
const remainingSpots = computed(() => Math.max(capacity.value - registered.value, 0))

function unwrapResponse(res) {
  return res?.data ?? res
}

function extractList(res) {
  const data = unwrapResponse(res)
  if (Array.isArray(data)) {
    return data
  }

  return data?.rows || data?.list || data?.content || []
}

const filteredAttendees = computed(() => {
  const keyword = searchText.value.trim().toLowerCase()

  return attendees.value.filter((attendee) => {
    const matchesSearch = !keyword ||
      attendee.name.toLowerCase().includes(keyword) ||
      attendee.role.toLowerCase().includes(keyword)
    const matchesStatus = statusFilter.value === 'all' || attendee.status === statusFilter.value

    return matchesSearch && matchesStatus
  })
})

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

function formatDateTimeParts(value) {
  const date = new Date(value)
  if (!value || Number.isNaN(date.getTime())) {
    return { date: 'Not available', time: '' }
  }

  return {
    date: date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    }),
    time: date.toLocaleTimeString('en-US', {
      hour: 'numeric',
      minute: '2-digit'
    })
  }
}

function avatarUrl(name) {
  return `https://ui-avatars.com/api/?background=eef5ff&color=0f66e9&name=${encodeURIComponent(name || 'Attendee')}`
}

function normalizeStatus(status) {
  return String(status || 'PENDING').toUpperCase()
}

function mapRegistration(registration) {
  const eventData = registration.event || {}
  const user = registration.user || {}
  const name = user.fullName || user.name || user.email || 'Unknown attendee'
  const registeredAt = formatDateTimeParts(registration.registeredAt)

  return {
    id: registration.id,
    eventId: eventData.id || registration.eventId || eventId.value,
    userId: user.id || registration.userId || '',
    name,
    role: user.role || 'USER',
    status: normalizeStatus(registration.status),
    registeredDate: registeredAt.date,
    registeredTime: registeredAt.time,
    raw: registration,
    avatar: user.photo || avatarUrl(name)
  }
}

function applyEvent(data) {
  const eventData = unwrapResponse(data) || {}
  const location = eventData.location || {}

  event.id = eventData.id || ''
  event.title = eventData.title || 'Untitled activity'
  event.date = formatDate(eventData.startTime)
  event.time = formatTime(eventData.startTime, eventData.endTime)
  event.location = [location.name, location.address, location.city, location.country].filter(Boolean).join(', ') || 'Location TBA'
  event.image = eventData.coverImageUrl || eventData.imageUrls?.[0] || defaultEventImage
  event.status = normalizeStatus(eventData.status || 'DRAFT')
  event.capacity = Number(eventData.capacity || 0)
}

async function loadAttendees() {
  if (!eventId.value) {
    return
  }

  loading.value = true
  try {
    const [eventData, registrations] = await Promise.all([
      getEvent(eventId.value),
      getEventRegistrations(eventId.value)
    ])

    applyEvent(eventData)
    attendees.value = extractList(registrations).map(mapRegistration)
  } catch (error) {
    console.error(error)
    ElMessage.error('Failed to load attendees')
  } finally {
    loading.value = false
  }
}

function backToManage() {
  router.push({
    path: '/manager/manageActivity',
    query: eventId.value ? { id: eventId.value } : {}
  })
}

async function viewAttendee(attendee) {
  try {
    const registration = unwrapResponse(await getRegistration(attendee.id))
    const latest = mapRegistration(registration)
    ElMessage.info(`${latest.name} - ${latest.status}`)
  } catch (error) {
    console.error(error)
    ElMessage.error('Failed to load attendee details')
  }
}

async function changeStatus(attendee, status) {
  if (attendee.status === status) {
    return
  }

  try {
    const updated = unwrapResponse(await updateRegistration(attendee.id, {
      eventId: attendee.eventId,
      status
    }))
    const nextAttendee = updated?.id ? mapRegistration(updated) : { ...attendee, status }
    attendees.value = attendees.value.map((item) => item.id === attendee.id ? nextAttendee : item)
    ElMessage.success('Attendee status updated')
  } catch (error) {
    console.error(error)
    ElMessage.error('Failed to update attendee status')
  }
}

async function removeAttendee(attendee) {
  try {
    await ElMessageBox.confirm(`Remove ${attendee.name} from this event?`, 'Warning', {
      confirmButtonText: 'Confirm',
      cancelButtonText: 'Cancel',
      type: 'warning'
    })
    await deleteRegistration(attendee.id)
    ElMessage.success('Attendee removed')
    await loadAttendees()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error('Failed to remove attendee')
    }
  }
}

onMounted(loadAttendees)
</script>

<style scoped lang="scss">
.attendee-list-page {
  min-height: 100vh;
  background: #f7faff;
  color: #10234f;
  font-family: Inter, "Segoe UI", Arial, sans-serif;
}

.attendee-shell {
  padding: 34px 44px 48px;
}

.page-heading {
  margin-bottom: 26px;
  display: flex;
  align-items: center;
  gap: 22px;
}

.back-icon {
  width: 48px;
  height: 48px;
  padding: 0;
  border-color: #d6dfef;
  border-radius: 50%;
  color: #0f66e9;
  background: #fff;
  font-size: 20px;
}

.page-heading h1 {
  margin: 0;
  color: #0d1f4f;
  font-size: 38px;
  line-height: 1.15;
  font-weight: 800;
  letter-spacing: 0;
}

.page-heading p {
  margin: 8px 0 0;
  color: #667091;
  font-size: 14px;
}

.overview-card,
.summary-card,
.table-card {
  background: rgba(255, 255, 255, 0.97);
  border: 1px solid #e0e8f6;
  border-radius: 14px;
  box-shadow: 0 12px 28px rgba(26, 54, 100, 0.09);
}

.overview-card {
  padding: 22px;
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 32px;
  align-items: center;
}

.event-image {
  width: 100%;
  height: 176px;
  border-radius: 12px;
  object-fit: cover;
}

.title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
}

.title-row h2 {
  margin: 0;
  color: #071a47;
  font-size: 25px;
  line-height: 1.2;
  font-weight: 800;
}

.published-tag {
  height: 28px;
  border: 0;
  border-radius: 7px;
  color: #16833d;
  background: #dcf6e3;
  font-weight: 800;
}

.event-meta {
  margin-top: 18px;
  display: grid;
  gap: 12px;
}

.event-meta p {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #53617f;
  font-size: 15px;
}

.event-meta .el-icon {
  color: #0f66e9;
  font-size: 19px;
}

.summary-grid {
  margin-top: 20px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.summary-card {
  min-height: 112px;
  padding: 22px;
  display: flex;
  align-items: center;
  gap: 18px;
}

.summary-icon {
  width: 58px;
  height: 58px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-size: 28px;
}

.summary-icon.blue {
  color: #0f66e9;
  background: #eef5ff;
}

.summary-icon.green {
  color: #159947;
  background: #e9f9ef;
}

.summary-icon.amber {
  color: #d98a00;
  background: #fff5df;
}

.summary-card p {
  margin: 0 0 8px;
  color: #667091;
  font-size: 15px;
}

.summary-card strong {
  color: #071a47;
  font-size: 28px;
  line-height: 1;
  font-weight: 800;
}

.table-card {
  margin-top: 20px;
  overflow: hidden;
}

.toolbar {
  padding: 22px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 18px;
  border-bottom: 1px solid #e0e8f6;
}

.search-input :deep(.el-input__wrapper),
.status-filter :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #d6dfef inset;
}

.attendee-table {
  width: 100%;
}

.attendee-table :deep(th.el-table__cell) {
  height: 56px;
  background: #fbfdff;
  color: #1c2c57;
  font-weight: 800;
}

.attendee-table :deep(td.el-table__cell) {
  height: 72px;
  color: #26365f;
}

.attendee-cell {
  display: flex;
  align-items: center;
  gap: 14px;
}

.attendee-cell img {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
}

.attendee-cell span {
  color: #071a47;
  font-weight: 700;
}

.ticket-cell {
  display: grid;
  gap: 4px;
}

.ticket-cell span {
  color: #1c2c57;
  font-weight: 600;
}

.ticket-cell small {
  color: #667091;
  font-size: 13px;
}

.status-tag {
  min-width: 88px;
  justify-content: center;
  border: 0;
  border-radius: 7px;
  font-weight: 700;
}

.status-tag.confirmed {
  color: #16833d;
  background: #dcf6e3;
}

.status-tag.pending {
  color: #c47a00;
  background: #fff1d2;
}

.status-tag.cancelled {
  color: #e23a4a;
  background: #ffe5e9;
}

.row-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.row-actions .el-button {
  width: 42px;
  height: 42px;
  padding: 0;
  margin-left: 0;
  border-color: #d6dfef;
  border-radius: 9px;
  color: #1c2c57;
  background: #fff;
}

.row-actions .remove-button {
  color: #ef3c4d;
}

@media (max-width: 980px) {
  .attendee-shell {
    padding: 28px 24px 40px;
  }

  .overview-card,
  .summary-grid,
  .toolbar {
    grid-template-columns: 1fr;
  }

  .event-image {
    height: 220px;
  }
}

@media (max-width: 560px) {
  .attendee-shell {
    padding: 24px 16px 32px;
  }

  .page-heading h1 {
    font-size: 32px;
  }

  .overview-card,
  .summary-card {
    padding: 18px;
  }
}
</style>
