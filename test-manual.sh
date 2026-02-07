#!/bin/bash

# Script para testar manualmente o servidor MCP Git
# Uso: ./test-manual.sh

set -e

echo "=== MCP Git Server - Teste Manual ==="
echo ""

# Cores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Função para enviar requisição
send_request() {
    local request="$1"
    local description="$2"

    echo -e "${BLUE}>>> $description${NC}"
    echo -e "${YELLOW}Enviando:${NC} $request"
    echo "$request" | ./mcp-git-server.sh 2>/dev/null
    echo ""
}

# Build do projeto
echo "Building projeto..."
./gradlew build -q
echo ""

# Iniciar os testes
echo -e "${GREEN}Iniciando testes...${NC}"
echo ""

# Teste 1: Initialize
send_request \
    '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}' \
    "Test 1: Initialize"

# Teste 2: List Tools
send_request \
    '{"jsonrpc":"2.0","id":2,"method":"tools/list"}' \
    "Test 2: List Tools"

# Teste 3: Git Status
send_request \
    '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"git_status","arguments":{}}}' \
    "Test 3: Git Status"

# Teste 4: Git Log
send_request \
    '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"git_log","arguments":{"max_count":5,"oneline":true}}}' \
    "Test 4: Git Log (últimos 5 commits)"

# Teste 5: Git Branch List
send_request \
    '{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"git_branch","arguments":{"action":"list"}}}' \
    "Test 5: Git Branch List"

# Teste 6: Git Diff
send_request \
    '{"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":"git_diff","arguments":{}}}' \
    "Test 6: Git Diff"

echo -e "${GREEN}Testes concluídos!${NC}"
