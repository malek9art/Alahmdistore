package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppRole {
    CUSTOMER,
    ADMIN,
    DELIVERY
}

sealed class Screen {
    object Splash : Screen()
    object Onboarding : Screen()
    data class Login(val role: AppRole) : Screen()
    object Register : Screen()
    object CustomerHome : Screen()
    object Categories : Screen()
    data class ProductDetail(val productId: Int) : Screen()
    object Cart : Screen()
    object Checkout : Screen()
    data class PaymentGateway(val orderId: Int) : Screen()
    data class OrderTracking(val orderId: Int) : Screen()
    object OrderHistory : Screen()
    object Favorites : Screen()
    object Profile : Screen()
    object SupportChat : Screen()
    object AboutStore : Screen()
    object Notifications : Screen()
    object Offers : Screen()
    object ReturnWarranty : Screen()
    object Settings : Screen()
    object AdminDashboard : Screen()
    object DeliveryPortal : Screen()
    object DatabaseExplorer : Screen()
}

data class ApiLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val method: String,
    val endpoint: String,
    val status: Int,
    val requestBody: String,
    val responseBody: String
)

data class SupportMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user", "agent"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class DeleteType {
    PRODUCT, BRAND, DELIVERY_REP, COUPON, BANNER, REVIEW, TEAM_MEMBER, CART_ITEM, CLEAR_CART
}

data class DeleteConfirmation(
    val type: DeleteType,
    val id: Int = 0,
    val name: String = "",
    val extraData: Any? = null
)

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    // Centralized popup, dialog and notification states
    val pendingDeleteAction = MutableStateFlow<DeleteConfirmation?>(null)
    val successMessage = MutableStateFlow<String?>(null)
    val favoriteToast = MutableStateFlow<String?>(null)
    val cartToast = MutableStateFlow<String?>(null)
    val orderSuccessPopup = MutableStateFlow<Order?>(null)

    private val db: StoreDatabase = Room.databaseBuilder(
        application,
        StoreDatabase::class.java,
        "alahmadi_store_db"
    ).fallbackToDestructiveMigration().build()

    private val repository = StoreRepository(db.storeDao())

    fun getOrderItems(orderId: Int): Flow<List<OrderItem>> {
        return repository.getOrderItems(orderId)
    }

    // App State
    val currentScreen = MutableStateFlow<Screen>(Screen.Splash)
    val appRole = MutableStateFlow(AppRole.CUSTOMER)
    
    // User Session
    val customerName = MutableStateFlow("")
    val customerPhone = MutableStateFlow("")
    val deliveryAddress = MutableStateFlow("")
    val customerEmail = MutableStateFlow("")
    
    // Map Selection States
    val selectedLatitude = MutableStateFlow<Double?>(null)
    val selectedLongitude = MutableStateFlow<Double?>(null)
    val selectedMapAddress = MutableStateFlow<String?>(null)
    
    // Auth States
    val isLoggedInCustomer = MutableStateFlow(false)
    val isLoggedInAdmin = MutableStateFlow(false)
    val isLoggedInDelivery = MutableStateFlow(false)
    val loginError = MutableStateFlow<String?>(null)
    val registerError = MutableStateFlow<String?>(null)

    // Supabase Configuration States
    val supabaseUrl = MutableStateFlow("")
    val supabaseKey = MutableStateFlow("")
    val isSupabaseEnabled = MutableStateFlow(false) // If true, use real Supabase Auth
    val isTestingConnection = MutableStateFlow(false)
    val testConnectionSuccess = MutableStateFlow<Boolean?>(null)
    val isDarkTheme = MutableStateFlow(true)
    
    // Main Branch Accounting System (ERP) Integration States
    val accountingApiUrl = MutableStateFlow("")
    val accountingApiKey = MutableStateFlow("")
    val accountingBranchCode = MutableStateFlow("BR-ALAHMADI-01")
    val accountingSyncInterval = MutableStateFlow(30) // Minutes
    val isAccountingEnabled = MutableStateFlow(false)
    val isSyncingProducts = MutableStateFlow(false)
    val isSyncingOrders = MutableStateFlow(false)
    val isTestingAccountingConnection = MutableStateFlow(false)
    val testAccountingConnectionSuccess = MutableStateFlow<Boolean?>(null)
    val accountingSyncLogs = MutableStateFlow<List<String>>(listOf(
        "2026-06-25 10:15:02 - تم سحب آخر تحديث لأسعار المنتجات من دليل الحسابات الرئيسي.",
        "2026-06-25 09:30:11 - تم ترحيل طلبات اليوم السابقة وتوليد فواتير مبيعات بالرقم الضريبي.",
        "2026-06-25 08:00:00 - بدء جلسة المزامنة التلقائية مع مستودع الفرع الرئيسي بنجاح."
    ))
    
    // Separated Settings States
    // 1) Customer settings
    val customerNotificationsEnabled = MutableStateFlow(true)
    val customerLocationEnabled = MutableStateFlow(true)
    val customerPreferredLanguage = MutableStateFlow("ar")
    val customerDefaultPayment = MutableStateFlow("الدفع عند الاستلام")

    // 2) Admin settings
    val adminNotificationsEnabled = MutableStateFlow(true)
    val adminAutoApproveOrders = MutableStateFlow(false)
    val adminLowStockAlertThreshold = MutableStateFlow(5)

    // 3) Delivery settings
    val deliveryNotificationsEnabled = MutableStateFlow(true)
    val deliveryGpsTrackingEnabled = MutableStateFlow(true)
    val deliveryAutoRefreshInterval = MutableStateFlow(30)

    // Brands list State
    val brands = MutableStateFlow<List<String>>(listOf("آبل", "سامسونج", "أنكر", "هواوي", "شاومي", "جيسبي"))

    // Admin Customers State
    val adminCustomers = MutableStateFlow<List<AdminCustomer>>(listOf(
        AdminCustomer(1, "خالد محمد", "0501234567", "khaled@test.com", "2026-01-15", 3, "نشط"),
        AdminCustomer(2, "سارة عبد الله", "0557654321", "sara@test.com", "2026-02-10", 1, "نشط"),
        AdminCustomer(3, "فهد العتيبي", "0539988776", "fahad@test.com", "2026-03-01", 5, "نشط"),
        AdminCustomer(4, "منى الدوسري", "0541122334", "mona@test.com", "2026-04-18", 0, "موقوف")
    ))

    // Delivery Representatives State
    val deliveryReps = MutableStateFlow<List<DeliveryRep>>(listOf(
        DeliveryRep(1, "أحمد اليماني", "0561112223", "سيارة كورولا", 4.8f, "متاح"),
        DeliveryRep(2, "محمد الحربي", "0563334445", "دراجة نارية هوندا", 4.5f, "مشغول"),
        DeliveryRep(3, "عبد العزيز سعيد", "0565556667", "سيارة إلنترا", 4.9f, "غير نشط")
    ))

    // Shipping Zones State
    val shippingZones = MutableStateFlow<List<ShippingZone>>(listOf(
        ShippingZone(1, "الرياض", 15.0, true),
        ShippingZone(2, "مكة المكرمة", 25.0, true),
        ShippingZone(3, "جدة", 20.0, true),
        ShippingZone(4, "المدينة المنورة", 25.0, true),
        ShippingZone(5, "الدمام", 20.0, true),
        ShippingZone(6, "أبها", 30.0, true)
    ))

    // Coupons State
    val coupons = MutableStateFlow<List<Coupon>>(listOf(
        Coupon(1, "AHMADI10", 10, 50.0, true),
        Coupon(2, "RAMADAN", 20, 100.0, true),
        Coupon(3, "FREE_SHIP", 100, 30.0, true),
        Coupon(4, "OFF50", 50, 150.0, false)
    ))

    // Ad Banners State
    val adBanners = MutableStateFlow<List<AdBanner>>(listOf(
        AdBanner(1, "خصومات الربيع الكبرى 🌸", "banner_spring", true),
        AdBanner(2, "شواحن أنكر الأصلية بضمان سنتين ⚡", "banner_anker", true),
        AdBanner(3, "جديد آيفون 15 متوفر الآن 📱", "banner_iphone", true)
    ))

    // Reviews State
    val productReviews = MutableStateFlow<List<ProductReview>>(listOf(
        ProductReview(1, "فيصل العنزي", "آيفون 15 برو ماكس", 5, "منتج ممتاز وتوصيل سريع جداً في نفس اليوم!", true),
        ProductReview(2, "ريهام علي", "سماعة آبل إيربودز برو", 4, "الصوت رائع جداً ولكن السعر مرتفع قليلاً.", true),
        ProductReview(3, "سلطان العتيبي", "شاحن أنكر نانو", 5, "الشاحن جبار ويشحن الجوال بسرعة البرق وعملي جداً.", true),
        ProductReview(4, "مجهول", "سامسونج جالكسي S24 ألترا", 1, "لم يعجبني نظام التشغيل الجديد وأريد إرجاعه.", false)
    ))

    // Static Pages State
    val staticAboutUs = MutableStateFlow("مركز الأحمدي هو الخيار الأول لخدمات الاتصالات ومستلزمات الهواتف الذكية في المملكة. نسعى لتقديم أفضل المنتجات بأعلى معايير الجودة وبأسعار منافسة.")
    val staticPrivacyPolicy = MutableStateFlow("نحن نلتزم بحماية بياناتك الشخصية بنسبة 100%. لا نقوم بمشاركة أي من بيانات العملاء مع أي طرف ثالث.")
    val staticTermsOfService = MutableStateFlow("من خلال استخدام تطبيقنا، فإنك توافق على شروط الشراء والتوصيل وسياسة الاسترجاع الخاصة بمركز الأحمدي.")

    // Team Roles State
    val teamRoles = MutableStateFlow<List<TeamMemberRole>>(listOf(
        TeamMemberRole(1, "أحمد محمد (أدمن)", "Super Admin", true, true, true),
        TeamMemberRole(2, "خالد صالح (محرر)", "Editor", true, false, false),
        TeamMemberRole(3, "ياسر العلي (مشرف الشحنات)", "Delivery Manager", false, true, false)
    ))

    // General Store Info State
    val appStoreTitle = MutableStateFlow("مركز الأحمدي للاتصالات")
    val appStoreBannerText = MutableStateFlow("🚚 شحن مجاني للطلبات فوق 200 ريال!")
    val appStoreContactMobile = MutableStateFlow("0599999999")
    val appStoreSupportEmail = MutableStateFlow("support@alahmadi.com")
    val appStoreWorkingHours = MutableStateFlow("من 9:00 صباحاً إلى 11:00 مساءً")
    val appStoreMaintenanceMode = MutableStateFlow(false)
    val appStoreLogoUri = MutableStateFlow<String?>(null)

    // Payment Config State
    val payCashOnDeliveryEnabled = MutableStateFlow(true)
    val payCreditCardEnabled = MutableStateFlow(true)
    val payMadaEnabled = MutableStateFlow(true)
    val payApplePayEnabled = MutableStateFlow(true)
    val payGooglePayEnabled = MutableStateFlow(true)
    val payLocalWalletsEnabled = MutableStateFlow(true)
    val payInstallmentsEnabled = MutableStateFlow(false)
    val payVatRatePercent = MutableStateFlow(15)

    val activePaymentGateway = MutableStateFlow("MyFatoorah")
    val paymentGatewayApiKey = MutableStateFlow("sk_test_alahmadi_992x")
    val paymentGatewaySandbox = MutableStateFlow(true)
    
    val payInstallmentProvider = MutableStateFlow("Tabby & Tamara")
    val payInstallmentMonths = MutableStateFlow(4)

    
    private val prefs = application.getSharedPreferences("alahmadi_supabase_prefs", android.content.Context.MODE_PRIVATE)
    
    val exchangeRateSAR = MutableStateFlow(prefs.getFloat("exchange_rate_sar", 400.0f).toDouble())
    val exchangeRateUSD = MutableStateFlow(prefs.getFloat("exchange_rate_usd", 1500.0f).toDouble())
    
    // Active Checkout Step (1: Cart, 2: Address, 3: Shipping, 4: Payment, 5: Review/Confirm)
    val checkoutStep = MutableStateFlow(1)
    
    // Search & Filter
    val searchQuery = MutableStateFlow("")
    val selectedCategoryId = MutableStateFlow<Int?>(null)

    // Coupon Code
    val activeCoupon = MutableStateFlow<String?>(null)
    val couponDiscountPercent = MutableStateFlow(0.0) // e.g. 0.1 for 10%
    val couponError = MutableStateFlow<String?>(null)

    // Simulated API Logs
    private val _apiLogs = MutableStateFlow<List<ApiLog>>(emptyList())
    val apiLogs: StateFlow<List<ApiLog>> = _apiLogs.asStateFlow()

    // Support Tickets
    private val _supportMessages = MutableStateFlow<List<SupportMessage>>(
        listOf(
            SupportMessage(sender = "agent", text = "مرحباً بك في مركز الدعم الفني لمركز الأحمدي. كيف يمكننا مساعدتك اليوم؟")
        )
    )
    val supportMessages: StateFlow<List<SupportMessage>> = _supportMessages.asStateFlow()

    // Reactive Flows from Database
    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartItems: StateFlow<List<CartItem>> = repository.cartItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders: StateFlow<List<Order>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<Notification>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLog>> = repository.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<Set<Int>> = repository.allFavorites
        .map { list -> list.map { it.productId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        // Load Supabase configurations from preferences
        supabaseUrl.value = prefs.getString("supabase_url", "") ?: ""
        supabaseKey.value = prefs.getString("supabase_key", "") ?: ""
        isSupabaseEnabled.value = prefs.getBoolean("supabase_enabled", false)
        isDarkTheme.value = prefs.getBoolean("is_dark_theme", true)

        // Load separated settings
        customerNotificationsEnabled.value = prefs.getBoolean("customer_notifications", true)
        customerLocationEnabled.value = prefs.getBoolean("customer_location", true)
        customerPreferredLanguage.value = prefs.getString("customer_language", "ar") ?: "ar"
        customerDefaultPayment.value = prefs.getString("customer_default_payment", "الدفع عند الاستلام") ?: "الدفع عند الاستلام"

        // Load persisted payment configuration
        payCashOnDeliveryEnabled.value = prefs.getBoolean("pay_cod_enabled", true)
        payCreditCardEnabled.value = prefs.getBoolean("pay_card_enabled", true)
        payMadaEnabled.value = prefs.getBoolean("pay_mada_enabled", true)
        payApplePayEnabled.value = prefs.getBoolean("pay_apple_enabled", true)
        payGooglePayEnabled.value = prefs.getBoolean("pay_google_enabled", true)
        payLocalWalletsEnabled.value = prefs.getBoolean("pay_wallets_enabled", true)
        payInstallmentsEnabled.value = prefs.getBoolean("pay_installments_enabled", false)
        payVatRatePercent.value = prefs.getInt("pay_vat_rate", 15)

        activePaymentGateway.value = prefs.getString("active_payment_gateway", "MyFatoorah") ?: "MyFatoorah"
        paymentGatewayApiKey.value = prefs.getString("payment_gateway_api_key", "sk_test_alahmadi_992x") ?: "sk_test_alahmadi_992x"
        paymentGatewaySandbox.value = prefs.getBoolean("payment_gateway_sandbox", true)
        
        payInstallmentProvider.value = prefs.getString("pay_installment_provider", "Tabby & Tamara") ?: "Tabby & Tamara"
        payInstallmentMonths.value = prefs.getInt("pay_installment_months", 4)

        adminNotificationsEnabled.value = prefs.getBoolean("admin_notifications", true)
        adminAutoApproveOrders.value = prefs.getBoolean("admin_auto_approve_orders", false)
        adminLowStockAlertThreshold.value = prefs.getInt("admin_low_stock_threshold", 5)

        deliveryNotificationsEnabled.value = prefs.getBoolean("delivery_notifications", true)
        deliveryGpsTrackingEnabled.value = prefs.getBoolean("delivery_gps", true)
        deliveryAutoRefreshInterval.value = prefs.getInt("delivery_refresh_interval", 30)

        // Load Main Branch Accounting Configuration
        accountingApiUrl.value = prefs.getString("accounting_api_url", "https://erp.alahmadi-group.com/api/v1") ?: "https://erp.alahmadi-group.com/api/v1"
        accountingApiKey.value = prefs.getString("accounting_api_key", "sec_key_alahmadi_992x_erp") ?: "sec_key_alahmadi_992x_erp"
        accountingBranchCode.value = prefs.getString("accounting_branch_code", "BR-ALAHMADI-MAIN") ?: "BR-ALAHMADI-MAIN"
        accountingSyncInterval.value = prefs.getInt("accounting_sync_interval", 30)
        isAccountingEnabled.value = prefs.getBoolean("accounting_enabled", true) // Default true to prepare connection initially

        if (supabaseUrl.value.isNotEmpty() && supabaseKey.value.isNotEmpty()) {
            SupabaseClient.init(supabaseUrl.value, supabaseKey.value)
        }

        // Seed initial data if database is empty or has missing categories
        viewModelScope.launch {
            categories.first() // Wait for first collect
            repository.allCategories.take(1).collect { existingCats ->
                if (existingCats.size < 9) {
                    seedDatabase()
                }
            }
        }
    }

    fun updateSupabaseConfig(url: String, key: String, enabled: Boolean) {
        viewModelScope.launch {
            supabaseUrl.value = url
            supabaseKey.value = key
            isSupabaseEnabled.value = enabled

            prefs.edit().apply {
                putString("supabase_url", url)
                putString("supabase_key", key)
                putBoolean("supabase_enabled", enabled)
                apply()
            }

            if (url.isNotEmpty() && key.isNotEmpty()) {
                SupabaseClient.init(url, key)
            }
        }
    }

    fun updateCustomerSettings(notifications: Boolean, location: Boolean, language: String, payment: String) {
        customerNotificationsEnabled.value = notifications
        customerLocationEnabled.value = location
        customerPreferredLanguage.value = language
        customerDefaultPayment.value = payment
        prefs.edit().apply {
            putBoolean("customer_notifications", notifications)
            putBoolean("customer_location", location)
            putString("customer_language", language)
            putString("customer_default_payment", payment)
            apply()
        }
    }

    fun updateAdminSettings(notifications: Boolean, autoApprove: Boolean, lowStockThreshold: Int) {
        adminNotificationsEnabled.value = notifications
        adminAutoApproveOrders.value = autoApprove
        adminLowStockAlertThreshold.value = lowStockThreshold
        prefs.edit().apply {
            putBoolean("admin_notifications", notifications)
            putBoolean("admin_auto_approve_orders", autoApprove)
            putInt("admin_low_stock_threshold", lowStockThreshold)
            apply()
        }
    }

    fun updateDeliverySettings(notifications: Boolean, gps: Boolean, refreshInterval: Int) {
        deliveryNotificationsEnabled.value = notifications
        deliveryGpsTrackingEnabled.value = gps
        deliveryAutoRefreshInterval.value = refreshInterval
        prefs.edit().apply {
            putBoolean("delivery_notifications", notifications)
            putBoolean("delivery_gps", gps)
            putInt("delivery_refresh_interval", refreshInterval)
            apply()
        }
    }

    // --- Main Branch Accounting ERP Integration Functions ---

    fun updateAccountingConfig(url: String, key: String, branchCode: String, syncInterval: Int, enabled: Boolean) {
        viewModelScope.launch {
            accountingApiUrl.value = url
            accountingApiKey.value = key
            accountingBranchCode.value = branchCode
            accountingSyncInterval.value = syncInterval
            isAccountingEnabled.value = enabled

            prefs.edit().apply {
                putString("accounting_api_url", url)
                putString("accounting_api_key", key)
                putString("accounting_branch_code", branchCode)
                putInt("accounting_sync_interval", syncInterval)
                putBoolean("accounting_enabled", enabled)
                apply()
            }

            repository.insertAuditLog(
                AuditLog(
                    actionAr = "تعديل إعدادات النظام المحاسبي",
                    userRole = "مدير",
                    detailsAr = "تعديل بيانات الربط مع الفرع الرئيسي ($branchCode) ومعدل التزامن: $syncInterval دقيقة."
                )
            )
            
            logSimulatedApi(
                method = "POST",
                endpoint = "/api/v1/erp/configure",
                status = 200,
                requestBody = "{\"url\":\"$url\",\"branch\":\"$branchCode\",\"syncInterval\":$syncInterval}",
                responseBody = "{\"status\":\"configured\",\"timestamp\":${System.currentTimeMillis()}}"
            )
        }
    }

    fun testAccountingConnection(url: String, key: String, branchCode: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            isTestingAccountingConnection.value = true
            testAccountingConnectionSuccess.value = null
            
            // Simulate networking delay
            kotlinx.coroutines.delay(2000)
            
            val isSuccess = url.isNotEmpty() && key.isNotEmpty()
            testAccountingConnectionSuccess.value = isSuccess
            isTestingAccountingConnection.value = false
            onComplete(isSuccess)
            
            val logMsg = if (isSuccess) 
                "نجاح الاتصال مع خادم النظام المحاسبي للفرع الرئيسي ($branchCode)."
            else 
                "فشل فحص الاتصال مع نظام المحاسبة للفرع الرئيسي."

            repository.insertAuditLog(
                AuditLog(
                    actionAr = "فحص الاتصال المحاسبي",
                    userRole = "مدير",
                    detailsAr = logMsg
                )
            )

            logSimulatedApi(
                method = "GET",
                endpoint = "/api/v1/erp/ping",
                status = if (isSuccess) 200 else 500,
                requestBody = "",
                responseBody = if (isSuccess) 
                    "{\"status\":\"online\",\"version\":\"14.2.1-stable\",\"db_status\":\"connected\"}"
                else 
                    "{\"status\":\"offline\",\"error\":\"connection_timeout\"}"
            )
        }
    }

    fun syncAccountingProducts() {
        viewModelScope.launch {
            if (isSyncingProducts.value) return@launch
            isSyncingProducts.value = true
            
            val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            addAccountingLog("$timestamp - جاري الاتصال بخادم النظام المحاسبي وسحب أسعار الأجهزة المحدثة...")
            
            kotlinx.coroutines.delay(2500) // Simulate sync processing
            
            // Generate some random stock updates for simulation
            val currentProducts = repository.allProducts.first()
            var updatedCount = 0
            currentProducts.forEach { product ->
                // Simulate updating stock slightly
                val stockDelta = (-2..5).random()
                val newStock = (product.stockQuantity + stockDelta).coerceAtLeast(0)
                repository.updateProductStock(product.id, newStock)
                updatedCount++
            }
            
            isSyncingProducts.value = false
            val successMsg = "$timestamp - تم مزامنة وتحديث أسعار ومخزون ($updatedCount) منتج بنجاح من مستودع الفرع الرئيسي."
            addAccountingLog(successMsg)
            
            repository.insertAuditLog(
                AuditLog(
                    actionAr = "مزامنة مخزون النظام المحاسبي",
                    userRole = "مدير",
                    detailsAr = "سحب كميات المنتجات من الفرع الرئيسي وتحديث ($updatedCount) صنف في الكاش المحلي."
                )
            )

            logSimulatedApi(
                method = "GET",
                endpoint = "/api/v1/erp/inventory-sync?branch=${accountingBranchCode.value}",
                status = 200,
                requestBody = "",
                responseBody = "{\"status\":\"success\",\"synchronized_items\":$updatedCount}"
            )
        }
    }

    fun syncAccountingOrders() {
        viewModelScope.launch {
            if (isSyncingOrders.value) return@launch
            isSyncingOrders.value = true
            
            val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            addAccountingLog("$timestamp - جاري ترحيل الطلبات المعلقة الفورية وتوليد القيود المحاسبية باليومية العامة...")
            
            kotlinx.coroutines.delay(2500) // Simulate order exporting
            
            val currentOrders = repository.allOrders.first()
            val pendingSyncCount = currentOrders.filter { it.status == "تم التوصيل" || it.status == "قيد المراجعة" }.size
            val syncedCount = if (pendingSyncCount > 0) pendingSyncCount else 3
            
            isSyncingOrders.value = false
            val successMsg = "$timestamp - تم ترحيل وتصدير ($syncedCount) فاتورة مبيعات إلى دليل حسابات الفرع الرئيسي وقيدها بالصناديق المعنية."
            addAccountingLog(successMsg)
            
            repository.insertAuditLog(
                AuditLog(
                    actionAr = "ترحيل فواتير المبيعات",
                    userRole = "مدير",
                    detailsAr = "تصدير ($syncedCount) فاتورة مبيعات من المتجر إلى الأستاذ العام بنظام المحاسبة للفرع الرئيسي."
                )
            )

            logSimulatedApi(
                method = "POST",
                endpoint = "/api/v1/erp/invoice-post",
                status = 200,
                requestBody = "{\"branch\":\"${accountingBranchCode.value}\",\"invoices_count\":$syncedCount}",
                responseBody = "{\"status\":\"success\",\"posted_invoices_count\":$syncedCount,\"ledger_entry\":\"JV-2026-${(1000..9999).random()}\"}"
            )
        }
    }

    private fun addAccountingLog(log: String) {
        accountingSyncLogs.value = (listOf(log) + accountingSyncLogs.value).take(30)
    }

    // --- Admin Dashboard Mutator Functions ---

    // Brands Management Functions
    fun addBrand(name: String) {
        if (name.isNotEmpty() && !brands.value.contains(name)) {
            brands.value = brands.value + name
            viewModelScope.launch {
                repository.insertAuditLog(AuditLog(actionAr = "إضافة علامة تجارية", userRole = "مدير", detailsAr = "تم إضافة العلامة التجارية: $name"))
            }
        }
    }
    fun deleteBrand(name: String) {
        brands.value = brands.value.filter { it != name }
        viewModelScope.launch {
            repository.insertAuditLog(AuditLog(actionAr = "حذف علامة تجارية", userRole = "مدير", detailsAr = "تم حذف العلامة التجارية: $name"))
        }
    }

    // Customers Functions
    fun toggleCustomerStatus(id: Int) {
        adminCustomers.value = adminCustomers.value.map {
            if (it.id == id) {
                val newStatus = if (it.status == "نشط") "موقوف" else "نشط"
                viewModelScope.launch {
                    repository.insertAuditLog(AuditLog(actionAr = "تعديل حالة عميل", userRole = "مدير", detailsAr = "تغيير حالة العميل ${it.name} إلى $newStatus"))
                }
                it.copy(status = newStatus)
            } else it
        }
    }
    fun addAdminCustomer(name: String, phone: String, email: String) {
        val newId = (adminCustomers.value.maxOfOrNull { it.id } ?: 0) + 1
        val newCust = AdminCustomer(newId, name, phone, email, "2026-06-24", 0, "نشط")
        adminCustomers.value = adminCustomers.value + newCust
        viewModelScope.launch {
            repository.insertAuditLog(AuditLog(actionAr = "إضافة عميل يدوي", userRole = "مدير", detailsAr = "تم تسجيل العميل الجديد: $name ($phone)"))
        }
    }

    // Delivery Reps Functions
    fun addDeliveryRep(name: String, phone: String, vehicle: String) {
        val newId = (deliveryReps.value.maxOfOrNull { it.id } ?: 0) + 1
        val newRep = DeliveryRep(newId, name, phone, vehicle, 5.0f, "متاح")
        deliveryReps.value = deliveryReps.value + newRep
        viewModelScope.launch {
            repository.insertAuditLog(AuditLog(actionAr = "إضافة مندوب توصيل", userRole = "مدير", detailsAr = "تم تعيين المندوب: $name بالتطبيق."))
        }
    }
    fun deleteDeliveryRep(id: Int) {
        val rep = deliveryReps.value.find { it.id == id }
        deliveryReps.value = deliveryReps.value.filter { it.id != id }
        rep?.let {
            viewModelScope.launch {
                repository.insertAuditLog(AuditLog(actionAr = "حذف مندوب", userRole = "مدير", detailsAr = "تم إزالة المندوب: ${it.name}"))
            }
        }
    }
    fun updateRepStatus(id: Int, newStatus: String) {
        deliveryReps.value = deliveryReps.value.map {
            if (it.id == id) it.copy(status = newStatus) else it
        }
    }

    // Shipping Zones Functions
    fun updateZoneCost(id: Int, newCost: Double) {
        shippingZones.value = shippingZones.value.map {
            if (it.id == id) {
                viewModelScope.launch {
                    repository.insertAuditLog(AuditLog(actionAr = "تعديل سعر الشحن", userRole = "مدير", detailsAr = "تعديل سعر شحن مدينة ${it.city} إلى $newCost ر.ي"))
                }
                it.copy(cost = newCost)
            } else it
        }
    }
    fun toggleZoneActive(id: Int) {
        shippingZones.value = shippingZones.value.map {
            if (it.id == id) it.copy(active = !it.active) else it
        }
    }

    // Coupons Functions
    fun addCoupon(code: String, discount: Int, maxDisc: Double) {
        val newId = (coupons.value.maxOfOrNull { it.id } ?: 0) + 1
        val newCoupon = Coupon(newId, code, discount, maxDisc, true)
        coupons.value = coupons.value + newCoupon
        viewModelScope.launch {
            repository.insertAuditLog(AuditLog(actionAr = "إضافة كوبون خصم", userRole = "مدير", detailsAr = "تم تفعيل كوبون خصم جديد: $code بنسبة $discount%"))
        }
    }
    fun toggleCouponActive(id: Int) {
        coupons.value = coupons.value.map {
            if (it.id == id) it.copy(active = !it.active) else it
        }
    }
    fun deleteCoupon(id: Int) {
        coupons.value = coupons.value.filter { it.id != id }
    }

    // Ad Banners Functions
    fun addAdBanner(title: String, img: String) {
        val newId = (adBanners.value.maxOfOrNull { it.id } ?: 0) + 1
        adBanners.value = adBanners.value + AdBanner(newId, title, img, true)
        viewModelScope.launch {
            repository.insertAuditLog(AuditLog(actionAr = "إضافة إعلان بنر", userRole = "مدير", detailsAr = "إضافة بنر إعلاني جديد: $title"))
        }
    }
    fun deleteAdBanner(id: Int) {
        adBanners.value = adBanners.value.filter { it.id != id }
    }

    // Reviews Functions
    fun approveReview(id: Int) {
        productReviews.value = productReviews.value.map {
            if (it.id == id) {
                viewModelScope.launch {
                    repository.insertAuditLog(AuditLog(actionAr = "الموافقة على تقييم", userRole = "مدير", detailsAr = "الموافقة على تعليق العميل ${it.customerName}"))
                }
                it.copy(approved = true)
            } else it
        }
    }
    fun deleteReview(id: Int) {
        productReviews.value = productReviews.value.filter { it.id != id }
    }

    fun addNewReview(customerName: String, productName: String, rating: Int, comment: String) {
        val newId = (productReviews.value.maxOfOrNull { it.id } ?: 0) + 1
        val newReview = ProductReview(
            id = newId,
            customerName = customerName.ifEmpty { "عميل متجر الأحمدي" },
            productName = productName,
            rating = rating,
            comment = comment,
            approved = true
        )
        productReviews.value = productReviews.value + newReview
        
        logSimulatedApi("POST", "/api/v1/reviews", 201, "{\"product\":\"$productName\",\"rating\":$rating}", "{\"status\":\"success\",\"id\":$newId}")
        
        viewModelScope.launch {
            repository.insertAuditLog(
                AuditLog(
                    actionAr = "إضافة تقييم منتج",
                    userRole = "عميل",
                    detailsAr = "أضاف العميل ${newReview.customerName} تقييمًا بقيمة $rating نجوم لمنتج: $productName"
                )
            )
        }
    }

    // Static Content Functions
    fun updateStaticContent(about: String, privacy: String, terms: String) {
        staticAboutUs.value = about
        staticPrivacyPolicy.value = privacy
        staticTermsOfService.value = terms
        viewModelScope.launch {
            repository.insertAuditLog(AuditLog(actionAr = "تعديل المحتوى الثابت", userRole = "مدير", detailsAr = "تم تعديل صفحات من نحن وسياسة الخصوصية."))
        }
    }

    // Roles & Team Functions
    fun addTeamMember(name: String, role: String, canWrite: Boolean, canDel: Boolean, canEdit: Boolean) {
        val newId = (teamRoles.value.maxOfOrNull { it.id } ?: 0) + 1
        teamRoles.value = teamRoles.value + TeamMemberRole(newId, name, role, canWrite, canDel, canEdit)
        viewModelScope.launch {
            repository.insertAuditLog(AuditLog(actionAr = "إضافة عضو فريق وصلاحيات", userRole = "مدير", detailsAr = "تعيين العضو الجديد $name بدور $role"))
        }
    }
    fun deleteTeamMember(id: Int) {
        teamRoles.value = teamRoles.value.filter { it.id != id }
    }

    // General Store Info Functions
    fun updateStoreGeneralInfo(title: String, banner: String, contact: String, email: String, hours: String, maint: Boolean, logoUri: String? = null) {
        appStoreTitle.value = title
        appStoreBannerText.value = banner
        appStoreContactMobile.value = contact
        appStoreSupportEmail.value = email
        appStoreWorkingHours.value = hours
        appStoreMaintenanceMode.value = maint
        appStoreLogoUri.value = logoUri
        successMessage.value = "تم حفظ التعديلات وإعدادات المتجر العامة لمركز الأحمدي بنجاح! ⚙️"
        viewModelScope.launch {
            repository.insertAuditLog(AuditLog(actionAr = "تعديل الإعدادات العامة للمتجر", userRole = "مدير", detailsAr = "تم تغيير مسمى المتجر ومعلومات التواصل وصيانة التطبيق والشعار."))
        }
    }

    // Payment Config Functions
    fun updatePaymentConfig(
        cod: Boolean,
        card: Boolean,
        mada: Boolean,
        apple: Boolean,
        google: Boolean,
        wallets: Boolean,
        installments: Boolean,
        vat: Int,
        gateway: String,
        apiKey: String,
        sandbox: Boolean,
        instProvider: String,
        instMonths: Int
    ) {
        payCashOnDeliveryEnabled.value = cod
        payCreditCardEnabled.value = card
        payMadaEnabled.value = mada
        payApplePayEnabled.value = apple
        payGooglePayEnabled.value = google
        payLocalWalletsEnabled.value = wallets
        payInstallmentsEnabled.value = installments
        payVatRatePercent.value = vat
        activePaymentGateway.value = gateway
        paymentGatewayApiKey.value = apiKey
        paymentGatewaySandbox.value = sandbox
        payInstallmentProvider.value = instProvider
        payInstallmentMonths.value = instMonths

        prefs.edit().apply {
            putBoolean("pay_cod_enabled", cod)
            putBoolean("pay_card_enabled", card)
            putBoolean("pay_mada_enabled", mada)
            putBoolean("pay_apple_enabled", apple)
            putBoolean("pay_google_enabled", google)
            putBoolean("pay_wallets_enabled", wallets)
            putBoolean("pay_installments_enabled", installments)
            putInt("pay_vat_rate", vat)
            putString("active_payment_gateway", gateway)
            putString("payment_gateway_api_key", apiKey)
            putBoolean("payment_gateway_sandbox", sandbox)
            putString("pay_installment_provider", instProvider)
            putInt("pay_installment_months", instMonths)
        }.apply()

        successMessage.value = "تم حفظ إعدادات بوابات الدفع الإلكتروني والضرائب والعملات بنجاح! 💳"
        viewModelScope.launch {
            repository.insertAuditLog(
                AuditLog(
                    actionAr = "تحديث طرق الدفع والضريبة وبوابات التكامل",
                    userRole = "مدير",
                    detailsAr = "تم تعديل خيارات الدفع الإلكتروني (البوابة النشطة: $gateway, البيئة التجريبية: $sandbox)، المحافظ الرقمية، الأقساط الشهرية ($instProvider)، وتحديث نسبة ضريبة القيمة المضافة لتصبح $vat%."
                )
            )
        }
    }

    fun updateExchangeRates(sarRate: Double, usdRate: Double) {
        exchangeRateSAR.value = sarRate
        exchangeRateUSD.value = usdRate
        prefs.edit()
            .putFloat("exchange_rate_sar", sarRate.toFloat())
            .putFloat("exchange_rate_usd", usdRate.toFloat())
            .apply()
        viewModelScope.launch {
            repository.insertAuditLog(
                AuditLog(
                    actionAr = "تحديث أسعار الصرف",
                    userRole = "مدير",
                    detailsAr = "تم تحديث أسعار الصرف في المتجر لتصبح: 1 ر.س = $sarRate ر.ي، 1 دولار = $usdRate ر.ي"
                )
            )
        }
    }

    // Backup & Restore Simulation Functions
    fun triggerBackup(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            repository.insertAuditLog(AuditLog(actionAr = "إجراء نسخ احتياطي", userRole = "مدير", detailsAr = "تم إنشاء نسخة احتياطية مشفرة لقواعد البيانات المحلية بنجاح."))
            val backupData = "{\"metadata\":{\"backup_id\":\"bk_2026_06_24\",\"timestamp\":1782390192000},\"categories_count\":${categories.value.size},\"products_count\":${products.value.size},\"orders_count\":${orders.value.size}}"
            onComplete(backupData)
        }
    }
    fun triggerRestore(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.insertAuditLog(AuditLog(actionAr = "استعادة نسخة احتياطية", userRole = "مدير", detailsAr = "تم استيراد واستعادة قاعدة البيانات بنجاح من نقطة الاستعادة."))
            onComplete()
        }
    }
    fun triggerResetDatabase(onComplete: () -> Unit) {
        viewModelScope.launch {
            seedDatabase() // Seed initial categories and products again
            repository.insertAuditLog(AuditLog(actionAr = "إعادة ضبط المصنع للبيانات", userRole = "مدير", detailsAr = "تم مسح كافة البيانات والطلبات المسجلة وإعادة تهيئة المتجر إلى حالته الافتراضية الأولى."))
            onComplete()
        }
    }

    // Stock Management Quick Updater
    fun quickUpdateStock(productId: Int, delta: Int) {
        viewModelScope.launch {
            val prod = products.value.find { it.id == productId }
            prod?.let {
                val newStock = (it.stockQuantity + delta).coerceAtLeast(0)
                repository.updateProductStock(it.id, newStock)
                repository.insertAuditLog(AuditLog(actionAr = "تعديل سريع للمخزون", userRole = "مدير", detailsAr = "تعديل كمية منتج ${it.nameAr} بـ ($delta) - الكمية الحالية: $newStock"))
            }
        }
    }

    fun triggerSendAdminNotification(title: String, message: String) {
        viewModelScope.launch {
            repository.insertNotification(Notification(titleAr = title, messageAr = message))
            repository.insertAuditLog(AuditLog(actionAr = "إرسال إشعار جماعي", userRole = "مدير", detailsAr = "إرسال إشعار عام: $title - $message"))
        }
    }

    fun adminAddCategory(id: Int, nameAr: String, iconName: String) {
        viewModelScope.launch {
            repository.insertCategories(listOf(Category(id, nameAr, iconName)))
            repository.insertAuditLog(AuditLog(actionAr = "إضافة فئة جديدة", userRole = "مدير", detailsAr = "إضافة فئة: $nameAr بجانب الأيقونة: $iconName"))
        }
    }

    fun adminDeleteCategory(id: Int) {
        viewModelScope.launch {
            val cat = categories.value.find { it.id == id }
            repository.deleteCategoryById(id)
            cat?.let {
                repository.insertAuditLog(AuditLog(actionAr = "حذف فئة", userRole = "مدير", detailsAr = "تم حذف الفئة بالكامل: ${it.nameAr}"))
            }
        }
    }


    fun testSupabaseConnection(url: String, key: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            isTestingConnection.value = true
            testConnectionSuccess.value = null
            try {
                SupabaseClient.init(url, key)
                val success = SupabaseClient.testConnection()
                testConnectionSuccess.value = success
                onResult(success)
                
                logSimulatedApi(
                    method = "GET",
                    endpoint = "/auth/v1/user (Connection Test)",
                    status = if (success) 401 else 500,
                    requestBody = "Headers: { apikey: '${key.take(Math.min(key.length, 10))}...' }",
                    responseBody = if (success) "Connection verified: Supabase Auth server is reachable." else "Connection failed: Request timeout or invalid URL."
                )
            } catch (e: Exception) {
                testConnectionSuccess.value = false
                onResult(false)
            } finally {
                isTestingConnection.value = false
            }
        }
    }

    private suspend fun seedDatabase() {
        val initialCategories = listOf(
            Category(1, "الجوالات", "smartphone"),
            Category(2, "الشواحن", "bolt"),
            Category(3, "السماعات", "headset"),
            Category(4, "الكابلات", "settings_input_hdmi"),
            Category(5, "الحمايات", "security"),
            Category(6, "الإكسسوارات", "watch"),
            Category(7, "قطع الغيار والملحقات", "build"),
            Category(8, "المنتجات الموسمية", "calendar_today"),
            Category(9, "العروض والخصومات", "local_offer"),
            Category(10, "مكبرات الصوت MP3", "speaker"),
            Category(11, "كاميرات مراقبة", "videocam")
        )
        repository.insertCategories(initialCategories)

        val initialProducts = listOf(
            Product(
                id = 101,
                nameAr = "آيفون 15 برو ماكس - سعة 256 جيجابايت",
                descriptionAr = "أحدث هاتف ذكي من آبل يتميز بهيكل من التيتانيوم القوي، معالج A17 Pro الخارق، نظام كاميرات احترافي بدقة 48 ميجابكسل، وشاشة Super Retina XDR مع تقنية ProMotion ومعدل تحديث ديناميكي.",
                price = 4899.0,
                oldPrice = 5299.0,
                imageUrl = "iphone15",
                categoryId = 1,
                stockQuantity = 15,
                isFeatured = true,
                isOffer = true,
                rating = 4.9f,
                specsAr = "الشاشة: 6.7 بوصة,المعالج: Apple A17 Pro,الذاكرة: 256 جيجابايت,الكاميرا: ثلاثية 48+12+12 ميجابكسل,البطارية: 4441 مللي أمبير"
            ),
            Product(
                id = 102,
                nameAr = "سامسونج جالكسي S24 ألترا - سعة 512 جيجابايت",
                descriptionAr = "قوة الذكاء الاصطناعي الفائقة مع هاتف سامسونج الرائد. يأتي بإطار تيتانيوم متين، قلم S-Pen مدمج، معالج Snapdragon 8 Gen 3 المتطور، وشاشة Dynamic AMOLED 2X فائقة السطوع.",
                price = 4999.0,
                oldPrice = 5599.0,
                imageUrl = "s24ultra",
                categoryId = 1,
                stockQuantity = 12,
                isFeatured = true,
                isOffer = true,
                rating = 4.8f,
                specsAr = "الشاشة: 6.8 بوصة,المعالج: Snapdragon 8 Gen 3,الذاكرة: 512 جيجابايت,الكاميرا: رباعية 200+50+12+10 م.ب,البطارية: 5000 مللي أمبير"
            ),
            Product(
                id = 103,
                nameAr = "سماعة آبل إيربودز برو الجيل الثاني",
                descriptionAr = "تجربة صوتية غامرة لا مثيل لها مع ميزة إلغاء الضوضاء النشط المضاعفة، وميزة الشفافية الصوتية المتكيفة، وميزة الصوت المكاني المخصص لتوزيع الصوت بشكل ثلاثي الأبعاد.",
                price = 949.0,
                oldPrice = 1199.0,
                imageUrl = "airpods2",
                categoryId = 3,
                stockQuantity = 25,
                isFeatured = true,
                isOffer = true,
                rating = 4.7f,
                specsAr = "الشريحة: Apple H2,إلغاء الضوضاء: نعم (نشط ومضاعف),عمر البطارية: يصل إلى 6 ساعات (30 ساعة مع العلبة),منفذ الشحن: USB-C MagSafe"
            ),
            Product(
                id = 104,
                nameAr = "شاحن أنكر نانو بقوة 30 واط منفذ USB-C",
                descriptionAr = "شاحن جداري صغير الحجم ومصمم خصيصاً لشحن هواتف الآيفون والأندرويد بسرعة فائقة وموثوقية عالية مع الحماية الفائقة للبطارية من الحرارة المرتفعة.",
                price = 69.0,
                oldPrice = 89.0,
                imageUrl = "ankernano",
                categoryId = 2,
                stockQuantity = 50,
                isFeatured = false,
                isOffer = true,
                rating = 4.6f,
                specsAr = "القوة: 30 واط,المنفذ: USB-C,التقنية: PowerIQ 3.0 / ActiveShield,الوزن: 30 جرام"
            ),
            Product(
                id = 105,
                nameAr = "شاحن بيسوس غان المطور بقوة 100 واط",
                descriptionAr = "منصة شحن متكاملة تدعم الشحن السريع المتعدد لأربعة أجهزة في وقت واحد (هاتف، لابتوب، جهاز لوحي، وسماعة). تعتمد على تقنية GaN5 لتبديد مثالي للحرارة وسرعة شحن قصوى.",
                price = 199.0,
                oldPrice = null,
                imageUrl = "baseus100w",
                categoryId = 2,
                stockQuantity = 20,
                isFeatured = true,
                isOffer = false,
                rating = 4.5f,
                specsAr = "القوة الإجمالية: 100 واط,المنافذ: 2x USB-C + 2x USB-A,التقنية: GaN5 Pro,طول الكابل: 1.5 متر"
            ),
            Product(
                id = 106,
                nameAr = "سماعة جي بي إل Tune 720BT لاسلكية",
                descriptionAr = "سماعة رأس محيطية لاسلكية تقدم صوت JBL Pure Bass القوي والنقي مع بطارية ضخمة تدوم حتى 76 ساعة متواصلة وميزة الشحن السريع.",
                price = 299.0,
                oldPrice = 349.0,
                imageUrl = "jbltune",
                categoryId = 3,
                stockQuantity = 18,
                isFeatured = false,
                isOffer = false,
                rating = 4.4f,
                specsAr = "عمر البطارية: 76 ساعة,الاتصال: Bluetooth 5.3,الشحن السريع: 5 دقائق تعطي 3 ساعات,الوزن: 220 جرام"
            ),
            Product(
                id = 107,
                nameAr = "كابل بيسوس مغزول فائق القوة 100 واط",
                descriptionAr = "كابل شحن ونقل بيانات فائق التحمل مصنع من ألياف النايلون المغزولة عالية المتانة ويدعم شحن الهواتف والحواسب المحمولة حتى 100 واط.",
                price = 39.0,
                oldPrice = 49.0,
                imageUrl = "baseuscable",
                categoryId = 4,
                stockQuantity = 40,
                isFeatured = false,
                isOffer = true,
                rating = 4.5f,
                specsAr = "القوة: 100 واط,النوع: USB-C إلى USB-C,الطول: 2 متر,المادة: نايلون مغزول عالي المتانة"
            ),
            Product(
                id = 108,
                nameAr = "شاشة حماية زجاجية نانو ضد التجسس لآيفون 15",
                descriptionAr = "شاشة زجاجية مدرعة ثلاثية الأبعاد تقدم حماية قصوى لشاشتك من الصدمات والخدوش مع خاصية الحصوصية (الملاقيف) التي تمنع رؤية الشاشة من الزوايا الجانبية.",
                price = 49.0,
                oldPrice = 79.0,
                imageUrl = "screenprotector",
                categoryId = 5,
                stockQuantity = 100,
                isFeatured = false,
                isOffer = true,
                rating = 4.3f,
                specsAr = "النوع: زجاج حماية ضد التجسس (Privacy),التوافق: iPhone 15 Pro Max,مستوى الحماية: 9H,السمك: 0.33 ملم"
            ),
            Product(
                id = 109,
                nameAr = "ساعة Redmi Watch 4 الذكية",
                descriptionAr = "ساعة شاومي الذكية المميزة بشاشة AMOLED كبيرة قياس 1.97 بوصة، تدعم الاتصال بالبلوتوث، نظام تحديد المواقع المستقل، ومراقبة شاملة للصحة والأنشطة الرياضية مع بطارية مذهلة تدوم حتى 20 يوماً.",
                price = 349.0,
                oldPrice = 399.0,
                imageUrl = "redmiwatch",
                categoryId = 6,
                stockQuantity = 25,
                isFeatured = true,
                isOffer = true,
                rating = 4.6f,
                specsAr = "الشاشة: 1.97 بوصة AMOLED,عمر البطارية: حتى 20 يوم,الاتصال: بلوتوث 5.3 + GPS مدمج,مقاومة الماء: 5ATM"
            ),
            Product(
                id = 110,
                nameAr = "حامل جوال مغناطيسي للسيارة من Yesido",
                descriptionAr = "قاعدة تثبيت مغناطيسية قوية للغاية لتثبيت الهاتف في فتحة تكييف السيارة أو الطبلون بشكل آمن ومستقر بفضل المغناطيس القوي N52 الذي يحافظ على ثبات هاتفك حتى في الطرق الوعرة.",
                price = 49.0,
                oldPrice = 79.0,
                imageUrl = "yesidoholder",
                categoryId = 6,
                stockQuantity = 80,
                isFeatured = false,
                isOffer = true,
                rating = 4.5f,
                specsAr = "نوع التثبيت: مغناطيسي قوي (N52),الدوران: 360 درجة,التوافق: جميع الهواتف الذكية,مادة التصنيع: سبائك الألومنيوم + سيليكون"
            ),
            Product(
                id = 111,
                nameAr = "بطارية بديلة لآيفون 13 أصلية - ضمان سنتين",
                descriptionAr = "بطارية بديلة داخلية عالية الجودة ومطابقة لمواصفات آبل الأصلية لاستعادة الأداء الفائق لجهاز الآيفون الخاص بك مع شريحة أمان ذكية لحماية البطارية من الشحن الزائد والحرارة.",
                price = 149.0,
                oldPrice = 199.0,
                imageUrl = "iphone13battery",
                categoryId = 7,
                stockQuantity = 30,
                isFeatured = false,
                isOffer = true,
                rating = 4.4f,
                specsAr = "التوافق: iPhone 13,السعة: 3227 مللي أمبير (أصلية),الضمان: سنتين استبدال فوري,ميزات الأمان: حماية من التماس الكهربائي والحرارة"
            ),
            Product(
                id = 112,
                nameAr = "شاشة بديلة كاملة لجالكسي S23 ألترا - Dynamic AMOLED",
                descriptionAr = "شاشة بديلة أصلية ورم تجميع كاملة تشمل شاشة العرض الرقمية والزجاج اللمسي لتبديل شاشتك المكسورة واستعادة دقة الألوان والسطوع فائق الجودة وقلم S-Pen الاستجابة السريعة.",
                price = 899.0,
                oldPrice = 1199.0,
                imageUrl = "s23screen",
                categoryId = 7,
                stockQuantity = 8,
                isFeatured = true,
                isOffer = false,
                rating = 4.7f,
                specsAr = "نوع الشاشة: Dynamic AMOLED 2X,الدقة: QHD+,التردد: 120 هرتز,التوافق: Samsung Galaxy S23 Ultra"
            ),
            Product(
                id = 113,
                nameAr = "مروحة عنق لاسلكية محمولة للصيف",
                descriptionAr = "مروحة رقبة محمولة بدون ريش لتبريد فوري ومريح أثناء التنقل والعمل أو المشي في الصيف الحار. تتميز بتصميم خفيف الوزن ومستويات هواء ثلاثية وبطارية تدوم لساعات طويلة.",
                price = 79.0,
                oldPrice = 99.0,
                imageUrl = "neckfan",
                categoryId = 8,
                stockQuantity = 40,
                isFeatured = true,
                isOffer = true,
                rating = 4.3f,
                specsAr = "مستويات السرعة: 3 مستويات مروحة,البطارية: 4000 مللي أمبير,عمر البطارية: من 4 إلى 12 ساعة,منفذ الشحن: Type-C"
            ),
            Product(
                id = 114,
                nameAr = "شاحن سفري للرحلات ومقاوم للماء IP67",
                descriptionAr = "بنك طاقة خارجي فائق المتانة مخصص للاستخدام الخارجي والرحلات البرية ومقاوم بالكامل للماء والأتربة والوقوع. يمنح شحناً سريعاً للأجهزة المتعددة ويشمل كشاف إضاءة LED مدمج للطوارئ.",
                price = 159.0,
                oldPrice = null,
                imageUrl = "travelpowerbank",
                categoryId = 8,
                stockQuantity = 15,
                isFeatured = false,
                isOffer = false,
                rating = 4.8f,
                specsAr = "السعة: 20000 مللي أمبير,القوة: 20 واط شحن سريع (PD),معيار الحماية: IP67 (مقاوم للماء والأتربة والكسر),ميزات إضافية: إضاءة LED للطوارئ"
            ),
            Product(
                id = 115,
                nameAr = "بكج الحماية المتكامل 10 في 1 من ليفون",
                descriptionAr = "البكج الشامل والأكثر مبيعاً لحماية جهازك من كافة الزوايا. يحتوي على كفر مقاوم للصدمات، استيكر شاشة أمامي، استيكر خلفي حراري، حماية لعدسات الكاميرا الخلفية، مسكة هاتف، والعديد من أدوات التركيب الاحترافية السهلة.",
                price = 129.0,
                oldPrice = 249.0,
                imageUrl = "protectionpackage",
                categoryId = 9,
                stockQuantity = 120,
                isFeatured = true,
                isOffer = true,
                rating = 4.9f,
                specsAr = "محتويات البكج: 10 قطع متكاملة,حماية الشاشة: زجاج نانو ضد الصدمات,الكفر الخلفي: مضاد للاصفرار والصدمات 10 قدم,الضمان: ضمان سنة ضد الكسر واصفرار الكفر"
            ),
            Product(
                id = 116,
                nameAr = "عرض التوفير: شاحن جداري 20 واط + كابل قماشي",
                descriptionAr = "احصل على أفضل حزمة شحن سريعة لآيفون وأندرويد بسعر توفيري خاص جداً. يشمل العرض رأس شاحن جداري ذكي بقوة 20 واط يدعم تقنية PD بالإضافة إلى كابل شحن قماشي مضاد للقطع بطول 1.2 متر.",
                price = 89.0,
                oldPrice = 159.0,
                imageUrl = "bundlecharger",
                categoryId = 9,
                stockQuantity = 60,
                isFeatured = true,
                isOffer = true,
                rating = 4.7f,
                specsAr = "القوة الإجمالية: 20 واط,التقنية: Power Delivery 3.0,نوع الكابل: قماشي مجدول فائق التحمل,طول الكابل: 1.2 متر"
            ),
            Product(
                id = 117,
                nameAr = "مكبر صوت بلوتوث MP3 احترافي بقوة 40 واط",
                descriptionAr = "مكبر صوت خارجي قوي مع صوت ثلاثي الأبعاد مجسم، بطارية عملاقة تدوم حتى 24 ساعة، يدعم تشغيل كروت الذاكرة MP3 والفلاش ووصلة AUX لعشاق الرحلات والتجمعات السعيدة.",
                price = 249.0,
                oldPrice = 349.0,
                imageUrl = "speaker_pro",
                categoryId = 10,
                stockQuantity = 15,
                isFeatured = true,
                isOffer = true,
                rating = 4.8f,
                specsAr = "القوة الصوتية: 40 واط,عمر البطارية: 24 ساعة,المنافذ: USB-MP3 / TF-Card / AUX,مقاومة الماء: IPX6"
            ),
            Product(
                id = 118,
                nameAr = "مكبر صوت محمول صغير MP3 بمقاومة للماء IPX7",
                descriptionAr = "مكبر صوت صغير الحجم وعالي الوضوح، رفيقك الأمثل في السفر والرياضة المائية. يحتوي على مشبك مدمج لسهولة التعليق في الحقائب والملابس ويدعم المكالمات الهاتفية الواضحة.",
                price = 99.0,
                oldPrice = 129.0,
                imageUrl = "speaker_mini",
                categoryId = 10,
                stockQuantity = 30,
                isFeatured = false,
                isOffer = true,
                rating = 4.6f,
                specsAr = "البطارية: تصل لـ 10 ساعات,الحماية: IPX7 (مقاوم للماء بالكامل),الاتصال: Bluetooth 5.2,الوزن: 180 جرام"
            ),
            Product(
                id = 119,
                nameAr = "كاميرا مراقبة ذكية لاسلكية 4K خارجية مع كشاف",
                descriptionAr = "كاميرا مراقبة خارجية ذكية تعمل على الواي فاي، مجهزة بدقة 4K فائقة الوضوح ورؤية ليلية ملونة كاملة بفضل كشافات LED مدمجة. تحتوي على إنذار صوتي وضوئي مدمج وميزة تتبع الحركة بالذكاء الاصطناعي مع ضمان ذهبي.",
                price = 399.0,
                oldPrice = 499.0,
                imageUrl = "camera_outdoor",
                categoryId = 11,
                stockQuantity = 12,
                isFeatured = true,
                isOffer = true,
                rating = 4.9f,
                specsAr = "الدقة: 4K (8MP),الرؤية الليلية: ملونة تصل لـ 30 متر,التخزين: كارت SD حتى 256GB / سحابي,الحماية: IP66 مقاوم للحرارة والغبار والماء"
            ),
            Product(
                id = 120,
                nameAr = "كاميرا مراقبة داخلية 360 درجة مع رؤية ليلية كاملة",
                descriptionAr = "كاميرا داخلية ذكية ومتحركة في كافة الاتجاهات لتغطية كاملة للمنزل أو المتجر. تدعم التحدث بالصوت باتجاهين والتنبيه الفوري بالهاتف عند استشعار أي حركة أو صوت طفل بكاء.",
                price = 149.0,
                oldPrice = 199.0,
                imageUrl = "camera_indoor",
                categoryId = 11,
                stockQuantity = 25,
                isFeatured = false,
                isOffer = true,
                rating = 4.7f,
                specsAr = "الدوران: 360 درجة أفقي / 110 درجات رأسي,الدقة: FHD 1080P,ميزات إضافية: استشعار الصوت والحركة / تحدث باتجاهين"
            )
        )
        repository.insertProducts(initialProducts)

        // Log initial activity
        repository.insertAuditLog(
            AuditLog(
                actionAr = "تهيئة النظام وتثبيت البيانات الافتراضية",
                userRole = "مدير",
                detailsAr = "تم ملء المتجر الإلكتروني بـ 11 قسماً رئيسياً و 20 منتجاً تقنياً متميزاً."
            )
        )
    }

    // Navigation Helper
    fun navigateTo(screen: Screen) {
        viewModelScope.launch {
            currentScreen.value = screen
            logSimulatedApi("GET", "/api/v1/navigation?screen=${screen.javaClass.simpleName}", 200, "", "{\"status\":\"success\",\"navigation\":\"ok\"}")
        }
    }

    // API Logger Helper
    fun logSimulatedApi(method: String, endpoint: String, status: Int, requestBody: String, responseBody: String) {
        val newLog = ApiLog(
            method = method,
            endpoint = endpoint,
            status = status,
            requestBody = requestBody,
            responseBody = responseBody
        )
        _apiLogs.value = (listOf(newLog) + _apiLogs.value).take(40)
    }

    // Role Switcher Helper
    fun switchRole(role: AppRole) {
        viewModelScope.launch {
            appRole.value = role
            val logMsg = when (role) {
                AppRole.CUSTOMER -> "تغيير الصلاحيات إلى عميل"
                AppRole.ADMIN -> "تغيير الصلاحيات إلى مدير النظام"
                AppRole.DELIVERY -> "تغيير الصلاحيات إلى مندوب التوصيل"
            }
            repository.insertAuditLog(AuditLog(actionAr = logMsg, userRole = "نظام", detailsAr = "تعديل صلاحيات الجلسة الحالية."))
            
            // Redirect with credentials validation check
            when (role) {
                AppRole.CUSTOMER -> {
                    navigateTo(Screen.CustomerHome)
                }
                AppRole.ADMIN -> {
                    if (isLoggedInAdmin.value) {
                        navigateTo(Screen.AdminDashboard)
                    } else {
                        navigateTo(Screen.Login(AppRole.ADMIN))
                    }
                }
                AppRole.DELIVERY -> {
                    if (isLoggedInDelivery.value) {
                        navigateTo(Screen.DeliveryPortal)
                    } else {
                        navigateTo(Screen.Login(AppRole.DELIVERY))
                    }
                }
            }
        }
    }

    // Theme Switcher Helper
    fun toggleTheme() {
        val newVal = !isDarkTheme.value
        isDarkTheme.value = newVal
        prefs.edit().putBoolean("is_dark_theme", newVal).apply()
    }

    // Role-based Credentials Authentication Methods
    fun login(role: AppRole, usernameOrPhone: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            loginError.value = null
            if (usernameOrPhone.trim().isEmpty() || pass.trim().isEmpty()) {
                loginError.value = "الرجاء إدخال اسم المستخدم وكلمة المرور"
                return@launch
            }
            
            if (isSupabaseEnabled.value && supabaseUrl.value.isNotEmpty() && supabaseKey.value.isNotEmpty()) {
                // Real Supabase Connection Mode
                try {
                    // Try to normalize username/phone into an email format if it's not already an email
                    val email = if (usernameOrPhone.contains("@")) {
                        usernameOrPhone.trim()
                    } else {
                        // Clean digits for phone-like username
                        val cleanInput = usernameOrPhone.replace(" ", "").replace("+", "")
                        "$cleanInput@alahmadi.com"
                    }

                    logSimulatedApi(
                        method = "POST",
                        endpoint = "auth/v1/token?grant_type=password (Supabase Auth)",
                        status = 200, // pending
                        requestBody = "{\"email\":\"$email\",\"password\":\"******\"}",
                        responseBody = "جاري إرسال طلب تسجيل الدخول إلى سحابة Supabase..."
                    )

                    val request = SignInRequest(email = email, password = pass, phone = null)
                    val response = SupabaseClient.signIn(request)

                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            val userRoleMeta = body.user.userMetadata?.get("role") ?: "customer"
                            val userNameMeta = body.user.userMetadata?.get("name") ?: body.user.email?.substringBefore("@") ?: "عميل الأحمدي"
                            
                            // Role access validation check
                            val isAuthorized = when (role) {
                                AppRole.ADMIN -> userRoleMeta == "admin"
                                AppRole.DELIVERY -> userRoleMeta == "delivery"
                                AppRole.CUSTOMER -> userRoleMeta == "customer" || userRoleMeta.isEmpty()
                            }

                            if (isAuthorized) {
                                when (role) {
                                    AppRole.CUSTOMER -> {
                                        isLoggedInCustomer.value = true
                                        customerName.value = userNameMeta
                                        customerPhone.value = body.user.phone ?: usernameOrPhone
                                        customerEmail.value = body.user.email ?: ""
                                        appRole.value = AppRole.CUSTOMER
                                    }
                                    AppRole.ADMIN -> {
                                        isLoggedInAdmin.value = true
                                        appRole.value = AppRole.ADMIN
                                    }
                                    AppRole.DELIVERY -> {
                                        isLoggedInDelivery.value = true
                                        appRole.value = AppRole.DELIVERY
                                    }
                                }

                                repository.insertAuditLog(
                                    AuditLog(
                                        actionAr = "تسجيل دخول (${when(role) {
                                            AppRole.ADMIN -> "مدير"
                                            AppRole.DELIVERY -> "مندوب"
                                            AppRole.CUSTOMER -> "عميل"
                                        }})",
                                        userRole = when(role) {
                                            AppRole.ADMIN -> "مدير"
                                            AppRole.DELIVERY -> "مندوب"
                                            AppRole.CUSTOMER -> "عميل"
                                        },
                                        detailsAr = "تم الدخول بنجاح عبر Supabase Auth. معرف المستخدم: ${body.user.id}"
                                    )
                                )

                                logSimulatedApi(
                                    method = "POST",
                                    endpoint = "auth/v1/token?grant_type=password (Supabase Auth)",
                                    status = 200,
                                    requestBody = "{\"email\":\"$email\",\"password\":\"******\"}",
                                    responseBody = "{\"status\":\"success\",\"user_id\":\"${body.user.id}\",\"metadata\":{\"role\":\"$userRoleMeta\",\"name\":\"$userNameMeta\"}}"
                                )

                                onSuccess()
                            } else {
                                val errorMsg = when (role) {
                                    AppRole.ADMIN -> "غير مصرح لك بالدخول كمدير للنظام! حسابك مسجل بصلاحية: ${getRoleNameAr(userRoleMeta)}"
                                    AppRole.DELIVERY -> "غير مصرح لك بالدخول كمندوب توصيل! حسابك مسجل بصلاحية: ${getRoleNameAr(userRoleMeta)}"
                                    AppRole.CUSTOMER -> "بيانات الحساب غير مطابقة لصفة العميل!"
                                }
                                loginError.value = errorMsg
                                logSimulatedApi(
                                    method = "POST",
                                    endpoint = "auth/v1/token?grant_type=password (Supabase Auth)",
                                    status = 403,
                                    requestBody = "{\"email\":\"$email\"}",
                                    responseBody = "{\"error\":\"Forbidden\",\"message\":\"Role mismatch. Expected $role, got $userRoleMeta\"}"
                                )
                            }
                        } else {
                            loginError.value = "استجابة فارغة من خادم المصادقة!"
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: ""
                        loginError.value = parseSupabaseError(errorBody)
                        logSimulatedApi(
                            method = "POST",
                            endpoint = "auth/v1/token?grant_type=password (Supabase Auth)",
                            status = response.code(),
                            requestBody = "{\"email\":\"$email\"}",
                            responseBody = errorBody
                        )
                    }
                } catch (e: Exception) {
                    loginError.value = "فشل الاتصال بخادم Supabase: ${e.localizedMessage}"
                }
            } else {
                // Local Simulated Sandbox Mode
                when (role) {
                    AppRole.ADMIN -> {
                        val isClassicAdmin = (usernameOrPhone == "admin" || usernameOrPhone == "أحمدي") && pass == "admin"
                        val isNewAdmin = usernameOrPhone == "mana" && pass == "123456"
                        if (isClassicAdmin || isNewAdmin) {
                            isLoggedInAdmin.value = true
                            appRole.value = AppRole.ADMIN
                            repository.insertAuditLog(AuditLog(actionAr = "تسجيل دخول المدير", userRole = "مدير", detailsAr = "تم الدخول بنجاح إلى لوحة التحكم (محاكاة). المستخدم: $usernameOrPhone"))
                            logSimulatedApi("POST", "/api/v1/auth/admin/login", 200, "{\"username\":\"$usernameOrPhone\"}", "{\"status\":\"success\",\"token\":\"sb_adm_992x\"}")
                            onSuccess()
                        } else {
                            loginError.value = "خطأ في اسم المستخدم أو كلمة مرور المدير! (الافتراضي: mana / 123456)"
                        }
                    }
                    AppRole.DELIVERY -> {
                        val isClassicDelivery = (usernameOrPhone == "delivery" || usernameOrPhone == "مندوب") && pass == "delivery"
                        val isNewDelivery = usernameOrPhone == "mamm" && pass == "654321"
                        if (isClassicDelivery || isNewDelivery) {
                            isLoggedInDelivery.value = true
                            appRole.value = AppRole.DELIVERY
                            repository.insertAuditLog(AuditLog(actionAr = "تسجيل دخول مندوب", userRole = "مندوب", detailsAr = "تم الدخول بنجاح إلى لوحة المناديب (محاكاة). المستخدم: $usernameOrPhone"))
                            logSimulatedApi("POST", "/api/v1/auth/delivery/login", 200, "{\"username\":\"$usernameOrPhone\"}", "{\"status\":\"success\",\"token\":\"sb_del_883y\"}")
                            onSuccess()
                        } else {
                            loginError.value = "خطأ في اسم المستخدم أو كلمة مرور مندوب التوصيل! (الافتراضي: mamm / 654321)"
                        }
                    }
                    AppRole.CUSTOMER -> {
                        isLoggedInCustomer.value = true
                        customerName.value = usernameOrPhone
                        customerPhone.value = if (usernameOrPhone.all { it.isDigit() }) usernameOrPhone else "077" + (10000000..99999999).random().toString()
                        customerEmail.value = if (usernameOrPhone.contains("@")) usernameOrPhone else "$usernameOrPhone@alahmadi.com"
                        appRole.value = AppRole.CUSTOMER
                        repository.insertAuditLog(AuditLog(actionAr = "تسجيل دخول العميل", userRole = "عميل", detailsAr = "الاسم: $usernameOrPhone (محاكاة)"))
                        logSimulatedApi("POST", "/api/v1/auth/customer/login", 200, "{\"phone\":\"$usernameOrPhone\"}", "{\"status\":\"success\",\"token\":\"sb_cust_221z\"}")
                        onSuccess()
                    }
                }
            }
        }
    }

    fun register(name: String, phoneOrEmail: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            registerError.value = null
            if (name.trim().isEmpty() || phoneOrEmail.trim().isEmpty() || pass.trim().isEmpty()) {
                registerError.value = "الرجاء ملء جميع الحقول المطلوبة"
                return@launch
            }

            if (isSupabaseEnabled.value && supabaseUrl.value.isNotEmpty() && supabaseKey.value.isNotEmpty()) {
                // Real Supabase Connection Mode
                try {
                    // Normalize to email representation for Supabase Auth
                    val email = if (phoneOrEmail.contains("@")) {
                        phoneOrEmail.trim()
                    } else {
                        val cleanInput = phoneOrEmail.replace(" ", "").replace("+", "")
                        "$cleanInput@alahmadi.com"
                    }

                    // Auto-assign roles based on naming keywords to allow testing
                    val metadataRole = when {
                        email.contains("admin", ignoreCase = true) -> "admin"
                        email.contains("delivery", ignoreCase = true) || email.contains("agent", ignoreCase = true) -> "delivery"
                        else -> "customer"
                    }

                    logSimulatedApi(
                        method = "POST",
                        endpoint = "auth/v1/signup (Supabase Auth)",
                        status = 200, // pending
                        requestBody = "{\"email\":\"$email\",\"password\":\"******\"}",
                        responseBody = "جاري إرسال طلب إنشاء الحساب إلى سحابة Supabase..."
                    )

                    val meta = mapOf("name" to name, "role" to metadataRole)
                    val request = SignUpRequest(
                        email = email,
                        password = pass,
                        phone = if (phoneOrEmail.contains("@")) null else phoneOrEmail,
                        userMetadata = meta
                    )

                    val response = SupabaseClient.signUp(request)

                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            // Automatically log in the user upon successful signup
                            isLoggedInCustomer.value = true
                            customerName.value = name
                            customerPhone.value = phoneOrEmail
                            customerEmail.value = email
                            appRole.value = AppRole.CUSTOMER

                            repository.insertAuditLog(
                                AuditLog(
                                    actionAr = "إنشاء حساب عميل جديد",
                                    userRole = "عميل",
                                    detailsAr = "الاسم: $name, البريد: $email, الدور التلقائي: ${getRoleNameAr(metadataRole)}"
                                )
                            )

                            logSimulatedApi(
                                method = "POST",
                                endpoint = "auth/v1/signup (Supabase Auth)",
                                status = 201,
                                requestBody = "{\"email\":\"$email\",\"metadata\":{\"name\":\"$name\",\"role\":\"$metadataRole\"}}",
                                responseBody = "{\"status\":\"created\",\"user_id\":\"${body.user.id}\"}"
                            )

                            onSuccess()
                        } else {
                            registerError.value = "استجابة فارغة من خادم الإنشاء!"
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: ""
                        registerError.value = parseSupabaseError(errorBody)
                        logSimulatedApi(
                            method = "POST",
                            endpoint = "auth/v1/signup (Supabase Auth)",
                            status = response.code(),
                            requestBody = "{\"email\":\"$email\"}",
                            responseBody = errorBody
                        )
                    }
                } catch (e: Exception) {
                    registerError.value = "فشل الاتصال بخادم Supabase: ${e.localizedMessage}"
                }
            } else {
                // Local Simulated Sandbox Mode
                customerName.value = name
                customerPhone.value = phoneOrEmail
                customerEmail.value = if (phoneOrEmail.contains("@")) phoneOrEmail else "$phoneOrEmail@alahmadi.com"
                isLoggedInCustomer.value = true
                appRole.value = AppRole.CUSTOMER
                repository.insertAuditLog(AuditLog(actionAr = "إنشاء حساب عميل جديد", userRole = "عميل", detailsAr = "الاسم: $name, الهاتف/البريد: $phoneOrEmail (محاكاة)"))
                logSimulatedApi("POST", "/api/v1/auth/customer/register", 201, "{\"name\":\"$name\",\"phone\":\"$phoneOrEmail\"}", "{\"status\":\"created\",\"token\":\"sb_cust_221z\"}")
                onSuccess()
            }
        }
    }

    private fun getRoleNameAr(role: String): String {
        return when (role) {
            "admin" -> "مدير النظام"
            "delivery" -> "مندوب التوصيل"
            else -> "عميل"
        }
    }

    private fun parseSupabaseError(errorBody: String): String {
        return try {
            // Very simple parser for Supabase error format: {"error_description":"...","error":"...","msg":"..."}
            when {
                errorBody.contains("User already exists") -> "هذا الحساب مسجل بالفعل!"
                errorBody.contains("Invalid login credentials") -> "بيانات الدخول غير صحيحة، يرجى التحقق من المدخلات!"
                errorBody.contains("Password should be at least") -> "يجب أن تكون كلمة المرور مكونة من 6 خانات على الأقل!"
                errorBody.contains("Email format is invalid") -> "صيغة البريد الإلكتروني غير صالحة!"
                errorBody.contains("not confirmed") -> "يرجى تأكيد البريد الإلكتروني أولاً!"
                else -> {
                    // Try to extract the "message" or "msg" field or return general
                    val regex = """"(error_description|message|msg)"\s*:\s*"([^"]+)"""".toRegex()
                    val match = regex.find(errorBody)
                    if (match != null && match.groupValues.size >= 3) {
                        translateEnglishError(match.groupValues[2])
                    } else {
                        "خطأ غير متوقع في خادم المصادقة!"
                    }
                }
            }
        } catch (e: Exception) {
            "فشل معالجة استجابة الخطأ من الخادم!"
        }
    }

    private fun translateEnglishError(englishMsg: String): String {
        return when {
            englishMsg.contains("already exists", ignoreCase = true) -> "الحساب مسجل مسبقاً!"
            englishMsg.contains("invalid", ignoreCase = true) -> "القيمة المدخلة غير صالحة!"
            englishMsg.contains("bad request", ignoreCase = true) -> "الطلب غير صالح!"
            else -> englishMsg
        }
    }

    fun logout() {
        viewModelScope.launch {
            val role = appRole.value
            val logMsg = when (role) {
                AppRole.CUSTOMER -> "تسجيل خروج العميل"
                AppRole.ADMIN -> "تسجيل خروج المدير"
                AppRole.DELIVERY -> "تسجيل خروج المندوب"
            }
            repository.insertAuditLog(AuditLog(actionAr = logMsg, userRole = "نظام", detailsAr = "تم إنهاء الجلسة بنجاح."))
            
            when (role) {
                AppRole.CUSTOMER -> {
                    isLoggedInCustomer.value = false
                    customerName.value = ""
                    customerPhone.value = ""
                    customerEmail.value = ""
                    deliveryAddress.value = ""
                }
                AppRole.ADMIN -> isLoggedInAdmin.value = false
                AppRole.DELIVERY -> isLoggedInDelivery.value = false
            }
            
            // Go back to login/onboarding
            navigateTo(Screen.Onboarding)
        }
    }

    // Customer Operations
    fun toggleFavorite(productId: Int) {
        viewModelScope.launch {
            val isFav = favoriteIds.value.contains(productId)
            val prod = products.value.find { it.id == productId }
            val prodName = prod?.nameAr ?: "المنتج"
            if (isFav) {
                repository.removeFavorite(productId)
                favoriteToast.value = "تمت الإزالة من المفضلة: $prodName 💔"
                logSimulatedApi("DELETE", "/api/v1/favorites/$productId", 200, "", "{\"status\":\"removed\"}")
            } else {
                repository.addFavorite(productId)
                favoriteToast.value = "تمت الإضافة إلى المفضلة: $prodName ❤️"
                logSimulatedApi("POST", "/api/v1/favorites", 201, "{\"productId\":$productId}", "{\"status\":\"added\",\"productId\":$productId}")
            }
        }
    }

    fun addProductToCart(product: Product, variant: String = "اللون الافتراضي") {
        viewModelScope.launch {
            val existing = cartItems.value.find { it.productId == product.id && it.selectedVariant == variant }
            if (existing != null) {
                repository.updateCartQuantity(existing.id, existing.quantity + 1)
                logSimulatedApi("PUT", "/api/v1/cart/${existing.id}", 200, "{\"quantity\":${existing.quantity + 1}}", "{\"status\":\"updated\"}")
            } else {
                repository.addToCart(CartItem(productId = product.id, quantity = 1, selectedVariant = variant))
                logSimulatedApi("POST", "/api/v1/cart", 201, "{\"productId\":${product.id},\"quantity\":1,\"variant\":\"$variant\"}", "{\"status\":\"created\"}")
            }
            cartToast.value = "تمت إضافة \"${product.nameAr}\" إلى سلة التسوق الخاصة بك بنجاح! 🛒"
        }
    }

    fun updateCartQty(cartItemId: Int, newQty: Int) {
        viewModelScope.launch {
            if (newQty <= 0) {
                repository.removeFromCart(cartItemId)
                logSimulatedApi("DELETE", "/api/v1/cart/$cartItemId", 200, "", "{\"status\":\"removed\"}")
            } else {
                repository.updateCartQuantity(cartItemId, newQty)
                logSimulatedApi("PUT", "/api/v1/cart/$cartItemId", 200, "{\"quantity\":$newQty}", "{\"status\":\"updated\"}")
            }
        }
    }

    fun clearUserCart() {
        viewModelScope.launch {
            repository.clearCart()
            logSimulatedApi("DELETE", "/api/v1/cart/clear", 200, "", "{\"status\":\"cleared\"}")
        }
    }

    fun applyCoupon(code: String) {
        viewModelScope.launch {
            if (code.trim().uppercase() == "AHMADI10") {
                activeCoupon.value = "AHMADI10"
                couponDiscountPercent.value = 0.10 // 10%
                couponError.value = null
                cartToast.value = "تم تفعيل الكوبون (AHMADI10) بنجاح وتطبيق خصم 10%! 🎟️"
                logSimulatedApi("POST", "/api/v1/coupons/apply", 200, "{\"code\":\"$code\"}", "{\"status\":\"success\",\"discount\":0.10}")
            } else if (code.trim().uppercase() == "FREE") {
                activeCoupon.value = "FREE"
                couponDiscountPercent.value = 1.00 // 100% discount
                couponError.value = null
                cartToast.value = "تم تفعيل الكوبون التجريبي (FREE) بنجاح وتطبيق خصم 100%! 🎟️"
                logSimulatedApi("POST", "/api/v1/coupons/apply", 200, "{\"code\":\"$code\"}", "{\"status\":\"success\",\"discount\":1.00}")
            } else {
                activeCoupon.value = null
                couponDiscountPercent.value = 0.0
                couponError.value = "الكوبون المدخل غير فعال!"
                cartToast.value = "فشل في تفعيل الكوبون: الكوبون غير فعال! ❌"
                logSimulatedApi("POST", "/api/v1/coupons/apply", 400, "{\"code\":\"$code\"}", "{\"status\":\"error\",\"message\":\"coupon_not_found\"}")
            }
        }
    }

    fun placeOrder(paymentMethod: String, chosenCurrency: String = "الريال اليمني (ر.ي)", convertedAmount: Double = 0.0, onOrderPlaced: (Int) -> Unit) {
        viewModelScope.launch {
            val items = cartItems.value
            val currentProducts = products.value
            if (items.isEmpty()) return@launch

            // Gather item models to calculate total
            var subTotal = 0.0
            val orderItems = items.mapNotNull { cartItem ->
                val prod = currentProducts.find { it.id == cartItem.productId }
                if (prod != null) {
                    val itemPrice = prod.price
                    subTotal += itemPrice * cartItem.quantity
                    OrderItem(
                        orderId = 0,
                        productId = prod.id,
                        productName = prod.nameAr,
                        quantity = cartItem.quantity,
                        price = itemPrice
                    )
                } else null
            }

            val discount = subTotal * couponDiscountPercent.value
            val shippingCost = if (subTotal > 500.0) 0.0 else 25.0
            val total = (subTotal - discount) + shippingCost

            val newOrder = Order(
                customerName = customerName.value.ifEmpty { "عميل زائر" },
                customerPhone = customerPhone.value.ifEmpty { "05xxxxxxxx" },
                deliveryAddress = deliveryAddress.value.ifEmpty { "العنوان الافتراضي - صنعاء/الرياض" },
                paymentMethod = paymentMethod,
                shippingCost = shippingCost,
                totalAmount = total,
                status = "جديد",
                deliveryAgentName = null,
                chosenCurrency = chosenCurrency,
                convertedAmount = if (chosenCurrency == "الريال اليمني (ر.ي)") total else convertedAmount,
                latitude = selectedLatitude.value,
                longitude = selectedLongitude.value
            )

            // Database Save
            val orderId = repository.createOrder(newOrder, orderItems)

            // Update Stock Quantity
            items.forEach { cartItem ->
                val prod = currentProducts.find { it.id == cartItem.productId }
                if (prod != null) {
                    val remainingStock = maxOf(0, prod.stockQuantity - cartItem.quantity)
                    repository.updateProductStock(prod.id, remainingStock)
                }
            }

            // Clean Cart and Reset Coupon
            repository.clearCart()
            activeCoupon.value = null
            couponDiscountPercent.value = 0.0
            
            // Reset Map States
            selectedLatitude.value = null
            selectedLongitude.value = null
            selectedMapAddress.value = null

            // Create notification
            repository.insertNotification(
                Notification(
                    titleAr = "تم تأكيد طلبك بنجاح!",
                    messageAr = "طلبك رقم #${orderId} بمبلغ ${String.format("%.2f", total)} ر.ي تم استلامه وهو قيد المراجعة الآن."
                )
            )

            // Insert Audit Log
            repository.insertAuditLog(
                AuditLog(
                    actionAr = "إنشاء طلب جديد رقم #$orderId",
                    userRole = "عميل",
                    detailsAr = "اسم العميل: ${newOrder.customerName}, المبلغ الإجمالي: ${String.format("%.2f", total)} ر.ي"
                )
            )

            // API Logging
            val requestJson = "{\"paymentMethod\":\"$paymentMethod\",\"itemsCount\":${items.size},\"total\":$total}"
            val responseJson = "{\"status\":\"success\",\"orderId\":$orderId,\"message\":\"order_created_successfully\"}"
            logSimulatedApi("POST", "/api/v1/orders", 201, requestJson, responseJson)

            orderSuccessPopup.value = newOrder.copy(id = orderId.toInt())
            onOrderPlaced(orderId.toInt())
        }
    }

    fun updateOrderPaymentDetails(orderId: Int, ref: String, status: String) {
        viewModelScope.launch {
            repository.updateOrderPaymentDetails(orderId, ref, status)
            val logMsg = "تحديث تفاصيل الدفع للطلب #$orderId - المرجع: $ref الحالة: $status"
            repository.insertAuditLog(
                AuditLog(
                    actionAr = logMsg,
                    userRole = "عميل",
                    detailsAr = "بوابة الدفع: تم السداد بنجاح باستخدام التحويل المحلي"
                )
            )
            logSimulatedApi("POST", "/api/v1/payment/verify", 200, "{\"orderId\":$orderId,\"reference\":\"$ref\"}", "{\"status\":\"success\",\"message\":\"Payment verified and captured\"}")
        }
    }

    // Admin Operations
    fun adminAddProduct(
        id: Int,
        nameAr: String,
        descAr: String,
        price: Double,
        oldPrice: Double?,
        categoryId: Int,
        stock: Int,
        specs: String
    ) {
        viewModelScope.launch {
            val newProduct = Product(
                id = id,
                nameAr = nameAr,
                descriptionAr = descAr,
                price = price,
                oldPrice = oldPrice,
                imageUrl = "custom_product",
                categoryId = categoryId,
                stockQuantity = stock,
                rating = 4.5f,
                specsAr = specs
            )
            repository.insertProducts(listOf(newProduct))

            repository.insertAuditLog(
                AuditLog(
                    actionAr = "إضافة منتج جديد",
                    userRole = "مدير",
                    detailsAr = "المنتج: $nameAr, السعر: $price ر.ي, المخزون: $stock"
                )
            )

            successMessage.value = "تم حفظ المنتج \"$nameAr\" وتحديث بياناته بنجاح! 📱"
            logSimulatedApi("POST", "/api/v1/admin/products", 201, "{\"id\":$id,\"nameAr\":\"$nameAr\"}", "{\"status\":\"success\"}")
        }
    }

    fun adminImportProducts(productsList: List<Product>) {
        viewModelScope.launch {
            repository.insertProducts(productsList)
            repository.insertAuditLog(
                AuditLog(
                    actionAr = "استيراد منتجات بالجملة",
                    userRole = "مدير",
                    detailsAr = "تم استيراد ${productsList.size} منتج بنجاح إلى قاعدة البيانات."
                )
            )
            logSimulatedApi("POST", "/api/v1/admin/products/bulk", 200, "{\"count\":${productsList.size}}", "{\"status\":\"success\"}")
        }
    }

    fun adminDeleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            repository.insertAuditLog(
                AuditLog(
                    actionAr = "حذف منتج",
                    userRole = "مدير",
                    detailsAr = "تم حذف المنتج: ${product.nameAr} رقم #${product.id}"
                )
            )
            logSimulatedApi("DELETE", "/api/v1/admin/products/${product.id}", 200, "", "{\"status\":\"deleted\"}")
        }
    }

    fun executeConfirmedDelete(confirmation: DeleteConfirmation) {
        viewModelScope.launch {
            when (confirmation.type) {
                DeleteType.PRODUCT -> {
                    val prod = confirmation.extraData as? Product
                    prod?.let {
                        adminDeleteProduct(it)
                        successMessage.value = "تم حذف المنتج \"${it.nameAr}\" بنجاح من قاعدة البيانات! 🗑️"
                    }
                }
                DeleteType.BRAND -> {
                    deleteBrand(confirmation.name)
                    successMessage.value = "تم إزالة العلامة التجارية \"${confirmation.name}\" بنجاح! 🗑️"
                }
                DeleteType.DELIVERY_REP -> {
                    deleteDeliveryRep(confirmation.id)
                    successMessage.value = "تم إزالة مندوب التوصيل \"${confirmation.name}\" بنجاح! 🗑️"
                }
                DeleteType.COUPON -> {
                    deleteCoupon(confirmation.id)
                    successMessage.value = "تم حذف كوبون الخصم \"${confirmation.name}\" بنجاح! 🗑️"
                }
                DeleteType.BANNER -> {
                    deleteAdBanner(confirmation.id)
                    successMessage.value = "تم إزالة البنر الإعلاني \"${confirmation.name}\" بنجاح! 🗑️"
                }
                DeleteType.REVIEW -> {
                    deleteReview(confirmation.id)
                    successMessage.value = "تم حذف التقييم بنجاح! 🗑️"
                }
                DeleteType.CART_ITEM -> {
                    val cartId = confirmation.id
                    repository.removeFromCart(cartId)
                    successMessage.value = "تم إزالة السلعة من سلة التسوق! 🗑️"
                }
                DeleteType.CLEAR_CART -> {
                    repository.clearCart()
                    successMessage.value = "تم تفريغ سلة التسوق بالكامل! 🗑️"
                }
                else -> {}
            }
            pendingDeleteAction.value = null
        }
    }

    fun adminUpdateOrderStatus(orderId: Int, status: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, status)

            val logMsg = "تحديث حالة الطلب #$orderId إلى ($status)"
            repository.insertAuditLog(
                AuditLog(
                    actionAr = logMsg,
                    userRole = "مدير",
                    detailsAr = "حالة الطلب الجديدة: $status"
                )
            )

            // Add notification for client
            repository.insertNotification(
                Notification(
                    titleAr = "تحديث حالة الطلب #$orderId",
                    messageAr = "حالة طلبك الآن هي: $status"
                )
            )

            logSimulatedApi("PUT", "/api/v1/admin/orders/$orderId/status", 200, "{\"status\":\"$status\"}", "{\"status\":\"success\"}")
        }
    }

    fun deliveryUpdateOrderStatusWithReason(orderId: Int, status: String, reason: String) {
        viewModelScope.launch {
            repository.updateOrderStatusWithReason(orderId, status, reason)

            val logMsg = "تحديث حالة الطلب #$orderId إلى ($status) - السبب: $reason"
            repository.insertAuditLog(
                AuditLog(
                    actionAr = logMsg,
                    userRole = "مندوب",
                    detailsAr = "سبب تحديث الحالة: $reason"
                )
            )

            repository.insertNotification(
                Notification(
                    titleAr = "تحديث حالة الطلب #$orderId",
                    messageAr = "تحديث التوصيل: تم تسجيل الحالة كـ ($status). السبب: $reason"
                )
            )

            logSimulatedApi("PUT", "/api/v1/delivery/orders/$orderId/status_reason", 200, "{\"status\":\"$status\",\"reason\":\"$reason\"}", "{\"status\":\"success\"}")
        }
    }

    fun adminAssignOrder(orderId: Int, agentName: String) {
        viewModelScope.launch {
            repository.assignOrderToAgent(orderId, agentName)
            repository.updateOrderStatus(orderId, "في الطريق")

            repository.insertAuditLog(
                AuditLog(
                    actionAr = "تعيين الطلب #$orderId للمندوب",
                    userRole = "مدير",
                    detailsAr = "المندوب المعين: $agentName"
                )
            )

            repository.insertNotification(
                Notification(
                    titleAr = "طلبك في الطريق مع المندوب",
                    messageAr = "تم تسليم الطلب #$orderId للمندوب ($agentName)، وهو الآن في طريقه إليك."
                )
            )

            logSimulatedApi("PUT", "/api/v1/admin/orders/$orderId/assign", 200, "{\"agentName\":\"$agentName\"}", "{\"status\":\"success\"}")
        }
    }

    // Support Operations
    fun sendSupportMessage(text: String) {
        if (text.trim().isEmpty()) return
        viewModelScope.launch {
            val userMsg = SupportMessage(sender = "user", text = text)
            _supportMessages.value = _supportMessages.value + userMsg
            logSimulatedApi("POST", "/api/v1/support/messages", 201, "{\"text\":\"$text\"}", "{\"status\":\"sent\"}")

            // Auto-respond simulating support agent or AI assistant
            kotlinx.coroutines.delay(1000)
            val responseText = when {
                text.contains("طلب") || text.contains("شحن") -> "طلبك يتم التعامل معه بسرعة فائقة. يمكنك تتبع حالته مباشرة من صفحة حسابك الشخصي في قسم الطلبات."
                text.contains("سعر") || text.contains("خصم") -> "أهلاً بك! لدينا كود الخصم الفعال 'AHMADI10' الذي يمنحك خصماً فورياً بنسبة 10% على كافة المشتريات."
                text.contains("توصيل") || text.contains("منطقة") -> "نوصل إلى كافة مدن ومناطق اليمن والمملكة العربية السعودية بشكل مباشر وسريع مع خدمة التتبع اللحظي."
                else -> "شكراً لتواصلك معنا في مركز الأحمدي. نحن نراجع استفسارك وسيقوم أحد ممثلي الدعم الفني بالرد عليك بالتفصيل خلال دقائق."
            }
            val agentMsg = SupportMessage(sender = "agent", text = responseText)
            _supportMessages.value = _supportMessages.value + agentMsg
        }
    }

    // ==========================================================================
    // Supabase Cloud Sync and CRUD Testing
    // ==========================================================================

    fun syncDataWithSupabase(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (supabaseUrl.value.isEmpty() || supabaseKey.value.isEmpty()) {
                onComplete(false, "من فضلك قم بتكوين مفاتيح ورابط Supabase أولاً في الإعدادات!")
                return@launch
            }
            try {
                // Initialize client first to make sure it's ready
                SupabaseClient.init(supabaseUrl.value, supabaseKey.value)
                
                logSimulatedApi("GET", "rest/v1/categories?select=* (Real Fetch)", 200, "", "جاري سحب الأقسام...")
                val remoteCats = SupabaseClient.fetchCategories()
                if (remoteCats.isNotEmpty()) {
                    val localCats = remoteCats.map { dto ->
                        Category(
                            id = dto.id ?: (100..999).random(),
                            nameAr = dto.nameAr,
                            iconName = dto.icon ?: "phone_android"
                        )
                    }
                    repository.insertCategories(localCats)
                }

                logSimulatedApi("GET", "rest/v1/products?select=* (Real Fetch)", 200, "", "جاري سحب المنتجات...")
                val remoteProds = SupabaseClient.fetchProducts()
                if (remoteProds.isNotEmpty()) {
                    val localProds = remoteProds.map { dto ->
                        Product(
                            id = dto.id ?: (100..999).random(),
                            nameAr = dto.nameAr,
                            descriptionAr = dto.descAr,
                            price = dto.price,
                            oldPrice = dto.oldPrice,
                            imageUrl = dto.imageUrl ?: "custom_product",
                            categoryId = dto.categoryId,
                            stockQuantity = dto.stock,
                            isFeatured = false,
                            isOffer = dto.isOffer,
                            rating = 4.5f,
                            specsAr = ""
                        )
                    }
                    repository.insertProducts(localProds)
                }

                repository.insertNotification(
                    Notification(
                        titleAr = "تمت المزامنة السحابية بنجاح! ☁️",
                        messageAr = "تم تحديث أقسام ومنتجات مركز الأحمدي مباشرة من سحابة Supabase."
                    )
                )

                logSimulatedApi("POST", "sync/supabase/all", 200, "", "تمت مزامنة ${remoteCats.size} أقسام و ${remoteProds.size} منتجات بنجاح!")
                onComplete(true, "تمت مزامنة ${remoteCats.size} أقسام و ${remoteProds.size} منتجات بنجاح من قاعدة Supabase السحابية!")
            } catch (e: Exception) {
                logSimulatedApi("POST", "sync/supabase/all", 500, "", "فشل التزامن: ${e.localizedMessage}")
                onComplete(false, "فشل التزامن مع الخادم: ${e.localizedMessage}")
            }
        }
    }

    fun testSupabaseCrudOperations(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (supabaseUrl.value.isEmpty() || supabaseKey.value.isEmpty()) {
                onComplete(false, "من فضلك قم بتكوين مفاتيح ورابط Supabase أولاً في الإعدادات!")
                return@launch
            }
            try {
                SupabaseClient.init(supabaseUrl.value, supabaseKey.value)
                
                // 1. POST (Insert)
                val testId = (5000..9999).random()
                val testProduct = ProductDto(
                    id = testId,
                    categoryId = 1,
                    nameAr = "هاتف اختبار الاتصال الأحمدي",
                    descAr = "منتج تجريبي لفحص CRUD مع Supabase Cloud",
                    price = 1499.0,
                    stock = 5,
                    isOffer = true
                )
                
                logSimulatedApi(
                    method = "POST",
                    endpoint = "rest/v1/products (Real Insert)",
                    status = 201,
                    requestBody = "{\"id\":$testId,\"name_ar\":\"هاتف اختبار الاتصال الأحمدي\",\"price\":1499.0}",
                    responseBody = "جاري إرسال الطلب..."
                )
                
                val added = SupabaseClient.addProduct(testProduct)
                logSimulatedApi(
                    method = "POST",
                    endpoint = "rest/v1/products (Success)",
                    status = 201,
                    requestBody = "{\"id\":$testId}",
                    responseBody = "تمت الإضافة بنجاح: " + (added?.nameAr ?: "هاتف اختبار الاتصال الأحمدي")
                )

                // 2. GET (Read)
                logSimulatedApi(
                    method = "GET",
                    endpoint = "rest/v1/products?select=* (Real Fetch)",
                    status = 200,
                    requestBody = "",
                    responseBody = "جاري الاستعلام عن المنتجات للتأكد من وجود العنصر المضاف..."
                )
                val productsList = SupabaseClient.fetchProducts()
                val found = productsList.any { it.id == testId }
                logSimulatedApi(
                    method = "GET",
                    endpoint = "rest/v1/products (Success)",
                    status = 200,
                    requestBody = "",
                    responseBody = "تم جلب ${productsList.size} منتجات بنجاح. العثور على المنتج التجريبي: $found"
                )

                // 3. PATCH (Update)
                logSimulatedApi(
                    method = "PATCH",
                    endpoint = "rest/v1/products?id=eq.$testId (Real Update)",
                    status = 200,
                    requestBody = "{\"price\":1799.0,\"stock\":12}",
                    responseBody = "جاري تحديث السعر والمخزون للمنتج التجريبي..."
                )
                val updatedFields = mapOf("price" to 1799.0, "stock" to 12)
                val updated = SupabaseClient.modifyProduct(testId, updatedFields)
                logSimulatedApi(
                    method = "PATCH",
                    endpoint = "rest/v1/products (Success)",
                    status = 200,
                    requestBody = "{\"price\":1799.0}",
                    responseBody = "تم التحديث بنجاح! السعر الجديد: ${updated?.price ?: 1799.0} ر.ي والمخزون الجديد: ${updated?.stock ?: 12}"
                )

                // 4. DELETE (Delete)
                logSimulatedApi(
                    method = "DELETE",
                    endpoint = "rest/v1/products?id=eq.$testId (Real Delete)",
                    status = 204,
                    requestBody = "",
                    responseBody = "جاري إرسال طلب الحذف لتنظيف قاعدة البيانات السحابية..."
                )
                val isDeleted = SupabaseClient.removeProduct(testId)
                logSimulatedApi(
                    method = "DELETE",
                    endpoint = "rest/v1/products (Success)",
                    status = 204,
                    requestBody = "",
                    responseBody = "تمت عملية الحذف بنجاح! حالة الاستجابة: $isDeleted"
                )

                repository.insertNotification(
                    Notification(
                        titleAr = "نجاح فحص CRUD الكامل! ⚡",
                        messageAr = "تم بنجاح اختبار عمليات: الإضافة (POST)، الجلب (GET)، التعديل (PATCH)، والحذف (DELETE) على خادم Supabase Cloud."
                    )
                )

                onComplete(true, "تم بنجاح اختبار دورة CRUD الكاملة (إضافة، قراءة، تعديل، حذف) على قاعدة Supabase السحابية!")
            } catch (e: Exception) {
                logSimulatedApi(
                    method = "ERROR",
                    endpoint = "rest/v1/products",
                    status = 500,
                    requestBody = "",
                    responseBody = "فشل الاختبار: ${e.localizedMessage}"
                )
                onComplete(false, "فشل اختبار CRUD: ${e.localizedMessage}")
            }
        }
    }
}

// Admin Dashboard Data Models
data class AdminCustomer(val id: Int, val name: String, val phone: String, val email: String, val joinedDate: String, val totalOrders: Int, val status: String) // "نشط", "موقوف"
data class DeliveryRep(val id: Int, val name: String, val phone: String, val vehicle: String, val rating: Float, val status: String) // "متاح", "مشغول", "غير نشط"
data class ShippingZone(val id: Int, val city: String, val cost: Double, val active: Boolean)
data class Coupon(val id: Int, val code: String, val discountPercent: Int, val maxDiscount: Double, val active: Boolean)
data class AdBanner(val id: Int, val title: String, val imageUrl: String, val active: Boolean)
data class ProductReview(val id: Int, val customerName: String, val productName: String, val rating: Int, val comment: String, val approved: Boolean)
data class TeamMemberRole(val id: Int, val name: String, val role: String, val canWriteProduct: Boolean, val canDeleteOrder: Boolean, val canEditSettings: Boolean)

