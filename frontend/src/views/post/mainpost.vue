<template>
  <div class="posts-page">
    <header class="platform-header">
      <div class="brand">
        <div class="brand-mark">
          <el-icon><UserFilled /></el-icon>
        </div>
        <div>
          <div class="brand-title">Accessible Events</div>
          <div class="brand-subtitle">Platform</div>
        </div>
      </div>

      <div class="global-search">
        <el-icon><Search /></el-icon>
        <span>Search events, posts, categories...</span>
      </div>

      <nav class="header-actions" aria-label="Primary">
        <button class="icon-button" aria-label="Home">
          <el-icon><HomeFilled /></el-icon>
        </button>
        <button class="icon-button" aria-label="Events">
          <el-icon><Calendar /></el-icon>
        </button>
        <button class="icon-button" aria-label="Messages">
          <el-icon><ChatDotRound /></el-icon>
        </button>
        <button class="icon-button notification-button" aria-label="Notifications">
          <el-icon><Bell /></el-icon>
          <span>3</span>
        </button>
        <div class="profile">
          <img src="@/assets/images/profile.jpg" alt="Sarah Johnson" />
          <span>Sarah Johnson</span>
          <el-icon><ArrowDown /></el-icon>
        </div>
      </nav>
    </header>

    <main class="posts-shell">
      <section class="hero-row">
        <div>
          <h1>My Posts</h1>
          <p>Manage and organize your posts.</p>
        </div>
        <el-button type="primary" class="create-button" @click="goCreatePost">
          <el-icon><Plus /></el-icon>
          Create New Post
        </el-button>
      </section>

      <section class="content-grid">
        <aside class="side-panel">
          <div class="tabs-card">
            <button
              v-for="tab in tabs"
              :key="tab.value"
              class="post-tab"
              :class="{ active: activeTab === tab.value }"
              @click="activeTab = tab.value"
            >
              <span class="tab-label">
                <el-icon><Document /></el-icon>
                {{ tab.label }}
              </span>
              <span class="tab-count">{{ countByStatus(tab.value) }}</span>
            </button>
          </div>

          <div class="tip-card">
            <div class="tip-icon">
              <el-icon><Reading /></el-icon>
            </div>
            <div>
              <h3>Tip</h3>
              <p>Publish your posts to share important updates and insights with the community.</p>
            </div>
          </div>
        </aside>

        <section class="posts-card">
          <div class="list-toolbar">
            <div class="toolbar-spacer"></div>
            <div class="sort-control">
              <span>Sort by:</span>
              <el-select v-model="sortBy" size="large">
                <el-option label="Most Recent" value="recent" />
                <el-option label="Oldest" value="oldest" />
              </el-select>
            </div>
          </div>

          <div class="post-list">
            <article v-for="post in filteredPosts" :key="post.id" class="post-card">
              <img :src="post.cover" :alt="post.title" class="post-cover" />

              <div class="post-body">
                <h2>{{ post.title }}</h2>
                <p class="related-event">
                  <el-icon><Calendar /></el-icon>
                  {{ post.relatedEvent }}
                </p>
                <p class="summary">{{ post.summary }}</p>
              </div>

              <div class="post-date">
                <span>{{ post.dateLabel }}</span>
                <strong>{{ post.date }}</strong>
              </div>

              <div class="post-actions">
                <el-tag
                  class="status-tag"
                  :class="post.status === 'Published' ? 'published' : 'draft'"
                  effect="plain"
                >
                  <span class="status-dot"></span>
                  {{ post.status }}
                </el-tag>

                <div class="button-row">
                  <el-button>
                    <el-icon><View /></el-icon>
                    View
                  </el-button>
                  <el-button>
                    <el-icon><EditPen /></el-icon>
                    Edit
                  </el-button>
                  <el-button class="delete-button">
                    <el-icon><Delete /></el-icon>
                    Delete
                  </el-button>
                </div>
              </div>
            </article>

            <el-empty v-if="filteredPosts.length === 0" description="No posts found" />
          </div>
        </section>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowDown,
  Bell,
  Calendar,
  ChatDotRound,
  Delete,
  Document,
  EditPen,
  HomeFilled,
  Plus,
  Reading,
  Search,
  UserFilled,
  View
} from '@element-plus/icons-vue'

const activeTab = ref('Published')
const sortBy = ref('recent')
const router = useRouter()

const tabs = [
  { label: 'Published', value: 'Published' },
  { label: 'Draft', value: 'Draft' }
]

const posts = ref([
  {
    id: 1,
    title: 'Creating Inclusive Event Spaces',
    relatedEvent: 'Accessibility Summit 2024',
    dateLabel: 'Published on',
    date: 'May 18, 2024',
    summary: "Designing inclusive environments is more than compliance-it is about creating experiences...",
    status: 'Published',
    cover: 'https://images.unsplash.com/photo-1573164713988-8665fc963095?auto=format&fit=crop&w=420&q=80'
  },
  {
    id: 2,
    title: 'Communication Access: Key Considerations',
    relatedEvent: 'Inclusion in Action Conference',
    dateLabel: 'Updated on',
    date: 'May 10, 2024',
    summary: 'Effective communication access ensures that everyone can fully participate in events...',
    status: 'Published',
    cover: 'https://images.unsplash.com/photo-1475721027785-f74eccf877e2?auto=format&fit=crop&w=420&q=80'
  },
  {
    id: 3,
    title: 'Digital Accessibility Checklist',
    relatedEvent: 'Virtual Accessibility Workshop',
    dateLabel: 'Updated on',
    date: 'Apr 28, 2024',
    summary: 'Use this practical checklist to evaluate and improve the accessibility of your digital event content...',
    status: 'Published',
    cover: 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=420&q=80'
  },
  {
    id: 4,
    title: 'Planning Accessible Events: Where to Start',
    relatedEvent: 'Accessibility Summit 2024',
    dateLabel: 'Created on',
    date: 'Apr 20, 2024',
    summary: 'New to accessibility? Start here. We break down the essential steps to plan more inclusive events...',
    status: 'Draft',
    cover: 'https://images.unsplash.com/photo-1560264280-88b68371db39?auto=format&fit=crop&w=420&q=80'
  }
])

const filteredPosts = computed(() => {
  const result = posts.value.filter((post) => post.status === activeTab.value)
  return sortBy.value === 'oldest' ? [...result].reverse() : result
})

function countByStatus(status) {
  return posts.value.filter((post) => post.status === status).length
}

function goCreatePost() {
  router.push('/product/createPost')
}
</script>

<style scoped lang="scss">
.posts-page {
  min-height: 100vh;
  background: #f7faff;
  color: #10234f;
  font-family: Inter, "Segoe UI", Arial, sans-serif;
}

.platform-header {
  height: 88px;
  padding: 0 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid #dfe7f5;
  box-shadow: 0 8px 28px rgba(35, 67, 119, 0.05);
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 260px;
}

.brand-mark {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 28px;
  background: linear-gradient(145deg, #1d65ef, #5f96ff);
}

.brand-title {
  font-size: 18px;
  line-height: 1.2;
  font-weight: 700;
  color: #0d1f4f;
}

.brand-subtitle {
  margin-top: 3px;
  font-size: 18px;
  color: #5c95ff;
}

.global-search {
  width: min(580px, 42vw);
  height: 58px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid #ccd8ec;
  border-radius: 12px;
  background: #fbfdff;
  color: #7480a1;
  font-size: 16px;
}

.global-search .el-icon {
  font-size: 24px;
  color: #627392;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 24px;
}

.icon-button {
  width: 36px;
  height: 36px;
  padding: 0;
  display: grid;
  place-items: center;
  position: relative;
  border: 0;
  background: transparent;
  color: #071a47;
  font-size: 24px;
  cursor: pointer;
}

.notification-button span {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  position: absolute;
  top: -4px;
  right: -4px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  background: #2f75f6;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
}

.profile {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #0d1f4f;
  font-size: 14px;
}

.profile img {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 8px 18px rgba(20, 42, 79, 0.12);
}

.posts-shell {
  padding: 36px 44px;
}

.hero-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.hero-row h1 {
  margin: 0;
  font-size: 44px;
  line-height: 1.15;
  font-weight: 800;
  color: #0d1f4f;
  letter-spacing: 0;
}

.hero-row p {
  margin: 12px 0 0;
  color: #1d3264;
  font-size: 18px;
}

.create-button {
  height: 50px;
  padding: 0 24px;
  border-radius: 10px;
  font-size: 16px;
  box-shadow: 0 10px 20px rgba(47, 117, 246, 0.22);
}

.content-grid {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 28px;
}

.side-panel,
.posts-card {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #e0e8f6;
  border-radius: 14px;
  box-shadow: 0 12px 28px rgba(26, 54, 100, 0.09);
}

.side-panel {
  min-height: 720px;
  padding: 28px 22px;
}

.tabs-card {
  padding: 0 0 28px;
  border-bottom: 1px solid #dce5f4;
}

.post-tab {
  width: 100%;
  min-height: 64px;
  padding: 0 16px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 0;
  border-radius: 12px;
  background: transparent;
  color: #0d1f4f;
  cursor: pointer;
  font-size: 17px;
}

.post-tab.active {
  background: #eef6ff;
  box-shadow: inset 0 0 0 1px #d6e7ff;
  color: #0969f6;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 14px;
}

.tab-label .el-icon {
  font-size: 24px;
}

.tab-count {
  min-width: 34px;
  height: 34px;
  padding: 0 8px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: #eaf1fb;
  color: #0d62e9;
  font-size: 15px;
  font-weight: 700;
}

.tip-card {
  margin-top: 36px;
  padding: 20px;
  min-height: 170px;
  display: flex;
  gap: 16px;
  border: 1px solid #dbe8fb;
  border-radius: 12px;
  background: #f1f7ff;
}

.tip-icon {
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #2f75f6;
  color: #fff;
  font-size: 20px;
}

.tip-card h3 {
  margin: 6px 0 14px;
  font-size: 16px;
  font-weight: 700;
  color: #0f66e9;
}

.tip-card p {
  margin: 0;
  color: #415178;
  font-size: 15px;
  line-height: 1.65;
}

.posts-card {
  padding: 20px 24px 12px;
}

.list-toolbar {
  height: 54px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.sort-control {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #26365f;
  font-size: 14px;
}

.sort-control :deep(.el-select) {
  width: 156px;
}

.sort-control :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #d6dfef inset;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.post-card {
  min-height: 154px;
  padding: 16px;
  display: grid;
  grid-template-columns: 200px minmax(260px, 1fr) 150px 330px;
  gap: 24px;
  align-items: center;
  border: 1px solid #dde7f5;
  border-radius: 14px;
  background: #fff;
}

.post-cover {
  width: 200px;
  height: 120px;
  border-radius: 10px;
  object-fit: cover;
}

.post-body h2 {
  margin: 0 0 10px;
  color: #0d1f4f;
  font-size: 20px;
  line-height: 1.2;
  font-weight: 800;
  letter-spacing: 0;
}

.related-event {
  margin: 0 0 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #0969f6;
  font-size: 14px;
}

.summary {
  margin: 0;
  max-width: 420px;
  color: #667091;
  font-size: 15px;
  line-height: 1.6;
}

.post-date {
  align-self: start;
  padding-top: 22px;
  color: #647092;
  font-size: 14px;
}

.post-date span,
.post-date strong {
  display: block;
  font-weight: 400;
  line-height: 1.7;
}

.post-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 32px;
}

.status-tag {
  height: 32px;
  min-width: 112px;
  justify-content: center;
  border-radius: 9px;
  font-size: 14px;
}

.status-tag :deep(.el-tag__content) {
  display: flex;
  align-items: center;
  gap: 10px;
}

.status-tag.published {
  border-color: #c9edd7;
  background: #e8f9ee;
  color: #10833f;
}

.status-tag.draft {
  border-color: #ffe1ad;
  background: #fff7e8;
  color: #f18a00;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.button-row {
  display: flex;
  gap: 16px;
}

.button-row .el-button {
  width: 104px;
  height: 44px;
  border-radius: 9px;
  border-color: #d5deef;
  color: #0d1f4f;
  font-size: 14px;
}

.button-row .delete-button {
  color: #f3222d;
}

@media (max-width: 1280px) {
  .platform-header {
    padding: 0 24px;
  }

  .posts-shell {
    padding: 30px 24px;
  }

  .content-grid {
    grid-template-columns: 280px minmax(0, 1fr);
  }

  .post-card {
    grid-template-columns: 180px minmax(220px, 1fr) 120px;
  }

  .post-cover {
    width: 180px;
  }

  .post-actions {
    grid-column: 2 / 4;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
    gap: 18px;
  }
}

@media (max-width: 980px) {
  .platform-header {
    height: auto;
    padding: 18px 20px;
    flex-wrap: wrap;
  }

  .brand,
  .header-actions {
    min-width: 0;
  }

  .global-search {
    order: 3;
    width: 100%;
  }

  .content-grid {
    grid-template-columns: 1fr;
  }

  .side-panel {
    min-height: auto;
  }
}

@media (max-width: 720px) {
  .posts-shell {
    padding: 24px 16px;
  }

  .hero-row {
    align-items: flex-start;
    flex-direction: column;
    gap: 18px;
  }

  .hero-row h1 {
    font-size: 36px;
  }

  .post-card {
    grid-template-columns: 1fr;
  }

  .post-cover {
    width: 100%;
    height: 180px;
  }

  .post-date {
    padding-top: 0;
  }

  .post-actions {
    grid-column: auto;
    align-items: stretch;
    flex-direction: column;
  }

  .button-row {
    flex-wrap: wrap;
  }

  .button-row .el-button {
    flex: 1 1 120px;
  }
}
</style>
