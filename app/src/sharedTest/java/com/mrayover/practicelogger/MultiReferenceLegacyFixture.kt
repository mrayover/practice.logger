package com.mrayover.practicelogger

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.json.JSONObject
import org.junit.Assert.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.Base64
import java.util.zip.CRC32

/** Frozen Room23 input, not a schema24 migration. Never constructs legacy rows with future entities. */
internal class MultiReferenceLegacyFixture(private val context: Context, val name: String, val files: File,
    private val readResource: (String) -> java.io.InputStream = { MultiReferenceLegacyFixture::class.java.classLoader!!.getResourceAsStream(it)!! }) {
    private val schema = readResource("com.mrayover.practicelogger.BlockDatabase/23.json").bufferedReader().use {
        JSONObject(it.readText()).getJSONObject("database")
    }
    private val entities = schema.getJSONArray("entities")
    private var rows: Map<String, List<List<String?>>> = emptyMap()
    private var originals: Map<String, String> = emptyMap()

    fun create() {
        context.getDatabasePath(name).parentFile!!.mkdirs()
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name).callback(object : SupportSQLiteOpenHelper.Callback(23) {
                override fun onConfigure(db: SupportSQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
                override fun onCreate(db: SupportSQLiteDatabase) {
                    repeat(entities.length()) { i ->
                        val entity = entities.getJSONObject(i)
                        fun execute(sql: String) = db.execSQL(sql.replace("$" + "{TABLE_NAME}", entity.getString("tableName")))
                        execute(entity.getString("createSql"))
                        val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                        repeat(indices.length()) { execute(indices.getJSONObject(it).getString("createSql")) }
                    }
                    val setup = schema.getJSONArray("setupQueries")
                    repeat(setup.length()) { db.execSQL(setup.getString(it)) }
                    // Frozen separately because Room's JSON does not export application triggers.
                    resource("stage3a/room23-triggers.sql").split("-- next").filter { it.isNotBlank() }.forEach(db::execSQL)
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) =
                    error("Fixture construction must stay at Room23")
            }).build())
        try {
            val db = helper.writableDatabase
            val image = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+ip1sAAAAASUVORK5CYII=")
            val chunks = ByteBuffer.wrap(image).apply { position(8) }
            while (chunks.hasRemaining()) {
                val length = chunks.int
                val start = chunks.position()
                val crc = CRC32().apply { update(image, start, length + 4) }
                chunks.position(start + length + 4)
                check(crc.value == (chunks.int.toLong() and 0xffffffffL)) { "Invalid fixture PNG chunk" }
            }
            original("media-library", 1, "png", image, "IMAGE", "image/png", null, null, db)
            original("media-library", 2, "pdf", pdf(), "PDF", "application/pdf", 2, null, db)
            original("media-library", 3, "wav", wav(), "AUDIO", "audio/wav", null, 4000, db)
            File(files, "block-images/${id(4)}.img").apply { parentFile!!.mkdirs(); writeBytes(image) }
            File(files, "recordings/${id(5)}.wav").apply { parentFile!!.mkdirs(); writeBytes(wav()) }
            resource("stage3a/room23-rows.sql").split(';').filter { it.isNotBlank() }.forEach(db::execSQL)
            rows = snapshot(db)
            originals = hashes()
            assertEquals(27, rows.size)
            assertTrue("Every durable table has populated evidence", rows.values.all { it.isNotEmpty() })
            db.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
        } finally { helper.close() }
    }

    /** Call before migration, after migration, and after cold reopen. Projects only frozen23 columns. */
    fun remember(db: SupportSQLiteDatabase) { rows = snapshot(db); originals = hashes() }

    fun assertPreserved(db: SupportSQLiteDatabase, checkOriginals: Boolean = true) {
        assertEquals("All Room23 row values/IDs must survive", rows, snapshot(db))
        if (checkOriginals) assertEquals("Original bytes and ownership paths must survive", originals, hashes())
        db.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
        db.query("PRAGMA integrity_check").use { assertTrue(it.moveToFirst()); assertEquals("ok", it.getString(0)) }
    }

    private fun snapshot(db: SupportSQLiteDatabase): Map<String, List<List<String?>>> =
        (0 until entities.length()).associate { i ->
            val entity = entities.getJSONObject(i)
            val fields = entity.getJSONArray("fields")
            val columns = (0 until fields.length()).map { fields.getJSONObject(it).getString("columnName") }
            entity.getString("tableName") to db.query("SELECT ${columns.joinToString { "`$it`" }} FROM `${entity.getString("tableName")}` ORDER BY ${columns.joinToString { "`$it`" }}").use { c ->
                buildList { while (c.moveToNext()) add(columns.indices.map { if (c.isNull(it)) null else c.getString(it) }) }
            }
        }

    private fun hashes() = files.walkTopDown().filter { it.isFile }.associate {
        it.relativeTo(files).invariantSeparatorsPath to hash(it.readBytes())
    }
    private fun resource(path: String) = readResource(path).bufferedReader().use { it.readText() }
    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun id(n: Int) = "00000000-0000-4000-8000-${n.toString().padStart(12, '0')}"
    private fun original(store: String, n: Int, ext: String, bytes: ByteArray, category: String, mime: String,
        pages: Int?, duration: Int?, db: SupportSQLiteDatabase) {
        File(files, "$store/${id(n)}.$ext").apply { parentFile!!.mkdirs(); writeBytes(bytes) }
        db.execSQL("INSERT INTO media_assets VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)", arrayOf<Any?>(
            id(n), "Fixture $category", "Fixture.$ext", mime, category, "${id(n)}.$ext", bytes.size, 1,
            if (category == "AUDIO") null else if (category == "PDF") 100 else 1,
            if (category == "AUDIO") null else if (category == "PDF") 100 else 1, pages, duration, hash(bytes)))
    }
    private fun wav(): ByteArray = ByteBuffer.allocate(64044).order(ByteOrder.LITTLE_ENDIAN).apply {
        put("RIFF".toByteArray()); putInt(64036); put("WAVEfmt ".toByteArray()); putInt(16)
        putShort(1); putShort(1); putInt(8000); putInt(16000); putShort(2); putShort(16)
        put("data".toByteArray()); putInt(64000)
    }.array()
    private fun pdf(): ByteArray {
        val out = StringBuilder("%PDF-1.4\n")
        val objects = listOf("<< /Type /Catalog /Pages 2 0 R >>", "<< /Type /Pages /Kids [3 0 R 4 0 R] /Count 2 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 100 100] /Resources << >> /Contents 5 0 R >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 100 100] /Resources << >> /Contents 5 0 R >>",
            "<< /Length 0 >>\nstream\nendstream")
        val offsets = objects.mapIndexed { i, body -> out.length.also { out.append("${i + 1} 0 obj\n$body\nendobj\n") } }
        val xref = out.length
        out.append("xref\n0 6\n0000000000 65535 f \n")
        offsets.forEach { out.append("%010d 00000 n \n".format(it)) }
        out.append("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return out.toString().toByteArray(Charsets.US_ASCII)
    }
}
