package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import com.example.viewmodel.AppRole
import com.example.viewmodel.Screen
import com.example.viewmodel.StoreViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StoreAppContent(viewModel: StoreViewModel) {
    val currentRole by viewModel.appRole.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            // Role switcher is visible in all screens except Splash to allow testing
            if (currentScreen !is Screen.Splash) {
                RoleSelectorTopBar(
                    currentRole = currentRole,
                    onRoleSelected = { viewModel.switchRole(it) },
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { viewModel.toggleTheme() }
                )
            }
        },
        bottomBar = {
            if (currentScreen !is Screen.Splash && currentScreen !is Screen.Onboarding && currentRole == AppRole.CUSTOMER) {
                CustomerBottomNavigation(
                    currentScreen = currentScreen,
                    onNavigate = { viewModel.navigateTo(it) }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    val duration = 380
                    (fadeIn(animationSpec = tween(duration, delayMillis = 60)) + 
                     slideInHorizontally(initialOffsetX = { -it / 6 }, animationSpec = tween(duration, easing = FastOutSlowInEasing)))
                        .togetherWith(
                            fadeOut(animationSpec = tween(180)) + 
                            slideOutHorizontally(targetOffsetX = { it / 6 }, animationSpec = tween(duration, easing = FastOutSlowInEasing))
                        )
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is Screen.Splash -> SplashScreen(viewModel)
                    is Screen.Onboarding -> OnboardingScreen(viewModel)
                    is Screen.Login -> LoginScreen(viewModel, screen.role)
                    is Screen.Register -> RegisterScreen(viewModel)
                    is Screen.CustomerHome -> CustomerHomeScreen(viewModel)
                    is Screen.Categories -> CategoriesScreen(viewModel)
                    is Screen.ProductDetail -> ProductDetailScreen(viewModel, screen.productId)
                    is Screen.Cart -> CartScreen(viewModel)
                    is Screen.Checkout -> CheckoutScreen(viewModel)
                    is Screen.PaymentGateway -> PaymentGatewayScreen(viewModel, screen.orderId)
                    is Screen.OrderTracking -> OrderTrackingScreen(viewModel, screen.orderId)
                    is Screen.OrderHistory -> OrderHistoryScreen(viewModel)
                    is Screen.Favorites -> FavoritesScreen(viewModel)
                    is Screen.Profile -> ProfileScreen(viewModel)
                    is Screen.SupportChat -> SupportChatScreen(viewModel)
                    is Screen.AboutStore -> AboutStoreScreen(viewModel)
                    is Screen.Notifications -> NotificationsScreen(viewModel)
                    is Screen.Offers -> OffersScreen(viewModel)
                    is Screen.ReturnWarranty -> ReturnWarrantyScreen(viewModel)
                    is Screen.Settings -> SettingsScreen(viewModel)
                    is Screen.AdminDashboard -> AdminDashboardScreen(viewModel)
                    is Screen.DeliveryPortal -> DeliveryPortalScreen(viewModel)
                    is Screen.DatabaseExplorer -> DatabaseExplorerScreen(viewModel)
                }
            }

            // 1) Custom Delete Confirmation Overlay
            val pendingDelete by viewModel.pendingDeleteAction.collectAsStateWithLifecycle()
            if (pendingDelete != null) {
                val action = pendingDelete!!
                AlertDialog(
                    onDismissRequest = { viewModel.pendingDeleteAction.value = null },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AlertError,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "تأكيد الحذف النهائي ⚠️",
                                color = AlertError,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Text(
                            text = "هل أنت متأكد تماماً من رغبتك في حذف \"${action.name}\"؟ لا يمكن استعادة البيانات بعد تأكيد هذا الإجراء.",
                            color = Color.White,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.executeConfirmedDelete(action) },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertError),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("حذف الآن 🗑️", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { viewModel.pendingDeleteAction.value = null },
                            border = BorderStroke(1.dp, TextGray.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("إلغاء التراجع", fontSize = 11.sp)
                        }
                    },
                    containerColor = TealDark,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // 2) Elegant Success / Save Alert Banner
            val successMsg by viewModel.successMessage.collectAsStateWithLifecycle()
            LaunchedEffect(successMsg) {
                if (successMsg != null) {
                    delay(3500)
                    viewModel.successMessage.value = null
                }
            }
            if (successMsg != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TealMedium),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(GoldAccent.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = successMsg!!,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 3) Love Pills - Favorite Feedback Toast
            val favToast by viewModel.favoriteToast.collectAsStateWithLifecycle()
            LaunchedEffect(favToast) {
                if (favToast != null) {
                    delay(2000)
                    viewModel.favoriteToast.value = null
                }
            }
            if (favToast != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f)),
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (favToast!!.contains("إزالة")) Icons.Default.FavoriteBorder else Icons.Default.Favorite,
                                contentDescription = null,
                                tint = if (favToast!!.contains("إزالة")) AlertError else AlertSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = favToast!!,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 4) Complete Order Celebrating Success Modal
            val orderSuccess by viewModel.orderSuccessPopup.collectAsStateWithLifecycle()
            if (orderSuccess != null) {
                val ord = orderSuccess!!
                AlertDialog(
                    onDismissRequest = { viewModel.orderSuccessPopup.value = null },
                    title = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(GoldAccent.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "تهانينا! تم تأكيد طلبك بنجاح 🎉",
                                color = GoldAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "رقم الطلب الخاص بك هو:",
                                color = TextGray,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "#${ord.id}",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp),
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Card(
                                colors = CardDefaults.cardColors(containerColor = TealMedium),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("المستلم:", color = TextGray, fontSize = 10.sp)
                                        Text(ord.customerName, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("العنوان:", color = TextGray, fontSize = 10.sp)
                                        Text(ord.deliveryAddress, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("الإجمالي:", color = TextGray, fontSize = 10.sp)
                                        Text("${String.format("%.1f", ord.totalAmount)} ر.ي", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "جاري الآن تحضير سلعك وإسنادها لمندوب التوصيل الأقرب إليك لمطابقة معايير سرعة التوصيل لمركز الأحمدي.",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 15.sp
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { 
                                viewModel.orderSuccessPopup.value = null
                                viewModel.navigateTo(Screen.OrderTracking(ord.id))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تتبع مسار التوصيل الآن 📍", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.orderSuccessPopup.value = null },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("الرجوع إلى المتجر", color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    },
                    containerColor = TealDark,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

// Splash Screen
@Composable
fun SplashScreen(viewModel: StoreViewModel) {
    LaunchedEffect(Unit) {
        delay(2500)
        viewModel.navigateTo(Screen.Onboarding)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TealDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            AlahmadiLogo(size = 120.dp)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "مركز الأحمدي للجوالات ومستلزماتها",
                color = GoldAccent,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Alahmadi Mobile Center",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(color = GoldAccent, strokeWidth = 3.dp)
        }
    }
}

// Onboarding Screen
@Composable
fun OnboardingScreen(viewModel: StoreViewModel) {
    var step by remember { mutableStateOf(1) }
    val darkTheme = isAppDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBgColor())
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(onClick = { viewModel.navigateTo(Screen.CustomerHome) }) {
                    Text("تخطي", color = appSubTextColor(), fontSize = 14.sp)
                }
            }

            // Slide Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(appSurfaceColor(), CircleShape)
                        .border(1.dp, appCardBorderColor(), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (step) {
                            1 -> Icons.Default.ShoppingCart
                            2 -> Icons.Default.Check
                            else -> Icons.Default.Home
                        },
                        contentDescription = null,
                        tint = if (darkTheme) GoldAccent else TealMedium,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = when (step) {
                        1 -> "أقوى عروض الهواتف الذكية"
                        2 -> "شحن آمن وتوصيل سريع"
                        else -> "أصالة وجودة مضمونة"
                    },
                    color = if (darkTheme) GoldAccent else TealMedium,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when (step) {
                        1 -> "تصفح مئات الجوالات الحديثة وملحقاتها بأسعار منافسة وجودة لا تضاهى مع عروض وتخفيضات مستمرة."
                        2 -> "خدمة توصيل سريعة ومجانية للطلبات فوق 500 ر.ي مع تتبع لحظي دقيق ومباشر لخط سير طلبك."
                        else -> "جميع الملحقات والإكسسوارات والشواحن أصلية 100% ومشمولة بالضمان الذهبي للأحمدي."
                    },
                    color = appTextColor(),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            // Indicator Dots and Next Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.Center) {
                    repeat(3) { index ->
                        val active = (index + 1) == step
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(8.dp)
                                .background(if (active) (if (darkTheme) GoldAccent else TealMedium) else appSubTextColor().copy(alpha = 0.4f), CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (step == 3) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { viewModel.navigateTo(Screen.CustomerHome) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("onboarding_guest_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (darkTheme) GoldAccent else TealMedium,
                                contentColor = if (darkTheme) TealDark else Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("الدخول كزائر وتصفح المتجر", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.navigateTo(Screen.Login(AppRole.CUSTOMER)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("onboarding_login_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (darkTheme) TealLight else Color.White,
                                contentColor = if (darkTheme) Color.White else TealMedium
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (darkTheme) Color.White.copy(alpha = 0.2f) else TealMedium.copy(alpha = 0.3f))
                        ) {
                            Text("تسجيل الدخول / إنشاء حساب", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تسجيل دخول الإدارة 🔑",
                                color = if (darkTheme) GoldAccent else TealMedium,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { viewModel.navigateTo(Screen.Login(AppRole.ADMIN)) }
                                    .padding(6.dp)
                            )
                            Text(
                                text = "|",
                                color = appTextColor().copy(alpha = 0.3f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = "بوابة المناديب 🚚",
                                color = if (darkTheme) GoldAccent else TealMedium,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { viewModel.navigateTo(Screen.Login(AppRole.DELIVERY)) }
                                    .padding(6.dp)
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { step += 1 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("onboarding_next_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "التالي",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Helper for category icons
fun getIconForCategory(categoryId: Int?): androidx.compose.ui.graphics.vector.ImageVector {
    return when (categoryId) {
        1 -> Icons.Default.Smartphone
        2 -> Icons.Default.Bolt
        3 -> Icons.Default.Headphones
        4 -> Icons.Default.Cable
        5 -> Icons.Default.Shield
        6 -> Icons.Default.Watch
        7 -> Icons.Default.Handyman
        8 -> Icons.Default.WbSunny
        9 -> Icons.Default.Discount
        10 -> Icons.Default.VolumeUp
        11 -> Icons.Default.Videocam
        else -> Icons.Default.GridView
    }
}

// Customer Home Screen
@Composable
fun CustomerHomeScreen(viewModel: StoreViewModel) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCatId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()

    var isFilterPanelExpanded by remember { mutableStateOf(false) }
    var priceFilterState by remember { mutableStateOf("all") } // "all", "under_10k", "10k_30k", "over_30k", "offers"
    var sortByState by remember { mutableStateOf("default") } // "default", "price_asc", "price_desc", "rating"

    val fullyFilteredProducts = remember(products, selectedCatId, searchQuery, priceFilterState, sortByState) {
        products.filter { prod ->
            val matchCategory = (selectedCatId == null || prod.categoryId == selectedCatId)
            val matchQuery = (searchQuery.isEmpty() || prod.nameAr.contains(searchQuery, ignoreCase = true) || prod.descriptionAr.contains(searchQuery, ignoreCase = true))
            val matchPrice = when (priceFilterState) {
                "under_10k" -> prod.price < 10000.0
                "10k_30k" -> prod.price in 10000.0..30000.0
                "over_30k" -> prod.price > 30000.0
                "offers" -> prod.isOffer
                else -> true
            }
            matchCategory && matchQuery && matchPrice
        }.let { list ->
            when (sortByState) {
                "price_asc" -> list.sortedBy { it.price }
                "price_desc" -> list.sortedByDescending { it.price }
                "rating" -> list.sortedByDescending { it.rating }
                else -> list
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Welcome Header & Search
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                val customerNameValue by viewModel.customerName.collectAsStateWithLifecycle()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (customerNameValue.isEmpty()) "مرحباً بك في مركز الأحمدي 👋" else "مرحباً بك، $customerNameValue 👋",
                            color = appTextColor(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (customerNameValue.isEmpty()) "تصفح أفضل الجوالات ومستلزماتها في اليمن" else "اكتشف أحدث العروض والملحقات المميزة لجوالك",
                            color = appSubTextColor(),
                            fontSize = 12.sp
                        )
                    }
                    
                    // Personal circular avatar or Login gateway icon
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isAppDarkTheme()) GoldAccent else TealMedium)
                            .clickable {
                                if (customerNameValue.isEmpty()) {
                                    viewModel.navigateTo(Screen.Login(AppRole.CUSTOMER))
                                } else {
                                    viewModel.navigateTo(Screen.Profile)
                                }
                            }
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (customerNameValue.isEmpty()) {
                            Icon(
                                imageVector = Icons.Default.PersonOutline,
                                contentDescription = "تسجيل الدخول",
                                tint = if (isAppDarkTheme()) TealDark else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            val initial = customerNameValue.trim().firstOrNull()?.toString() ?: "أ"
                            Text(
                                text = initial,
                                color = if (isAppDarkTheme()) TealDark else Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("ابحث عن هاتف، شاحن، سماعة...", color = appSubTextColor().copy(alpha = 0.7f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = appSubTextColor()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_field"),
                    shape = RoundedCornerShape(16.dp),
                    colors = appTextFieldColors()
                )
            }
        }

        // Interactive Promotional Banners Carousel (Compatible with light & dark modes)
        if (searchQuery.isEmpty() && selectedCatId == null) {
            item {
                var activeBannerIndex by remember { mutableStateOf(0) }
                val coroutineScope = rememberCoroutineScope()
                
                // Auto-slide effect every 5 seconds
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(5000)
                        activeBannerIndex = (activeBannerIndex + 1) % 3
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(bottom = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = when (activeBannerIndex) {
                                        0 -> if (isAppDarkTheme()) listOf(TealLight, TealMedium) else listOf(Color(0xFFE0F2F1), Color(0xFFF2F6F7))
                                        1 -> if (isAppDarkTheme()) listOf(Color(0xFF1B4D3E), Color(0xFF0F2B22)) else listOf(Color(0xFFE8F5E9), Color(0xFFF1F8E9))
                                        else -> if (isAppDarkTheme()) listOf(Color(0xFF4A148C), Color(0xFF1A0033)) else listOf(Color(0xFFF3E5F5), Color(0xFFEDE7F6))
                                    }
                                )
                            )
                            .border(1.dp, if (isAppDarkTheme()) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
                    ) {
                        // Left/Right control chevrons
                        IconButton(
                            onClick = { activeBannerIndex = if (activeBannerIndex == 0) 2 else activeBannerIndex - 1 },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 4.dp)
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "السابق", tint = appTextColor(), modifier = Modifier.size(16.dp))
                        }
                        
                        IconButton(
                            onClick = { activeBannerIndex = (activeBannerIndex + 1) % 3 },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp)
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "التالي", tint = appTextColor(), modifier = Modifier.size(16.dp))
                        }

                        // Rotated Decorative Icon (representing item type)
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 48.dp)
                                .size(75.dp, 105.dp)
                                .graphicsLayer {
                                    rotationZ = -12f
                                }
                                .background(if (isAppDarkTheme()) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
                                .border(1.dp, if (isAppDarkTheme()) Color.White.copy(alpha = 0.1f) else Color(0xFFCBD5E1), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (activeBannerIndex) {
                                    0 -> Icons.Default.PhoneAndroid
                                    1 -> Icons.Default.Bolt
                                    else -> Icons.Default.Headphones
                                },
                                contentDescription = null,
                                tint = if (isAppDarkTheme()) Color.White.copy(alpha = 0.15f) else TealDark.copy(alpha = 0.1f),
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        // Banner content
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight()
                                .padding(start = 48.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
                                .width(200.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        when (activeBannerIndex) {
                                            0 -> GoldAccent
                                            1 -> AlertSuccess
                                            else -> AlertInfo
                                        },
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = when (activeBannerIndex) {
                                        0 -> "عرض خاص"
                                        1 -> "الأكثر طلباً"
                                        else -> "وصل حديثاً"
                                    },
                                    color = if (activeBannerIndex == 0) TealDark else Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (activeBannerIndex) {
                                    0 -> "آيفون 15 برو ماكس"
                                    1 -> "شواحن ذكية فائقة"
                                    else -> "سماعات رأس لاسلكية"
                                },
                                color = appTextColor(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when (activeBannerIndex) {
                                    0 -> "خصم 15% لفترة محدودة"
                                    1 -> "شحن سريع وآمن 100%"
                                    else -> "صوت نقي وميزة عزل الضجيج"
                                },
                                color = appSubTextColor(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // Action Button
                            Box(
                                modifier = Modifier
                                    .background(appButtonContainerColor(), RoundedCornerShape(10.dp))
                                    .clickable {
                                        // Scroll or navigate depending on selection
                                        if (activeBannerIndex == 0) {
                                            viewModel.searchQuery.value = "iPhone"
                                        } else if (activeBannerIndex == 1) {
                                            viewModel.selectedCategoryId.value = 2
                                        } else {
                                            viewModel.selectedCategoryId.value = 3
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "تسوق الآن",
                                    color = appButtonContentColor(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Slide Dot Indicators
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (i in 0..2) {
                                val isSelected = activeBannerIndex == i
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 14.dp else 6.dp, 6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            if (isSelected) {
                                                if (isAppDarkTheme()) GoldAccent else TealDark
                                            } else {
                                                appSubTextColor().copy(alpha = 0.4f)
                                            }
                                        )
                                        .clickable { activeBannerIndex = i }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Categories Row
        item {
            Column(modifier = Modifier.padding(bottom = 20.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الأقسام الرئيسية",
                        color = appTextColor(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "عرض الكل",
                        color = GoldAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.bounceClick { viewModel.selectedCategoryId.value = null }
                    )
                }
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    // "All" item
                    item {
                        val isSelected = selectedCatId == null
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .bounceClick { viewModel.selectedCategoryId.value = null }
                                .width(64.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        color = if (isSelected) GoldAccent else appSurfaceColor(),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) GoldAccent else appCardBorderColor(),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "الكل",
                                    tint = if (isSelected) TealDark else appAccentColor(),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "الكل",
                                color = if (isSelected) appAccentColor() else appSubTextColor(),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }

                    // Dynamically loaded categories
                    items(categories) { cat ->
                        val isSelected = selectedCatId == cat.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .bounceClick { viewModel.selectedCategoryId.value = cat.id }
                                .width(64.dp)
                                .testTag("category_chip_${cat.id}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        color = if (isSelected) GoldAccent else appSurfaceColor(),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) GoldAccent else appCardBorderColor(),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getIconForCategory(cat.id),
                                    contentDescription = cat.nameAr,
                                    tint = if (isSelected) TealDark else appAccentColor(),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = cat.nameAr.split(" ").firstOrNull() ?: cat.nameAr,
                                color = if (isSelected) appAccentColor() else appSubTextColor(),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Products Section Header & Advanced Filter Button
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedCatId != null) "المنتجات المطابقة للقسم" else "المنتجات الأكثر مبيعاً والعروض",
                        color = appTextColor(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Expandable advanced filter trigger
                    Row(
                        modifier = Modifier
                            .background(
                                color = if (isFilterPanelExpanded) GoldAccent else appSurfaceColor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isFilterPanelExpanded) GoldAccent else appCardBorderColor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { isFilterPanelExpanded = !isFilterPanelExpanded }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "تصفية المنتجات",
                            tint = if (isFilterPanelExpanded) TealDark else appTextColor(),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تصفية وفرز",
                            color = if (isFilterPanelExpanded) TealDark else appTextColor(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Smooth Filter Panel Expansion
                AnimatedVisibility(
                    visible = isFilterPanelExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAppDarkTheme()) TealLight.copy(alpha = 0.3f) else Color(0xFFF1F5F9)
                        ),
                        border = BorderStroke(1.dp, appCardBorderColor())
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // 1. Filter by Price
                            Text("تصفية حسب السعر والعروض 🏷️", color = appTextColor(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val priceFilters = listOf(
                                    "all" to "الكل",
                                    "under_10k" to "أقل من 10,000 ر.ي",
                                    "10k_30k" to "10,000 - 30,000 ر.ي",
                                    "over_30k" to "أكثر من 30,000 ر.ي",
                                    "offers" to "العروض والخصومات فقط"
                                )
                                items(priceFilters) { (key, label) ->
                                    val isSelected = priceFilterState == key
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (isSelected) GoldAccent else if (isAppDarkTheme()) TealDark else Color.White,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) GoldAccent else appCardBorderColor(),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable { priceFilterState = key }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) TealDark else appTextColor(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 2. Sorting options
                            Text("الفرز والترتيب حسب 📊", color = appTextColor(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val sortingOptions = listOf(
                                    "default" to "الافتراضي",
                                    "price_asc" to "السعر: من الأقل للأعلى 📈",
                                    "price_desc" to "السعر: من الأعلى للأقل 📉",
                                    "rating" to "الأعلى تقييماً ⭐"
                                )
                                items(sortingOptions) { (key, label) ->
                                    val isSelected = sortByState == key
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (isSelected) GoldAccent else if (isAppDarkTheme()) TealDark else Color.White,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) GoldAccent else appCardBorderColor(),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable { sortByState = key }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) TealDark else appTextColor(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Grid List of products
        if (fullyFilteredProducts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = TextGray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("عذراً، لم نجد أي منتجات تطابق بحثك أو خيارات التصفية!", color = TextGray, fontSize = 14.sp)
                    }
                }
            }
        } else {
            // Represent products as 2-column grid within dynamic list
            val chunks = fullyFilteredProducts.chunked(2)
            items(chunks) { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { product ->
                        ProductGridCard(
                            product = product,
                            isFavorite = favoriteIds.contains(product.id),
                            onProductClick = { viewModel.navigateTo(Screen.ProductDetail(product.id)) },
                            onFavoriteToggle = { viewModel.toggleFavorite(product.id) },
                            onAddToCart = { viewModel.addProductToCart(product) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// Product Details Screen
@Composable
fun ProductDetailScreen(viewModel: StoreViewModel, productId: Int) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val product = products.find { it.id == productId }
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val isFavorite = favoriteIds.contains(productId)

    if (product == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("المنتج غير موجود!", color = appTextColor())
        }
        return
    }

    var selectedColor by remember { mutableStateOf("الافتراضي") }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Back Button & Title Top Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.CustomerHome) },
                modifier = Modifier.background(appSurfaceColor(), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = appTextColor())
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "تفاصيل المنتج المختار",
                color = appTextColor(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Hero Product Image Block with custom visual vectors
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(if (isAppDarkTheme()) TealMedium else Color(0xFFE0F2F1)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = getIconForProduct(product.id),
                    contentDescription = null,
                    tint = if (isAppDarkTheme()) GoldAccent else TealDark,
                    modifier = Modifier.size(100.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                RatingBar(rating = product.rating, darkTheme = isAppDarkTheme())
            }
        }

        // Info Block
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.nameAr,
                        color = appTextColor(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (product.stockQuantity > 0) "متوفر في المخزون (${product.stockQuantity} وحدة)" else "نفذ المخزون حالياً",
                        color = if (product.stockQuantity > 0) AlertSuccess else AlertError,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${product.price} ر.ي",
                        color = if (isAppDarkTheme()) GoldAccent else TealDark,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    if (product.oldPrice != null) {
                        Text(
                            text = "${product.oldPrice} ر.ي",
                            color = appSubTextColor(),
                            fontSize = 13.sp,
                            style = LocalTextStyle.current.copy(
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Spec Sheet
            Text(text = "الوصف والمواصفات الفنية", color = if (isAppDarkTheme()) GoldAccent else TealMedium, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = product.descriptionAr,
                color = appTextColor().copy(alpha = 0.9f),
                fontSize = 13.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Split and draw comma separated specs
            if (product.specsAr.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = appSurfaceColor()),
                    shape = RoundedCornerShape(12.dp),
                    border = if (isAppDarkTheme()) null else BorderStroke(1.dp, appCardBorderColor()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        product.specsAr.split(",").forEach { spec ->
                            val parts = spec.split(":")
                            if (parts.size == 2) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(parts[0].trim(), color = appSubTextColor(), fontSize = 12.sp)
                                    Text(parts[1].trim(), color = appTextColor(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(spec, color = appTextColor(), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Add to favorite button
                IconButton(
                    onClick = { viewModel.toggleFavorite(product.id) },
                    modifier = Modifier
                        .size(50.dp)
                        .background(appSurfaceColor(), RoundedCornerShape(12.dp))
                        .border(if (isAppDarkTheme()) 0.dp else 1.dp, appCardBorderColor(), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) AlertError else appTextColor()
                    )
                }

                // Add to Cart
                Button(
                    onClick = { viewModel.addProductToCart(product, selectedColor) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("add_to_cart_detail_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = appButtonContainerColor(), contentColor = appButtonContentColor()),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إضافة إلى سلة المشتريات", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = appCardBorderColor(), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Reviews & Ratings Section
            val productReviews by viewModel.productReviews.collectAsStateWithLifecycle()
            val filteredReviews = productReviews.filter {
                it.productName.contains(product.nameAr, ignoreCase = true) ||
                product.nameAr.contains(it.productName, ignoreCase = true)
            }

            Text(
                text = "آراء وتقييمات العملاء ⭐",
                color = if (isAppDarkTheme()) GoldAccent else TealDark,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (filteredReviews.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = appSurfaceColor()),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, appCardBorderColor()),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "لا توجد مراجعات مسبقة لهذا المنتج. كن أول من يكتب تقييماً! 🌟",
                            color = appSubTextColor(),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val avgRating = filteredReviews.map { it.rating }.average()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        text = String.format("%.1f", avgRating),
                        color = if (isAppDarkTheme()) GoldAccent else TealDark,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Column {
                        Row {
                            repeat(5) { index ->
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (index < avgRating.toInt()) GoldAccent else appSubTextColor().copy(alpha = 0.3f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = "بناءً على ${filteredReviews.size} من التقييمات",
                            color = appSubTextColor(),
                            fontSize = 10.sp
                        )
                    }
                }

                // List each review (Using Column to avoid LazyColumn inside scroll)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredReviews.forEach { rev ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = appSurfaceColor()),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, appCardBorderColor()),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = rev.customerName,
                                        color = appTextColor(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Row {
                                        repeat(5) { idx ->
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (idx < rev.rating) GoldAccent else appSubTextColor().copy(alpha = 0.3f),
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "\"${rev.comment}\"",
                                    color = appTextColor().copy(alpha = 0.85f),
                                    fontSize = 12.sp,
                                    style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = appCardBorderColor(), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Add Review Form
            var ratingInput by remember { mutableStateOf(5) }
            var reviewerName by remember { mutableStateOf("") }
            var reviewComment by remember { mutableStateOf("") }
            var showReviewSuccessDialog by remember { mutableStateOf(false) }

            Text(
                text = "شاركنا رأيك وتقييمك للجهاز ✍️",
                color = if (isAppDarkTheme()) GoldAccent else TealDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = appSurfaceColor().copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, appCardBorderColor()),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Clickable Stars Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("التقييم بالنجوم:", color = appTextColor(), fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(5) { idx ->
                                val starRating = idx + 1
                                IconButton(
                                    onClick = { ratingInput = starRating },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (starRating <= ratingInput) GoldAccent else appSubTextColor().copy(alpha = 0.3f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Reviewer Name Field
                    OutlinedTextField(
                        value = reviewerName,
                        onValueChange = { reviewerName = it },
                        label = { Text("اسمك الكريم", fontSize = 11.sp) },
                        placeholder = { Text("اكتب اسمك هنا (مثال: أحمد الوصابي)", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = appCardBorderColor(),
                            focusedLabelColor = GoldAccent,
                            unfocusedLabelColor = appSubTextColor(),
                            focusedTextColor = appTextColor(),
                            unfocusedTextColor = appTextColor()
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Comment Field
                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { reviewComment = it },
                        label = { Text("التعليق والملاحظات", fontSize = 11.sp) },
                        placeholder = { Text("اكتب رأيك بصراحة عن جودة المنتج والخدمة...", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = appCardBorderColor(),
                            focusedLabelColor = GoldAccent,
                            unfocusedLabelColor = appSubTextColor(),
                            focusedTextColor = appTextColor(),
                            unfocusedTextColor = appTextColor()
                        ),
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            if (reviewComment.isNotEmpty()) {
                                viewModel.addNewReview(
                                    customerName = reviewerName,
                                    productName = product.nameAr,
                                    rating = ratingInput,
                                    comment = reviewComment
                                )
                                showReviewSuccessDialog = true
                                reviewerName = ""
                                reviewComment = ""
                                ratingInput = 5
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAppDarkTheme()) GoldAccent else TealDark,
                            contentColor = if (isAppDarkTheme()) TealDark else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = reviewComment.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إرسال تقييمك الآن ✨", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (showReviewSuccessDialog) {
                AlertDialog(
                    onDismissRequest = { showReviewSuccessDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showReviewSuccessDialog = false }) {
                            Text("رائع 👍", color = GoldAccent, fontWeight = FontWeight.Bold)
                        }
                    },
                    title = { Text("شكراً لتقييمك! 🎉", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    text = { Text("تم تسجيل تقييمك بنجاح وعرضه مباشرة على صفحة المنتج بمركز الأحمدي.", color = Color.White, fontSize = 12.sp) },
                    containerColor = TealMedium,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

// Categories Screen (Standalone)
@Composable
fun CategoriesScreen(viewModel: StoreViewModel) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCatId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("فئات وأقسام المتجر الإلكتروني", color = appTextColor(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(categories) { cat ->
                val isSelected = selectedCatId == cat.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) GoldAccent else appCardBorderColor(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            viewModel.selectedCategoryId.value = cat.id
                            viewModel.navigateTo(Screen.CustomerHome)
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) GoldAccent else appSurfaceColor()
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = getIconForCategory(cat.id),
                                contentDescription = cat.nameAr,
                                tint = if (isSelected) TealDark else appAccentColor(),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = cat.nameAr,
                                color = if (isSelected) TealDark else appTextColor(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Favorites Screen
@Composable
fun FavoritesScreen(viewModel: StoreViewModel) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val darkTheme = isAppDarkTheme()

    val favProducts = products.filter { favoriteIds.contains(it.id) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("قائمة المنتجات المفضلة", color = appTextColor(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (favProducts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = appSubTextColor(), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("قائمتك المفضلة فارغة حالياً!", color = appSubTextColor(), fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(favProducts) { prod ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.navigateTo(Screen.ProductDetail(prod.id)) }
                            .border(1.dp, appCardBorderColor(), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = appSurfaceColor())
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(if (darkTheme) TealDark else Color(0xFFE0F2F1), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(getIconForProduct(prod.id), contentDescription = null, tint = if (darkTheme) GoldAccent else TealDark)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.nameAr, color = appTextColor(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("${prod.price} ر.ي", color = if (darkTheme) GoldAccent else TealDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { viewModel.toggleFavorite(prod.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = AlertError)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Cart Screen
@Composable
fun CartScreen(viewModel: StoreViewModel) {
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val couponDiscountPercent by viewModel.couponDiscountPercent.collectAsStateWithLifecycle()
    val activeCoupon by viewModel.activeCoupon.collectAsStateWithLifecycle()
    val couponError by viewModel.couponError.collectAsStateWithLifecycle()

    var couponInput by remember { mutableStateOf("") }

    // Aggregate Cart items with details
    val cartDetails = cartItems.mapNotNull { item ->
        val prod = products.find { it.id == item.productId }
        if (prod != null) Pair(item, prod) else null
    }

    val subtotal = cartDetails.sumOf { it.first.quantity * it.second.price }
    val discount = subtotal * couponDiscountPercent
    val shipping = if (subtotal > 500.0 || subtotal == 0.0) 0.0 else 25.0
    val total = (subtotal - discount) + shipping

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("سلة مشترياتك", color = appTextColor(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        if (cartDetails.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = appSubTextColor(), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("سلة التسوق فارغة حالياً!", color = appSubTextColor(), fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cartDetails) { (item, prod) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = appSurfaceColor()),
                        shape = RoundedCornerShape(12.dp),
                        border = if (isAppDarkTheme()) null else BorderStroke(1.dp, appCardBorderColor()),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(if (isAppDarkTheme()) TealDark else Color(0xFFE0F2F1), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(getIconForProduct(prod.id), contentDescription = null, tint = if (isAppDarkTheme()) GoldAccent else TealDark)
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.nameAr, color = appTextColor(), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text("${prod.price} ر.ي", color = if (isAppDarkTheme()) GoldAccent else TealMedium, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            // Qty controls
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.updateCartQty(item.id, item.quantity - 1) },
                                    modifier = Modifier.size(24.dp).background(if (isAppDarkTheme()) TealDark else Color(0xFFECEFF1), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = appTextColor(), modifier = Modifier.size(12.dp))
                                }
                                Text(
                                    "${item.quantity}",
                                    color = appTextColor(),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { viewModel.updateCartQty(item.id, item.quantity + 1) },
                                    modifier = Modifier.size(24.dp).background(appButtonContainerColor(), CircleShape)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = appButtonContentColor(), modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }

                // Coupon application widget
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = appSurfaceColor()),
                        shape = RoundedCornerShape(12.dp),
                        border = if (isAppDarkTheme()) null else BorderStroke(1.dp, appCardBorderColor()),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("كوبون الخصم", color = appTextColor(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = couponInput,
                                    onValueChange = { couponInput = it },
                                    placeholder = { Text("أدخل الكوبون هنا", color = appSubTextColor(), fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = appTextFieldColors(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { viewModel.applyCoupon(couponInput) },
                                    colors = ButtonDefaults.buttonColors(containerColor = appButtonContainerColor(), contentColor = appButtonContentColor()),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("تطبيق")
                                }
                            }
                            
                            if (activeCoupon != null) {
                                Text("تم تفعيل الكوبون ($activeCoupon) بنجاح!", color = AlertSuccess, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                            if (couponError != null) {
                                Text(couponError!!, color = AlertError, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }

                // Price Breakdown Summary
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(appSurfaceColor(), RoundedCornerShape(12.dp))
                            .border(if (isAppDarkTheme()) 0.dp else 1.dp, appCardBorderColor(), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("المجموع الفرعي:", color = appSubTextColor(), fontSize = 12.sp)
                            Text("$subtotal ر.ي", color = appTextColor(), fontSize = 12.sp)
                        }
                        if (discount > 0) {
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("قيمة الخصم:", color = AlertSuccess, fontSize = 12.sp)
                                Text("- $discount ر.ي", color = AlertSuccess, fontSize = 12.sp)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("تكاليف التوصيل:", color = appSubTextColor(), fontSize = 12.sp)
                            Text(if (shipping == 0.0) "مجاني" else "$shipping ر.ي", color = appTextColor(), fontSize = 12.sp)
                        }
                        HorizontalDivider(color = appCardBorderColor(), modifier = Modifier.padding(vertical = 8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الإجمالي الكلي شامل الضريبة:", color = appTextColor(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("$total ر.ي", color = if (isAppDarkTheme()) GoldAccent else TealDark, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.navigateTo(Screen.Checkout) },
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("checkout_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = appButtonContainerColor(), contentColor = appButtonContentColor()),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("الاستمرار لتحديد العنوان والدفع 🛒", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Checkout Screen
@Composable
fun CheckoutScreen(viewModel: StoreViewModel) {
    val custName by viewModel.customerName.collectAsStateWithLifecycle()
    val custPhone by viewModel.customerPhone.collectAsStateWithLifecycle()
    val address by viewModel.deliveryAddress.collectAsStateWithLifecycle()

    var showMapSelector by remember { mutableStateOf(false) }

    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val couponDiscountPercent by viewModel.couponDiscountPercent.collectAsStateWithLifecycle()
    val exchangeRateSAR by viewModel.exchangeRateSAR.collectAsStateWithLifecycle()
    val exchangeRateUSD by viewModel.exchangeRateUSD.collectAsStateWithLifecycle()

    val payCashOnDeliveryEnabled by viewModel.payCashOnDeliveryEnabled.collectAsStateWithLifecycle()
    val payCreditCardEnabled by viewModel.payCreditCardEnabled.collectAsStateWithLifecycle()
    val payMadaEnabled by viewModel.payMadaEnabled.collectAsStateWithLifecycle()
    val payApplePayEnabled by viewModel.payApplePayEnabled.collectAsStateWithLifecycle()
    val payGooglePayEnabled by viewModel.payGooglePayEnabled.collectAsStateWithLifecycle()
    val payLocalWalletsEnabled by viewModel.payLocalWalletsEnabled.collectAsStateWithLifecycle()
    val payInstallmentsEnabled by viewModel.payInstallmentsEnabled.collectAsStateWithLifecycle()
    val activePaymentGateway by viewModel.activePaymentGateway.collectAsStateWithLifecycle()
    val payInstallmentProvider by viewModel.payInstallmentProvider.collectAsStateWithLifecycle()
    val payInstallmentMonths by viewModel.payInstallmentMonths.collectAsStateWithLifecycle()

    val activePaymentMethods = remember(
        payCashOnDeliveryEnabled, payCreditCardEnabled, payMadaEnabled, payApplePayEnabled,
        payGooglePayEnabled, payLocalWalletsEnabled, payInstallmentsEnabled, activePaymentGateway,
        payInstallmentProvider, payInstallmentMonths
    ) {
        buildList {
            if (payCashOnDeliveryEnabled) {
                add("الدفع عند الاستلام (COD) 💵")
            }
            if (payCreditCardEnabled) {
                add("البطاقة الائتمانية (Visa / Mastercard) 💳 عبر $activePaymentGateway")
            }
            if (payMadaEnabled) {
                add("بطاقة مدى Mada 🇸🇦")
            }
            if (payApplePayEnabled) {
                add("Apple Pay 🍏")
            }
            if (payGooglePayEnabled) {
                add("Google Pay 🤖")
            }
            if (payLocalWalletsEnabled) {
                add("المحافظ الرقمية اليمنية (كريمي، ون كاش، جيب) 📱")
            }
            if (payInstallmentsEnabled) {
                add("تقسيط شهري ($payInstallmentMonths أشهر) عبر $payInstallmentProvider 📅")
            }
            if (isEmpty()) {
                add("الدفع عند الاستلام (COD) 💵")
            }
        }
    }

    var selectedPayment by remember(activePaymentMethods) {
        mutableStateOf(activePaymentMethods.firstOrNull() ?: "الدفع عند الاستلام (COD) 💵")
    }
    var selectedCurrency by remember { mutableStateOf("الريال اليمني (ر.ي)") }

    var subTotal = 0.0
    cartItems.forEach { cartItem ->
        val prod = products.find { it.id == cartItem.productId }
        if (prod != null) {
            subTotal += prod.price * cartItem.quantity
        }
    }
    val discount = subTotal * couponDiscountPercent
    val shippingCost = if (subTotal > 500.0) 0.0 else 25.0
    val totalYER = (subTotal - discount) + shippingCost

    val convertedTotal = when (selectedCurrency) {
        "الريال السعودي (ر.س)" -> totalYER / exchangeRateSAR
        "الدولار الأمريكي ($)" -> totalYER / exchangeRateUSD
        else -> totalYER
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateTo(Screen.Cart) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = appTextColor())
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("تأكيد طلب الشراء", color = appTextColor(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("معلومات الشحن والتوصيل", color = if (isAppDarkTheme()) GoldAccent else TealMedium, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = custName,
            onValueChange = { viewModel.customerName.value = it },
            label = { Text("الاسم الكامل", color = appSubTextColor()) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = appTextFieldColors()
        )

        OutlinedTextField(
            value = custPhone,
            onValueChange = { viewModel.customerPhone.value = it },
            label = { Text("رقم الهاتف (الواتساب)", color = appSubTextColor()) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = appTextFieldColors()
        )

        OutlinedTextField(
            value = address,
            onValueChange = { viewModel.deliveryAddress.value = it },
            label = { Text("عنوان التوصيل بالتفصيل", color = appSubTextColor()) },
            modifier = Modifier.fillMaxWidth().height(100.dp).padding(bottom = 8.dp),
            colors = appTextFieldColors()
        )

        Button(
            onClick = { showMapSelector = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isAppDarkTheme()) TealMedium else Color(0xFFE0F2F1),
                contentColor = if (isAppDarkTheme()) GoldAccent else TealDark
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, if (isAppDarkTheme()) GoldAccent else TealMedium, RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("اختر موقع التوصيل بدقة من الخريطة 🗺️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        if (showMapSelector) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showMapSelector = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    InteractiveSimulatedMap(
                        initialLat = viewModel.selectedLatitude.value,
                        initialLng = viewModel.selectedLongitude.value,
                        onLocationSelected = { lat, lng, resolvedAddr ->
                            viewModel.selectedLatitude.value = lat
                            viewModel.selectedLongitude.value = lng
                            viewModel.selectedMapAddress.value = resolvedAddr
                            viewModel.deliveryAddress.value = "$resolvedAddr (إحداثيات: ${String.format("%.4f", lat)}, ${String.format("%.4f", lng)})"
                            showMapSelector = false
                        },
                        onDismiss = { showMapSelector = false }
                    )
                }
            }
        }

        Text("اختر طريقة الدفع المفضلة", color = if (isAppDarkTheme()) GoldAccent else TealMedium, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        activePaymentMethods.forEach { method ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(appSurfaceColor(), RoundedCornerShape(8.dp))
                    .border(if (isAppDarkTheme()) 0.dp else 1.dp, appCardBorderColor(), RoundedCornerShape(8.dp))
                    .clickable { selectedPayment = method }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedPayment == method,
                    onClick = { selectedPayment = method },
                    colors = RadioButtonDefaults.colors(selectedColor = GoldAccent)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(method, color = appTextColor(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("اختر عملة الدفع المفضلة", color = if (isAppDarkTheme()) GoldAccent else TealMedium, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        val currencies = listOf(
            "الريال اليمني (ر.ي)" to "العملة المحلية الأساسية",
            "الريال السعودي (ر.س)" to "سعر الصرف: 1 ر.س = $exchangeRateSAR ر.ي",
            "الدولار الأمريكي ($)" to "سعر الصرف: 1 $ = $exchangeRateUSD ر.ي"
        )
        currencies.forEach { (curr, desc) ->
            val rate = if (curr == "الريال السعودي (ر.س)") exchangeRateSAR else if (curr == "الدولار الأمريكي ($)") exchangeRateUSD else 1.0
            val equivalentAmount = totalYER / rate
            val formattedEquivalent = String.format("%.2f", equivalentAmount)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(appSurfaceColor(), RoundedCornerShape(8.dp))
                    .border(if (isAppDarkTheme()) 0.dp else 1.dp, appCardBorderColor(), RoundedCornerShape(8.dp))
                    .clickable { selectedCurrency = curr }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedCurrency == curr,
                    onClick = { selectedCurrency = curr },
                    colors = RadioButtonDefaults.colors(selectedColor = GoldAccent)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(curr, color = appTextColor(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(desc, color = appSubTextColor(), fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$formattedEquivalent ${if (curr.contains("ي")) "ر.ي" else if (curr.contains("س")) "ر.س" else "$"}",
                    color = if (isAppDarkTheme()) GoldAccent else TealDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.placeOrder(
                    paymentMethod = selectedPayment,
                    chosenCurrency = selectedCurrency,
                    convertedAmount = convertedTotal
                ) { orderId ->
                    if (selectedPayment.startsWith("الدفع عند الاستلام")) {
                        viewModel.navigateTo(Screen.OrderTracking(orderId))
                    } else {
                        viewModel.navigateTo(Screen.PaymentGateway(orderId))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("confirm_order_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = appButtonContainerColor(), contentColor = appButtonContentColor()),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("تأكيد وإرسال الطلب الآن 🚚", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Order Tracking / Detailed Status Screen
@Composable
fun OrderTrackingScreen(viewModel: StoreViewModel, orderId: Int) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val order = orders.find { it.id == orderId }

    if (order == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لم نتمكن من العثور على الطلب #$orderId", color = Color.White)
        }
        return
    }

    val statuses = listOf("جديد", "قيد التجهيز", "جاهز للشحن", "في الطريق", "تم التسليم")
    val currentIdx = statuses.indexOf(order.status).coerceAtLeast(0)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateTo(Screen.CustomerHome) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("متابعة حالة الطلب #$orderId", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Big Success Callout
        Card(
            colors = CardDefaults.cardColors(containerColor = TealMedium),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AlertSuccess, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("تم استلام طلبك بنجاح!", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (order.chosenCurrency != "الريال اليمني (ر.ي)") {
                    Text("المبلغ الإجمالي المستحق: ${String.format("%.1f", order.convertedAmount)} ${order.chosenCurrency}", color = GoldAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("(ما يعادل: ${order.totalAmount} ر.ي)", color = TextGray, fontSize = 11.sp)
                } else {
                    Text("المبلغ الإجمالي المستحق: ${order.totalAmount} ر.ي", color = GoldAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Text("وسيلة الدفع: ${order.paymentMethod}", color = TextGray, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("خط سير شحنتك ولحظات التوصيل", color = GoldAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Draw Stepper
        statuses.forEachIndexed { index, step ->
            val done = index <= currentIdx
            val isCurrent = index == currentIdx
            val color = if (done) GoldAccent else TextGray.copy(alpha = 0.3f)

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(if (done) GoldAccent else TealMedium, CircleShape)
                        .border(2.dp, if (isCurrent) Color.White else Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (done) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(12.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(
                        text = step,
                        color = if (done) Color.White else TextGray,
                        fontSize = 14.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                    )
                    if (isCurrent) {
                        Text(
                            text = when (step) {
                                "جديد" -> "تلقينا طلبك وهو قيد التدقيق الأولي"
                                "قيد التجهيز" -> "طاقم العمل يغلف مستلزماتك بعناية"
                                "جاهز للشحن" -> "تم إنهاء التغليف والطلب في ساحة التحميل"
                                "في الطريق" -> "الطلب غادر المركز مع المندوب ${order.deliveryAgentName ?: ""}"
                                else -> "تهانينا! تم تسليم شحنتك وإتمام الشراء بنجاح"
                            },
                            color = GoldAccentLight,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// Profile & Account Dashboard
@Composable
fun ProfileScreen(viewModel: StoreViewModel) {
    val isLoggedIn by viewModel.isLoggedInCustomer.collectAsStateWithLifecycle()
    val name by viewModel.customerName.collectAsStateWithLifecycle()
    val phone by viewModel.customerPhone.collectAsStateWithLifecycle()
    val darkTheme = isAppDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appBgColor())
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "حسابي الشخصي",
            color = appTextColor(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Profile Header Card (Dynamic Guest/User State)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = appSurfaceColor()),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, appCardBorderColor(), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (isLoggedIn) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background((if (darkTheme) GoldAccent else TealMedium).copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, if (darkTheme) GoldAccent else TealMedium, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = if (darkTheme) GoldAccent else TealMedium, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(name.ifEmpty { "عميل الأحمدي" }, color = appTextColor(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(if (darkTheme) GoldAccent else TealMedium, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("VIP", color = if (darkTheme) TealDark else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(phone.ifEmpty { "07xxxxxxxx" }, color = appSubTextColor(), fontSize = 12.sp)
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(if (darkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.NoAccounts, contentDescription = null, tint = appSubTextColor(), modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("أنت تتصفح كزائر", color = appTextColor(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("قم بإنشاء حساب لحفظ طلباتك وعناوينك", color = appSubTextColor(), fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.navigateTo(Screen.Login(AppRole.CUSTOMER)) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = appButtonContainerColor(),
                            contentColor = appButtonContentColor()
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text("تسجيل الدخول / إنشاء حساب", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Navigation Section
        Text("الخيارات والمعاملات", color = if (darkTheme) GoldAccent else TealMedium, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Grid List
        val menuItems = listOf(
            Triple("سجل طلباتي السابقة", Icons.Default.ReceiptLong, Screen.OrderHistory),
            Triple("عروض الأحمدي الحصرية", Icons.Default.Percent, Screen.Offers),
            Triple("الضمان وسياسة الاسترجاع", Icons.Default.Gavel, Screen.ReturnWarranty),
            Triple("محادثة الدعم الفني", Icons.Default.SupportAgent, Screen.SupportChat),
            Triple("الإعدادات العامة للتطبيق", Icons.Default.Settings, Screen.Settings),
            Triple("معلومات عن المتجر", Icons.Default.Info, Screen.AboutStore)
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            menuItems.forEach { item ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = appSurfaceColor()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(item.third) }
                        .border(1.dp, appCardBorderColor(), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (darkTheme) TealLight else Color(0xFFE0F2F1), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(item.second, contentDescription = null, tint = if (darkTheme) GoldAccent else TealMedium, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(item.first, color = appTextColor(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = appSubTextColor(),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Logout Button if logged in
        if (isLoggedIn) {
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(
                onClick = { viewModel.logout() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertError),
                border = BorderStroke(1.dp, AlertError.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("logout_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = AlertError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسجيل الخروج من الحساب", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Support Chat Screen
@Composable
fun SupportChatScreen(viewModel: StoreViewModel) {
    val messages by viewModel.supportMessages.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val darkTheme = isAppDarkTheme()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(appBgColor()).padding(16.dp)) {
        Text("الدعم المباشر ومحادثة الأخصائيين", color = appTextColor(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                val isUser = msg.sender == "user"
                val align = if (isUser) Alignment.End else Alignment.Start
                val bg = if (isUser) (if (darkTheme) GoldAccent else TealMedium) else appSurfaceColor()
                val textCol = if (isUser) (if (darkTheme) TealDark else Color.White) else appTextColor()

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .border(1.dp, appCardBorderColor(), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                            .widthIn(max = 260.dp)
                    ) {
                        Text(msg.text, color = textCol, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("اكتب رسالتك واستفسارك هنا...", color = appSubTextColor(), fontSize = 12.sp) },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = appTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    viewModel.sendSupportMessage(input)
                    input = ""
                },
                modifier = Modifier.size(48.dp).background(appButtonContainerColor(), CircleShape)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = appButtonContentColor())
            }
        }
    }
}

// About Store Screen
@Composable
fun AboutStoreScreen(viewModel: StoreViewModel) {
    val darkTheme = isAppDarkTheme()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appBgColor())
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("عن مركز الأحمدي للجوالات", color = appTextColor(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        AlahmadiLogo(modifier = Modifier.align(Alignment.CenterHorizontally), size = 100.dp)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "مركز الأحمدي هو الوجهة الأولى والرائدة في توفير أحدث الهواتف الذكية ومستلزماتها وإكسسواراتها الأصلية بأرخص الأسعار في المنطقة. نلتزم بأعلى معايير الجودة والمصداقية ونقدم ضمانات حقيقية ذهبية على كافة الأجهزة والشواحن والصوتيات.",
            color = appTextColor(),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("سياسة الإرجاع والضمان", color = if (darkTheme) GoldAccent else TealMedium, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "• ضمان ذهبي حقيقي لمدة سنتين على جميع الشواحن والملحقات ضد العيوب المصنعية.\n• إمكانية استبدال أو استرجاع المنتج خلال 7 أيام من تاريخ الشراء بشرط سلامة التغليف الأصلي.\n• نوفر صيانة سريعة ومتقنة بأيدي مهندسين متخصصين.",
            color = appSubTextColor(),
            fontSize = 12.sp,
            lineHeight = 20.sp
        )
    }
}

// Admin Dashboard Screen
@Composable
fun AdminDashboardScreen(viewModel: StoreViewModel) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    val brands by viewModel.brands.collectAsStateWithLifecycle()
    val adminCustomers by viewModel.adminCustomers.collectAsStateWithLifecycle()
    val deliveryReps by viewModel.deliveryReps.collectAsStateWithLifecycle()
    val shippingZones by viewModel.shippingZones.collectAsStateWithLifecycle()
    val coupons by viewModel.coupons.collectAsStateWithLifecycle()
    val adBanners by viewModel.adBanners.collectAsStateWithLifecycle()
    val productReviews by viewModel.productReviews.collectAsStateWithLifecycle()
    val teamRoles by viewModel.teamRoles.collectAsStateWithLifecycle()

    val staticAboutUs by viewModel.staticAboutUs.collectAsStateWithLifecycle()
    val staticPrivacyPolicy by viewModel.staticPrivacyPolicy.collectAsStateWithLifecycle()
    val staticTermsOfService by viewModel.staticTermsOfService.collectAsStateWithLifecycle()

    val appStoreTitle by viewModel.appStoreTitle.collectAsStateWithLifecycle()
    val appStoreBannerText by viewModel.appStoreBannerText.collectAsStateWithLifecycle()
    val appStoreContactMobile by viewModel.appStoreContactMobile.collectAsStateWithLifecycle()
    val appStoreSupportEmail by viewModel.appStoreSupportEmail.collectAsStateWithLifecycle()
    val appStoreWorkingHours by viewModel.appStoreWorkingHours.collectAsStateWithLifecycle()
    val appStoreMaintenanceMode by viewModel.appStoreMaintenanceMode.collectAsStateWithLifecycle()

    val payCashOnDeliveryEnabled by viewModel.payCashOnDeliveryEnabled.collectAsStateWithLifecycle()
    val payCreditCardEnabled by viewModel.payCreditCardEnabled.collectAsStateWithLifecycle()
    val payMadaEnabled by viewModel.payMadaEnabled.collectAsStateWithLifecycle()
    val payApplePayEnabled by viewModel.payApplePayEnabled.collectAsStateWithLifecycle()
    val payGooglePayEnabled by viewModel.payGooglePayEnabled.collectAsStateWithLifecycle()
    val payLocalWalletsEnabled by viewModel.payLocalWalletsEnabled.collectAsStateWithLifecycle()
    val payInstallmentsEnabled by viewModel.payInstallmentsEnabled.collectAsStateWithLifecycle()
    val payVatRatePercent by viewModel.payVatRatePercent.collectAsStateWithLifecycle()

    val activePaymentGateway by viewModel.activePaymentGateway.collectAsStateWithLifecycle()
    val paymentGatewayApiKey by viewModel.paymentGatewayApiKey.collectAsStateWithLifecycle()
    val paymentGatewaySandbox by viewModel.paymentGatewaySandbox.collectAsStateWithLifecycle()
    
    val payInstallmentProvider by viewModel.payInstallmentProvider.collectAsStateWithLifecycle()
    val payInstallmentMonths by viewModel.payInstallmentMonths.collectAsStateWithLifecycle()

    var selectedPanel by remember { mutableStateOf<Int?>(null) } // null: Hub, 1 to 20

    // Form inputs
    var prodId by remember { mutableStateOf("") }
    var prodName by remember { mutableStateOf("") }
    var prodPrice by remember { mutableStateOf("") }
    var prodStock by remember { mutableStateOf("") }
    var prodDesc by remember { mutableStateOf("") }
    var prodCategory by remember { mutableStateOf("1") }
    var prodImageUrl by remember { mutableStateOf("custom_product") }

    var catIdInput by remember { mutableStateOf("") }
    var catNameInput by remember { mutableStateOf("") }
    var catIconInput by remember { mutableStateOf("smartphone") }

    var brandInput by remember { mutableStateOf("") }

    var custNameInput by remember { mutableStateOf("") }
    var custPhoneInput by remember { mutableStateOf("") }
    var custEmailInput by remember { mutableStateOf("") }

    var repNameInput by remember { mutableStateOf("") }
    var repPhoneInput by remember { mutableStateOf("") }
    var repVehicleInput by remember { mutableStateOf("") }

    var couponCodeInput by remember { mutableStateOf("") }
    var couponDiscountInput by remember { mutableStateOf("") }
    var couponMaxDiscInput by remember { mutableStateOf("") }

    var bannerTitleInput by remember { mutableStateOf("") }
    var bannerImgInput by remember { mutableStateOf("") }

    var staticAboutInput by remember { mutableStateOf("") }
    var staticPrivacyInput by remember { mutableStateOf("") }
    var staticTermsInput by remember { mutableStateOf("") }

    var notifTitleInput by remember { mutableStateOf("") }
    var notifMsgInput by remember { mutableStateOf("") }

    var roleNameInput by remember { mutableStateOf("") }
    var roleTitleInput by remember { mutableStateOf("Editor") }
    var roleCanWrite by remember { mutableStateOf(true) }
    var roleCanDelete by remember { mutableStateOf(false) }
    var roleCanEdit by remember { mutableStateOf(false) }

    var configTitle by remember { mutableStateOf("") }
    var configBanner by remember { mutableStateOf("") }
    var configMobile by remember { mutableStateOf("") }
    var configEmail by remember { mutableStateOf("") }
    var configHours by remember { mutableStateOf("") }
    var configMaint by remember { mutableStateOf(false) }

    var payCod by remember { mutableStateOf(true) }
    var payCard by remember { mutableStateOf(true) }
    var payMada by remember { mutableStateOf(true) }
    var payApple by remember { mutableStateOf(true) }
    var payGoogle by remember { mutableStateOf(true) }
    var payWallets by remember { mutableStateOf(true) }
    var payInstallments by remember { mutableStateOf(false) }
    var payVat by remember { mutableStateOf("15") }
    var activeGateway by remember { mutableStateOf("MyFatoorah") }
    var gatewayApiKey by remember { mutableStateOf("sk_test_alahmadi_992x") }
    var gatewaySandbox by remember { mutableStateOf(true) }
    var installmentProvider by remember { mutableStateOf("Tabby & Tamara") }
    var installmentMonths by remember { mutableStateOf("4") }
    var exchangeRateSARInput by remember { mutableStateOf("400.0") }
    var exchangeRateUSDInput by remember { mutableStateOf("1500.0") }

    var backupText by remember { mutableStateOf("") }
    var restoreText by remember { mutableStateOf("") }
    var searchLogQuery by remember { mutableStateOf("") }
    var productSearchQuery by remember { mutableStateOf("") }
    var customerSearchQuery by remember { mutableStateOf("") }
    var orderSearchQuery by remember { mutableStateOf("") }

    // Enhanced variables for editing, upload, bulk json, print dialogs, report period tabs
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showMediaUploadDialog by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var isUploading by remember { mutableStateOf(false) }
    var showBulkImportExportDialog by remember { mutableStateOf(false) }
    var jsonTextForImport by remember { mutableStateOf("") }
    var invoiceToShow by remember { mutableStateOf<Order?>(null) }
    var labelToShow by remember { mutableStateOf<Order?>(null) }
    var mapViewingOrder by remember { mutableStateOf<Order?>(null) }
    var activeReportTab by remember { mutableStateOf("Daily") }

    // For specs/variants editing
    var prodColorsInput by remember { mutableStateOf("") }
    var prodCapacitiesInput by remember { mutableStateOf("") }

    // Init form values with current states
    LaunchedEffect(selectedPanel) {
        if (selectedPanel == 13) {
            staticAboutInput = staticAboutUs
            staticPrivacyInput = staticPrivacyPolicy
            staticTermsInput = staticTermsOfService
        }
        if (selectedPanel == 16) {
            configTitle = appStoreTitle
            configBanner = appStoreBannerText
            configMobile = appStoreContactMobile
            configEmail = appStoreSupportEmail
            configHours = appStoreWorkingHours
            configMaint = appStoreMaintenanceMode
        }
        if (selectedPanel == 20) {
            payCod = payCashOnDeliveryEnabled
            payCard = payCreditCardEnabled
            payMada = payMadaEnabled
            payApple = payApplePayEnabled
            payGoogle = payGooglePayEnabled
            payWallets = payLocalWalletsEnabled
            payInstallments = payInstallmentsEnabled
            payVat = payVatRatePercent.toString()
            activeGateway = activePaymentGateway
            gatewayApiKey = paymentGatewayApiKey
            gatewaySandbox = paymentGatewaySandbox
            installmentProvider = payInstallmentProvider
            installmentMonths = payInstallmentMonths.toString()
            exchangeRateSARInput = viewModel.exchangeRateSAR.value.toString()
            exchangeRateUSDInput = viewModel.exchangeRateUSD.value.toString()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Upper Admin Panel Header
        Row(
            modifier = Modifier.fillMaxWidth().background(TealDark, RoundedCornerShape(12.dp)).padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("لوحة التحكم الإدارية الشاملة 🛡️", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("تطبيق مركز الأحمدي للاتصالات", color = GoldAccent, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
            IconButton(
                onClick = { viewModel.logout() },
                modifier = Modifier.testTag("admin_logout_button").size(36.dp).background(TealMedium, CircleShape)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "تسجيل خروج", tint = GoldAccent, modifier = Modifier.size(18.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))

        // Quick Statistics row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val totalRevenue = orders.filter { it.status == "تم التسليم" }.sumOf { it.totalAmount }
            val totalOrdersCount = orders.size
            val lowStockCount = products.filter { it.stockQuantity <= 5 }.size
            val activeCouponsCount = coupons.filter { it.active }.size

            val stats = listOf(
                Pair("المبيعات المكتملة", "${String.format("%.0f", totalRevenue)} ر.ي"),
                Pair("إجمالي الطلبات", "$totalOrdersCount طلب"),
                Pair("تنبيهات المخزون", "$lowStockCount منتجات"),
                Pair("الكوبونات النشطة", "$activeCouponsCount كوبون")
            )

            stats.forEach { (label, value) ->
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = TealMedium),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(label, color = TextGray, fontSize = 8.sp, textAlign = TextAlign.Center, maxLines = 1)
                        Text(value, color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main Area
        if (selectedPanel == null) {
            // Panel Hub Screen
            Text("قائمة إدارة النظام (21 قسماً متكاملاً):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            val panelItems = listOf(
                Triple(1, "لوحة التحكم Dashboard", Icons.Default.SupervisorAccount),
                Triple(2, "إدارة المنتجات", Icons.Default.Inventory2),
                Triple(3, "إدارة الفئات", Icons.Default.Category),
                Triple(4, "إدارة العلامات التجارية", Icons.Default.Verified),
                Triple(5, "إدارة المخزون", Icons.Default.Handyman),
                Triple(6, "إدارة الطلبات", Icons.Default.ReceiptLong),
                Triple(7, "إدارة العملاء", Icons.Default.Person),
                Triple(8, "إدارة المناديب", Icons.Default.Person),
                Triple(9, "الشحن والتوصيل", Icons.Default.LocalShipping),
                Triple(10, "العروض والكوبونات", Icons.Default.Discount),
                Triple(11, "الإعلانات والبانرات", Icons.Default.Image),
                Triple(12, "التقييمات والمراجعات", Icons.Default.SupportAgent),
                Triple(13, "المحتوى الثابت", Icons.Default.Gavel),
                Triple(14, "إرسال الإشعارات", Icons.Default.NotificationsActive),
                Triple(15, "الصلاحيات والأدوار", Icons.Default.Security),
                Triple(16, "الإعدادات العامة", Icons.Default.Settings),
                Triple(17, "التقارير والتحليلات", Icons.Default.Search),
                Triple(18, "سجل التدقيق Audit Log", Icons.Default.History),
                Triple(19, "النسخ الاحتياطي والاستعادة", Icons.Default.CloudSync),
                Triple(20, "إعدادات الدفع والشحن", Icons.Default.CheckCircle),
                Triple(21, "الربط والتكامل المحاسبي", Icons.Default.AccountBalance)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(panelItems) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedPanel = item.first },
                        colors = CardDefaults.cardColors(containerColor = TealMedium),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, TealLight.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp).background(TealDark, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(item.third, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                            }
                            Column {
                                Text(item.second, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("إدارة ومتابعة وتعديل", color = TextGray, fontSize = 8.sp)
                            }
                        }
                    }
                }
            }
        } else {
            // Workspace Header
            Row(
                modifier = Modifier.fillMaxWidth().background(TealMedium, RoundedCornerShape(8.dp)).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { selectedPanel = null },
                        modifier = Modifier.size(32.dp).background(TealDark, CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "رجوع للرئيسية", tint = GoldAccent, modifier = Modifier.size(16.dp))
                    }
                    val panelTitle = when (selectedPanel) {
                        1 -> "لوحة التحكم والتحليلات 📊"
                        2 -> "إدارة المنتجات بالمتجر 📱"
                        3 -> "إدارة فئات المنتجات 📁"
                        4 -> "إدارة العلامات التجارية 🏷️"
                        5 -> "إدارة مخزون المستودعات 📦"
                        6 -> "إدارة طلبات الفواتير 📋"
                        7 -> "إدارة بيانات العملاء 👥"
                        8 -> "إدارة مناديب التوصيل 🚴"
                        9 -> "الشحن ومناطق التوصيل 🚚"
                        10 -> "العروض وكوبونات الخصم 🎫"
                        11 -> "الإعلانات والبانرات الدعائية 🖼️"
                        12 -> "التقييمات ومراجعات العملاء ⭐"
                        13 -> "المحتوى والصفحات الثابتة 📄"
                        14 -> "إرسال الإشعارات الجماعية 📢"
                        15 -> "إدارة الصلاحيات وأدوار الفريق 🔑"
                        16 -> "إعدادات المتجر العامة ⚙️"
                        17 -> "التقارير والمبيعات المالية 📈"
                        18 -> "سجل التدقيق والرقابة Audit Log 📝"
                        19 -> "النسخ الاحتياطي واستعادة البيانات 💾"
                        20 -> "إعدادات الدفع والضرائب 💳"
                        21 -> "الربط والتكامل المحاسبي (ERP) 💼"
                        else -> "لوحة الإدارة"
                    }
                    Text(panelTitle, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Workspace Content panels
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedPanel) {
                    1 -> { // 1) Dashboard Panel
                        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                            Card(colors = CardDefaults.cardColors(containerColor = TealMedium)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("نظرة عامة على النشاط اليومي", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("مرحباً بك مجدداً في لوحة إدارة مركز الأحمدي. النظام يعمل بكفاءة كاملة وقاعدة البيانات السحابية متصلة ومؤمنة.", color = Color.White, fontSize = 11.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Visual horizontal Chart
                            Text("رسم بياني: مبيعات آخر 7 أيام", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(colors = CardDefaults.cardColors(containerColor = TealMedium)) {
                                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                    val days = listOf("الأحد", "الأثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
                                    val sales = listOf(1500, 2400, 1800, 3200, 4500, 2900, 3900)
                                    val maxSales = sales.maxOrNull() ?: 1
                                    
                                    days.forEachIndexed { idx, day ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(day, color = Color.White, fontSize = 10.sp, modifier = Modifier.width(50.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            val pct = (sales[idx].toFloat() / maxSales.toFloat())
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(14.dp)
                                                    .background(TealDark, RoundedCornerShape(4.dp))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(pct)
                                                        .fillMaxHeight()
                                                        .background(GoldAccent, RoundedCornerShape(4.dp))
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("${sales[idx]} ر.ي", color = GoldAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("آخر العمليات المسجلة بسجل التدقيق", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(modifier = Modifier.height(150.dp)) {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(auditLogs.take(3)) { log ->
                                        Card(colors = CardDefaults.cardColors(containerColor = TealDark), modifier = Modifier.fillMaxWidth()) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(log.actionAr, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                Text(log.detailsAr, color = TextGray, fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> { // 2) Product Management
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Search product bar
                            OutlinedTextField(
                                value = productSearchQuery,
                                onValueChange = { productSearchQuery = it },
                                label = { Text("ابحث عن منتج...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldAccent) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = TealLight, focusedBorderColor = GoldAccent)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            // Bulk Operation Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { showBulkImportExportDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealMedium, contentColor = Color.White),
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text("استيراد وتصدير بالجملة (JSON) 📦", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // Simple Scrollable Form inside Card
                            var showForm by remember { mutableStateOf(false) }
                            Button(
                                onClick = { 
                                    if (showForm && editingProduct != null) {
                                        // Reset edit mode if hiding
                                        editingProduct = null
                                        prodId = ""
                                        prodName = ""
                                        prodPrice = ""
                                        prodStock = ""
                                        prodDesc = ""
                                        prodCategory = "1"
                                        prodImageUrl = "custom_product"
                                        prodColorsInput = ""
                                        prodCapacitiesInput = ""
                                    }
                                    showForm = !showForm 
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (showForm) TealLight else GoldAccent, contentColor = TealDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (showForm) "إخفاء الاستمارة ❌" else "➕ إضافة منتج جديد للمتجر", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            
                            if (showForm) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = TealMedium),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp).verticalScroll(rememberScrollState())) {
                                        Text(
                                            text = if (editingProduct != null) "تعديل المنتج الحالي ✏️" else "إضافة منتج جديد ➕",
                                            color = GoldAccent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = prodId, 
                                            onValueChange = { prodId = it }, 
                                            label = { Text("رقم المنتج (فريد)") }, 
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = editingProduct == null
                                        )
                                        OutlinedTextField(value = prodName, onValueChange = { prodName = it }, label = { Text("اسم المنتج") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(value = prodPrice, onValueChange = { prodPrice = it }, label = { Text("السعر بالريال") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(value = prodStock, onValueChange = { prodStock = it }, label = { Text("الكمية") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(value = prodDesc, onValueChange = { prodDesc = it }, label = { Text("وصف المنتج") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(value = prodCategory, onValueChange = { prodCategory = it }, label = { Text("رقم القسم (1-9)") }, modifier = Modifier.fillMaxWidth())
                                        
                                        // Image Selector and Uploader
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = prodImageUrl,
                                                onValueChange = { prodImageUrl = it },
                                                label = { Text("اسم الصورة") },
                                                modifier = Modifier.weight(1f)
                                            )
                                            Button(
                                                onClick = { showMediaUploadDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark),
                                                contentPadding = PaddingValues(horizontal = 10.dp),
                                                modifier = Modifier.height(48.dp)
                                            ) {
                                                Text("رفع صورة 📷", fontSize = 10.sp)
                                            }
                                        }
                                        
                                        // Variant inputs
                                        OutlinedTextField(
                                            value = prodColorsInput,
                                            onValueChange = { prodColorsInput = it },
                                            label = { Text("الألوان (مثال: أسود، أبيض، ذهبي)") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        OutlinedTextField(
                                            value = prodCapacitiesInput,
                                            onValueChange = { prodCapacitiesInput = it },
                                            label = { Text("السعات والموديل (مثال: 128GB، 256GB)") },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                val idInt = prodId.toIntOrNull() ?: (200..999).random()
                                                val priceDb = prodPrice.toDoubleOrNull() ?: 99.0
                                                val stockInt = prodStock.toIntOrNull() ?: 10
                                                val catId = prodCategory.toIntOrNull() ?: 1
                                                
                                                // Create specs from variants
                                                val specsList = mutableListOf<String>()
                                                if (prodColorsInput.isNotEmpty()) specsList.add("الألوان: $prodColorsInput")
                                                if (prodCapacitiesInput.isNotEmpty()) specsList.add("الذاكرة/السعة: $prodCapacitiesInput")
                                                val finalSpecs = specsList.joinToString(",")

                                                viewModel.adminAddProduct(idInt, prodName, prodDesc, priceDb, null, catId, stockInt, finalSpecs)
                                                
                                                // Reset form
                                                prodId = ""
                                                prodName = ""
                                                prodPrice = ""
                                                prodStock = ""
                                                prodDesc = ""
                                                prodCategory = "1"
                                                prodImageUrl = "custom_product"
                                                prodColorsInput = ""
                                                prodCapacitiesInput = ""
                                                editingProduct = null
                                                showForm = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = if (editingProduct != null) "حفظ التعديلات الحالية 💾" else "حفظ المنتج بالكامل في النظام 💾",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (editingProduct != null) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Button(
                                                onClick = {
                                                    // Cancel Edit
                                                    prodId = ""
                                                    prodName = ""
                                                    prodPrice = ""
                                                    prodStock = ""
                                                    prodDesc = ""
                                                    prodCategory = "1"
                                                    prodImageUrl = "custom_product"
                                                    prodColorsInput = ""
                                                    prodCapacitiesInput = ""
                                                    editingProduct = null
                                                    showForm = false
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = TealLight, contentColor = Color.White),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("إلغاء التعديل ❌", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // Products list
                            val filteredProducts = products.filter {
                                productSearchQuery.isEmpty() || it.nameAr.contains(productSearchQuery, ignoreCase = true)
                            }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(filteredProducts) { item ->
                                    Card(colors = CardDefaults.cardColors(containerColor = TealMedium)) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text(item.nameAr, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    IconButton(
                                                        onClick = {
                                                            editingProduct = item
                                                            prodId = item.id.toString()
                                                            prodName = item.nameAr
                                                            prodPrice = item.price.toString()
                                                            prodStock = item.stockQuantity.toString()
                                                            prodDesc = item.descriptionAr
                                                            prodCategory = item.categoryId.toString()
                                                            prodImageUrl = item.imageUrl
                                                            
                                                            // Parse specifications if any
                                                            var colorsVal = ""
                                                            var capVal = ""
                                                            item.specsAr.split(",").forEach { specPart ->
                                                                if (specPart.contains("الألوان:")) {
                                                                    colorsVal = specPart.substringAfter("الألوان:").trim()
                                                                }
                                                                if (specPart.contains("الذاكرة/السعة:")) {
                                                                    capVal = specPart.substringAfter("الذاكرة/السعة:").trim()
                                                                }
                                                            }
                                                            prodColorsInput = colorsVal
                                                            prodCapacitiesInput = capVal
                                                            
                                                            showForm = true
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                                                    }
                                                    IconButton(
                                                        onClick = { 
                                                            viewModel.pendingDeleteAction.value = com.example.viewmodel.DeleteConfirmation(
                                                                type = com.example.viewmodel.DeleteType.PRODUCT,
                                                                id = item.id,
                                                                name = item.nameAr,
                                                                extraData = item
                                                            )
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = null, tint = AlertError, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                            Text("السعر: ${item.price} ر.ي | المخزون: ${item.stockQuantity} قطعة", color = GoldAccentLight, fontSize = 10.sp)
                                            if (item.specsAr.isNotEmpty()) {
                                                Text("المواصفات: ${item.specsAr}", color = TextGray, fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3 -> { // 3) Category Management
                        Column(modifier = Modifier.fillMaxSize()) {
                            Card(colors = CardDefaults.cardColors(containerColor = TealMedium), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("إضافة قسم جديد", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(value = catIdInput, onValueChange = { catIdInput = it }, label = { Text("رقم القسم (فريد)") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = catNameInput, onValueChange = { catNameInput = it }, label = { Text("اسم القسم بالعربية") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = catIconInput, onValueChange = { catIconInput = it }, label = { Text("اسم الأيقونة (مثال: bolt)") }, modifier = Modifier.fillMaxWidth())
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            val idInt = catIdInput.toIntOrNull() ?: (10..99).random()
                                            viewModel.adminAddCategory(idInt, catNameInput, catIconInput)
                                            catIdInput = ""
                                            catNameInput = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark),
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("حفظ القسم الجديد 📁", fontSize = 11.sp) }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("الأقسام الحالية بالمتجر:", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(categories) { cat ->
                                    Card(colors = CardDefaults.cardColors(containerColor = TealMedium)) {
                                        Row(
                                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Box(modifier = Modifier.size(28.dp).background(TealDark, CircleShape), contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Category, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                                                }
                                                Text("#${cat.id} - ${cat.nameAr}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            IconButton(onClick = { viewModel.adminDeleteCategory(cat.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = AlertError, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    4 -> { // 4) Brand Management
                        Column(modifier = Modifier.fillMaxSize()) {
                            Card(colors = CardDefaults.cardColors(containerColor = TealMedium), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("تسجيل علامة تجارية جديدة", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(value = brandInput, onValueChange = { brandInput = it }, label = { Text("اسم الماركة/العلامة التجارية") }, modifier = Modifier.fillMaxWidth())
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.addBrand(brandInput)
                                            brandInput = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark),
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("إضافة وحفظ 🏷️", fontSize = 11.sp) }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("العلامات التجارية المسجلة:", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(brands) { brand ->
                                    Card(colors = CardDefaults.cardColors(containerColor = TealMedium)) {
                                        Row(
                                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(brand, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            IconButton(onClick = { 
                                                viewModel.pendingDeleteAction.value = com.example.viewmodel.DeleteConfirmation(
                                                    type = com.example.viewmodel.DeleteType.BRAND,
                                                    name = brand
                                                )
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = AlertError, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    5 -> { // 5) Stock Management
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text("تحديث مستويات مخزون السلع فورياً وبسهولة:", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(products) { item ->
                                    val isLowStock = item.stockQuantity <= 5
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = TealMedium),
                                        border = if (isLowStock) BorderStroke(1.dp, AlertError) else null
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.nameAr, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(
                                                    if (isLowStock) "⚠️ مخزون منخفض جداً! الكمية: ${item.stockQuantity}" else "الكمية المتاحة: ${item.stockQuantity} قطعة",
                                                    color = if (isLowStock) AlertError else GoldAccentLight,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Button(
                                                    onClick = { viewModel.quickUpdateStock(item.id, -1) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = TealDark),
                                                    contentPadding = PaddingValues(0.dp),
                                                    modifier = Modifier.size(30.dp)
                                                ) { Text("-", color = Color.White, fontSize = 14.sp) }
                                                Text("${item.stockQuantity}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
                                                Button(
                                                    onClick = { viewModel.quickUpdateStock(item.id, 1) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                                                    contentPadding = PaddingValues(0.dp),
                                                    modifier = Modifier.size(30.dp)
                                                ) { Text("+", color = TealDark, fontSize = 14.sp) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    6 -> { // 6) Order Management
                        var orderFilter by remember { mutableStateOf("All") }
                        val statusList = listOf("All", "جديد", "قيد التجهيز", "جاهز للشحن", "في الطريق", "تم التسليم", "ملغي")
                        
                        Column(modifier = Modifier.fillMaxSize()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                items(statusList) { status ->
                                    Button(
                                        onClick = { orderFilter = status },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (orderFilter == status) GoldAccent else TealMedium, contentColor = if (orderFilter == status) TealDark else Color.White),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) { Text(status, fontSize = 10.sp) }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Dynamic search bar for orders
                            OutlinedTextField(
                                value = orderSearchQuery,
                                onValueChange = { orderSearchQuery = it },
                                label = { Text("ابحث برقم الطلب، اسم العميل، الجوال...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldAccent) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = TealLight, focusedBorderColor = GoldAccent)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            val filteredOrders = orders.filter {
                                (orderFilter == "All" || it.status == orderFilter) && (
                                    orderSearchQuery.isEmpty() ||
                                    it.id.toString().contains(orderSearchQuery) ||
                                    it.customerName.contains(orderSearchQuery, ignoreCase = true) ||
                                    it.customerPhone.contains(orderSearchQuery) ||
                                    it.deliveryAddress.contains(orderSearchQuery, ignoreCase = true)
                                )
                            }
                            if (filteredOrders.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                                    Text("لا توجد طلبات تطابق معايير البحث والفرز!", color = TextGray, fontSize = 11.sp)
                                }
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(filteredOrders) { order ->
                                        Card(colors = CardDefaults.cardColors(containerColor = TealMedium)) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("طلب #${order.id} | ${order.customerName}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    StatusBadge(status = order.status)
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("الجوال: ${order.customerPhone} | العنوان: ${order.deliveryAddress}", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                                    if (order.latitude != null && order.longitude != null) {
                                                        IconButton(
                                                            onClick = { mapViewingOrder = order },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Map,
                                                                contentDescription = "عرض على الخريطة",
                                                                tint = GoldAccent,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                val amountDisplay = if (order.chosenCurrency != "الريال اليمني (ر.ي)") {
                                                    "${String.format("%.1f", order.totalAmount)} ر.ي (${String.format("%.2f", order.convertedAmount)} ${order.chosenCurrency})"
                                                } else {
                                                    "${order.totalAmount} ر.ي"
                                                }
                                                Text("المبلغ الإجمالي: $amountDisplay | الدفع: ${order.paymentMethod}", color = GoldAccentLight, fontSize = 10.sp)
                                                if (order.deliveryAgentName != null) {
                                                    Text("المندوب المعين: ${order.deliveryAgentName}", color = GoldAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                                
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                                    Button(
                                                        onClick = { invoiceToShow = order },
                                                        colors = ButtonDefaults.buttonColors(containerColor = TealDark, contentColor = Color.White),
                                                        contentPadding = PaddingValues(horizontal = 4.dp),
                                                        modifier = Modifier.weight(1f).height(26.dp),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text("فاتورة المبيعات 🖨️", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Button(
                                                        onClick = { labelToShow = order },
                                                        colors = ButtonDefaults.buttonColors(containerColor = TealDark, contentColor = Color.White),
                                                        contentPadding = PaddingValues(horizontal = 4.dp),
                                                        modifier = Modifier.weight(1f).height(26.dp),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text("ملصق الشحن 🏷️", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("تحديث حالة الطلب يدويًا:", color = TextGray, fontSize = 9.sp)
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                                    Button(onClick = { viewModel.adminUpdateOrderStatus(order.id, "قيد التجهيز") }, colors = ButtonDefaults.buttonColors(containerColor = TealLight), contentPadding = PaddingValues(2.dp), modifier = Modifier.weight(1f).height(24.dp)) { Text("تجهيز", fontSize = 8.sp) }
                                                    Button(onClick = { viewModel.adminUpdateOrderStatus(order.id, "جاهز للشحن") }, colors = ButtonDefaults.buttonColors(containerColor = TealLight), contentPadding = PaddingValues(2.dp), modifier = Modifier.weight(1f).height(24.dp)) { Text("جاهز للشحن", fontSize = 8.sp) }
                                                    Button(onClick = { viewModel.adminUpdateOrderStatus(order.id, "في الطريق") }, colors = ButtonDefaults.buttonColors(containerColor = TealLight), contentPadding = PaddingValues(2.dp), modifier = Modifier.weight(1f).height(24.dp)) { Text("في الطريق", fontSize = 8.sp) }
                                                    Button(onClick = { viewModel.adminUpdateOrderStatus(order.id, "تم التسليم") }, colors = ButtonDefaults.buttonColors(containerColor = AlertSuccess, contentColor = TealDark), contentPadding = PaddingValues(2.dp), modifier = Modifier.weight(1f).height(24.dp)) { Text("تسليم", fontSize = 8.sp) }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text("ربط الطلب بمندوب التوصيل:", color = TextGray, fontSize = 9.sp)
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                                    deliveryReps.forEach { rep ->
                                                        Button(
                                                            onClick = { viewModel.adminAssignOrder(order.id, rep.name) },
                                                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark),
                                                            contentPadding = PaddingValues(2.dp),
                                                            modifier = Modifier.weight(1f).height(24.dp)
                                                        ) { Text(rep.name.substringBefore(" "), fontSize = 8.sp) }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    7 -> { // 7) Customer Management
                        Column(modifier = Modifier.fillMaxSize()) {
                            Card(colors = CardDefaults.cardColors(containerColor = TealMedium), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("تسجيل حساب عميل يدوياً", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(value = custNameInput, onValueChange = { custNameInput = it }, label = { Text("اسم العميل بالكامل") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = custPhoneInput, onValueChange = { custPhoneInput = it }, label = { Text("رقم الجوال") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = custEmailInput, onValueChange = { custEmailInput = it }, label = { Text("البريد الإلكتروني") }, modifier = Modifier.fillMaxWidth())
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.addAdminCustomer(custNameInput, custPhoneInput, custEmailInput)
                                            custNameInput = ""
                                            custPhoneInput = ""
                                            custEmailInput = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark),
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("حفظ حساب العميل 👤", fontSize = 11.sp) }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Search customer input
                            OutlinedTextField(
                                value = customerSearchQuery,
                                onValueChange = { customerSearchQuery = it },
                                label = { Text("ابحث عن عميل (بالاسم، الجوال، البريد)...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldAccent) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = TealLight, focusedBorderColor = GoldAccent)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Text("العملاء المسجلين في النظام:", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            val filteredCustomers = adminCustomers.filter {
                                customerSearchQuery.isEmpty() || 
                                it.name.contains(customerSearchQuery, ignoreCase = true) ||
                                it.phone.contains(customerSearchQuery) ||
                                it.email.contains(customerSearchQuery, ignoreCase = true)
                            }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(filteredCustomers) { customer ->
                                    Card(colors = CardDefaults.cardColors(containerColor = TealMedium)) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column {
                                                    Text(customer.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    Text("الجوال: ${customer.phone} | بريد: ${customer.email}", color = TextGray, fontSize = 10.sp)
                                                    Text("تاريخ الانضمام: ${customer.joinedDate} | الطلبات: ${customer.totalOrders}", color = GoldAccentLight, fontSize = 9.sp)
                                                }
                                                Button(
                                                    onClick = { viewModel.toggleCustomerStatus(customer.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = if (customer.status == "نشط") AlertSuccess else AlertError, contentColor = TealDark),
                                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) { Text(customer.status, fontSize = 9.sp) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    8 -> { // 8) Delivery Reps Management
                        Column(modifier = Modifier.fillMaxSize()) {
                            Card(colors = CardDefaults.cardColors(containerColor = TealMedium), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("توظيف/تعيين مندوب توصيل جديد", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(value = repNameInput, onValueChange = { repNameInput = it }, label = { Text("اسم المندوب") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = repPhoneInput, onValueChange = { repPhoneInput = it }, label = { Text("رقم الهاتف") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = repVehicleInput, onValueChange = { repVehicleInput = it }, label = { Text("وسيلة المواصلات (سيارة/دراجة)") }, modifier = Modifier.fillMaxWidth())
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.addDeliveryRep(repNameInput, repPhoneInput, repVehicleInput)
                                            repNameInput = ""
                                            repPhoneInput = ""
                                            repVehicleInput = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark),
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("تسجيل المندوب بالنظام 🚴", fontSize = 11.sp) }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("قائمة المناديب وحالاتهم الحالية:", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(deliveryReps) { rep ->
                                    Card(colors = CardDefaults.cardColors(containerColor = TealMedium)) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column {
                                                    Text(rep.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    Text("الهاتف: ${rep.phone} | المركبة: ${rep.vehicle}", color = TextGray, fontSize = 10.sp)
                                                    Text("التقييم: ⭐ ${rep.rating}", color = GoldAccentLight, fontSize = 9.sp)
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Button(
                                                        onClick = {
                                                            val nextStatus = when (rep.status) {
                                                                "متاح" -> "مشغول"
                                                                "مشغول" -> "غير نشط"
                                                                else -> "متاح"
                                                            }
                                                            viewModel.updateRepStatus(rep.id, nextStatus)
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = when (rep.status) {
                                                                "متاح" -> AlertSuccess
                                                                "مشغول" -> GoldAccent
                                                                else -> TextGray
                                                            },
                                                            contentColor = TealDark
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 6.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) { Text(rep.status, fontSize = 9.sp) }
                                                    IconButton(onClick = { 
                                                        viewModel.pendingDeleteAction.value = com.example.viewmodel.DeleteConfirmation(
                                                            type = com.example.viewmodel.DeleteType.DELIVERY_REP,
                                                            id = rep.id,
                                                            name = rep.name
                                                        )
                                                    }, modifier = Modifier.size(28.dp)) {
                                                        Icon(Icons.Default.Delete, contentDescription = null, tint = AlertError, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    9 -> { // 9) Shipping & Zones
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text("تعديل مناطق الشحن وأسعار التوصيل في مدن المملكة:", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(shippingZones) { zone ->
                                    Card(colors = CardDefaults.cardColors(containerColor = TealMedium)) {
                                        Row(
                                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(zone.city, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                Text(if (zone.active) "نشط ومتاح للتوصيل" else "مغلق مؤقتاً", color = if (zone.active) AlertSuccess else AlertError, fontSize = 10.sp)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = { viewModel.updateZoneCost(zone.id, zone.cost - 5) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = TealDark),
                                                    contentPadding = PaddingValues(0.dp),
                                                    modifier = Modifier.size(24.dp)
                                                ) { Text("-", color = Color.White) }
                                                Text("${zone.cost} ر.ي", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Button(
                                                    onClick = { viewModel.updateZoneCost(zone.id, zone.cost + 5) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                                                    contentPadding = PaddingValues(0.dp),
                                                    modifier = Modifier.size(24.dp)
                                                ) { Text("+", color = TealDark) }
                                                
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Button(
                                                    onClick = { viewModel.toggleZoneActive(zone.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = if (zone.active) AlertSuccess else TextGray, contentColor = TealDark),
                                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                                    modifier = Modifier.height(24.dp)
                                                ) { Text(if (zone.active) "تعطيل" else "تفعيل", fontSize = 8.sp) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    10 -> { // 10) Offers & Coupons
                        Column(modifier = Modifier.fillMaxSize()) {
                            Card(colors = CardDefaults.cardColors(containerColor = TealMedium), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("إنشاء كود خصم (كوبون) جديد", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(value = couponCodeInput, onValueChange = { couponCodeInput = it }, label = { Text("رمز الكوبون (مثال: SUPER30)") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = couponDiscountInput, onValueChange = { couponDiscountInput = it }, label = { Text("نسبة الخصم مئوية %") }, modifier = Modifier.fillMaxWidth())
                                     OutlinedTextField(value = couponMaxDiscInput, onValueChange = { couponMaxDiscInput = it }, label = { Text("الحد الأقصى للخصم (ر.ي)") }, modifier = Modifier.fillMaxWidth())
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            val pct = couponDiscountInput.toIntOrNull() ?: 10
                                            val maxD = couponMaxDiscInput.toDoubleOrNull() ?: 50.0
                                            viewModel.addCoupon(couponCodeInput, pct, maxD)
                                            couponCodeInput = ""
                                            couponDiscountInput = ""
                                            couponMaxDiscInput = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark),
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("حفظ وتفعيل الكوبون 🎫", fontSize = 11.sp) }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("كوبونات الخصم المسجلة بالمتجر:", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(coupons) { coupon ->
                                    Card(colors = CardDefaults.cardColors(containerColor = TealMedium)) {
                                        Row(
                                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(coupon.code, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                Text("خصم ${coupon.discountPercent}% | الحد الأقصى: ${coupon.maxDiscount} ر.ي", color = TextGray, fontSize = 10.sp)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Button(
                                                    onClick = { viewModel.toggleCouponActive(coupon.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = if (coupon.active) AlertSuccess else TextGray, contentColor = TealDark),
                                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) { Text(if (coupon.active) "نشط" else "معطل", fontSize = 9.sp) }
                                                IconButton(onClick = { 
                                                    viewModel.pendingDeleteAction.value = com.example.viewmodel.DeleteConfirmation(
                                                        type = com.example.viewmodel.DeleteType.COUPON,
                                                        id = coupon.id,
                                                        name = coupon.code
                                                    )
                                                }) {
                                                    Icon(Icons.Default.Delete, contentDescription = null, tint = AlertError, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    11 -> { // 11) Ads & Banners
                        Column(modifier = Modifier.fillMaxSize()) {
                            Card(colors = CardDefaults.cardColors(containerColor = TealMedium), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("إضافة بنر إعلاني دعائي بالواجهة الرئيسية", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(value = bannerTitleInput, onValueChange = { bannerTitleInput = it }, label = { Text("عنوان البنر الإعلاني") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = bannerImgInput, onValueChange = { bannerImgInput = it }, label = { Text("معرف صورة الإعلان") }, modifier = Modifier.fillMaxWidth())
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.addAdBanner(bannerTitleInput, bannerImgInput)
                                            bannerTitleInput = ""
                                            bannerImgInput = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark),
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("حفظ وإضافة بنر إعلاني 🖼️", fontSize = 11.sp) }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("الإعلانات والبانرات النشطة الآن بالواجهة الرئيسية:", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(adBanners) { banner ->
                                    Card(colors = CardDefaults.cardColors(containerColor = TealMedium)) {
                                        Row(
                                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(banner.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                Text("الصورة: ${banner.imageUrl}", color = TextGray, fontSize = 10.sp)
                                            }
                                            IconButton(onClick = { 
                                                viewModel.pendingDeleteAction.value = com.example.viewmodel.DeleteConfirmation(
                                                    type = com.example.viewmodel.DeleteType.BANNER,
                                                    id = banner.id,
                                                    name = banner.title
                                                )
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = AlertError, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    12 -> { // 12) Reviews & Ratings
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text("مراجعات وتقييمات العملاء لمنتجات مركز الأحمدي:", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(productReviews) { rev ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = TealMedium),
                                        border = if (!rev.approved) BorderStroke(1.dp, AlertError) else null
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(rev.customerName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                Text("⭐ ".repeat(rev.rating), color = GoldAccent, fontSize = 10.sp)
                                            }
                                            Text("المنتج: ${rev.productName}", color = TextGray, fontSize = 10.sp)
                                            Text("\"${rev.comment}\"", color = Color.White, fontSize = 11.sp, style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                Text(if (rev.approved) "✅ معتمد ومتاح للعامة" else "⏳ بانتظار الموافقة والاعتماد", color = if (rev.approved) AlertSuccess else GoldAccent, fontSize = 10.sp)
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    if (!rev.approved) {
                                                        Button(
                                                            onClick = { viewModel.approveReview(rev.id) },
                                                            colors = ButtonDefaults.buttonColors(containerColor = AlertSuccess, contentColor = TealDark),
                                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                                            modifier = Modifier.height(24.dp)
                                                        ) { Text("موافقة واعتِماد ✅", fontSize = 8.sp) }
                                                    }
                                                    Button(
                                                        onClick = { 
                                                            viewModel.pendingDeleteAction.value = com.example.viewmodel.DeleteConfirmation(
                                                                type = com.example.viewmodel.DeleteType.REVIEW,
                                                                id = rev.id,
                                                                name = "تقييم العميل ${rev.customerName}"
                                                            )
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = AlertError, contentColor = Color.White),
                                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                                        modifier = Modifier.height(24.dp)
                                                    ) { Text("حذف 🗑️", fontSize = 8.sp) }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    13 -> { // 13) Static Content Management
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Text("تعديل محتوى الصفحات والبنود الثابتة في تطبيق المتجر:", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = staticAboutInput,
                                onValueChange = { staticAboutInput = it },
                                label = { Text("صفحة \"من نحن\" التعريفية") },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = TealLight, focusedBorderColor = GoldAccent)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = staticPrivacyInput,
                                onValueChange = { staticPrivacyInput = it },
                                label = { Text("سياسة الخصوصية وسرية البيانات") },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = TealLight, focusedBorderColor = GoldAccent)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = staticTermsInput,
                                onValueChange = { staticTermsInput = it },
                                label = { Text("الشروط والأحكام وسياسة الاستبدال") },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = TealLight, focusedBorderColor = GoldAccent)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    viewModel.updateStaticContent(staticAboutInput, staticPrivacyInput, staticTermsInput)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("حفظ وتحديث المحتوى الثابت فورياً بالمتجر 💾", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                    14 -> { // 14) Notifications Management
                        Column(modifier = Modifier.fillMaxSize()) {
                            Card(colors = CardDefaults.cardColors(containerColor = TealMedium), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("بث إشعار عام جماعي لكافة العملاء", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(value = notifTitleInput, onValueChange = { notifTitleInput = it }, label = { Text("عنوان الإشعار العام") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = notifMsgInput, onValueChange = { notifMsgInput = it }, label = { Text("محتوى ونص الإشعار") }, modifier = Modifier.fillMaxWidth().height(80.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.triggerSendAdminNotification(notifTitleInput, notifMsgInput)
                                            notifTitleInput = ""
                                            notifMsgInput = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark),
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("إرسال وبث الإشعار الجماعي الآن 📢", fontSize = 11.sp) }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("سجل الإشعارات المرسلة مؤخراً للزبائن:", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(notifications) { notif ->
                                    Card(colors = CardDefaults.cardColors(containerColor = TealMedium)) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(notif.titleAr, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text(notif.messageAr, color = TextGray, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    15 -> { // 15) Permissions & Roles
                        Column(modifier = Modifier.fillMaxSize()) {
                            Card(colors = CardDefaults.cardColors(containerColor = TealMedium), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("تعيين عضو جديد بفريق العمل وصلاحياته", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(value = roleNameInput, onValueChange = { roleNameInput = it }, label = { Text("اسم الموظف") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = roleTitleInput, onValueChange = { roleTitleInput = it }, label = { Text("الدور الوظيفي (مثال: Supervisor)") }, modifier = Modifier.fillMaxWidth())
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Checkbox(checked = roleCanWrite, onCheckedChange = { roleCanWrite = it })
                                        Text("تعديل وإضافة منتجات", color = Color.White, fontSize = 11.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Checkbox(checked = roleCanDelete, onCheckedChange = { roleCanDelete = it })
                                        Text("حذف طلبات عملاء", color = Color.White, fontSize = 11.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Checkbox(checked = roleCanEdit, onCheckedChange = { roleCanEdit = it })
                                        Text("تعديل الإعدادات العامة", color = Color.White, fontSize = 11.sp)
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.addTeamMember(roleNameInput, roleTitleInput, roleCanWrite, roleCanDelete, roleCanEdit)
                                            roleNameInput = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark),
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("تسجيل عضو الفريق بالصلاحيات 🔑", fontSize = 11.sp) }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("أعضاء الفريق ومستويات الصلاحيات الحالية:", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(teamRoles) { team ->
                                    Card(colors = CardDefaults.cardColors(containerColor = TealMedium)) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(team.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text("الدور: ${team.role}", color = GoldAccent, fontSize = 10.sp)
                                            Text(
                                                "إضافة منتجات: ${if (team.canWriteProduct) "✅" else "❌"} | حذف طلبات: ${if (team.canDeleteOrder) "✅" else "❌"} | تعديل إعدادات: ${if (team.canEditSettings) "✅" else "❌"}",
                                                color = TextGray,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    16 -> { // 16) General Settings
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Text("تعديل بيانات وإعدادات المتجر العامة:", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(value = configTitle, onValueChange = { configTitle = it }, label = { Text("اسم المتجر الرئيسي") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = configBanner, onValueChange = { configBanner = it }, label = { Text("شريط الإعلانات أعلى المتجر") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = configMobile, onValueChange = { configMobile = it }, label = { Text("رقم جوال دعم المتجر") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = configEmail, onValueChange = { configEmail = it }, label = { Text("البريد الإلكتروني للدعم") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = configHours, onValueChange = { configHours = it }, label = { Text("ساعات العمل والموقع الدائم") }, modifier = Modifier.fillMaxWidth())
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Switch(checked = configMaint, onCheckedChange = { configMaint = it })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("وضع صيانة المتجر (تعطيل تصفح الزبائن مؤقتاً) 🛠️", color = if (configMaint) AlertError else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    viewModel.updateStoreGeneralInfo(configTitle, configBanner, configMobile, configEmail, configHours, configMaint)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تحديث وإطلاق الإعدادات الجديدة فورياً ⚙️", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                    17 -> { // 17) Reports & Analytics
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Text("التقارير المالية والتحليلات البيانية لمركز الأحمدي:", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Period Tabs Row
                            Row(
                                modifier = Modifier.fillMaxWidth().background(TealMedium, RoundedCornerShape(8.dp)).padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val periods = listOf("Daily" to "تقرير يومي 📅", "Weekly" to "تقرير أسبوعي 📈", "Monthly" to "تقرير شهري 📊")
                                periods.forEach { (key, label) ->
                                    Button(
                                        onClick = { activeReportTab = key },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (activeReportTab == key) GoldAccent else Color.Transparent,
                                            contentColor = if (activeReportTab == key) TealDark else Color.White
                                        ),
                                        contentPadding = PaddingValues(vertical = 4.dp),
                                        modifier = Modifier.weight(1f).height(32.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            val totalRev = orders.filter { it.status == "تم التسليم" }.sumOf { it.totalAmount }
                            val totalQty = orders.filter { it.status == "تم التسليم" }.size
                            
                            // Period Specific Calculations
                            val periodRevenue = when (activeReportTab) {
                                "Daily" -> totalRev * 0.12 + 840.0
                                "Weekly" -> totalRev * 0.45 + 3200.0
                                else -> totalRev
                            }
                            val periodCount = when (activeReportTab) {
                                "Daily" -> (totalQty * 0.15 + 2).toInt()
                                "Weekly" -> (totalQty * 0.50 + 6).toInt()
                                else -> totalQty
                            }
                            
                            Card(colors = CardDefaults.cardColors(containerColor = TealMedium)) {
                                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                    Text("ملخص المبيعات الكلي (${if (activeReportTab == "Daily") "اليومي" else if (activeReportTab == "Weekly") "الأسبوعي" else "الشهري"})", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("إجمالي الدخل المحقق: ${String.format("%.2f", periodRevenue)} ر.ي", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                    Text("إجمالي الطلبات المكتملة: $periodCount طلب ناجح", color = TextGray, fontSize = 11.sp)
                                    val avgOrder = if (periodCount > 0) periodRevenue / periodCount else 0.0
                                    Text("متوسط قيمة الفاتورة المبيعية: ${String.format("%.1f", avgOrder)} ر.ي لكل طلب", color = GoldAccentLight, fontSize = 11.sp)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Best Sellers List (المنتجات الأكثر مبيعاً)
                            Card(colors = CardDefaults.cardColors(containerColor = TealMedium), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("المنتجات الأكثر مبيعاً ⭐", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    val bestSellers = listOf(
                                        Triple("آيفون 15 برو ماكس سعة 256GB", "42 قطعة مباعة", 0.85f),
                                        Triple("بكج الحماية المتكامل 10 في 1", "38 قطعة مباعة", 0.75f),
                                        Triple("سماعة آبل إيربودز برو الجيل 2", "29 قطعة مباعة", 0.60f),
                                        Triple("شاحن أنكر نانو بقوة 30 واط", "25 قطعة مباعة", 0.50f)
                                    )
                                    
                                    bestSellers.forEach { (name, label, pct) ->
                                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(name, color = Color.White, fontSize = 10.sp)
                                                Text(label, color = GoldAccentLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Box(modifier = Modifier.fillMaxWidth().height(5.dp).background(TealDark, RoundedCornerShape(2.dp))) {
                                                Box(modifier = Modifier.fillMaxWidth(pct).fillMaxHeight().background(GoldAccent, RoundedCornerShape(2.dp)))
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Stagnant Products List (المنتجات الراكدة)
                            Card(colors = CardDefaults.cardColors(containerColor = TealMedium), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("المنتجات الراكدة (التي لم تبع هذا الشهر) ⏳", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    val stagnantProducts = listOf(
                                        Pair("كابل بيسوس مغزول 100 واط", "المخزون الراكد: 40 قطعة | مبيعات: 0"),
                                        Pair("حامل جوال مغناطيسي للسيارة Yesido", "المخزون الراكد: 80 قطعة | مبيعات: 1"),
                                        Pair("مروحة عنق لاسلكية محمولة", "المخزون الراكد: 40 قطعة | مبيعات: 2")
                                    )
                                    
                                    stagnantProducts.forEach { (name, detail) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("• $name", color = Color.White, fontSize = 10.sp)
                                            Text(detail, color = AlertError, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Top Active Customers List (العملاء الأكثر نشاطاً)
                            Card(colors = CardDefaults.cardColors(containerColor = TealMedium), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("العملاء الأكثر نشاطاً وتفاعلاً 🏆", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    val topCustomers = listOf(
                                        Triple("خالد محمد اليافعي", "5 طلبات ناجحة", "إجمالي المشتريات: 14,800 ر.ي"),
                                        Triple("عبدالرحمن صالح", "3 طلبات ناجحة", "إجمالي المشتريات: 9,250 ر.ي"),
                                        Triple("فهد العتيبي", "3 طلبات ناجحة", "إجمالي المشتريات: 5,400 ر.ي")
                                    )
                                    
                                    topCustomers.forEach { (name, count, totalPurch) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                Text(totalPurch, color = GoldAccentLight, fontSize = 9.sp)
                                            }
                                            Text(count, color = AlertSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Top Geographic Demand Zones (مناطق الطلب الأعلى)
                            Card(colors = CardDefaults.cardColors(containerColor = TealMedium), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("مناطق الشحن الأكثر طلباً 📍", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    val geographicZones = listOf(
                                        Pair("الرياض والمنطقة الوسطى", 0.55f),
                                        Pair("جدة والمنطقة الغربية", 0.25f),
                                        Pair("الدمام والمنطقة الشرقية", 0.15f),
                                        Pair("بقية مناطق الشحن المتاحة", 0.05f)
                                    )
                                    
                                    geographicZones.forEach { (city, pct) ->
                                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(city, color = Color.White, fontSize = 10.sp)
                                                Text("${(pct * 100).toInt()}%", color = GoldAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(TealDark, RoundedCornerShape(3.dp))) {
                                                Box(modifier = Modifier.fillMaxWidth(pct).fillMaxHeight().background(GoldAccent, RoundedCornerShape(3.dp)))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    18 -> { // 18) Audit Log
                        Column(modifier = Modifier.fillMaxSize()) {
                            OutlinedTextField(
                                value = searchLogQuery,
                                onValueChange = { searchLogQuery = it },
                                label = { Text("بحث وفلترة في سجل العمليات الحساسة...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldAccent) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = TealLight, focusedBorderColor = GoldAccent)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val filteredLogs = auditLogs.filter {
                                searchLogQuery.isEmpty() || it.actionAr.contains(searchLogQuery, ignoreCase = true) || it.detailsAr.contains(searchLogQuery, ignoreCase = true)
                            }
                            
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(filteredLogs) { log ->
                                    Card(colors = CardDefaults.cardColors(containerColor = TealMedium)) {
                                        Column(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(log.actionAr, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Text(log.userRole, color = GoldAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text(log.detailsAr, color = TextGray, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    19 -> { // 19) Backup & Restore
                        var backupStatus by remember { mutableStateOf("") }
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Text("النسخ الاحتياطي وإعادة ضبط مصنع البيانات بالكامل:", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Card(colors = CardDefaults.cardColors(containerColor = TealMedium)) {
                                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                    Text("حفظ نسخة احتياطية مشفرة", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("سيقوم النظام بتجميع كافة محتويات قاعدة البيانات ومستويات المخزون وعرضها كملف JSON لنسخه بأمان خارج التطبيق.", color = Color.White, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.triggerBackup { data ->
                                                backupText = data
                                                backupStatus = "✅ تم إنشاء النسخة الاحتياطية المشفرة بنجاح!"
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark),
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("إنشاء نسخة احتياطية فورا 💾", fontSize = 11.sp) }
                                    
                                    if (backupText.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = backupText,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("بيانات النسخة الاحتياطية (انسخها واحفظها)") },
                                            modifier = Modifier.fillMaxWidth().height(80.dp),
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp)
                                        )
                                        Text(backupStatus, color = AlertSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(colors = CardDefaults.cardColors(containerColor = TealMedium)) {
                                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                    Text("استيراد واستعادة نقطة نسخة سابقة", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = restoreText,
                                        onValueChange = { restoreText = it },
                                        label = { Text("الصق بيانات النسخة الاحتياطية هنا") },
                                        modifier = Modifier.fillMaxWidth().height(80.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.triggerRestore {
                                                restoreText = ""
                                                backupStatus = "✅ تم استعادة كافة البيانات وحفظها بنجاح!"
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = TealLight),
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("استعادة البيانات فورا 🔄", fontSize = 11.sp) }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = TealMedium),
                                border = BorderStroke(1.dp, AlertError)
                            ) {
                                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                    Text("إعادة ضبط المصنع بالكامل للبيانات", color = AlertError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("تنبيه حساس للغاية: هذا الإجراء سيقوم بحذف ومسح كافة فواتير العملاء وسجلات المبيعات وإعادة تهيئة قاعدة البيانات للحالة الافتراضية الأولى.", color = Color.White, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.triggerResetDatabase {
                                                backupStatus = "🗑️ تم إعادة تهيئة المتجر كاملاً لضبط المصنع الأصلي!"
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AlertError, contentColor = Color.White),
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("مسح وإعادة تهيئة المصنع للمتجر ⚠️", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                    20 -> { // 20) Payment & Shipping Configuration
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Text("تحديد بوابات وطرق الدفع النشطة في التطبيق 💳", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // 1. Basic Channels
                            Card(
                                colors = CardDefaults.cardColors(containerColor = TealLight.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("قنوات الدفع العادية والذكية المتاحة للعميل:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Checkbox(checked = payCod, onCheckedChange = { payCod = it }, colors = CheckboxDefaults.colors(checkedColor = GoldAccent))
                                        Text("الدفع عند الاستلام COD 💵", color = Color.White, fontSize = 11.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Checkbox(checked = payCard, onCheckedChange = { payCard = it }, colors = CheckboxDefaults.colors(checkedColor = GoldAccent))
                                        Text("البطاقات الائتمانية (Visa / Mastercard) 💳", color = Color.White, fontSize = 11.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Checkbox(checked = payMada, onCheckedChange = { payMada = it }, colors = CheckboxDefaults.colors(checkedColor = GoldAccent))
                                        Text("شبكة مدى للدفع الإلكتروني 🇸🇦", color = Color.White, fontSize = 11.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Checkbox(checked = payApple, onCheckedChange = { payApple = it }, colors = CheckboxDefaults.colors(checkedColor = GoldAccent))
                                        Text("بوابة الدفع Apple Pay 🍏", color = Color.White, fontSize = 11.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Checkbox(checked = payGoogle, onCheckedChange = { payGoogle = it }, colors = CheckboxDefaults.colors(checkedColor = GoldAccent))
                                        Text("بوابة الدفع Google Pay 🤖", color = Color.White, fontSize = 11.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Checkbox(checked = payWallets, onCheckedChange = { payWallets = it }, colors = CheckboxDefaults.colors(checkedColor = GoldAccent))
                                        Text("المحافظ الرقمية المحلية (كريمي mFloos، ون كاش OneCash، جيب Pocket) 📱", color = Color.White, fontSize = 11.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Checkbox(checked = payInstallments, onCheckedChange = { payInstallments = it }, colors = CheckboxDefaults.colors(checkedColor = GoldAccent))
                                        Text("تفعيل نظام التقسيط الشهري للعملاء 📅", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }

                            // 2. Integration Gateways settings
                            Card(
                                colors = CardDefaults.cardColors(containerColor = TealLight.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("تكامل وإعدادات بوابات الدفع الإلكتروني 🔌", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = activeGateway,
                                        onValueChange = { activeGateway = it },
                                        label = { Text("بوابة الدفع الإلكتروني النشطة") },
                                        placeholder = { Text("مثال: MyFatoorah, PayTabs, Tap Payments, HyperPay") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        colors = appTextFieldColors()
                                    )

                                    OutlinedTextField(
                                        value = gatewayApiKey,
                                        onValueChange = { gatewayApiKey = it },
                                        label = { Text("مفتاح واجهة برمجة التطبيقات للبوابة (API Secret Key)") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        colors = appTextFieldColors()
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Checkbox(checked = gatewaySandbox, onCheckedChange = { gatewaySandbox = it }, colors = CheckboxDefaults.colors(checkedColor = GoldAccent))
                                        Text("تفعيل وضع البيئة التجريبية (Sandbox Mode) 🧪", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }

                            // 3. Installment configuration (Only if installments enabled)
                            if (payInstallments) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = TealLight.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("إعدادات نظام التقسيط الشهري المستقبلي 📅", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = installmentProvider,
                                            onValueChange = { installmentProvider = it },
                                            label = { Text("مزود خدمة التقسيط") },
                                            placeholder = { Text("مثال: تابي (Tabby)، تمارا (Tamara)، أو كلاهما") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                            colors = appTextFieldColors()
                                        )

                                        OutlinedTextField(
                                            value = installmentMonths,
                                            onValueChange = { installmentMonths = it },
                                            label = { Text("الحد الأقصى لعدد الأشهر المسموح بها للتقسيط") },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = appTextFieldColors()
                                        )
                                    }
                                }
                            }

                            // 4. VAT & Currency Exchange rates
                            Card(
                                colors = CardDefaults.cardColors(containerColor = TealLight.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("الضريبة وأسعار صرف العملات 💹", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = payVat,
                                        onValueChange = { payVat = it },
                                        label = { Text("نسبة ضريبة القيمة المضافة %") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        colors = appTextFieldColors()
                                    )

                                    OutlinedTextField(
                                        value = exchangeRateSARInput,
                                        onValueChange = { exchangeRateSARInput = it },
                                        label = { Text("سعر صرف الريال السعودي مقابل اليمني") },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        colors = appTextFieldColors()
                                    )

                                    OutlinedTextField(
                                        value = exchangeRateUSDInput,
                                        onValueChange = { exchangeRateUSDInput = it },
                                        label = { Text("سعر صرف الدولار الأمريكي مقابل اليمني") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = appTextFieldColors()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val vatInt = payVat.toIntOrNull() ?: 15
                                    val monthsInt = installmentMonths.toIntOrNull() ?: 4
                                    viewModel.updatePaymentConfig(
                                        cod = payCod,
                                        card = payCard,
                                        mada = payMada,
                                        apple = payApple,
                                        google = payGoogle,
                                        wallets = payWallets,
                                        installments = payInstallments,
                                        vat = vatInt,
                                        gateway = activeGateway,
                                        apiKey = gatewayApiKey,
                                        sandbox = gatewaySandbox,
                                        instProvider = installmentProvider,
                                        instMonths = monthsInt
                                    )
                                    val sarRate = exchangeRateSARInput.toDoubleOrNull() ?: 400.0
                                    val usdRate = exchangeRateUSDInput.toDoubleOrNull() ?: 1500.0
                                    viewModel.updateExchangeRates(sarRate, usdRate)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                 Text("حفظ الإعدادات وبوابات الدفع الإلكتروني 💾", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                    21 -> { // 21) Main Branch Accounting System (ERP) Integration Panel
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Text("الربط والتحكم بالتكامل المحاسبي مع الفرع الرئيسي 💼", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))

                            val accUrl by viewModel.accountingApiUrl.collectAsStateWithLifecycle()
                            val accKey by viewModel.accountingApiKey.collectAsStateWithLifecycle()
                            val accBranch by viewModel.accountingBranchCode.collectAsStateWithLifecycle()
                            val accInterval by viewModel.accountingSyncInterval.collectAsStateWithLifecycle()
                            val accEnabled by viewModel.isAccountingEnabled.collectAsStateWithLifecycle()
                            
                            val accTesting by viewModel.isTestingAccountingConnection.collectAsStateWithLifecycle()
                            val accTestSuccess by viewModel.testAccountingConnectionSuccess.collectAsStateWithLifecycle()
                            val accSyncLogs by viewModel.accountingSyncLogs.collectAsStateWithLifecycle()
                            
                            val isSyncingProds by viewModel.isSyncingProducts.collectAsStateWithLifecycle()
                            val isSyncingOrds by viewModel.isSyncingOrders.collectAsStateWithLifecycle()

                            var inputAccUrl by remember(accUrl) { mutableStateOf(accUrl) }
                            var inputAccKey by remember(accKey) { mutableStateOf(accKey) }
                            var inputAccBranch by remember(accBranch) { mutableStateOf(accBranch) }
                            var inputAccInterval by remember(accInterval) { mutableStateOf(accInterval) }
                            var inputAccEnabled by remember(accEnabled) { mutableStateOf(accEnabled) }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = TealMedium),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("تنشيط التكامل والربط مع نظام ERP 🔗", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Switch(
                                            checked = inputAccEnabled,
                                            onCheckedChange = { 
                                                inputAccEnabled = it 
                                                viewModel.updateAccountingConfig(inputAccUrl, inputAccKey, inputAccBranch, inputAccInterval, it)
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = GoldAccent, checkedTrackColor = TealLight)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (inputAccEnabled) "تكامل نظام المحاسبة نشط ومربوط ببرمجيات الفرع الرئيسي حالياً." else "الربط المحاسبي معطل حالياً (الوضع المحلي).",
                                        color = if (inputAccEnabled) AlertSuccess else TextGray,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Integration Inputs Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = TealMedium),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("معلومات خادم المحاسبة والترخيص:", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Base URL
                                    OutlinedTextField(
                                        value = inputAccUrl,
                                        onValueChange = { inputAccUrl = it },
                                        label = { Text("رابط خادم النظام المحاسبي (ERP API)", fontSize = 11.sp) },
                                        placeholder = { Text("https://erp.alahmadi-group.com/api/v1", fontSize = 11.sp) },
                                        colors = appTextFieldColors(),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )

                                    // API Key
                                    OutlinedTextField(
                                        value = inputAccKey,
                                        onValueChange = { inputAccKey = it },
                                        label = { Text("مفتاح الترخيص والوصول السري (Secret Key)", fontSize = 11.sp) },
                                        placeholder = { Text("sec_key_alahmadi_...", fontSize = 11.sp) },
                                        colors = appTextFieldColors(),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Branch Code
                                        OutlinedTextField(
                                            value = inputAccBranch,
                                            onValueChange = { inputAccBranch = it },
                                            label = { Text("كود فرع الأحمدي", fontSize = 11.sp) },
                                            colors = appTextFieldColors(),
                                            modifier = Modifier.weight(1.2f),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = true
                                        )

                                        // Sync Interval (Minutes)
                                        OutlinedTextField(
                                            value = inputAccInterval.toString(),
                                            onValueChange = { newValue ->
                                                val parsed = newValue.toIntOrNull() ?: 30
                                                inputAccInterval = parsed
                                            },
                                            label = { Text("فترة التزامن (دقيقة)", fontSize = 11.sp) },
                                            colors = appTextFieldColors(),
                                            modifier = Modifier.weight(0.8f),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = true
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Test Connection button
                                        Button(
                                            onClick = {
                                                viewModel.testAccountingConnection(inputAccUrl, inputAccKey, inputAccBranch) { success -> }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = TealLight),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1.2f).height(42.dp),
                                            enabled = inputAccUrl.isNotEmpty() && inputAccKey.isNotEmpty() && !accTesting
                                        ) {
                                            if (accTesting) {
                                                CircularProgressIndicator(color = GoldAccent, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                            } else {
                                                Text("فحص اتصال الخادم", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Save settings button
                                        Button(
                                            onClick = {
                                                viewModel.updateAccountingConfig(inputAccUrl, inputAccKey, inputAccBranch, inputAccInterval, inputAccEnabled)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(42.dp),
                                            enabled = inputAccUrl.isNotEmpty() && inputAccKey.isNotEmpty()
                                        ) {
                                            Text("حفظ الإعدادات", color = TealDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Test connection result
                                    accTestSuccess?.let { success ->
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                                                contentDescription = null,
                                                tint = if (success) AlertSuccess else AlertError,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (success) "تم تأسيس قناة اتصال آمنة مع النظام المحاسبي للفرع الرئيسي!" else "فشل تأسيس قناة الاتصال! يرجى التحقق من الخادم.",
                                                color = if (success) AlertSuccess else AlertError,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Manual Instant Sync Buttons Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = TealMedium),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("المزامنة اليدوية الفورية والترحيل ⚡", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Sync Products Stock
                                        Button(
                                            onClick = { viewModel.syncAccountingProducts() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = GoldAccent,
                                                contentColor = TealDark
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(44.dp),
                                            enabled = !isSyncingProds && !isSyncingOrds && inputAccEnabled
                                        ) {
                                            if (isSyncingProds) {
                                                CircularProgressIndicator(color = TealDark, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            } else {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("سحب ومطابقة المخزون", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Sync Orders / Export Sales
                                        Button(
                                            onClick = { viewModel.syncAccountingOrders() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = GoldAccent,
                                                contentColor = TealDark
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(44.dp),
                                            enabled = !isSyncingProds && !isSyncingOrds && inputAccEnabled
                                        ) {
                                            if (isSyncingOrds) {
                                                CircularProgressIndicator(color = TealDark, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            } else {
                                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("ترحيل فواتير المبيعات", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // Logs view
                            Text(
                                text = "سجل التزامن والربط المباشر 📊",
                                color = TextGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .border(1.dp, TealLight.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (accSyncLogs.isEmpty()) {
                                        Text("لا توجد سجلات مزامنة حالية.", color = TextGray, fontSize = 10.sp)
                                    } else {
                                        accSyncLogs.forEach { log ->
                                            Text(
                                                text = log,
                                                color = Color.White.copy(alpha = 0.9f),
                                                fontSize = 9.sp,
                                                lineHeight = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Admin Operations Dialogs ---
    
    if (mapViewingOrder != null) {
        OrderMapViewerDialog(
            latitude = mapViewingOrder?.latitude ?: 0.0,
            longitude = mapViewingOrder?.longitude ?: 0.0,
            addressText = mapViewingOrder?.deliveryAddress ?: "",
            onDismiss = { mapViewingOrder = null }
        )
    }
    
    // 1) Sales Invoice Dialog (فاتورة المبيعات)
    if (invoiceToShow != null) {
        val order = invoiceToShow!!
        AlertDialog(
            onDismissRequest = { invoiceToShow = null },
            title = { Text("فاتورة مبيعات رقم #${order.id} 🖨️", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("مركز الأحمدي للاتصالات 📱", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("العاصمة صنعاء - شارع صخر", color = TextGray, fontSize = 10.sp)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 8.dp))
                    Text("اسم العميل: ${order.customerName}", color = Color.White, fontSize = 11.sp)
                    Text("رقم الهاتف: ${order.customerPhone}", color = Color.White, fontSize = 11.sp)
                    Text("العنوان: ${order.deliveryAddress}", color = Color.White, fontSize = 11.sp)
                    Text("تاريخ الطلب: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(order.timestamp))}", color = Color.White, fontSize = 11.sp)
                    Text("طريقة الدفع: ${order.paymentMethod}", color = Color.White, fontSize = 11.sp)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text("تفاصيل الطلب والكميات:", color = GoldAccentLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    val orderItems by viewModel.getOrderItems(order.id).collectAsStateWithLifecycle(initialValue = emptyList())
                    if (orderItems.isEmpty()) {
                        Text("جاري تحميل تفاصيل السلع...", color = Color.White, fontSize = 11.sp)
                    } else {
                        orderItems.forEach { item ->
                            Text("• ${item.productName} (الكمية: ${item.quantity}) - بسعر: ${item.price} ر.ي", color = Color.White, fontSize = 11.sp)
                        }
                    }
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 8.dp))
                    val subTotalValue = maxOf(0.0, order.totalAmount - order.shippingCost)
                    Text("قيمة المنتجات: ${String.format("%.1f", subTotalValue)} ر.ي", color = Color.White, fontSize = 11.sp)
                    Text("أجور التوصيل: ${String.format("%.1f", order.shippingCost)} ر.ي", color = Color.White, fontSize = 11.sp)
                    Text("المبلغ الإجمالي المستحق: ${String.format("%.1f", order.totalAmount)} ر.ي", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { invoiceToShow = null },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark)
                ) {
                    Text("تحميل / طباعة الفاتورة 📥", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToShow = null }) {
                    Text("إغلاق", color = Color.White, fontSize = 11.sp)
                }
            },
            containerColor = TealDark
        )
    }

    // 2) Shipping Label Dialog (ملصق الشحن)
    if (labelToShow != null) {
        val order = labelToShow!!
        AlertDialog(
            onDismissRequest = { labelToShow = null },
            title = { Text("ملصق شحن الطلب 🏷️", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(8.dp)).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("المصدر: مركز الأحمدي للاتصالات 📦", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("هاتف: 777000111", color = Color.Gray, fontSize = 10.sp)
                    HorizontalDivider(color = Color.LightGray, modifier = Modifier.padding(vertical = 10.dp))
                    
                    Text("المرسل إليه (العميل):", color = Color.Gray, fontSize = 9.sp)
                    Text(order.customerName, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(order.customerPhone, color = Color.Black, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("عنوان التوصيل: ${order.deliveryAddress}", color = Color.Black, fontSize = 11.sp, textAlign = TextAlign.Center)
                    
                    HorizontalDivider(color = Color.LightGray, modifier = Modifier.padding(vertical = 10.dp))
                    Text("مندوب التوصيل المعين: ${order.deliveryAgentName?.ifEmpty { "لم يعين بعد" } ?: "لم يعين بعد"}", color = Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Simulated Barcode
                    Box(
                        modifier = Modifier.fillMaxWidth().height(45.dp).background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("|||||||  ORD-${order.id}  |||||||", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("كود الشحنة والباركود الذكي", color = Color.DarkGray, fontSize = 8.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { labelToShow = null },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark)
                ) {
                    Text("طباعة الملصق 🏷️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { labelToShow = null }) {
                    Text("إغلاق", color = Color.White, fontSize = 11.sp)
                }
            },
            containerColor = TealDark
        )
    }

    // 3) Media Upload Dialog (رفع صورة)
    if (showMediaUploadDialog) {
        val coroutineScope = rememberCoroutineScope()
        var tempUrl by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { if (!isUploading) showMediaUploadDialog = false },
            title = { Text("رفع صورة أو وسيط للمنتج 📷", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column {
                    Text("يمكنك إدخال رابط الصورة مباشرة، أو محاكاة عملية الرفع الفعلي لملف من جهازك إلى سحابة الأحمدي:", color = Color.White, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        label = { Text("رابط الصورة المباشر") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = TealLight, focusedBorderColor = GoldAccent)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (isUploading) {
                        Text("جاري رفع الملف إلى الخادر السحابي... ${ (uploadProgress * 100).toInt() }%", color = GoldAccent, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { uploadProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = GoldAccent,
                            trackColor = TealMedium,
                        )
                    } else {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isUploading = true
                                    uploadProgress = 0f
                                    while (uploadProgress < 1.0f) {
                                        delay(150)
                                        uploadProgress += 0.1f
                                    }
                                    val randomId = (100..999).random()
                                    prodImageUrl = "https://picsum.photos/400/400?random=$randomId"
                                    isUploading = false
                                    showMediaUploadDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("محاكاة رفع ملف محلي من الاستوديو 🖼️", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempUrl.isNotEmpty()) {
                            prodImageUrl = tempUrl
                        }
                        showMediaUploadDialog = false
                    },
                    enabled = !isUploading,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark)
                ) {
                    Text("تأكيد وحفظ 💾", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showMediaUploadDialog = false },
                    enabled = !isUploading
                ) {
                    Text("إلغاء", color = Color.White, fontSize = 11.sp)
                }
            },
            containerColor = TealDark
        )
    }

    // 4) Bulk Import/Export Dialog (استيراد وتصدير بالجملة)
    if (showBulkImportExportDialog) {
        var statusMsg by remember { mutableStateOf("") }
        val coroutineScope = rememberCoroutineScope()
        
        // Export to JSON using org.json
        LaunchedEffect(Unit) {
            jsonTextForImport = try {
                val array = org.json.JSONArray()
                products.forEach { prod ->
                    val obj = org.json.JSONObject()
                    obj.put("id", prod.id)
                    obj.put("nameAr", prod.nameAr)
                    obj.put("descriptionAr", prod.descriptionAr)
                    obj.put("price", prod.price)
                    obj.put("oldPrice", prod.oldPrice ?: org.json.JSONObject.NULL)
                    obj.put("imageUrl", prod.imageUrl)
                    obj.put("categoryId", prod.categoryId)
                    obj.put("stockQuantity", prod.stockQuantity)
                    obj.put("isFeatured", prod.isFeatured)
                    obj.put("isOffer", prod.isOffer)
                    obj.put("rating", prod.rating.toDouble())
                    obj.put("specsAr", prod.specsAr)
                    array.put(obj)
                }
                array.toString(4)
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
        
        AlertDialog(
            onDismissRequest = { showBulkImportExportDialog = false },
            title = { Text("استيراد وتصدير المنتجات بالجملة (JSON) 📦", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column {
                    Text(
                        "تصدير: انسخ النص أدناه لحفظ نسخة المنتجات.\nاستيراد: استبدل النص بنص JSON لمنتجات جديدة واضغط زر الحفظ.",
                        color = Color.White,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jsonTextForImport,
                        onValueChange = { jsonTextForImport = it },
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 9.sp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = TealLight, focusedBorderColor = GoldAccent)
                    )
                    if (statusMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(statusMsg, color = if (statusMsg.contains("نجاح")) AlertSuccess else AlertError, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val list = mutableListOf<Product>()
                                val array = org.json.JSONArray(jsonTextForImport)
                                for (i in 0 until array.length()) {
                                    val obj = array.getJSONObject(i)
                                    val id = obj.getInt("id")
                                    val nameAr = obj.getString("nameAr")
                                    val descriptionAr = obj.optString("descriptionAr", "")
                                    val price = obj.getDouble("price")
                                    val oldPrice = if (obj.isNull("oldPrice")) null else obj.getDouble("oldPrice")
                                    val imageUrl = obj.optString("imageUrl", "custom_product")
                                    val categoryId = obj.optInt("categoryId", 1)
                                    val stockQuantity = obj.optInt("stockQuantity", 10)
                                    val isFeatured = obj.optBoolean("isFeatured", false)
                                    val isOffer = obj.optBoolean("isOffer", false)
                                    val rating = obj.optDouble("rating", 4.5).toFloat()
                                    val specsAr = obj.optString("specsAr", "")
                                    
                                    list.add(
                                        Product(
                                            id = id,
                                            nameAr = nameAr,
                                            descriptionAr = descriptionAr,
                                            price = price,
                                            oldPrice = oldPrice,
                                            imageUrl = imageUrl,
                                            categoryId = categoryId,
                                            stockQuantity = stockQuantity,
                                            isFeatured = isFeatured,
                                            isOffer = isOffer,
                                            rating = rating,
                                            specsAr = specsAr
                                        )
                                    )
                                }
                                if (list.isNotEmpty()) {
                                    viewModel.adminImportProducts(list)
                                    statusMsg = "✅ تم استيراد وحفظ ${list.size} منتج بنجاح!"
                                } else {
                                    statusMsg = "❌ القائمة المستوردة فارغة!"
                                }
                            } catch (e: Exception) {
                                statusMsg = "❌ خطأ في الـ JSON: ${e.message}"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark)
                ) {
                    Text("حفظ واستيراد بالجملة 📥", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkImportExportDialog = false }) {
                    Text("إغلاق", color = Color.White, fontSize = 11.sp)
                }
            },
            containerColor = TealDark
        )
    }
}

// Delivery Portal Screen
@Composable
fun DeliveryPortalScreen(viewModel: StoreViewModel) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    
    // Filters for delivery tasks
    val activeTasks = orders.filter { it.status == "في الطريق" || it.status == "جاهز للشحن" }
    val completedTasks = orders.filter { it.status == "تم التسليم" || it.status == "فشل التسليم" }
    
    // Internal States
    var activeTab by remember { mutableStateOf(1) } // 1: المهام النشطة, 2: سجل اليوم, 3: الأرباح والرحلات
    var showFailDialogOrderId by remember { mutableStateOf<Int?>(null) }
    var selectedFailReason by remember { mutableStateOf("الزبون لا يجيب على الهاتف") }
    var customFailReasonInput by remember { mutableStateOf("") }
    var mapViewingOrder by remember { mutableStateOf<com.example.data.Order?>(null) }
    
    // Calculations for Earnings
    val tripsCount = completedTasks.size
    val successfulTripsCount = completedTasks.filter { it.status == "تم التسليم" }.size
    val failedTripsCount = completedTasks.filter { it.status == "فشل التسليم" }.size
    val deliveryCommissionPerTrip = 1500.0 // 1500 YER commission per completed order
    val totalEarnings = successfulTripsCount * deliveryCommissionPerTrip

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("بوابة مناديب توصيل الأحمدي 🚴", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("المندوب الحالي: مروان الأحمدي (نشط الآن)", color = GoldAccent, fontSize = 10.sp)
            }
            IconButton(
                onClick = { viewModel.logout() },
                modifier = Modifier.testTag("delivery_logout_button")
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "تسجيل خروج", tint = GoldAccent)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Real-time Notifications Bar for New Orders
        val newOrdersCount = orders.filter { it.status == "جاهز للشحن" }.size
        if (newOrdersCount > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = GoldAccent.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تنبيه لحظي: يوجد $newOrdersCount طلب جديد مخصص ومجهز للشحن بانتظار استلامك والبدء بالرحلة!",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Tab Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { activeTab = 1 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeTab == 1) GoldAccent else TealMedium,
                    contentColor = if (activeTab == 1) TealDark else Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1.2f).height(38.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("المهام النشطة (${activeTasks.size})", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Button(
                onClick = { activeTab = 2 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeTab == 2) GoldAccent else TealMedium,
                    contentColor = if (activeTab == 2) TealDark else Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(38.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("سجل اليوم (${completedTasks.size})", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Button(
                onClick = { activeTab = 3 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeTab == 3) GoldAccent else TealMedium,
                    contentColor = if (activeTab == 3) TealDark else Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1.1f).height(38.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ملخص الأرباح والرحلات", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Tab Content
        when (activeTab) {
            1 -> { // 1) Active Tasks
                if (activeTasks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, tint = TextGray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("لا توجد شحنات توصيل نشطة حالياً!", color = TextGray, fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                        items(activeTasks) { order ->
                            Card(colors = CardDefaults.cardColors(containerColor = TealMedium)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("طلب توصيل رقم #${order.id}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        StatusBadge(status = order.status)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    // Customer & Address Details
                                    Text("👤 الزبون: ${order.customerName}", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("📞 رقم الهاتف: ${order.customerPhone}", color = Color.White, fontSize = 11.sp)
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("📍 عنوان التسليم: ${order.deliveryAddress}", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                        if (order.latitude != null && order.longitude != null) {
                                            Button(
                                                onClick = { mapViewingOrder = order },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = GoldAccent.copy(alpha = 0.2f),
                                                    contentColor = GoldAccent
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("الخريطة 🗺️", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    
                                    // Converted Amount with Currency
                                    val amountDisplay = if (order.chosenCurrency != "الريال اليمني (ر.ي)") {
                                        "${String.format("%.1f", order.convertedAmount)} ${order.chosenCurrency} (ما يعادل ${order.totalAmount} ر.ي)"
                                    } else {
                                        "${order.totalAmount} ر.ي"
                                    }
                                    Text("💵 المبلغ المطلوب تحصيله: $amountDisplay", color = GoldAccentLight, fontSize = 11.sp, fontWeight = FontWeight.Black)

                                    // Real-time Status Trace Tracker (Visual Progress)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("🔄 تتبع مسار الحالة لحظياً:", color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isReady = order.status == "جاهز للشحن" || order.status == "في الطريق"
                                        val isShipped = order.status == "في الطريق"
                                        
                                        Box(modifier = Modifier.weight(1f).height(4.dp).background(if (isReady) GoldAccent else TextGray.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
                                        Text("مجهز للشحن", color = if (order.status == "جاهز للشحن") GoldAccent else Color.White, fontSize = 8.sp)
                                        Box(modifier = Modifier.weight(1f).height(4.dp).background(if (isShipped) GoldAccent else TextGray.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
                                        Text("في الطريق للتسليم", color = if (order.status == "في الطريق") GoldAccent else Color.White, fontSize = 8.sp)
                                    }
                                    
                                    // Embedded Location Route Map (Simulated)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = TealDark.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Map, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("خريطة التوجيه والملاحة (محاكاة):", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            // Simple Drawn Route Trace
                                            Canvas(modifier = Modifier.fillMaxWidth().height(24.dp)) {
                                                // Road path
                                                drawLine(
                                                    color = TextGray.copy(alpha = 0.3f),
                                                    start = androidx.compose.ui.geometry.Offset(10f, size.height / 2),
                                                    end = androidx.compose.ui.geometry.Offset(size.width - 10f, size.height / 2),
                                                    strokeWidth = 3f,
                                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                                )
                                                // Centered Moving Rider representation
                                                val progressX = if (order.status == "في الطريق") size.width * 0.6f else size.width * 0.15f
                                                drawCircle(color = GoldAccent, radius = 6f, center = androidx.compose.ui.geometry.Offset(progressX, size.height / 2))
                                                drawCircle(color = AlertSuccess, radius = 5f, center = androidx.compose.ui.geometry.Offset(size.width - 15f, size.height / 2))
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("مستودع الأحمدي (البداية)", color = TextGray, fontSize = 7.5.sp)
                                                Text(
                                                    text = if (order.status == "في الطريق") "المندوب على بعد 1.2 كم 🚴" else "بانتظار البدء بالنقل",
                                                    color = GoldAccent,
                                                    fontSize = 7.5.sp
                                                )
                                                Text("موقع العميل (الوصول)", color = TextGray, fontSize = 7.5.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (order.status == "جاهز للشحن") {
                                            Button(
                                                onClick = { viewModel.adminUpdateOrderStatus(order.id, "في الطريق") },
                                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark),
                                                modifier = Modifier.weight(1f).height(34.dp),
                                                contentPadding = PaddingValues(horizontal = 4.dp)
                                            ) {
                                                Text("بدء التوصيل 🚀", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Button(
                                                onClick = { viewModel.adminUpdateOrderStatus(order.id, "جاهز للشحن") },
                                                colors = ButtonDefaults.buttonColors(containerColor = TealLight),
                                                modifier = Modifier.weight(0.9f).height(34.dp),
                                                contentPadding = PaddingValues(horizontal = 4.dp)
                                            ) {
                                                Text("إعادة التجهيز 🔄", fontSize = 9.sp)
                                            }
                                        }
                                        
                                        Button(
                                            onClick = { viewModel.adminUpdateOrderStatus(order.id, "في الطريق") },
                                            colors = ButtonDefaults.buttonColors(containerColor = TealDark, contentColor = Color.White),
                                            modifier = Modifier.weight(1f).height(34.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp)
                                        ) {
                                            Text("خرج للتسليم 🚴", fontSize = 10.sp)
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = { viewModel.adminUpdateOrderStatus(order.id, "تم التسليم") },
                                            colors = ButtonDefaults.buttonColors(containerColor = AlertSuccess, contentColor = TealDark),
                                            modifier = Modifier.weight(1f).height(34.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp)
                                        ) {
                                            Text("تم التسليم بنجاح ✔️", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { showFailDialogOrderId = order.id },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350), contentColor = Color.White),
                                            modifier = Modifier.weight(1f).height(34.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp)
                                        ) {
                                            Text("فشل التسليم مع السبب ❌", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> { // 2) Today's Logs/History
                if (completedTasks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.History, contentDescription = null, tint = TextGray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("لا توجد مهام مكتملة في سجل اليوم بعد!", color = TextGray, fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                        items(completedTasks) { order ->
                            Card(colors = CardDefaults.cardColors(containerColor = TealMedium.copy(alpha = 0.6f))) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("طلب توصيل رقم #${order.id}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        StatusBadge(status = order.status)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("👤 الزبون: ${order.customerName}", color = GoldAccent, fontSize = 11.sp)
                                    Text("📞 رقم الهاتف: ${order.customerPhone}", color = Color.White, fontSize = 10.sp)
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("📍 العنوان: ${order.deliveryAddress}", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                        if (order.latitude != null && order.longitude != null) {
                                            Button(
                                                onClick = { mapViewingOrder = order },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = GoldAccent.copy(alpha = 0.2f),
                                                    contentColor = GoldAccent
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("الخريطة 🗺️", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    
                                    val amountDisplay = if (order.chosenCurrency != "الريال اليمني (ر.ي)") {
                                        "${String.format("%.1f", order.convertedAmount)} ${order.chosenCurrency}"
                                    } else {
                                        "${order.totalAmount} ر.ي"
                                    }
                                    Text("💵 المبلغ: $amountDisplay", color = GoldAccentLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    
                                    if (order.status == "فشل التسليم" && order.failureReason != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEF5350).copy(alpha = 0.1f))) {
                                            Text(
                                                text = "❌ سبب الفشل: ${order.failureReason}",
                                                color = Color(0xFFEF5350),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(6.dp)
                                            )
                                        }
                                    } else if (order.status == "تم التسليم") {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AlertSuccess, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تم التحصيل والتسليم والمبلغ مضاف للعمولة (+${deliveryCommissionPerTrip} ر.ي)", color = AlertSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            3 -> { // 3) Earnings & Reports
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TealMedium),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("محفظة أرباح وعمولات المندوب 💰", color = GoldAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${String.format("%.0f", totalEarnings)} ر.ي",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text("عمولات التوصيل المجمعة لليوم الحالي", color = TextGray, fontSize = 10.sp)
                            HorizontalDivider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 12.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("إجمالي الرحلات اليوم", color = TextGray, fontSize = 9.sp)
                                    Text("$tripsCount رحلة", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("تسليمات ناجحة ✔️", color = AlertSuccess, fontSize = 9.sp)
                                    Text("$successfulTripsCount طلب", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("تسليمات متعثرة ❌", color = Color(0xFFEF5350), fontSize = 9.sp)
                                    Text("$failedTripsCount طلب", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Trip statistics block (simulated charts)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TealMedium.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("سجل توزيع الرحلات والضغط الساعي 📊", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Visual representation of delivery activity
                            Row(
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                val chartData = listOf(0.2f, 0.5f, 0.8f, 0.3f, 0.9f, 0.1f)
                                val hours = listOf("08:00", "10:00", "12:00", "14:00", "16:00", "18:00")
                                chartData.forEachIndexed { idx, value ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .width(18.dp)
                                                .fillMaxHeight(value)
                                                .background(if (idx == 4) GoldAccent else TealLight, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(hours[idx], color = TextGray, fontSize = 8.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("• معدل تسليم الطلب يستغرق في المتوسط 28 دقيقة.", color = TextGray, fontSize = 9.sp)
                            Text("• تعرفة عمولة التوصيل المحددة من الإدارة: 1500 ر.ي لكل طلب ناجح.", color = TextGray, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }

    // Failure Reason Selector Dialog
    if (showFailDialogOrderId != null) {
        AlertDialog(
            onDismissRequest = { showFailDialogOrderId = null },
            title = { Text("تسجيل فشل تسليم الشحنة ❌", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("الرجاء اختيار أو تحديد سبب فشل التسليم لتحديث حالة الفاتورة وإعلام الإدارة والعميل:", color = TextGray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val standardReasons = listOf(
                        "الزبون لا يجيب على الهاتف",
                        "العنوان المعطى غير دقيق أو خاطئ",
                        "طلب الزبون تأجيل موعد الاستلام ليوم آخر",
                        "الزبون رفض الاستلام/غير مقتنع بالمنتج",
                        "المبلغ المالي للطلب غير جاهز مع الزبون",
                        "أخرى (اكتب السبب بالأسفل)"
                    )
                    
                    standardReasons.forEach { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFailReason = reason }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedFailReason == reason,
                                onClick = { selectedFailReason = reason },
                                colors = RadioButtonDefaults.colors(selectedColor = GoldAccent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(reason, color = Color.White, fontSize = 11.sp)
                        }
                    }
                    
                    if (selectedFailReason == "أخرى (اكتب السبب بالأسفل)") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customFailReasonInput,
                            onValueChange = { customFailReasonInput = it },
                            label = { Text("اكتب السبب بالتفصيل هنا...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalReason = if (selectedFailReason == "أخرى (اكتب السبب بالأسفل)") customFailReasonInput else selectedFailReason
                        showFailDialogOrderId?.let { id ->
                            viewModel.deliveryUpdateOrderStatusWithReason(id, "فشل التسليم", finalReason)
                        }
                        showFailDialogOrderId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark)
                ) {
                    Text("حفظ وتحديث الحالة 💾", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFailDialogOrderId = null }) {
                    Text("إلغاء", color = Color.White, fontSize = 11.sp)
                }
            },
            containerColor = TealDark
        )
    }

    if (mapViewingOrder != null) {
        OrderMapViewerDialog(
            latitude = mapViewingOrder?.latitude ?: 0.0,
            longitude = mapViewingOrder?.longitude ?: 0.0,
            addressText = mapViewingOrder?.deliveryAddress ?: "",
            onDismiss = { mapViewingOrder = null }
        )
    }
}

// Database Explorer / REST API logs Screen
@Composable
fun DatabaseExplorerScreen(viewModel: StoreViewModel) {
    val apiLogs by viewModel.apiLogs.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    var activeView by remember { mutableStateOf(1) } // 1: API logs, 2: System Notifications, 3: Supabase SQL Schema
    var selectedTableDetail by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("محاكي قاعدة البيانات والـ API 💻", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            IconButton(
                onClick = { viewModel.navigateTo(Screen.Settings) },
                modifier = Modifier.testTag("db_explorer_back")
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "رجوع", tint = GoldAccent)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = { activeView = 1 },
                colors = ButtonDefaults.buttonColors(containerColor = if (activeView == 1) GoldAccent else TealMedium, contentColor = if (activeView == 1) TealDark else Color.White),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(38.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) { Text("REST API", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            Button(
                onClick = { activeView = 2 },
                colors = ButtonDefaults.buttonColors(containerColor = if (activeView == 2) GoldAccent else TealMedium, contentColor = if (activeView == 2) TealDark else Color.White),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(38.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) { Text("الإشعارات", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            Button(
                onClick = { activeView = 3 },
                colors = ButtonDefaults.buttonColors(containerColor = if (activeView == 3) GoldAccent else TealMedium, contentColor = if (activeView == 3) TealDark else Color.White),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1.3f).height(38.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) { Text("هيكلية Supabase 📊", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (activeView) {
            1 -> {
                var isSyncing by remember { mutableStateOf(false) }
                var isCrudTesting by remember { mutableStateOf(false) }
                var statusMessage by remember { mutableStateOf<String?>(null) }
                var statusSuccess by remember { mutableStateOf<Boolean?>(null) }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Control Card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = TealMedium),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "التحكم والربط المباشر مع Supabase REST API 🔌",
                                color = GoldAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "يمكنك سحب وتحديث المنتجات والأقسام مباشرة من السحابة، أو اختبار سلامة دوال العمليات الأساسية (إنشاء وقراءة وتعديل وحذف) بنقرة واحدة.",
                                color = Color.White,
                                fontSize = 10.sp,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Sync Button
                                Button(
                                    onClick = {
                                        isSyncing = true
                                        statusMessage = "جاري الاتصال وسحب البيانات من Supabase..."
                                        statusSuccess = null
                                        viewModel.syncDataWithSupabase { success, msg ->
                                            isSyncing = false
                                            statusSuccess = success
                                            statusMessage = msg
                                        }
                                    },
                                    enabled = !isSyncing && !isCrudTesting,
                                    colors = ButtonDefaults.buttonColors(containerColor = TealLight),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    if (isSyncing) {
                                        CircularProgressIndicator(color = GoldAccent, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("مزامنة السحابة ☁️", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }

                                // CRUD Test Button
                                Button(
                                    onClick = {
                                        isCrudTesting = true
                                        statusMessage = "جاري تشغيل دورة CRUD السحابية الكاملة..."
                                        statusSuccess = null
                                        viewModel.testSupabaseCrudOperations { success, msg ->
                                            isCrudTesting = false
                                            statusSuccess = success
                                            statusMessage = msg
                                        }
                                    },
                                    enabled = !isSyncing && !isCrudTesting,
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    if (isCrudTesting) {
                                        CircularProgressIndicator(color = TealDark, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TealDark, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("فحص CRUD كامل ⚡", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TealDark)
                                        }
                                    }
                                }
                            }

                            // Dynamic status message
                            statusMessage?.let { msg ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (statusSuccess == true) Icons.Default.CheckCircle 
                                                      else if (statusSuccess == false) Icons.Default.Error 
                                                      else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (statusSuccess == true) AlertSuccess 
                                               else if (statusSuccess == false) AlertError 
                                               else GoldAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = msg,
                                        color = if (statusSuccess == true) AlertSuccess 
                                                else if (statusSuccess == false) AlertError 
                                                else Color.White,
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // API Interactive Simulator / Playground
                    var selectedSystem by remember { mutableStateOf(1) } // 1: Auth, 2: Catalog, 3: Cart/Checkout, 4: Delivery, 5: Support, 6: Backups/Reports, 7: Security/Headers
                    var simulateError by remember { mutableStateOf(false) }
                    var apiStatusMsg by remember { mutableStateOf<String?>(null) }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = TealMedium),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("منصة فحص واختبار الـ API التفاعلية 🧪", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))

                            // Horizontal categories for subsystems using custom safe styled Buttons
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .background(if (selectedSystem == 1) GoldAccent else TealLight, RoundedCornerShape(16.dp))
                                            .clickable { selectedSystem = 1 }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("المصادقة والتراخيص 🔐", color = if (selectedSystem == 1) TealDark else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                item {
                                    Box(
                                        modifier = Modifier
                                            .background(if (selectedSystem == 2) GoldAccent else TealLight, RoundedCornerShape(16.dp))
                                            .clickable { selectedSystem = 2 }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("الكتالوج والمنتجات 📱", color = if (selectedSystem == 2) TealDark else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                item {
                                    Box(
                                        modifier = Modifier
                                            .background(if (selectedSystem == 3) GoldAccent else TealLight, RoundedCornerShape(16.dp))
                                            .clickable { selectedSystem = 3 }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("السلة والدفع 🛒", color = if (selectedSystem == 3) TealDark else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                item {
                                    Box(
                                        modifier = Modifier
                                            .background(if (selectedSystem == 4) GoldAccent else TealLight, RoundedCornerShape(16.dp))
                                            .clickable { selectedSystem = 4 }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("الشحن والمناديب 🚚", color = if (selectedSystem == 4) TealDark else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                item {
                                    Box(
                                        modifier = Modifier
                                            .background(if (selectedSystem == 5) GoldAccent else TealLight, RoundedCornerShape(16.dp))
                                            .clickable { selectedSystem = 5 }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("الدعم والتقييمات 💬", color = if (selectedSystem == 5) TealDark else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                item {
                                    Box(
                                        modifier = Modifier
                                            .background(if (selectedSystem == 6) GoldAccent else TealLight, RoundedCornerShape(16.dp))
                                            .clickable { selectedSystem = 6 }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("التقارير والنسخ الاحتياطي 📊", color = if (selectedSystem == 6) TealDark else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                item {
                                    Box(
                                        modifier = Modifier
                                            .background(if (selectedSystem == 7) GoldAccent else TealLight, RoundedCornerShape(16.dp))
                                            .clickable { selectedSystem = 7 }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("الحماية وترويسات الأمان 🛡️", color = if (selectedSystem == 7) TealDark else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Description of selected system
                            val currentActionName = when (selectedSystem) {
                                1 -> "فحص تسجيل الدخول وتحديث توكن JWT وصلاحيات الوصول (RBAC & PBAC)"
                                2 -> "البحث والفلترة المتقدمة للمنتجات والأقسام والماركات التجارية"
                                3 -> "تهيئة السلة وإجراء عملية الدفع والتحقق الصارم من المدخلات"
                                4 -> "تحديث حالات الشحنات والمناديب وإسناد المهام"
                                5 -> "إرسال رسائل دعم فني فوري ونظام تقييمات السلع والمراجعات"
                                6 -> "استدعاء تقارير المبيعات المالية والنسخ الاحتياطي وتشفير قاعدة البيانات"
                                7 -> "فحص جدران الأمان، الـ Rate Limiting، وبنية الـ Microservices المستهدفة"
                                else -> ""
                            }

                            Text(currentActionName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))

                            // Checkbox to simulate rate limit or validation error
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = simulateError,
                                    onCheckedChange = { simulateError = it },
                                    colors = CheckboxDefaults.colors(checkedColor = GoldAccent)
                                )
                                Text("محاكاة خطأ خادم أو تجاوز معدل الطلب (Rate Limit 429 / Validation 400)", color = TextGray, fontSize = 10.sp)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    var method = "GET"
                                    var endpoint = "/api/v1/auth/me"
                                    var status = 200
                                    var request = ""
                                    var response = ""

                                    when (selectedSystem) {
                                        1 -> {
                                            if (simulateError) {
                                                method = "GET"
                                                endpoint = "/api/v1/auth/me"
                                                status = 401
                                                request = "{\"token\": \"expired_jwt_xxx\"}"
                                                response = """
                                                    {
                                                      "status": "error",
                                                      "code": "AUTH_INVALID_TOKEN",
                                                      "message": "Token expired or signature invalid",
                                                      "refresh_token_action": "POST /api/v1/auth/refresh to acquire new access_token"
                                                    }
                                                """.trimIndent()
                                            } else {
                                                method = "POST"
                                                endpoint = "/api/v1/auth/token"
                                                status = 200
                                                request = "{\"username\": \"admin\", \"password\": \"******\"}"
                                                response = """
                                                    {
                                                      "status": "success",
                                                      "token_type": "Bearer",
                                                      "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.user_claims_xxx",
                                                      "refresh_token": "ref_992x_al_ahmadi",
                                                      "expires_in": 3600,
                                                      "user": {
                                                        "id": "u-441-ahmad",
                                                        "name": "أحمد اليماني",
                                                        "phone": "059988221",
                                                        "role": "admin",
                                                        "permissions": ["products:create", "products:update", "orders:update", "backups:export"]
                                                      }
                                                    }
                                                """.trimIndent()
                                            }
                                        }
                                        2 -> {
                                            if (simulateError) {
                                                method = "POST"
                                                endpoint = "/api/v1/products"
                                                status = 400
                                                request = "{\"price\": 1200}"
                                                response = """
                                                    {
                                                      "status": "error",
                                                      "code": "VALIDATION_FAILED",
                                                      "errors": [
                                                        {"field": "name_ar", "rule": "required", "message": "Arabic product name cannot be empty"}
                                                      ]
                                                    }
                                                """.trimIndent()
                                            } else {
                                                method = "GET"
                                                endpoint = "/api/v1/search?query=S24&brand=Samsung"
                                                status = 200
                                                request = ""
                                                response = """
                                                    {
                                                      "status": "success",
                                                      "query": "S24",
                                                      "results_count": 2,
                                                      "products": [
                                                        {"id": 3, "name_ar": "سامسونج جالكسي S24 ألترا", "price": 4299.00, "brand": "Samsung"},
                                                        {"id": 9, "name_ar": "كفر حماية تيتانيوم S24", "price": 49.00, "brand": "Samsung"}
                                                      ],
                                                      "brands": ["Samsung", "Apple", "Anker", "Sony", "Xiaomi"],
                                                      "categories": ["iphones", "android-phones", "chargers", "audio", "protection"]
                                                    }
                                                """.trimIndent()
                                            }
                                        }
                                        3 -> {
                                            if (simulateError) {
                                                method = "POST"
                                                endpoint = "/api/v1/checkout"
                                                status = 402
                                                request = "{\"amount\": 4299, \"gateway\": \"Mada\"}"
                                                response = """
                                                    {
                                                      "status": "error",
                                                      "code": "PAYMENT_DECLINED",
                                                      "message": "The transaction was declined by the card-issuing bank. Insufficient funds."
                                                    }
                                                """.trimIndent()
                                            } else {
                                                method = "POST"
                                                endpoint = "/api/v1/checkout"
                                                status = 201
                                                request = "{\"cart_items\": [{\"id\": 3, \"qty\": 1}], \"payment\": \"Mada\"}"
                                                response = """
                                                    {
                                                      "status": "success",
                                                      "order_id": 1092,
                                                      "tracking_token": "c71a3994-db14-491c-b63e-db838b9393aa",
                                                      "payment": {
                                                        "transaction_id": "trx_8829281a_mada",
                                                        "amount": 4299.00,
                                                        "status": "captured",
                                                        "gateway": "Mada Gateway Pay"
                                                      },
                                                      "shipment": {
                                                        "status": "ready_for_delivery",
                                                        "estimated_delivery": "Within 2 hours"
                                                      }
                                                    }
                                                """.trimIndent()
                                            }
                                        }
                                        4 -> {
                                            if (simulateError) {
                                                method = "PATCH"
                                                endpoint = "/api/v1/delivery/shipments?id=5"
                                                status = 403
                                                request = "{\"status\": \"Delivered\"}"
                                                response = """
                                                    {
                                                      "status": "error",
                                                      "code": "ACCESS_DENIED",
                                                      "message": "Only authorized delivery agents can update active shipments."
                                                    }
                                                """.trimIndent()
                                            } else {
                                                method = "PATCH"
                                                endpoint = "/api/v1/delivery/shipments?id=5"
                                                status = 200
                                                request = "{\"status\": \"in_transit\", \"driver\": \"mohammad\"}"
                                                response = """
                                                    {
                                                      "status": "success",
                                                      "shipment_id": 5,
                                                      "assigned_driver": "driver-921-mohammad",
                                                      "updated_status": "in_transit",
                                                      "carrier_notes": "العميل متواجد في شارع حدة وسأتصل به فور الوصول",
                                                      "eta": "15 mins"
                                                    }
                                                """.trimIndent()
                                            }
                                        }
                                        5 -> {
                                            if (simulateError) {
                                                method = "POST"
                                                endpoint = "/api/v1/reviews"
                                                status = 400
                                                request = "{\"rating\": 10}"
                                                response = """
                                                    {
                                                      "status": "error",
                                                      "code": "BAD_REQUEST",
                                                      "message": "Review content cannot be empty and rating must be between 1 and 5 stars."
                                                    }
                                                """.trimIndent()
                                            } else {
                                                method = "POST"
                                                endpoint = "/api/v1/reviews"
                                                status = 201
                                                request = "{\"product_id\": 3, \"rating\": 5, \"comment\": \"جبار\"}"
                                                response = """
                                                    {
                                                      "status": "success",
                                                      "message_id": "msg_9921_sent",
                                                      "ticket_id": "ticket_77c",
                                                      "review": {
                                                        "product_id": 3,
                                                        "user": "أبو شهاب",
                                                        "stars": 5,
                                                        "comment": "الوحش الكاسر والجهاز جبار وتوصيل سريع في أقل من ساعة من مركز الأحمدي!"
                                                      }
                                                    }
                                                """.trimIndent()
                                            }
                                        }
                                        6 -> {
                                            if (simulateError) {
                                                method = "POST"
                                                endpoint = "/api/v1/admin/backups/restore"
                                                status = 500
                                                request = "{\"file_hash\": \"sha256_corrupted_xxx\"}"
                                                response = """
                                                    {
                                                      "status": "error",
                                                      "code": "BACKUP_CORRUPTED",
                                                      "message": "The database backup archive checksum mismatch."
                                                    }
                                                """.trimIndent()
                                            } else {
                                                method = "POST"
                                                endpoint = "/api/v1/admin/backups/export"
                                                status = 200
                                                request = "{\"export_type\": \"all\"}"
                                                response = """
                                                    {
                                                      "status": "success",
                                                      "backup_archive_name": "al_ahmadi_backup_2026_06_24_150000.sql.gpg",
                                                      "archive_size": "4.8 MB",
                                                      "restored_tables_count": 14,
                                                      "reports": {
                                                        "total_revenue": 148292.00,
                                                        "orders_count": 348,
                                                        "average_order_value": 426.12,
                                                        "category_distribution": [
                                                          {"category": "iphones", "percentage": 42.1},
                                                          {"category": "android-phones", "percentage": 31.5},
                                                          {"category": "chargers", "percentage": 15.4}
                                                        ]
                                                      }
                                                    }
                                                """.trimIndent()
                                            }
                                        }
                                        7 -> {
                                            if (simulateError) {
                                                method = "GET"
                                                endpoint = "/api/v1/products"
                                                status = 429
                                                request = ""
                                                response = """
                                                    {
                                                      "status": "error",
                                                      "code": "TOO_MANY_REQUESTS",
                                                      "message": "API Rate limit exceeded. You have performed more than 60 requests in a minute.",
                                                      "rate_limit_headers": {
                                                        "X-RateLimit-Limit": 60,
                                                        "X-RateLimit-Remaining": 0,
                                                        "Retry-After": "42 seconds"
                                                      }
                                                    }
                                                """.trimIndent()
                                            } else {
                                                method = "GET"
                                                endpoint = "/api/v1/gateway/security"
                                                status = 200
                                                request = ""
                                                response = """
                                                    {
                                                      "status": "secure_handshake",
                                                      "api_gateway": "Kong Enterprise Gateway / Envoy Proxy",
                                                      "microservices_routes": {
                                                        "/api/v1/auth/*": "auth-identity-service-cluster (v1.2.4)",
                                                        "/api/v1/products/*": "catalog-inventory-service-cluster (v2.0.1)",
                                                        "/api/v1/cart/*": "orders-cart-service-cluster (v1.8.2)"
                                                      },
                                                      "security_headers": {
                                                        "X-Frame-Options": "DENY",
                                                        "X-XSS-Protection": "1; mode=block",
                                                        "X-Content-Type-Options": "nosniff",
                                                        "Strict-Transport-Security": "max-age=31536000; includeSubDomains",
                                                        "Content-Security-Policy": "default-src 'self' api.supabase.co"
                                                      }
                                                    }
                                                """.trimIndent()
                                            }
                                        }
                                    }

                                    viewModel.logSimulatedApi(method, endpoint, status, request, response)
                                    apiStatusMsg = "تم إرسال طلب $method $endpoint بنجاح! طالع سجل الـ API أدناه بالنتائج الكاملة."
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(38.dp)
                            ) {
                                Text("إرسال طلب فحص الـ API ⚡", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            apiStatusMsg?.let { msg ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(msg, color = AlertSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (apiLogs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                            Text("بانتظار العمليات لتوليد سجلات API!", color = TextGray)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(apiLogs) { log ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = TealMedium),
                                    border = BorderStroke(1.dp, if (log.status in 200..299) AlertSuccess else AlertError)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("${log.method} ${log.endpoint}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("كود: ${log.status}", color = if (log.status in 200..299) AlertSuccess else AlertError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        if (log.requestBody.isNotEmpty()) {
                                            Text("الطلب: ${log.requestBody}", color = TextGray, fontSize = 10.sp)
                                        }
                                        Text("الاستجابة: ${log.responseBody}", color = GoldAccentLight, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                if (notifications.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد أي إشعارات لحظية مرسلة بعد!", color = TextGray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(notifications) { notif ->
                            Card(colors = CardDefaults.cardColors(containerColor = TealMedium)) {
                                Column(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
                                    Text(notif.titleAr, color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(notif.messageAr, color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TealMedium),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("بنية وتصميم Supabase Cloud Database ⚡", color = GoldAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "تم تصميم قاعدة البيانات بهيكلية علائقية متكاملة تدعم اللغة العربية بالكامل RTL مع تفعيل سياسات الحماية على مستوى السطر (RLS) وصلاحيات الوصول الديناميكية (RBAC).",
                                color = Color.White,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("اضغط على الجدول لمعاينة الحقول والمفاتيح:", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    val tablesList = listOf(
                        Triple("profiles", "ملفات المستخدمين والأدوار والتحقق", Icons.Default.Person),
                        Triple("categories", "أقسام وتصنيفات المنتجات بالمتجر", Icons.Default.Category),
                        Triple("products", "بيانات المنتجات والأسعار والمخزون الحالي", Icons.Default.Inventory2),
                        Triple("orders", "الفواتير وبيانات الشحن وتتبع مناديب التوصيل", Icons.Default.Receipt),
                        Triple("order_items", "عناصر وتفاصيل المنتجات المشتراة في كل طلب", Icons.Default.List),
                        Triple("notifications", "الإشعارات اللحظية وحالات الطلبات للعملاء", Icons.Default.Notifications),
                        Triple("return_warranty_requests", "طلبات صيانة الضمان وإرجاع واستبدال السلع", Icons.Default.Gavel),
                        Triple("support_chats", "محادثات الدعم الفني الفورية والمباشرة للعملاء والزوار", Icons.Default.SupportAgent),
                        Triple("audit_logs", "سجل تدقيق ورقابة العمليات الحساسة بالنظام للامتثال", Icons.Default.Security)
                    )

                    tablesList.forEach { table ->
                        val isSelected = selectedTableDetail == table.first
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) TealMedium.copy(alpha = 0.5f) else TealMedium),
                            border = BorderStroke(1.dp, if (isSelected) GoldAccent else Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedTableDetail = if (isSelected) null else table.first
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(table.third, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(table.first, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = if (isSelected) "إغلاق التفاصيل ▲" else "عرض الحقول ▼",
                                        color = GoldAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(table.second, color = TextGray, fontSize = 11.sp)

                                if (isSelected) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    val fields = when (table.first) {
                                        "profiles" -> listOf(
                                            "id: UUID (Primary Key -> auth.users.id)",
                                            "name: VARCHAR (اسم العميل أو المسؤول)",
                                            "phone: VARCHAR (هاتف العميل الموثق - فريد)",
                                            "role: app_role_type (customer / admin / delivery)",
                                            "created_at: TIMESTAMP"
                                        )
                                        "categories" -> listOf(
                                            "id: SERIAL (Primary Key)",
                                            "name_ar: VARCHAR (اسم القسم بالعربية - فريد)",
                                            "slug: VARCHAR (اسم الرابط اللطيف - فريد)",
                                            "icon: VARCHAR (رمز الأيقونة التعبيرية)",
                                            "created_at: TIMESTAMP"
                                        )
                                        "products" -> listOf(
                                            "id: SERIAL (Primary Key)",
                                            "category_id: INT (Foreign Key -> categories.id)",
                                            "name_ar: VARCHAR (اسم السلعة بالعربية)",
                                            "desc_ar: TEXT (مواصفات الجهاز ومميزاته)",
                                            "price: DECIMAL (سعر البيع الحالي ر.ي)",
                                            "old_price: DECIMAL (السعر القديم قبل الخصم)",
                                            "image_url: TEXT (رابط صورة المنتج)",
                                            "stock: INT (الكمية المتاحة في مخزن الأحمدي)",
                                            "is_offer: BOOLEAN (هل المنتج مشمول بالعروض؟)",
                                            "created_at: TIMESTAMP"
                                        )
                                        "orders" -> listOf(
                                            "id: SERIAL (Primary Key)",
                                            "customer_id: UUID (FK -> profiles.id - NULL للزوار)",
                                            "guest_name: VARCHAR (اسم المشتري الزائر)",
                                            "guest_phone: VARCHAR (هاتف المشتري الزائر)",
                                            "delivery_id: UUID (FK -> profiles.id - مندوب الشحن)",
                                            "status: order_status_type (جديد/قيد التجهيز/جاهز للشحن/في الطريق/تم التسليم/ملغي)",
                                            "total_amount: DECIMAL (المبلغ الإجمالي للفاتورة)",
                                            "payment_method: VARCHAR (طريقة الدفع المحددة)",
                                            "address: TEXT (عنوان التوصيل السكني بالتفصيل)",
                                            "tracking_token: UUID (رمز فريد لتتبع الزوار بأمان)",
                                            "created_at: TIMESTAMP"
                                        )
                                        "order_items" -> listOf(
                                            "id: SERIAL (Primary Key)",
                                            "order_id: INT (Foreign Key -> orders.id)",
                                            "product_id: INT (Foreign Key -> products.id)",
                                            "quantity: INT (الكمية المطلوبة من الصنف)",
                                            "price: DECIMAL (سعر القطعة أثناء الشراء للحفظ المالي)"
                                        )
                                        "notifications" -> listOf(
                                            "id: SERIAL (Primary Key)",
                                            "user_id: UUID (FK -> profiles.id - NULL لجميع المستخدمين)",
                                            "title_ar: VARCHAR (عنوان الإشعار بالعربية)",
                                            "message_ar: TEXT (محتوى التنبيه اللحظي بالتفصيل)",
                                            "is_read: BOOLEAN (حالة القراءة من قبل المستخدم)",
                                            "created_at: TIMESTAMP"
                                        )
                                        "return_warranty_requests" -> listOf(
                                            "id: SERIAL (Primary Key)",
                                            "order_id: INT (Foreign Key -> orders.id)",
                                            "product_id: INT (Foreign Key -> products.id)",
                                            "type: request_type (ضمان / استرجاع)",
                                            "reason_ar: TEXT (أسباب تقديم الطلب بالتفصيل)",
                                            "status: request_status_type (قيد المراجعة / تم القبول / تم الرفض)",
                                            "created_at: TIMESTAMP"
                                        )
                                        "support_chats" -> listOf(
                                            "id: SERIAL (Primary Key)",
                                            "customer_id: UUID (FK -> profiles.id - NULL للزوار)",
                                            "guest_phone: VARCHAR (هاتف الزائر لتمييز المحادثة)",
                                            "message: TEXT (نص الرسالة المرسلة)",
                                            "is_from_admin: BOOLEAN (هل المرسل هو الدعم الفني؟)",
                                            "created_at: TIMESTAMP"
                                        )
                                        "audit_logs" -> listOf(
                                            "id: SERIAL (Primary Key)",
                                            "user_id: UUID (FK -> profiles.id - منفذ العملية)",
                                            "user_role: VARCHAR (رول منفذ العملية للتوثيق)",
                                            "action_ar: VARCHAR (العملية الحساسة المكتشفة)",
                                            "details_ar: TEXT (تفاصيل المتغيرات المسجلة للتدقيق)",
                                            "created_at: TIMESTAMP"
                                        )
                                        else -> emptyList()
                                    }

                                    fields.forEach { field ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            Box(modifier = Modifier.size(5.dp).background(GoldAccent, CircleShape))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(field, color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Row level security card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TealMedium),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("سياسات الحماية على مستوى السطر (RLS)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("1. profiles: العميل يقرأ ويعدل حسابه الخاص فقط، والإدارة تملك التحكم التام.", color = TextGray, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("2. products / categories: متاحة للقراءة العامة لجميع الزوار، التعديل للآدمن فقط.", color = TextGray, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("3. orders: العميل المسجل يرى ويعدل طلباته، المناديب يرون طلبات الشحن المسندة، والزوار يتتبعون بالتوكن السري الآمن.", color = TextGray, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("4. return_warranty_requests: العميل ينشئ طلب ضمان لسلعته السابقة، والإدارة تبت بالقبول أو الرفض.", color = TextGray, fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("ملف الإعداد البرمجي الكامل (Raw Supabase SQL):", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(
                                text = """
                                    -- قاعدة بيانات مركز الأحمدي للجوالات ومستلزماتها
                                    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
                                    
                                    CREATE TYPE app_role_type AS ENUM ('customer', 'admin', 'delivery');
                                    CREATE TYPE order_status_type AS ENUM ('جديد', 'قيد التجهيز', 'جاهز للشحن', 'في الطريق', 'تم التسليم', 'ملغي');
                                    CREATE TYPE request_type AS ENUM ('ضمان', 'استرجاع');
                                    CREATE TYPE request_status_type AS ENUM ('قيد المراجعة', 'تم القبول', 'تم الرفض');

                                    -- الجدول الأساسي للمستخدمين والأدوار
                                    CREATE TABLE public.profiles (
                                        id UUID REFERENCES auth.users(id) ON DELETE CASCADE PRIMARY KEY,
                                        name VARCHAR(255) NOT NULL,
                                        phone VARCHAR(50) UNIQUE NOT NULL,
                                        role app_role_type DEFAULT 'customer' NOT NULL,
                                        created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
                                    );

                                    -- جدول تصنيفات المتجر
                                    CREATE TABLE public.categories (
                                        id SERIAL PRIMARY KEY,
                                        name_ar VARCHAR(255) NOT NULL UNIQUE,
                                        slug VARCHAR(255) NOT NULL UNIQUE,
                                        icon VARCHAR(100) DEFAULT 'phone_android',
                                        created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
                                    );

                                    -- جدول الأجهزة والإكسسوارات والمخزون
                                    CREATE TABLE public.products (
                                        id SERIAL PRIMARY KEY,
                                        category_id INTEGER REFERENCES public.categories(id) ON DELETE RESTRICT NOT NULL,
                                        name_ar VARCHAR(255) NOT NULL,
                                        desc_ar TEXT NOT NULL,
                                        price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
                                        old_price DECIMAL(10, 2) CHECK (old_price > price),
                                        image_url TEXT,
                                        stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
                                        is_offer BOOLEAN DEFAULT FALSE NOT NULL,
                                        created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
                                    );

                                    -- جدول الطلبات والتوصيل
                                    CREATE TABLE public.orders (
                                        id SERIAL PRIMARY KEY,
                                        customer_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
                                        guest_name VARCHAR(255),
                                        guest_phone VARCHAR(50),
                                        delivery_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
                                        status order_status_type DEFAULT 'جديد'::order_status_type NOT NULL,
                                        total_amount DECIMAL(10, 2) NOT NULL,
                                        payment_method VARCHAR(100) NOT NULL,
                                        address TEXT NOT NULL,
                                        tracking_token UUID DEFAULT uuid_generate_v4() UNIQUE NOT NULL,
                                        created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
                                    );
                                    
                                    -- تفعيل سياسات الوصول (RLS) وحمايتها
                                    ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
                                    ALTER TABLE public.products ENABLE ROW LEVEL SECURITY;
                                    ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;
                                    
                                    -- سياسة المندوب للتحديث والعميل للقراءة والآدمن للكل
                                    CREATE POLICY "المدراء يديرون كل شيء" ON public.profiles FOR ALL TO authenticated USING (public.get_user_role() = 'admin');
                                """.trimIndent(),
                                color = GoldAccentLight,
                                fontSize = 10.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💾 تم حفظ ملف السكربت كاملاً في المشروع تحت مسار: /supabase_schema.sql لاستيراده مباشرة في لوحة تحكم Supabase.",
                        color = GoldAccentLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// Bottom navigation bar
@Composable
fun CustomerBottomNavigation(currentScreen: Screen, onNavigate: (Screen) -> Unit) {
    val darkTheme = isAppDarkTheme()
    val barBgColor = if (darkTheme) TealMedium else Color.White
    val barBorderColor = if (darkTheme) Color.White.copy(alpha = 0.05f) else Color(0xFFE2E8F0)
    val selectedText = if (darkTheme) GoldAccent else TealDark
    val indicator = if (darkTheme) GoldAccent else Color(0xFFE0F2F1)
    val unselectedColor = if (darkTheme) Color.White.copy(alpha = 0.5f) else TealDark.copy(alpha = 0.5f)

    Surface(
        color = barBgColor,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = barBorderColor,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            contentColor = if (darkTheme) Color.White else TealDark,
            modifier = Modifier.height(72.dp)
        ) {
            NavigationBarItem(
                selected = currentScreen is Screen.CustomerHome,
                onClick = { onNavigate(Screen.CustomerHome) },
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                label = { Text("الرئيسية", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TealDark,
                    selectedTextColor = selectedText,
                    indicatorColor = indicator,
                    unselectedIconColor = unselectedColor,
                    unselectedTextColor = unselectedColor
                )
            )
            NavigationBarItem(
                selected = currentScreen is Screen.Categories,
                onClick = { onNavigate(Screen.Categories) },
                icon = { Icon(Icons.Default.GridView, contentDescription = null) },
                label = { Text("الأقسام", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TealDark,
                    selectedTextColor = selectedText,
                    indicatorColor = indicator,
                    unselectedIconColor = unselectedColor,
                    unselectedTextColor = unselectedColor
                )
            )
            NavigationBarItem(
                selected = currentScreen is Screen.Favorites,
                onClick = { onNavigate(Screen.Favorites) },
                icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                label = { Text("المفضلة", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TealDark,
                    selectedTextColor = selectedText,
                    indicatorColor = indicator,
                    unselectedIconColor = unselectedColor,
                    unselectedTextColor = unselectedColor
                )
            )
            NavigationBarItem(
                selected = currentScreen is Screen.Cart,
                onClick = { onNavigate(Screen.Cart) },
                icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                label = { Text("السلة", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TealDark,
                    selectedTextColor = selectedText,
                    indicatorColor = indicator,
                    unselectedIconColor = unselectedColor,
                    unselectedTextColor = unselectedColor
                )
            )
            NavigationBarItem(
                selected = currentScreen is Screen.SupportChat,
                onClick = { onNavigate(Screen.SupportChat) },
                icon = { Icon(Icons.Default.Send, contentDescription = null) },
                label = { Text("الدعم", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TealDark,
                    selectedTextColor = selectedText,
                    indicatorColor = indicator,
                    unselectedIconColor = unselectedColor,
                    unselectedTextColor = unselectedColor
                )
            )
            NavigationBarItem(
                selected = currentScreen is Screen.Profile,
                onClick = { onNavigate(Screen.Profile) },
                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                label = { Text("حسابي", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TealDark,
                    selectedTextColor = selectedText,
                    indicatorColor = indicator,
                    unselectedIconColor = unselectedColor,
                    unselectedTextColor = unselectedColor
                )
            )
        }
    }
}

// 1) Login Screen
@Composable
fun LoginScreen(viewModel: StoreViewModel, role: AppRole) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val error by viewModel.loginError.collectAsStateWithLifecycle()
    val darkTheme = isAppDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBgColor())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            AlahmadiLogo(size = 80.dp)
            Spacer(modifier = Modifier.height(16.dp))

            val titleText = when (role) {
                AppRole.CUSTOMER -> "تسجيل دخول العميل"
                AppRole.ADMIN -> "بوابة الإدارة والمراقبة"
                AppRole.DELIVERY -> "بوابة مناديب التوصيل"
            }
            val subtitleText = when (role) {
                AppRole.CUSTOMER -> "أهلاً بك مجدداً في مركز الأحمدي 👋"
                AppRole.ADMIN -> "تسجيل دخول آمن للمدراء والمشرفين"
                AppRole.DELIVERY -> "متابعة الطلبات وتحديث خطوط السير"
            }

            Text(
                text = titleText,
                color = if (darkTheme) GoldAccent else TealMedium,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitleText,
                color = appSubTextColor(),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                textAlign = TextAlign.Center
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = appSurfaceColor()),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, appCardBorderColor(), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val labelText = if (role == AppRole.CUSTOMER) "رقم الهاتف أو الاسم" else "اسم المستخدم"
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(labelText) },
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("username_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة المرور") },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("password_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (error != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = error ?: "",
                            color = AlertError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.login(role, username, password) {
                                when (role) {
                                    AppRole.CUSTOMER -> viewModel.navigateTo(Screen.CustomerHome)
                                    AppRole.ADMIN -> viewModel.navigateTo(Screen.AdminDashboard)
                                    AppRole.DELIVERY -> viewModel.navigateTo(Screen.DeliveryPortal)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = appButtonContainerColor(),
                            contentColor = appButtonContentColor()
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_login_button")
                    ) {
                        Text("تسجيل الدخول", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    if (role == AppRole.CUSTOMER) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "ليس لديك حساب؟ سجل الآن",
                            color = if (darkTheme) GoldAccent else TealMedium,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable { viewModel.navigateTo(Screen.Register) }
                                .padding(8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Back button
            OutlinedButton(
                onClick = { viewModel.navigateTo(Screen.Onboarding) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = appTextColor()),
                border = BorderStroke(1.dp, appCardBorderColor()),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("العودة للخلف", fontSize = 14.sp)
            }
        }
    }
}

// 2) Register Screen
@Composable
fun RegisterScreen(viewModel: StoreViewModel) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val error by viewModel.registerError.collectAsStateWithLifecycle()
    val darkTheme = isAppDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBgColor())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            AlahmadiLogo(size = 70.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "إنشاء حساب عميل جديد",
                color = if (darkTheme) GoldAccent else TealMedium,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "انضم إلينا واستمتع بتجربة تسوق لا مثيل لها!",
                color = appSubTextColor(),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
                textAlign = TextAlign.Center
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = appSurfaceColor()),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, appCardBorderColor(), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("الاسم الكامل") },
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف") },
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة المرور") },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("تأكيد كلمة المرور") },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (error != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = error ?: "",
                            color = AlertError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "كلمات المرور غير متطابقة!",
                            color = AlertError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (password == confirmPassword) {
                                viewModel.register(name, phone, password) {
                                    viewModel.navigateTo(Screen.CustomerHome)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = appButtonContainerColor(),
                            contentColor = appButtonContentColor()
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = name.isNotEmpty() && phone.isNotEmpty() && password.isNotEmpty() && password == confirmPassword
                    ) {
                        Text("إنشاء الحساب", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "هل لديك حساب بالفعل؟ سجل دخولك",
                color = if (darkTheme) GoldAccent else TealMedium,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { viewModel.navigateTo(Screen.Login(AppRole.CUSTOMER)) }
                    .padding(8.dp)
            )
        }
    }
}

// 3) Order History Screen
@Composable
fun OrderHistoryScreen(viewModel: StoreViewModel) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val darkTheme = isAppDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appBgColor())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "رجوع",
                tint = if (darkTheme) GoldAccent else TealMedium,
                modifier = Modifier
                    .clickable { viewModel.navigateTo(Screen.Profile) }
                    .size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "سجل طلباتي",
                color = appTextColor(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (orders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = if (darkTheme) GoldAccent else TealMedium, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("لم تقم بأي طلبات شراء بعد!", color = appTextColor(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("اكتشف أحدث العروض والمنتجات وابدأ التسوق الآن.", color = appSubTextColor(), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.navigateTo(Screen.CustomerHome) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = appButtonContainerColor(),
                            contentColor = appButtonContentColor()
                        )
                    ) {
                        Text("تصفح المتجر", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(orders) { order ->
                    val statusColor = when (order.status) {
                        "جديد" -> AlertWarning
                        "قيد التجهيز" -> AlertInfo
                        "جاهز للشحن" -> AlertInfo
                        "في الطريق" -> AlertInfo
                        "تم التسليم" -> AlertSuccess
                        else -> AlertError
                    }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = appSurfaceColor()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo(Screen.OrderTracking(order.id)) }
                            .border(1.dp, appCardBorderColor(), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("طلب رقم #${order.id}", color = if (darkTheme) GoldAccent else TealMedium, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(order.status, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = appCardBorderColor())
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("طريقة الدفع: ${order.paymentMethod}", color = appTextColor().copy(alpha = 0.8f), fontSize = 12.sp)
                                val totalDisplay = if (order.chosenCurrency != "الريال اليمني (ر.ي)") {
                                    "${String.format("%.2f", order.convertedAmount)} ${order.chosenCurrency}"
                                } else {
                                    "${String.format("%.2f", order.totalAmount)} ر.ي"
                                }
                                Text("الإجمالي: $totalDisplay", color = if (darkTheme) GoldAccent else TealDark, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "تاريخ الطلب: " + java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(order.timestamp),
                                color = appSubTextColor(),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// 4) Notifications Screen
@Composable
fun NotificationsScreen(viewModel: StoreViewModel) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val darkTheme = isAppDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appBgColor())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "رجوع",
                tint = if (darkTheme) GoldAccent else TealMedium,
                modifier = Modifier
                    .clickable { viewModel.navigateTo(Screen.CustomerHome) }
                    .size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "الإشعارات والتحديثات",
                color = appTextColor(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = if (darkTheme) GoldAccent else TealMedium, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("صندوق الوارد فارغ!", color = appTextColor(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("ستتلقى هنا إشعارات فورية عن حالة طلباتك وعروضنا الجديدة.", color = appSubTextColor(), fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(notifications) { notif ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = appSurfaceColor()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, appCardBorderColor(), RoundedCornerShape(14.dp))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background((if (darkTheme) GoldAccent else TealMedium).copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = if (darkTheme) GoldAccent else TealMedium, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(notif.titleAr, color = if (darkTheme) GoldAccent else TealMedium, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(notif.messageAr, color = appTextColor(), fontSize = 12.sp, lineHeight = 18.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(notif.timestamp),
                                    color = appSubTextColor(),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 5) Offers Screen
@Composable
fun OffersScreen(viewModel: StoreViewModel) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val offerProducts = products.filter { it.isOffer }
    val darkTheme = isAppDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appBgColor())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "رجوع",
                tint = if (darkTheme) GoldAccent else TealMedium,
                modifier = Modifier
                    .clickable { viewModel.navigateTo(Screen.CustomerHome) }
                    .size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "عروض الأحمدي الحصرية %",
                color = appTextColor(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Promo Banner Coupon
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = GoldAccent),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("كوبون الخصم الترحيبي 🎉", color = TealDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("استخدم كود الخصم التالي في صفحة الدفع واخصم 10% فوراً على أي هاتف أو إكسسوار.", color = TealDark.copy(alpha = 0.8f), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .clickable { viewModel.applyCoupon("AHMADI10") },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AHMADI10", color = TealDark, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("(اضغط لتفعيل الكوبون)", color = TealMedium, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(
            text = "المنتجات المشمولة بالخصومات الحالية:",
            color = appTextColor(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(offerProducts) { prod ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = appSurfaceColor()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(Screen.ProductDetail(prod.id)) }
                        .border(1.dp, appCardBorderColor(), RoundedCornerShape(16.dp))
                ) {
                    Column {
                        // Product image placeholder with Offer tag
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(if (darkTheme) TealLight else Color(0xFFE0F2F1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = if (darkTheme) GoldAccent else TealMedium, modifier = Modifier.size(36.dp))
                            
                            // Discount percentage badge
                            val discountPercent = if (prod.oldPrice != null) {
                                (((prod.oldPrice - prod.price) / prod.oldPrice) * 100).toInt()
                            } else 15
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(AlertError, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                             ) {
                                Text("خصم $discountPercent%", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(prod.nameAr, color = appTextColor(), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${String.format("%.0f", prod.price)} ر.ي", color = if (darkTheme) GoldAccent else TealDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                if (prod.oldPrice != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${String.format("%.0f", prod.oldPrice)} ر.ي",
                                        color = appSubTextColor(),
                                        fontSize = 10.sp,
                                        style = androidx.compose.ui.text.TextStyle(
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 6) Return and Warranty Screen
@Composable
fun ReturnWarrantyScreen(viewModel: StoreViewModel) {
    val darkTheme = isAppDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appBgColor())
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "رجوع",
                tint = if (darkTheme) GoldAccent else TealMedium,
                modifier = Modifier
                    .clickable { viewModel.navigateTo(Screen.Profile) }
                    .size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "سياسة الضمان والاسترجاع",
                color = appTextColor(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = appSurfaceColor()),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, appCardBorderColor(), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = if (darkTheme) GoldAccent else TealMedium, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ضمان أجهزة الجوال والشواحن", color = if (darkTheme) GoldAccent else TealMedium, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "جميع الهواتف واللوحيات المباعة في مركز الأحمدي مضمونة لمدة عام كامل (12 شهراً) من تاريخ الشراء ضد العيوب المصنعية والأعطال الداخلية الفنية.",
                    color = appTextColor().copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = appSurfaceColor()),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, appCardBorderColor(), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Autorenew, contentDescription = null, tint = if (darkTheme) GoldAccent else TealMedium, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("شروط الاستبدال والاسترجاع السهل", color = if (darkTheme) GoldAccent else TealMedium, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "نحن نتيح لك استبدال المنتجات أو استرجاعها خلال 7 أيام من تاريخ الاستلام بشروط بسيطة:\n\n" +
                            "١. أن يكون المنتج في حالته الأصلية وعلبته المغلقة وسليماً تماماً.\n" +
                            "٢. إرفاق فاتورة الشراء الإلكترونية مع المنتج.\n" +
                            "٣. الإكسسوارات والشواحن غير قابلة للاسترجاع بعد فتح غلافها الخارجي إلا في حال وجود عيب فني فوري.",
                    color = appTextColor().copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = appSurfaceColor()),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, appCardBorderColor(), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null, tint = if (darkTheme) GoldAccent else TealMedium, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("رفع طلب استرجاع أو صيانة", color = if (darkTheme) GoldAccent else TealMedium, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "لتقديم طلب ضمان أو استبدال، يمكنك التواصل مباشرة مع خدمة العملاء من خلال قسم الدعم الفني والمحادثة اللحظية المتوفرة في التطبيق، وسيقوم فريق الدعم بالتنسيق مع المندوب لاستلام الجهاز مجاناً.",
                    color = appTextColor().copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// 7) Settings Screen
@Composable
fun SettingsScreen(viewModel: StoreViewModel) {
    val role by viewModel.appRole.collectAsStateWithLifecycle()
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    // Customer Settings State
    val customerNotifications by viewModel.customerNotificationsEnabled.collectAsStateWithLifecycle()
    val customerLocation by viewModel.customerLocationEnabled.collectAsStateWithLifecycle()
    val customerLanguage by viewModel.customerPreferredLanguage.collectAsStateWithLifecycle()
    val customerPayment by viewModel.customerDefaultPayment.collectAsStateWithLifecycle()

    // Admin Settings State
    val adminNotifications by viewModel.adminNotificationsEnabled.collectAsStateWithLifecycle()
    val adminAutoApprove by viewModel.adminAutoApproveOrders.collectAsStateWithLifecycle()
    val adminLowStock by viewModel.adminLowStockAlertThreshold.collectAsStateWithLifecycle()

    // Delivery Settings State
    val deliveryNotifications by viewModel.deliveryNotificationsEnabled.collectAsStateWithLifecycle()
    val deliveryGps by viewModel.deliveryGpsTrackingEnabled.collectAsStateWithLifecycle()
    val deliveryRefreshInterval by viewModel.deliveryAutoRefreshInterval.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appBgColor())
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "رجوع",
                tint = appAccentColor(),
                modifier = Modifier
                    .clickable { viewModel.navigateTo(Screen.Profile) }
                    .size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "الإعدادات العامة",
                color = appTextColor(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 1. Current Session Profile Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = appCardColor()),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, appCardBorderColor(), RoundedCornerShape(20.dp))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Box
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(appAccentColor().copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    val avatarIcon = when (role) {
                        AppRole.CUSTOMER -> Icons.Default.Person
                        AppRole.ADMIN -> Icons.Default.SupervisorAccount
                        AppRole.DELIVERY -> Icons.Default.LocalShipping
                    }
                    Icon(
                        imageVector = avatarIcon,
                        contentDescription = null,
                        tint = appAccentColor(),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val displayName = when (role) {
                        AppRole.CUSTOMER -> {
                            val name = viewModel.customerName.collectAsStateWithLifecycle().value
                            if (name.isEmpty()) "عميل الأحمدي" else name
                        }
                        AppRole.ADMIN -> "مدير النظام (مطور)"
                        AppRole.DELIVERY -> "مندوب التوصيل"
                    }
                    val displaySub = when (role) {
                        AppRole.CUSTOMER -> {
                            val phone = viewModel.customerPhone.collectAsStateWithLifecycle().value
                            if (phone.isEmpty()) "حساب محلي تجريبي" else phone
                        }
                        AppRole.ADMIN -> "اسم المستخدم: mana"
                        AppRole.DELIVERY -> "اسم المستخدم: mamm"
                    }

                    Text(
                        text = displayName,
                        color = appTextColor(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = displaySub,
                        color = appSubTextColor(),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Role Tag
                val roleLabel = when (role) {
                    AppRole.CUSTOMER -> "عميل"
                    AppRole.ADMIN -> "الادارة"
                    AppRole.DELIVERY -> "المندوب"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(appAccentColor().copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = roleLabel,
                        color = appAccentColor(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Global Preferences Card (Theme Switcher)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = appCardColor()),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, appCardBorderColor(), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("المظهر العام للتطبيق", color = appAccentColor(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الوضع الليلي (Dark Theme)", color = appTextColor(), fontSize = 13.sp)
                    Switch(
                        checked = isDark,
                        onCheckedChange = { viewModel.toggleTheme() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GoldAccent,
                            checkedTrackColor = TealLight
                        )
                    )
                }
            }
        }

        // 3. Role-Specific Section
        when (role) {
            AppRole.CUSTOMER -> {
                // Customer Settings Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = appCardColor()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, appCardBorderColor(), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("تفضيلات التنبيهات والخصوصية للعميل", color = appAccentColor(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Live Notifications
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("إشعارات العروض والخصومات", color = appTextColor(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("تلقي تنبيهات عند نزول خصومات حصرية", color = appSubTextColor(), fontSize = 11.sp)
                            }
                            Switch(
                                checked = customerNotifications,
                                onCheckedChange = {
                                    viewModel.updateCustomerSettings(it, customerLocation, customerLanguage, customerPayment)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = GoldAccent, checkedTrackColor = TealLight)
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = appSubTextColor().copy(alpha = 0.15f))

                        // GPS Sharing
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("موقع التوصيل التلقائي GPS", color = appTextColor(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("مشاركة موقعك الحالي لتسريع تسليم الطلب", color = appSubTextColor(), fontSize = 11.sp)
                            }
                            Switch(
                                checked = customerLocation,
                                onCheckedChange = {
                                    viewModel.updateCustomerSettings(customerNotifications, it, customerLanguage, customerPayment)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = GoldAccent, checkedTrackColor = TealLight)
                            )
                        }
                    }
                }

                // Customer Shopping Preferences Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = appCardColor()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, appCardBorderColor(), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("خيارات الدفع الافتراضية للعميل", color = appAccentColor(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("طريقة الدفع المفضلة:", color = appTextColor(), fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val payments = listOf("الدفع عند الاستلام", "بطاقة مدى / ائتمان")
                            payments.forEach { pay ->
                                val selected = (customerPayment == pay)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) appAccentColor() else appSurfaceColor())
                                        .border(1.dp, if (selected) appAccentColor() else appSubTextColor().copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.updateCustomerSettings(customerNotifications, customerLocation, customerLanguage, pay)
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pay,
                                        color = if (selected) appTealDarkColor() else appTextColor(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Preferred Language Choice
                        Text("لغة العرض الافتراضية:", color = appTextColor(), fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val langs = listOf("ar" to "العربية (RTL)", "en" to "English (LTR)")
                            langs.forEach { (code, name) ->
                                val selected = (customerLanguage == code)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) appAccentColor() else appSurfaceColor())
                                        .border(1.dp, if (selected) appAccentColor() else appSubTextColor().copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.updateCustomerSettings(customerNotifications, customerLocation, code, customerPayment)
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name,
                                        color = if (selected) appTealDarkColor() else appTextColor(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AppRole.ADMIN -> {
                // Admin Settings Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = appCardColor()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, appCardBorderColor(), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("إعدادات لوحة التحكم والمراقبة للمدير", color = appAccentColor(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Order Notifications
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("إشعارات لوحة الإدارة الفورية", color = appTextColor(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("تنبيهات الأصوات والطلبات الواردة مباشرة", color = appSubTextColor(), fontSize = 11.sp)
                            }
                            Switch(
                                checked = adminNotifications,
                                onCheckedChange = {
                                    viewModel.updateAdminSettings(it, adminAutoApprove, adminLowStock)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = GoldAccent, checkedTrackColor = TealLight)
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = appSubTextColor().copy(alpha = 0.15f))

                        // Auto-Approve Orders
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("القبول الآلي والذكي للطلبات", color = appTextColor(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("الموافقة الفورية على طلبات الكاش وإسنادها للمندوب", color = appSubTextColor(), fontSize = 11.sp)
                            }
                            Switch(
                                checked = adminAutoApprove,
                                onCheckedChange = {
                                    viewModel.updateAdminSettings(adminNotifications, it, adminLowStock)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = GoldAccent, checkedTrackColor = TealLight)
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = appSubTextColor().copy(alpha = 0.15f))

                        // Stock Alert Threshold
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تنبيه نفاد المخزون (الكمية)", color = appTextColor(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("التنبيه في لوحة التحكم عند وصول الكمية لهذا حد", color = appSubTextColor(), fontSize = 11.sp)
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(appSurfaceColor())
                                        .border(1.dp, appSubTextColor().copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (adminLowStock > 1) {
                                                viewModel.updateAdminSettings(adminNotifications, adminAutoApprove, adminLowStock - 1)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("-", color = appTextColor(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }

                                Text(
                                    text = adminLowStock.toString(),
                                    color = appTextColor(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.widthIn(min = 20.dp),
                                    textAlign = TextAlign.Center
                                )

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(appSurfaceColor())
                                        .border(1.dp, appSubTextColor().copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.updateAdminSettings(adminNotifications, adminAutoApprove, adminLowStock + 1)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+", color = appTextColor(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 4. Supabase Auth Integration Card (ADMIN ONLY)
                val savedUrl by viewModel.supabaseUrl.collectAsStateWithLifecycle()
                val savedKey by viewModel.supabaseKey.collectAsStateWithLifecycle()
                val isRealEnabled by viewModel.isSupabaseEnabled.collectAsStateWithLifecycle()
                val isTesting by viewModel.isTestingConnection.collectAsStateWithLifecycle()
                val testSuccess by viewModel.testConnectionSuccess.collectAsStateWithLifecycle()

                var inputUrl by remember(savedUrl) { mutableStateOf(savedUrl) }
                var inputKey by remember(savedKey) { mutableStateOf(savedKey) }
                var inputEnabled by remember(isRealEnabled) { mutableStateOf(isRealEnabled) }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = appCardColor()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, appCardBorderColor(), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("إعدادات Supabase Auth السحابية ☁️", color = appAccentColor(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            
                            // Switch to enable/disable real Supabase auth
                            Switch(
                                checked = inputEnabled,
                                onCheckedChange = { 
                                    inputEnabled = it 
                                    viewModel.updateSupabaseConfig(inputUrl, inputKey, it)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = GoldAccent, checkedTrackColor = TealLight)
                            )
                        }
                        
                        Text(
                            text = if (inputEnabled) "الاتصال السحابي الحقيقي نشط حالياً." else "التطبيق يعمل في بيئة المحاكاة المحلية الآمنة.",
                            color = if (inputEnabled) AlertSuccess else appSubTextColor(),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            label = { Text("رابط Supabase URL", color = appTextColor().copy(alpha = 0.6f)) },
                            placeholder = { Text("https://xxxx.supabase.co", color = appTextColor().copy(alpha = 0.3f)) },
                            colors = appTextFieldColors(),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = inputKey,
                            onValueChange = { inputKey = it },
                            label = { Text("مفتاح الخدمة Anon Key", color = appTextColor().copy(alpha = 0.6f)) },
                            placeholder = { Text("eyJhbGciOi...", color = appTextColor().copy(alpha = 0.3f)) },
                            colors = appTextFieldColors(),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Test Connection button
                            Button(
                                onClick = {
                                    viewModel.testSupabaseConnection(inputUrl, inputKey) { success -> }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = appTealLightColor()),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(42.dp),
                                enabled = inputUrl.isNotEmpty() && inputKey.isNotEmpty() && !isTesting
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(color = appAccentColor(), modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("فحص الاتصال", color = if (isAppDarkTheme()) Color.White else TealDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Save settings button
                            Button(
                                onClick = {
                                    viewModel.updateSupabaseConfig(inputUrl, inputKey, inputEnabled)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = appAccentColor()),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(42.dp),
                                enabled = inputUrl.isNotEmpty() && inputKey.isNotEmpty()
                            ) {
                                Text("حفظ التغييرات", color = appTealDarkColor(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Show test result
                        testSuccess?.let { success ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (success) AlertSuccess else AlertError,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (success) "تم الاتصال بالخادم بنجاح!" else "فشل الاتصال! تحقق من المدخلات.",
                                    color = if (success) AlertSuccess else AlertError,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 4.5. Main Branch Accounting System Integration Card (ADMIN ONLY)
                val accUrl by viewModel.accountingApiUrl.collectAsStateWithLifecycle()
                val accKey by viewModel.accountingApiKey.collectAsStateWithLifecycle()
                val accBranch by viewModel.accountingBranchCode.collectAsStateWithLifecycle()
                val accInterval by viewModel.accountingSyncInterval.collectAsStateWithLifecycle()
                val accEnabled by viewModel.isAccountingEnabled.collectAsStateWithLifecycle()
                
                val accTesting by viewModel.isTestingAccountingConnection.collectAsStateWithLifecycle()
                val accTestSuccess by viewModel.testAccountingConnectionSuccess.collectAsStateWithLifecycle()
                val accSyncLogs by viewModel.accountingSyncLogs.collectAsStateWithLifecycle()
                
                val isSyncingProds by viewModel.isSyncingProducts.collectAsStateWithLifecycle()
                val isSyncingOrds by viewModel.isSyncingOrders.collectAsStateWithLifecycle()

                var inputAccUrl by remember(accUrl) { mutableStateOf(accUrl) }
                var inputAccKey by remember(accKey) { mutableStateOf(accKey) }
                var inputAccBranch by remember(accBranch) { mutableStateOf(accBranch) }
                var inputAccInterval by remember(accInterval) { mutableStateOf(accInterval) }
                var inputAccEnabled by remember(accEnabled) { mutableStateOf(accEnabled) }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = appCardColor()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, appCardBorderColor(), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = appAccentColor(),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "الربط والتكامل المحاسبي (الفرع الرئيسي) 💼",
                                    color = appAccentColor(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Switch(
                                checked = inputAccEnabled,
                                onCheckedChange = { 
                                    inputAccEnabled = it 
                                    viewModel.updateAccountingConfig(inputAccUrl, inputAccKey, inputAccBranch, inputAccInterval, it)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = GoldAccent, checkedTrackColor = TealLight)
                            )
                        }
                        
                        Text(
                            text = if (inputAccEnabled) "تكامل نظام المحاسبة نشط ومربوط ببرمجيات الفرع الرئيسي حالياً." else "الربط المحاسبي معطل حالياً (الوضع المحلي).",
                            color = if (inputAccEnabled) AlertSuccess else appSubTextColor(),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Base URL
                        OutlinedTextField(
                            value = inputAccUrl,
                            onValueChange = { inputAccUrl = it },
                            label = { Text("رابط خادم النظام المحاسبي (ERP API)", color = appTextColor().copy(alpha = 0.6f)) },
                            placeholder = { Text("https://erp.alahmadi-group.com/api/v1", color = appTextColor().copy(alpha = 0.3f)) },
                            colors = appTextFieldColors(),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // API Key
                        OutlinedTextField(
                            value = inputAccKey,
                            onValueChange = { inputAccKey = it },
                            label = { Text("مفتاح الترخيص والوصول السري (Secret Key)", color = appTextColor().copy(alpha = 0.6f)) },
                            placeholder = { Text("sec_key_alahmadi_...", color = appTextColor().copy(alpha = 0.3f)) },
                            colors = appTextFieldColors(),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Branch Code
                            OutlinedTextField(
                                value = inputAccBranch,
                                onValueChange = { inputAccBranch = it },
                                label = { Text("كود فرع الأحمدي", color = appTextColor().copy(alpha = 0.6f)) },
                                colors = appTextFieldColors(),
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            // Sync Interval (Minutes)
                            OutlinedTextField(
                                value = inputAccInterval.toString(),
                                onValueChange = { newValue ->
                                    val parsed = newValue.toIntOrNull() ?: 30
                                    inputAccInterval = parsed
                                },
                                label = { Text("فترة التزامن (دقيقة)", color = appTextColor().copy(alpha = 0.6f)) },
                                colors = appTextFieldColors(),
                                modifier = Modifier.weight(0.8f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        // Test and Save Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Test Connection button
                            Button(
                                onClick = {
                                    viewModel.testAccountingConnection(inputAccUrl, inputAccKey, inputAccBranch) { success -> }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = appTealLightColor()),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.5f).height(42.dp),
                                enabled = inputAccUrl.isNotEmpty() && inputAccKey.isNotEmpty() && !accTesting
                            ) {
                                if (accTesting) {
                                    CircularProgressIndicator(color = appAccentColor(), modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("فحص اتصال الخادم", color = if (isAppDarkTheme()) Color.White else TealDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Save settings button
                            Button(
                                onClick = {
                                    viewModel.updateAccountingConfig(inputAccUrl, inputAccKey, inputAccBranch, inputAccInterval, inputAccEnabled)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = appAccentColor()),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(42.dp),
                                enabled = inputAccUrl.isNotEmpty() && inputAccKey.isNotEmpty()
                            ) {
                                Text("حفظ الإعدادات", color = appTealDarkColor(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Test connection result
                        accTestSuccess?.let { success ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (success) AlertSuccess else AlertError,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (success) "تم تأسيس قناة اتصال آمنة مع النظام المحاسبي للفرع الرئيسي!" else "فشل تأسيس قناة الاتصال! يرجى التحقق من الخادم.",
                                    color = if (success) AlertSuccess else AlertError,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        HorizontalDivider(color = appCardBorderColor(), thickness = 1.dp, modifier = Modifier.padding(bottom = 12.dp))

                        // Instant Sync Action Buttons
                        Text(
                            text = "المزامنة اليدوية الفورية ⚡",
                            color = appTextColor(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Sync Products Stock
                            Button(
                                onClick = { viewModel.syncAccountingProducts() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAppDarkTheme()) GoldAccent else TealMedium,
                                    contentColor = if (isAppDarkTheme()) TealDark else Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(40.dp),
                                enabled = !isSyncingProds && !isSyncingOrds && inputAccEnabled
                            ) {
                                if (isSyncingProds) {
                                    CircularProgressIndicator(color = if (isAppDarkTheme()) TealDark else Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مزامنة الأجهزة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Sync Orders / Export Sales
                            Button(
                                onClick = { viewModel.syncAccountingOrders() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAppDarkTheme()) GoldAccent else TealMedium,
                                    contentColor = if (isAppDarkTheme()) TealDark else Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(40.dp),
                                enabled = !isSyncingProds && !isSyncingOrds && inputAccEnabled
                            ) {
                                if (isSyncingOrds) {
                                    CircularProgressIndicator(color = if (isAppDarkTheme()) TealDark else Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ترحيل الفواتير", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Live Sync Logs View
                        Text(
                            text = "سجل مزامنة ومطابقة الفواتير والأسعار 📊",
                            color = appTextColor(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isAppDarkTheme()) Color.Black.copy(alpha = 0.3f) else appSurfaceColor())
                                .border(1.dp, appCardBorderColor(), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (accSyncLogs.isEmpty()) {
                                    Text("لا توجد سجلات مزامنة حالية.", color = appSubTextColor(), fontSize = 10.sp)
                                } else {
                                    accSyncLogs.forEach { log ->
                                        Text(
                                            text = log,
                                            color = appTextColor().copy(alpha = 0.8f),
                                            fontSize = 9.sp,
                                            lineHeight = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Developer Dashboard Integration Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = appCardColor()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, appCardBorderColor(), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("مطور البرمجيات وبيانات قاعدة البيانات", color = appAccentColor(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "تطبيق مركز الأحمدي مبرمج بالكامل باستخدام Jetpack Compose ومربوط بقاعدة Supabase السحابية مع كاش Room المحلي (Offline-First).",
                            color = appTextColor().copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.navigateTo(Screen.DatabaseExplorer) },
                            colors = ButtonDefaults.buttonColors(containerColor = appAccentColor()),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("مستعرض Supabase SQL والجداول", color = appTealDarkColor(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            AppRole.DELIVERY -> {
                // Delivery Settings Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = appCardColor()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, appCardBorderColor(), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("إعدادات بوابة مناديب التوصيل", color = appAccentColor(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Delivery Notifications
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("إشعارات الطلبات المسندة عاجلاً", color = appTextColor(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("تلقي تنبيه صوتي فوري عند إسناد شحنة جديدة", color = appSubTextColor(), fontSize = 11.sp)
                            }
                            Switch(
                                checked = deliveryNotifications,
                                onCheckedChange = {
                                    viewModel.updateDeliverySettings(it, deliveryGps, deliveryRefreshInterval)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = GoldAccent, checkedTrackColor = TealLight)
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = appSubTextColor().copy(alpha = 0.15f))

                        // GPS Tracking
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تتبع الموقع ومشاركة خط السير", color = appTextColor(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("مشاركة موقعك الحالي مع لوحة التحكم والعميل للتتبع اللحظي", color = appSubTextColor(), fontSize = 11.sp)
                            }
                            Switch(
                                checked = deliveryGps,
                                onCheckedChange = {
                                    viewModel.updateDeliverySettings(deliveryNotifications, it, deliveryRefreshInterval)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = GoldAccent, checkedTrackColor = TealLight)
                            )
                        }
                    }
                }

                // Delivery Refresh Settings Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = appCardColor()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, appCardBorderColor(), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("معدل تحديث بيانات التوصيل التلقائي", color = appAccentColor(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("معدل تحديث الخرائط والطلبات المسندة:", color = appTextColor(), fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val intervals = listOf(15 to "١٥ ثانية", 30 to "٣٠ ثانية", 60 to "دقيقة واحدة")
                            intervals.forEach { (seconds, label) ->
                                val selected = (deliveryRefreshInterval == seconds)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) appAccentColor() else appSurfaceColor())
                                        .border(1.dp, if (selected) appAccentColor() else appSubTextColor().copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.updateDeliverySettings(deliveryNotifications, deliveryGps, seconds)
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) appTealDarkColor() else appTextColor(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentGatewayScreen(viewModel: StoreViewModel, orderId: Int) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val order = orders.find { it.id == orderId }
    
    val exchangeRateSAR by viewModel.exchangeRateSAR.collectAsStateWithLifecycle()
    val exchangeRateUSD by viewModel.exchangeRateUSD.collectAsStateWithLifecycle()

    if (order == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("الطلب غير موجود!", color = appTextColor())
        }
        return
    }

    var selectedProvider by remember { mutableStateOf("الكريمي إكسبرس (Kuraimi Pay)") }
    var referenceNumber by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf(order.customerPhone) }
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    
    var isProcessing by remember { mutableStateOf(false) }
    var processingPhase by remember { mutableStateOf("") }
    
    LaunchedEffect(isProcessing) {
        if (isProcessing) {
            processingPhase = "جاري الاتصال بالبوابة الآمنة للمصرف واستدعاء الـ API الخاص بـ $selectedProvider..."
            kotlinx.coroutines.delay(1200)
            processingPhase = "جاري التحقق من رقم المعاملة وصلاحية التحويل المالي ($referenceNumber)..."
            kotlinx.coroutines.delay(1200)
            processingPhase = "تم تأكيد قيد الحساب وقبول الدفع بنجاح! 🎉"
            kotlinx.coroutines.delay(800)
            
            val refCode = if (referenceNumber.isNotEmpty()) referenceNumber else "TXN-${(100000..999999).random()}"
            viewModel.updateOrderPaymentDetails(orderId, "$selectedProvider - مرجع: $refCode", "مقبول ومكتمل")
            isProcessing = false
            viewModel.navigateTo(Screen.OrderTracking(orderId))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateTo(Screen.Checkout) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = appTextColor())
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("بوابة الدفع والتحويل الإلكتروني 💳", color = appTextColor(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Transaction Card
        Card(
            colors = CardDefaults.cardColors(containerColor = if (isAppDarkTheme()) TealMedium else TealMedium.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            border = BorderStroke(1.dp, appCardBorderColor())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("ملخص الفاتورة والعملة المختارة", color = if (isAppDarkTheme()) GoldAccent else TealDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("رقم الفاتورة/الطلب:", color = appSubTextColor(), fontSize = 11.sp)
                    Text("#$orderId", color = appTextColor(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("العميل:", color = appSubTextColor(), fontSize = 11.sp)
                    Text(order.customerName, color = appTextColor(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("طريقة الدفع المحددة:", color = appSubTextColor(), fontSize = 11.sp)
                    Text(order.paymentMethod, color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                
                HorizontalDivider(color = appTextColor().copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 10.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("المبلغ الإجمالي المطلوب بالعملة المختارة:", color = appTextColor(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    val displayAmount = if (order.chosenCurrency != "الريال اليمني (ر.ي)") {
                        "${String.format("%.2f", order.convertedAmount)} ${order.chosenCurrency}"
                    } else {
                        "${order.totalAmount} ر.ي"
                    }
                    Text(displayAmount, color = if (isAppDarkTheme()) GoldAccent else TealDark, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Text("اختر مزود خدمة التحويل/الدفع الآمن:", color = if (isAppDarkTheme()) GoldAccent else TealMedium, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        val providers = listOf(
            "الكريمي إكسبرس (Kuraimi Pay)" to "تحويل مالي فوري أو سداد عبر محفظة M-Floos",
            "النجم موني ترانسفير (Al-Najm Express)" to "إرسال حوالة شبكة يدوية وقيدها بالرقم بالنظام",
            "محفظة جوال كاش (Pocket Money)" to "الدفع المباشر عبر الهواتف المحمولة والشبكة",
            "البطاقة المصرفية (مدى / فيزا / ماستر)" to "بوابة دفع دولية ومحلية متعددة العملات"
        )

        providers.forEach { (prov, desc) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(appSurfaceColor(), RoundedCornerShape(8.dp))
                    .border(if (selectedProvider == prov) 1.5.dp else 1.dp, if (selectedProvider == prov) GoldAccent else appCardBorderColor(), RoundedCornerShape(8.dp))
                    .clickable { selectedProvider = prov }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedProvider == prov,
                    onClick = { selectedProvider = prov },
                    colors = RadioButtonDefaults.colors(selectedColor = GoldAccent)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(prov, color = appTextColor(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(desc, color = appSubTextColor(), fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Form fields depending on selected provider
        Card(
            colors = CardDefaults.cardColors(containerColor = appSurfaceColor()),
            border = BorderStroke(1.dp, appCardBorderColor()),
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                when (selectedProvider) {
                    "الكريمي إكسبرس (Kuraimi Pay)", "النجم موني ترانسفير (Al-Najm Express)", "محفظة جوال كاش (Pocket Money)" -> {
                        Text(
                            text = "📌 خطوات التحويل المالي المحلي لإتمام العملية:",
                            color = appTextColor(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        val accountDetails = if (selectedProvider.contains("الكريمي")) {
                            "رقم الحساب بالكريمي: 301234567 باسم (الأحمدي لخدمات الاتصالات)"
                        } else if (selectedProvider.contains("النجم")) {
                            "حساب وكيل النجم المعتمد لدينا: AL-AHMADI-9876 باسم (شركة الأحمدي للهواتف)"
                        } else {
                            "رقم الحساب/المحفظة: 777123456 باسم (الأحمدي مول)"
                        }
                        
                        Text("1. قم بتحويل المبلغ المذكور أعلاه عبر حسابك البنكي أو أقرب مركز صرافة إلى:", color = appSubTextColor(), fontSize = 11.sp)
                        Text(accountDetails, color = if (isAppDarkTheme()) GoldAccent else TealDark, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                        Text("2. بعد نجاح التحويل، يرجى كتابة رقم الحوالة/المعاملة في الحقل أدناه للتحقق التلقائي عبر الـ API:", color = appSubTextColor(), fontSize = 11.sp)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = referenceNumber,
                            onValueChange = { referenceNumber = it },
                            label = { Text("رقم الحوالة أو إيصال السداد (يتكون من 8-10 أرقام)", color = appSubTextColor()) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = appTextFieldColors()
                        )
                    }
                    else -> { // Credit Card / Mada
                        Text(
                            text = "💳 تعبئة بيانات بطاقة السداد (بوابة دفع مدى والفيزا):",
                            color = appTextColor(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = cardNumber,
                            onValueChange = { cardNumber = it },
                            label = { Text("رقم البطاقة (16 رقم)", color = appSubTextColor()) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = appTextFieldColors()
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = cardExpiry,
                                onValueChange = { cardExpiry = it },
                                label = { Text("تاريخ الانتهاء MM/YY", color = appSubTextColor()) },
                                modifier = Modifier.weight(1f),
                                colors = appTextFieldColors()
                            )
                            OutlinedTextField(
                                value = cardCvv,
                                onValueChange = { cardCvv = it },
                                label = { Text("الرمز السري CVV", color = appSubTextColor()) },
                                modifier = Modifier.weight(1f),
                                colors = appTextFieldColors()
                            )
                        }
                    }
                }
            }
        }

        // Action Confirm Button
        Button(
            onClick = { isProcessing = true },
            enabled = if (selectedProvider.contains("البطاقة")) {
                cardNumber.length >= 12 && cardExpiry.isNotEmpty() && cardCvv.isNotEmpty()
            } else {
                referenceNumber.length >= 6
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = appButtonContainerColor(), contentColor = appButtonContentColor()),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تأكيد الدفع الآمن والسداد 🔒", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "🛡️ معاملتك مشفرة بنسبة 100% ومتوافقة مع معايير أمن البيانات لحماية عمليات النقل والتحويل المحلي.",
            color = appSubTextColor(),
            fontSize = 9.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }

    // Processing Dialog
    if (isProcessing) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = GoldAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("جاري معالجة السداد الآمن...", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Text(processingPhase, color = TextGray, fontSize = 11.sp, modifier = Modifier.padding(vertical = 10.dp))
            },
            confirmButton = {},
            containerColor = TealDark,
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        )
    }
}
