package local.daily.habits.backup

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import local.daily.habits.data.*
import java.io.*
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.*

data class RestorePreview(val document:BackupDocument,val assets:File?,val missingAssets:Boolean)
class BackupService(private val context:Context,private val repo:HabitRepository,private val settings:SettingsStore) {
    private val assets=File(context.filesDir,"assets").apply{mkdirs()}
    private val journal=File(context.filesDir,"restore-journal.json")
    private fun hash(file:File):String { val md=MessageDigest.getInstance("SHA-256");file.inputStream().use{input->val buffer=ByteArray(8192);while(true){val n=input.read(buffer);if(n<0)break;md.update(buffer,0,n)}};return md.digest().joinToString(""){"%02x".format(it)} }
    suspend fun export(output:OutputStream,zip:Boolean)=withContext(Dispatchers.IO) {
        val doc=BackupDocument(data=repo.snapshot.first(),settings=settings.values.first())
        if(!zip){output.use{it.write(BackupCodec.encode(doc).toByteArray())};return@withContext}
        val files=listOf("wallpaper","audio").mapNotNull{doc.settings[it]?.takeIf{it.isNotEmpty()}?.let{File(assets,it)}}.filter{it.isFile}.distinct()
        val checksums=files.associate{"assets/${it.name}" to hash(it)}
        ZipOutputStream(output).use { z->
            fun entry(name:String,bytes:ByteArray){z.putNextEntry(ZipEntry(name));z.write(bytes);z.closeEntry()}
            entry("data.json",BackupCodec.encode(doc).toByteArray());entry("checksums.json",Json.encodeToString(checksums).toByteArray())
            files.forEach{f->z.putNextEntry(ZipEntry("assets/${f.name}"));f.inputStream().use{it.copyTo(z)};z.closeEntry()}
        }
    }
    suspend fun inspect(input:InputStream):RestorePreview=withContext(Dispatchers.IO) {
        val dir=File(context.cacheDir,"restore-${UUID.randomUUID()}").apply{mkdirs()}
        val source=File(dir,"source")
        input.use{i->source.outputStream().use{o->copyLimited(i,o,100L*1024*1024)}}
        val header=source.inputStream().use{byteArrayOf(it.read().toByte(),it.read().toByte())}
        if(header[0]!=80.toByte() || header[1]!=75.toByte())return@withContext RestorePreview(BackupCodec.decode(source.readText()),null,true)
        var total=0L;var count=0;val names=mutableSetOf<String>()
        ZipInputStream(source.inputStream()).use{z->while(true){val e=z.nextEntry?:break
            require(++count<=2000 && BackupCodec.safePath(e.name) && names.add(e.name)){"压缩包路径或条目无效"}
            require(e.name=="data.json" || e.name=="checksums.json" || (e.name.startsWith("assets/") && e.name.split('/').size==2)){"不支持的备份条目"}
            require(!e.isDirectory)
            val file=File(dir,e.name);file.parentFile?.mkdirs()
            file.outputStream().use{o->total+=copyLimited(z,o,250L*1024*1024-total)}
        }}
        val doc=BackupCodec.decode(File(dir,"data.json").readText())
        val checksums=Json.decodeFromString<Map<String,String>>(File(dir,"checksums.json").readText())
        names.filter{it.startsWith("assets/")}.forEach{require(checksums[it]==hash(File(dir,it))){"素材校验失败"}}
        require(checksums.keys.all{it in names})
        RestorePreview(doc,File(dir,"assets"),false)
    }
    suspend fun restore(preview:RestorePreview)=withContext(Dispatchers.IO) {
        val old=BackupDocument(data=repo.snapshot.first(),settings=settings.values.first())
        val newSettings=preview.document.settings.toMutableMap()
        listOf("wallpaper","audio").forEach{key->val name=newSettings[key];val source=name?.let{preview.assets?.resolve(it)}
            if(source?.isFile==true){val newName=UUID.randomUUID().toString()+"."+source.extension;source.copyTo(File(assets,newName));newSettings[key]=newName}else newSettings.remove(key)
        }
        val pending=File(context.filesDir,"restore-journal.tmp")
        FileOutputStream(pending).use{it.write(BackupCodec.encode(old).toByteArray());it.fd.sync()}
        check(pending.renameTo(journal)){"无法创建恢复安全快照"}
        try{repo.replace(preview.document.data);settings.replace(newSettings);check(journal.delete()){ "无法完成恢复提交" }}catch(e:Exception){repo.replace(old.data);settings.replace(old.settings);journal.delete();throw e}
    }
    suspend fun recover()=withContext(Dispatchers.IO){if(journal.isFile){val old=BackupCodec.decode(journal.readText());repo.replace(old.data);settings.replace(old.settings);check(journal.delete())}}
    private fun copyLimited(input:InputStream,out:OutputStream,limit:Long):Long { var total=0L;val b=ByteArray(8192);while(true){val n=input.read(b);if(n<0)break;total+=n;require(total<=limit){"备份文件超过大小限制"};out.write(b,0,n)};return total }
}
