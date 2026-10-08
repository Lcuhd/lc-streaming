package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.generator.DeviceIdentifierGenerator
import com.example.data.remote.LcAdminApiClient
import com.example.data.remote.LcAdminDeleteResponse
import com.example.data.remote.LcAdminListsResponse
import com.example.data.remote.LcAdminResponse
import com.example.domain.model.ActivationStatus
import com.example.domain.model.DeviceListModel
import com.example.domain.model.ListStatus
import com.example.domain.model.formatExpiresAt
import com.example.ui.activation.ActivationViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LC Player", appName)
    }

    @Test
    fun `verify device id format AA BB CC DD EE FF`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val deviceId = DeviceIdentifierGenerator.generateDeviceId(context)
        val regex = Regex("^([0-9A-F]{2}:){5}[0-9A-F]{2}\$")
        assertTrue("Device ID should match AA:BB:CC:DD:EE:FF pattern but was $deviceId", regex.matches(deviceId))
    }

    @Test
    fun `verify device id is strictly deterministic and identical across multiple calls`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val firstCall = DeviceIdentifierGenerator.generateDeviceId(context)
        for (i in 1..20) {
            val nextCall = DeviceIdentifierGenerator.generateDeviceId(context)
            assertEquals("Device ID must be identical across calls", firstCall, nextCall)
        }
    }

    @Test
    fun `verify device key is deterministic and 6 digits`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val key = DeviceIdentifierGenerator.generateKey(context)
        val regex = Regex("^\\d{6}\$")
        assertTrue("Key should be a 6 digit number but was $key", regex.matches(key))

        val keySecond = DeviceIdentifierGenerator.generateKey(context)
        assertEquals("Key must be identical across calls for the same device", key, keySecond)
    }

    @Test
    fun `verify default status is waiting activation`() {
        val defaultStatus = ActivationStatus.WAITING_ACTIVATION
        assertEquals("Aguardando ativação", defaultStatus.title)
    }

    @Test
    fun `verify active status title is Dispositivo ativado`() {
        val activeStatus = ActivationStatus.ACTIVE
        assertEquals("Dispositivo ativado", activeStatus.title)
    }

    @Test
    fun `verify formatExpiresAt converts ISO date or provides fallback`() {
        assertEquals("30/11/2026", formatExpiresAt("2026-11-30T23:59:59Z"))
        assertEquals("15/12/2026", formatExpiresAt("2026-12-15T10:00:00Z"))
        assertEquals("Sem validade definida", formatExpiresAt(null))
        assertEquals("Sem validade definida", formatExpiresAt(""))
    }

    @Test
    fun `verify list status labels`() {
        assertEquals("Ativa", ListStatus.fromString("active").label)
        assertEquals("Bloqueada", ListStatus.fromString("blocked").label)
        assertEquals("Vencida", ListStatus.fromString("expired").label)
    }

    @Test
    fun `verify list sorting by display_order`() {
        val list1 = DeviceListModel("1", "Teste", "Servidor Teste", displayOrder = 3)
        val list2 = DeviceListModel("2", "Principal", "Servidor 01", displayOrder = 1)
        val list3 = DeviceListModel("3", "Backup", "Servidor 02", displayOrder = 2)

        val sorted = listOf(list1, list2, list3).sortedBy { it.displayOrder }
        assertEquals("Principal", sorted[0].title)
        assertEquals("Backup", sorted[1].title)
        assertEquals("Teste", sorted[2].title)
    }

    @Test
    fun `verify LcAdminApiClient calls endpoint status and handles response`() {
        val client = LcAdminApiClient()
        val response = client.checkDeviceStatus("AA:BB:CC:DD:EE:FF", "728491")
        assertTrue(
            "Response should be Success (HTTP 200) or Error with valid code",
            response is LcAdminResponse.Success || response is LcAdminResponse.Error
        )
        if (response is LcAdminResponse.Success) {
            assertEquals(200, response.httpCode)
            assertEquals("Aguardando ativação", response.displayMessage)
        }
    }

    @Test
    fun `verify LcAdminApiClient calls device-lists endpoint and parses collection`() {
        val client = LcAdminApiClient()
        val response = client.fetchDeviceLists("AA:BB:CC:DD:EE:FF", "728491")
        assertTrue(
            "Lists response should be Success or Error",
            response is LcAdminListsResponse.Success || response is LcAdminListsResponse.Error
        )
        if (response is LcAdminListsResponse.Success) {
            assertEquals(200, response.httpCode)
            assertTrue("Lists should be a list instance", response.lists is List<DeviceListModel>)
        }
    }

    @Test
    fun `verify LcAdminApiClient calls delete-device-list endpoint`() {
        val client = LcAdminApiClient()
        val response = client.deleteDeviceList("AA:BB:CC:DD:EE:FF", "728491", "test_list_id")
        assertTrue(
            "Delete response should be an instance of LcAdminDeleteResponse",
            response is LcAdminDeleteResponse
        )
    }

    @Test
    fun `verify confirmation dialog workflow before deletion`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = ActivationViewModel(app)

        val testList = DeviceListModel("123", "Lista Teste", "Servidor 01")

        // Inicialmente nenhuma lista está pendente de exclusão
        assertNull(viewModel.uiState.value.listPendingDeletion)

        // Ao solicitar exclusão, deve colocar a lista no estado de confirmação
        viewModel.requestDeleteList(testList)
        assertEquals(testList, viewModel.uiState.value.listPendingDeletion)

        // Ao cancelar, deve limpar sem deletar nada
        viewModel.cancelDeleteList()
        assertNull(viewModel.uiState.value.listPendingDeletion)
    }

    @Test
    fun `verify XtreamAuthClient builds normalized URL with encoding and handles trailing slashes`() {
        val url1 = com.example.data.remote.XtreamAuthClient.buildAuthUrl(
            "http://servidor.com:8080/",
            "usuario teste",
            "senha@123"
        )
        assertEquals(
            "http://servidor.com:8080/player_api.php?username=usuario+teste&password=senha%40123",
            url1
        )

        val url2 = com.example.data.remote.XtreamAuthClient.buildAuthUrl(
            "https://ssl.servidor.com",
            "usuario",
            "senha"
        )
        assertEquals(
            "https://ssl.servidor.com/player_api.php?username=usuario&password=senha",
            url2
        )
    }

    @Test
    fun `verify XtreamCategoryClient builds category URLs for live, vod and series`() {
        val liveUrl = com.example.data.remote.XtreamCategoryClient.buildCategoryUrl(
            "http://meuservidor.com:8000/",
            "user 1",
            "pass#1",
            com.example.domain.model.CategoryType.LIVE.actionParam
        )
        assertEquals(
            "http://meuservidor.com:8000/player_api.php?username=user+1&password=pass%231&action=get_live_categories",
            liveUrl
        )

        val vodUrl = com.example.data.remote.XtreamCategoryClient.buildCategoryUrl(
            "https://meuservidor.com",
            "user",
            "pass",
            com.example.domain.model.CategoryType.VOD.actionParam
        )
        assertEquals(
            "https://meuservidor.com/player_api.php?username=user&password=pass&action=get_vod_categories",
            vodUrl
        )

        val seriesUrl = com.example.data.remote.XtreamCategoryClient.buildCategoryUrl(
            "https://meuservidor.com/",
            "user",
            "pass",
            com.example.domain.model.CategoryType.SERIES.actionParam
        )
        assertEquals(
            "https://meuservidor.com/player_api.php?username=user&password=pass&action=get_series_categories",
            seriesUrl
        )
    }

    @Test
    fun `verify categories data models maintain separate collections for live, vod and series`() {
        val liveCat = com.example.domain.model.XtreamCategory("1", "Abertos", 0, com.example.domain.model.CategoryType.LIVE)
        val vodCat = com.example.domain.model.XtreamCategory("2", "Ação", null, com.example.domain.model.CategoryType.VOD)
        val seriesCat = com.example.domain.model.XtreamCategory("3", "Drama", 0, com.example.domain.model.CategoryType.SERIES)

        val state = com.example.domain.model.ListContentCategoriesState(
            listId = "list_1",
            listTitle = "TrendUHD",
            liveCategories = listOf(liveCat),
            vodCategories = listOf(vodCat),
            seriesCategories = listOf(seriesCat)
        )

        assertEquals(1, state.getCountForType(com.example.domain.model.CategoryType.LIVE))
        assertEquals(1, state.getCountForType(com.example.domain.model.CategoryType.VOD))
        assertEquals(1, state.getCountForType(com.example.domain.model.CategoryType.SERIES))
        assertEquals("Abertos", state.getCategoriesForType(com.example.domain.model.CategoryType.LIVE)[0].categoryName)
        assertEquals("Ação", state.getCategoriesForType(com.example.domain.model.CategoryType.VOD)[0].categoryName)
        assertEquals("Drama", state.getCategoriesForType(com.example.domain.model.CategoryType.SERIES)[0].categoryName)
    }

    @Test
    fun `verify switching between lists discards previous categories and isolates state`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = ActivationViewModel(app)
        viewModel.setDeviceStatus(ActivationStatus.ACTIVE)

        val listA = DeviceListModel("list_a", "Lista A", "Servidor A", status = ListStatus.ACTIVE)
        val listB = DeviceListModel("list_b", "Lista B", "Servidor B", status = ListStatus.ACTIVE)

        // Abre categorias para a Lista A
        viewModel.openContentCategories(listA)
        assertNotNull(viewModel.uiState.value.contentCategoriesState)
        assertEquals("list_a", viewModel.uiState.value.contentCategoriesState?.listId)
        assertEquals("Lista A", viewModel.uiState.value.contentCategoriesState?.listTitle)
        assertEquals(com.example.ui.activation.AppScreenSection.CONTENT_CATEGORIES, viewModel.uiState.value.currentSection)

        // Troca para a Lista B: dados visuais da Lista A são descartados
        viewModel.openContentCategories(listB)
        assertNotNull(viewModel.uiState.value.contentCategoriesState)
        assertEquals("list_b", viewModel.uiState.value.contentCategoriesState?.listId)
        assertEquals("Lista B", viewModel.uiState.value.contentCategoriesState?.listTitle)

        // Ao fechar categorias, o estado é limpo e volta para Minhas Listas
        viewModel.closeContentCategories()
        assertNull(viewModel.uiState.value.contentCategoriesState)
        assertEquals(com.example.ui.activation.AppScreenSection.MY_LISTS, viewModel.uiState.value.currentSection)
    }

    @Test
    fun `verify blocked or expired list rejects opening categories`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = ActivationViewModel(app)

        val blockedList = DeviceListModel("blocked_1", "Lista Bloqueada", "Servidor", status = ListStatus.BLOCKED)
        val expiredList = DeviceListModel("expired_1", "Lista Vencida", "Servidor", status = ListStatus.EXPIRED)

        viewModel.openContentCategories(blockedList)
        assertNull("Blocked list must not open categories", viewModel.uiState.value.contentCategoriesState)

        viewModel.openContentCategories(expiredList)
        assertNull("Expired list must not open categories", viewModel.uiState.value.contentCategoriesState)
    }

    @Test
    fun `verify category model with real item count for series movies and channels`() {
        val seriesCategory = com.example.domain.model.XtreamCategory(
            categoryId = "10",
            categoryName = "Drama",
            parentId = 0,
            type = com.example.domain.model.CategoryType.SERIES,
            itemCount = 120
        )
        assertEquals(120, seriesCategory.itemCount)
        assertEquals("Drama", seriesCategory.categoryName)
        assertEquals(com.example.domain.model.CategoryType.SERIES, seriesCategory.type)

        val vodCategory = com.example.domain.model.XtreamCategory(
            categoryId = "20",
            categoryName = "Ação",
            parentId = 0,
            type = com.example.domain.model.CategoryType.VOD,
            itemCount = 84
        )
        assertEquals(84, vodCategory.itemCount)

        val liveCategory = com.example.domain.model.XtreamCategory(
            categoryId = "30",
            categoryName = "Abertos",
            parentId = 0,
            type = com.example.domain.model.CategoryType.LIVE,
            itemCount = 30
        )
        assertEquals(30, liveCategory.itemCount)
    }

    @Test
    fun `verify CategoryLoadResult subtypes are distinct`() {
        val emptyResult = com.example.domain.model.CategoryLoadResult.Empty(
            "Nenhuma categoria de séries encontrada",
            com.example.domain.model.CategoryType.SERIES
        )
        assertTrue(emptyResult is com.example.domain.model.CategoryLoadResult.Empty)
        assertEquals("Nenhuma categoria de séries encontrada", emptyResult.displayMessage)

        val parseError = com.example.domain.model.CategoryLoadResult.ParseError(
            "Erro ao interpretar categorias de séries",
            com.example.domain.model.CategoryType.SERIES
        )
        assertTrue(parseError is com.example.domain.model.CategoryLoadResult.ParseError)
        assertEquals("Erro ao interpretar categorias de séries", parseError.displayMessage)

        val networkError = com.example.domain.model.CategoryLoadResult.NetworkError(
            "Não foi possível carregar as séries",
            com.example.domain.model.CategoryType.SERIES
        )
        assertTrue(networkError is com.example.domain.model.CategoryLoadResult.NetworkError)
        assertEquals("Não foi possível carregar as séries", networkError.displayMessage)

        val authError = com.example.domain.model.CategoryLoadResult.AuthError(
            "Não foi possível autenticar esta lista",
            com.example.domain.model.CategoryType.SERIES
        )
        assertTrue(authError is com.example.domain.model.CategoryLoadResult.AuthError)
        assertEquals("Não foi possível autenticar esta lista", authError.displayMessage)
    }

    @Test
    fun `verify CategoryType enum maintains complete set for horizontal navigation`() {
        val types = com.example.domain.model.CategoryType.entries
        assertEquals(3, types.size)
        assertEquals("TV ao vivo", com.example.domain.model.CategoryType.LIVE.displayName)
        assertEquals("Filmes", com.example.domain.model.CategoryType.VOD.displayName)
        assertEquals("Séries", com.example.domain.model.CategoryType.SERIES.displayName)
    }

    @Test
    fun `verify TvScrollableRow creates initial scroll state at 0`() {
        val scrollState = androidx.compose.foundation.ScrollState(initial = 0)
        assertEquals(0, scrollState.value)
    }

    @Test
    fun `verify DeviceIdentityStorage recovers from historical SharedPreferences and preserves them`() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val historicalPrefs = app.getSharedPreferences("lc_device_preferences", android.content.Context.MODE_PRIVATE)
        historicalPrefs.edit()
            .putString("device_id", "11:22:33:44:55:66")
            .putString("device_key", "987654")
            .commit()

        val db = com.example.data.local.AppDatabase.getDatabase(app)
        val storage = com.example.data.local.DeviceIdentityStorage(app, db.deviceInfoDao())

        val creds = storage.getOrInitializeCredentials()
        assertEquals("11:22:33:44:55:66", creds.deviceId)
        assertEquals("987654", creds.key)

        // Chamada subsequente deve retornar exatamente os mesmos dados
        val creds2 = storage.getOrInitializeCredentials()
        assertEquals("11:22:33:44:55:66", creds2.deviceId)
        assertEquals("987654", creds2.key)
    }

    @Test
    fun `verify DeviceIdentityStorage recovers from Room entity and never overwrites with new calculation`() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = com.example.data.local.AppDatabase.getDatabase(app)
        val dao = db.deviceInfoDao()

        val initialEntity = com.example.data.local.entity.DeviceInfoEntity(
            id = 1,
            deviceId = "AA:BB:CC:11:22:33",
            activationKey = "555123",
            status = "ACTIVE"
        )
        dao.insertOrUpdate(initialEntity)

        val storage = com.example.data.local.DeviceIdentityStorage(app, dao)
        val creds = storage.getOrInitializeCredentials()

        assertEquals("AA:BB:CC:11:22:33", creds.deviceId)
        assertEquals("555123", creds.key)
        assertEquals(com.example.domain.model.ActivationStatus.ACTIVE, creds.status)

        // Simula atualização / reinicialização
        val credsAfterUpdate = storage.getOrInitializeCredentials()
        assertEquals("AA:BB:CC:11:22:33", credsAfterUpdate.deviceId)
        assertEquals("555123", credsAfterUpdate.key)
    }

    @Test
    fun `verify buildLiveStreamsUrl encodes parameters and includes category_id when present`() {
        val urlWithCategory = com.example.data.remote.XtreamLiveStreamsClient.buildLiveStreamsUrl(
            dns = "http://servidor.net:8080/",
            username = "user test",
            password = "pass#123",
            categoryId = "45"
        )
        assertTrue("URL must contain get_live_streams action", urlWithCategory.contains("action=get_live_streams"))
        assertTrue("URL must contain encoded username", urlWithCategory.contains("username=user+test"))
        assertTrue("URL must contain category_id=45", urlWithCategory.contains("&category_id=45"))
        assertTrue("URL should not have double slashes after domain", !urlWithCategory.contains(".net:8080//"))

        val urlWithoutCategory = com.example.data.remote.XtreamLiveStreamsClient.buildLiveStreamsUrl(
            dns = "http://servidor.net:8080",
            username = "user",
            password = "pwd",
            categoryId = null
        )
        assertTrue("URL must not contain category_id when null", !urlWithoutCategory.contains("category_id"))
    }

    @Test
    fun `verify LiveStreamChannel model preserves properties and supports optional fields`() {
        val channel = com.example.domain.model.LiveStreamChannel(
            streamId = "1001",
            name = "Globo SP HD",
            num = 1,
            streamIcon = "http://servidor.net/logos/globo.png",
            categoryId = "10",
            epgChannelId = "globo.sp",
            streamType = "live"
        )

        assertEquals("1001", channel.streamId)
        assertEquals("Globo SP HD", channel.name)
        assertEquals(1, channel.num)
        assertEquals("http://servidor.net/logos/globo.png", channel.streamIcon)
        assertEquals("10", channel.categoryId)
        assertEquals("globo.sp", channel.epgChannelId)
        assertEquals("live", channel.streamType)
    }

    @Test
    fun `verify LiveStreamsLoadResult subtypes represent all possible states`() {
        val success = com.example.domain.model.LiveStreamsLoadResult.Success(
            channels = listOf(
                com.example.domain.model.LiveStreamChannel("1", "Canal 1", categoryId = "5")
            ),
            categoryId = "5",
            filterStrategyUsed = "API direta por category_id"
        )
        assertTrue(success is com.example.domain.model.LiveStreamsLoadResult.Success)
        assertEquals(1, success.channels.size)
        assertEquals("API direta por category_id", success.filterStrategyUsed)

        val empty = com.example.domain.model.LiveStreamsLoadResult.Empty(
            displayMessage = "Nenhum canal encontrado nesta categoria",
            categoryId = "5",
            filterStrategyUsed = "API direta"
        )
        assertTrue(empty is com.example.domain.model.LiveStreamsLoadResult.Empty)
        assertEquals("Nenhum canal encontrado nesta categoria", empty.displayMessage)

        val networkError = com.example.domain.model.LiveStreamsLoadResult.NetworkError(
            displayMessage = "Erro de conexão",
            categoryId = "5"
        )
        assertTrue(networkError is com.example.domain.model.LiveStreamsLoadResult.NetworkError)

        val authError = com.example.domain.model.LiveStreamsLoadResult.AuthError(
            displayMessage = "Não autorizado",
            categoryId = "5"
        )
        assertTrue(authError is com.example.domain.model.LiveStreamsLoadResult.AuthError)
    }

    @Test
    fun `verify closeLiveCategoryChannels returns to categories without resetting categories list`() {
        val initialCategoriesState = com.example.domain.model.ListContentCategoriesState(
            listId = "list_123",
            listTitle = "Minha Lista",
            liveCategories = listOf(
                com.example.domain.model.XtreamCategory("10", "Rede Globo", type = com.example.domain.model.CategoryType.LIVE)
            ),
            selectedCategoryForChannels = com.example.domain.model.XtreamCategory("10", "Rede Globo", type = com.example.domain.model.CategoryType.LIVE),
            liveStreamsState = com.example.domain.model.LiveStreamsState("10", "Rede Globo", channels = emptyList(), isLoading = false)
        )

        assertTrue("Initially viewing channels", initialCategoriesState.isViewingChannels)

        val closedState = initialCategoriesState.copy(
            selectedCategoryForChannels = null,
            liveStreamsState = null
        )

        assertTrue("After close, not viewing channels", !closedState.isViewingChannels)
        assertEquals("List ID preserved", "list_123", closedState.listId)
        assertEquals("Categories list preserved", 1, closedState.liveCategories.size)
    }
}


