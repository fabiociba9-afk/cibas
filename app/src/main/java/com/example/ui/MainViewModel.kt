package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.ExecutivoGoDatabase
import com.example.data.entity.*
import com.example.data.repository.ExecutivoGoRepository
import com.example.location.LocationTracker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ExecutivoGoDatabase.getInstance(application)
    val repository = ExecutivoGoRepository(db)
    val locationTracker = LocationTracker(application, repository)

    // Current Session
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    val userCount: StateFlow<Int?> = repository.userCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _superAdminCreationSuccess = MutableStateFlow<String?>(null)
    val superAdminCreationSuccess: StateFlow<String?> = _superAdminCreationSuccess.asStateFlow()

    // Base Entities from DB
    val companies: StateFlow<List<CompanyEntity>> = repository.allCompanies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val drivers: StateFlow<List<DriverEntity>> = repository.allDrivers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paymentMethods: StateFlow<List<PaymentMethodEntity>> = repository.allPaymentMethods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trips: StateFlow<List<TripEntity>> = repository.allTrips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Tab / Navigation State
    private val _selectedNavTab = MutableStateFlow("dashboard")
    val selectedNavTab: StateFlow<String> = _selectedNavTab.asStateFlow()

    // Filters for Trips
    private val _tripStatusFilter = MutableStateFlow<String?>("TODOS")
    val tripStatusFilter: StateFlow<String?> = _tripStatusFilter.asStateFlow()

    private val _tripCompanyFilter = MutableStateFlow<Long?>(null)
    val tripCompanyFilter: StateFlow<Long?> = _tripCompanyFilter.asStateFlow()

    private val _tripDriverFilter = MutableStateFlow<Long?>(null)
    val tripDriverFilter: StateFlow<Long?> = _tripDriverFilter.asStateFlow()

    private val _tripSearchQuery = MutableStateFlow("")
    val tripSearchQuery: StateFlow<String> = _tripSearchQuery.asStateFlow()

    // Period filter for Financial: "TODOS", "HOJE", "SEMANA", "MES"
    private val _financialPeriodFilter = MutableStateFlow("MES")
    val financialPeriodFilter: StateFlow<String> = _financialPeriodFilter.asStateFlow()

    init {
        // App starts with empty database and no auto-logged-in session.
        _currentUser.value = null
    }

    fun clearSuperAdminCreationSuccess() {
        _superAdminCreationSuccess.value = null
    }

    fun createInitialSuperAdmin(
        name: String,
        emailOrLogin: String,
        phone: String,
        pass: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val admin = UserEntity(
                name = name.trim(),
                email = emailOrLogin.trim(),
                phone = phone.trim(),
                role = UserRole.SUPER_ADMIN.name,
                companyId = null,
                jobTitle = "Super Administrador Geral",
                driverId = null,
                password = pass.trim(),
                isActive = true
            )
            repository.insertUser(admin)
            _superAdminCreationSuccess.value = "Super Administrador cadastrado com sucesso! Faça seu login para continuar."
            onSuccess()
        }
    }

    fun setNavTab(tab: String) {
        _selectedNavTab.value = tab
    }

    // --- Authentication ---
    fun login(loginOrPhone: String, pass: String) {
        viewModelScope.launch {
            _loginError.value = null
            val user = repository.authenticate(loginOrPhone, pass)
            if (user != null) {
                if (!user.isActive) {
                    _loginError.value = "Este usuário foi desativado pelo administrador."
                } else {
                    _currentUser.value = user
                    _loginError.value = null
                    // If driver, start continuous tracking simulation or GPS
                    if (user.role == UserRole.DRIVER.name && user.driverId != null) {
                        locationTracker.startContinuousTracking(user.driverId, false)
                    }
                }
            } else {
                _loginError.value = "Credenciais inválidas. Verifique o e-mail/telefone e a senha."
            }
        }
    }

    fun switchProfile(userRole: UserRole) {
        viewModelScope.launch {
            val userList = repository.allUsers.first()
            val target = userList.firstOrNull { it.role == userRole.name }
            if (target != null) {
                _currentUser.value = target
                if (target.role == UserRole.DRIVER.name && target.driverId != null) {
                    locationTracker.startContinuousTracking(target.driverId, false)
                } else {
                    locationTracker.stopTracking()
                }
            }
        }
    }

    fun quickLoginAsRole(userRole: UserRole) {
        viewModelScope.launch {
            _loginError.value = null
            var userList = repository.allUsers.first()
            var target = userList.firstOrNull { it.role == userRole.name && it.isActive }
            if (target == null) {
                // Ensure default company exists if needed
                var companyList = repository.allCompanies.first()
                val companyId = if (companyList.isNotEmpty()) {
                    companyList.first().id
                } else {
                    val company = CompanyEntity(
                        name = "Alpha Corporativo S.A.",
                        cnpj = "12.345.678/0001-90",
                        phone = "(11) 3100-2000",
                        address = "Av. Paulista, 1000 - Bela Vista",
                        isActive = true
                    )
                    repository.insertCompany(company)
                }

                // If driver, ensure driver record exists
                var driverId: Long? = null
                if (userRole == UserRole.DRIVER) {
                    val driverList = repository.allDrivers.first()
                    driverId = if (driverList.isNotEmpty()) {
                        driverList.first().id
                    } else {
                        val newDriver = DriverEntity(
                            fullName = "Carlos Eduardo Santos",
                            phone = "(11) 98111-2233",
                            vehiclePlate = "BRA2E19",
                            vehicleModel = "Toyota Corolla Preto",
                            commissionPercentage = 20.0,
                            isActive = true,
                            latitude = -23.5874,
                            longitude = -46.6823,
                            isOnline = true
                        )
                        repository.insertDriver(newDriver)
                    }
                }

                val newUser = when (userRole) {
                    UserRole.SUPER_ADMIN -> UserEntity(
                        name = "Administrador Geral",
                        email = "admin@executivogo.com",
                        phone = "(11) 99000-0001",
                        role = UserRole.SUPER_ADMIN.name,
                        companyId = null,
                        jobTitle = "Super Administrador Master",
                        driverId = null,
                        password = "admin",
                        isActive = true
                    )
                    UserRole.COMPANY_ADMIN -> UserEntity(
                        name = "Mariana Gestora",
                        email = "gestao@alphacorp.com",
                        phone = "(11) 99000-0002",
                        role = UserRole.COMPANY_ADMIN.name,
                        companyId = companyId,
                        jobTitle = "Gestora Corporativa de Frotas",
                        driverId = null,
                        password = "admin",
                        isActive = true
                    )
                    UserRole.DRIVER -> UserEntity(
                        name = "Carlos Eduardo Santos",
                        email = "carlos.motorista@executivogo.com",
                        phone = "(11) 98111-2233",
                        role = UserRole.DRIVER.name,
                        companyId = null,
                        jobTitle = "Motorista Executivo Parceiro",
                        driverId = driverId,
                        password = "driver",
                        isActive = true
                    )
                    UserRole.COMPANY_USER -> UserEntity(
                        name = "Roberto Diretor",
                        email = "roberto.cliente@alphacorp.com",
                        phone = "(11) 99000-0004",
                        role = UserRole.COMPANY_USER.name,
                        companyId = companyId,
                        jobTitle = "Diretor Executivo Comercial",
                        driverId = null,
                        password = "user",
                        isActive = true
                    )
                }
                val createdId = repository.insertUser(newUser)
                target = repository.getUserById(createdId) ?: newUser.copy(id = createdId)
            }

            _currentUser.value = target
            if (target.role == UserRole.DRIVER.name && target.driverId != null) {
                locationTracker.startContinuousTracking(target.driverId, false)
            } else {
                locationTracker.stopTracking()
            }
        }
    }

    fun logout() {
        locationTracker.stopTracking()
        _currentUser.value = null
    }

    // --- Companies CRUD ---
    fun saveCompany(id: Long = 0, name: String, cnpj: String, phone: String, address: String, isActive: Boolean) {
        viewModelScope.launch {
            val entity = CompanyEntity(
                id = id,
                name = name.trim(),
                cnpj = cnpj.trim(),
                phone = phone.trim(),
                address = address.trim(),
                isActive = isActive
            )
            if (id == 0L) {
                repository.insertCompany(entity)
            } else {
                repository.updateCompany(entity)
            }
        }
    }

    fun toggleCompanyStatus(company: CompanyEntity) {
        viewModelScope.launch {
            repository.updateCompany(company.copy(isActive = !company.isActive))
        }
    }

    fun deleteCompany(company: CompanyEntity) {
        viewModelScope.launch {
            repository.deleteCompany(company)
        }
    }

    // --- Users CRUD ---
    fun saveUser(
        id: Long = 0,
        name: String,
        email: String,
        phone: String,
        role: UserRole,
        companyId: Long?,
        jobTitle: String?,
        driverId: Long? = null,
        password: String = "123456",
        isActive: Boolean = true
    ) {
        viewModelScope.launch {
            val entity = UserEntity(
                id = id,
                name = name.trim(),
                email = email.trim(),
                phone = phone.trim(),
                role = role.name,
                companyId = companyId,
                jobTitle = jobTitle?.trim(),
                driverId = driverId,
                password = password.trim().ifEmpty { "123456" },
                isActive = isActive
            )
            if (id == 0L) {
                repository.insertUser(entity)
            } else {
                repository.updateUser(entity)
            }
        }
    }

    fun toggleUserStatus(user: UserEntity) {
        viewModelScope.launch {
            repository.updateUser(user.copy(isActive = !user.isActive))
        }
    }

    fun deleteUser(user: UserEntity) {
        viewModelScope.launch {
            repository.deleteUser(user)
        }
    }

    // --- Drivers CRUD ---
    fun saveDriver(
        id: Long = 0,
        fullName: String,
        phone: String,
        vehiclePlate: String,
        vehicleModel: String,
        commissionPercentage: Double,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            val existing = if (id != 0L) repository.getDriverById(id) else null
            val entity = DriverEntity(
                id = id,
                fullName = fullName.trim(),
                phone = phone.trim(),
                vehiclePlate = vehiclePlate.trim().uppercase(),
                vehicleModel = vehicleModel.trim(),
                commissionPercentage = commissionPercentage,
                isActive = isActive,
                latitude = existing?.latitude ?: -23.5874,
                longitude = existing?.longitude ?: -46.6823,
                locationUpdatedAt = System.currentTimeMillis(),
                isOnline = existing?.isOnline ?: true
            )
            if (id == 0L) {
                val newId = repository.insertDriver(entity)
                // Also create a linked user account for this driver if not exists
                repository.insertUser(
                    UserEntity(
                        name = entity.fullName,
                        email = "${entity.fullName.lowercase().replace(" ", ".")}@executivogo.com",
                        phone = entity.phone,
                        role = UserRole.DRIVER.name,
                        driverId = newId,
                        jobTitle = "Motorista Parceiro",
                        password = "123456"
                    )
                )
            } else {
                repository.updateDriver(entity)
            }
        }
    }

    fun toggleDriverStatus(driver: DriverEntity) {
        viewModelScope.launch {
            repository.updateDriver(driver.copy(isActive = !driver.isActive))
        }
    }

    fun deleteDriver(driver: DriverEntity) {
        viewModelScope.launch {
            repository.deleteDriver(driver)
        }
    }

    // --- Payment Methods CRUD ---
    fun savePaymentMethod(id: Long = 0, name: String, description: String, isActive: Boolean = true) {
        viewModelScope.launch {
            val entity = PaymentMethodEntity(
                id = id,
                name = name.trim(),
                description = description.trim(),
                isActive = isActive
            )
            if (id == 0L) {
                repository.insertPaymentMethod(entity)
            } else {
                repository.updatePaymentMethod(entity)
            }
        }
    }

    fun deletePaymentMethod(method: PaymentMethodEntity) {
        viewModelScope.launch {
            repository.deletePaymentMethod(method)
        }
    }

    // --- Trips CRUD & Status Flow ---
    fun createTrip(
        company: CompanyEntity,
        requesterName: String,
        requesterUserId: Long,
        dateTimeMillis: Long,
        origin: String,
        destination: String,
        price: Double,
        paymentMethod: PaymentMethodEntity,
        clientPaymentDueDateMillis: Long,
        driver: DriverEntity,
        driverCommissionDueDateMillis: Long,
        notes: String
    ) {
        viewModelScope.launch {
            val commissionAmount = (price * (driver.commissionPercentage / 100.0))
            val trip = TripEntity(
                companyId = company.id,
                companyName = company.name,
                requesterUserId = requesterUserId,
                requesterName = requesterName,
                dateTimeMillis = dateTimeMillis,
                origin = origin.trim(),
                destination = destination.trim(),
                price = price,
                paymentMethodId = paymentMethod.id,
                paymentMethodName = paymentMethod.name,
                clientPaymentDueDateMillis = clientPaymentDueDateMillis,
                driverId = driver.id,
                driverName = driver.fullName,
                driverCommissionAmount = commissionAmount,
                driverCommissionDueDateMillis = driverCommissionDueDateMillis,
                status = TripStatus.AGENDADA.name,
                notes = notes.trim()
            )
            repository.insertTrip(trip)
        }
    }

    fun updateTrip(trip: TripEntity) {
        viewModelScope.launch {
            repository.updateTrip(trip)
        }
    }

    fun updateTripStatus(tripId: Long, newStatus: TripStatus) {
        viewModelScope.launch {
            repository.updateTripStatus(tripId, newStatus)
            if (newStatus == TripStatus.CONCLUIDA) {
                repository.markReceiptGenerated(tripId)
            }
        }
    }

    fun toggleDriverPaid(trip: TripEntity) {
        viewModelScope.launch {
            repository.updateDriverPaymentStatus(trip.id, !trip.isDriverPaid)
        }
    }

    fun toggleClientPaid(trip: TripEntity) {
        viewModelScope.launch {
            repository.updateClientPaymentStatus(trip.id, !trip.isClientPaid)
        }
    }

    fun deleteTrip(trip: TripEntity) {
        viewModelScope.launch {
            repository.deleteTrip(trip)
        }
    }

    // --- Trip Filters ---
    fun setTripStatusFilter(status: String?) {
        _tripStatusFilter.value = status
    }

    fun setTripCompanyFilter(companyId: Long?) {
        _tripCompanyFilter.value = companyId
    }

    fun setTripDriverFilter(driverId: Long?) {
        _tripDriverFilter.value = driverId
    }

    fun setTripSearchQuery(query: String) {
        _tripSearchQuery.value = query
    }

    fun setFinancialPeriod(period: String) {
        _financialPeriodFilter.value = period
    }
}
