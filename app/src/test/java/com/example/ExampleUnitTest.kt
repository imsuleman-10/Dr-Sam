package com.example

import com.example.util.SupabaseService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testSupabaseConnectionAndKeys() = runBlocking {
    val url = try { BuildConfig.SUPABASE_URL } catch (e: Exception) { "" }
    val key = try { BuildConfig.SUPABASE_ANON_KEY } catch (e: Exception) { "" }

    println("=== SUPABASE CREDENTIALS CHECK ===")
    println("Supabase URL in BuildConfig exists: ${url.isNotBlank()}")
    if (url.isNotBlank()) {
        println("URL format: ${if (url.startsWith("http")) "Valid (starts with http)" else "Invalid (does not start with http)"}")
        println("URL content: $url")
    } else {
        println("URL is Blank!")
    }

    println("Supabase Key in BuildConfig exists: ${key.isNotBlank()}")
    if (key.isNotBlank()) {
        println("Key snippet (first 10 chars): ${if (key.length > 10) key.substring(0, 10) + "..." else key}")
    } else {
        println("Key is Blank!")
    }

    println("Conducting active server connection test...")
    if (url.isNotBlank() && key.isNotBlank() && !url.contains("your-project-id")) {
        val success = SupabaseService.testConnection(url, key)
        println("Connection ping status: ${if (success) "SUCCESS!" else "FAILED!"}")
        if (success) {
            println("Verification complete: Supabase credentials are valid and active!")
        } else {
            println("Verification complete: PING FAILED! Check if Supabase keys are active, database is running, or if tables are set up.")
        }
    } else {
        println("Connection test skipped because URL or Anon Key is empty or placeholder.")
    }
    println("==================================")
  }
}
