# CLAUDE.md — 自动加载入口

本仓库的 AI 员工手册是 **`AGENTS.md`**。开工阅读顺序与冲突优先级（**PRD → `docs/design/proposal_J_final.html` → `DESIGN_SYSTEM.md` → `ARCHITECTURE.md` → 现有代码**）、产品不变量、旧 UI 删除授权与数据安全约束，全部以 `AGENTS.md` 为准，本文件不另立规则。

## 环境事实

- 命令行编译需 JDK 17+；本机默认 JDK 11，请使用 Android Studio 内置 JBR：
  `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
- 视觉 token 唯一来源：`ui/theme/`（Color / Type / Shapes / MomentMarkTokens）+ `DESIGN_SYSTEM.md`；新 UI 禁止硬编码 `Color(0x…)`、裸 `dp/sp`、圆角、柔和阴影。
- 旧 UI / 旧逻辑删除授权与前置核查：见 `AGENTS.md` §2.3；删除后在 `DESIGN_SYSTEM.md` §7 删除登记表登记。
