#!/bin/bash

# Script para testar o servidor MCP Git via HTTP
# Uso: ./test-http.sh

set -e

# Cores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuração
SERVER_URL="${MCP_SERVER_URL:-http://localhost:8080}"
MCP_ENDPOINT="$SERVER_URL/mcp"

echo "=== MCP Git Server - Teste HTTP ==="
echo ""
echo "Server URL: $SERVER_URL"
echo ""

# Função para enviar requisição HTTP
send_http_request() {
    local request="$1"
    local description="$2"

    echo -e "${BLUE}>>> $description${NC}"
    echo -e "${YELLOW}Enviando para $MCP_ENDPOINT:${NC}"
    echo "$request" | jq '.' 2>/dev/null || echo "$request"

    response=$(curl -s -X POST "$MCP_ENDPOINT" \
        -H "Content-Type: application/json" \
        -d "$request")

    echo -e "${GREEN}Resposta:${NC}"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    echo ""
}

# Verificar se o servidor está rodando
echo "Verificando se o servidor está rodando..."
if ! curl -s "$SERVER_URL/health" > /dev/null 2>&1; then
    echo -e "${RED}✗ Servidor não está rodando em $SERVER_URL${NC}"
    echo ""
    echo "Inicie o servidor com:"
    echo "  ./gradlew run"
    echo ""
    echo "Ou em outro terminal:"
    echo "  ./mcp-git-server.sh http"
    exit 1
fi

echo -e "${GREEN}✓ Servidor está rodando${NC}"
echo ""

# Verificar se jq está instalado (para pretty print)
if ! command -v jq &> /dev/null; then
    echo -e "${YELLOW}⚠ jq não está instalado (JSON não será formatado)${NC}"
    echo ""
fi

# Iniciar os testes
echo -e "${GREEN}Iniciando testes HTTP...${NC}"
echo ""

# Teste 1: Health Check
echo -e "${BLUE}>>> Test 0: Health Check${NC}"
curl -s "$SERVER_URL/health" | jq '.' 2>/dev/null || curl -s "$SERVER_URL/health"
echo ""
echo ""

# Teste 2: Initialize
send_http_request \
    '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}' \
    "Test 1: Initialize"

# Teste 3: List Tools
send_http_request \
    '{"jsonrpc":"2.0","id":2,"method":"tools/list"}' \
    "Test 2: List Tools"

# Teste 4: Git Status
send_http_request \
    '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"git_status","arguments":{}}}' \
    "Test 3: Git Status"

# Teste 5: Git Log
send_http_request \
    '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"git_log","arguments":{"max_count":5,"oneline":true}}}' \
    "Test 4: Git Log (últimos 5 commits)"

# Teste 6: Git Branch List
send_http_request \
    '{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"git_branch","arguments":{"action":"list"}}}' \
    "Test 5: Git Branch List"

# Teste 7: Git Diff
send_http_request \
    '{"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":"git_diff","arguments":{}}}' \
    "Test 6: Git Diff"

echo -e "${GREEN}Testes HTTP concluídos!${NC}"
