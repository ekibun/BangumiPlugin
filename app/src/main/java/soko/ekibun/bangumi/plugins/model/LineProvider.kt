package soko.ekibun.bangumi.plugins.model

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.model.provider.ProviderDatabase
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo

object LineProvider {
    private val providerDao by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Room.databaseBuilder(App.app.plugin, ProviderDatabase::class.java, "provider.sqlite").build().providerDao()
    }

    fun getProviderList(type: String): List<ProviderInfo> = runBlocking {
        providerDao.get(type)
    }

    fun getProvider(type: String, site: String): ProviderInfo? = runBlocking {
        providerDao.get(type, site)
    }

    fun addProvider(provider: ProviderInfo) = runBlocking {
        providerDao.insert(provider)
    }

    fun removeProvider(provider: ProviderInfo) = runBlocking {
        providerDao.delete(provider)
    }
}