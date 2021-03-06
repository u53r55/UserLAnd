package tech.ula.utils

import tech.ula.model.entities.Asset
import java.io.File

class DownloadUtility(
    private val timestampPreferences: TimestampPreferences,
    private val downloadManagerWrapper: DownloadManagerWrapper,
    private val applicationFilesDir: File
) {

    private val downloadDirectory = downloadManagerWrapper.getDownloadsDirectory()

    fun downloadRequirements(assetList: List<Asset>): List<Long> {
        clearPreviousDownloadsFromDownloadsDirectory()
        return assetList.map { download(it) }
    }

    fun downloadedSuccessfully(id: Long): Boolean {
        return downloadManagerWrapper.downloadHasNotFailed(id)
    }

    fun getReasonForDownloadFailure(id: Long): String {
        return downloadManagerWrapper.getDownloadFailureReason(id)
    }

    private fun download(asset: Asset): Long {
        var branch = "master"
        if (asset.distributionType.equals("support", true))
            branch = "staging"
        val url = "https://github.com/CypherpunkArmory/UserLAnd-Assets-" +
                "${asset.distributionType}/raw/$branch/assets/" +
                "${asset.architectureType}/${asset.name}"
        val destination = asset.concatenatedName
        val request = downloadManagerWrapper.generateDownloadRequest(url, destination)
        deletePreviousDownloadFromLocalDirectory(asset)
        return downloadManagerWrapper.enqueue(request)
    }

    private fun clearPreviousDownloadsFromDownloadsDirectory() {
        for (file in downloadDirectory.listFiles()) {
            if (file.name.toLowerCase().contains("userland")) {
                file.delete()
            }
        }
    }

    private fun deletePreviousDownloadFromLocalDirectory(asset: Asset) {
        val localFile = File(applicationFilesDir, asset.pathName)

        if (localFile.exists())
            localFile.delete()
    }

    fun setTimestampForDownloadedFile(id: Long) {
        val titleName = downloadManagerWrapper.getDownloadTitle(id)
        if (titleName == "" || !titleName.contains("UserLAnd")) return
        // Title should be asset.concatenatedName
        timestampPreferences.setSavedTimestampForFileToNow(titleName)
    }

    @Throws(Exception::class)
    fun moveAssetsToCorrectLocalDirectory() {
        downloadDirectory.walkBottomUp()
                .filter { it.name.contains("UserLAnd-") }
                .forEach {
                    val delimitedContents = it.name.split("-", limit = 3)
                    if (delimitedContents.size != 3) return@forEach
                    val (_, directory, filename) = delimitedContents
                    val containingDirectory = File("${applicationFilesDir.path}/$directory")
                    val targetDestination = File("${containingDirectory.path}/$filename")
                    it.copyTo(targetDestination, overwrite = true)
                    makePermissionsUsable(containingDirectory.path, filename)
                    it.delete()
                }
    }
}
