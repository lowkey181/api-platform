# API 开放平台 / 接口网关系统

这是一个基于微服务架构的 **API 开放平台**，旨在为开发者提供安全、高效、可计费的接口调用服务。系统包含完整的接口生命周期管理、网关安全防护、支付宝支付集成以及实时日志统计功能。

## 🌟 核心功能

### 1. API 管理后台 (api-admin)
- **用户认证**：集成 Spring Security 6.x + JWT 认证，支持角色权限控制（ADMIN/USER）。
- **密钥管理**：为每个用户生成唯一的 `accessKey` 和 `secretKey`，用于接口调用验签。
- **接口市场**：管理员可发布接口，用户可在线浏览、申请并测试接口。
- **支付系统**：集成支付宝沙箱支付，支持用户在线充值及订单管理。
- **日志处理**：通过 RabbitMQ 异步消费网关生成的调用日志，实时统计接口调用情况。

### 2. 接口网关 (api-gateway)
- **安全验签**：基于 HmacSHA256 算法的 SignFilter，验证 `accessKey`、`sign`、`timestamp`、`nonce`。
- **防重放攻击**：利用 Redis 存储 `nonce` 并配合 5 分钟时间戳过期机制。
- **流量控制**：集成 Lua 脚本实现高性能的分布式限流（Rate Limiting）。
- **权限校验**：调用 `api-admin` 校验用户是否有权限调用特定接口。
- **黑名单防护**：实时拦截异常 IP 或非法用户的请求。

### 3. 前端界面 (api-web-vue)
- **现代化 UI**：基于 Vue 3 + TypeScript + Element Plus 打造，响应式设计。
- **开发者控制台**：管理个人密钥、查看已购接口、监控调用日志。
- **管理看板**：提供接口调用频率统计、用户增长趋势等可视化图表。

### 4. 示例 Demo & SDK
- 提供 `api-sdk` 快速接入包，封装签名生成逻辑。
- 包含 `demo` 项目，演示如何几行代码完成对网关保护接口的调用。

## 🛠️ 技术栈

| 维度 | 技术选型 |
| :--- | :--- |
| **后端核心** | Spring Boot 3.x, Spring Security 6.x |
| **持久层** | MyBatis Plus, MySQL |
| **中间件** | Redis (缓存/限流/防重放), RabbitMQ (异步日志) |
| **网关** | Spring Cloud Gateway (WebFlux 响应式) |
| **前端** | Vue 3, TypeScript, Vite, Element Plus, Pinia |
| **支付** | Alipay SDK (支付宝沙箱) |

## 📂 项目结构

```text
api/
├── api-admin/       # 管理后台服务（认证、业务、日志消费）
├── api-gateway/     # 统一网关服务（验签、限流、转发）
├── api-service/     # 示例 API 提供方服务
├── api-web-vue/     # 前端项目
├── demo/            # 客户端接入演示项目
└── out/             # 编译输出目录（包含 SDK jar 包）
```

## 🚀 快速开始

### 1. 环境准备
- JDK 17+
- MySQL 8.0+
- Redis
- RabbitMQ
- Node.js 18+

### 2. 数据库配置
导入 `api-admin` 目录下的 SQL 文件（如有），并修改各模块 `src/main/resources/application.yaml` 中的数据库连接信息。

### 3. 启动顺序
1. 启动 **Redis** 和 **RabbitMQ**。
2. 运行 `ApiAdminApplication` (端口: 9002)。
3. 运行 `ApiGatewayApplication` (端口: 8090)。
4. 运行 `ApiServiceApplication` (端口: 9003)。
5. 进入 `api-web-vue` 执行 `npm install` 且 `npm run dev` 启动前端。

## 🛡️ 安全机制 (签名算法)

所有通过网关的请求必须在 Header 中携带以下参数：
- `accessKey`: 开发者公钥
- `timestamp`: 当前时间戳（5 分钟内有效）
- `nonce`: 随机不重复字符串
- `sign`: 签名结果，计算方式：`HmacSHA256(accessKey=...&nonce=...&timestamp=..., secretKey)`

---
*本项目持续更新中，欢迎贡献代码或提出建议。*
