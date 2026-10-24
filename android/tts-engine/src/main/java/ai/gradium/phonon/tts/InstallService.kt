package ai.gradium.phonon.tts

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import java.util.concurrent.CancellationException
import kotlin.concurrent.thread

/** A user-started foreground operation survives activity recreation and screen backgrounding. */
class InstallService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "cancel") { cancel(); return START_NOT_STICKY }
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel("models", getString(R.string.download), NotificationManager.IMPORTANCE_LOW))
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, "models") else Notification.Builder(this)
        val open = PendingIntent.getActivity(this, 0, Intent(this, SettingsActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, InstallService::class.java).setAction("cancel"), PendingIntent.FLAG_IMMUTABLE)
        startForeground(1, builder.setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle(message)
            .setContentText(getString(if (deleting) R.string.deleting_model else R.string.download_background)).setContentIntent(open).setOngoing(true)
            .apply { if (!deleting) addAction(android.R.drawable.ic_delete, getString(R.string.cancel), stop) }.build())
        val task = synchronized(InstallService) { val next = pending; pending = null; next }
        if (task == null) { if (!running) stopSelf(); return START_NOT_STICKY }
        thread(name = "model-install") {
            try { task.run(); message = getString(task.successMessage) }
            catch (e: CancellationException) { message = getString(R.string.cancelled) }
            catch (t: Throwable) { message = if (cancelled) getString(R.string.cancelled) else getString(task.failureMessage) }
            finally {
                synchronized(InstallService) {
                    downloader = null
                    stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
                    running = false
                }
            }
        }
        return START_NOT_STICKY
    }

    companion object {
        @Volatile var deleting = false; private set
        @Volatile var running = false; private set
        @Volatile var cancelled = false; private set
        @Volatile var message = ""; private set
        @Volatile private var downloader: SecureDownload? = null
        private data class Operation(val successMessage: Int, val failureMessage: Int, val run: () -> Unit)
        private var pending: Operation? = null
        @Synchronized fun start(context: Context, successMessage: Int = R.string.install_complete, deletingModel: Boolean = false, task: () -> Unit): Boolean {
            if (running || Model.installing) return false
            running = true; deleting = deletingModel; cancelled = false; message = context.getString(if (deletingModel) R.string.deleting_model else R.string.installing_model); pending = Operation(successMessage, if (deletingModel) R.string.delete_model_failed else R.string.operation_failed, task)
            try {
                val intent = Intent(context, InstallService::class.java)
                if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
            } catch (t: Throwable) { pending = null; running = false; throw t }
            return true
        }
        internal fun packDownloader(client: SecureDownload) { downloader = client }
        internal fun packProgress(context: Context, done: Long, total: Long) {
            message = "${context.getString(R.string.download)}: ${done * 100 / total}% (${done / 1024} / ${total / 1024} KiB)"
        }
        fun cancel() { if (deleting) return; cancelled = true; downloader?.cancel() }
        fun checkCancelled() { if (cancelled) throw CancellationException() }
        fun download(context: Context, model: DownloadModel, token: String?, targetId: String = model.id) {
            val client = SecureDownload({ cancelled }) { done, total ->
                message = "${context.getString(model.label)}: ${done * 100 / total}% (${done / 1024} / ${total / 1024} KiB)"
            }
            downloader = client
            Model.install(context, { cancelled }, targetId) { into -> client.download(model, token, into); checkCancelled() }
        }
    }
}
