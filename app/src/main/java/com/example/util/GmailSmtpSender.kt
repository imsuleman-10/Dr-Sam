package com.example.util

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import javax.net.ssl.SSLSocketFactory

object GmailSmtpSender {
    private const val TAG = "GmailSmtpSender"

    suspend fun sendVerificationEmail(
        recipientEmail: String,
        verificationCode: String
    ): Boolean = withContext(Dispatchers.IO) {
        val smtpServer = "smtp.gmail.com"
        val smtpPort = 465
        val username = "sulemanzaheer09@gmail.com"
        val password = "twuv pwfh ejjl zjsq" // Gmail app password provided by user

        try {
            Log.d(TAG, "Starting SSL connection to smtp.gmail.com:465")
            val socketFactory = SSLSocketFactory.getDefault()
            val socket = socketFactory.createSocket(smtpServer, smtpPort)
            socket.soTimeout = 10000 // 10s timeout to prevent infinite blocking

            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), "UTF-8"))
            val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream(), "UTF-8"))

            fun readResponse(expectCode: String): Boolean {
                val line = reader.readLine() ?: ""
                Log.d(TAG, "SMTP response: $line")
                return line.startsWith(expectCode)
            }

            fun sendCommand(cmd: String, expectCode: String): Boolean {
                Log.d(TAG, "SMTP send: $cmd")
                writer.print(cmd + "\r\n")
                writer.flush()
                return readResponse(expectCode)
            }

            // Expect greeting 220
            if (!readResponse("220")) { socket.close(); return@withContext false }

            // EHLO
            if (!sendCommand("EHLO localhost", "250")) { socket.close(); return@withContext false }

            // AUTH LOGIN
            if (!sendCommand("AUTH LOGIN", "334")) { socket.close(); return@withContext false }

            // Send base64 username
            val encodedUser = Base64.encodeToString(username.toByteArray(), Base64.NO_WRAP)
            if (!sendCommand(encodedUser, "334")) { socket.close(); return@withContext false }

            // Send base64 password
            val encodedPass = Base64.encodeToString(password.toByteArray(), Base64.NO_WRAP)
            if (!sendCommand(encodedPass, "235")) { socket.close(); return@withContext false }

            // MAIL FROM
            if (!sendCommand("MAIL FROM:<$username>", "250")) { socket.close(); return@withContext false }

            // RCPT TO
            if (!sendCommand("RCPT TO:<$recipientEmail>", "250")) { socket.close(); return@withContext false }

            // DATA
            if (!sendCommand("DATA", "354")) { socket.close(); return@withContext false }

            // Send Email Headers & Body
            writer.print("From: Dr SAM AI Health Companion <$username>\r\n")
            writer.print("To: $recipientEmail\r\n")
            writer.print("Subject: Your Dr SAM Verification Code\r\n")
            writer.print("Content-Type: text/html; charset=UTF-8\r\n")
            writer.print("\r\n") // Headers/Body Separator
            writer.print("""
                <html>
                <body style="font-family: Arial, sans-serif; background-color: #F8FAFC; padding: 20px; color: #1E293B;">
                    <div style="max-width: 500px; margin: 0 auto; background: #FFFFFF; border-radius: 16px; border: 1px solid #E2E8F0; padding: 30px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);">
                        <div style="text-align: center; margin-bottom: 24px;">
                            <span style="background-color: #2563EB; color: white; border-radius: 8px; padding: 8px 16px; font-weight: bold; font-size: 20px;">Dr SAM</span>
                            <h2 style="color: #2563EB; margin-top: 15px; margin-bottom: 5px;">Verify Your Account</h2>
                            <p style="color: #64748B; font-size: 14px; margin-top: 5px;">Your AI Healthcare Companion</p>
                        </div>
                        <p>Hello,</p>
                        <p>Thank you for signing up with Dr SAM. Please use the verification code below to authorize your registration:</p>
                        <div style="background-color: #EFF6FF; border: 1.5px dashed #3B82F6; border-radius: 12px; padding: 15px; text-align: center; margin: 24px 0;">
                            <span style="font-size: 32px; font-weight: 800; letter-spacing: 5px; color: #2563EB;">$verificationCode</span>
                        </div>
                        <p style="font-size: 13px; color: #64748B;">This verification code is confidential and intended solely for your initial sign-up approval.</p>
                        <hr style="border: 0; border-top: 1px solid #E2E8F0; margin: 24px 0;" />
                        <p style="font-size: 11px; color: #94A3B8; text-align: center; line-height: 1.5;">
                            IMPORTANT DISCLAIMER: Dr SAM is an AI health assistant and not a licensed hospital. Seek professional medical consultation for serious symptoms.
                        </p>
                    </div>
                </body>
                </html>
            """.trimIndent() + "\r\n")
            writer.print(".\r\n") // commit
            writer.flush()

            if (!readResponse("250")) { socket.close(); return@withContext false }

            sendCommand("QUIT", "221")
            socket.close()
            Log.d(TAG, "Email dispatched successfully!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending email via SMTP: ${e.message}", e)
            false
        }
    }
}
