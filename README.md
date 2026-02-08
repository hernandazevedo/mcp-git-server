# MCP Git Server (Kotlin + Ktor)

An MCP (Model Context Protocol) server for Git operations, built with Kotlin and Ktor.

## Features

This MCP server provides tools to perform Git operations through the MCP protocol via HTTP REST API or STDIO:

### Available Tools

1. **git_status** - Shows the repository state
2. **git_diff** - Shows changes made (with option for specific file and staged)
3. **git_commit** - Creates commits with messages
4. **git_log** - Views commit history
5. **git_branch** - Lists/creates/deletes branches
6. **git_checkout** - Restores files or switches branches
7. **git_add** - Adds files to staging area
8. **git_push** - Pushes commits to remote repository

## Requirements

- Java 17 or higher
- Git installed on the system

## Installation

1. Clone the repository
2. Make the script executable:
```bash
chmod +x mcp-git-server.sh
```

## Usage

### HTTP Mode (Default)

Starts the HTTP server on port 8080:

```bash
./gradlew run
```

Or using the script:

```bash
./mcp-git-server.sh http
```

The server will be available at `http://localhost:8080`

**Endpoints:**
- `GET /` - Server information
- `GET /health` - Health check
- `POST /mcp` - JSON-RPC endpoint for MCP commands

### STDIO Mode (Claude Desktop)

To use with Claude Desktop via stdin/stdout:

```bash
./mcp-git-server.sh stdio
```

### Configure with Claude Desktop

Add to your Claude Desktop configuration file (`claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "git": {
      "command": "/full/path/to/mcp-git-server.sh",
      "args": ["stdio"],
      "env": {
        "GIT_WORKING_DIR": "/path/to/repository"
      }
    }
  }
}
```

### Environment Variables

- `GIT_WORKING_DIR`: Git repository directory (default: current directory)
- `MCP_PORT`: HTTP server port (default: 8080)
- `MCP_HOST`: HTTP server host (default: 0.0.0.0)

## Usage Examples

### Via HTTP (curl)

```bash
# Health check
curl http://localhost:8080/health

# View repository status
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/call",
    "params": {
      "name": "git_status",
      "arguments": {}
    }
  }'
```

### Via STDIO

```bash
echo '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"git_status","arguments":{}}}' | ./mcp-git-server.sh stdio
```

### JSON-RPC Requests

#### View repository status
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "git_status",
    "arguments": {}
  }
}
```

### View file diff
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/call",
  "params": {
    "name": "git_diff",
    "arguments": {
      "file_path": "src/Main.kt",
      "staged": false
    }
  }
}
```

### Make commit
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "git_commit",
    "arguments": {
      "message": "feat: add new feature",
      "add_all": true
    }
  }
}
```

### View commit history
```json
{
  "jsonrpc": "2.0",
  "id": 4,
  "method": "tools/call",
  "params": {
    "name": "git_log",
    "arguments": {
      "max_count": 5,
      "oneline": true
    }
  }
}
```

### List branches
```json
{
  "jsonrpc": "2.0",
  "id": 5,
  "method": "tools/call",
  "params": {
    "name": "git_branch",
    "arguments": {
      "action": "list"
    }
  }
}
```

### Create new branch
```json
{
  "jsonrpc": "2.0",
  "id": 6,
  "method": "tools/call",
  "params": {
    "name": "git_branch",
    "arguments": {
      "action": "create",
      "branch_name": "feature/new-feature"
    }
  }
}
```

### Checkout
```json
{
  "jsonrpc": "2.0",
  "id": 7,
  "method": "tools/call",
  "params": {
    "name": "git_checkout",
    "arguments": {
      "target": "main",
      "create_new": false
    }
  }
}
```

### Add files
```json
{
  "jsonrpc": "2.0",
  "id": 8,
  "method": "tools/call",
  "params": {
    "name": "git_add",
    "arguments": {
      "files": "."
    }
  }
}
```

### Push to remote
```json
{
  "jsonrpc": "2.0",
  "id": 9,
  "method": "tools/call",
  "params": {
    "name": "git_push",
    "arguments": {
      "remote": "origin",
      "branch": "main",
      "set_upstream": true,
      "force": false
    }
  }
}
```

## Development

### Build the project

```bash
./gradlew build
```

### Run unit tests

```bash
./gradlew test
```

### Test HTTP server

```bash
# Terminal 1: Start server
./gradlew run

# Terminal 2: Run HTTP tests
./test-http.sh
```

### Complete rebuild

```bash
./mcp-git-server.sh --rebuild
```

## Project Structure

```
mcp-git-server/
├── build.gradle.kts          # Gradle configuration
├── settings.gradle.kts        # Project settings
├── mcp-git-server.sh          # Execution script
├── test-http.sh               # HTTP tests
├── test-manual.sh             # STDIO tests
├── README.md                  # Documentation
├── TESTING.md                 # Testing guide
└── src/
    ├── main/
    │   └── kotlin/
    │       └── com/
    │           └── mcp/
    │               └── git/
    │                   ├── Main.kt           # Entry point
    │                   ├── KtorServer.kt     # Ktor HTTP server
    │                   ├── McpServer.kt      # MCP server (logic)
    │                   ├── McpProtocol.kt    # Protocol structures
    │                   └── GitOperations.kt  # Git operations
    └── test/
        └── kotlin/
            └── com/
                └── mcp/
                    └── git/
                        ├── GitOperationsTest.kt  # Git operations tests
                        ├── McpServerTest.kt      # MCP server tests
                        └── KtorServerTest.kt     # HTTP tests
```

## Protocol

This server implements the [Model Context Protocol](https://modelcontextprotocol.io/) version 2024-11-05.

## Documentation

- [TESTING.md](TESTING.md) - Comprehensive testing guide with examples
- [HTTP-MIGRATION.md](HTTP-MIGRATION.md) - HTTP migration and dual-mode support documentation
- [http-usage-example.md](http-usage-example.md) - HTTP usage examples and API reference

## License

MIT
