package com.vishruu.vsfileexplorer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class NetworkType(val label: String) {
    FTP("FTP"),
    SFTP("SFTP")
}

data class NetworkConnection(
    val id: String,
    val name: String,
    val type: NetworkType,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val basePath: String = "/"
)

object NetworkStorage {
    private const val PREFS = "vs_network_prefs"
    private const val KEY_LIST = "connections"

    fun load(context: Context): List<NetworkConnection> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_LIST, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                NetworkConnection(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    type = NetworkType.valueOf(o.getString("type")),
                    host = o.getString("host"),
                    port = o.getInt("port"),
                    username = o.getString("username"),
                    password = o.getString("password"),
                    basePath = o.optString("basePath", "/")
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    fun save(context: Context, list: List<NetworkConnection>) {
        val arr = JSONArray()
        list.forEach { c ->
            val o = JSONObject()
            o.put("id", c.id)
            o.put("name", c.name)
            o.put("type", c.type.name)
            o.put("host", c.host)
            o.put("port", c.port)
            o.put("username", c.username)
            o.put("password", c.password)
            o.put("basePath", c.basePath)
            arr.put(o)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LIST, arr.toString()).apply()
    }

    fun add(context: Context, conn: NetworkConnection) {
        val list = load(context).toMutableList()
        list.add(conn)
        save(context, list)
    }

    fun update(context: Context, conn: NetworkConnection) {
        val list = load(context).toMutableList()
        val idx = list.indexOfFirst { it.id == conn.id }
        if (idx >= 0) list[idx] = conn
        save(context, list)
    }

    fun delete(context: Context, id: String) {
        val list = load(context).filter { it.id != id }
        save(context, list)
    }
}