<template>
  <div class="book-activity-page">
    <main class="booking-shell">
      <section class="page-heading">
        <h1>Book Activity</h1>
        <p>Review your details and confirm your booking.</p>
      </section>

      <section class="booking-grid">
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

              <div class="ticket-row">
                <div>
                  <h3>General Admission</h3>
                  <p>Standard activity booking</p>
                </div>

                <div class="quantity-control">
                  <el-button :icon="Minus" @click="decreaseQuantity" />
                  <span>{{ quantity }}</span>
                  <el-button :icon="Plus" @click="increaseQuantity" />
                </div>

                <strong>{{ formatPrice(unitPrice) }}</strong>
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
            <el-button size="large" type="primary" class="confirm-button" @click="confirmBooking">
              <el-icon><Lock /></el-icon>
              Confirm Booking
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
              <strong>General Admission x{{ quantity }}</strong>
            </div>
            <div>
              <span>Price</span>
              <strong>{{ formatPrice(unitPrice) }}</strong>
            </div>
          </div>

          <div class="summary-total">
            <span>Total</span>
            <strong>{{ formatPrice(totalPrice) }}</strong>
          </div>

          <el-button size="large" type="primary" class="summary-confirm" @click="confirmBooking">
            Confirm Booking
          </el-button>
        </aside>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createEventRegistration } from '@/api/events/detail'
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

const unitPrice = 45
const eventId = route.query.eventId || route.query.id
const quantity = ref(Number(route.query.quantity) || 1)

const event = {
  title: 'Sunset Sounds: Outdoor Acoustic Concert',
  date: 'Sat, 24 May 2025',
  time: '6:30 PM - 9:00 PM',
  location: 'Riverside Park, Central Promenade, Singapore',
  image: 'https://images.unsplash.com/photo-1525625293386-3f8f99389edd?auto=format&fit=crop&w=720&q=80'
}

const contact = ref({
  fullName: 'Sarah Pang',
  email: 'sarah.pang@example.com',
  phone: '+65 8123 4567'
})

const totalPrice = computed(() => unitPrice * quantity.value)

function increaseQuantity() {
  quantity.value += 1
}

function decreaseQuantity() {
  if (quantity.value > 1) {
    quantity.value -= 1
  }
}

function formatPrice(value) {
  return `S$${value.toFixed(2)}`
}

function backToEvent() {
  router.push({
    path: '/product/eventDetails',
    query: eventId ? { id: eventId } : {}
  })
}

function confirmBooking() {
  if (!eventId) {
    ElMessage.error('Missing event id')
    return
  }

  createEventRegistration({
    eventId,
    quantity: quantity.value,
    fullName: contact.value.fullName,
    email: contact.value.email,
    phone: contact.value.phone
  }).then(() => {
    ElMessage.success('Booking confirmed')
    router.push('/product/bookingConfirmation')
  }).catch(error => {
    console.error('Failed to confirm booking:', error)
  })
}
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
