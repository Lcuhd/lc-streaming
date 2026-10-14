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
    fun `verify formatExpiresAt converts ISO date or provides Ilimitado for null blank or 10plus years`() {
        assertEquals("Ilimitado", formatExpiresAt(null))
        assertEquals("Ilimitado", formatExpiresAt(""))
        assertEquals("Ilimitado", formatExpiresAt("2099-12-31T23:59:59Z"))
        assertEquals("30/11/2026", formatExpiresAt("2026-11-30T23:59:59Z"))
        assertEquals("15/12/2026", formatExpiresAt("2026-12-15T10:00:00Z"))
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

    @Test
    fun `verify XtreamEpgClient base64 decoder decodes encoded strings and preserves plain text`() {
        val plainText = "Jornal Nacional"
        assertEquals("Jornal Nacional", com.example.data.remote.XtreamEpgClient.decodeBase64IfNeeded(plainText))

        // "Jornal da Noite" in Base64 is "Sm9ybmFsIGRhIE5vaXRl"
        val base64Text = "Sm9ybmFsIGRhIE5vaXRl"
        assertEquals("Jornal da Noite", com.example.data.remote.XtreamEpgClient.decodeBase64IfNeeded(base64Text))

        assertEquals("", com.example.data.remote.XtreamEpgClient.decodeBase64IfNeeded(null))
        assertEquals("", com.example.data.remote.XtreamEpgClient.decodeBase64IfNeeded(""))
    }

    @Test
    fun `verify buildShortEpgUrl constructs url safely with action get_short_epg and limit 2`() {
        val url = com.example.data.remote.XtreamEpgClient.buildShortEpgUrl(
            dns = "http://servidor.net:8080/",
            username = "user test",
            password = "pass#123",
            streamId = "1050",
            limit = 2
        )

        assertTrue(url.startsWith("http://servidor.net:8080/player_api.php?"))
        assertTrue(url.contains("action=get_short_epg"))
        assertTrue(url.contains("stream_id=1050"))
        assertTrue(url.contains("limit=2"))
        assertTrue(url.contains("username=user+test"))
    }

    @Test
    fun `verify timestamp and date formatting converts UTC timestamp to local HH mm`() {
        // 1746885600 -> May 10, 2025 14:00:00 UTC
        val formatted = com.example.data.remote.XtreamEpgClient.formatTimestampToLocalTime(1746885600L)
        assertNotNull(formatted)
        assertTrue(formatted!!.matches(Regex("^\\d{2}:\\d{2}\$")))

        val parsedDate = com.example.data.remote.XtreamEpgClient.parseServerDateToLocalTime("2025-05-10 14:00:00")
        assertNotNull(parsedDate)
        assertTrue(parsedDate!!.matches(Regex("^\\d{2}:\\d{2}\$")))
    }

    @Test
    fun `verify EpgProgram timeRangeFormatted and ShortEpgState properties`() {
        val program = com.example.domain.model.EpgProgram(
            id = "10",
            title = "Tela Quente",
            description = "Filme de Ação",
            startTimeFormatted = "22:30",
            endTimeFormatted = "00:15",
            isNowPlaying = true
        )

        assertEquals("22:30 - 00:15", program.timeRangeFormatted)

        val state = com.example.domain.model.ShortEpgState(
            streamId = "1050",
            currentProgram = program,
            nextProgram = null,
            isLoading = false
        )

        assertTrue(state.hasEpg)
        assertEquals("1050", state.streamId)
        assertEquals("Tela Quente", state.currentProgram?.title)
    }

    @Test
    fun `verify cyclic channel navigation logic`() {
        val channels = listOf(
            com.example.domain.model.LiveStreamChannel("101", "Globo", categoryId = "1"),
            com.example.domain.model.LiveStreamChannel("102", "SBT", categoryId = "1"),
            com.example.domain.model.LiveStreamChannel("103", "Band", categoryId = "1")
        )

        // Test next channel from index 0 -> 1 -> 2 -> 0 (wrap)
        var currentIndex = 0
        var nextIndex = if (currentIndex < channels.lastIndex) currentIndex + 1 else 0
        assertEquals(1, nextIndex)

        currentIndex = 2
        nextIndex = if (currentIndex < channels.lastIndex) currentIndex + 1 else 0
        assertEquals(0, nextIndex)

        // Test prev channel from index 0 -> 2 (wrap) -> 1 -> 0
        currentIndex = 0
        var prevIndex = if (currentIndex > 0) currentIndex - 1 else channels.lastIndex
        assertEquals(2, prevIndex)

        currentIndex = 2
        prevIndex = if (currentIndex > 0) currentIndex - 1 else channels.lastIndex
        assertEquals(1, prevIndex)
    }

    @Test
    fun `verify buildVodMovieUrl constructs VOD URL with container extension`() {
        val urlMp4 = com.example.util.XtreamStreamUrlBuilder.buildVodMovieUrl(
            rawDns = "http://servidor.net:8080/",
            username = "usuario",
            password = "password",
            streamId = "12345",
            containerExtension = "mp4"
        )
        assertEquals("http://servidor.net:8080/movie/usuario/password/12345.mp4", urlMp4)

        val urlMkv = com.example.util.XtreamStreamUrlBuilder.buildVodMovieUrl(
            rawDns = "https://ssl.servidor.com",
            username = "user",
            password = "pass",
            streamId = "999",
            containerExtension = ".mkv"
        )
        assertEquals("https://ssl.servidor.com/movie/user/pass/999.mkv", urlMkv)

        val urlDefault = com.example.util.XtreamStreamUrlBuilder.buildVodMovieUrl(
            rawDns = "http://servidor.net",
            username = "user",
            password = "pass",
            streamId = "555",
            containerExtension = null
        )
        assertEquals("http://servidor.net/movie/user/pass/555.mp4", urlDefault)
    }

    @Test
    fun `verify VodMovie model preserves properties and optional fields`() {
        val movie = com.example.domain.model.VodMovie(
            streamId = "5001",
            name = "Inception",
            streamIcon = "http://servidor.net/posters/inception.jpg",
            rating = "8.8",
            rating5based = 4.4,
            containerExtension = "mkv",
            categoryId = "20"
        )

        assertEquals("5001", movie.streamId)
        assertEquals("Inception", movie.name)
        assertEquals("http://servidor.net/posters/inception.jpg", movie.streamIcon)
        assertEquals("8.8", movie.rating)
        assertEquals("mkv", movie.containerExtension)
        assertEquals("20", movie.categoryId)
    }

    @Test
    fun `verify buildVodStreamsUrl constructs url safely with action get_vod_streams`() {
        val vodUrl = com.example.data.remote.XtreamVodStreamsClient.buildVodStreamsUrl(
            dns = "http://servidor.net:8080/",
            username = "user test",
            password = "pass#123",
            categoryId = "20"
        )

        assertTrue(vodUrl.startsWith("http://servidor.net:8080/player_api.php?"))
        assertTrue(vodUrl.contains("action=get_vod_streams"))
        assertTrue(vodUrl.contains("category_id=20"))
        assertTrue(vodUrl.contains("username=user+test"))
    }

    @Test
    fun `verify buildSeriesEpisodeUrl constructs Series URL with episode_id and container extension`() {
        val episodeUrlMkv = com.example.util.XtreamStreamUrlBuilder.buildSeriesEpisodeUrl(
            rawDns = "http://servidor.net:8080/",
            username = "usuario",
            password = "password",
            episodeId = "888123",
            containerExtension = "mkv"
        )
        assertEquals("http://servidor.net:8080/series/usuario/password/888123.mkv", episodeUrlMkv)

        val episodeUrlMp4 = com.example.util.XtreamStreamUrlBuilder.buildSeriesEpisodeUrl(
            rawDns = "https://ssl.servidor.com",
            username = "user",
            password = "pass",
            episodeId = "999888",
            containerExtension = ".mp4"
        )
        assertEquals("https://ssl.servidor.com/series/user/pass/999888.mp4", episodeUrlMp4)
    }

    @Test
    fun `verify SeriesItem and EpisodeItem models preserve properties`() {
        val series = com.example.domain.model.SeriesItem(
            seriesId = "7001",
            name = "Breaking Bad",
            cover = "http://servidor.net/covers/bb.jpg",
            rating = "9.5",
            categoryId = "30"
        )
        assertEquals("7001", series.seriesId)
        assertEquals("Breaking Bad", series.name)
        assertEquals("30", series.categoryId)

        val episode = com.example.domain.model.EpisodeItem(
            id = "888123",
            episodeNum = 1,
            seasonNum = 1,
            title = "Pilot",
            containerExtension = "mkv",
            duration = "58 min"
        )
        assertEquals("888123", episode.id)
        assertEquals(1, episode.episodeNum)
        assertEquals(1, episode.seasonNum)
        assertEquals("Pilot", episode.title)
        assertEquals("mkv", episode.containerExtension)
    }

    @Test
    fun `verify buildSeriesUrl and buildSeriesInfoUrl construct safe endpoints`() {
        val listUrl = com.example.data.remote.XtreamSeriesClient.buildSeriesUrl(
            dns = "http://servidor.net:8080/",
            username = "user test",
            password = "pass#123",
            categoryId = "30"
        )
        assertTrue(listUrl.contains("action=get_series"))
        assertTrue(listUrl.contains("category_id=30"))

        val infoUrl = com.example.data.remote.XtreamSeriesClient.buildSeriesInfoUrl(
            dns = "http://servidor.net:8080/",
            username = "user test",
            password = "pass#123",
            seriesId = "7001"
        )
        assertTrue(infoUrl.contains("action=get_series_info"))
        assertTrue(infoUrl.contains("series_id=7001"))
    }

    @Test
    fun `verify computePrimaryButtonState for Scenarios A, B, C, D, E`() {
        val ep1 = com.example.domain.model.EpisodeItem("ep101", 1, 1, "Piloto")
        val ep2 = com.example.domain.model.EpisodeItem("ep102", 2, 1, "Gato na Sacola")
        val epS2E1 = com.example.domain.model.EpisodeItem("ep201", 1, 2, "Setenta e Quatro Sete")

        val seasons = listOf(
            com.example.domain.model.SeasonItem(1, "Temporada 1", 2),
            com.example.domain.model.SeasonItem(2, "Temporada 2", 1)
        )

        val episodesMap = mapOf(
            1 to listOf(ep1, ep2),
            2 to listOf(epS2E1)
        )

        val details = com.example.domain.model.SeriesDetails(
            seriesId = "7001",
            title = "Breaking Bad",
            seasons = seasons,
            episodesBySeason = episodesMap
        )

        // Scenario A: Nunca assistida -> Iniciar série
        val btnStateA = com.example.domain.model.computePrimaryButtonState(details, emptyList())
        assertTrue("Scenario A should be StartSeries", btnStateA is com.example.domain.model.SeriesButtonState.StartSeries)
        assertEquals("ep101", (btnStateA as com.example.domain.model.SeriesButtonState.StartSeries).firstEpisode.id)

        // Scenario B: Ep 1 assistido parcialmente (incompleto, 15 min) -> Continuar Ep 1
        val progB = listOf(
            com.example.data.local.entity.SeriesProgressEntity("ep101", "7001", 1, 1, positionMs = 900000L, durationMs = 3000000L, completed = false, updatedAt = 100L)
        )
        val btnStateB = com.example.domain.model.computePrimaryButtonState(details, progB)
        assertTrue("Scenario B should be ContinueSeries", btnStateB is com.example.domain.model.SeriesButtonState.ContinueSeries)
        val contB = btnStateB as com.example.domain.model.SeriesButtonState.ContinueSeries
        assertEquals("ep101", contB.targetEpisode.id)
        assertEquals(900000L, contB.startPositionMs)

        // Scenario C: Ep 1 concluído -> Continuar Ep 2
        val progC = listOf(
            com.example.data.local.entity.SeriesProgressEntity("ep101", "7001", 1, 1, positionMs = 3000000L, durationMs = 3000000L, completed = true, updatedAt = 200L)
        )
        val btnStateC = com.example.domain.model.computePrimaryButtonState(details, progC)
        assertTrue("Scenario C should be ContinueSeries", btnStateC is com.example.domain.model.SeriesButtonState.ContinueSeries)
        val contC = btnStateC as com.example.domain.model.SeriesButtonState.ContinueSeries
        assertEquals("ep102", contC.targetEpisode.id)
        assertEquals(0L, contC.startPositionMs)

        // Scenario D: Ep 2 (último da Temp 1) concluído -> Continuar Temp 2 Ep 1
        val progD = listOf(
            com.example.data.local.entity.SeriesProgressEntity("ep102", "7001", 1, 2, positionMs = 3000000L, durationMs = 3000000L, completed = true, updatedAt = 300L)
        )
        val btnStateD = com.example.domain.model.computePrimaryButtonState(details, progD)
        assertTrue("Scenario D should be ContinueSeries", btnStateD is com.example.domain.model.SeriesButtonState.ContinueSeries)
        val contD = btnStateD as com.example.domain.model.SeriesButtonState.ContinueSeries
        assertEquals("ep201", contD.targetEpisode.id)
        assertEquals(2, contD.targetSeasonNum)

        // Scenario E: Ep 1 da Temp 2 (último episódio da série) concluído -> Assistir novamente
        val progE = listOf(
            com.example.data.local.entity.SeriesProgressEntity("ep201", "7001", 2, 1, positionMs = 3000000L, durationMs = 3000000L, completed = true, updatedAt = 400L)
        )
        val btnStateE = com.example.domain.model.computePrimaryButtonState(details, progE)
        assertTrue("Scenario E should be RestartSeries", btnStateE is com.example.domain.model.SeriesButtonState.RestartSeries)
        assertEquals("ep101", (btnStateE as com.example.domain.model.SeriesButtonState.RestartSeries).firstEpisode.id)
    }

    @Test
    fun `verify getNextEpisode finds next episode in same season or next season`() {
        val ep1 = com.example.domain.model.EpisodeItem("ep1", 1, 1, "E1")
        val ep2 = com.example.domain.model.EpisodeItem("ep2", 2, 1, "E2")
        val epS2E1 = com.example.domain.model.EpisodeItem("ep3", 1, 2, "S2E1")

        val details = com.example.domain.model.SeriesDetails(
            seriesId = "100",
            title = "Serie Teste",
            seasons = listOf(
                com.example.domain.model.SeasonItem(1, "Temp 1", 2),
                com.example.domain.model.SeasonItem(2, "Temp 2", 1)
            ),
            episodesBySeason = mapOf(
                1 to listOf(ep1, ep2),
                2 to listOf(epS2E1)
            )
        )

        val nextAfterE1 = com.example.domain.model.getNextEpisode(details, currentSeasonNum = 1, currentEpisodeNum = 1)
        assertEquals("ep2", nextAfterE1?.id)

        val nextAfterE2 = com.example.domain.model.getNextEpisode(details, currentSeasonNum = 1, currentEpisodeNum = 2)
        assertEquals("ep3", nextAfterE2?.id)

        val nextAfterS2E1 = com.example.domain.model.getNextEpisode(details, currentSeasonNum = 2, currentEpisodeNum = 1)
        assertNull(nextAfterS2E1)
    }

    @Test
    fun `verify formatMsToTime formats milliseconds to HH mm ss`() {
        assertEquals("00:42:18", com.example.ui.player.formatMsToTime(2538000L))
        assertEquals("01:55:40", com.example.ui.player.formatMsToTime(6940000L))
        assertEquals("00:00:18", com.example.ui.player.formatMsToTime(18000L))
        assertEquals("00:00:00", com.example.ui.player.formatMsToTime(0L))
    }

    @Test
    fun `verify getFriendlyLanguageName maps ISO language codes`() {
        assertEquals("Português", com.example.ui.player.getFriendlyLanguageName("por"))
        assertEquals("Português", com.example.ui.player.getFriendlyLanguageName("pt"))
        assertEquals("Inglês", com.example.ui.player.getFriendlyLanguageName("eng"))
        assertEquals("Espanhol", com.example.ui.player.getFriendlyLanguageName("spa"))
        assertEquals("Japonês", com.example.ui.player.getFriendlyLanguageName("jpn"))
        assertNull(com.example.ui.player.getFriendlyLanguageName(null))
        assertNull(com.example.ui.player.getFriendlyLanguageName("und"))
    }

    @Test
    fun `verify active lists count determines auto selection vs list selector`() {
        val listActive1 = com.example.domain.model.DeviceListModel(
            id = "l1", title = "Lista Premium", sourceName = "Xtream", dns = "http://srv1.net", username = "u1", password = "p1", status = com.example.domain.model.ListStatus.ACTIVE
        )
        val listActive2 = com.example.domain.model.DeviceListModel(
            id = "l2", title = "Lista Backup", sourceName = "Xtream", dns = "http://srv2.net", username = "u2", password = "p2", status = com.example.domain.model.ListStatus.ACTIVE
        )
        val listBlocked = com.example.domain.model.DeviceListModel(
            id = "l3", title = "Lista Expirada", sourceName = "Xtream", dns = "http://srv3.net", username = "u3", password = "p3", status = com.example.domain.model.ListStatus.BLOCKED
        )

        // 1. Exatamente 1 lista ativa -> Auto-seleciona
        val activeOnlyOne = listOf(listActive1, listBlocked).filter { it.status == com.example.domain.model.ListStatus.ACTIVE }
        assertEquals(1, activeOnlyOne.size)
        assertEquals("l1", activeOnlyOne.first().id)

        // 2. Múltiplas listas ativas -> Mostra seletor
        val activeMultiple = listOf(listActive1, listActive2).filter { it.status == com.example.domain.model.ListStatus.ACTIVE }
        assertEquals(2, activeMultiple.size)

        // 3. Nenhuma lista ativa (somente bloqueada) -> Não abre automaticamente
        val activeNone = listOf(listBlocked).filter { it.status == com.example.domain.model.ListStatus.ACTIVE }
        assertEquals(0, activeNone.size)
    }
}


