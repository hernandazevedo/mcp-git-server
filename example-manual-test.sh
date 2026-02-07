#!/bin/bash

# Simple manual test example for MCP Git Server
# This script demonstrates how to send JSON-RPC commands to the server

echo "=== MCP Git Server Manual Test Example ==="
echo ""
echo "1. Start the server in background:"
echo "   ./mcp-git-server.sh &"
echo ""
echo "2. Or test commands directly with pipe:"
echo ""

# Command 1: Initialize
echo "▶ Command: Initialize"
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}' | ./mcp-git-server.sh 2>/dev/null
echo ""

# Command 2: List Tools
echo "▶ Command: List Tools"
echo '{"jsonrpc":"2.0","id":2,"method":"tools/list"}' | ./mcp-git-server.sh 2>/dev/null
echo ""

# Command 3: Git Status
echo "▶ Command: Git Status"
echo '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"git_status","arguments":{}}}' | ./mcp-git-server.sh 2>/dev/null
echo ""

echo "=== Test completed! ==="
echo ""
echo "For more examples, see:"
echo "  - TESTING.md for complete testing guide"
echo "  - test-manual.sh for automated tests"
echo "  - test-client.kt for interactive client"
