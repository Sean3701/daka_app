package local.daily.habits.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import local.daily.habits.data.*
import local.daily.habits.backup.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

@Composable fun SettingsScreen(settings:Map<String,String>,set:(String,String)->Unit,repository:HabitRepository,settingsStore:SettingsStore,onMessage:(String)->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val service=remember{BackupService(context,repository,settingsStore)}
    var preview by remember{mutableStateOf<RestorePreview?>(null)}
    var busy by remember{mutableStateOf(false)}
    fun job(block:suspend ()->Unit){scope.launch{busy=true;try{block()}catch(e:Exception){onMessage(e.message?:"操作失败")}finally{busy=false}}}
    val jsonExport=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->if(uri!=null)job{service.export(requireNotNull(context.contentResolver.openOutputStream(uri)),false);onMessage("JSON 备份已导出")}}
    val zipExport=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")){uri->if(uri!=null)job{service.export(requireNotNull(context.contentResolver.openOutputStream(uri)),true);onMessage("完整 ZIP 备份已导出")}}
    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)job{preview=service.inspect(requireNotNull(context.contentResolver.openInputStream(uri)))}}
    fun importAsset(uri:android.net.Uri?,key:String){if(uri!=null)job{
        val dir=File(context.filesDir,"assets").apply{mkdirs()};val file=File(dir,UUID.randomUUID().toString()+if(key=="wallpaper")".image" else ".audio")
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){context.contentResolver.openInputStream(uri)!!.use{input->file.outputStream().use{output->val b=ByteArray(8192);var total=0;while(true){val n=input.read(b);if(n<0)break;total+=n;require(total<=20*1024*1024){"素材不得超过 20 MB"};output.write(b,0,n)}}}}
        settingsStore.set(key,file.name)
    }}
    val wallpaper=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){importAsset(it,"wallpaper")}
    val audio=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){importAsset(it,"audio")}
    val notificationPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->onMessage(if(granted)"通知权限已开启" else "通知未授权，打卡仍可使用")}
    SodaCard {
        Text("打卡提醒")
        TextButton(onClick={if(android.os.Build.VERSION.SDK_INT>=33)notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS) else onMessage("此系统无需单独申请通知权限")}){Text("开启通知权限")}
        TextButton(onClick={job{local.daily.habits.reminders.ReminderScheduler(context).notify("测试通知","打卡提醒已就绪",100);onMessage("已尝试发送，请检查系统通知；未授权时不会显示。")}}){Text("测试通知")}
        TextButton(onClick={context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,context.packageName))}){Text("系统通知与提示音设置")}
        if(android.os.Build.VERSION.SDK_INT>=31 && !context.getSystemService(android.app.AlarmManager::class.java).canScheduleExactAlarms()) {
            Text("未获准精确提醒，通知可能延迟。")
            TextButton(onClick={context.startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:${context.packageName}")))}){Text("允许精确提醒")}
        }
    }
    SodaCard { Text("外观",style=MaterialTheme.typography.titleMedium)
        listOf("跟随系统","浅色","深色").forEach { v->Row{RadioButton((settings["theme"]?:"跟随系统")==v,{set("theme",v)});TextButton(onClick={set("theme",v)}){Text(v)}} }
        TextButton(onClick={wallpaper.launch(arrayOf("image/*"))}){Text("选择壁纸")}
        TextButton(onClick={set("wallpaper","")}){Text("清除壁纸")}
    }
    SodaCard { Text("底栏模块")
        listOf("习惯","月历","记录","设置").forEach{v->Row{Text(v,Modifier.weight(1f));Switch(settings["show_$v"]!="false",{set("show_$v",it.toString())})}}
    }
    SodaCard { Text("反馈与提醒");Row{Text("打卡音效",Modifier.weight(1f));Switch(settings["sound"]!="false",{set("sound",it.toString())})};Row{Text("震动",Modifier.weight(1f));Switch(settings["vibration"]=="true",{set("vibration",it.toString())})};TextButton(onClick={audio.launch(arrayOf("audio/*"))}){Text("导入打卡音频")};TextButton(onClick={set("audio","")}){Text("恢复默认音效")} }
    SodaCard { Text("数据备份",style=MaterialTheme.typography.titleMedium);Text("数据仅保存在本机，卸载前请导出备份。")
        Button(onClick={zipExport.launch("日常打卡完整备份.zip")},enabled=!busy){Text("导出完整 ZIP")}
        TextButton(onClick={jsonExport.launch("日常打卡.json")},enabled=!busy){Text("导出 JSON（不含素材）")}
        TextButton(onClick={importer.launch(arrayOf("application/json","application/zip","application/octet-stream"))},enabled=!busy){Text("导入恢复 ZIP / JSON")}
        if(busy)Text("正在处理，请稍候…")
    }
    preview?.let{p->AlertDialog(onDismissRequest={if(!busy)preview=null},title={Text("替换本机数据？")},text={Text("备份包含 ${p.document.data.habits.size} 个习惯、${p.document.data.completions.size} 条打卡和 ${p.document.data.flights.size} 次起飞。\n恢复将覆盖现有数据。"+(if(p.missingAssets)"\nJSON 中的壁纸和音效将恢复默认。" else ""))},confirmButton={TextButton(onClick={job{service.restore(p);preview=null;onMessage("恢复完成")}},enabled=!busy){Text("确认恢复")}},dismissButton={TextButton(onClick={preview=null},enabled=!busy){Text("取消")}})}
}
