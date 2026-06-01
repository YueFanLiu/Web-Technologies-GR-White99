# Frontend

这是 Web-Technologies-GR-White99 的前端项目。

前端基于 RuoYi Vue3 架构二次开发，保留了 RuoYi 的布局、路由、权限、请求封装、Pinia 状态管理和 Element Plus 组件基础；实际业务页面是本项目自定义实现，并对接 `../backend` 中小组自行开发的 Spring Boot 后端。

## 技术栈

- Vue 3
- Vite
- Element Plus
- Pinia
- Vue Router
- Axios
- Supabase JavaScript client
- Playwright 端到端测试

## 本地运行

先启动后端：

Windows:

```bash
cd ../backend
mvnw.cmd spring-boot:run
```

macOS / Linux:

```bash
cd ../backend
./mvnw spring-boot:run
```

再启动前端：

```bash
npm install
npm run dev
```

访问地址：

```text
http://localhost:5173
```

Vite 开发服务器会把 `/api` 和 `/v3/api-docs` 代理到：

```text
http://localhost:9192
```

## Scripts

```bash
npm run dev          # 启动 Vite 开发服务器
npm run build:prod   # 生产环境构建
npm run build:stage  # staging 环境构建
npm run preview      # 预览构建结果
npm run e2e          # 运行 Playwright 测试
npm run e2e:headed   # 以 headed 模式运行 Playwright
npm run e2e:ui       # 打开 Playwright UI 模式
```

## 环境变量

开发环境变量位于 `.env.development`。

常用变量：

```text
VITE_APP_TITLE
VITE_APP_ENV
VITE_APP_BASE_API
VITE_SUPABASE_URL
VITE_SUPABASE_ANON_KEY
```

开发环境中 `VITE_APP_BASE_API` 为空，因为接口代理由 Vite 配置处理。

## 源码结构

```text
src/
|-- api/                  后端接口封装
|-- assets/               图片、图标和公共样式
|-- components/           公共 Vue 组件
|-- constants/            共享常量
|-- layout/               RuoYi 布局外壳
|-- plugins/              RuoYi 风格的全局插件
|-- router/               路由定义
|-- store/                Pinia store
|-- utils/                请求、鉴权、Supabase、通用工具
|-- views/                业务页面和保留的 RuoYi 系统页面
```

## 业务页面

- 登录、注册、邮箱验证、忘记密码。
- 用户个人中心和公开用户主页。
- 无障碍活动浏览、详情、报名、评价、已参加活动。
- 组织者/管理员创建活动、管理活动、管理帖子、查看参与者列表。
- 社区帖子发布、详情、图片和评价。
- 好友、私信、活动私聊和通知中心。

## 后端接口

前端主要使用自定义后端接口，统一位于 `/api/...` 下：

- `/api/auth`
- `/api/users`
- `/api/events`
- `/api/locations`
- `/api/posts`
- `/api/registrations`
- `/api/friends`
- `/api/friend-requests`
- `/api/chats`
- `/api/notifications`

后端使用 Supabase access token 做鉴权。Axios 请求封装会在登录后自动携带：

```http
Authorization: Bearer <accessToken>
```

## 关于 RuoYi

本目录不是原版 RuoYi 前端说明。当前项目只使用 RuoYi Vue3 作为前端工程基础。

代码中仍保留了一些 RuoYi 系统/监控/工具页面和旧 API 模块，例如 `/system`、`/monitor`、`/tool`、`/getRouters` 等。如果后端没有实现这些接口，它们默认不可用；项目核心业务应以 `/api/...` 下的自定义接口为准。
