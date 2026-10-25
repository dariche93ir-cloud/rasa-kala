package com.example.mali

import android.content.Context
import java.sql.Connection
import java.sql.DriverManager

data class Conn(
    val server: String = "",
    val port: Int = 1433,
    val db: String = "db_mali",
    val user: String = "",
    val pass: String = "",
    val instance: String = "",
    val ssl: Boolean = false
) {
    fun isConfigured() = server.isNotBlank() && db.isNotBlank() && user.isNotBlank()
}

object Db {
    private const val PREF = "conn_settings"

    fun load(ctx: Context): Conn {
        val p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return Conn(
            server = p.getString("server", "") ?: "",
            port = p.getInt("port", 1433),
            db = p.getString("db", "db_mali") ?: "db_mali",
            user = p.getString("user", "") ?: "",
            pass = p.getString("pass", "") ?: "",
            instance = p.getString("instance", "") ?: "",
            ssl = p.getBoolean("ssl", false)
        )
    }

    fun save(ctx: Context, c: Conn) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString("server", c.server)
            .putInt("port", c.port)
            .putString("db", c.db)
            .putString("user", c.user)
            .putString("pass", c.pass)
            .putString("instance", c.instance)
            .putBoolean("ssl", c.ssl)
            .apply()
    }

    /** باید روی Thread پس‌زمینه صدا زده شود */
    fun connect(c: Conn): Connection {
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        val inst = if (c.instance.isNotBlank()) ";instance=${c.instance}" else ""
        val ssl = if (c.ssl) "request" else "off"
        val url = "jdbc:jtds:sqlserver://${c.server}:${c.port}/${c.db}$inst;ssl=$ssl;loginTimeout=10;socketTimeout=30"
        return DriverManager.getConnection(url, c.user, c.pass)
    }
}
