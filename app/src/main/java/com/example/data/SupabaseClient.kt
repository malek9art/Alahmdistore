package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// ==========================================================================
// 1. Supabase Auth DTOs & Models
// ==========================================================================

@JsonClass(generateAdapter = true)
data class SignUpRequest(
    @Json(name = "email") val email: String?,
    @Json(name = "password") val password: String,
    @Json(name = "phone") val phone: String?,
    @Json(name = "user_metadata") val userMetadata: Map<String, String>?
)

@JsonClass(generateAdapter = true)
data class SignInRequest(
    @Json(name = "email") val email: String?,
    @Json(name = "password") val password: String,
    @Json(name = "phone") val phone: String?
)

@JsonClass(generateAdapter = true)
data class ResetPasswordRequest(
    @Json(name = "email") val email: String
)

@JsonClass(generateAdapter = true)
data class UserResponse(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String?,
    @Json(name = "phone") val phone: String?,
    @Json(name = "user_metadata") val userMetadata: Map<String, String>?
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String,
    @Json(name = "expires_in") val expiresIn: Long,
    @Json(name = "refresh_token") val refreshToken: String?,
    @Json(name = "user") val user: UserResponse
)

// ==========================================================================
// 2. Database Table CRUD DTOs (Supabase Postgrest API)
// ==========================================================================

@JsonClass(generateAdapter = true)
data class ProfileDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "role") val role: String,
    @Json(name = "allowed_panels") val allowedPanels: String? = null
)

@JsonClass(generateAdapter = true)
data class CategoryDto(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "name_ar") val nameAr: String,
    @Json(name = "slug") val slug: String,
    @Json(name = "icon") val icon: String? = "phone_android"
)

@JsonClass(generateAdapter = true)
data class ProductDto(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "category_id") val categoryId: Int,
    @Json(name = "name_ar") val nameAr: String,
    @Json(name = "desc_ar") val descAr: String,
    @Json(name = "price") val price: Double,
    @Json(name = "old_price") val oldPrice: Double? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "stock") val stock: Int = 0,
    @Json(name = "is_offer") val isOffer: Boolean = false
)

@JsonClass(generateAdapter = true)
data class OrderDto(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "customer_id") val customerId: String? = null,
    @Json(name = "guest_name") val guestName: String? = null,
    @Json(name = "guest_phone") val guestPhone: String? = null,
    @Json(name = "status") val status: String,
    @Json(name = "total_amount") val totalAmount: Double,
    @Json(name = "payment_method") val paymentMethod: String,
    @Json(name = "address") val address: String,
    @Json(name = "tracking_token") val trackingToken: String? = null
)

@JsonClass(generateAdapter = true)
data class OrderItemDto(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "order_id") val orderId: Int,
    @Json(name = "product_id") val productId: Int,
    @Json(name = "quantity") val quantity: Int,
    @Json(name = "price") val price: Double
)

@JsonClass(generateAdapter = true)
data class PaymentDto(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "order_id") val orderId: Int,
    @Json(name = "transaction_id") val transactionId: String? = null,
    @Json(name = "amount") val amount: Double,
    @Json(name = "status") val status: String,
    @Json(name = "gateway") val gateway: String
)

@JsonClass(generateAdapter = true)
data class ShipmentDto(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "order_id") val orderId: Int,
    @Json(name = "delivery_id") val deliveryId: String? = null,
    @Json(name = "status") val status: String,
    @Json(name = "carrier_notes") val carrierNotes: String? = null,
    @Json(name = "estimated_delivery") val estimatedDelivery: String? = null
)

@JsonClass(generateAdapter = true)
data class AuditLogDto(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "user_role") val userRole: String,
    @Json(name = "action_ar") val actionAr: String,
    @Json(name = "details_ar") val detailsAr: String
)

// ==========================================================================
// 3. Supabase API Retrofit Interfaces
// ==========================================================================

interface SupabaseAuthApi {
    @POST("auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") apiKey: String,
        @Body request: SignUpRequest
    ): Response<AuthResponse>

    @POST("auth/v1/token?grant_type=password")
    suspend fun signInWithPassword(
        @Header("apikey") apiKey: String,
        @Body request: SignInRequest
    ): Response<AuthResponse>

    @GET("auth/v1/user")
    suspend fun getUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String
    ): Response<UserResponse>

    @POST("auth/v1/recover")
    suspend fun resetPassword(
        @Header("apikey") apiKey: String,
        @Body request: ResetPasswordRequest
    ): Response<Unit>
}

interface SupabaseRestApi {
    // ----------------------
    // Profiles CRUD
    // ----------------------
    @GET("rest/v1/profiles?select=*")
    suspend fun getProfiles(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String
    ): Response<List<ProfileDto>>

    @GET("rest/v1/profiles")
    suspend fun getProfileById(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") filter: String
    ): Response<List<ProfileDto>>

    @POST("rest/v1/profiles")
    suspend fun insertProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body profile: ProfileDto
    ): Response<List<ProfileDto>>

    // ----------------------
    // Categories CRUD
    // ----------------------
    @GET("rest/v1/categories?select=*")
    suspend fun getCategories(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String
    ): Response<List<CategoryDto>>

    @POST("rest/v1/categories")
    suspend fun insertCategory(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body category: CategoryDto
    ): Response<List<CategoryDto>>

    // ----------------------
    // Products CRUD
    // ----------------------
    @GET("rest/v1/products?select=*")
    suspend fun getProducts(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String
    ): Response<List<ProductDto>>

    @POST("rest/v1/products")
    suspend fun insertProduct(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body product: ProductDto
    ): Response<List<ProductDto>>

    @PATCH("rest/v1/products")
    suspend fun updateProduct(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Query("id") filter: String, // e.g. "eq.5"
        @Body fields: Map<String, @JvmSuppressWildcards Any?>
    ): Response<List<ProductDto>>

    @DELETE("rest/v1/products")
    suspend fun deleteProduct(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") filter: String // e.g. "eq.5"
    ): Response<Unit>

    // ----------------------
    // Orders CRUD
    // ----------------------
    @GET("rest/v1/orders?select=*")
    suspend fun getOrders(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String
    ): Response<List<OrderDto>>

    @POST("rest/v1/orders")
    suspend fun insertOrder(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body order: OrderDto
    ): Response<List<OrderDto>>

    @PATCH("rest/v1/orders")
    suspend fun updateOrder(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Query("id") filter: String,
        @Body fields: Map<String, @JvmSuppressWildcards Any?>
    ): Response<List<OrderDto>>

    // ----------------------
    // Order Items CRUD
    // ----------------------
    @GET("rest/v1/order_items?select=*")
    suspend fun getOrderItems(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String
    ): Response<List<OrderItemDto>>

    @POST("rest/v1/order_items")
    suspend fun insertOrderItem(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body orderItem: OrderItemDto
    ): Response<List<OrderItemDto>>

    // ----------------------
    // Payments CRUD
    // ----------------------
    @GET("rest/v1/payments?select=*")
    suspend fun getPayments(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String
    ): Response<List<PaymentDto>>

    @POST("rest/v1/payments")
    suspend fun insertPayment(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body payment: PaymentDto
    ): Response<List<PaymentDto>>

    // ----------------------
    // Shipments CRUD
    // ----------------------
    @GET("rest/v1/shipments?select=*")
    suspend fun getShipments(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String
    ): Response<List<ShipmentDto>>

    @POST("rest/v1/shipments")
    suspend fun insertShipment(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body shipment: ShipmentDto
    ): Response<List<ShipmentDto>>

    @PATCH("rest/v1/shipments")
    suspend fun updateShipment(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Query("id") filter: String,
        @Body fields: Map<String, @JvmSuppressWildcards Any?>
    ): Response<List<ShipmentDto>>

    // ----------------------
    // Audit Logs CRUD
    // ----------------------
    @GET("rest/v1/audit_logs?select=*")
    suspend fun getAuditLogs(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String
    ): Response<List<AuditLogDto>>

    @POST("rest/v1/audit_logs")
    suspend fun insertAuditLog(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body log: AuditLogDto
    ): Response<List<AuditLogDto>>
}

// ==========================================================================
// 4. Supabase Client Initializer & Convenience Wrapper
// ==========================================================================

object SupabaseClient {
    private var currentUrl: String = ""
    private var currentKey: String = ""
    private var authService: SupabaseAuthApi? = null
    private var restService: SupabaseRestApi? = null

    /**
     * Initializes Retrofit client with dynamic Supabase URL and Key (Project ID configuration)
     */
    fun init(url: String, key: String) {
        val sanitizedUrl = if (url.endsWith("/")) url else "$url/"
        if (sanitizedUrl == currentUrl && key == currentKey && authService != null && restService != null) {
            return
        }
        
        currentUrl = sanitizedUrl
        currentKey = key

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        try {
            val retrofit = Retrofit.Builder()
                .baseUrl(currentUrl)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            authService = retrofit.create(SupabaseAuthApi::class.java)
            restService = retrofit.create(SupabaseRestApi::class.java)
        } catch (e: Exception) {
            authService = null
            restService = null
            e.printStackTrace()
        }
    }

    private fun getAuthService(): SupabaseAuthApi {
        return authService ?: throw IllegalStateException("Supabase client is not initialized. Setup URL and Anon Key in Settings.")
    }

    private fun getRestService(): SupabaseRestApi {
        return restService ?: throw IllegalStateException("Supabase client is not initialized. Setup URL and Anon Key in Settings.")
    }

    private fun getAuthHeader(): String {
        return "Bearer $currentKey" // Using anon key for default operations; could be replaced by user token
    }

    // ==========================================================================
    // 5. Auth API Methods
    // ==========================================================================

    suspend fun signUp(request: SignUpRequest): Response<AuthResponse> {
        return getAuthService().signUp(currentKey, request)
    }

    suspend fun signIn(request: SignInRequest): Response<AuthResponse> {
        return getAuthService().signInWithPassword(currentKey, request)
    }

    suspend fun testConnection(): Boolean {
        val service = authService ?: return false
        return try {
            val response = service.getUser(currentKey, "Bearer invalid_test_token")
            response.code() == 401 || response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    // ==========================================================================
    // 6. Postgrest REST CRUD Convenience Wrapper Methods
    // ==========================================================================

    // Categories
    suspend fun fetchCategories(): List<CategoryDto> {
        val response = getRestService().getCategories(currentKey, getAuthHeader())
        return if (response.isSuccessful) response.body() ?: emptyList() else throw Exception("Failed categories: ${response.message()}")
    }

    suspend fun addCategory(category: CategoryDto): CategoryDto? {
        val response = getRestService().insertCategory(currentKey, getAuthHeader(), category = category)
        return if (response.isSuccessful) response.body()?.firstOrNull() else throw Exception("Failed category insert: ${response.message()}")
    }

    // Products
    suspend fun fetchProducts(): List<ProductDto> {
        val response = getRestService().getProducts(currentKey, getAuthHeader())
        return if (response.isSuccessful) response.body() ?: emptyList() else throw Exception("Failed products: ${response.message()}")
    }

    suspend fun addProduct(product: ProductDto): ProductDto? {
        val response = getRestService().insertProduct(currentKey, getAuthHeader(), product = product)
        return if (response.isSuccessful) response.body()?.firstOrNull() else throw Exception("Failed product insert: ${response.message()}")
    }

    suspend fun modifyProduct(id: Int, fields: Map<String, Any?>): ProductDto? {
        val response = getRestService().updateProduct(currentKey, getAuthHeader(), filter = "eq.$id", fields = fields)
        return if (response.isSuccessful) response.body()?.firstOrNull() else throw Exception("Failed product update: ${response.message()}")
    }

    suspend fun removeProduct(id: Int): Boolean {
        val response = getRestService().deleteProduct(currentKey, getAuthHeader(), filter = "eq.$id")
        return response.isSuccessful
    }

    // Orders
    suspend fun fetchOrders(): List<OrderDto> {
        val response = getRestService().getOrders(currentKey, getAuthHeader())
        return if (response.isSuccessful) response.body() ?: emptyList() else throw Exception("Failed orders: ${response.message()}")
    }

    suspend fun addOrder(order: OrderDto): OrderDto? {
        val response = getRestService().insertOrder(currentKey, getAuthHeader(), order = order)
        return if (response.isSuccessful) response.body()?.firstOrNull() else throw Exception("Failed order insert: ${response.message()}")
    }

    suspend fun modifyOrder(id: Int, fields: Map<String, Any?>): OrderDto? {
        val response = getRestService().updateOrder(currentKey, getAuthHeader(), filter = "eq.$id", fields = fields)
        return if (response.isSuccessful) response.body()?.firstOrNull() else throw Exception("Failed order update: ${response.message()}")
    }

    // Order Items
    suspend fun addOrderItem(orderItem: OrderItemDto): OrderItemDto? {
        val response = getRestService().insertOrderItem(currentKey, getAuthHeader(), orderItem = orderItem)
        return if (response.isSuccessful) response.body()?.firstOrNull() else throw Exception("Failed order item insert: ${response.message()}")
    }

    // Payments
    suspend fun fetchPayments(): List<PaymentDto> {
        val response = getRestService().getPayments(currentKey, getAuthHeader())
        return if (response.isSuccessful) response.body() ?: emptyList() else throw Exception("Failed payments: ${response.message()}")
    }

    suspend fun addPayment(payment: PaymentDto): PaymentDto? {
        val response = getRestService().insertPayment(currentKey, getAuthHeader(), payment = payment)
        return if (response.isSuccessful) response.body()?.firstOrNull() else throw Exception("Failed payment insert: ${response.message()}")
    }

    // Shipments
    suspend fun fetchShipments(): List<ShipmentDto> {
        val response = getRestService().getShipments(currentKey, getAuthHeader())
        return if (response.isSuccessful) response.body() ?: emptyList() else throw Exception("Failed shipments: ${response.message()}")
    }

    suspend fun addShipment(shipment: ShipmentDto): ShipmentDto? {
        val response = getRestService().insertShipment(currentKey, getAuthHeader(), shipment = shipment)
        return if (response.isSuccessful) response.body()?.firstOrNull() else throw Exception("Failed shipment insert: ${response.message()}")
    }

    suspend fun modifyShipment(id: Int, fields: Map<String, Any?>): ShipmentDto? {
        val response = getRestService().updateShipment(currentKey, getAuthHeader(), filter = "eq.$id", fields = fields)
        return if (response.isSuccessful) response.body()?.firstOrNull() else throw Exception("Failed shipment update: ${response.message()}")
    }

    // Audit Logs
    suspend fun fetchAuditLogs(): List<AuditLogDto> {
        val response = getRestService().getAuditLogs(currentKey, getAuthHeader())
        return if (response.isSuccessful) response.body() ?: emptyList() else throw Exception("Failed audit logs: ${response.message()}")
    }

    suspend fun addAuditLog(log: AuditLogDto): AuditLogDto? {
        val response = getRestService().insertAuditLog(currentKey, getAuthHeader(), log = log)
        return if (response.isSuccessful) response.body()?.firstOrNull() else throw Exception("Failed audit log insert: ${response.message()}")
    }

    // Profiles & Auth Extra
    suspend fun fetchProfileById(id: String): ProfileDto? {
        val service = restService ?: return null
        return try {
            val response = service.getProfileById(currentKey, getAuthHeader(), "eq.$id")
            if (response.isSuccessful) response.body()?.firstOrNull() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun resetPassword(email: String): Response<Unit> {
        val service = authService ?: throw IllegalStateException("Supabase client is not initialized.")
        return service.resetPassword(currentKey, ResetPasswordRequest(email))
    }
}
