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
              <span>Booking ID</span>
              <strong class="booking-id">{{ booking.id }}</strong>
            </div>
            <div>
              <span>Ticket type</span>
              <strong>{{ booking.ticketType }}</strong>
            </div>
            <div>
              <span>Quantity</span>
              <strong>{{ booking.quantity }}</strong>
            </div>
            <div>
              <span>Total paid</span>
              <strong>{{ booking.totalPaid }}</strong>
            </div>
          </div>
        </section>

        <div class="action-row">
          <el-button size="large" type="primary" class="primary-action" @click="viewMyEvents">
            <el-icon><Tickets /></el-icon>
            View My Events
          </el-button>
          <el-button size="large" class="secondary-action" @click="backToEvent">
            <el-icon><ArrowLeft /></el-icon>
            Back to Event
          </el-button>
          <el-button size="large" class="calendar-action" @click="addToCalendar">
            <el-icon><Calendar /></el-icon>
            Add to Calendar
          </el-button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Calendar,
  Check,
  Clock,
  Location,
  Tickets
} from '@element-plus/icons-vue'

const router = useRouter()

const event = {
  title: 'Sunset Sounds: Outdoor Acoustic Concert',
  date: 'Sat, 24 May 2025',
  time: '6:30 PM - 9:00 PM',
  location: 'Riverside Park, Central Promenade, Singapore',
  image: 'https://images.unsplash.com/photo-1525625293386-3f8f99389edd?auto=format&fit=crop&w=720&q=80'
}

const booking = {
  id: 'AE-2025-0524-7894',
  ticketType: 'General Admission',
  quantity: 1,
  totalPaid: 'S$45.00'
}

function viewMyEvents() {
  router.push('/my-events/joined')
}

function backToEvent() {
  router.push('/product/eventDetails')
}

function addToCalendar() {
  ElMessage.success('Added to calendar')
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

.action-row {
  margin-top: 28px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.primary-action,
.secondary-action,
.calendar-action {
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

.calendar-action {
  border-color: #d3ddeb;
  color: #344466;
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
