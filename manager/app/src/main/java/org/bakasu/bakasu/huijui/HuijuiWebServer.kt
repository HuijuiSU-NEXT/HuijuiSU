package org.bakasu.bakasu.huijui

import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import kotlin.random.Random

object HuijuiWebServer {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    var currentToken: String = ""
        private set
    var port: Int = 8080
        private set

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                if (!intf.isLoopback && intf.isUp) {
                    for (addr in intf.inetAddresses) {
                        if (!addr.isLoopbackAddress && addr.hostAddress?.contains(":") == false) {
                            return addr.hostAddress ?: "127.0.0.1"
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }

    fun start(onStarted: (String) -> Unit) {
        if (isRunning) return
        isRunning = true
        currentToken = String.format("%06d", Random.nextInt(999999))
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(port)
                onStarted("http://${getLocalIpAddress()}:$port")
                while (isRunning) {
                    val socket = serverSocket?.accept() ?: break
                    launch { handleClient(socket) }
                }
            } catch (e: Exception) {
                isRunning = false
            }
        }
    }

    fun stop() {
        isRunning = false
        try { serverSocket?.close() } catch (_: Exception) {}
    }

    private fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)
            
            var line: String?
            var queryString = ""
            while (reader.readLine().also { line = it } != null) {
                if (line?.startsWith("GET /") == true) {
                    queryString = line.substring(5).split(" ")[0]
                }
                if (line.isNullOrEmpty()) break
            }

            // 鉴权处理
            if (!queryString.startsWith("?token=$currentToken") && queryString != "/favicon.ico") {
                sendResponse(writer, 401, "Unauthorized", "text/plain", "401 Unauthorized: Please provide valid token.")
                return
            }

            // 路由处理
            when {
                queryString.contains("action=get_stats") -> {
                    val out = Shell.cmd("dumpsys battery | grep temperature; cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq").exec().out.joinToString("|")
                    sendResponse(writer, 200, "OK", "application/json", """{"data":"$out"}""")
                }
                queryString.contains("action=run_cmd&cmd=") -> {
                    val cmd = queryString.substringAfter("cmd=").replace("%20", " ")
                    val result = Shell.cmd(cmd).exec().out.joinToString("\n")
                    sendResponse(writer, 200, "OK", "text/plain", result.ifEmpty { "Command executed." })
                }
                else -> {
                    sendResponse(writer, 200, "OK", "text/html", getHtml(currentToken))
                }
            }
        } catch (_: Exception) {
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun sendResponse(writer: PrintWriter, code: Int, msg: String, contentType: String, body: String) {
        writer.println("HTTP/1.1 $code $msg")
        writer.println("Content-Type: $contentType; charset=utf-8")
        writer.println("Access-Control-Allow-Origin: *")
        writer.println("Connection: close")
        writer.println()
        writer.println(body)
        writer.flush()
    }

    private fun getHtml(token: String) = """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>HuiJuiSU NEXT Ultra - Web Admin</title>
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; background: #1a1a1a; color: #fff; margin: 0; padding: 20px; display: flex; justify-content: center; }
                .card { background: rgba(40, 40, 40, 0.7); backdrop-filter: blur(16px); border: 1px solid rgba(255,255,255,0.1); border-radius: 24px; padding: 24px; width: 100%; max-width: 600px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
                h1 { font-size: 24px; margin-top: 0; color: #a8ffdb; }
                .status { font-size: 14px; margin: 16px 0; padding: 16px; background: rgba(0,0,0,0.3); border-radius: 12px; font-family: monospace; white-space: pre-wrap; }
                input, button { width: 100%; padding: 12px; margin: 8px 0; border-radius: 8px; border: none; box-sizing: border-box; }
                input { background: rgba(255,255,255,0.1); color: #fff; outline: none; }
                button { background: #a8ffdb; color: #000; font-weight: bold; cursor: pointer; transition: 0.2s; }
                button:hover { background: #fff; }
            </style>
        </head>
        <body>
            <div class="card">
                <h1>HuiJuiSU NEXT Ultra WebUI</h1>
                <p style="font-size: 12px; color: #888;">Security Token: $token</p>
                <div class="status" id="stats">正在获取状态...</div>
                <input type="text" id="cmdInput" placeholder="输入 Shell 命令 (如: id)">
                <button onclick="runCmd()">执行命令</button>
                <div class="status" id="cmdOutput" style="min-height: 50px;">命令输出区...</div>
            </div>
            <script>
                const token = "$token";
                setInterval(() => {
                    fetch("/?token=" + token + "&action=get_stats").then(r => r.json()).then(d => {
                        document.getElementById('stats').innerText = d.data.replace(/\\|/g, "\\n");
                    });
                }, 2000);
                function runCmd() {
                    let cmd = document.getElementById('cmdInput').value;
                    if (!cmd) return;
                    document.getElementById('cmdOutput').innerText = "执行中...";
                    fetch("/?token=" + token + "&action=run_cmd&cmd=" + encodeURIComponent(cmd)).then(r => r.text()).then(d => {
                        document.getElementById('cmdOutput').innerText = d || "执行成功 (无输出)";
                    });
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
