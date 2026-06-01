# Web-Technologies-GR-White99

这是一个面向无障碍活动与社区互动场景的 Web Technologies 课程项目。

项目采用前后端分离结构。前端基于 RuoYi Vue3 的架构进行二次开发，保留了它的布局、路由、权限、请求封装、Pinia 状态管理和 Element Plus 组件基础；业务页面和交互逻辑是本项目自定义实现。后端不是 RuoYi 原后端，而是小组自行开发的 Spring Boot REST API，连接 Supabase PostgreSQL、Supabase Auth 和 Supabase Storage。

## 主要功能

- 用户注册、登录、邮箱验证、忘记密码、个人资料编辑和头像上传。
- 无障碍活动浏览、搜索、详情、报名、收藏、评价和参与记录。
- 组织者/管理员创建活动、编辑活动、管理活动、查看参与者列表。
- 地点管理，包含无障碍设施信息和地点图片。
- 社区帖子发布、详情、图片、评价，以及关联活动/地点信息。
- 好友列表、好友申请、私信、活动私聊和通知中心。
- 地点、活动、帖子列表中加入推荐/排序评分逻辑。
- 后端提供 Swagger/OpenAPI 文档，便于调试接口。

## 技术栈

### 前端

- Vue 3
- Vite
- Element Plus
- Pinia
- Vue Router
- Axios
- RuoYi Vue3 前端架构
- Supabase JavaScript client
- Playwright 端到端测试

### 后端

- Java 17
- Spring Boot
- Spring Web MVC
- Spring Security OAuth2 Resource Server
- Spring Data JPA
- PostgreSQL on Supabase
- Supabase Auth / Storage
- Spring Mail
- Springdoc OpenAPI / Swagger UI
- Maven

## 目录结构

```text
.
|-- backend/              Spring Boot 后端 API
|-- frontend/             基于 RuoYi Vue3 架构的前端项目
|-- test/e2e/             Playwright 端到端测试配置
|-- README.md             项目总说明
```

后端主要结构：

```text
backend/src/main/java/fr/isep/projectweb/
|-- config/               安全配置和 OpenAPI 配置
|-- controller/           REST 接口控制器，主要路径为 /api/...
|-- model/dao/            Spring Data JPA Repository
|-- model/dto/            请求和响应 DTO
|-- model/entity/         数据库实体
|-- model/service/        业务逻辑、鉴权、Supabase 集成
|-- model/algorithm/      推荐评分算法
|-- view/api/             前端接口说明文档
```

前端主要结构：

```text
frontend/src/
|-- api/                  Axios 接口封装
|-- router/               静态路由和动态路由
|-- store/                Pinia 状态管理
|-- views/                业务页面和保留的 RuoYi 系统页面
|-- utils/                请求、鉴权、Supabase、通用工具
|-- layout/               RuoYi 布局外壳
```

## 环境要求

- Java 17
- Node.js 和 npm
- 使用 `backend/` 内置的 Maven Wrapper
- 可用的 Supabase 项目，包含 PostgreSQL、Auth 和 Storage

项目配置文件里有开发默认值，但本地开发和部署时建议通过环境变量覆盖数据库、邮箱和 Supabase 密钥。

## 环境变量

后端配置文件：

```text
backend/src/main/resources/application.yaml
```

常用后端环境变量：

```text
DB_URL
DB_USERNAME
DB_PASSWORD
SUPABASE_URL
SUPABASE_PUBLISHABLE_KEY
SUPABASE_SERVICE_ROLE_KEY
SUPABASE_IMAGES_BUCKET
SUPABASE_MAX_IMAGE_SIZE_BYTES
SUPABASE_ISSUER_URI
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
MAIL_HEALTH_ENABLED
```

前端环境文件：

```text
frontend/.env.development
frontend/.env.staging
frontend/.env.production
```

开发环境下，Vite 会把 `/api` 和 `/v3/api-docs` 代理到：

```text
http://localhost:9192
```

## 本地运行

### 1. 启动后端

Windows:

```bash
cd backend
mvnw.cmd spring-boot:run
```

macOS / Linux:

```bash
cd backend
./mvnw spring-boot:run
```

后端地址：

```text
http://localhost:9192
```

Swagger UI：

```text
http://localhost:9192/swagger-ui.html
```

### 2. 启动前端

打开另一个终端：

```bash
cd frontend
npm install
npm run dev
```

前端地址：

```text
http://localhost:5173
```

## 构建

前端生产构建：

```bash
cd frontend
npm run build:prod
```

前端 staging 构建：

```bash
cd frontend
npm run build:stage
```

后端打包：

Windows:

```bash
cd backend
mvnw.cmd package
```

macOS / Linux:

```bash
cd backend
./mvnw package
```

## 测试

后端测试：

Windows:

```bash
cd backend
mvnw.cmd test
```

macOS / Linux:

```bash
cd backend
./mvnw test
```

前端端到端测试：

```bash
cd frontend
npm run e2e
```

可视化模式：

```bash
npm run e2e:headed
npm run e2e:ui
```

## API 概览

自定义后端接口统一使用 `/api/...` 前缀。

- Auth：`/api/auth/...`
- Users：`/api/users/...`
- Locations：`/api/locations/...`
- Location accessibility：`/api/locations/{locationId}/accessibility`
- Location images：`/api/locations/{locationId}/images`
- Events：`/api/events/...`
- Event saves：`/api/events/{eventId}/save`
- Event images：`/api/events/{eventId}/images`
- Event reviews：`/api/events/{eventId}/reviews`
- Posts：`/api/posts/...`
- Post images：`/api/posts/{postId}/images`
- Post reviews：`/api/posts/{postId}/reviews`
- Registrations：`/api/registrations/...`
- Friends / Friend requests：`/api/friends`、`/api/friend-requests`
- Chats：`/api/chats/...`
- Notifications：`/api/notifications/...`

更详细的前端接口说明在：

```text
backend/src/main/java/fr/isep/projectweb/view/api/frontend-api-guide.md
```

## 鉴权说明

项目使用 Supabase Auth。前端调用后端登录/注册接口，后端与 Supabase Auth 交互并同步本地用户信息。受保护接口由 Spring Security Resource Server 校验 Supabase access token。

受保护接口需要请求头：

```http
Authorization: Bearer <accessToken>
```

部分公开读取接口不需要登录，例如 `GET /api/events/**`、`GET /api/locations/**`、`GET /api/posts/**`。

## 关于 RuoYi

前端仍保留 RuoYi Vue3 的部分基础模块，包括布局、权限指令、请求封装、系统/监控/工具页面和通用样式。这些内容主要作为前端工程基础使用。

后端不是 RuoYi 后端。本项目后端拥有自己的数据库模型、接口设计、鉴权流程、业务逻辑和推荐算法。因此，前端中仍保留的 `/system`、`/monitor`、`/tool`、`/getRouters` 等 RuoYi 旧接口，如果没有对应后端实现，默认不可用。

## License

本项目用于课程学习与展示。
