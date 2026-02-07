#!/usr/bin/env kotlin

/**
 * Cliente de teste manual para o MCP Git Server
 *
 * Uso:
 * 1. Compile e rode o servidor: ./gradlew build && ./mcp-git-server.sh
 * 2. Em outro terminal, execute este script: kotlin test-client.kt
 *
 * Ou use o modo interativo para testar comandos específicos
 */

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter

data class TestRequest(
    val id: Int,
    val name: String,
    val request: String,
    val description: String
)

fun main(args: Array<String>) {
    println("=== MCP Git Server - Cliente de Teste ===\n")

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
        ),
        TestRequest(
            7,
            "git_add",
            """{"jsonrpc":"2.0","id":7,"method":"tools/call","params":{"name":"git_add","arguments":{"files":"."}}}""",
            "Adicionar todos os arquivos (exemplo)"
        ),
        TestRequest(
            8,
            "git_commit",
            """{"jsonrpc":"2.0","id":8,"method":"tools/call","params":{"name":"git_commit","arguments":{"message":"test commit","add_all":false}}}""",
            "Fazer commit (exemplo - não executará se não houver mudanças staged)"
        )
    )

    if (args.contains("--interactive") || args.contains("-i")) {
        interactiveMode()
    } else {
        runTests(requests)
    }
}

fun runTests(requests: List<TestRequest>) {
    println("Modo: Testes Automáticos\n")

    try {
        val process = ProcessBuilder("./mcp-git-server.sh")
            .redirectErrorStream(false)
            .start()

        val writer = PrintWriter(process.outputStream, true)
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val errorReader = BufferedReader(InputStreamReader(process.errorStream))

        // Thread para ler erros
        Thread {
            errorReader.lineSequence().forEach { line ->
                System.err.println("[SERVER] $line")
            }
        }.start()

        for (request in requests) {
            println("\n--- Test ${request.id}: ${request.description} ---")
            println("Requisição: ${request.name}")
            println("Enviando: ${request.request}")

            writer.println(request.request)
            writer.flush()

            // Aguardar resposta
            val response = reader.readLine()
            if (response != null) {
                println("Resposta: $response")
            } else {
                println("⚠ Sem resposta do servidor")
            }

            Thread.sleep(500) // Pequeno delay entre requisições
        }

        println("\n✓ Todos os testes foram executados")

        writer.close()
        process.destroy()

    } catch (e: Exception) {
        println("✗ Erro ao executar testes: ${e.message}")
        e.printStackTrace()
    }
}

fun interactiveMode() {
    println("Modo: Interativo")
    println("Digite comandos JSON-RPC ou 'exit' para sair\n")

    try {
        val process = ProcessBuilder("./mcp-git-server.sh")
            .redirectErrorStream(false)
            .start()

        val writer = PrintWriter(process.outputStream, true)
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val errorReader = BufferedReader(InputStreamReader(process.errorStream))
        val consoleReader = BufferedReader(InputStreamReader(System.`in`))

        // Thread para ler respostas do servidor
        Thread {
            reader.lineSequence().forEach { line ->
                println("← $line")
            }
        }.start()

        // Thread para ler erros
        Thread {
            errorReader.lineSequence().forEach { line ->
                System.err.println("[SERVER] $line")
            }
        }.start()

        while (true) {
            print("→ ")
            val input = consoleReader.readLine() ?: break

            if (input.trim().equals("exit", ignoreCase = true)) {
                break
            }

            if (input.trim().isEmpty()) {
                continue
            }

            writer.println(input)
            writer.flush()

            Thread.sleep(100) // Dar tempo para resposta
        }

        writer.close()
        process.destroy()
        println("\nEncerrando...")

    } catch (e: Exception) {
        println("✗ Erro: ${e.message}")
        e.printStackTrace()
    }
}
