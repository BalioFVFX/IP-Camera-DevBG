package com.example.ipcameraskeleton

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

}