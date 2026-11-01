package com.mrayover.practicelogger

import android.content.ContentValues
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ImportedAudioWorkflowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val app get()=compose.activity.application
    private val device get()=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val audio get()=ViewModelProvider(compose.activity)[RecordingViewModel::class.java]
    private fun shown(text:String)=compose.waitUntil(15000){compose.onAllNodesWithText(text).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()}
    private fun tag(name:String)=compose.onNodeWithTag(name).scrollIfNeeded().assertIsDisplayed()
    private fun click(text:String){shown(text);compose.onNodeWithText(text).scrollIfNeeded().performClick()}
    private fun media(name:String,mime:String,bytes:ByteArray):Uri {
        val modern = android.os.Build.VERSION.SDK_INT >= 29
        if (!modern) {
            device.executeShellCommand("pm grant ${app.packageName} android.permission.READ_EXTERNAL_STORAGE")
            device.executeShellCommand("pm grant ${app.packageName} android.permission.WRITE_EXTERNAL_STORAGE")
        }
        val collection = if (modern) MediaStore.Downloads.EXTERNAL_CONTENT_URI else MediaStore.Files.getContentUri("external")
        val uri=app.contentResolver.insert(collection,ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME,name);put(MediaStore.MediaColumns.MIME_TYPE,mime)
            if (!modern) {
                val downloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                check(downloads.isDirectory || downloads.mkdirs())
                @Suppress("DEPRECATION")
                put(MediaStore.MediaColumns.DATA,File(downloads,name).absolutePath)
            }
        })!!
        app.contentResolver.openOutputStream(uri)!!.use{it.write(bytes)};return uri
    }
    private fun wave():ByteArray {
        val frames=10000;return ByteBuffer.allocate(44+frames*2).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray());putInt(36+frames*2);put("WAVEfmt ".toByteArray());putInt(16);putShort(1);putShort(1)
            putInt(8000);putInt(16000);putShort(2);putShort(16);put("data".toByteArray());putInt(frames*2)
            repeat(frames){putShort((kotlin.math.sin(it*2*Math.PI*440/8000)*2000).toInt().toShort())}
        }.array()
    }
    private fun shot(name:String){val dir=File(compose.activity.getExternalFilesDir(null),"audio-import").apply{mkdirs()};device.takeScreenshot(File(dir,"$name.png"))}
    @Test fun importSelectRepeatInterruptAndKeepExistingTakes() {
        val db=BlockDatabase.open(app)
        try {
            val practice=ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
            val (block,session)=runBlocking{
                val b=BlockRepository(db).save(null,BlockDraft("Imported audio checks","Play","5","120"))
                val repo=TakeRepository(db,app)
                val free=repo.prepare(b);free.file.writeBytes(InstrumentationRegistry.getInstrumentation().context.assets.open("audio/backing.m4a").use{it.readBytes()});repo.save(free)
                val phrase=repo.prepare(b,PhraseSpec(120,4));PhraseWave.write(phrase.file,ShortArray(PhraseSpec(120,4).frames));repo.save(phrase)
                val sessions=SessionRepository(db);sessions.addBlock(b);b to sessions.start()
            }
            shown("Practice Now");compose.runOnIdle{practice.openSession(session)};shown("Practice Description")
            compose.waitUntil(15000){audio.ready};click("Loop Setup")
            tag("backing-track-toggle").assertIsOff().performClick()
            tag("phrase-bpm").assertIsNotEnabled();tag("phrase-beats").assertIsNotEnabled();tag("phrase-count-in").assertIsNotEnabled()
            tag("backing-track-save").assertIsNotEnabled()
            val old=audio.takes.value.filter{it.blockId==block}.map{it.id}.toSet();assertEquals(2,old.size)
            tag("backing-track-choose").performClick()
            assertNotNull(device.wait(Until.findObject(By.desc("Show roots")),10000));device.pressBack()
            shown("Choose file…");assertNull(audio.backingTrack);assertTrue(audio.takes.value.map{it.id}.containsAll(old))
            val uri=media("p61-backing.wav","audio/wav",wave())
            tag("backing-track-choose").performClick()
            device.wait(Until.findObject(By.desc("Show roots")),10000)!!.click()
            assertTrue(device.wait(Until.hasObject(By.text("Downloads")),10000));device.findObjects(By.text("Downloads")).last().click()
            val file=device.wait(Until.findObject(By.text("p61-backing.wav")),10000) ?: error("Picker audio fixture missing")
            file.click();compose.waitUntil(15000){!audio.busy && audio.backingTrack!=null}
            compose.onNodeWithTag("backing-track-filename",useUnmergedTree=true).assertIsDisplayed().assertTextContains("p61-backing.wav");tag("backing-track-save").assertIsEnabled();shot("ready-import")
            assertEquals(1250L,audio.backingTrack!!.imported!!.durationMs)
            tag("backing-track-save").performClick()
            compose.waitUntil(15000){!audio.busy && audio.takes.value.any{it.label=="p61-backing.wav"}}
            val saved=audio.takes.value.single{it.label=="p61-backing.wav"}
            tag("practice-loop-selector").assertTextContains(saved.label);assertNull(audio.playingId)
            assertEquals(TakeKind.IMPORTED_LOOP,saved.kind);assertNull(saved.recordedBpm);assertNull(saved.beatCount)
            app.contentResolver.delete(uri,null,null) // No retained provider permission/path is needed after Save.
            compose.runOnIdle{audio.chooseBackingTrack(block,null)};tag("practice-loop-selector").assertTextContains(saved.label)
            val bad=media("p61-invalid.wav","audio/wav",byteArrayOf(1,2,3))
            compose.runOnIdle{audio.chooseBackingTrack(block,bad)};compose.waitUntil(15000){!audio.busy};assertNull(audio.backingTrack)
            tag("practice-loop-selector").assertTextContains(saved.label);app.contentResolver.delete(bad,null,null)
            tag("backing-track-toggle").performClick();tag("phrase-start").assertExists();assertTrue(saved.isLoop)
            tag("practice-loop-play").performClick();compose.waitUntil(15000){audio.importedRepeatActive()}
            var last=0;var wraps=0
            val positions=mutableListOf<Int>()
            try { compose.waitUntil(10000){val pos=compose.runOnIdle{audio.importedPlaybackPosition()};positions.add(pos);if(pos+30<last)wraps++;last=pos;wraps>=2} }
            finally { println("Imported repeat positions: $positions; wraps=$wraps");shot("repeat-observation") }
            tag("practice-loop-play").performClick();compose.runOnIdle{assertNull(audio.playingId)}
            compose.runOnIdle{audio.play(saved)};compose.waitUntil(10000){audio.importedRepeatActive()}
            val manager=app.getSystemService(AudioManager::class.java)
            val focus=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()).setOnAudioFocusChangeListener{}.build()
            compose.runOnIdle{manager.requestAudioFocus(focus)};compose.waitUntil(10000){audio.playingId==null};compose.runOnIdle{manager.abandonAudioFocusRequest(focus)}
            compose.runOnIdle{audio.play(saved)};compose.waitUntil(10000){audio.importedRepeatActive()};device.pressHome()
            compose.waitUntil(10000){audio.playingId==null}
            app.startActivity(app.packageManager.getLaunchIntentForPackage(app.packageName)!!.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
            shown("Practice Description");compose.runOnIdle{assertNull(audio.playingId)}
            for(extension in listOf("mp3","m4a")) {
                if (extension=="mp3") { compose.runOnIdle{audio.play(saved)};compose.waitUntil(10000){audio.importedRepeatActive()} }
                val name="p61 backing with a long original filename.$extension"
                val source=media(name,if(extension=="mp3")"audio/mpeg" else "audio/mp4",InstrumentationRegistry.getInstrumentation().context.assets.open("audio/backing.$extension").use{it.readBytes()})
                compose.runOnIdle{audio.chooseBackingTrack(block,source)};compose.waitUntil(20000){!audio.busy}
                assertNotNull("$extension validation: ${audio.message}",audio.backingTrack)
                compose.runOnIdle{audio.saveBackingTrack(block)};compose.waitUntil(15000){!audio.busy && audio.takes.value.any{it.label==name}}
                assertNull("Saving must stop earlier playback without starting the import",audio.playingId)
                val row=audio.takes.value.single{it.label==name};tag("practice-loop-selector").assertTextContains(name)
                compose.runOnIdle{audio.play(row)};compose.waitUntil(15000){audio.importedRepeatActive()};compose.runOnIdle{audio.stopPlayback()}
                app.contentResolver.delete(source,null,null)
            }
            assertTrue(audio.takes.value.map{it.id}.containsAll(old));shot("imported-selected")
        } finally {compose.runOnIdle{audio.stopPlayback()};db.close()}
    }
    @Test fun reopenAndManageImportedAndEarlierRecordings() {
        val db=BlockDatabase.open(app)
        try {
            shown("Practice Now");compose.waitUntil(15000){audio.ready && audio.takes.value.any{it.kind==TakeKind.IMPORTED_LOOP}}
            val rows=audio.takes.value;val imported=rows.filter{it.kind==TakeKind.IMPORTED_LOOP};assertEquals(3,imported.size)
            assertNull(audio.playingId);assertNull(audio.backingTrack)
            val repo=TakeRepository(db,app);imported.forEach{assertTrue(repo.audioFile(it).isFile)}
            val block=runBlocking{db.blocks().find(imported.first().blockId!!)!!}
            click("Blocks");click(block.name);click("Recordings");shown("Block recordings")
            val selected=imported.first{it.fileName.endsWith(".mp3")};compose.onNodeWithTag("take-list").performScrollToNode(hasTestTag("take-${selected.id}"));compose.onNodeWithText(selected.label).assertIsDisplayed()
            tag("rename-${selected.id}").performClick();shown("Rename take")
            compose.onNodeWithText("Take label (optional)").performTextReplacement("p61 renamed imported loop")
            compose.runOnIdle { compose.activity.getSystemService(android.view.inputmethod.InputMethodManager::class.java).hideSoftInputFromWindow(compose.activity.window.decorView.windowToken,0) };click("Save take label")
            compose.waitUntil(15000){audio.takes.value.any{it.id==selected.id && it.label=="p61 renamed imported loop"}}
            val output=media("p61-export.mp3","audio/mpeg",byteArrayOf())
            compose.runOnIdle{audio.export(selected,output)};compose.waitUntil(15000){!audio.busy && audio.message?.startsWith("Audio exported")==true}
            assertArrayEquals(repo.audioFile(selected).readBytes(),app.contentResolver.openInputStream(output)!!.use{it.readBytes()});app.contentResolver.delete(output,null,null)
            val remove=imported.first{it.fileName.endsWith(".m4a")};compose.onNodeWithTag("take-list").performScrollToNode(hasTestTag("take-${remove.id}"));tag("delete-${remove.id}").performClick();click("Delete take permanently")
            compose.waitUntil(15000){!audio.busy && audio.takes.value.none{it.id==remove.id}}
            assertFalse(File(app.filesDir,"recordings/${remove.fileName}").exists())
            assertEquals(2,audio.takes.value.count{it.kind==TakeKind.IMPORTED_LOOP});assertTrue(audio.takes.value.any{it.kind==TakeKind.FREE});assertTrue(audio.takes.value.any{it.kind==TakeKind.RECORDED_LOOP})
            shot("managed-import")
        } finally {db.close()}
    }
}
