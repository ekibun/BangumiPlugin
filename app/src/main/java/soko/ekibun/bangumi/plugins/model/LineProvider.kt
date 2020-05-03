package soko.ekibun.bangumi.plugins.model

import androidx.room.Room
import io.reactivex.schedulers.Schedulers
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.model.provider.ProviderDatabase
import soko.ekibun.bangumi.plugins.model.provider.ProviderInfo

object LineProvider {
    private val providerDao by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Room.databaseBuilder(App.app.plugin, ProviderDatabase::class.java, "provider.sqlite").build().providerDao()
    }

    fun getProviderList(type: String): List<ProviderInfo> {
        return providerDao.get(type).subscribeOn(Schedulers.io()).blockingGet()
    }

    fun getProvider(type: String, site: String): ProviderInfo? {
        return providerDao.get(type, site).subscribeOn(Schedulers.io()).blockingGet()
    }

    fun addProvider(provider: ProviderInfo) {
        providerDao.insert(provider).subscribeOn(Schedulers.io()).blockingAwait()
    }

    fun removeProvider(provider: ProviderInfo) {
        providerDao.delete(provider).subscribeOn(Schedulers.io()).blockingAwait()
    }
}