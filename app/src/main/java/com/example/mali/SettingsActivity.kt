package com.example.mali

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var edtServer: EditText
    private lateinit var edtPort: EditText
    private lateinit var edtInstance: EditText
    private lateinit var edtDb: EditText
    private lateinit var edtUser: EditText
    private lateinit var edtPass: EditText
    private lateinit var chkSsl: CheckBox
    private lateinit var btnTest: Button
    private lateinit var btnSave: Button
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        edtServer = findViewById(R.id.edtServer)
        edtPort = findViewById(R.id.edtPort)
        edtInstance = findViewById(R.id.edtInstance)
        edtDb = findViewById(R.id.edtDb)
        edtUser = findViewById(R.id.edtUser)
        edtPass = findViewById(R.id.edtPass)
        chkSsl = findViewById(R.id.chkSsl)
        btnTest = findViewById(R.id.btnTest)
        btnSave = findViewById(R.id.btnSaveSettings)
        progress = findViewById(R.id.progressSettings)

        val c = Db.load(this)
        edtServer.setText(c.server)
        edtPort.setText(c.port.toString())
        edtInstance.setText(c.instance)
        edtDb.setText(c.db)
        edtUser.setText(c.user)
        edtPass.setText(c.pass)
        chkSsl.isChecked = c.ssl

        btnSave.setOnClickListener {
            val conn = readForm() ?: return@setOnClickListener
            Db.save(this, conn)
            Toast.makeText(this, "تنظیمات ذخیره شد", Toast.LENGTH_SHORT).show()
            finish()
        }
        btnTest.setOnClickListener { testConnection() }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun readForm(): Conn? {
        val server = edtServer.text.toString().trim()
        val db = edtDb.text.toString().trim()
        val user = edtUser.text.toString().trim()
        if (server.isEmpty()) { edtServer.error = "آدرس سرور را وارد کنید"; return null }
        if (db.isEmpty()) { edtDb.error = "نام دیتابیس را وارد کنید"; return null }
        if (user.isEmpty()) { edtUser.error = "نام کاربری را وارد کنید"; return null }
        val port = edtPort.text.toString().trim().toIntOrNull() ?: 1433
        return Conn(
            server = server,
            port = port,
            db = db,
            user = user,
            pass = edtPass.text.toString(),
            instance = edtInstance.text.toString().trim(),
            ssl = chkSsl.isChecked
        )
    }

    private fun testConnection() {
        val conn = readForm() ?: return
        progress.visibility = View.VISIBLE
        btnTest.isEnabled = false
        lifecycleScope.launch {
            val result = try {
                withContext(Dispatchers.IO) {
                    Db.connect(conn).use { con ->
                        con.createStatement().use { st ->
                            st.executeQuery("SELECT COUNT(*) FROM t_group_k").use { rs ->
                                rs.next()
                                "اتصال موفق بود ✔ (تعداد گروه‌ها: ${rs.getInt(1)})"
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                "خطا: ${e.message}"
            }
            progress.visibility = View.INVISIBLE
            btnTest.isEnabled = true
            Toast.makeText(this@SettingsActivity, result, Toast.LENGTH_LONG).show()
        }
    }
}
