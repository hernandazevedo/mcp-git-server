#!/usr/bin/env kotlin

@file:DependsOn("io.ktor:ktor-client-core:2.3.7")
@file:DependsOn("io.ktor:ktor-client-cio:2.3.7")
@file:DependsOn("io.ktor:ktor-client-content-negotiation:2.3.7")
@file:DependsOn("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import kotlinx.coroutines.runBlocking

/**
 * Cliente HTTP para testar o MCP Git Server
 *
 * Uso:
 * 1. Inicie o servidor: ./gradlew run
 * 2. Execute este script: kotlin test-client-http.kt
 */

data class TestRequest(
    val id: Int,
    val name: String,
    val request: String,
    val description: String
)

fun main(args: Array<String>) = runBlocking {
    val serverUrl = System.getenv("MCP_SERVER_URL") ?: "http://localhost:8080"
    val mcpEndpoint = "$serverUrl/mcp"

    println("=== MCP Git Server - Cliente HTTP ===\n")
    println("Server URL: $serverUrl\n")

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    try {
        // Verificar health
        println("Verificando saúde do servidor...")
        val healthResponse = client.get("$serverUrl/health")
        if (healthResponse.status == HttpStatusCode.OK) {
            println("✓ Servidor está saudável\n")
        } else {
            println("✗ Servidor retornou status: ${healthResponse.status}\n")
            return@runBlocking
        }

        // Lista de testes
        val requests = listOf(
            TestRequest(
                1,
                "initialize",
                """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}""",
                "Inicializar servidor MCP"
            ),
            TestRequest(
                2,
                "tools/list",
                """{"jsonrpc":"2.0","id":2,"method":"tools/list"}""",
                "Listar ferramentas disponíveis"
            ),
            TestRequest(
                3,
                "git_status",
                """{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"git_status","arguments":{}}}""",
                "Verificar status do repositório"
            ),
            TestRequest(
                4,
                "git_log",
                """{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"git_log","arguments":{"max_count":5,"oneline":true}}}""",
                "Ver últimos 5 commits"
            ),
            TestRequest(
                5,
                "git_branch",
                """{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"git_branch","arguments":{"action":"list"}}}""",
                "Listar branches"
            ),
            TestRequest(
                6,
                "git_diff",
                """{"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":"git_diff","arguments":{}}}""",
                "Ver diferenças não commitadas"
            )
        )

        for (testReq in requests) {
            println("\n--- Test ${testReq.id}: ${testReq.description} ---")
            println("Requisição: ${testReq.name}")

            val response = client.post(mcpEndpoint) {
                contentType(ContentType.Application.Json)
                setBody(testReq.request)
            }

            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val jsonElement = Json.parseToJsonElement(body)
                println("✓ Status: ${response.status}")
                println("Resposta:")
                println(Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), jsonElement))
            } else {
                println("✗ Status: ${response.status}")
                println("Erro: ${response.bodyAsText()}")
            }
        }

        println("\n✓ Todos os testes foram executados")

    } catch (e: Exception) {
        println("✗ Erro ao executar testes: ${e.message}")
        e.printStackTrace()
    } finally {
        client.close()
    }
}
