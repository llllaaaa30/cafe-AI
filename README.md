# 智能咖啡店客服系统

基于 Spring AI Alibaba + Graph 工作流 + RAG 知识库的智能客服对话系统，支持自然语言下单、查订单、取消订单及商品咨询，提供流式对话体验。

## 项目架构

```
┌────────────────────────────────────────────────────────────┐
│                      index.html (前端)                       │
│                    localhost:8080                            │
│                 POST /api/workflows/run                      │
└──────────────────────────┬─────────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────────┐
│            intelligent-customer-service (端口 8080)          │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Graph 工作流引擎                                        │ │
│  │  START → intentRecognition → 条件路由 → ... → END       │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  RAG 知识库检索        对话记忆(滑动窗口+增量压缩)          │ │
│  └────────────────────────────────────────────────────────┘ │
│                         │ OpenFeign                         │
└─────────────────────────┼──────────────────────────────────┘
                          │
┌─────────────────────────▼──────────────────────────────────┐
│               order-server (端口 8083)                       │
│    下单 / 查订单 / 取消订单 / 订单详情     MCP Server         │
│                     MySQL t_order                            │
└────────────────────────────────────────────────────────────┘
```

## 技术栈

| 依赖 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.3 | 基础框架 |
| Spring AI | 1.0.1 | AI 对话、MCP 客户端、向量存储 |
| Spring AI Alibaba Graph | 1.0.0.4 | StateGraph 工作流引擎 |
| Spring AI ZhipuAI | GLM-5.3-Flash / embedding-3 | 大语言模型 + Embedding |
| OpenFeign | 2025.0.0 | 微服务间 HTTP 调用 |
| Redis Stack | latest | 向量存储（RediSearch） |
| MySQL | 8.0 | 业务数据 + 对话记忆持久化 |
| EasyExcel | 3.3.4 | CSV 知识库解析 |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- Docker & Docker Compose

### 1. 启动基础设施

```bash
cd docker
docker-compose up -d
```

这会启动 MySQL (3307) 和 Redis Stack (6380)，并自动创建数据库表。

### 2. 配置智谱 AI API Key

在 `src/main/resources/application.yml` 中设置：

```yaml
spring:
  ai:
    zhipuai:
      api-key: ${ZP-KEY}   # 环境变量或直接替换为你的 API Key
```

### 3. 编译并启动主服务

```bash
mvn clean package -DskipTests
java -jar target/intelligent-customer-service-0.0.1-SNAPSHOT.jar
```

### 4. 编译并启动订单服务

```bash
cd order-server
mvn clean package -DskipTests
java -jar target/order-server-0.0.1-SNAPSHOT.jar
```

### 5. 访问

打开浏览器访问 `http://localhost:8080`，注册用户后即可开始对话。

## 核心功能

### 1. Graph 工作流引擎

基于 StateGraph 有向状态图实现意图识别与路由分发：

```
START → intentRecognition ──┬── 商品咨询 → knowledgeRetrieval → generateResponse → END
                            ├── 下单/查订单/取消订单 → orderHandler → END
                            └── 闲聊 → generateResponse → END
```

- **intentRecognition**：LLM 分类用户意图（商品咨询/下单/查订单/取消订单/闲聊）
- **knowledgeRetrieval**：从 Redis 向量库检索相关知识（TopK=3）
- **orderHandler**：通过 Feign 调用 order-server 处理订单操作
- **generateResponse**：根据意图选择 Prompt 模板，生成回复

### 2. RAG 检索增强生成

```
离线阶段：CSV知识库 → EasyExcel解析 → embedding-3向量化 → Redis VectorStore
在线阶段：用户输入 → 向量检索(TopK=3) → 注入Prompt → LLM基于知识库回答
```

知识库文件：[qa/QA.csv](qa/QA.csv)，包含 6 条咖啡相关 QA。

### 3. 对话记忆（滑动窗口 + 增量压缩）

- 窗口大小固定 5 轮，最近对话保留完整原文
- 溢出部分由 LLM 压缩为 200 字摘要存入 MySQL
- `compressedRoundCount` 字段做缓存命中，相同轮次零 LLM 调用
- 新增轮次仅增量压缩一轮，O(1) LLM 开销

### 4. MCP 协议（扩展能力）

order-server 通过 `@Tool` 注解暴露 MCP Tool：
- `createOrder` — 创建订单
- `orderDetail` — 订单详情
- `userOrder` — 用户订单查询
- `orderCancel` — 取消订单

### 5. 流式输出

将 LLM 调用从 Graph 节点下沉至 Service 层，通过 `chatClient.prompt().stream()` 实现逐 token SSE 推送，前端打字机效果。

## 项目结构

```
Intelligent_customer_SpringAIAlibaba-main/
├── docker/
│   ├── docker-compose.yml            # MySQL + Redis Stack
│   └── mysql-init/init.sql           # 数据库建表脚本
├── qa/QA.csv                         # 知识库文件
├── order-server/                     # 订单微服务 (端口 8083)
│   └── src/main/java/cn/sg/orderserver/
│       ├── mcp/OrderMcpController.java     # MCP Tool 暴露
│       ├── controller/OrderController.java # REST API
│       ├── entity/Order.java               # 订单实体
│       └── config/McpConfiguration.java    # MCP 配置
├── src/main/java/cn/sg/intelligentcustomerservice/
│   ├── application/
│   │   ├── service/
│   │   │   ├── WorkflowApplicationService.java   # 工作流编排
│   │   │   ├── ConversationMemoryService.java    # 对话记忆
│   │   │   └── DocumentLoaderService.java        # 知识库加载
│   │   └── workflow/
│   │       ├── IntelligentCustomerServiceWorkflowConfig.java  # Graph定义
│   │       └── node/
│   │           ├── IntentRecognitionNode.java    # 意图识别节点
│   │           ├── KnowledgeRetrievalNode.java   # 知识检索节点
│   │           ├── GenerateResponseNode.java     # 回答生成节点
│   │           └── OrderHandlerNode.java         # 订单处理节点
│   ├── domain/                                   # 领域层
│   ├── infrastructure/
│   │   ├── client/OrderClient.java               # Feign 客户端
│   │   └── interfaces/WorkflowController.java    # REST 控制器
│   └── IntelligentCustomerServiceApplication.java
└── src/main/resources/
    ├── static/index.html                         # 前端页面
    └── application.yml                           # 主配置
```

## API 接口

### 用户管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/user/register` | 注册用户 |

### 智能客服对话

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/workflows/run` | 流式对话（SSE） |
| POST | `/api/workflows/run/sync` | 全量对话 |

### 订单管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/order/create` | 创建订单 |
| POST | `/api/order/list` | 订单列表 |
| POST | `/api/order/userOrder` | 用户订单 |
| POST | `/api/order/detail` | 订单详情 |
| POST | `/api/order/complete` | 完成订单 |
| POST | `/api/order/cancel` | 取消订单 |

## 数据库表

| 表名 | 说明 |
|------|------|
| `t_user` | 用户信息 |
| `t_message` | 对话消息（user/assistant） |
| `t_conversation_summary` | 对话摘要缓存 |
| `t_order` | 订单数据 |

## 对话示例

```
用户: 你们有哪些咖啡？
AI:   我们提供多种经典咖啡饮品：美式咖啡（醇香浓郁）、拿铁（香浓奶香）...

用户: 我要一杯美式
AI:   已为您下单成功！
      订单号：ORDER1760973030123 | 美式 | ¥9.9 | 制作中

用户: 查一下我的订单
AI:   您的订单如下：
      1. 订单号：ORDER1760973030123 | 美式 | ¥9.9 | 制作中

用户: 帮我取消 ORDER1760973030123
AI:   订单 ORDER1760973030123 已成功取消并退款。
```