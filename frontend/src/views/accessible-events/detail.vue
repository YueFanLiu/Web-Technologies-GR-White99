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
      <h1 class="event-title">Adventure Playground Day</h1>

      <!-- 活动主图 -->
      <div class="event-image-wrapper">
        <img
          src="https://picsum.photos/id/1083/1200/600"
          alt="Adventure Playground Day"
          class="event-image"
        />
      </div>

      <!-- 活动信息卡片 -->
      <div class="event-info-card">
        <h2 class="event-name">Adventure Playground Day</h2>
        <div class="event-meta">
          <div class="meta-item">
            <el-icon><Calendar /></el-icon>
            <span>Saturday, April 25, 2026</span>
          </div>
          <div class="meta-item">
            <el-icon><Clock /></el-icon>
            <span>10:00 AM - 2:00 PM</span>
          </div>
          <div class="meta-item">
            <el-icon><Location /></el-icon>
            <span>Greenwood Park</span>
          </div>
          <div class="meta-item">
            <el-icon><Location /></el-icon>
            <span>123 Park Lane, Cityville</span>
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
            Join us for a fun day at Greenwood Park! Adventure Playground Day is designed for families with disabled or chronically ill children to enjoy a safe and accessible playground experience.
          </p>
          <p class="description-includes">The event includes:</p>
          <ul class="includes-list">
            <li>Responsive activities for all abilities</li>
            <li>Games and group activities</li>
            <li>Picnic area with accessible facilities</li>
          </ul>
        </div>
      </div>
    </el-aside>

    <!-- 右侧边栏 -->
    <el-aside width="40%" class="sidebar">
      <!-- 预订卡片 -->
      <div class="booking-card">
        <div class="price-row">
          <div class="price-info">
            <span class="price">$20 per Child</span>
          </div>
          <span class="price-label">Price per Child</span>
        </div>

        <div class="quantity-row">
          <el-button circle size="small" @click="decreaseCount">-</el-button>
          <span class="count">{{ childCount }}</span>
          <el-button circle size="small" @click="increaseCount">+</el-button>
          <el-button type="primary" class="book-btn">Book Now</el-button>
        </div>

        <div class="spots-left">
          <el-icon><Warning /></el-icon>
          <span>5 spots left</span>
        </div>
      </div>

      <!-- 无障碍信息卡片 -->
      <div class="accessibility-card">
        <h3>Accessibility Info</h3>
        <div class="accessibility-tags">
          <el-tag type="success" class="access-tag">
            <el-icon><Lock /></el-icon> Wheelchair Accessible
          </el-tag>
          <el-tag type="success" class="access-tag">
            <el-icon><Service /></el-icon> Elevator
          </el-tag>
          <el-tag type="success" class="access-tag">
            <el-icon><Message /></el-icon> Accessible Restroom
          </el-tag>
          <el-tag type="success" class="access-tag">
            <el-icon><Phone /></el-icon> Low Noise Level
          </el-tag>
        </div>
      </div>

      <!-- 评论区 -->
      <div class="reviews-card">
        <div class="reviews-header">
          <h3>Reviews</h3>
          <div class="rating-info">
            <el-icon><Star /></el-icon>
            <span class="rating">4.8</span>
            <span class="review-count">13 reviews</span>
          </div>
        </div>

        <div class="review-item">
          <div class="reviewer-info">
            <img src="https://picsum.photos/id/1027/100/100" alt="Emily S." class="reviewer-avatar" />
            <div class="reviewer-name">Emily S.</div>
            <div class="review-stars">
              <el-icon v-for="i in 5" :key="i" color="#f7ba2a"><Star /></el-icon>
            </div>
          </div>
          <p class="review-text">Great place for the kids, very accessible and fun!</p>
        </div>

        <div class="review-item">
          <div class="reviewer-info">
            <img src="https://picsum.photos/id/1012/100/100" alt="David R." class="reviewer-avatar" />
            <div class="reviewer-name">David R.</div>
            <div class="review-stars">
              <el-icon v-for="i in 4" :key="i" color="#f7ba2a"><Star /></el-icon>
              <el-icon color="#ddd"><Star /></el-icon>
            </div>
          </div>
          <p class="review-text">Nice event, facilities were good but a bit crowded.</p>
        </div>
      </div>
    </el-aside>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import {
  Calendar,
  Clock,
  Location,
  Lock,
  Service,
  Message,
  Phone,
  Warning,
  Star,
} from '@element-plus/icons-vue'

// 儿童数量
const childCount = ref(1)

const increaseCount = () => {
  childCount.value++
}

const decreaseCount = () => {
  if (childCount.value > 1) {
    childCount.value--
  }
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
.includes-list {
  list-style-type: disc;
  padding-left: 20px;
  color: #555;
  line-height: 1.8;
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
  margin-bottom: 20px;
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
</style>