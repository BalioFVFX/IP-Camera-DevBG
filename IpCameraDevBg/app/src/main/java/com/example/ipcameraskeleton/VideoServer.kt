package com.example.ipcameraskeleton

import java.lang.Exception
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList

class VideoServer {

    companion object {
        private val HEADERS = "HTTP/1.0 200 OK\r\nContent-Type: multipart/x-mixed-replace; boundary=frame\r\n\r\n"
            .toByteArray()

        private val BOUNDARY = "--frame\r\n".toByteArray()

        private val CONTENT_TYPE = "Content-Type: image/jpeg\r\n".toByteArray()

        private fun contentLength(size: Int) : ByteArray {
            return "Content-Length: ${size}\r\n\r\n".toByteArray()
        }

        private val END = "\r\n".toByteArray()
    }

    private val queue = ConcurrentLinkedQueue<ByteArray>()
    private val clients = CopyOnWriteArrayList<Socket>()

    @Volatile private var serverSocket: ServerSocket? = null


    fun start () {
        acceptClients()
        handleClients()
    }

    fun stop() {

    }

    fun send(frame: ByteArray) {
        queue.add(frame)
    }

    private fun acceptClients() {
        serverSocket = ServerSocket(5454)
        Thread() {
            while (true) {
                val client = serverSocket!!.accept()

                client.getOutputStream().write(HEADERS)
                client.getOutputStream().flush()

                clients.add(client)
            }
        }.start()
    }

    private fun handleClients() {
        Thread() {
            while (true) {
                val frame = try {
                    queue.remove()
                } catch (ex: NoSuchElementException) {
                    continue
                }

                for (client in clients) {
                    client.getOutputStream().write(BOUNDARY)
                    client.getOutputStream().write(CONTENT_TYPE)
                    client.getOutputStream().write(contentLength(frame.size))
                    client.getOutputStream().write(frame)
                    client.getOutputStream().write(END)
                    client.getOutputStream().flush()
                }
            }
        }.start()
    }

}