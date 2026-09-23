package com.aistudio.executivogo.trnsp.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.executivogo.trnsp.MyFirebaseMessagingService
import com.aistudio.executivogo.trnsp.data.*
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Success(val message: String) : UiState
    data class Error(val message: String) : UiState
}

enum class PeriodType {
    HOJE,
    ESTA_SEMANA,
    ESTE_MES,
    MES_ANTERIOR,
    ESTE_ANO,
    TODOS,
    PERSONALIZADO
}

data class FinancialFilter(
    val type: PeriodType = PeriodType.ESTE_MES,
    val customStartMs: Long? = null,
    val customEndMs: Long? = null,
    val selectedCompanyId: String? = null,
    val selectedDriverId: String? = null
)

class MainViewModel(
    application: Application,
    private val repository: ExecutivoGoRepository = ExecutivoGoRepository()
) : AndroidViewModel(application) {

    private val TAG = "ExecutivoGoVM"

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<AppUser?>(null)
    val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()

    private val _users = MutableStateFlow<List<AppUser>>(emptyList())
    val users: StateFlow<List<AppUser>> = _users.asStateFlow()

    private val _drivers = MutableStateFlow<List<AppUser>>(emptyList())
    val drivers: StateFlow<List<AppUser>> = _drivers.asStateFlow()

    private val _companies = MutableStateFlow<List<Company>>(emptyList())
    val companies: StateFlow<List<Company>> = _companies.asStateFlow()

    private val _passengers = MutableStateFlow<List<Passenger>>(emptyList())
    val passengers: StateFlow<List<Passenger>> = _passengers.asStateFlow()

    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips.asStateFlow()

    private val _driverTrips = MutableStateFlow<List<Trip>>(emptyList())
    val driverTrips: StateFlow<List<Trip>> = _driverTrips.asStateFlow()

    private val _paymentMethods = MutableStateFlow<List<PaymentMethod>>(emptyList())
    val paymentMethods: StateFlow<List<PaymentMethod>> = _paymentMethods.asStateFlow()

    private val _fareBands = MutableStateFlow<List<FareBand>>(emptyList())
    val fareBands: StateFlow<List<FareBand>> = _fareBands.asStateFlow()

    private val _routes = MutableStateFlow<List<Route>>(emptyList())
    val routes: StateFlow<List<Route>> = _routes.asStateFlow()

    private val _companyTrips = MutableStateFlow<List<Trip>>(emptyList())
    val companyTrips: StateFlow<List<Trip>> = _companyTrips.asStateFlow()

    private val _companyPassengers = MutableStateFlow<List<Passenger>>(emptyList())
    val companyPassengers: StateFlow<List<Passenger>> = _companyPassengers.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _financialFilter = MutableStateFlow(FinancialFilter(PeriodType.ESTE_MES))
    val financialFilter: StateFlow<FinancialFilter> = _financialFilter.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _pendingTripId = MutableStateFlow<String?>(null)
    val pendingTripId: StateFlow<String?> = _pendingTripId.asStateFlow()

    private val _pendingNotifType = MutableStateFlow<String?>(null)
    val pendingNotifType: StateFlow<String?> = _pendingNotifType.asStateFlow()

    fun setPendingNotification(tripId: String, type: String) {
        _pendingTripId.value = tripId
        _pendingNotifType.value = type
    }

    fun clearPendingNotification() {
        _pendingTripId.value = null
        _pendingNotifType.value = null
    }

    // Firestore Listeners
    private var usersListener: ListenerRegistration? = null
    private var companiesListener: ListenerRegistration? = null
    private var passengersListener: ListenerRegistration? = null
    private var tripsListener: ListenerRegistration? = null
    private var driverTripsListener: ListenerRegistration? = null
    private var paymentMethodsListener: ListenerRegistration? = null
    private var fareBandsListener: ListenerRegistration? = null
    private var routesListener: ListenerRegistration? = null
    private var companyTripsListener: ListenerRegistration? = null
    private var companyPassengersListener: ListenerRegistration? = null
    private var notificationsListener: ListenerRegistration? = null
    private var driverLocationsListener: ListenerRegistration? = null
    private val sessionStartTime = System.currentTimeMillis()
    private val _notifications = MutableStateFlow<List<NotificationRecord>>(emptyList())
    val notifications: StateFlow<List<NotificationRecord>> = _notifications.asStateFlow()

    private val _driverLocations = MutableStateFlow<List<DriverLocation>>(emptyList())
    val driverLocations: StateFlow<List<DriverLocation>> = _driverLocations.asStateFlow()

    init {
        // Assegura canal de notificações FCM com alta prioridade
        MyFirebaseMessagingService.createNotificationChannel(application)

        // Verifica se há usuário autenticado persistido
        checkPersistedSession()
    }

    fun checkPersistedSession() {
        val authUser = repository.getCurrentUser()
        if (authUser != null) {
            viewModelScope.launch {
                try {
                    val profile = repository.getUserById(authUser.uid)
                    if (profile == null || !profile.active) {
                        repository.signOut()
                        _currentUser.value = null
                        _errorMessage.value = "Conta inativa ou não encontrada. Faça login novamente."
                        return@launch
                    }
                    var activeProfile = profile
                    if (activeProfile.role.equals(UserRole.COMPANY, ignoreCase = true) && activeProfile.companyId.isBlank()) {
                        val matchingComp = repository.findCompanyByEmail(activeProfile.email)
                            ?: _companies.value.find { it.name.equals(activeProfile.companyName, ignoreCase = true) }
                            ?: if (_companies.value.size == 1) _companies.value.first() else null
                        if (matchingComp != null) {
                            repository.linkCompanyToUser(activeProfile.id, matchingComp.id, matchingComp.name)
                            activeProfile = activeProfile.copy(companyId = matchingComp.id, companyName = matchingComp.name)
                        } else {
                            repository.signOut()
                            _currentUser.value = null
                            _errorMessage.value = "Conta corporativa sem empresa vinculada. Solicite ao administrador vincular sua empresa no painel."
                            return@launch
                        }
                    }
                    _currentUser.value = activeProfile
                    startAllListeners()
                    // Sincroniza FCM Token automaticamente no Firestore para o usuário logado
                    MyFirebaseMessagingService.fetchAndSyncToken(getApplication<Application>(), authUser.uid) { token ->
                        _currentUser.value = _currentUser.value?.copy(fcmToken = token, fcmTokenAndroid = token)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao recuperar sessão persistida: ${e.message}")
                }
            }
        }
    }

    fun startAllListeners() {
        stopAllListeners()

        val activeUser = _currentUser.value
        val isCompany = activeUser?.role.equals(UserRole.COMPANY, ignoreCase = true)
        val companyId = activeUser?.companyId.orEmpty()

        // 1. Users (se for empresa, isola estritamente o perfil dela)
        usersListener = repository.listenUsers(
            onUpdate = { list ->
                val current = _currentUser.value
                val userIsCompany = current?.role.equals(UserRole.COMPANY, ignoreCase = true)
                if (userIsCompany) {
                    _users.value = list.filter { it.id == current?.id }
                    _drivers.value = list.filter { it.role.equals(UserRole.DRIVER, ignoreCase = true) }
                } else {
                    _users.value = list
                    _drivers.value = list.filter { it.role.equals(UserRole.DRIVER, ignoreCase = true) }
                }
                // Atualiza currentUser se mudou no banco
                current?.let { curr ->
                    list.find { it.id == curr.id }?.let { updated ->
                        _currentUser.value = updated
                    }
                }
            },
            onError = { e -> _errorMessage.value = "Erro em usuários: ${e.localizedMessage}" }
        )

        // 2. Companies (ISOLAMENTO ESTRITO: se for empresa, filtra APENAS a própria empresa)
        companiesListener = repository.listenCompanies(
            onUpdate = { list ->
                val current = _currentUser.value
                val userIsCompany = current?.role.equals(UserRole.COMPANY, ignoreCase = true)
                val cid = current?.companyId.orEmpty()
                _companies.value = if (userIsCompany && cid.isNotBlank()) {
                    list.filter { it.id == cid }
                } else {
                    list
                }
            },
            onError = { e -> _errorMessage.value = "Erro em empresas: ${e.localizedMessage}" }
        )

        // 3. Passengers (ISOLAMENTO ESTRITO: empresa só tem acesso aos seus próprios colaboradores)
        passengersListener = if (isCompany && companyId.isNotBlank()) {
            repository.listenPassengersForCompany(
                companyId = companyId,
                onUpdate = { list ->
                    val sorted = list.sortedBy { it.name }
                    _passengers.value = sorted
                    _companyPassengers.value = sorted
                },
                onError = { e -> _errorMessage.value = "Erro em passageiros da empresa: ${e.localizedMessage}" }
            )
        } else {
            repository.listenPassengers(
                onUpdate = { list ->
                    val current = _currentUser.value
                    val userIsCompany = current?.role.equals(UserRole.COMPANY, ignoreCase = true)
                    val cid = current?.companyId.orEmpty()
                    if (userIsCompany && cid.isNotBlank()) {
                        val filtered = list.filter { it.companyId == cid }.sortedBy { it.name }
                        _passengers.value = filtered
                        _companyPassengers.value = filtered
                    } else {
                        _passengers.value = list
                    }
                },
                onError = { e -> _errorMessage.value = "Erro em passageiros: ${e.localizedMessage}" }
            )
        }

        // 4. Trips (ISOLAMENTO ESTRITO: empresa só tem acesso às viagens dela)
        tripsListener = if (isCompany && companyId.isNotBlank()) {
            repository.listenTripsForCompany(
                companyId = companyId,
                onUpdate = { list ->
                    val sorted = list.sortedByDescending { it.scheduledTime }
                    _trips.value = sorted
                    _companyTrips.value = sorted
                },
                onError = { e -> _errorMessage.value = "Erro em viagens da empresa: ${e.localizedMessage}" }
            )
        } else {
            repository.listenAllTrips(
                onUpdate = { list ->
                    val current = _currentUser.value
                    val userIsCompany = current?.role.equals(UserRole.COMPANY, ignoreCase = true)
                    val cid = current?.companyId.orEmpty()
                    if (userIsCompany && cid.isNotBlank()) {
                        val filtered = list.filter { it.companyId == cid }.sortedByDescending { it.scheduledTime }
                        _trips.value = filtered
                        _companyTrips.value = filtered
                    } else {
                        _trips.value = list.sortedByDescending { it.scheduledTime }
                    }
                },
                onError = { e -> _errorMessage.value = "Erro em viagens: ${e.localizedMessage}" }
            )
        }

        // 5. Payment Methods
        paymentMethodsListener = repository.listenPaymentMethods(
            onUpdate = { list -> _paymentMethods.value = list },
            onError = { e -> _errorMessage.value = "Erro em pagamentos: ${e.localizedMessage}" }
        )

        // 6. Fare Bands (Faixas de KM)
        fareBandsListener = repository.listenFareBands(
            onUpdate = { list -> _fareBands.value = list },
            onError = { e -> _errorMessage.value = "Erro em faixas de KM: ${e.localizedMessage}" }
        )

        // 7. Routes (Rotas / Tabela de Preços)
        routesListener = repository.listenRoutes(
            onUpdate = { list -> _routes.value = list },
            onError = { e -> _errorMessage.value = "Erro em rotas: ${e.localizedMessage}" }
        )

        // Inicializa métodos de pagamento padrão se a coleção estiver vazia
        viewModelScope.launch {
            repository.seedDefaultPaymentMethodsIfEmpty()
        }

        // Se for motorista, inicializa listener específico
        _currentUser.value?.let { user ->
            if (user.role.equals(UserRole.DRIVER, ignoreCase = true)) {
                loadTripsForDriver(user.id)
            } else if (user.role.equals(UserRole.COMPANY, ignoreCase = true) && user.companyId.isNotBlank()) {
                loadCompanyData(user.companyId)
            }
        }

        // 11. Listener de Notificações em Tempo Real (Pop-up nativo Android para o usuário logado)
        notificationsListener?.remove()
        val currentUid = activeUser?.id.orEmpty()
        val currentRole = activeUser?.role.orEmpty()
        val currentCompany = activeUser?.companyId.orEmpty()
        notificationsListener = repository.listenNotifications(
            userId = currentUid,
            role = currentRole,
            companyId = currentCompany,
            sinceTimestamp = sessionStartTime
        ) { notif ->
            MyFirebaseMessagingService.showNotification(
                getApplication<Application>(),
                notif.title,
                notif.body,
                notif.tripId
            )
            _notifications.value = listOf(notif) + _notifications.value
        }

        // 6. Driver Real-time Locations (Telemetria Contínua de GPS)
        driverLocationsListener?.remove()
        driverLocationsListener = repository.listenDriverLocations(
            onUpdate = { list -> _driverLocations.value = list },
            onError = { /* ignored */ }
        )
    }

    fun startDriverTracking(context: Context) {
        val user = _currentUser.value ?: return
        if (user.role.equals(UserRole.DRIVER, ignoreCase = true)) {
            try {
                com.aistudio.executivogo.trnsp.location.DriverLocationService.startTracking(
                    context = context.applicationContext,
                    driverId = user.id,
                    driverName = user.name,
                    driverPhone = user.phone,
                    vehicleModel = user.vehicleModel,
                    vehiclePlate = user.vehiclePlate,
                    companyId = user.companyId
                )
            } catch (e: Exception) {
                Log.e("MainViewModel", "Erro ao chamar startDriverTracking: ${e.message}")
            }
        }
    }

    fun stopDriverTracking(context: Context) {
        com.aistudio.executivogo.trnsp.location.DriverLocationService.stopTracking(context)
    }

    fun loadTripsForDriver(driverId: String) {
        driverTripsListener?.remove()
        driverTripsListener = repository.listenDriverTrips(
            driverId = driverId,
            onUpdate = { list ->
                _driverTrips.value = list.sortedByDescending { it.scheduledTime }
            },
            onError = { e -> _errorMessage.value = "Erro nas viagens do motorista: ${e.localizedMessage}" }
        )
    }

    fun loadCompanyData(companyId: String) {
        companyTripsListener?.remove()
        companyPassengersListener?.remove()

        companyTripsListener = repository.listenTripsForCompany(
            companyId = companyId,
            onUpdate = { list ->
                _companyTrips.value = list.sortedByDescending { it.scheduledTime }
            },
            onError = { e -> _errorMessage.value = "Erro nas viagens da empresa: ${e.localizedMessage}" }
        )

        companyPassengersListener = repository.listenPassengersForCompany(
            companyId = companyId,
            onUpdate = { list ->
                _companyPassengers.value = list.sortedBy { it.name }
            },
            onError = { e -> _errorMessage.value = "Erro nos passageiros da empresa: ${e.localizedMessage}" }
        )
    }

    private fun stopAllListeners() {
        usersListener?.remove()
        companiesListener?.remove()
        passengersListener?.remove()
        tripsListener?.remove()
        driverTripsListener?.remove()
        paymentMethodsListener?.remove()
        fareBandsListener?.remove()
        routesListener?.remove()
        companyTripsListener?.remove()
        companyPassengersListener?.remove()
        notificationsListener?.remove()
        driverLocationsListener?.remove()
    }

    override fun onCleared() {
        super.onCleared()
        stopAllListeners()
    }

    fun clearMessages() {
        _statusMessage.value = null
        _errorMessage.value = null
        _uiState.value = UiState.Idle
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
        _uiState.value = UiState.Idle
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // ==========================================
    // AUTHENTICATION
    // ==========================================
    fun login(email: String, pass: String, onSuccess: (AppUser) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.signIn(email, pass)
            result.onSuccess { firebaseUser ->
                try {
                    var profile = try {
                        repository.getUserById(firebaseUser.uid)
                    } catch (_: Exception) {
                        null
                    }

                    if (profile == null) {
                        val emailLower = (firebaseUser.email ?: email).trim().lowercase()
                        val assignedRole = when {
                            emailLower.contains("admin") -> UserRole.ADMIN
                            emailLower.contains("motorista") -> UserRole.DRIVER
                            emailLower.contains("empresa") || emailLower.contains("company") -> UserRole.COMPANY
                            else -> UserRole.ADMIN
                        }
                        val newAppUser = AppUser(
                            id = firebaseUser.uid,
                            name = firebaseUser.displayName ?: emailLower.substringBefore("@"),
                            email = emailLower,
                            role = assignedRole,
                            active = true,
                            isOnline = assignedRole.equals(UserRole.DRIVER, ignoreCase = true)
                        )
                        try {
                            repository.saveUser(newAppUser)
                        } catch (_: Exception) {}
                        profile = newAppUser
                    }

                    if (!profile.active) {
                        repository.signOut()
                        _currentUser.value = null
                        val msg = "Perfil de usuário inativo."
                        _uiState.value = UiState.Error(msg)
                        onError(msg)
                        return@launch
                    }

                    var activeProfile = profile
                    if (activeProfile.role.equals(UserRole.COMPANY, ignoreCase = true) && activeProfile.companyId.isBlank()) {
                        val matchingComp = try {
                            repository.findCompanyByEmail(activeProfile.email)
                                ?: _companies.value.find { it.name.equals(activeProfile.companyName, ignoreCase = true) }
                                ?: if (_companies.value.size == 1) _companies.value.first() else null
                        } catch (_: Exception) {
                            null
                        }
                        if (matchingComp != null) {
                            try {
                                repository.linkCompanyToUser(activeProfile.id, matchingComp.id, matchingComp.name)
                            } catch (_: Exception) {}
                            activeProfile = activeProfile.copy(companyId = matchingComp.id, companyName = matchingComp.name)
                        }
                    }

                    _currentUser.value = activeProfile
                    try {
                        startAllListeners()
                    } catch (_: Exception) {}

                    try {
                        MyFirebaseMessagingService.fetchAndSyncToken(getApplication<Application>(), firebaseUser.uid) { token ->
                            _currentUser.value = _currentUser.value?.copy(fcmToken = token, fcmTokenAndroid = token)
                        }
                    } catch (_: Exception) {}

                    _uiState.value = UiState.Success("Sucesso")
                    onSuccess(activeProfile)
                } catch (e: Exception) {
                    val msg = e.localizedMessage ?: "Erro ao processar login."
                    _uiState.value = UiState.Error(msg)
                    onError(msg)
                }
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Falha ao realizar login."
                _uiState.value = UiState.Error(msg)
                onError(msg)
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            // Se for motorista, desativa online antes de sair
            _currentUser.value?.let { user ->
                if (user.role.equals(UserRole.DRIVER, ignoreCase = true)) {
                    repository.updateDriverOnlineStatus(user.id, false)
                }
            }
            stopAllListeners()
            repository.signOut()
            _currentUser.value = null
            _companies.value = emptyList()
            _passengers.value = emptyList()
            _trips.value = emptyList()
            _driverTrips.value = emptyList()
            _companyTrips.value = emptyList()
            _companyPassengers.value = emptyList()
            onComplete()
        }
    }

    fun setDriverOnlineStatus(isOnline: Boolean) {
        val uid = _currentUser.value?.id ?: repository.getCurrentUserId()
        if (uid.isNullOrBlank()) return

        viewModelScope.launch {
            try {
                repository.updateDriverOnlineStatus(uid, isOnline)
                _currentUser.value?.let { current ->
                    _currentUser.value = current.copy(isOnline = isOnline)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao atualizar status: ${e.localizedMessage}"
            }
        }
    }

    // ==========================================
    // USERS / DRIVERS MANAGEMENT
    // ==========================================
    fun saveUser(user: AppUser, password: String? = null, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                if (user.role.equals(UserRole.COMPANY, ignoreCase = true) && user.companyId.isBlank()) {
                    _errorMessage.value = "Selecione uma empresa vinculada para o perfil Empresa."
                    return@launch
                }
                if (!password.isNullOrBlank()) {
                    // Criação com Auth secundário
                    val result = repository.createUserWithAuth(user, password)
                    result.onSuccess { createdUser ->
                        _statusMessage.value = "Usuário ${createdUser.name} cadastrado com sucesso!"
                        onComplete()
                    }.onFailure { e ->
                        _errorMessage.value = e.localizedMessage ?: "Erro ao criar credencial de usuário."
                    }
                } else {
                    // Edição de cadastro existente
                    repository.saveUser(user)
                    _statusMessage.value = "Dados de ${user.name} atualizados com sucesso."
                    onComplete()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao salvar usuário: ${e.localizedMessage}"
            }
        }
    }

    fun linkCompanyToUser(userId: String, companyId: String, companyName: String = "", onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val result = repository.linkCompanyToUser(userId, companyId, companyName)
                result.onSuccess {
                    _statusMessage.value = "Empresa vinculada com sucesso ao usuário."
                    onComplete()
                }.onFailure { err ->
                    _errorMessage.value = err.localizedMessage ?: "Erro ao vincular empresa ao usuário."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao vincular empresa: ${e.localizedMessage}"
            }
        }
    }

    fun deleteUser(user: AppUser, explicitPassword: String? = null, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val authDeleted = repository.deleteUser(user.id, explicitPassword)
                if (authDeleted) {
                    _statusMessage.value = "Usuário ${user.name} excluído do sistema e do Firebase Authentication com sucesso."
                } else {
                    _statusMessage.value = "Usuário ${user.name} removido do sistema."
                }
                onComplete()
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao excluir usuário: ${e.localizedMessage}"
            }
        }
    }

    fun deleteUser(userId: String, explicitPassword: String? = null, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val authDeleted = repository.deleteUser(userId, explicitPassword)
                if (authDeleted) {
                    _statusMessage.value = "Usuário excluído do sistema e do Firebase Authentication com sucesso."
                } else {
                    _statusMessage.value = "Usuário removido do sistema."
                }
                onComplete()
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao excluir usuário: ${e.localizedMessage}"
            }
        }
    }

    // ==========================================
    // COMPANIES
    // ==========================================
    fun saveCompany(company: Company, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.saveCompany(company)
                _statusMessage.value = "Empresa ${company.name} salva com sucesso."
                onComplete()
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao salvar empresa: ${e.localizedMessage}"
            }
        }
    }

    fun deleteCompany(company: Company, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteCompany(company.id)
                _statusMessage.value = "Empresa ${company.name} excluída."
                onComplete()
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao excluir empresa: ${e.localizedMessage}"
            }
        }
    }

    fun deleteCompany(companyId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteCompany(companyId)
                _statusMessage.value = "Empresa excluída."
                onComplete()
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao excluir empresa: ${e.localizedMessage}"
            }
        }
    }

    // ==========================================
    // PASSENGERS
    // ==========================================
    fun savePassenger(passenger: Passenger, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.savePassenger(passenger)
                _statusMessage.value = "Passageiro ${passenger.name} salvo com sucesso."
                onComplete()
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao salvar passageiro: ${e.localizedMessage}"
            }
        }
    }

    fun deletePassenger(passenger: Passenger, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deletePassenger(passenger.id)
                _statusMessage.value = "Passageiro ${passenger.name} removido."
                onComplete()
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao remover passageiro: ${e.localizedMessage}"
            }
        }
    }

    fun deletePassenger(passengerId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deletePassenger(passengerId)
                _statusMessage.value = "Passageiro removido."
                onComplete()
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao remover passageiro: ${e.localizedMessage}"
            }
        }
    }

    // ==========================================
    // PAYMENT METHODS MASTER
    // ==========================================
    fun savePaymentMethod(
        name: String,
        description: String,
        id: String = "",
        active: Boolean = true,
        termDays: Int = 0,
        means: String = PaymentMeans.PIX
    ) {
        viewModelScope.launch {
            try {
                val method = PaymentMethod(
                    id = id,
                    name = name,
                    description = description,
                    termDays = termDays,
                    means = means,
                    active = active
                )
                repository.savePaymentMethod(method)
                _statusMessage.value = "Forma de pagamento salva."
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao salvar forma de pagamento: ${e.localizedMessage}"
            }
        }
    }

    fun deletePaymentMethod(method: PaymentMethod) {
        viewModelScope.launch {
            try {
                repository.deletePaymentMethod(method.id)
                _statusMessage.value = "Forma de pagamento excluída."
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao excluir forma de pagamento: ${e.localizedMessage}"
            }
        }
    }

    // ==========================================
    // TRIPS (CORRIDAS) & BUSINESS RULES
    // ==========================================
    fun saveTrip(
        id: String = "",
        companyId: String,
        companyName: String,
        driverId: String?,
        driverName: String,
        passengerName: String,
        passengerPhone: String,
        origin: String,
        destination: String,
        scheduledTime: Long,
        price: Double,
        status: String = TripStatus.PENDING,
        notes: String = "",
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                // 1. Busca dados da empresa para congelar prazos e formas de pagamento
                val company = _companies.value.find { it.id == companyId }
                val term = company?.paymentTerm ?: PaymentTerms.A_VISTA
                val means = company?.paymentMeans ?: PaymentMeans.PIX

                // 2. Busca motorista para aplicar a comissão EXATA do cadastro (sem fixar em 20%)
                val driver = _drivers.value.find { it.id == driverId }
                val commissionPercent = driver?.commissionPercentage ?: 20.0
                val calculatedCommission = if (driverId != null) {
                    price * (commissionPercent / 100.0)
                } else {
                    0.0
                }

                // 3. Cadastra ou localiza o passageiro automaticamente
                val resolvedPassenger = repository.findOrCreatePassenger(
                    name = passengerName,
                    phone = passengerPhone,
                    companyId = companyId
                )

                // 4. Constrói o objeto Trip
                val currentTrip = if (id.isNotBlank()) _trips.value.find { it.id == id } else null
                val termDays = PaymentTerms.getDays(term)
                val now = System.currentTimeMillis()

                val tripToSave = (currentTrip ?: Trip()).copy(
                    id = id,
                    companyId = companyId,
                    companyName = companyName.ifBlank { company?.name ?: "Empresa Corporativa" },
                    driverId = driverId,
                    driverName = driverName.ifBlank { driver?.name ?: "" },
                    passengerId = resolvedPassenger.id,
                    passengerName = passengerName,
                    passengerPhone = passengerPhone,
                    origin = origin.trim(),
                    destination = destination.trim(),
                    scheduledTime = scheduledTime,
                    price = price,
                    driverCommission = calculatedCommission,
                    status = status,
                    notes = notes,
                    paymentTermSnapshot = term,
                    paymentMeansSnapshot = means,
                    completedAt = if (status == TripStatus.COMPLETED && currentTrip?.completedAt == null) now else currentTrip?.completedAt,
                    dueDate = if (status == TripStatus.COMPLETED && currentTrip?.dueDate == null) now + (termDays * 86_400_000L) else currentTrip?.dueDate,
                    commissionAvailableAt = if (status == TripStatus.COMPLETED && currentTrip?.commissionAvailableAt == null) now + (termDays * 86_400_000L) else currentTrip?.commissionAvailableAt
                )

                val savedId = repository.saveTrip(tripToSave)
                val finalSavedTrip = tripToSave.copy(id = savedId)

                // Cenário 1: Viagem criada pelo administrador
                if (id.isEmpty()) {
                    repository.notifyTripCreatedByAdmin(finalSavedTrip)
                } else if (!driverId.isNullOrBlank() && currentTrip?.driverId != driverId) {
                    // Cenário 7: Motorista atribuído / reatribuído na edição
                    val assignedDriver = _drivers.value.find { it.id == driverId }
                    if (assignedDriver != null) {
                        repository.notifyDriverAssigned(finalSavedTrip, assignedDriver)
                    } else {
                        repository.dispatchNotificationRecord(
                            driverId = driverId,
                            title = "Nova Viagem Atribuída",
                            body = "Trecho: $origin ➔ $destination. Horário: ${formatDateTime(scheduledTime)}",
                            tripId = savedId,
                            type = NotificationType.DRIVER_ASSIGNED
                        )
                    }
                }

                _statusMessage.value = "Viagem salva com sucesso!"
                onComplete()
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao salvar viagem: ${e.localizedMessage}"
            }
        }
    }

    fun saveTrip(trip: Trip, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val company = _companies.value.find { it.id == trip.companyId }
                val term = if (trip.paymentTermSnapshot.isNotBlank()) trip.paymentTermSnapshot else (company?.paymentTerm ?: PaymentTerms.A_VISTA)
                val means = if (trip.paymentMeansSnapshot.isNotBlank()) trip.paymentMeansSnapshot else (company?.paymentMeans ?: PaymentMeans.PIX)
                val driver = _drivers.value.find { it.id == trip.driverId }
                val commissionPercent = driver?.commissionPercentage ?: 20.0
                val comm = if (trip.driverCommission > 0) {
                    trip.driverCommission
                } else if (trip.driverId != null) {
                    trip.price * (commissionPercent / 100.0)
                } else {
                    0.0
                }

                val baseTripToSave = trip.copy(
                    companyName = trip.companyName.ifBlank { company?.name ?: "Empresa Corporativa" },
                    driverName = trip.driverName.ifBlank { driver?.name ?: "" },
                    driverCommission = comm,
                    paymentTermSnapshot = term,
                    paymentMeansSnapshot = means
                )

                val isCompleted = baseTripToSave.status.equals(TripStatus.COMPLETED, ignoreCase = true)
                val compAt = baseTripToSave.completedAt ?: 0L
                val tripToSave = if (isCompleted && compAt <= 0L) {
                    val now = System.currentTimeMillis()
                    val termDays = PaymentTerms.getDays(term)
                    val termMillis = termDays * 86_400_000L
                    val calculatedDueDate = now + termMillis
                    baseTripToSave.copy(
                        completedAt = now,
                        dueDate = calculatedDueDate,
                        commissionAvailableAt = calculatedDueDate,
                        companyPaymentStatus = baseTripToSave.companyPaymentStatus.ifBlank { PaymentStatus.PENDING },
                        paymentStatus = baseTripToSave.paymentStatus.ifBlank { PaymentStatus.PENDING }
                    )
                } else {
                    baseTripToSave
                }

                val savedId = repository.saveTrip(tripToSave)
                val finalSavedTrip = tripToSave.copy(id = savedId)

                if (trip.id.isEmpty()) {
                    repository.notifyTripCreatedByAdmin(finalSavedTrip)
                } else if (!trip.driverId.isNullOrBlank()) {
                    val assignedDriver = _drivers.value.find { it.id == trip.driverId }
                    if (assignedDriver != null) {
                        repository.notifyDriverAssigned(finalSavedTrip, assignedDriver)
                    } else {
                        repository.dispatchNotificationRecord(
                            driverId = trip.driverId,
                            title = "Nova Viagem Atribuída",
                            body = "Trecho: ${trip.origin} ➔ ${trip.destination}",
                            tripId = savedId,
                            type = NotificationType.DRIVER_ASSIGNED
                        )
                    }
                }

                _statusMessage.value = "Viagem salva com sucesso!"
                onComplete()
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao salvar viagem: ${e.localizedMessage}"
            }
        }
    }

    fun reassignDriver(tripId: String, newDriver: AppUser, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val trip = _trips.value.find { it.id == tripId } ?: _driverTrips.value.find { it.id == tripId }
                val targetTripId = if (trip != null && trip.id.isNotBlank()) trip.id else tripId
                if (targetTripId.isNotBlank()) {
                    val commissionPercent = newDriver.commissionPercentage
                    val price = trip?.price ?: 0.0
                    val newCommission = price * (commissionPercent / 100.0)
                    // Atualiza diretamente a viagem existente no Firestore, sem criar documento duplicado
                    repository.assignDriverToTrip(targetTripId, newDriver, newCommission)
                    val updatedTrip = (trip ?: Trip(id = targetTripId)).copy(
                        id = targetTripId,
                        driverId = newDriver.id,
                        driverName = newDriver.name,
                        driverPhone = newDriver.phone,
                        vehicleModel = newDriver.vehicleModel,
                        vehiclePlate = newDriver.vehiclePlate,
                        vehicleColor = newDriver.vehicleColor,
                        driverCommission = newCommission,
                        status = TripStatus.PENDING
                    )
                    repository.notifyDriverAssigned(updatedTrip, newDriver)
                    _statusMessage.value = "Motorista ${newDriver.name} atribuído à viagem com sucesso!"
                    onComplete()
                } else {
                    _errorMessage.value = "Erro: Identificador da viagem não encontrado."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao atribuir motorista: ${e.localizedMessage}"
            }
        }
    }

    fun reassignDriver(trip: Trip, newDriver: AppUser, onComplete: () -> Unit = {}) {
        reassignDriver(trip.id, newDriver, onComplete)
    }

    fun rejectTrip(trip: Trip, reason: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val driverName = _currentUser.value?.name ?: "Motorista"
                val refusalNote = "[RECUSA: $driverName - Motivo: $reason]"
                val updatedNotes = if (trip.notes.isNotBlank()) "${trip.notes}\n$refusalNote" else refusalNote
                // Desassocia o motorista e deixa a viagem como PENDENTE para a central poder reatribuir
                val updatedTrip = trip.copy(
                    driverId = null,
                    driverName = "",
                    driverPhone = "",
                    vehicleModel = "",
                    vehiclePlate = "",
                    vehicleColor = "",
                    driverCommission = 0.0,
                    status = TripStatus.PENDING,
                    notes = updatedNotes
                )
                repository.saveTrip(updatedTrip)
                // Cenário 4: Motorista recusou a viagem
                repository.notifyTripRejectedByDriver(trip, driverName, reason)
                _statusMessage.value = "Viagem recusada. Motivo enviado à central."
                onComplete()
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao recusar viagem: ${e.localizedMessage}"
            }
        }
    }

    fun completeTrip(trip: Trip) {
        viewModelScope.launch {
            try {
                val validStops = trip.stops.map { it.trim() }.filter { it.isNotBlank() }
                if (validStops.isNotEmpty() && trip.currentStopIndex < validStops.size) {
                    _errorMessage.value = "Confirme todas as paradas intermediárias antes de concluir a viagem."
                    return@launch
                }
                val termDays = PaymentTerms.getDays(trip.paymentTermSnapshot)
                repository.completeTrip(trip.id, termDays)
                _statusMessage.value = "Viagem concluída! Prazos financeiros atualizados."
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao concluir viagem: ${e.localizedMessage}"
            }
        }
    }

    fun confirmTripStop(trip: Trip) {
        val validStops = trip.stops.map { it.trim() }.filter { it.isNotBlank() }
        val nextIndex = trip.currentStopIndex + 1
        viewModelScope.launch {
            try {
                repository.updateTripStopIndex(trip.id, nextIndex)
                _trips.value = _trips.value.map {
                    if (it.id == trip.id) it.copy(currentStopIndex = nextIndex) else it
                }
                _driverTrips.value = _driverTrips.value.map {
                    if (it.id == trip.id) it.copy(currentStopIndex = nextIndex) else it
                }
                if (nextIndex < validStops.size) {
                    _statusMessage.value = "Parada $nextIndex concluída! Próximo destino: ${validStops[nextIndex]}"
                } else {
                    _statusMessage.value = "Todas as paradas concluídas! Próximo destino: Destino Final"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao confirmar parada: ${e.localizedMessage}"
            }
        }
    }

    fun updateTripStatus(tripId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                val trip = _trips.value.find { it.id == tripId }
                    ?: _driverTrips.value.find { it.id == tripId }
                    ?: _companyTrips.value.find { it.id == tripId }
                    ?: repository.getTripById(tripId)
                val previousStatus = trip?.status.orEmpty()
                val currentUser = _currentUser.value
                val isAdmin = currentUser?.role.equals(UserRole.ADMIN, ignoreCase = true)

                if (newStatus == TripStatus.COMPLETED) {
                    val termDays = PaymentTerms.getDays(trip?.paymentTermSnapshot ?: PaymentTerms.A_VISTA)
                    repository.completeTrip(tripId, termDays)
                } else if (newStatus == TripStatus.CANCELLED && isAdmin) {
                    repository.adminCancelTrip(tripId)
                } else {
                    repository.updateTripStatus(tripId, newStatus)
                }

                // Sincroniza estado em memória imediatamente
                _trips.value = _trips.value.map { if (it.id == tripId) it.copy(status = newStatus) else it }
                _companyTrips.value = _companyTrips.value.map { if (it.id == tripId) it.copy(status = newStatus) else it }
                _driverTrips.value = _driverTrips.value.map { if (it.id == tripId) it.copy(status = newStatus) else it }

                if (trip != null) {
                    val updatedTrip = trip.copy(status = newStatus)
                    // Cenário 3: Motorista aceitou a viagem
                    if (newStatus == TripStatus.ACCEPTED &&
                        (previousStatus == TripStatus.PENDING || previousStatus.isBlank())) {
                        val driverName = currentUser?.name ?: trip.driverName.ifBlank { "Motorista" }
                        repository.notifyTripAcceptedByDriver(updatedTrip, driverName)
                    }

                    // Cenário 4: Viagem Iniciada (para Empresas e Administrador)
                    if (newStatus == TripStatus.IN_PROGRESS && previousStatus != TripStatus.IN_PROGRESS) {
                        val driverName = currentUser?.name ?: trip.driverName.ifBlank { "Motorista" }
                        repository.notifyTripStarted(updatedTrip, driverName)
                    }

                    // Cenário 5: Empresa ou Administrador cancelou a viagem
                    if (newStatus == TripStatus.CANCELLED) {
                        val role = currentUser?.role ?: UserRole.ADMIN
                        val name = currentUser?.name ?: "Central"
                        repository.notifyTripCancelled(updatedTrip, role, name)
                    }
                }

                _statusMessage.value = "Status atualizado para $newStatus"
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao atualizar status: ${e.localizedMessage}"
            }
        }
    }

    fun cancelTrip(
        tripId: String,
        reason: String = "",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val trip = _trips.value.find { it.id == tripId }
                    ?: _companyTrips.value.find { it.id == tripId }
                    ?: _driverTrips.value.find { it.id == tripId }
                    ?: repository.getTripById(tripId)
                val currentUser = _currentUser.value
                val isAdmin = currentUser?.role.equals(UserRole.ADMIN, ignoreCase = true)

                val result = if (isAdmin) {
                    repository.adminCancelTrip(tripId, reason)
                } else if (currentUser != null && currentUser.companyId.isNotBlank()) {
                    repository.cancelTripIfCompanyAllowed(tripId, currentUser.companyId, isAdmin = false)
                } else {
                    repository.updateTripStatus(tripId, TripStatus.CANCELLED)
                    Result.success(Unit)
                }

                result.onSuccess {
                    _trips.value = _trips.value.map { if (it.id == tripId) it.copy(status = TripStatus.CANCELLED) else it }
                    _companyTrips.value = _companyTrips.value.map { if (it.id == tripId) it.copy(status = TripStatus.CANCELLED) else it }
                    _driverTrips.value = _driverTrips.value.map { if (it.id == tripId) it.copy(status = TripStatus.CANCELLED) else it }

                    if (trip != null) {
                        val role = currentUser?.role ?: UserRole.ADMIN
                        val name = currentUser?.name ?: "Central"
                        repository.notifyTripCancelled(trip.copy(status = TripStatus.CANCELLED), role, name)
                    }
                    _statusMessage.value = "Viagem cancelada com sucesso."
                    onSuccess()
                }.onFailure { err ->
                    val msg = err.localizedMessage ?: "Erro ao cancelar viagem."
                    _errorMessage.value = msg
                    onError(msg)
                }
            } catch (e: Exception) {
                val msg = "Erro ao cancelar viagem: ${e.localizedMessage}"
                _errorMessage.value = msg
                onError(msg)
            }
        }
    }

    fun deleteTrip(trip: Trip) {
        deleteTrip(trip.id)
    }

    fun deleteTrip(tripId: String) {
        viewModelScope.launch {
            try {
                repository.deleteTrip(tripId)
                _trips.value = _trips.value.filter { it.id != tripId }
                _companyTrips.value = _companyTrips.value.filter { it.id != tripId }
                _driverTrips.value = _driverTrips.value.filter { it.id != tripId }
                _statusMessage.value = "Viagem removida com sucesso."
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao excluir viagem: ${e.localizedMessage}"
            }
        }
    }

    // ==========================================
    // MÓDULO FINANCEIRO ADMIN
    // ==========================================
    fun setFinancialFilter(type: PeriodType, startMs: Long? = null, endMs: Long? = null) {
        _financialFilter.value = _financialFilter.value.copy(
            type = type,
            customStartMs = startMs,
            customEndMs = endMs
        )
    }

    fun clearFinancialFilters() {
        _financialFilter.value = FinancialFilter(
            type = PeriodType.ESTE_MES,
            customStartMs = null,
            customEndMs = null,
            selectedCompanyId = null,
            selectedDriverId = null
        )
    }

    fun isTripInFinancialPeriod(trip: Trip, filter: FinancialFilter): Boolean {
        // Viagem entra na apuração pela data de conclusão ou de agendamento se concluída
        val tripTime = trip.completedAt ?: trip.scheduledTime
        val calendar = Calendar.getInstance()

        val matchesPeriod = when (filter.type) {
            PeriodType.TODOS -> true
            PeriodType.HOJE -> {
                val start = calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val end = calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                tripTime in start..end
            }
            PeriodType.ESTA_SEMANA -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, ..., 7 = Saturday
                val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)
                val start = cal.timeInMillis

                cal.add(Calendar.DAY_OF_YEAR, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis

                tripTime in start..end
            }
            PeriodType.ESTE_MES -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis

                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis

                tripTime in start..end
            }
            PeriodType.MES_ANTERIOR -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis

                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis

                tripTime in start..end
            }
            PeriodType.ESTE_ANO -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis

                calendar.set(Calendar.MONTH, Calendar.DECEMBER)
                calendar.set(Calendar.DAY_OF_MONTH, 31)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis

                tripTime in start..end
            }
            PeriodType.PERSONALIZADO -> {
                val s = filter.customStartMs ?: 0L
                val e = filter.customEndMs ?: Long.MAX_VALUE
                tripTime in s..e
            }
        }

        if (!matchesPeriod) return false
        if (!filter.selectedCompanyId.isNullOrBlank() && trip.companyId != filter.selectedCompanyId) {
            return false
        }
        if (!filter.selectedDriverId.isNullOrBlank() && trip.driverId != filter.selectedDriverId) {
            return false
        }
        return true
    }

    fun setFinancialCompanyFilter(companyId: String?) {
        _financialFilter.value = _financialFilter.value.copy(selectedCompanyId = companyId)
    }

    fun setFinancialDriverFilter(driverId: String?) {
        _financialFilter.value = _financialFilter.value.copy(selectedDriverId = driverId)
    }

    fun updateDriverLocation(latitude: Double, longitude: Double) {
        val uid = _currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.updateDriverLocation(uid, latitude, longitude)
        }
    }

    // Ações de Faturamento da Empresa
    fun markCompanyPaid(trip: Trip) {
        val adminId = _currentUser.value?.id ?: "ADMIN"
        viewModelScope.launch {
            try {
                repository.markCompanyPaid(trip.id, adminId)
                // Cenário 8: Recebimento de pagamento pelas empresas
                repository.notifyCompanyPaymentReceived(trip, trip.price)
                _statusMessage.value = "Faturamento da viagem marcado como RECEBIDO."
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao liquidar faturamento: ${e.localizedMessage}"
            }
        }
    }

    fun undoCompanyPaid(trip: Trip) {
        viewModelScope.launch {
            try {
                repository.undoCompanyPaid(trip.id)
                _statusMessage.value = "Faturamento revertido para PENDENTE."
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao desfazer faturamento: ${e.localizedMessage}"
            }
        }
    }

    fun markBatchCompanyPaid(tripIds: List<String>) {
        if (tripIds.isEmpty()) return
        val adminId = _currentUser.value?.id ?: "ADMIN"
        viewModelScope.launch {
            try {
                repository.markBatchCompanyPaid(tripIds, adminId)
                // Cenário 8: Notifica cada viagem baixada no lote
                for (tId in tripIds) {
                    val trip = _trips.value.find { it.id == tId }
                    if (trip != null) {
                        repository.notifyCompanyPaymentReceived(trip, trip.price)
                    }
                }
                _statusMessage.value = "${tripIds.size} viagens marcadas como RECEBIDAS em lote!"
            } catch (e: Exception) {
                _errorMessage.value = "Erro na baixa em lote: ${e.localizedMessage}"
            }
        }
    }

    fun markAllCompanyPendingPaidInPeriod(companyId: String) {
        val filter = _financialFilter.value
        val pendingTrips = _trips.value.filter {
            it.companyId == companyId &&
                    it.status == TripStatus.COMPLETED &&
                    it.companyPaymentStatus == PaymentStatus.PENDING &&
                    isTripInFinancialPeriod(it, filter)
        }
        markBatchCompanyPaid(pendingTrips.map { it.id })
    }

    // Ações de Comissão do Motorista
    fun markCommissionPaid(trip: Trip, force: Boolean = false) {
        val adminId = _currentUser.value?.id ?: "ADMIN"
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val isLiberated = (trip.commissionAvailableAt ?: 0L) <= now

                if (!isLiberated && !force) {
                    _errorMessage.value = "Esta comissão ainda não está liberada pelo prazo. Confirme para forçar pagamento."
                    return@launch
                }

                repository.markCommissionPaid(trip.id, adminId)

                // Cenário 6: Pagamento da comissão do motorista realizada
                repository.notifyCommissionPaid(trip)

                _statusMessage.value = "Comissão do motorista marcada como PAGA!"
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao pagar comissão: ${e.localizedMessage}"
            }
        }
    }

    fun undoCommissionPaid(trip: Trip) {
        viewModelScope.launch {
            try {
                repository.undoCommissionPaid(trip.id)
                _statusMessage.value = "Comissão revertida para PENDENTE."
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao desfazer comissão: ${e.localizedMessage}"
            }
        }
    }

    fun markBatchCommissionPaid(tripIds: List<String>) {
        if (tripIds.isEmpty()) return
        val adminId = _currentUser.value?.id ?: "ADMIN"
        viewModelScope.launch {
            try {
                repository.markBatchCommissionPaid(tripIds, adminId)
                _statusMessage.value = "${tripIds.size} comissões pagas com sucesso em lote!"
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao pagar lote de comissões: ${e.localizedMessage}"
            }
        }
    }

    fun markAllDriverLiberatedPaidInPeriod(driverId: String) {
        val now = System.currentTimeMillis()
        val filter = _financialFilter.value
        val liberatedTrips = _trips.value.filter {
            it.driverId == driverId &&
                    it.status == TripStatus.COMPLETED &&
                    it.paymentStatus == PaymentStatus.PENDING &&
                    (it.commissionAvailableAt ?: 0L) <= now &&
                    isTripInFinancialPeriod(it, filter)
        }
        markBatchCommissionPaid(liberatedTrips.map { it.id })
    }

    // ==========================================
    // REFRESH HELPERS
    // ==========================================
    fun refreshAll() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                startAllListeners()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun refreshUsers() = refreshAll()
    fun refreshCompanies() = refreshAll()
    fun refreshPassengers() = refreshAll()
    fun refreshPaymentMethods() = refreshAll()
    fun refreshTrips() = refreshAll()
    fun refreshFareBands() = refreshAll()
    fun refreshRoutes() = refreshAll()
    fun refreshDriverTrips() {
        val uid = _currentUser.value?.id
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                if (!uid.isNullOrBlank()) {
                    loadTripsForDriver(uid)
                }
                delay(300)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun refreshCompanyData() {
        val cid = _currentUser.value?.companyId
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                if (!cid.isNullOrBlank()) {
                    loadCompanyData(cid)
                }
                delay(300)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // ==========================================
    // FAIXAS DE KM (FARE BANDS)
    // ==========================================
    fun findMatchingBand(distanceKm: Double): FareBand? {
        return repository.findBand(distanceKm, _fareBands.value)
    }

    fun calculateRoutePricePreview(distanceKm: Double): Double {
        return repository.calculatePricePerKm(distanceKm, _fareBands.value)
    }

    fun saveFareBand(
        band: FareBand,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (band.name.isBlank()) {
            val msg = "Nome da faixa é obrigatório."
            _errorMessage.value = msg
            onError(msg)
            return
        }
        if (band.maxKm <= band.minKm) {
            val msg = "KM Máximo deve ser maior que KM Mínimo."
            _errorMessage.value = msg
            onError(msg)
            return
        }
        if (band.multiplier <= 0.0) {
            val msg = "Multiplicador (R$/km) deve ser maior que zero."
            _errorMessage.value = msg
            onError(msg)
            return
        }

        viewModelScope.launch {
            try {
                val id = repository.saveFareBand(band)
                _statusMessage.value = "Faixa de KM salva com sucesso!"
                onSuccess(id)
            } catch (e: Exception) {
                val msg = "Erro ao salvar faixa de KM: ${e.localizedMessage}"
                _errorMessage.value = msg
                onError(msg)
            }
        }
    }

    fun deleteFareBand(
        bandId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.deleteFareBand(bandId)
                _statusMessage.value = "Faixa de KM excluída."
                onSuccess()
            } catch (e: Exception) {
                val msg = "Erro ao excluir faixa de KM: ${e.localizedMessage}"
                _errorMessage.value = msg
                onError(msg)
            }
        }
    }

    // ==========================================
    // ROTAS / TABELA DE PREÇOS (ROUTES)
    // ==========================================
    fun saveRoute(
        route: Route,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.saveRoute(route, _fareBands.value)
            result.onSuccess { id ->
                _statusMessage.value = "Rota salva com sucesso!"
                onSuccess(id)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Erro ao salvar rota."
                _errorMessage.value = msg
                onError(msg)
            }
        }
    }

    fun deleteRoute(
        routeId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.deleteRoute(routeId)
                _statusMessage.value = "Rota excluída com sucesso."
                onSuccess()
            } catch (e: Exception) {
                val msg = "Erro ao excluir rota: ${e.localizedMessage}"
                _errorMessage.value = msg
                onError(msg)
            }
        }
    }

    // ==========================================
    // USUÁRIOS DE EMPRESA (COMPANY USER AUTH)
    // ==========================================
    fun createCompanyUser(
        name: String,
        email: String,
        password: String,
        phone: String = "",
        companyId: String,
        companyName: String = "",
        onSuccess: (AppUser) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.createCompanyUserWithAuth(
                name = name,
                email = email,
                password = password,
                phone = phone,
                companyId = companyId,
                companyName = companyName
            )
            result.onSuccess { user ->
                _uiState.value = UiState.Success("Usuário corporativo criado com sucesso!")
                _statusMessage.value = "Usuário corporativo cadastrado com sucesso!"
                onSuccess(user)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Falha ao criar usuário corporativo."
                _uiState.value = UiState.Error(msg)
                _errorMessage.value = msg
                onError(msg)
            }
        }
    }

    // ==========================================
    // PASSAGEIROS DA EMPRESA (COMPANY PORTAL)
    // ==========================================
    fun saveCompanyPassenger(
        passenger: Passenger,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = _currentUser.value
        if (user == null || user.companyId.isBlank()) {
            val msg = "Empresa não autenticada."
            _errorMessage.value = msg
            onError(msg)
            return
        }
        if (passenger.name.isBlank()) {
            val msg = "Nome do passageiro é obrigatório."
            _errorMessage.value = msg
            onError(msg)
            return
        }

        viewModelScope.launch {
            try {
                val id = repository.savePassengerForCompany(passenger, user.companyId)
                _statusMessage.value = "Passageiro salvo com sucesso!"
                onSuccess(id)
            } catch (e: Exception) {
                val msg = "Erro ao salvar passageiro: ${e.localizedMessage}"
                _errorMessage.value = msg
                onError(msg)
            }
        }
    }

    fun deleteCompanyPassenger(
        passengerId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.deletePassenger(passengerId)
                _statusMessage.value = "Passageiro removido com sucesso."
                onSuccess()
            } catch (e: Exception) {
                val msg = "Erro ao remover passageiro: ${e.localizedMessage}"
                _errorMessage.value = msg
                onError(msg)
            }
        }
    }

    // ==========================================
    // SOLICITAÇÃO DE VIAGEM PELA EMPRESA
    // ==========================================
    fun requestTripByCompany(
        passenger: Passenger,
        route: Route,
        additionalPassengers: List<String> = emptyList(),
        scheduledTime: Long,
        notes: String = "",
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = _currentUser.value
        if (user == null || user.companyId.isBlank()) {
            val msg = "Empresa não identificada na sessão."
            _errorMessage.value = msg
            onError(msg)
            return
        }
        if (passenger.id.isBlank() && passenger.name.isBlank()) {
            val msg = "Selecione um passageiro."
            _errorMessage.value = msg
            onError(msg)
            return
        }
        if (route.origin.isBlank() || route.destination.isBlank()) {
            val msg = "Origem e destino da rota são obrigatórios."
            _errorMessage.value = msg
            onError(msg)
            return
        }
        val finalRoute = if (route.id.isBlank()) {
            route.copy(id = UUID.randomUUID().toString())
        } else {
            route
        }
        if (finalRoute.price <= 0.0) {
            val msg = "O preço da viagem deve ser maior que zero."
            _errorMessage.value = msg
            onError(msg)
            return
        }

        viewModelScope.launch {
            try {
                // Recupera dados de pagamento da empresa
                val company = _companies.value.find { it.id == user.companyId }
                val termSnapshot = company?.paymentTerm ?: PaymentTerms.A_VISTA
                val meansSnapshot = company?.paymentMeans ?: PaymentMeans.PIX

                // Assegura passageiro persistido
                val finalPassenger = if (passenger.id.isBlank()) {
                    repository.findOrCreatePassenger(passenger.name, passenger.phone, user.companyId)
                } else {
                    passenger
                }

                val trip = Trip(
                    companyId = user.companyId,
                    companyName = user.companyName.ifBlank { company?.name ?: "Empresa" },
                    passengerId = finalPassenger.id,
                    passengerName = finalPassenger.name,
                    passengerPhone = finalPassenger.phone,
                    additionalPassengers = additionalPassengers,
                    origin = finalRoute.origin,
                    destination = finalRoute.destination,
                    stops = emptyList(),
                    scheduledTime = scheduledTime,
                    status = TripStatus.PENDING,
                    price = finalRoute.price,
                    driverCommission = 0.0, // Admin define na atribuição
                    requestedByCompany = true,
                    requestedByUserId = user.id,
                    routeId = finalRoute.id,
                    distanceKm = finalRoute.distanceKm,
                    pricingMode = finalRoute.pricingMode,
                    fareBandId = finalRoute.fareBandId,
                    multiplierSnapshot = finalRoute.multiplierSnapshot,
                    paymentTermSnapshot = termSnapshot,
                    paymentMeansSnapshot = meansSnapshot,
                    notes = notes.trim(),
                    companyPaymentStatus = PaymentStatus.PENDING,
                    paymentStatus = PaymentStatus.PENDING,
                    createdAt = System.currentTimeMillis()
                )

                val tripId = repository.saveTrip(trip)
                // Cenário 2: Viagem criada pela empresa
                repository.notifyTripCreatedByCompany(trip.copy(id = tripId))
                _statusMessage.value = "Viagem solicitada com sucesso!"
                onSuccess(tripId)
            } catch (e: Exception) {
                val msg = "Erro ao solicitar viagem: ${e.localizedMessage}"
                _errorMessage.value = msg
                onError(msg)
            }
        }
    }

    fun cancelTripByCompany(
        tripId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = _currentUser.value
        if (user == null) {
            val msg = "Usuário não autenticado."
            _errorMessage.value = msg
            onError(msg)
            return
        }

        val isAdmin = user.role.equals(UserRole.ADMIN, ignoreCase = true)
        if (!isAdmin && user.companyId.isBlank()) {
            val msg = "Empresa não autenticada."
            _errorMessage.value = msg
            onError(msg)
            return
        }

        viewModelScope.launch {
            val trip = _trips.value.find { it.id == tripId }
                ?: _companyTrips.value.find { it.id == tripId }
                ?: repository.getTripById(tripId)

            val result = if (isAdmin) {
                repository.adminCancelTrip(tripId)
            } else {
                repository.cancelTripIfCompanyAllowed(tripId, user.companyId, isAdmin = false)
            }

            result.onSuccess {
                _trips.value = _trips.value.map { if (it.id == tripId) it.copy(status = TripStatus.CANCELLED) else it }
                _companyTrips.value = _companyTrips.value.map { if (it.id == tripId) it.copy(status = TripStatus.CANCELLED) else it }
                _driverTrips.value = _driverTrips.value.map { if (it.id == tripId) it.copy(status = TripStatus.CANCELLED) else it }

                if (trip != null) {
                    val role = if (isAdmin) UserRole.ADMIN else UserRole.COMPANY
                    repository.notifyTripCancelled(trip.copy(status = TripStatus.CANCELLED), role, user.name)
                }
                _statusMessage.value = "Viagem cancelada com sucesso."
                onSuccess()
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Erro ao cancelar viagem."
                _errorMessage.value = msg
                onError(msg)
            }
        }
    }

    private fun formatDateTime(millis: Long): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(millis))
    }
}
