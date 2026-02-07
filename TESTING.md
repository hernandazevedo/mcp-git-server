# Testing Guide - MCP Git Server

This document describes how to run unit tests and manual tests for the MCP Git Server (HTTP and STDIO).

## Unit Tests

### Run all tests

```bash
./gradlew test
```

### Run tests with detailed report

```bash
./gradlew test --info
```

### View HTML test report

```bash
./gradlew test
open build/reports/tests/test/index.html
```

### Test Structure

- **GitOperationsTest.kt**: Tests Git operations (status, diff, commit, log, branch, checkout, add)
- **McpServerTest.kt**: Tests MCP server and JSON-RPC protocol

## Manual Tests

### HTTP Mode (Recommended)

#### Option 1: HTTP Bash Script (test-http.sh)

The `test-http.sh` script tests the HTTP server:

```bash
# Terminal 1: Start server
./gradlew run

# Terminal 2: Run tests
./test-http.sh
```

This script will:
1. Check if the server is running
2. Send HTTP POST requests to /mcp
3. Display formatted responses (if jq is installed)

#### Option 2: Kotlin HTTP Client (test-client-http.kt)

HTTP client written in Kotlin:

```bash
# Terminal 1: Start server
./gradlew run

# Terminal 2: Run client
kotlin test-client-http.kt
```

#### Option 3: curl manually

```bash
# Health check
curl http://localhost:8080/health

# Initialize
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}'

# Git status
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"git_status","arguments":{}}}'
```

### STDIO Mode

#### Option 1: STDIO Bash Script (test-manual.sh)

The `test-manual.sh` script executes a series of test commands via STDIO:

```bash
./test-manual.sh
```

This script will:
1. Build the project
2. Run the server in STDIO mode
3. Send multiple JSON-RPC requests via stdin
4. Display responses

### Option 2: Kotlin Client (test-client.kt)

#### Automatic Mode

Executes a series of predefined tests:

```bash
kotlin test-client.kt
```

#### Interactive Mode

Allows sending JSON-RPC commands manually:

```bash
kotlin test-client.kt --interactive
```

In interactive mode, you can type JSON-RPC commands directly, for example:

```json
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}
```

Type `exit` to quit.

#### Option 3: STDIO Test with echo and pipe

You can test individual commands using echo and pipe:

```bash
# Initialize
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}' | ./mcp-git-server.sh stdio

# List tools
echo '{"jsonrpc":"2.0","id":2,"method":"tools/list"}' | ./mcp-git-server.sh stdio

# Git status
echo '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"git_status","arguments":{}}}' | ./mcp-git-server.sh stdio

# Git log
echo '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"git_log","arguments":{"max_count":5,"oneline":true}}}' | ./mcp-git-server.sh stdio

# Git branch list
echo '{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"git_branch","arguments":{"action":"list"}}}' | ./mcp-git-server.sh stdio
```

## JSON-RPC Request Examples

### Initialize

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {}
}
```

### List Tools

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/list"
}
```

### Git Status

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "git_status",
    "arguments": {}
  }
}
```

### Git Diff

```json
{
  "jsonrpc": "2.0",
  "id": 4,
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

### Git Commit

```json
{
  "jsonrpc": "2.0",
  "id": 5,
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

### Git Log

```json
{
  "jsonrpc": "2.0",
  "id": 6,
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

### Git Branch List

```json
{
  "jsonrpc": "2.0",
  "id": 7,
  "method": "tools/call",
  "params": {
    "name": "git_branch",
    "arguments": {
      "action": "list"
    }
  }
}
```

### Git Branch Create

```json
{
  "jsonrpc": "2.0",
  "id": 8,
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

### Git Checkout

```json
{
  "jsonrpc": "2.0",
  "id": 9,
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

### Git Add

```json
{
  "jsonrpc": "2.0",
  "id": 10,
  "method": "tools/call",
  "params": {
    "name": "git_add",
    "arguments": {
      "files": "."
    }
  }
}
```

## Coverage Verification

To generate a code coverage report (requires jacoco plugin):

```bash
./gradlew test jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

## Debugging

### HTTP Mode

To view detailed HTTP server logs:

```bash
# Logs are automatically displayed in the console
./gradlew run

# Or with more detailed logs
MCP_LOG_LEVEL=DEBUG ./gradlew run
```

### STDIO Mode

To view detailed STDIO server logs, redirect stderr:

```bash
./mcp-git-server.sh stdio 2>&1 | tee server.log
```

Or use the interactive client to view logs in real-time:

```bash
kotlin test-client.kt --interactive
```

## Continuous Integration

Tests can be run in CI/CD by adding to your workflow:

```yaml
- name: Run tests
  run: ./gradlew test

- name: Publish test results
  uses: actions/upload-artifact@v3
  if: always()
  with:
    name: test-results
    path: build/reports/tests/test/
```
