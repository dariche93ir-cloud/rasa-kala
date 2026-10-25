package com.example.mali

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var edtCode: EditText
    private lateinit var edtName: EditText
    private lateinit var edtEtc: EditText
    private lateinit var btnSave: Button
    private lateinit var progress: ProgressBar
    private lateinit var lstGroups: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val items = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        edtCode = findViewById(R.id.edtCode)
        edtName = findViewById(R.id.edtName)
        edtEtc = findViewById(R.id.edtEtc)
        btnSave = findViewById(R.id.btnSave)
        progress = findViewById(R.id.progress)
        lstGroups = findViewById(R.id.lstGroups)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        lstGroups.adapter = adapter

        btnSave.setOnClickListener { saveGroup() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "تنظیمات اتصال").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setBusy(busy: Boolean) {
        progress.visibility = if (busy) View.VISIBLE else View.INVISIBLE
        btnSave.isEnabled = !busy
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    /** خواندن لیست گروه‌ها و محاسبه کد بعدی */
    private fun refresh() {
        val conn = Db.load(this)
        if (!conn.isConfigured()) {
            items.clear(); adapter.notifyDataSetChanged()
            edtCode.setText("")
            toast("ابتدا از منوی «تنظیمات اتصال» اطلاعات سرور را وارد کنید")
            return
        }
        setBusy(true)
        lifecycleScope.launch {
            try {
                val (list, nextCode) = withContext(Dispatchers.IO) {
                    Db.connect(conn).use { con ->
                        val rows = mutableListOf<String>()
                        con.createStatement().use { st ->
                            st.executeQuery(
                                "SELECT f_code, f_name, f_etc FROM t_group_k " +
                                "ORDER BY CAST(f_code AS BIGINT) DESC"
                            ).use { rs ->
                                while (rs.next()) {
                                    val etc = rs.getString("f_etc")?.trim().orEmpty()
                                    rows += "${rs.getString("f_code")} - ${rs.getString("f_name")}" +
                                            if (etc.isNotEmpty()) "\n$etc" else ""
                                }
                            }
                        }
                        var next = 1L
                        con.createStatement().use { st ->
                            st.executeQuery(
                                "SELECT ISNULL(MAX(CAST(f_code AS BIGINT)),0)+1 FROM t_group_k"
                            ).use { rs -> if (rs.next()) next = rs.getLong(1) }
                        }
                        rows to next
                    }
                }
                items.clear(); items.addAll(list); adapter.notifyDataSetChanged()
                edtCode.setText(nextCode.toString())
            } catch (e: Exception) {
                toast("خطا در اتصال: ${e.message}")
            } finally {
                setBusy(false)
            }
        }
    }

    /** ثبت گروه جدید؛ کد = بزرگترین f_code + 1 (داخل خود INSERT) */
    private fun saveGroup() {
        val conn = Db.load(this)
        if (!conn.isConfigured()) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return
        }
        val name = edtName.text.toString().trim()
        val etc = edtEtc.text.toString().trim()
        if (name.isEmpty()) {
            edtName.error = "نام گروه را وارد کنید"
            return
        }
        setBusy(true)
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    Db.connect(conn).use { con ->
                        con.prepareStatement(
                            "INSERT INTO t_group_k (f_code, f_name, f_etc) " +
                            "SELECT ISNULL(MAX(CAST(f_code AS BIGINT)),0)+1, ?, ? " +
                            "FROM t_group_k WITH (UPDLOCK, HOLDLOCK)"
                        ).use { ps ->
                            ps.setString(1, name)
                            ps.setString(2, etc)
                            ps.executeUpdate()
                        }
                    }
                }
                edtName.text.clear(); edtEtc.text.clear()
                toast("گروه با موفقیت ثبت شد")
                refresh()
            } catch (e: Exception) {
                toast("خطا در ثبت: ${e.message}")
                setBusy(false)
            }
        }
    }
}
