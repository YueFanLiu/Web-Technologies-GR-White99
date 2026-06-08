<template>
  <div class="book-activity-page">
    <main class="booking-shell">
      <section class="page-heading">
        <h1>Book Activity</h1>
        <p>Review your details and confirm your booking.</p>
      </section>

      <el-alert
        v-if="alreadyRegistered"
        class="booking-alert"
        title="You are already registered for this activity."
        type="info"
        show-icon
        :closable="false"
      />

      <section class="booking-grid" v-loading="loading">
        <div class="left-column">
          <section class="booking-card">
            <div class="form-section">
              <div class="section-title">
                <span class="section-icon">
                  <el-icon><Tickets /></el-icon>
                </span>
                <h2>1. Event Summary</h2>
              </div>

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
            </div>

            <el-divider />

            <div class="form-section">
              <div class="section-title">
                <span class="section-icon">
                  <el-icon><PriceTag /></el-icon>
                </span>
                <h2>2. Booking Details</h2>
              </div>

              <div
                v-for="tier in visibleTicketTiers"
                :key="tier.id"
                class="ticket-row"
                :class="{ selected: ticketTierQuantity(tier) > 0, disabled: isTicketTierBooked(tier) }"
              >
                <div>
                  <h3>{{ tier.name }}</h3>
                  <p>{{ ticketTierDescription(tier) }}</p>
                </div>

                <div class="quantity-control">
                  <el-button :icon="Minus" :disabled="isTicketTierBooked(tier)" @click.stop="decreaseQuantity(tier)" />
                  <span>{{ ticketTierQuantity(tier) }}</span>
                  <el-button :icon="Plus" :disabled="isTicketTierBooked(tier)" @click.stop="increaseQuantity(tier)" />
                </div>

                <strong>{{ formatPrice(tier.price) }}</strong>
              </div>

              <div class="total-row">
                <span>Total</span>
                <strong>{{ formatPrice(totalPrice) }}</strong>
              </div>
            </div>

            <el-divider />

            <div class="form-section">
              <div class="section-title">
                <span class="section-icon">
                  <el-icon><User /></el-icon>
                </span>
                <h2>3. Contact Information</h2>
              </div>

              <el-form class="contact-form" label-position="top">
                <el-form-item label="Full Name">
                  <el-input v-model="contact.fullName" size="large" placeholder="Enter your full name" />
                </el-form-item>

                <el-form-item label="Email">
                  <el-input v-model="contact.email" size="large" placeholder="Enter your email" />
                </el-form-item>

                <el-form-item label="Phone Number">
                  <el-input v-model="contact.phone" size="large" placeholder="Enter your phone number" />
                </el-form-item>
              </el-form>
            </div>
          </section>

          <div class="bottom-actions">
            <el-button size="large" class="back-button" @click="backToEvent">
              <el-icon><ArrowLeft /></el-icon>
              Back to Event
            </el-button>
            <el-button
              size="large"
              type="primary"
              class="confirm-button"
              :loading="submitting"
              @click="confirmBooking"
            >
              <el-icon><Lock /></el-icon>
              {{ alreadyRegistered ? 'View Confirmation' : 'Confirm Booking' }}
            </el-button>
          </div>
        </div>

        <aside class="summary-card">
          <h2>Booking Summary</h2>

          <div class="summary-block">
            <span>Event</span>
            <strong>{{ event.title }}</strong>
          </div>

          <div class="summary-list">
            <div>
              <span>Date</span>
              <strong>{{ event.date }}</strong>
            </div>
            <div>
              <span>Time</span>
              <strong>{{ event.time }}</strong>
            </div>
            <div>
              <span>Location</span>
              <strong>{{ event.location }}</strong>
            </div>
          </div>

          <el-divider />

          <div class="summary-list">
            <div>
              <span>Ticket</span>
              <strong>{{ selectedTicketSummary }}</strong>
            </div>
            <div>
              <span>Price</span>
              <strong>{{ selectedTicketPriceSummary }}</strong>
            </div>
          </div>

          <div class="summary-total">
            <span>Total</span>
            <strong>{{ formatPrice(totalPrice) }}</strong>
          </div>

          <el-button
            size="large"
            type="primary"
            class="summary-confirm"
            :loading="submitting"
            @click="confirmBooking"
          >
            {{ alreadyRegistered ? 'View Confirmation' : 'Confirm Booking' }}
          </el-button>
        </aside>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  createEventRegistration,
  getCurrentUserProfile,
  getEventDetail,
  getEventTicketTiers,
  getRegistrationsByUser
} from '@/api/events/detail'
import { isAlreadyRegisteredError } from '@/constants/events'
import fallbackEventImage from '@/assets/images/login-background.jpg'
import {
  ArrowLeft,
  Calendar,
  Clock,
  Location,
  Lock,
  Minus,
  Plus,
  PriceTag,
  Tickets,
  User
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const eventId = route.query.eventId || route.query.id
const initialQuantity = Math.max(1, Number(route.query.quantity) || 1)
const loading = ref(false)
const submitting = ref(false)
const alreadyRegistered = ref(false)
const existingRegistration = ref(null)
const existingActiveRegistrations = ref([])
const rawEvent = ref(null)
const ticketTiers = ref([])
const ticketTierQuantities = ref({})

const event = reactive({
  id: eventId || '',
  title: 'Loading event...',
  date: 'Date TBA',
  time: 'Time TBA',
  location: 'Location TBA',
  image: fallbackEventImage,
  price: 0,
  capacity: 0
})

const contact = ref({
  fullName: '',
  email: '',
  phone: ''
})

const fallbackTicketTier = computed(() => ({
  id: 'fallback-standard',
  name: 'Standard',
  type: 'STANDARD',
  price: Number(event.price || 0),
  active: true,
  remainingQuantity: event.capacity || 0
}))

const visibleTicketTiers = computed(() => {
  const tiers = ticketTiers.value.length ? ticketTiers.value : [fallbackTicketTier.value]
  return tiers.filter((tier) => tier.active !== false)
})

const selectedTicketItems = computed(() => {
  return visibleTicketTiers.value
    .map((tier) => ({
      tier,
      quantity: ticketTierQuantity(tier),
      unitPrice: Number(tier.price || 0)
    }))
    .filter((item) => item.quantity > 0)
})

const totalQuantity = computed(() => {
  return selectedTicketItems.value.reduce((sum, item) => sum + item.quantity, 0)
})

const totalPrice = computed(() => {
  return selectedTicketItems.value.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0)
})

const selectedTicketSummary = computed(() => {
  if (!selectedTicketItems.value.length) return 'No tickets selected'
  return selectedTicketItems.value
    .map((item) => `${item.tier.name || 'Standard'} x${item.quantity}`)
    .join(', ')
})

const selectedTicketPriceSummary = computed(() => {
  if (!selectedTicketItems.value.length) return formatPrice(0)
  return selectedTicketItems.value
    .map((item) => `${item.tier.name || 'Standard'} ${formatPrice(item.unitPrice)}`)
    .join(', ')
})

function ticketTierKey(tier) {
  return tier?.id || 'fallback-standard'
}

function registrationTicketTierKey(registration) {
  return registration?.ticketTierId || registration?.ticketTier?.id || 'fallback-standard'
}

function ticketTierQuantity(tier) {
  return Number(ticketTierQuantities.value[ticketTierKey(tier)] || 0)
}

function setTicketTierQuantity(tier, value) {
  const quantity = Math.max(0, Number(value || 0))
  ticketTierQuantities.value = {
    ...ticketTierQuantities.value,
    [ticketTierKey(tier)]: quantity
  }
}

function increaseQuantity(tier) {
  if (isTicketTierBooked(tier)) {
    ElMessage.info('You already booked this ticket tier')
    return
  }
  const nextQuantity = ticketTierQuantity(tier) + 1
  const remaining = Number(tier.remainingQuantity || 0)
  if (remaining > 0 && nextQuantity > remaining) {
    ElMessage.warning('Not enough remaining tickets')
    return
  }
  setTicketTierQuantity(tier, nextQuantity)
}

function decreaseQuantity(tier) {
  if (isTicketTierBooked(tier)) {
    return
  }
  setTicketTierQuantity(tier, ticketTierQuantity(tier) - 1)
}

function formatPrice(value) {
  const amount = Number(value || 0)
  return amount === 0 ? 'Free' : `S$${amount.toFixed(2)}`
}

function ensureTicketTierQuantities() {
  const nextQuantities = { ...ticketTierQuantities.value }
  visibleTicketTiers.value.forEach((tier, index) => {
    const key = ticketTierKey(tier)
    if (nextQuantities[key] === undefined) {
      nextQuantities[key] = route.query.quantity && index === 0 ? initialQuantity : 0
    }
  })
  ticketTierQuantities.value = nextQuantities
}

function ticketTierDescription(tier) {
  const parts = []
  if (tier.type) parts.push(String(tier.type).replaceAll('_', ' '))
  if (tier.remainingQuantity !== undefined && tier.remainingQuantity !== null) {
    parts.push(`${tier.remainingQuantity} remaining`)
  }
  if (isTicketTierBooked(tier)) {
    parts.push('Already booked')
  }
  return parts.join(' · ') || 'Activity ticket'
}

function tierDescription(tier) {
  const parts = []
  if (tier.type) parts.push(String(tier.type).replaceAll('_', ' '))
  if (tier.remainingQuantity !== undefined && tier.remainingQuantity !== null) {
    parts.push(`${tier.remainingQuantity} remaining`)
  }
  return parts.join(' · ') || 'Activity ticket'
}

function backToEvent() {
  router.push({
    path: '/product/eventDetails',
    query: eventId ? { id: eventId } : {}
  })
}

function formatEventDate(value) {
  const date = new Date(value)
  if (!value || Number.isNaN(date.getTime())) return 'Date TBA'
  return date.toLocaleDateString('en-US', {
    weekday: 'short',
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

function formatEventTime(startValue, endValue) {
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

function fallbackImage(id) {
  return `https://picsum.photos/seed/booking-${encodeURIComponent(id || 'event')}/720/420`
}

function applyEvent(detail) {
  rawEvent.value = detail || {}
  event.id = detail?.id || eventId || ''
  event.title = detail?.title || 'Untitled event'
  event.date = formatEventDate(detail?.startTime)
  event.time = formatEventTime(detail?.startTime, detail?.endTime)
  event.location = formatLocation(detail?.location)
  event.image = normalizeImageUrl(detail?.coverImageUrl)
    || normalizeImageUrl(detail?.imageUrls?.[0])
    || fallbackImage(detail?.id || eventId)
  event.price = Number(detail?.price || 0)
  event.capacity = Number(detail?.capacity || 0)
}

function normalizeStatus(status) {
  return String(status || '').trim().toUpperCase()
}

function isActiveRegistration(registration) {
  return !['CANCELLED', 'CANCELED', 'REJECTED'].includes(normalizeStatus(registration?.status))
}

function registrationEventId(registration) {
  return registration?.event?.id || registration?.eventId || ''
}

function extractList(res) {
  if (Array.isArray(res)) return res
  return res?.rows || res?.data || res?.list || res?.content || []
}

function persistConfirmation(registration, ticketItems = selectedTicketItems.value) {
  const registrations = Array.isArray(registration) ? registration : [registration].filter(Boolean)
  const primaryRegistration = registrations[0] || {}
  const payload = {
    registration: primaryRegistration,
    registrations,
    event: rawEvent.value || event,
    quantity: ticketItems.reduce((sum, item) => sum + item.quantity, 0),
    totalPrice: ticketItems.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0),
    ticketItems: ticketItems.map((item, index) => ({
      ticketTier: item.tier,
      quantity: item.quantity,
      unitPrice: item.unitPrice,
      totalPrice: item.unitPrice * item.quantity,
      registration: registrations[index] || null
    })),
    ticketTier: ticketItems[0]?.tier || null
  }
  sessionStorage.setItem('lastBookingConfirmation', JSON.stringify(payload))
}

async function loadTicketTiers() {
  try {
    const tiers = extractList(await getEventTicketTiers(eventId))
    ticketTiers.value = tiers.length ? tiers : []
    ensureTicketTierQuantities()
  } catch (error) {
    console.warn('Failed to load ticket tiers:', error)
    ticketTiers.value = []
    ensureTicketTierQuantities()
  }
}

async function loadExistingRegistration() {
  const profile = await getCurrentUserProfile()
  contact.value.fullName = contact.value.fullName || profile?.fullName || profile?.name || ''
  contact.value.email = contact.value.email || profile?.email || ''
  contact.value.phone = contact.value.phone || profile?.phone || ''

  const userId = profile?.id || profile?.userId || profile?.user?.id || profile?.profile?.id
  if (!userId) return

  const registrations = extractList(await getRegistrationsByUser(userId))
  const activeMatches = registrations.filter((registration) => {
    return String(registrationEventId(registration)) === String(eventId) && isActiveRegistration(registration)
  })

  existingActiveRegistrations.value = activeMatches
  const bookedKeys = new Set(activeMatches.map(registrationTicketTierKey))
  const bookableTiers = visibleTicketTiers.value.filter((tier) => !bookedKeys.has(ticketTierKey(tier)))

  if (activeMatches.length && bookableTiers.length === 0) {
    alreadyRegistered.value = true
    existingRegistration.value = activeMatches[0]
  }
}

function isTicketTierBooked(tier) {
  const key = ticketTierKey(tier)
  return existingActiveRegistrations.value.some((registration) => registrationTicketTierKey(registration) === key)
}

async function loadPage() {
  if (!eventId) {
    ElMessage.error('Missing event id')
    return
  }

  loading.value = true
  try {
    const detail = await getEventDetail(eventId)
    applyEvent(detail)
    await loadTicketTiers()
    await loadExistingRegistration()
  } catch (error) {
    console.error('Failed to load booking page:', error)
    ElMessage.error('Failed to load booking details')
  } finally {
    loading.value = false
  }
}

function confirmBooking() {
  if (!eventId) {
    ElMessage.error('Missing event id')
    return
  }

  const contactEmail = contact.value.email.trim()
  if (contactEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(contactEmail)) {
    ElMessage.warning('Please enter a valid contact email')
    return
  }

  if (alreadyRegistered.value && existingRegistration.value) {
    persistConfirmation(existingRegistration.value, [{
      tier: {
        id: existingRegistration.value.ticketTierId || 'existing-ticket',
        name: existingRegistration.value.ticketTierName || 'Standard',
        price: Number(existingRegistration.value.unitPrice || existingRegistration.value.totalPrice || 0)
      },
      quantity: Number(existingRegistration.value.quantity || 1),
      unitPrice: Number(existingRegistration.value.unitPrice || existingRegistration.value.totalPrice || 0)
    }])
    ElMessage.info('You are already registered for this activity.')
    router.push({
      path: '/product/bookingConfirmation',
      query: { eventId }
    })
    return
  }

  if (!selectedTicketItems.value.length || totalQuantity.value <= 0) {
    ElMessage.warning('Please select at least one ticket')
    return
  }

  submitting.value = true
  const itemsToSubmit = selectedTicketItems.value
    .filter((item) => !isTicketTierBooked(item.tier))
    .map((item) => ({ ...item }))
  const createdRegistrations = []

  if (!itemsToSubmit.length) {
    ElMessage.warning('Please select a ticket tier you have not booked yet')
    submitting.value = false
    return
  }

  itemsToSubmit.reduce((promise, item) => {
    return promise.then(async () => {
      const registration = await createEventRegistration({
        eventId,
        status: 'REGISTERED',
        ticketTierId: item.tier?.id === 'fallback-standard' ? null : item.tier?.id,
        quantity: item.quantity,
        contactFullName: contact.value.fullName.trim(),
        contactEmail,
        contactPhone: contact.value.phone.trim()
      })
      createdRegistrations.push(registration)
    })
  }, Promise.resolve()).then(() => {
    persistConfirmation(createdRegistrations, itemsToSubmit)
    ElMessage.success('Booking confirmed')
    router.push({
      path: '/product/bookingConfirmation',
      query: { eventId }
    })
  }).catch(error => {
    if (isAlreadyRegisteredError(error)) {
      alreadyRegistered.value = true
      ElMessage.warning('You are already registered for this activity.')
      return
    }
    console.error('Failed to confirm booking:', error)
  }).finally(() => {
    submitting.value = false
  })
}

onMounted(loadPage)
</script>

<style scoped lang="scss">
.book-activity-page {
  min-height: 100vh;
  background: #f7faff;
  color: #10234f;
  font-family: Inter, "Segoe UI", Arial, sans-serif;
}

.booking-shell {
  padding: 36px 44px 48px;
}

.page-heading {
  margin-bottom: 28px;
}

.page-heading h1 {
  margin: 0;
  color: #0d1f4f;
  font-size: 44px;
  line-height: 1.15;
  font-weight: 800;
  letter-spacing: 0;
}

.page-heading p {
  margin: 12px 0 0;
  color: #1d3264;
  font-size: 18px;
}

.booking-alert {
  margin-bottom: 18px;
}

.booking-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 28px;
  align-items: start;
}

.booking-card,
.summary-card {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #e0e8f6;
  border-radius: 14px;
  box-shadow: 0 12px 28px rgba(26, 54, 100, 0.09);
}

.booking-card {
  padding: 28px;
}

.form-section {
  min-width: 0;
}

.section-title {
  margin-bottom: 18px;
  display: flex;
  align-items: center;
  gap: 14px;
}

.section-icon {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border: 1px solid #2f75f6;
  border-radius: 50%;
  color: #0f66e9;
  background: #f4f8ff;
  font-size: 18px;
}

.section-title h2 {
  margin: 0;
  color: #0f66e9;
  font-size: 20px;
  font-weight: 800;
}

.event-summary {
  display: grid;
  grid-template-columns: 286px minmax(0, 1fr);
  gap: 34px;
  align-items: center;
}

.event-image {
  width: 100%;
  height: 166px;
  border-radius: 14px;
  object-fit: cover;
}

.event-copy h3 {
  margin: 0 0 22px;
  color: #071a47;
  font-size: 24px;
  line-height: 1.25;
  font-weight: 800;
}

.event-meta {
  display: grid;
  gap: 13px;
}

.event-meta p {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 14px;
  color: #1c2c57;
  font-size: 16px;
}

.event-meta .el-icon {
  color: #0f66e9;
  font-size: 20px;
}

.ticket-row {
  min-height: 74px;
  padding: 14px 18px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px 120px;
  gap: 20px;
  align-items: center;
  border: 1px solid #d8e3f4;
  border-radius: 12px;
  background: #fbfdff;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.ticket-row.selected {
  border-color: #0f66e9;
  box-shadow: 0 0 0 3px rgba(15, 102, 233, 0.12);
}

.ticket-row.disabled {
  cursor: not-allowed;
  background: #f2f5fa;
  opacity: 0.72;
}

.ticket-row h3 {
  margin: 0;
  color: #071a47;
  font-size: 17px;
  font-weight: 800;
}

.ticket-row p {
  margin: 5px 0 0;
  color: #667091;
  font-size: 14px;
}

.ticket-row strong {
  justify-self: end;
  color: #071a47;
  font-size: 19px;
}

.quantity-control {
  height: 46px;
  display: grid;
  grid-template-columns: 56px 1fr 56px;
  overflow: hidden;
  border: 1px solid #d2ddeb;
  border-radius: 10px;
  background: #fff;
}

.quantity-control .el-button {
  height: 44px;
  border: 0;
  border-radius: 0;
  color: #071a47;
  background: #fff;
}

.quantity-control span {
  display: grid;
  place-items: center;
  border-left: 1px solid #d2ddeb;
  border-right: 1px solid #d2ddeb;
  color: #071a47;
  font-size: 16px;
  font-weight: 700;
}

.total-row {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  align-items: baseline;
  gap: 28px;
  color: #071a47;
  font-size: 16px;
  font-weight: 700;
}

.total-row strong {
  color: #0f66e9;
  font-size: 24px;
}

.contact-form {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.contact-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.contact-form :deep(.el-form-item__label) {
  margin-bottom: 7px;
  color: #1c2c57;
  font-size: 14px;
  font-weight: 600;
}

.contact-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #d6dfef inset;
}

.bottom-actions {
  margin-top: 26px;
  display: grid;
  grid-template-columns: 330px minmax(0, 1fr);
  gap: 18px;
}

.back-button,
.confirm-button,
.summary-confirm {
  height: 56px;
  border-radius: 11px;
  font-size: 17px;
  font-weight: 700;
}

.back-button {
  border-color: #2f75f6;
  color: #0f66e9;
  background: #fff;
}

.confirm-button,
.summary-confirm {
  box-shadow: 0 10px 22px rgba(47, 117, 246, 0.22);
}

.summary-card {
  padding: 28px;
  position: sticky;
  top: 24px;
}

.summary-card h2 {
  margin: 0 0 26px;
  color: #071a47;
  font-size: 24px;
  font-weight: 800;
}

.summary-block {
  padding-bottom: 22px;
  border-bottom: 1px solid #dce5f4;
}

.summary-block span,
.summary-list span {
  display: block;
  color: #344466;
  font-size: 15px;
}

.summary-block strong {
  display: block;
  max-width: 260px;
  margin-top: 8px;
  color: #071a47;
  font-size: 17px;
  line-height: 1.45;
}

.summary-list {
  margin-top: 22px;
  display: grid;
  gap: 18px;
}

.summary-list div {
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr);
  gap: 10px;
}

.summary-list strong {
  color: #071a47;
  font-size: 16px;
  line-height: 1.45;
  font-weight: 500;
}

.summary-total {
  margin-top: 24px;
  padding: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid #d4e3fa;
  border-radius: 10px;
  background: #eef6ff;
}

.summary-total span {
  color: #071a47;
  font-size: 20px;
  font-weight: 800;
}

.summary-total strong {
  color: #0f66e9;
  font-size: 26px;
  font-weight: 800;
}

.summary-confirm {
  width: 100%;
  margin-top: 18px;
}

@media (max-width: 1180px) {
  .booking-shell {
    padding: 30px 24px 40px;
  }

  .booking-grid {
    grid-template-columns: 1fr;
  }

  .summary-card {
    position: static;
  }
}

@media (max-width: 820px) {
  .event-summary,
  .contact-form,
  .ticket-row,
  .bottom-actions {
    grid-template-columns: 1fr;
  }

  .ticket-row strong {
    justify-self: start;
  }

  .bottom-actions {
    gap: 12px;
  }
}

@media (max-width: 720px) {
  .booking-shell {
    padding: 24px 16px 32px;
  }

  .page-heading h1 {
    font-size: 36px;
  }

  .booking-card,
  .summary-card {
    padding: 20px;
  }
}
</style>
