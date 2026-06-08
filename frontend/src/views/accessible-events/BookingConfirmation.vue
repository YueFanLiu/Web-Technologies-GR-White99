<template>
  <div class="booking-confirmation-page">
    <main class="confirmation-shell">
      <section class="confirmation-card">
        <div class="success-panel">
          <div class="success-icon">
            <el-icon><Check /></el-icon>
          </div>
          <h1>Booking Confirmed</h1>
          <p>Your booking was successful.</p>
          <p>A confirmation email has been sent with your booking details.</p>
        </div>

        <el-divider />

        <section class="event-section">
          <h2>Event Summary</h2>

          <div class="event-summary">
            <img :src="event.image" :alt="event.title" class="event-image" />

            <div class="event-copy">
              <h3>{{ event.title }}</h3>
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
        </section>

        <el-divider />

        <section class="details-section">
          <h2>Booking Details</h2>

          <div class="details-list">
            <div>
              <span>Confirmation number</span>
              <strong class="booking-id">{{ booking.confirmationNumber }}</strong>
            </div>
            <div>
              <span>Status</span>
              <strong>{{ booking.status }}</strong>
            </div>
            <div>
              <span>Registered at</span>
              <strong>{{ booking.registeredAt }}</strong>
            </div>
            <div class="ticket-detail-row">
              <span>Tickets</span>
              <strong>
                <span v-for="item in bookingItems" :key="item.key" class="ticket-line">
                  {{ item.name }} x{{ item.quantity }} · {{ item.total }}
                </span>
              </strong>
            </div>
            <div>
              <span>Total</span>
              <strong>{{ booking.total }}</strong>
            </div>
          </div>
        </section>

        <div class="calendar-row">
          <el-button size="large" @click="downloadIcs">
            <el-icon><Download /></el-icon>
            Download ICS
          </el-button>
          <el-button size="large" @click="openGoogleCalendar">
            <el-icon><Calendar /></el-icon>
            Google Calendar
          </el-button>
          <el-button size="large" @click="openOutlookCalendar">
            <el-icon><Calendar /></el-icon>
            Outlook Calendar
          </el-button>
        </div>

        <div class="action-row">
          <el-button size="large" type="primary" class="primary-action" @click="viewMyEvents">
            <el-icon><Tickets /></el-icon>
            View My Events
          </el-button>
          <el-button size="large" class="secondary-action" @click="backToEvent">
            <el-icon><ArrowLeft /></el-icon>
            Back to Event
          </el-button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import fallbackEventImage from '@/assets/images/login-background.jpg'
import { downloadRegistrationCalendar } from '@/api/events/detail'
import {
  ArrowLeft,
  Calendar,
  Check,
  Clock,
  Download,
  Location,
  Tickets
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

function readStoredConfirmation() {
  try {
    return JSON.parse(sessionStorage.getItem('lastBookingConfirmation') || '{}')
  } catch (error) {
    return {}
  }
}

const storedConfirmation = readStoredConfirmation()
const registration = storedConfirmation.registration || {}
const registrations = Array.isArray(storedConfirmation.registrations) && storedConfirmation.registrations.length
  ? storedConfirmation.registrations
  : [registration].filter(Boolean)
const storedEvent = storedConfirmation.event || registration.event || {}
const storedTicketTier = storedConfirmation.ticketTier || {}
const storedTicketItems = Array.isArray(storedConfirmation.ticketItems) ? storedConfirmation.ticketItems : []

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

function formatDateTime(value) {
  const date = new Date(value)
  if (!value || Number.isNaN(date.getTime())) return 'Not available'
  return date.toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit'
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

function formatLocation(location) {
  return [location?.name, location?.address, location?.city, location?.country]
    .filter(Boolean)
    .join(', ') || 'Location TBA'
}

function fallbackImage(id) {
  return id ? `https://picsum.photos/seed/confirmation-${encodeURIComponent(id)}/720/420` : fallbackEventImage
}

const eventId = computed(() => storedEvent.id || registration.event?.id || route.query.eventId || '')

const event = computed(() => ({
  title: storedEvent.title || registration.event?.title || 'Activity',
  date: formatDate(storedEvent.startTime || registration.event?.startTime),
  time: formatTime(storedEvent.startTime || registration.event?.startTime, storedEvent.endTime || registration.event?.endTime),
  location: formatLocation(storedEvent.location || registration.event?.location),
  image: normalizeImageUrl(storedEvent.coverImageUrl)
    || normalizeImageUrl(storedEvent.imageUrls?.[0])
    || fallbackImage(eventId.value)
}))

const booking = computed(() => {
  return {
    confirmationNumber: registration.id || 'Not available',
    status: registration.status || 'REGISTERED',
    registeredAt: formatDateTime(registration.registeredAt),
    total: formatPrice(registrations.reduce((sum, item) => sum + Number(item?.totalPrice || 0), 0) || storedConfirmation.totalPrice)
  }
})

const bookingItems = computed(() => {
  if (storedTicketItems.length) {
    return storedTicketItems.map((item, index) => {
      const itemRegistration = item.registration || registrations[index] || {}
      const unitPrice = Number(item.unitPrice ?? itemRegistration.unitPrice ?? item.ticketTier?.price ?? 0)
      const quantity = Number(item.quantity || itemRegistration.quantity || 1)
      return {
        key: itemRegistration.id || item.ticketTier?.id || index,
        name: itemRegistration.ticketTierName || item.ticketTier?.name || 'Standard',
        quantity,
        total: formatPrice(item.totalPrice ?? itemRegistration.totalPrice ?? unitPrice * quantity)
      }
    })
  }

  return registrations.map((item, index) => ({
    key: item.id || index,
    name: item.ticketTierName || storedTicketTier.name || 'Standard',
    quantity: Number(item.quantity || storedConfirmation.quantity || 1),
    total: formatPrice(item.totalPrice ?? storedConfirmation.totalPrice)
  }))
})

function formatPrice(value) {
  const amount = Number(value || 0)
  return amount === 0 ? 'Free' : `S$${amount.toFixed(2)}`
}

function viewMyEvents() {
  router.push('/my-events/joined')
}

function backToEvent() {
  router.push({
    path: '/product/eventDetails',
    query: eventId.value ? { id: eventId.value } : {}
  })
}

function calendarDate(value) {
  const date = new Date(value)
  if (!value || Number.isNaN(date.getTime())) return ''
  return date.toISOString().replace(/[-:]/g, '').replace(/\.\d{3}Z$/, 'Z')
}

function providerCalendarPayload() {
  const startValue = storedEvent.startTime || registration.event?.startTime
  const endValue = storedEvent.endTime || registration.event?.endTime
  return {
    start: calendarDate(startValue),
    end: calendarDate(endValue),
    startIso: startValue ? new Date(startValue).toISOString() : '',
    endIso: endValue ? new Date(endValue).toISOString() : '',
    title: encodeURIComponent(storedEvent.title || registration.event?.title || 'Access4All Event'),
    details: encodeURIComponent(storedEvent.description || ''),
    location: encodeURIComponent(event.value.location || '')
  }
}

async function downloadIcs() {
  if (!registration.id) {
    ElMessage.warning('Registration is not available')
    return
  }
  try {
    const blob = await downloadRegistrationCalendar(registration.id)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `access4all-registration-${registration.id}.ics`
    link.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    console.error(error)
  }
}

function openGoogleCalendar() {
  const payload = providerCalendarPayload()
  if (!payload.start || !payload.end) {
    ElMessage.warning('Event time is not available')
    return
  }
  window.open(`https://calendar.google.com/calendar/render?action=TEMPLATE&text=${payload.title}&dates=${payload.start}/${payload.end}&details=${payload.details}&location=${payload.location}`, '_blank')
}

function openOutlookCalendar() {
  const payload = providerCalendarPayload()
  if (!payload.startIso || !payload.endIso) {
    ElMessage.warning('Event time is not available')
    return
  }
  window.open(`https://outlook.live.com/calendar/0/deeplink/compose?subject=${payload.title}&startdt=${encodeURIComponent(payload.startIso)}&enddt=${encodeURIComponent(payload.endIso)}&body=${payload.details}&location=${payload.location}`, '_blank')
}

</script>

<style scoped lang="scss">
.booking-confirmation-page {
  min-height: 100vh;
  background: #f7faff;
  color: #10234f;
  font-family: Inter, "Segoe UI", Arial, sans-serif;
}

.confirmation-shell {
  padding: 48px 44px 56px;
  display: flex;
  justify-content: center;
}

.confirmation-card {
  width: min(860px, 100%);
  padding: 34px 40px 38px;
  background: rgba(255, 255, 255, 0.97);
  border: 1px solid #e0e8f6;
  border-radius: 16px;
  box-shadow: 0 18px 38px rgba(26, 54, 100, 0.11);
}

.success-panel {
  text-align: center;
}

.success-icon {
  width: 82px;
  height: 82px;
  margin: 0 auto 20px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #129e4c;
  background: #dcf8e5;
  font-size: 46px;
}

.success-panel h1 {
  margin: 0;
  color: #0d1f4f;
  font-size: 40px;
  line-height: 1.15;
  font-weight: 800;
  letter-spacing: 0;
}

.success-panel p {
  margin: 10px 0 0;
  color: #4f5d7c;
  font-size: 16px;
  line-height: 1.5;
}

.event-section h2,
.details-section h2 {
  margin: 0 0 16px;
  color: #071a47;
  font-size: 18px;
  font-weight: 800;
}

.event-summary {
  display: grid;
  grid-template-columns: 286px minmax(0, 1fr);
  gap: 30px;
  align-items: center;
}

.event-image {
  width: 100%;
  height: 156px;
  border-radius: 12px;
  object-fit: cover;
}

.event-copy h3 {
  margin: 0 0 18px;
  color: #071a47;
  font-size: 23px;
  line-height: 1.25;
  font-weight: 800;
}

.event-meta {
  display: grid;
  gap: 12px;
}

.event-meta p {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #4f5d7c;
  font-size: 16px;
}

.event-meta .el-icon {
  color: #0f66e9;
  font-size: 19px;
}

.details-list {
  display: grid;
  border-top: 1px solid #e1e8f3;
}

.details-list div {
  min-height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  border-bottom: 1px solid #e1e8f3;
}

.details-list span {
  color: #4f5d7c;
  font-size: 15px;
}

.details-list strong {
  color: #071a47;
  font-size: 16px;
  font-weight: 700;
  text-align: right;
}

.details-list .booking-id {
  color: #0f66e9;
}

.details-list .ticket-detail-row {
  align-items: flex-start;
  padding: 13px 0;
}

.ticket-detail-row strong {
  display: grid;
  gap: 6px;
}

.ticket-line {
  display: block;
  color: #071a47;
}

.action-row {
  margin-top: 28px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.calendar-row {
  margin-top: 24px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.calendar-row .el-button {
  width: 100%;
}

.primary-action,
.secondary-action {
  height: 54px;
  border-radius: 11px;
  font-size: 16px;
  font-weight: 700;
}

.primary-action {
  box-shadow: 0 10px 22px rgba(47, 117, 246, 0.22);
}

.secondary-action {
  border-color: #2f75f6;
  color: #0f66e9;
  background: #fff;
}

@media (max-width: 820px) {
  .confirmation-shell {
    padding: 30px 24px 40px;
  }

  .confirmation-card {
    padding: 28px 24px 30px;
  }

  .event-summary,
  .calendar-row,
  .action-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .confirmation-shell {
    padding: 24px 16px 32px;
  }

  .success-panel h1 {
    font-size: 32px;
  }

  .details-list div {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
    padding: 12px 0;
  }

  .details-list strong {
    text-align: left;
  }
}
</style>
