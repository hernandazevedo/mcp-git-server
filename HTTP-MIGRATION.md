# Migração para HTTP - MCP Git Server

Este documento descreve as mudanças implementadas para expor o MCP Git Server via HTTP usando Ktor.

## O que mudou?

### 1. Novo Modo HTTP (Padrão)

O servidor agora inicia em modo HTTP por padrão, expondo uma REST API na porta 8080:

```bash
./gradlew run                    # HTTP mode (default)
./mcp-git-server.sh http         # HTTP mode explícito
```

**Endpoints disponíveis:**
- `GET /` - Informações do servidor
- `GET /health` - Health check
- `POST /mcp` - Endpoint JSON-RPC principal

### 2. Modo STDIO Mantido

O modo STDIO original (stdin/stdout) foi mantido para compatibilidade com Claude Desktop:

```bash
./mcp-git-server.sh stdio        # STDIO mode
```

### 3. Nova Arquitetura

```
Main.kt
  ├─→ HTTP mode → KtorServer.kt → McpServer.kt → GitOperations.kt
  └─→ STDIO mode → McpServer.kt → GitOperations.kt
```

**Arquivos novos:**
- `src/main/kotlin/com/mcp/git/KtorServer.kt` - Configuração do servidor Ktor

**Arquivos modificados:**
- `src/main/kotlin/com/mcp/git/Main.kt` - Suporte para ambos os modos
- `build.gradle.kts` - Dependências do Ktor

**Arquivos não modificados:**
- `McpServer.kt` - Lógica MCP permanece igual
- `McpProtocol.kt` - Protocolo JSON-RPC permanece igual
- `GitOperations.kt` - Operações Git permanecem iguais

### 4. Novos Testes

**Testes unitários:**
- `KtorServerTest.kt` - 6 testes para endpoints HTTP

**Scripts de teste:**
- `test-http.sh` - Testes HTTP automatizados com curl
- `test-client-http.kt` - Cliente HTTP em Kotlin
- `exemplo-uso-http.md` - Guia de uso HTTP completo

**Testes mantidos:**
- `GitOperationsTest.kt` - 18 testes (sem alterações)
- `McpServerTest.kt` - 12 testes (sem alterações)
- `test-manual.sh` - Testes STDIO (atualizado)
- `test-client.kt` - Cliente STDIO (mantido)

## Como usar?

### Modo HTTP (Recomendado para desenvolvimento)

#### Iniciar servidor

```bash
./gradlew run
```

#### Testar

```bash
# Terminal 2
./test-http.sh

# Ou com curl
curl http://localhost:8080/health
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}'
```

### Modo STDIO (Para Claude Desktop)

#### Configuração Claude Desktop

```json
{
  "mcpServers": {
    "git": {
      "command": "/path/to/mcp-git-server.sh",
      "args": ["stdio"],
      "env": {
        "GIT_WORKING_DIR": "/path/to/repo"
      }
    }
  }
}
```

## Variáveis de Ambiente

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `MCP_PORT` | 8080 | Porta do servidor HTTP |
| `MCP_HOST` | 0.0.0.0 | Host do servidor HTTP |
| `GIT_WORKING_DIR` | . | Diretório do repositório Git |

## Benefícios da Migração

### 1. Melhor Testabilidade
- Testes HTTP mais fáceis com curl ou ferramentas REST
- Não precisa simular stdin/stdout
- Health checks para monitoramento

### 2. Integração Mais Simples
- Qualquer linguagem pode consumir via HTTP
- CORS configurado para uso em navegadores
- REST API padrão

### 3. Desenvolvimento Mais Fácil
- Logs estruturados via Logback
- Facilmente inspecionável com ferramentas HTTP
- Hot reload possível (com ferramentas como Ktor's auto-reload)

### 4. Produção Ready
- Health checks para load balancers
- Métricas facilmente adicionáveis
- Pode ser containerizado (Docker)
- Escalável horizontalmente

## Compatibilidade

✅ **100% compatível com código existente**
- Protocolo JSON-RPC idêntico
- Todas as ferramentas funcionam igual
- Modo STDIO mantido para Claude Desktop
- Testes unitários existentes sem alterações

## Próximos Passos (Opcional)

### 1. Dockerfile

```dockerfile
FROM openjdk:17-slim
WORKDIR /app
COPY build/libs/mcp-git-server-*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar", "http"]
```

### 2. Docker Compose

```yaml
version: '3'
services:
  mcp-git-server:
    build: .
    ports:
      - "8080:8080"
    environment:
      - GIT_WORKING_DIR=/repo
    volumes:
      - ./repo:/repo
```

### 3. Métricas (Prometheus)

Adicionar dependência:
```kotlin
implementation("io.ktor:ktor-server-metrics-micrometer:$ktor_version")
implementation("io.micrometer:micrometer-registry-prometheus:1.12.0")
```

### 4. Autenticação

```kotlin
install(Authentication) {
    bearer("auth-bearer") {
        authenticate { credential ->
            // Validar token
        }
    }
}
```

## Troubleshooting

### Porta 8080 em uso

```bash
MCP_PORT=9000 ./gradlew run
```

### Servidor não responde

Verifique logs e health:
```bash
curl http://localhost:8080/health
```

### Claude Desktop não funciona

Certifique-se de usar modo STDIO:
```json
{
  "mcpServers": {
    "git": {
      "command": "/path/to/mcp-git-server.sh",
      "args": ["stdio"]
    }
  }
}
```

## Documentação Adicional

- `README.md` - Guia principal (atualizado)
- `TESTING.md` - Guia de testes (atualizado)
- `exemplo-uso-http.md` - Exemplos HTTP detalhados
