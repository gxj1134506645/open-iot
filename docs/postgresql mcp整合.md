# PostgreSQL MCP 整合说明

## 本次结论

本项目使用**项目级** PostgreSQL MCP（`F:\codes\open-iot\.mcp.json`），不是全局配置。

原因：

1. 数据库连接是强业务上下文配置，不同项目通常连接不同库。
2. 项目级配置随仓库走，团队成员进入项目即可复用同一套 MCP 定义。
3. 风险隔离更好，避免全局配置误连到其他项目。

## 当前配置内容（环境变量版）

文件：`F:\codes\open-iot\.mcp.json`

```json
{
  "mcpServers": {
    "postgres": {
      "command": "cmd",
      "args": [
        "/c",
        "npx",
        "-y",
        "@modelcontextprotocol/server-postgres",
        "postgresql://%PG_USER%:%PG_PASSWORD%@%PG_HOST%:%PG_PORT%/%PG_DATABASE%"
      ]
    }
  }
}
```

说明：

- 在 Windows 下使用 `cmd /c npx ...`，兼容性比直接 `npx` 更稳。
- MCP 名称为 `postgres`，在 Claude Code 中通过 `/mcp` 可见。
- 连接信息通过环境变量注入，避免在仓库里硬编码数据库账号密码。

建议在启动 Claude Code 前设置：

```powershell
$env:PG_USER="openiot"
$env:PG_PASSWORD="openiot123"
$env:PG_HOST="localhost"
$env:PG_PORT="5432"
$env:PG_DATABASE="openiot"
```

## 项目级配置步骤（推荐）

1. 在项目根目录创建或编辑 `.mcp.json`。
2. 写入 `mcpServers.postgres` 配置（见上文）。
3. 在项目目录启动 Claude Code：`cd F:\codes\open-iot` 后执行 `claude`。
4. 首次提示 `New MCP server found in .mcp.json: postgres` 时选择：
   - `1. Use this and all future MCP servers in this project`（推荐）
5. 验证：
   - 运行 `claude mcp list`
   - 看到 `postgres ... ✓ Connected` 即成功。

## 为什么你之前看不到 postgres

排查结果有两个关键点：

1. 全局配置文件 `C:\Users\admin\.claude\settings.json` 有 JSON 语法错误，导致设置加载异常。
2. 项目 MCP 需要在 Claude Code 启动时授权；若曾拒绝/跳过，需要重新授权。

已处理动作：

1. 修复了 `C:\Users\admin\.claude\settings.json` 语法。
2. 执行了 `claude mcp reset-project-choices`，让项目 MCP 重新弹出授权。

## 如果你想改成全局配置

适用场景：你所有项目都连接同一个本地 PostgreSQL，且希望不依赖项目仓库文件。

步骤：

1. 添加全局（user scope）MCP：

```powershell
claude mcp add -s user postgres -- cmd /c npx -y @modelcontextprotocol/server-postgres postgresql://%PG_USER%:%PG_PASSWORD%@%PG_HOST%:%PG_PORT%/%PG_DATABASE%
```

2. 验证：

```powershell
claude mcp list
```

3. 若项目级和全局同名服务器并存，建议统一只保留一种，避免混淆。

## 安全建议

1. 开发环境可以使用当前账号；生产环境请使用最小权限账号。
2. 建议改为环境变量注入连接信息，避免在仓库中明文保存密码。
3. 如果仓库会对外共享，建议将敏感连接串移出版本库。
