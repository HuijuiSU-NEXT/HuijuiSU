package org.bakasu.bakasu.data.module

import com.topjohnwu.superuser.io.SuFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.bakasu.bakasu.data.network.NetworkRequestRepository
import org.bakasu.bakasu.data.network.NetworkStatusRepository
import org.bakasu.bakasu.domain.model.CatalogAuthor
import org.bakasu.bakasu.domain.model.CatalogModule
import org.bakasu.bakasu.domain.model.ModuleCatalogFailure
import org.bakasu.bakasu.domain.model.ModuleCatalogResult
import org.bakasu.bakasu.domain.model.ModuleRelease
import org.bakasu.bakasu.domain.model.ModuleReleaseAsset
import org.json.JSONArray
import org.json.JSONObject

// For modules repo
class ModuleCatalogRepository(
    private val networkStatusRepository: NetworkStatusRepository,
    private val networkRequestRepository: NetworkRequestRepository,
) {
    private companion object {
        const val MODULES_URL = "https://hhcdxii-ship-it.github.io/Huijui_Module_Repo/modules.json"
    }

    private val refreshMutex = Mutex()
    private val mutableModules = MutableStateFlow<List<CatalogModule>>(emptyList())
    private val mutableRefreshing = MutableStateFlow(false)
    private val mutableOffline = MutableStateFlow(false)

    val modules: StateFlow<List<CatalogModule>> = mutableModules.asStateFlow()
    val refreshing: StateFlow<Boolean> = mutableRefreshing.asStateFlow()
    val offline: StateFlow<Boolean> = mutableOffline.asStateFlow()

    suspend fun refresh(): ModuleCatalogResult<List<CatalogModule>> = refreshMutex.withLock {
        if (!networkStatusRepository.isAvailable()) {
            mutableOffline.value = true
            return@withLock ModuleCatalogResult.Failure(ModuleCatalogFailure.Offline)
        }
        mutableOffline.value = false
        mutableRefreshing.value = true
        try {
            withContext(Dispatchers.IO) {
                runCatching { fetchModules() }
                    .fold(
                        onSuccess = {
                            mutableModules.value = it
                            ModuleCatalogResult.Success(it)
                        },
                        onFailure = {
                            ModuleCatalogResult.Failure(
                                ModuleCatalogFailure.Network(it.message.orEmpty()),
                            )
                        },
                    )
            }
        } finally {
            mutableRefreshing.value = false
        }
    }

    suspend fun get(moduleId: String): ModuleCatalogResult<CatalogModule> {
        mutableModules.value.firstOrNull { it.moduleId == moduleId }?.let {
            return ModuleCatalogResult.Success(it)
        }
        return when (val refreshed = refresh()) {
            is ModuleCatalogResult.Failure -> refreshed

            is ModuleCatalogResult.Success -> refreshed.value.firstOrNull { it.moduleId == moduleId }
                ?.let { ModuleCatalogResult.Success(it) }
                ?: ModuleCatalogResult.Failure(ModuleCatalogFailure.NotFound)
        }
    }

    private suspend fun fetchModules(): List<CatalogModule> {
        val body = networkRequestRepository.fetch(MODULES_URL).getOrThrow()
        val json = JSONArray(body)
        return coroutineScope {
            (0 until json.length()).map { index ->
                async(Dispatchers.IO) { json.optJSONObject(index)?.let { parseModule(it) } }
            }.awaitAll().filterNotNull()
        }
    }

    private fun parseModule(item: JSONObject): CatalogModule? {
        // 适配咱们爬虫的 "id" 字段
        val moduleId = item.optString("id").takeIf(String::isNotEmpty) ?: return null
        val authorStr = item.optString("author")
        val authorList = if (authorStr.isNotEmpty()) {
            listOf(CatalogAuthor(authorStr, ""))
        } else emptyList()
        
        val versionName = item.optString("version")
        val versionCode = item.opt("versionCode").toIntCompat()
        val zipUrl = item.optString("zipUrl")
        val size = item.optLong("size")
        val downloadCount = item.opt("downloadCount").toIntCompat()
        
        // 构建咱们的 Asset
        val latestAsset = if (zipUrl.isNotEmpty()) {
            ModuleReleaseAsset(
                name = "${moduleId}-${versionName}.zip",
                downloadUrl = zipUrl,
                size = size,
                downloadCount = downloadCount
            )
        } else null

        // 把单个版本伪装成 releases 列表，防止 UI 报错
        val releases = if (latestAsset != null) {
            listOf(
                ModuleRelease(
                    name = versionName,
                    tagName = versionName,
                    publishedAt = item.optString("updatedAt"),
                    descriptionHTML = "",
                    assets = listOf(latestAsset)
                )
            )
        } else emptyList()

        return CatalogModule(
            moduleId = moduleId,
            moduleName = item.optString("name"),
            authors = authorStr,
            authorList = authorList,
            summary = item.optString("description"),
            metamodule = false,
            stargazerCount = item.optInt("stars"),
            updatedAt = item.optString("updatedAt"),
            createdAt = item.optString("updatedAt"),
            latestRelease = versionName,
            latestReleaseTime = item.optString("updatedAt"),
            latestVersionCode = versionCode,
            latestAsset = latestAsset,
            installed = SuFile.open("/data/adb/modules/$moduleId/module.prop").exists(),
            readme = "", // 没有单独详情了，置空
            sourceUrl = "",
            releases = releases,
        )
    }



    private fun stripTicks(value: String): String = value.trim().let {
        if (it.startsWith('`') && it.endsWith('`') && it.length >= 2) {
            it.substring(1, it.length - 1)
        } else {
            it
        }
    }

    private fun Any?.toIntCompat(): Int = when (this) {
        is Number -> toInt()
        is String -> toIntOrNull() ?: 0
        else -> 0
    }


}
