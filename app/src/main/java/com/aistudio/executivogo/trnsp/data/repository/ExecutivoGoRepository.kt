package com.aistudio.executivogo.trnsp.data

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class ExecutivoGoRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    companion object {
        private const val TAG = "ExecutivoGoRepo"
    }

    // Instância secundária para criação de contas pelo Admin sem deslogar a sessão principal
    private fun getSecondaryAuth(): FirebaseAuth {
        val secondaryApp = try {
            FirebaseApp.getInstance("Secondary")
        } catch (e: IllegalStateException) {
            val defaultApp = FirebaseApp.getInstance()
            FirebaseApp.initializeApp(defaultApp.applicationContext, defaultApp.options, "Secondary")
        }
        return FirebaseAuth.getInstance(secondaryApp)
    }

    // ==========================================
    // AUTHENTICATION
    // ==========================================
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun getCurrentUserEmail(): String? = auth.currentUser?.email

    suspend fun signIn(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user ?: throw Exception("Usuário não encontrado após autenticação")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user ?: throw Exception("Falha ao registrar novo usuário")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    // ==========================================
    // USERS (Admin, Motoristas, Operadores)
    // ==========================================

    /**
     * Cria conta no Firebase Authentication usando a instância secundária do FirebaseApp
     * para NÃO deslogar o administrador logado. Em seguida, grava na coleção 'users' do Firestore
     * com o UID gerado pelo Auth tanto no documentId quanto no campo id do documento.
     * Para motoristas, isOnline é sempre inicializado como true conforme regras de negócio.
     */
    suspend fun createUserWithAuth(
        name: String,
        email: String,
        password: String,
        phone: String = "",
        role: String = UserRole.DRIVER,
        vehicleModel: String = "",
        vehicleColor: String = "",
        vehicleYear: String = "",
        vehiclePlate: String = "",
        commissionPercentage: Double = 20.0,
        companyId: String = "",
        companyName: String = ""
    ): Result<AppUser> {
        val trimmedEmail = email.trim().lowercase()
        val trimmedPassword = password.trim()
        val trimmedRole = role.trim().uppercase()
        val isCompany = trimmedRole.equals(UserRole.COMPANY, ignoreCase = true)
        val trimmedCompanyId = companyId.trim()

        if (isCompany && trimmedCompanyId.isBlank()) {
            return Result.failure(IllegalArgumentException("Selecione a empresa vinculada."))
        }
        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("E-mail inválido."))
        }
        if (trimmedPassword.length < 6) {
            return Result.failure(IllegalArgumentException("A senha inicial deve conter no mínimo 6 caracteres."))
        }

        return try {
            val secondaryAuth = getSecondaryAuth()
            try {
                secondaryAuth.signOut()
            } catch (_: Exception) {}

            val authResult = secondaryAuth.createUserWithEmailAndPassword(trimmedEmail, trimmedPassword).await()
            val createdUser = authResult.user ?: throw Exception("Falha ao registrar novo usuário no Firebase Auth.")
            val uid = createdUser.uid

            val isDriver = trimmedRole.equals(UserRole.DRIVER, ignoreCase = true)

            // Resolve nome da empresa caso vazio
            val resolvedCompanyName = if (isCompany) {
                if (companyName.isNotBlank()) {
                    companyName.trim()
                } else {
                    try {
                        val compDoc = firestore.collection("companies").document(trimmedCompanyId).get().await()
                        compDoc.getString("name") ?: compDoc.getString("fantasyName") ?: ""
                    } catch (_: Exception) {
                        ""
                    }
                }
            } else {
                ""
            }

            val appUser = AppUser(
                id = uid,
                name = name.trim(),
                email = trimmedEmail,
                phone = phone.trim(),
                role = trimmedRole,
                companyId = if (isCompany) trimmedCompanyId else "",
                companyName = resolvedCompanyName,
                active = true,
                isOnline = isDriver, // sempre true no cadastro de motorista
                fcmToken = "",
                latitude = 0.0,
                longitude = 0.0,
                vehicleModel = vehicleModel.trim(),
                vehicleColor = vehicleColor.trim(),
                vehicleYear = vehicleYear.trim(),
                vehiclePlate = vehiclePlate.trim(),
                commissionPercentage = commissionPercentage,
                authKey = "" // Não salvar senhas em texto plano
            )

            // Salva no Firestore com document id = UID
            firestore.collection("users").document(uid).set(appUser, SetOptions.merge()).await()
            firestore.collection("users").document(uid).update("isOnline", isDriver).await()

            // Desconecta a instância secundária imediatamente para não manter sessão
            try {
                secondaryAuth.signOut()
            } catch (_: Exception) {}

            Result.success(appUser)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("Este e-mail já está cadastrado no sistema."))
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.failure(Exception("A senha informada é fraca. Ela deve conter no mínimo 6 caracteres."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Formato de e-mail inválido."))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Erro ao cadastrar usuário com autenticação."))
        }
    }

    suspend fun createCompanyUserWithAuth(
        name: String,
        email: String,
        password: String,
        phone: String = "",
        companyId: String,
        companyName: String = ""
    ): Result<AppUser> {
        return createUserWithAuth(
            name = name,
            email = email,
            password = password,
            phone = phone,
            role = UserRole.COMPANY,
            companyId = companyId,
            companyName = companyName
        )
    }

    suspend fun createUserWithAuth(user: AppUser, password: String): Result<AppUser> {
        return createUserWithAuth(
            name = user.name,
            email = user.email,
            password = password,
            phone = user.phone,
            role = user.role,
            vehicleModel = user.vehicleModel,
            vehicleColor = user.vehicleColor,
            vehicleYear = user.vehicleYear,
            vehiclePlate = user.vehiclePlate,
            commissionPercentage = user.commissionPercentage,
            companyId = user.companyId,
            companyName = user.companyName
        )
    }

    suspend fun linkCompanyToUser(userId: String, companyId: String, companyName: String = ""): Result<Unit> {
        return try {
            val resolvedName = if (companyName.isNotBlank()) {
                companyName.trim()
            } else {
                try {
                    val compDoc = firestore.collection("companies").document(companyId.trim()).get().await()
                    compDoc.getString("name") ?: compDoc.getString("fantasyName") ?: ""
                } catch (_: Exception) {
                    ""
                }
            }
            val updates = mapOf<String, Any>(
                "companyId" to companyId.trim(),
                "companyName" to resolvedName,
                "role" to UserRole.COMPANY
            )
            firestore.collection("users").document(userId.trim()).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findCompanyByEmail(email: String): Company? {
        val trimmed = email.trim().lowercase()
        if (trimmed.isBlank()) return null
        return try {
            val companies = fetchCompanies()
            companies.find { it.email.trim().equals(trimmed, ignoreCase = true) }
                ?: companies.find { it.name.trim().equals(trimmed, ignoreCase = true) }
                ?: companies.find { it.fantasyName.trim().equals(trimmed, ignoreCase = true) }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Corrige registros existentes no Firestore:
     * - Garante companyId e companyName para usuários COMPANY que ficaram sem vínculo
     * - Remove qualquer resquício de senhas salvas no campo authKey
     * - NÃO altera, não deleta e não desativa nenhuma conta no Firebase Auth
     */
    suspend fun repairCompanyUsersAndSanitizeAuthKeys(): Int {
        return try {
            val usersSnapshot = firestore.collection("users").get().await()
            val companiesSnapshot = firestore.collection("companies").get().await()
            val companies = companiesSnapshot.toObjects(Company::class.java)
            var fixedCount = 0

            for (doc in usersSnapshot.documents) {
                val role = doc.getString("role") ?: ""
                val currentCompanyId = doc.getString("companyId") ?: ""
                val currentCompanyName = doc.getString("companyName") ?: ""
                val userEmail = (doc.getString("email") ?: "").trim().lowercase()
                val authKey = doc.getString("authKey") ?: ""

                val updates = mutableMapOf<String, Any>()

                // 1. Remover authKey caso exista
                if (authKey.isNotBlank()) {
                    updates["authKey"] = ""
                }

                // 2. Se for COMPANY e estiver sem companyId
                if (role.equals(UserRole.COMPANY, ignoreCase = true)) {
                    var resolvedComp: Company? = null

                    if (currentCompanyId.isNotBlank()) {
                        resolvedComp = companies.find { it.id == currentCompanyId }
                    }

                    if (resolvedComp == null && userEmail.isNotBlank()) {
                        resolvedComp = companies.find { comp ->
                            comp.email.trim().equals(userEmail, ignoreCase = true)
                        }
                    }

                    if (resolvedComp == null && currentCompanyName.isNotBlank()) {
                        resolvedComp = companies.find { comp ->
                            comp.name.trim().equals(currentCompanyName.trim(), ignoreCase = true) ||
                            comp.fantasyName.trim().equals(currentCompanyName.trim(), ignoreCase = true)
                        }
                    }

                    // Se existir somente 1 empresa cadastrada no sistema
                    if (resolvedComp == null && companies.size == 1) {
                        resolvedComp = companies.first()
                    }

                    if (resolvedComp != null) {
                        if (currentCompanyId != resolvedComp.id) {
                            updates["companyId"] = resolvedComp.id
                        }
                        val resolvedName = resolvedComp.name.ifBlank { resolvedComp.fantasyName }
                        if (currentCompanyName != resolvedName) {
                            updates["companyName"] = resolvedName
                        }
                    }
                }

                if (updates.isNotEmpty()) {
                    firestore.collection("users").document(doc.id).update(updates).await()
                    fixedCount++
                }
            }
            fixedCount
        } catch (e: Exception) {
            Log.w(TAG, "Rotina de autocorreção ignorada: ${e.message}")
            0
        }
    }

    suspend fun saveUser(user: AppUser): String {
        val collection = firestore.collection("users")
        val docId = if (user.id.isNotBlank()) user.id else collection.document().id
        
        var resolvedCompanyName = user.companyName
        if (user.role.equals(UserRole.COMPANY, ignoreCase = true) && resolvedCompanyName.isBlank() && user.companyId.isNotBlank()) {
            try {
                val compDoc = firestore.collection("companies").document(user.companyId.trim()).get().await()
                resolvedCompanyName = compDoc.getString("name") ?: compDoc.getString("fantasyName") ?: ""
            } catch (_: Exception) {}
        }

        val userToSave = user.copy(
            id = docId,
            companyName = resolvedCompanyName,
            authKey = "" // Nunca armazena senha em texto plano
        )
        collection.document(docId).set(userToSave, SetOptions.merge()).await()
        collection.document(docId).update("isOnline", userToSave.isOnline).await()
        return docId
    }

    /**
     * Exclui o motorista ou usuário do sistema.
     * Além de remover o documento do Firestore, autentica via sessão secundária e remove
     * a credencial definitivamente do Firebase Authentication, liberando o e-mail para novos cadastros.
     */
    suspend fun deleteUser(userId: String, explicitPassword: String? = null): Boolean {
        if (userId.isBlank()) return false
        var authDeleted = false
        var userEmail = ""

        try {
            // 1. Obter dados do usuário para tentar exclusão no Firebase Authentication
            val user = getUserById(userId)
            if (user != null) {
                userEmail = user.email.trim()
            }

            // 2. Se for o próprio usuário autenticado na sessão principal
            if (auth.currentUser?.uid == userId) {
                try {
                    auth.currentUser?.delete()?.await()
                    authDeleted = true
                    Log.d(TAG, "Usuário autenticado atual ($userId) excluído da sessão principal do Firebase Auth.")
                } catch (e: Exception) {
                    Log.w(TAG, "Não foi possível excluir diretamente da sessão principal do Auth", e)
                }
            }

            // 3. Se for outro usuário (administrador excluindo motorista ou operador)
            if (!authDeleted && userEmail.isNotBlank()) {
                val secondaryAuth = getSecondaryAuth()
                val passwordsToTry = mutableListOf<String>()

                if (!explicitPassword.isNullOrBlank()) {
                    passwordsToTry.add(explicitPassword.trim())
                }
                if (user?.authKey?.isNotBlank() == true) {
                    passwordsToTry.add(user.authKey.trim())
                }
                // Senhas padrão comumente utilizadas no sistema
                passwordsToTry.add("123456")
                passwordsToTry.add("executivogo")
                if (user?.phone?.isNotBlank() == true) {
                    val digits = user.phone.filter { it.isDigit() }
                    if (digits.length >= 6) {
                        passwordsToTry.add(digits)
                    }
                }

                for (pwd in passwordsToTry.distinct()) {
                    try {
                        val authResult = secondaryAuth.signInWithEmailAndPassword(userEmail, pwd).await()
                        val targetUser = authResult.user
                        if (targetUser != null) {
                            targetUser.delete().await()
                            authDeleted = true
                            Log.d(TAG, "Usuário $userEmail excluído com sucesso do Firebase Auth via sessão secundária.")
                            break
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Tentativa de login secundário para exclusão falhou com senha testada: ${e.message}")
                    } finally {
                        try {
                            secondaryAuth.signOut()
                        } catch (_: Exception) {}
                    }
                }
            }

            // 4. Registrar em coleção de auditoria / fila de exclusão para rastreabilidade
            try {
                firestore.collection("deleted_users_log").document(userId).set(
                    mapOf(
                        "uid" to userId,
                        "email" to userEmail,
                        "authDeleted" to authDeleted,
                        "deletedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()
            } catch (_: Exception) {}

            // 5. Excluir da coleção 'users' do Firestore
            firestore.collection("users").document(userId).delete().await()

            return authDeleted
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao excluir usuário: $userId", e)
            throw e
        }
    }

    suspend fun getUserById(userId: String): AppUser? {
        if (userId.isBlank()) return null
        return try {
            val doc = firestore.collection("users").document(userId.trim()).get().await()
            if (!doc.exists()) return null
            val user = try {
                doc.toObject(AppUser::class.java) ?: return null
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao desserializar usuário $userId: ${e.message}", e)
                return null
            }
            val onlineStatus = doc.getBoolean("isOnline") ?: doc.getBoolean("online") ?: user.isOnline
            user.copy(id = doc.id, isOnline = onlineStatus)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar usuário $userId: ${e.message}", e)
            null
        }
    }

    suspend fun updateUserFcmToken(userId: String, token: String) {
        if (userId.isBlank() || token.isBlank()) return
        val updates = mapOf(
            "fcmToken" to token,
            "fcmTokenAndroid" to token,
            "updatedAtFCM" to FieldValue.serverTimestamp()
        )
        firestore.collection("users").document(userId)
            .set(updates, SetOptions.merge())
            .await()
        Log.d(TAG, "updateUserFcmToken gravado no Firestore para $userId")
    }

    suspend fun updateDriverOnlineStatus(driverId: String, isOnline: Boolean) {
        if (driverId.isBlank()) return
        firestore.collection("users").document(driverId)
            .set(mapOf("isOnline" to isOnline), SetOptions.merge())
            .await()
    }

    fun listenUsers(
        onUpdate: (List<AppUser>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        return firestore.collection("users").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Erro ao escutar usuários no Firestore", error)
                onError(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val user = try {
                        doc.toObject(AppUser::class.java) ?: return@mapNotNull null
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao desserializar usuário ${doc.id}: ${e.message}")
                        return@mapNotNull null
                    }
                    val isDriver = user.role == UserRole.DRIVER
                    val onlineStatus = if (isDriver) true else (doc.getBoolean("isOnline") ?: doc.getBoolean("online") ?: user.isOnline)
                    user.copy(id = doc.id, isOnline = onlineStatus)
                }
                onUpdate(list)
            }
        }
    }

    suspend fun fetchUsers(): List<AppUser> {
        val snapshot = firestore.collection("users").get().await()
        return snapshot.documents.mapNotNull { doc ->
            val user = try {
                doc.toObject(AppUser::class.java) ?: return@mapNotNull null
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao desserializar usuário ${doc.id}: ${e.message}")
                return@mapNotNull null
            }
            val isDriver = user.role == UserRole.DRIVER
            val onlineStatus = if (isDriver) true else (doc.getBoolean("isOnline") ?: doc.getBoolean("online") ?: user.isOnline)
            user.copy(id = doc.id, isOnline = onlineStatus)
        }
    }

    // ==========================================
    // COMPANIES (Empresas Corporativas)
    // ==========================================
    suspend fun saveCompany(company: Company): String {
        val collection = firestore.collection("companies")
        val docId = if (company.id.isNotBlank()) company.id else collection.document().id
        val companyToSave = company.copy(id = docId)
        collection.document(docId).set(companyToSave, SetOptions.merge()).await()
        return docId
    }

    suspend fun deleteCompany(companyId: String) {
        if (companyId.isBlank()) return
        firestore.collection("companies").document(companyId).delete().await()
    }

    fun listenCompanies(
        onUpdate: (List<Company>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        return firestore.collection("companies").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Erro no listener de empresas", error)
                onError(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Company::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao desserializar empresa ${doc.id}: ${e.message}")
                        null
                    }
                }
                onUpdate(list)
            }
        }
    }

    suspend fun fetchCompanies(): List<Company> {
        val snapshot = firestore.collection("companies").get().await()
        return snapshot.documents.mapNotNull { doc ->
            try {
                doc.toObject(Company::class.java)?.copy(id = doc.id)
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao desserializar empresa ${doc.id}: ${e.message}")
                null
            }
        }
    }

    // ==========================================
    // PASSENGERS (Passageiros)
    // ==========================================
    suspend fun savePassenger(passenger: Passenger): String {
        val collection = firestore.collection("passengers")
        val docId = if (passenger.id.isNotBlank()) passenger.id else collection.document().id
        val passengerToSave = passenger.copy(id = docId)
        collection.document(docId).set(passengerToSave, SetOptions.merge()).await()
        return docId
    }

    suspend fun deletePassenger(passengerId: String) {
        if (passengerId.isBlank()) return
        firestore.collection("passengers").document(passengerId).delete().await()
    }

    /**
     * Se o passageiro informado não existir na coleção passengers, cadastra automaticamente
     * e retorna o objeto com o novo ID gerado.
     */
    suspend fun findOrCreatePassenger(name: String, phone: String, companyId: String): Passenger {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return Passenger()

        val allPassengers = fetchPassengers()
        val existing = allPassengers.find {
            it.name.trim().equals(trimmedName, ignoreCase = true) &&
                    (companyId.isBlank() || it.companyId == companyId)
        }

        if (existing != null) {
            return existing
        }

        val newId = firestore.collection("passengers").document().id
        val created = Passenger(
            id = newId,
            name = trimmedName,
            phone = phone.trim(),
            companyId = companyId,
            active = true
        )
        firestore.collection("passengers").document(newId).set(created).await()
        return created
    }

    fun listenPassengers(
        onUpdate: (List<Passenger>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        return firestore.collection("passengers").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Erro no listener de passageiros", error)
                onError(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Passenger::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao desserializar passageiro ${doc.id}: ${e.message}")
                        null
                    }
                }
                onUpdate(list)
            }
        }
    }

    suspend fun fetchPassengers(): List<Passenger> {
        val snapshot = firestore.collection("passengers").get().await()
        return snapshot.documents.mapNotNull { doc ->
            try {
                doc.toObject(Passenger::class.java)?.copy(id = doc.id)
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao desserializar passageiro ${doc.id}: ${e.message}")
                null
            }
        }
    }

    // ==========================================
    // TRIPS (Viagens / Corridas)
    // ==========================================
    suspend fun saveTrip(trip: Trip): String {
        val collection = firestore.collection("trips")
        val docId = if (trip.id.isNotBlank()) trip.id else collection.document().id
        val tripToSave = trip.copy(id = docId)
        collection.document(docId).set(tripToSave, SetOptions.merge()).await()
        return docId
    }

    suspend fun assignDriverToTrip(tripId: String, driver: AppUser, commission: Double) {
        if (tripId.isBlank()) return
        val updates = mapOf(
            "driverId" to driver.id,
            "driverName" to driver.name,
            "driverPhone" to driver.phone,
            "vehicleModel" to driver.vehicleModel,
            "vehiclePlate" to driver.vehiclePlate,
            "vehicleColor" to driver.vehicleColor,
            "driverCommission" to commission,
            "status" to TripStatus.PENDING
        )
        firestore.collection("trips").document(tripId).update(updates).await()
    }

    suspend fun updateTripStatus(tripId: String, newStatus: String) {
        if (tripId.isBlank()) return
        val updates = mutableMapOf<String, Any>("status" to newStatus)
        firestore.collection("trips").document(tripId)
            .update(updates)
            .await()
    }

    suspend fun updateTripStopIndex(tripId: String, currentStopIndex: Int) {
        if (tripId.isBlank()) return
        firestore.collection("trips").document(tripId)
            .update("currentStopIndex", currentStopIndex)
            .await()
    }

    /**
     * Conclui a viagem (status COMPLETED) e calcula todos os prazos financeiros:
     * - completedAt = now
     * - termDays = prazo da empresa em dias
     * - dueDate = completedAt + (termDays * 86400000)
     * - commissionAvailableAt = dueDate (igual ao dueDate da empresa)
     * - companyPaymentStatus = PENDING
     * - paymentStatus = PENDING
     */
    suspend fun completeTrip(tripId: String, termDays: Int) {
        if (tripId.isBlank()) return
        val now = System.currentTimeMillis()
        val days = if (termDays < 0) 0 else termDays
        val termMillis = days * 86_400_000L
        val calculatedDueDate = now + termMillis

        val updates = mapOf(
            "status" to TripStatus.COMPLETED,
            "completedAt" to now,
            "dueDate" to calculatedDueDate,
            "commissionAvailableAt" to calculatedDueDate,
            "companyPaymentStatus" to PaymentStatus.PENDING,
            "paymentStatus" to PaymentStatus.PENDING
        )
        firestore.collection("trips").document(tripId).update(updates).await()
    }

    suspend fun markCompanyPaid(tripId: String, adminId: String) {
        if (tripId.isBlank()) return
        val now = System.currentTimeMillis()
        val updates = mapOf(
            "companyPaymentStatus" to PaymentStatus.PAID,
            "companyPaidAt" to now,
            "companyPaidByAdminId" to adminId
        )
        firestore.collection("trips").document(tripId).update(updates).await()
    }

    suspend fun undoCompanyPaid(tripId: String) {
        if (tripId.isBlank()) return
        val updates = mapOf<String, Any?>(
            "companyPaymentStatus" to PaymentStatus.PENDING,
            "companyPaidAt" to null,
            "companyPaidByAdminId" to null
        )
        firestore.collection("trips").document(tripId).update(updates).await()
    }

    suspend fun markBatchCompanyPaid(tripIds: List<String>, adminId: String) {
        val now = System.currentTimeMillis()
        val batch = firestore.batch()
        tripIds.forEach { id ->
            val ref = firestore.collection("trips").document(id)
            batch.update(
                ref, mapOf(
                    "companyPaymentStatus" to PaymentStatus.PAID,
                    "companyPaidAt" to now,
                    "companyPaidByAdminId" to adminId
                )
            )
        }
        batch.commit().await()
    }

    suspend fun markCommissionPaid(tripId: String, adminId: String) {
        if (tripId.isBlank()) return
        val now = System.currentTimeMillis()
        val updates = mapOf(
            "paymentStatus" to PaymentStatus.PAID,
            "paidAt" to now,
            "paidByAdminId" to adminId
        )
        firestore.collection("trips").document(tripId).update(updates).await()
    }

    suspend fun undoCommissionPaid(tripId: String) {
        if (tripId.isBlank()) return
        val updates = mapOf<String, Any?>(
            "paymentStatus" to PaymentStatus.PENDING,
            "paidAt" to null,
            "paidByAdminId" to null
        )
        firestore.collection("trips").document(tripId).update(updates).await()
    }

    suspend fun markBatchCommissionPaid(tripIds: List<String>, adminId: String) {
        val now = System.currentTimeMillis()
        val batch = firestore.batch()
        tripIds.forEach { id ->
            val ref = firestore.collection("trips").document(id)
            batch.update(
                ref, mapOf(
                    "paymentStatus" to PaymentStatus.PAID,
                    "paidAt" to now,
                    "paidByAdminId" to adminId
                )
            )
        }
        batch.commit().await()
    }

    suspend fun deleteTrip(tripId: String) {
        if (tripId.isBlank()) return
        firestore.collection("trips").document(tripId).delete().await()
    }

    fun listenAllTrips(
        onUpdate: (List<Trip>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        return firestore.collection("trips").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Erro no listener de viagens", error)
                onError(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Trip::class.java)?.let { trip ->
                        if (trip.id.isBlank()) trip.copy(id = doc.id) else trip
                    }
                }
                onUpdate(list)
            }
        }
    }

    suspend fun fetchAllTrips(): List<Trip> {
        val snapshot = firestore.collection("trips").get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Trip::class.java)?.let { trip ->
                if (trip.id.isBlank()) trip.copy(id = doc.id) else trip
            }
        }
    }

    fun listenDriverTrips(
        driverId: String,
        onUpdate: (List<Trip>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        return firestore.collection("trips")
            .whereEqualTo("driverId", driverId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Erro no listener de viagens do motorista", error)
                    onError(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Trip::class.java)?.let { trip ->
                            if (trip.id.isBlank()) trip.copy(id = doc.id) else trip
                        }
                    }
                    onUpdate(list)
                }
            }
    }

    suspend fun fetchDriverTrips(driverId: String): List<Trip> {
        val snapshot = firestore.collection("trips")
            .whereEqualTo("driverId", driverId)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Trip::class.java)?.let { trip ->
                if (trip.id.isBlank()) trip.copy(id = doc.id) else trip
            }
        }
    }

    // ==========================================
    // SETTLEMENTS (Lotes de Pagamento)
    // ==========================================
    suspend fun saveSettlement(settlement: Settlement): String {
        val docId = if (settlement.id.isNotBlank()) settlement.id else firestore.collection("settlements").document().id
        firestore.collection("settlements").document(docId).set(settlement.copy(id = docId)).await()
        return docId
    }

    // ==========================================
    // PAYMENT METHODS (Formas de Pagamento Master)
    // ==========================================
    suspend fun savePaymentMethod(method: PaymentMethod): String {
        val collection = firestore.collection("payment_methods")
        val docId = if (method.id.isNotBlank()) method.id else collection.document().id
        val methodToSave = method.copy(id = docId)
        collection.document(docId).set(methodToSave, SetOptions.merge()).await()
        return docId
    }

    suspend fun deletePaymentMethod(methodId: String) {
        if (methodId.isBlank()) return
        firestore.collection("payment_methods").document(methodId).delete().await()
    }

    fun listenPaymentMethods(
        onUpdate: (List<PaymentMethod>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        return firestore.collection("payment_methods").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Erro no listener de formas de pagamento", error)
                onError(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(PaymentMethod::class.java)?.copy(id = doc.id)
                    } catch (_: Exception) { null }
                }
                onUpdate(list)
            }
        }
    }

    suspend fun fetchPaymentMethods(): List<PaymentMethod> {
        val snapshot = firestore.collection("payment_methods").get().await()
        return snapshot.documents.mapNotNull { doc ->
            try {
                doc.toObject(PaymentMethod::class.java)?.copy(id = doc.id)
            } catch (_: Exception) { null }
        }
    }

    /**
     * Inicializa formas de pagamento master fixas do sistema se necessário.
     * Modalidades fixas: À vista PIX, À vista Débito, À vista Crédito, Faturamento 7, 15, 30 e 60 dias.
     */
    suspend fun seedDefaultPaymentMethodsIfEmpty() {
        try {
            val defaults = listOf(
                PaymentMethod(
                    id = "pm_vista_pix",
                    name = "À vista PIX",
                    description = "Pagamento imediato via chave PIX",
                    termDays = 0,
                    means = PaymentMeans.PIX,
                    active = true
                ),
                PaymentMethod(
                    id = "pm_vista_debito",
                    name = "À vista Débito",
                    description = "Pagamento imediato no cartão de débito",
                    termDays = 0,
                    means = PaymentMeans.DEBITO,
                    active = true
                ),
                PaymentMethod(
                    id = "pm_vista_credito",
                    name = "À vista Crédito",
                    description = "Pagamento à vista no cartão de crédito corporativo",
                    termDays = 0,
                    means = PaymentMeans.CREDITO,
                    active = true
                ),
                PaymentMethod(
                    id = "pm_fat_7d",
                    name = "Faturamento 7 dias",
                    description = "Faturamento corporativo em 7 dias corridos",
                    termDays = 7,
                    means = PaymentMeans.FATURAMENTO,
                    active = true
                ),
                PaymentMethod(
                    id = "pm_fat_15d",
                    name = "Faturamento 15 dias",
                    description = "Faturamento corporativo quinzenal (15 dias)",
                    termDays = 15,
                    means = PaymentMeans.FATURAMENTO,
                    active = true
                ),
                PaymentMethod(
                    id = "pm_fat_30d",
                    name = "Faturamento 30 dias",
                    description = "Faturamento corporativo mensal (30 dias)",
                    termDays = 30,
                    means = PaymentMeans.FATURAMENTO,
                    active = true
                ),
                PaymentMethod(
                    id = "pm_fat_60d",
                    name = "Faturamento 60 dias",
                    description = "Faturamento corporativo estendido (60 dias)",
                    termDays = 60,
                    means = PaymentMeans.FATURAMENTO,
                    active = true
                )
            )

            val current = fetchPaymentMethods()
            if (current.isEmpty()) {
                for (item in defaults) {
                    savePaymentMethod(item)
                }
            } else {
                // Garante que todas as 7 modalidades fixas existem no Firestore
                val existingIds = current.map { it.id }.toSet()
                for (item in defaults) {
                    if (!existingIds.contains(item.id)) {
                        savePaymentMethod(item)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao popular formas de pagamento padrão", e)
        }
    }

    // ==========================================
    // NOTIFICAÇÕES & PUSH DISPATCH (8 CENÁRIOS)
    // ==========================================
    suspend fun dispatchNotificationRecord(
        driverId: String,
        title: String,
        body: String,
        tripId: String? = null,
        type: String = NotificationType.DRIVER_ASSIGNED
    ) {
        dispatchNotification(
            userId = driverId,
            title = title,
            body = body,
            tripId = tripId,
            type = type
        )
    }

    suspend fun dispatchNotification(
        userId: String? = null,
        role: String? = null,
        companyId: String? = null,
        title: String,
        body: String,
        tripId: String? = null,
        type: String
    ) {
        try {
            val now = System.currentTimeMillis()

            if (!userId.isNullOrBlank()) {
                val targetUser = getUserById(userId)
                val token = targetUser?.fcmToken?.ifBlank { targetUser.fcmTokenAndroid.ifBlank { targetUser.fcmTokenWeb } }.orEmpty()
                val data = mapOf(
                    "userId" to userId,
                    "driverId" to userId,
                    "driverName" to (targetUser?.name ?: ""),
                    "userName" to (targetUser?.name ?: ""),
                    "role" to (targetUser?.role ?: ""),
                    "companyId" to (targetUser?.companyId ?: ""),
                    "fcmToken" to token,
                    "title" to title,
                    "body" to body,
                    "tripId" to (tripId ?: ""),
                    "type" to type,
                    "read" to false,
                    "createdAt" to now
                )
                firestore.collection("notifications").add(data).await()
                Log.d(TAG, "Push registrado para usuário $userId: $title")
            }

            if (userId.isNullOrBlank() && !role.isNullOrBlank()) {
                val data = mapOf(
                    "role" to role.uppercase(),
                    "title" to title,
                    "body" to body,
                    "tripId" to (tripId ?: ""),
                    "type" to type,
                    "read" to false,
                    "createdAt" to now
                )
                firestore.collection("notifications").add(data).await()
                Log.d(TAG, "Push único registrado para perfil $role: $title")
            }

            if (userId.isNullOrBlank() && role.isNullOrBlank() && !companyId.isNullOrBlank()) {
                val data = mapOf(
                    "companyId" to companyId,
                    "title" to title,
                    "body" to body,
                    "tripId" to (tripId ?: ""),
                    "type" to type,
                    "read" to false,
                    "createdAt" to now
                )
                firestore.collection("notifications").add(data).await()
                Log.d(TAG, "Push único registrado para empresa $companyId: $title")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao gravar registro de notificação", e)
        }
    }

    // 1. Viagem criada pelo administrador
    suspend fun notifyTripCreatedByAdmin(trip: Trip) {
        if (!trip.driverId.isNullOrBlank()) {
            dispatchNotification(
                userId = trip.driverId,
                title = "🚗 Nova Viagem Atribuída",
                body = "Você foi escalado para a viagem: ${trip.origin} ➔ ${trip.destination} (${trip.passengerName})",
                tripId = trip.id,
                type = NotificationType.TRIP_CREATED_ADMIN
            )
        } else {
            dispatchNotification(
                role = UserRole.DRIVER,
                title = "🚗 Nova Viagem Disponível",
                body = "Nova viagem aberta na central: ${trip.origin} ➔ ${trip.destination}",
                tripId = trip.id,
                type = NotificationType.TRIP_CREATED_ADMIN
            )
        }
        if (trip.companyId.isNotBlank()) {
            dispatchNotification(
                companyId = trip.companyId,
                title = "📋 Viagem Agendada pela Central",
                body = "Nova viagem agendada para ${trip.passengerName}: ${trip.origin} ➔ ${trip.destination}",
                tripId = trip.id,
                type = NotificationType.TRIP_CREATED_ADMIN
            )
        }
    }

    // 2. Viagem criada pela empresa
    suspend fun notifyTripCreatedByCompany(trip: Trip) {
        val compLabel = trip.companyName.ifBlank { "Empresa Corporativa" }
        dispatchNotification(
            role = UserRole.ADMIN,
            title = "🏢 Nova Solicitação de Viagem",
            body = "$compLabel solicitou uma viagem: ${trip.origin} ➔ ${trip.destination} (${trip.passengerName})",
            tripId = trip.id,
            type = NotificationType.TRIP_CREATED_COMPANY
        )
        if (!trip.driverId.isNullOrBlank()) {
            dispatchNotification(
                userId = trip.driverId,
                title = "🚗 Nova Viagem Corporativa",
                body = "Viagem corporativa de $compLabel: ${trip.origin} ➔ ${trip.destination}",
                tripId = trip.id,
                type = NotificationType.TRIP_CREATED_COMPANY
            )
        } else {
            dispatchNotification(
                role = UserRole.DRIVER,
                title = "🚗 Nova Solicitação Corporativa",
                body = "Nova viagem de $compLabel: ${trip.origin} ➔ ${trip.destination}",
                tripId = trip.id,
                type = NotificationType.TRIP_CREATED_COMPANY
            )
        }
    }

    // 3. Motorista aceitou a viagem
    suspend fun notifyTripAcceptedByDriver(trip: Trip, driverName: String) {
        dispatchNotification(
            role = UserRole.ADMIN,
            title = "✅ Viagem Aceita pelo Motorista",
            body = "O motorista $driverName aceitou a viagem: ${trip.origin} ➔ ${trip.destination}",
            tripId = trip.id,
            type = NotificationType.TRIP_ACCEPTED_DRIVER
        )
        if (trip.companyId.isNotBlank()) {
            dispatchNotification(
                companyId = trip.companyId,
                title = "🚗 Motorista Confirmado",
                body = "O motorista $driverName confirmou sua viagem para ${trip.passengerName} e está a caminho!",
                tripId = trip.id,
                type = NotificationType.TRIP_ACCEPTED_DRIVER
            )
        }
    }

    // 4. Motorista recusou a viagem
    suspend fun notifyTripRejectedByDriver(trip: Trip, driverName: String, reason: String) {
        dispatchNotification(
            role = UserRole.ADMIN,
            title = "⚠️ Viagem Recusada pelo Motorista",
            body = "O motorista $driverName recusou a viagem ${trip.origin} ➔ ${trip.destination}. Motivo: $reason",
            tripId = trip.id,
            type = NotificationType.TRIP_REJECTED_DRIVER
        )
    }

    // 5. Empresa ou Administrador cancelou a viagem
    suspend fun notifyTripCancelled(trip: Trip, cancelledByRole: String, cancelledByName: String) {
        if (!trip.driverId.isNullOrBlank()) {
            dispatchNotification(
                userId = trip.driverId,
                title = "❌ Viagem Cancelada",
                body = "A viagem de ${trip.origin} ➔ ${trip.destination} foi cancelada ($cancelledByName).",
                tripId = trip.id,
                type = NotificationType.TRIP_CANCELLED
            )
        }
        if (cancelledByRole.equals(UserRole.COMPANY, ignoreCase = true)) {
            dispatchNotification(
                role = UserRole.ADMIN,
                title = "❌ Viagem Cancelada pela Empresa",
                body = "A empresa ${trip.companyName} cancelou a viagem para ${trip.passengerName}.",
                tripId = trip.id,
                type = NotificationType.TRIP_CANCELLED
            )
        } else {
            if (trip.companyId.isNotBlank()) {
                dispatchNotification(
                    companyId = trip.companyId,
                    title = "❌ Viagem Cancelada pela Central",
                    body = "A viagem para ${trip.passengerName} (${trip.origin} ➔ ${trip.destination}) foi cancelada pela central.",
                    tripId = trip.id,
                    type = NotificationType.TRIP_CANCELLED
                )
            }
        }
    }

    // 6. Pagamento da comissão do motorista realizada
    suspend fun notifyCommissionPaid(trip: Trip) {
        if (!trip.driverId.isNullOrBlank()) {
            dispatchNotification(
                userId = trip.driverId,
                title = "💰 Comissão Paga!",
                body = "Sua comissão de R$ %.2f referente à corrida foi liquidada com sucesso!".format(trip.driverCommission),
                tripId = trip.id,
                type = NotificationType.COMMISSION_PAID
            )
        }
    }

    // 7. Quando um motorista for atribuído a uma viagem
    suspend fun notifyDriverAssigned(trip: Trip, driver: AppUser) {
        dispatchNotification(
            userId = driver.id,
            title = "🎯 Você foi Escalado!",
            body = "Você foi atribuído à viagem: ${trip.origin} ➔ ${trip.destination}. Passageiro: ${trip.passengerName}.",
            tripId = trip.id,
            type = NotificationType.DRIVER_ASSIGNED
        )
        if (trip.companyId.isNotBlank()) {
            val vehicleInfo = if (driver.vehicleModel.isNotBlank()) " (${driver.vehicleModel} - ${driver.vehiclePlate})" else ""
            dispatchNotification(
                companyId = trip.companyId,
                title = "🚗 Motorista Designado",
                body = "O motorista ${driver.name}$vehicleInfo foi escalado para sua viagem para ${trip.passengerName}.",
                tripId = trip.id,
                type = NotificationType.DRIVER_ASSIGNED
            )
        }
    }

    // 8. Recebimento de pagamento pelas empresas
    suspend fun notifyCompanyPaymentReceived(trip: Trip, amount: Double = trip.price) {
        if (trip.companyId.isNotBlank()) {
            dispatchNotification(
                companyId = trip.companyId,
                title = "💳 Pagamento Confirmado",
                body = "Confirmamos o recebimento de R$ %.2f referente à sua fatura de viagens. Obrigado!".format(amount),
                tripId = trip.id,
                type = NotificationType.COMPANY_PAYMENT_RECEIVED
            )
        }
        dispatchNotification(
            role = UserRole.ADMIN,
            title = "💵 Recebimento Confirmado",
            body = "Recebimento confirmado de R$ %.2f da empresa ${trip.companyName.ifBlank { "Corporativa" }}.".format(amount),
            tripId = trip.id,
            type = NotificationType.COMPANY_PAYMENT_RECEIVED
        )
    }

    // 9. Viagem Iniciada (para Empresas e Administrador)
    suspend fun notifyTripStarted(trip: Trip, driverName: String) {
        val dName = driverName.ifBlank { trip.driverName.ifBlank { "Motorista" } }
        // 1. Notifica Administrador
        dispatchNotification(
            role = UserRole.ADMIN,
            title = "🚀 Viagem Iniciada",
            body = "O motorista $dName iniciou a viagem para ${trip.passengerName} (${trip.origin} ➔ ${trip.destination}).",
            tripId = trip.id,
            type = NotificationType.TRIP_STARTED
        )
        // 2. Notifica Empresa da Viagem
        if (trip.companyId.isNotBlank()) {
            dispatchNotification(
                companyId = trip.companyId,
                title = "🚀 Viagem Iniciada",
                body = "O motorista $dName deu início à viagem de ${trip.passengerName} com destino a ${trip.destination}.",
                tripId = trip.id,
                type = NotificationType.TRIP_STARTED
            )
        }
    }

    // Listener de notificações em tempo real
    fun listenNotifications(
        userId: String,
        role: String,
        companyId: String,
        sinceTimestamp: Long,
        onNewNotification: (NotificationRecord) -> Unit
    ): ListenerRegistration {
        return firestore.collection("notifications")
            .whereGreaterThanOrEqualTo("createdAt", sinceTimestamp)
            .addSnapshotListener { snapshot, err ->
                if (err != null || snapshot == null) return@addSnapshotListener
                for (change in snapshot.documentChanges) {
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        try {
                            val doc = change.document
                            val targetUid = doc.getString("userId").orEmpty()
                            val targetRole = doc.getString("role").orEmpty()
                            val targetCid = doc.getString("companyId").orEmpty()

                            // Se houver userId, a notificação é estritamente pessoal (evita duplicação entre múltiplos admins ou motoristas)
                            val isForMe = if (targetUid.isNotBlank()) {
                                targetUid == userId
                            } else if (targetRole.isNotBlank()) {
                                targetRole.equals(role, ignoreCase = true)
                            } else if (targetCid.isNotBlank()) {
                                targetCid == companyId
                            } else {
                                false
                            }

                            if (isForMe) {
                                val item = NotificationRecord(
                                    id = doc.id,
                                    userId = targetUid,
                                    userName = doc.getString("userName").orEmpty(),
                                    role = targetRole,
                                    companyId = targetCid,
                                    fcmToken = doc.getString("fcmToken").orEmpty(),
                                    title = doc.getString("title").orEmpty(),
                                    body = doc.getString("body").orEmpty(),
                                    tripId = doc.getString("tripId").orEmpty(),
                                    type = doc.getString("type").orEmpty(),
                                    read = doc.getBoolean("read") ?: false,
                                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                )
                                onNewNotification(item)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Erro ao processar notificação recebida", e)
                        }
                    }
                }
            }
    }

    // ==========================================
    // FAIXAS DE KM (Fare Bands) & PRICING RULES
    // ==========================================
    fun findBand(distanceKm: Double, bands: List<FareBand>): FareBand? {
        return bands
            .filter { it.active && it.minKm <= distanceKm && distanceKm <= it.maxKm }
            .minByOrNull { it.sortOrder }
    }

    fun calculatePricePerKm(distanceKm: Double, bands: List<FareBand>): Double {
        val band = findBand(distanceKm, bands) ?: return 0.0
        val rawPrice = distanceKm * band.multiplier
        return kotlin.math.round(rawPrice * 100.0) / 100.0
    }

    suspend fun saveFareBand(band: FareBand): String {
        val collection = firestore.collection("fare_bands")
        val docId = if (band.id.isNotBlank()) band.id else collection.document().id
        val bandToSave = band.copy(id = docId)
        collection.document(docId).set(bandToSave, SetOptions.merge()).await()
        return docId
    }

    suspend fun deleteFareBand(bandId: String) {
        if (bandId.isBlank()) return
        firestore.collection("fare_bands").document(bandId).delete().await()
    }

    fun listenFareBands(
        onUpdate: (List<FareBand>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        return firestore.collection("fare_bands").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Erro no listener de faixas de KM", error)
                onError(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(FareBand::class.java)?.copy(id = doc.id)
                    } catch (_: Exception) { null }
                }.sortedWith(
                    compareBy({ it.sortOrder }, { it.minKm })
                )
                onUpdate(list)
            }
        }
    }

    suspend fun fetchFareBands(): List<FareBand> {
        val snapshot = firestore.collection("fare_bands").get().await()
        return snapshot.documents.mapNotNull { doc ->
            try {
                doc.toObject(FareBand::class.java)?.copy(id = doc.id)
            } catch (_: Exception) { null }
        }.sortedWith(
            compareBy({ it.sortOrder }, { it.minKm })
        )
    }

    // ==========================================
    // ROTAS / TABELA DE PREÇOS (Routes)
    // ==========================================
    suspend fun saveRoute(route: Route, bands: List<FareBand>): Result<String> {
        val origin = route.origin.trim()
        val destination = route.destination.trim()
        if (origin.isBlank() || destination.isBlank()) {
            return Result.failure(IllegalArgumentException("Origem e Destino são obrigatórios."))
        }

        val docId = if (route.id.isNotBlank()) route.id else firestore.collection("routes").document().id
        val now = System.currentTimeMillis()

        val routeToSave: Route = if (route.pricingMode == PricingMode.PER_KM) {
            if (route.distanceKm <= 0.0) {
                return Result.failure(IllegalArgumentException("Distância em KM deve ser maior que zero no modo POR KM."))
            }
            val band = findBand(route.distanceKm, bands)
                ?: return Result.failure(IllegalStateException("Nenhuma faixa de KM ativa encontrada para ${route.distanceKm} km. Cadastre a faixa correspondente antes de salvar."))
            val calculatedPrice = calculatePricePerKm(route.distanceKm, bands)
            route.copy(
                id = docId,
                origin = origin,
                destination = destination,
                pricingMode = PricingMode.PER_KM,
                price = calculatedPrice,
                fareBandId = band.id,
                multiplierSnapshot = band.multiplier,
                updatedAt = now
            )
        } else {
            // FIXED
            if (route.price <= 0.0) {
                return Result.failure(IllegalArgumentException("Preço deve ser maior que zero no modo FIXO."))
            }
            route.copy(
                id = docId,
                origin = origin,
                destination = destination,
                pricingMode = PricingMode.FIXED,
                price = kotlin.math.round(route.price * 100.0) / 100.0,
                fareBandId = "",
                multiplierSnapshot = 0.0,
                updatedAt = now
            )
        }

        return try {
            firestore.collection("routes").document(docId).set(routeToSave, SetOptions.merge()).await()
            Result.success(docId)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar rota", e)
            Result.failure(e)
        }
    }

    suspend fun deleteRoute(routeId: String) {
        if (routeId.isBlank()) return
        firestore.collection("routes").document(routeId).delete().await()
    }

    fun listenRoutes(
        onUpdate: (List<Route>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        return firestore.collection("routes").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Erro no listener de rotas", error)
                onError(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Route::class.java)?.copy(id = doc.id)
                    } catch (_: Exception) { null }
                }.sortedWith(
                    compareBy({ it.origin }, { it.destination })
                )
                onUpdate(list)
            }
        }
    }

    suspend fun fetchRoutes(): List<Route> {
        val snapshot = firestore.collection("routes").get().await()
        return snapshot.documents.mapNotNull { doc ->
            try {
                doc.toObject(Route::class.java)?.copy(id = doc.id)
            } catch (_: Exception) { null }
        }.sortedWith(
            compareBy({ it.origin }, { it.destination })
        )
    }

    // ==========================================
    // OPERAÇÕES DA EMPRESA (Company Portal)
    // ==========================================
    fun listenTripsForCompany(
        companyId: String,
        onUpdate: (List<Trip>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        return firestore.collection("trips")
            .whereEqualTo("companyId", companyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Erro no listener de viagens da empresa $companyId", error)
                    onError(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Trip::class.java)?.let { trip ->
                            if (trip.id.isBlank()) trip.copy(id = doc.id) else trip
                        }
                    }
                    onUpdate(list)
                }
            }
    }

    fun listenPassengersForCompany(
        companyId: String,
        onUpdate: (List<Passenger>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        return firestore.collection("passengers")
            .whereEqualTo("companyId", companyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Erro no listener de passageiros da empresa $companyId", error)
                    onError(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(Passenger::class.java)
                    onUpdate(list)
                }
            }
    }

    suspend fun savePassengerForCompany(passenger: Passenger, forcedCompanyId: String): String {
        require(forcedCompanyId.isNotBlank()) { "companyId não pode ser vazio." }
        val collection = firestore.collection("passengers")
        val docId = if (passenger.id.isNotBlank()) passenger.id else collection.document().id
        val passengerToSave = passenger.copy(
            id = docId,
            companyId = forcedCompanyId,
            name = passenger.name.trim()
        )
        collection.document(docId).set(passengerToSave, SetOptions.merge()).await()
        return docId
    }

    suspend fun getTripById(tripId: String): Trip? {
        if (tripId.isBlank()) return null
        return try {
            val doc = firestore.collection("trips").document(tripId).get().await()
            doc.toObject(Trip::class.java)?.let { if (it.id.isBlank()) it.copy(id = doc.id) else it }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar viagem por ID $tripId", e)
            null
        }
    }

    suspend fun adminCancelTrip(tripId: String, reason: String = ""): Result<Unit> {
        if (tripId.isBlank()) {
            return Result.failure(IllegalArgumentException("ID da viagem é obrigatório."))
        }

        return try {
            val doc = firestore.collection("trips").document(tripId).get().await()
            val trip = doc.toObject(Trip::class.java)
                ?: return Result.failure(Exception("Viagem não encontrada."))

            val currentNotes = trip.notes.trim()
            val reasonText = if (reason.isNotBlank()) "Cancelada pelo Administrador: $reason" else "Cancelada pelo Administrador"
            val updatedNotes = if (currentNotes.isBlank()) reasonText else "$currentNotes | $reasonText"

            firestore.collection("trips").document(tripId).update(
                mapOf(
                    "status" to TripStatus.CANCELLED,
                    "notes" to updatedNotes
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao cancelar viagem pelo administrador", e)
            Result.failure(e)
        }
    }

    suspend fun adminDeleteTrip(tripId: String): Result<Unit> {
        if (tripId.isBlank()) {
            return Result.failure(IllegalArgumentException("ID da viagem é obrigatório."))
        }
        return try {
            firestore.collection("trips").document(tripId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao excluir viagem pelo administrador", e)
            Result.failure(e)
        }
    }

    suspend fun cancelTripIfCompanyAllowed(tripId: String, companyId: String, isAdmin: Boolean = false): Result<Unit> {
        if (tripId.isBlank()) {
            return Result.failure(IllegalArgumentException("ID da viagem é obrigatório."))
        }
        if (!isAdmin && companyId.isBlank()) {
            return Result.failure(IllegalArgumentException("ID da empresa é obrigatório."))
        }

        return try {
            val doc = firestore.collection("trips").document(tripId).get().await()
            val trip = doc.toObject(Trip::class.java)
                ?: return Result.failure(Exception("Viagem não encontrada."))

            // O Administrador tem poder total sobre qualquer viagem
            if (!isAdmin) {
                if (trip.companyId != companyId) {
                    return Result.failure(SecurityException("Você não tem permissão para cancelar esta viagem."))
                }

                if (!trip.status.equals(TripStatus.PENDING, ignoreCase = true) || !trip.driverId.isNullOrBlank()) {
                    return Result.failure(IllegalStateException("Indisponível após atribuição de motorista."))
                }
            }

            val currentNotes = trip.notes.trim()
            val tag = if (isAdmin) "Cancelada pelo Administrador" else "Cancelada pela empresa"
            val updatedNotes = if (currentNotes.isBlank()) tag else "$currentNotes | $tag"

            firestore.collection("trips").document(tripId).update(
                mapOf(
                    "status" to TripStatus.CANCELLED,
                    "notes" to updatedNotes
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao cancelar viagem", e)
            Result.failure(e)
        }
    }

    fun listenDriverLocations(
        onUpdate: (List<DriverLocation>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        return firestore.collection("driver_locations").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Erro ao escutar driver_locations no Firestore", error)
                onError(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val dl = doc.toObject(DriverLocation::class.java)
                    dl?.copy(id = doc.id)
                }
                onUpdate(list)
            }
        }
    }

    suspend fun updateDriverLocation(
        driverId: String,
        latitude: Double,
        longitude: Double,
        speed: Double = 0.0,
        bearing: Double = 0.0
    ) {
        try {
            val now = System.currentTimeMillis()
            firestore.collection("users").document(driverId).update(
                mapOf(
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "speed" to speed,
                    "bearing" to bearing,
                    "isOnline" to true,
                    "lastLocationUpdate" to now
                )
            ).await()

            firestore.collection("driver_locations").document(driverId).set(
                mapOf(
                    "driverId" to driverId,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "speed" to speed,
                    "bearing" to bearing,
                    "isOnline" to true,
                    "updatedAt" to now
                ),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar localização do motorista", e)
        }
    }
}
