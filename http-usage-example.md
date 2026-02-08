# Usage Example - MCP Git Server HTTP

This guide shows how to use the MCP Git Server in HTTP mode.

## 1. Start the Server

```bash
./gradlew run
```

Or:

```bash
./mcp-git-server.sh http
```

The server will start at `http://localhost:8080`

## 2. Test with curl

### Health Check

```bash
curl http://localhost:8080/health
```

Response:
```json
{"status":"healthy"}
```

### Initialize

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {}
  }'
```

### List Tools

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/list"
  }'
```

### Git Status

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
      "name": "git_status",
      "arguments": {}
    }
  }'
```

### Git Log

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
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
  }'
```

### Git Branch List

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 5,
    "method": "tools/call",
    "params": {
      "name": "git_branch",
      "arguments": {
        "action": "list"
      }
    }
  }'
```

### Git Diff

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 6,
    "method": "tools/call",
    "params": {
      "name": "git_diff",
      "arguments": {}
    }
  }'
```

### Git Add

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 7,
    "method": "tools/call",
    "params": {
      "name": "git_add",
      "arguments": {
        "files": "."
      }
    }
  }'
```

### Git Commit

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 8,
    "method": "tools/call",
    "params": {
      "name": "git_commit",
      "arguments": {
        "message": "feat: add new feature",
        "add_all": false
      }
    }
  }'
```

### Git Push

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
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
  }'
```

## 3. Use Test Scripts

### Automated Bash Script

```bash
# Terminal 1: Server
./gradlew run

# Terminal 2: Tests
./test-http.sh
```

### Kotlin Client

```bash
# Terminal 1: Server
./gradlew run

# Terminal 2: Client
kotlin test-client-http.kt
```

## 4. Configuration

### Custom Port

```bash
MCP_PORT=9000 ./gradlew run
```

### Custom Host

```bash
MCP_HOST=127.0.0.1 ./gradlew run
```

### Custom Git Directory

```bash
GIT_WORKING_DIR=/path/to/repo ./gradlew run
```

## 5. Application Integration

### JavaScript/TypeScript (fetch)

```javascript
async function callMcpServer(method, params = {}) {
  const response = await fetch('http://localhost:8080/mcp', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      jsonrpc: '2.0',
      id: Date.now(),
      method: method,
      params: params
    })
  });
  return await response.json();
}

// Usage example
const status = await callMcpServer('tools/call', {
  name: 'git_status',
  arguments: {}
});
console.log(status);
```

### Python (requests)

```python
import requests
import json

def call_mcp_server(method, params=None):
    url = "http://localhost:8080/mcp"
    payload = {
        "jsonrpc": "2.0",
        "id": 1,
        "method": method,
        "params": params or {}
    }
    response = requests.post(url, json=payload)
    return response.json()

# Usage example
status = call_mcp_server("tools/call", {
    "name": "git_status",
    "arguments": {}
})
print(json.dumps(status, indent=2))
```

## 6. Troubleshooting

### Server won't start

Check if port 8080 is not in use:

```bash
lsof -i :8080
```

Use another port:

```bash
MCP_PORT=9000 ./gradlew run
```

### Connection error

Check if the server is running:

```bash
curl http://localhost:8080/health
```

### View detailed logs

Logs are automatically displayed in the console where the server was started.
