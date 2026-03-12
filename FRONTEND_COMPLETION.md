# API 开放平台 - 前端完成总结

## 项目概述
这是一个基于 Vue 3 + TypeScript + Element Plus 的 API 开放平台前端，配合 Spring Boot 后端提供完整的 API 管理和交易功能。

## 已完成的功能模块

### 1. 用户端功能

#### 主页 (Home.vue)
- 欢迎页面，展示平台特色
- 快捷导航到 API 市场、我的接口、我的应用

#### API 市场 (ApiMarket.vue)
- 浏览所有可用的 API 接口
- 按接口查看套餐列表
- 支持购买套餐（集成支付宝支付）
- 分页展示接口列表

#### 我的接口 (MyInterfaces.vue)
- 查看已购买的接口授权
- 显示调用次数、剩余次数、过期时间
- 支持接口调用测试
- 实时更新调用次数

#### 我的应用 (MyApps.vue)
- 创建新应用
- 查看应用的 Access Key 和 Secret Key
- 支持复制密钥
- 显示应用创建时间和状态

#### 个人中心 (Profile.vue)
- 查看用户信息
- 退出登录功能

### 2. 管理员功能

#### 仪表盘 (Dashboard.vue)
- 显示平台统计数据
- 用户总数、今日调用、应用数量

#### 用户管理 (Users.vue)
- 分页查看所有用户
- 启用/禁用用户
- 删除用户功能
- 显示用户角色、邮箱、手机号

#### 应用管理 (Apps.vue)
- 查看所有用户应用
- 显示应用的 Access Key 和 Secret Key
- 应用状态管理

#### 接口管理 (Interfaces.vue)
- 新增/编辑/删除 API 接口
- 设置接口名称、URL、请求方法、描述
- 启用/禁用接口
- 分页查看接口列表

#### 产品套餐管理 (Products.vue)
- 新增/编辑/删除产品套餐
- 设置套餐名称、调用次数、价格
- 上架/下架套餐
- 分页查看套餐列表

### 3. 公共功能

#### 登录 (Login.vue)
- 用户名密码登录
- 自动跳转到对应角色首页

#### 注册 (Register.vue)
- 新用户注册
- 密码确认验证

#### 404 页面 (NotFound.vue)
- 路由不存在时显示

## API 接口文件

### 前端 API 模块
- `src/api/user.ts` - 用户认证接口
- `src/api/app.ts` - 应用管理接口
- `src/api/interface.ts` - API 接口管理
- `src/api/product.ts` - 产品套餐管理
- `src/api/auth.ts` - 用户接口授权
- `src/api/order.ts` - 订单和支付
- `src/api/admin.ts` - 管理员功能

### 工具函数
- `src/utils/request.ts` - Axios 请求拦截器（自动添加 JWT 和签名验证）
- `src/utils/sign.ts` - HmacSHA256 签名生成工具

## 后端修改

### 新增/修改的服务类
- `UserService.java` - 添加分页查询、状态更新、删除用户功能
- `ApiInterfaceService.java` - 统一返回 Result 类型
- `ApiProductService.java` - 统一返回 Result 类型
- `UserInterfaceAuthService.java` - 统一返回 Result 类型

### 新增/修改的控制器
- `UserController.java` - 添加用户管理接口
- `AppController.java` - 添加应用列表查询接口
- `ApiInterfaceController.java` - 统一返回类型
- `ApiProductController.java` - 统一返回类型
- `UserInterfaceAuthController.java` - 统一返回类型

## 路由配置

### 用户路由
- `/user/home` - 主页
- `/user/api-market` - API 市场
- `/user/my-interfaces` - 我的接口
- `/user/my-apps` - 我的应用
- `/user/profile` - 个人中心

### 管理员路由
- `/admin/dashboard` - 仪表盘
- `/admin/users` - 用户管理
- `/admin/apps` - 应用管理
- `/admin/interfaces` - 接口管理
- `/admin/products` - 产品套餐

### 公共路由
- `/login` - 登录
- `/register` - 注册
- `/` - 重定向到用户主页

## 技术栈

### 前端
- Vue 3 + TypeScript
- Vite 构建工具
- Element Plus UI 框架
- Pinia 状态管理
- Axios HTTP 客户端
- CryptoJS 加密库

### 后端
- Spring Boot 3.5.11
- Spring Security + JWT
- MyBatis-Plus ORM
- MySQL 数据库
- Redis 缓存
- RabbitMQ 消息队列

## 功能特性

1. **完整的用户认证系统** - 支持登录、注册、JWT 令牌管理
2. **API 市场** - 用户可以浏览和购买 API 套餐
3. **应用管理** - 用户可以创建应用并获取 Access Key/Secret Key
4. **接口授权** - 管理员可以管理用户对接口的访问权限
5. **调用统计** - 记录用户的 API 调用次数和使用情况
6. **支付集成** - 集成支付宝支付功能
7. **权限控制** - 基于角色的访问控制（RBAC）

## 使用说明

### 前端启动
```bash
cd api-web-vue
npm install
npm run dev
```

### 后端启动
```bash
cd api-admin
mvn clean install
mvn spring-boot:run
```

## 注意事项

1. 前端请求会自动添加 JWT 令牌和签名验证头
2. 所有 API 返回统一的 Result 格式：`{ code, msg, data }`
3. 管理员用户角色为 `ADMIN`，普通用户为 `USER`
4. 支付宝支付需要配置相应的密钥和回调地址
5. 接口调用次数限制由 `UserInterfaceAuth` 表管理

## 后续优化建议

1. 添加 API 调用日志查询页面
2. 添加订单管理和支付记录查询
3. 添加数据统计和图表展示
4. 添加用户反馈和问题报告功能
5. 优化移动端适配
6. 添加国际化支持
