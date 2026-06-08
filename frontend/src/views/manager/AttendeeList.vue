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
        <el-button v-if="eventId" class="refresh-button" :loading="loading" @click="loadAttendees">
          <el-icon><Refresh /></el-icon>
          Refresh
        </el-button>
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
            <p>
              <el-icon><Tickets /></el-icon>
              <span>Ticket Sales: {{ formatMoney(ticketSalesTotal) }}</span>
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
          <span class="summary-icon purple">
            <el-icon><Tickets /></el-icon>
          </span>
          <div>
            <p>Confirmed</p>
            <strong>{{ confirmedCount }}</strong>
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

        <article class="summary-card">
          <span class="summary-icon teal">
            <el-icon><Money /></el-icon>
          </span>
          <div>
            <p>Ticket Sales</p>
            <strong>{{ formatMoney(ticketSalesTotal) }}</strong>
          </div>
        </article>

        <article class="summary-card">
          <span class="summary-icon rose">
            <el-icon><Star /></el-icon>
          </span>
          <div>
            <p>Average Rating</p>
            <strong>{{ averageRatingLabel }}</strong>
          </div>
        </article>
      </section>

      <section v-if="eventId && ticketTierAnalytics.length" class="table-card analytics-card">
        <div class="section-heading">
          <h2>Ticket Tier Analytics</h2>
          <p>Revenue is calculated from registration price snapshots.</p>
        </div>
        <el-table :data="ticketTierAnalytics" class="attendee-table" empty-text="No ticket tiers found">
          <el-table-column prop="name" label="Ticket Tier" min-width="180" />
          <el-table-column prop="type" label="Type" min-width="130" />
          <el-table-column label="Sold" width="120">
            <template #default="{ row }">{{ row.soldQuantity || 0 }}</template>
          </el-table-column>
          <el-table-column label="Remaining" width="130">
            <template #default="{ row }">{{ row.remainingQuantity || 0 }}</template>
          </el-table-column>
          <el-table-column label="Revenue" width="150">
            <template #default="{ row }">{{ formatMoney(row.revenue) }}</template>
          </el-table-column>
        </el-table>
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
            <el-option
              v-for="option in registrationStatusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </div>

        <el-table :data="filteredAttendees" class="attendee-table" empty-text="No attendees found">
          <el-table-column label="Attendee" min-width="220">
            <template #default="{ row }">
              <div class="attendee-cell">
                <img :src="row.avatar" :alt="row.name" @error="handleAvatarError" />
                <div>
                  <span>{{ row.name }}</span>
                  <small>{{ row.userId || 'No user id' }}</small>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="role" label="Role" min-width="150" />

          <el-table-column label="Ticket" min-width="180">
            <template #default="{ row }">
              <div class="ticket-cell">
                <span>{{ row.ticketName }}</span>
                <small>x{{ row.quantity }} · {{ formatMoney(row.totalPrice) }}</small>
              </div>
            </template>
          </el-table-column>

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
                {{ formatRegistrationStatus(row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="Actions" width="210" align="center">
            <template #default="{ row }">
              <div class="row-actions">
                <el-button :icon="View" aria-label="View attendee" @click="viewAttendee(row)" />
                <el-button
                  v-if="canMessageAttendee(row)"
                  :icon="Message"
                  :loading="messagingId === row.id"
                  aria-label="Message attendee"
                  @click="messageAttendee(row)"
                />
                <el-dropdown
                  v-if="canManageAttendeeRegistration(row)"
                  trigger="click"
                  @command="(status) => changeStatus(row, status)"
                >
                  <el-button :icon="Edit" :loading="updatingId === row.id" aria-label="Change status" />
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item
                        v-for="option in registrationStatusOptions"
                        :key="option.value"
                        :command="option.value"
                      >
                        {{ option.actionLabel }}
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
                <el-button
                  v-if="canCancelAttendeeRegistration(row)"
                  :icon="Delete"
                  class="remove-button"
                  :loading="removingId === row.id"
                  aria-label="Remove attendee"
                  @click="removeAttendee(row)"
                />
              </div>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </main>

    <el-drawer
      v-model="detailVisible"
      title="Attendee Details"
      size="420px"
      class="attendee-drawer"
    >
      <div v-if="selectedAttendee" class="detail-panel">
        <div class="detail-header">
          <img :src="selectedAttendee.avatar" :alt="selectedAttendee.name" @error="handleAvatarError" />
          <div>
            <h2>{{ selectedAttendee.name }}</h2>
            <el-tag class="status-tag" :class="selectedAttendee.status.toLowerCase()" effect="plain">
              {{ formatRegistrationStatus(selectedAttendee.status) }}
            </el-tag>
          </div>
        </div>

        <dl class="detail-list">
          <div>
            <dt>Role</dt>
            <dd>{{ selectedAttendee.role }}</dd>
          </div>
          <div>
            <dt>User ID</dt>
            <dd>{{ selectedAttendee.userId || 'Not available' }}</dd>
          </div>
          <div>
            <dt>Registration ID</dt>
            <dd>{{ selectedAttendee.id || 'Not available' }}</dd>
          </div>
          <div>
            <dt>Registered At</dt>
            <dd>{{ selectedAttendee.registeredDate }} {{ selectedAttendee.registeredTime }}</dd>
          </div>
          <div>
            <dt>Activity</dt>
            <dd>{{ event.title }}</dd>
          </div>
        </dl>
      </div>
    </el-drawer>
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
  Message,
  Money,
  Plus,
  Refresh,
  Search,
  Star,
  Tickets,
  User,
  UserFilled,
  View
} from '@element-plus/icons-vue'
import {
  deleteRegistration,
  getEvent,
  getEventAnalytics,
  getRegistration,
  getRegistrationsByEvent,
  updateRegistration
} from '@/api/manager/AttendeeList'
import { createEventDirectChat } from '@/api/chat'
import useUserStore from '@/store/modules/user'
import {
  canCancelRegistration,
  canManageActivityForUser,
  canManageRegistration,
  roleDisplayLabel
} from '@/utils/accessControl'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const eventId = computed(() => route.query.id || route.query.eventId || route.query.activityId || '')
const originalEvent = ref(null)

const loading = ref(false)
const updatingId = ref('')
const removingId = ref('')
const messagingId = ref('')
const detailVisible = ref(false)
const selectedAttendee = ref(null)
const searchText = ref('')
const statusFilter = ref('all')
const attendees = ref([])
const analytics = ref(null)

const defaultEventImage = 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=760&q=80'
const defaultAvatar = 'https://ui-avatars.com/api/?background=eef5ff&color=0f66e9&name=Attendee'
const registrationStatusOptions = [
  { label: 'Confirmed', actionLabel: 'Confirm', value: 'CONFIRMED' },
  { label: 'Registered', actionLabel: 'Registered', value: 'REGISTERED' },
  { label: 'Cancelled', actionLabel: 'Cancel', value: 'CANCELLED' }
]
const registrationStatusLabels = registrationStatusOptions.reduce((labels, option) => {
  labels[option.value] = option.label
  return labels
}, {})
const supportedRegistrationStatuses = new Set(registrationStatusOptions.map((option) => option.value))

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
const registered = computed(() => analytics.value?.soldQuantity ?? attendees.value
  .filter((attendee) => isActiveRegistration(attendee.status))
  .reduce((sum, attendee) => sum + Number(attendee.quantity || 1), 0))
const confirmedCount = computed(() => analytics.value?.confirmedCount ?? attendees.value.filter((attendee) => attendee.status === 'CONFIRMED').length)
const remainingSpots = computed(() => analytics.value?.remainingSpots ?? Math.max(capacity.value - registered.value, 0))
const ticketSalesTotal = computed(() => analytics.value?.ticketSalesTotal ?? 0)
const ticketTierAnalytics = computed(() => analytics.value?.ticketTiers || [])
const averageRatingLabel = computed(() => {
  const rating = Number(analytics.value?.averageRating || 0)
  return rating > 0 ? rating.toFixed(1) : 'N/A'
})

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

const filteredAttendees = computed(() => {
  const keyword = searchText.value.trim().toLowerCase()

  return attendees.value.filter((attendee) => {
    const matchesSearch = !keyword ||
      attendee.name.toLowerCase().includes(keyword) ||
      attendee.role.toLowerCase().includes(keyword) ||
      attendee.userId.toLowerCase().includes(keyword)
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
  return String(status || 'REGISTERED').trim().toUpperCase()
}

function formatRegistrationStatus(status) {
  const normalized = normalizeStatus(status)
  return registrationStatusLabels[normalized] || normalized
}

function formatMoney(value) {
  const amount = Number(value || 0)
  return amount === 0 ? 'Free' : `S$${amount.toFixed(2)}`
}

function isActiveRegistration(status) {
  return !['CANCELLED', 'CANCELED', 'REJECTED'].includes(normalizeStatus(status))
}

function mapRegistration(registration, userDetails = null) {
  const eventData = registration.event || {}
  const user = userDetails || registration.user || {}
  const name = user.fullName || user.name || user.email || 'Unknown attendee'
  const registeredAt = formatDateTimeParts(registration.registeredAt)

  return {
    id: registration.id,
    eventId: eventData.id || registration.eventId || eventId.value,
    userId: user.id || registration.userId || '',
    name,
    role: roleDisplayLabel(user.role || 'USER'),
    status: normalizeStatus(registration.status),
    ticketName: registration.ticketTierName || 'Standard',
    quantity: registration.quantity || 1,
    totalPrice: registration.totalPrice || 0,
    registeredDate: registeredAt.date,
    registeredTime: registeredAt.time,
    raw: registration,
    avatar: normalizeImageUrl(user.photo || user.avatar) || avatarUrl(name)
  }
}

function getRegistrationEventId(attendee) {
  return attendee?.eventId || attendee?.raw?.event?.id || attendee?.raw?.eventId || eventId.value
}

function getErrorMessage(error, fallback) {
  return error?.response?.data?.message ||
    error?.response?.data?.msg ||
    error?.message ||
    fallback
}

function applyEvent(data) {
  const eventData = unwrapResponse(data) || {}
  const location = eventData.location || {}

  event.id = eventData.id || ''
  event.title = eventData.title || 'Untitled activity'
  event.date = formatDate(eventData.startTime)
  event.time = formatTime(eventData.startTime, eventData.endTime)
  event.location = [location.name, location.address, location.city, location.country].filter(Boolean).join(', ') || 'Location TBA'
  event.image = normalizeImageUrl(eventData.coverImageUrl) || normalizeImageUrl(eventData.imageUrls?.[0]) || defaultEventImage
  event.status = normalizeStatus(eventData.status || 'DRAFT')
  event.capacity = Number(eventData.capacity || 0)
}

async function hydrateRegistration(registration) {
  return mapRegistration(registration)
}

async function mapRegistrations(registrations) {
  return Promise.all(registrations.map(hydrateRegistration))
}

async function loadAttendees() {
  if (!eventId.value) {
    return
  }

  loading.value = true
  try {
    if (!userStore.userInfo) {
      await userStore.getInfo()
    }

    const eventData = await getEvent(eventId.value)
    const rawEvent = unwrapResponse(eventData)
    originalEvent.value = rawEvent

    if (!canManageActivityForUser(userStore.userInfo, rawEvent)) {
      ElMessage.warning('You do not have permission to view attendees')
      router.replace('/product/mainEvent')
      return
    }

    applyEvent(eventData)
    const registrations = await getRegistrationsByEvent(eventId.value)
    attendees.value = await mapRegistrations(extractList(registrations))
    analytics.value = unwrapResponse(await getEventAnalytics(eventId.value))
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
    selectedAttendee.value = await hydrateRegistration(registration)
    detailVisible.value = true
  } catch (error) {
    console.error(error)
    ElMessage.error('Failed to load attendee details')
  }
}

async function changeStatus(attendee, status) {
  if (!canManageAttendeeRegistration(attendee)) {
    ElMessage.warning('You do not have permission to update attendees')
    return
  }

  const nextStatus = normalizeStatus(status)
  if (!supportedRegistrationStatuses.has(nextStatus)) {
    ElMessage.warning('Unsupported registration status')
    return
  }

  if (attendee.status === nextStatus) {
    return
  }

  const requestEventId = getRegistrationEventId(attendee)
  if (!requestEventId) {
    ElMessage.error('Registration event id is missing')
    return
  }

  updatingId.value = attendee.id
  try {
    const updated = unwrapResponse(await updateRegistration(attendee.id, {
      eventId: requestEventId,
      status: nextStatus
    }))
    const nextAttendee = updated?.id ? await hydrateRegistration(updated) : { ...attendee, status: nextStatus }
    attendees.value = attendees.value.map((item) => item.id === attendee.id ? nextAttendee : item)
    if (selectedAttendee.value?.id === attendee.id) {
      selectedAttendee.value = nextAttendee
    }
    ElMessage.success('Attendee status updated')
  } catch (error) {
    console.error(error)
    ElMessage.error(getErrorMessage(error, 'Failed to update attendee status'))
  } finally {
    updatingId.value = ''
  }
}

async function removeAttendee(attendee) {
  if (!canCancelAttendeeRegistration(attendee)) {
    ElMessage.warning('You do not have permission to remove attendees')
    return
  }

  try {
    await ElMessageBox.confirm(`Remove ${attendee.name} from this event?`, 'Warning', {
      confirmButtonText: 'Confirm',
      cancelButtonText: 'Cancel',
      type: 'warning'
    })
    removingId.value = attendee.id
    await deleteRegistration(attendee.id)
    attendees.value = attendees.value.filter((item) => item.id !== attendee.id)
    if (selectedAttendee.value?.id === attendee.id) {
      detailVisible.value = false
      selectedAttendee.value = null
    }
    ElMessage.success('Attendee removed')
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error('Failed to remove attendee')
    }
  } finally {
    removingId.value = ''
  }
}

function canMessageAttendee(attendee) {
  return attendee?.status === 'CONFIRMED' && canManageActivityForUser(userStore.userInfo, originalEvent.value)
}

async function messageAttendee(attendee) {
  const attendeeUserId = attendee?.raw?.user?.id
  if (!attendeeUserId) {
    ElMessage.warning('Cannot start chat: attendee user information is missing.')
    return
  }

  if (!eventId.value) {
    ElMessage.warning('Missing event id')
    return
  }

  messagingId.value = attendee.id
  try {
    const chat = unwrapResponse(await createEventDirectChat(eventId.value, attendeeUserId))
    if (chat?.id) {
      router.push(`/messages/${chat.id}`)
    }
  } catch (error) {
    console.error('Failed to start attendee chat:', error)
  } finally {
    messagingId.value = ''
  }
}

function canManageAttendeeRegistration(attendee) {
  return canManageRegistration(attendee?.raw || attendee, originalEvent.value, userStore.userInfo)
}

function canCancelAttendeeRegistration(attendee) {
  return canCancelRegistration(attendee?.raw || attendee, originalEvent.value, userStore.userInfo)
}

function handleAvatarError(error) {
  error.target.src = defaultAvatar
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

.page-heading > div {
  min-width: 0;
  flex: 1;
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

.refresh-button {
  height: 44px;
  padding: 0 18px;
  border-color: #c7d8f4;
  border-radius: 10px;
  color: #0f66e9;
  background: #fff;
  font-weight: 700;
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
  grid-template-columns: repeat(6, minmax(0, 1fr));
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

.summary-icon.purple {
  color: #7344c9;
  background: #f1ebff;
}

.summary-icon.amber {
  color: #d98a00;
  background: #fff5df;
}

.summary-icon.teal {
  color: #0b8f83;
  background: #e4fbf8;
}

.summary-icon.rose {
  color: #c2416c;
  background: #fff0f5;
}

.analytics-card {
  margin-top: 20px;
  padding: 22px;
}

.section-heading {
  margin-bottom: 16px;
}

.section-heading h2 {
  margin: 0;
  color: #071a47;
  font-size: 22px;
  font-weight: 800;
}

.section-heading p {
  margin: 6px 0 0;
  color: #667091;
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

.attendee-cell > div {
  min-width: 0;
  display: grid;
  gap: 4px;
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

.attendee-cell small {
  max-width: 260px;
  overflow: hidden;
  color: #667091;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.status-tag.registered,
.status-tag.attended {
  color: #0f66e9;
  background: #e8f1ff;
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

.detail-panel {
  display: grid;
  gap: 28px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.detail-header img {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
}

.detail-header h2 {
  margin: 0 0 10px;
  color: #071a47;
  font-size: 22px;
  line-height: 1.2;
  font-weight: 800;
}

.detail-list {
  margin: 0;
  display: grid;
  gap: 18px;
}

.detail-list div {
  display: grid;
  gap: 6px;
}

.detail-list dt {
  color: #667091;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
}

.detail-list dd {
  margin: 0;
  overflow-wrap: anywhere;
  color: #1c2c57;
  font-size: 15px;
  line-height: 1.45;
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
