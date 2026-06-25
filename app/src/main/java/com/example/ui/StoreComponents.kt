package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.ui.theme.*
import com.example.viewmodel.AppRole

@Composable
fun AlahmadiLogo(modifier: Modifier = Modifier, size: Dp = 80.dp) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(TealLight, TealDark)
                    ),
                    shape = CircleShape
                )
                .border(2.dp, GoldAccent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                val w = size.toPx()
                val h = size.toPx()

                // 1. Phone frame silhouette in center
                val phoneW = this.size.width * 0.35f
                val phoneH = this.size.height * 0.58f
                val phoneX = this.size.width * 0.38f
                val phoneY = this.size.height * 0.20f
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(phoneX, phoneY),
                    size = Size(phoneW, phoneH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                    style = Stroke(width = 4f)
                )

                // Phone Notch
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(phoneX + phoneW * 0.25f, phoneY + phoneH * 0.04f),
                    size = Size(phoneW * 0.5f, phoneH * 0.06f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f),
                    style = androidx.compose.ui.graphics.drawscope.Fill
                )

                // 2. Screwdriver on the left side
                val sdX = this.size.width * 0.22f
                val sdY = this.size.height * 0.30f
                // Handle
                drawRoundRect(
                    color = GoldAccent,
                    topLeft = Offset(sdX - 4f, sdY),
                    size = Size(10f, h * 0.12f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f),
                    style = androidx.compose.ui.graphics.drawscope.Fill
                )
                // Shaft
                drawLine(
                    color = GoldAccent,
                    start = Offset(sdX, sdY + h * 0.12f),
                    end = Offset(sdX, sdY + h * 0.24f),
                    strokeWidth = 3f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                // Head
                drawLine(
                    color = GoldAccent,
                    start = Offset(sdX - 3f, sdY + h * 0.24f),
                    end = Offset(sdX + 3f, sdY + h * 0.24f),
                    strokeWidth = 3f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Square
                )

                // 3. Gear outline at the bottom left
                val gearX = this.size.width * 0.22f
                val gearY = this.size.height * 0.64f
                val gearRadius = this.size.width * 0.08f
                drawCircle(
                    color = GoldAccent,
                    radius = gearRadius,
                    center = Offset(gearX, gearY),
                    style = Stroke(width = 3f)
                )
                for (angle in 0 until 360 step 45) {
                    val rad = Math.toRadians(angle.toDouble())
                    val startDist = gearRadius
                    val endDist = gearRadius + 4f
                    val sX = (gearX + Math.cos(rad) * startDist).toFloat()
                    val sY = (gearY + Math.sin(rad) * startDist).toFloat()
                    val eX = (gearX + Math.cos(rad) * endDist).toFloat()
                    val eY = (gearY + Math.sin(rad) * endDist).toFloat()
                    drawLine(
                        color = GoldAccent,
                        start = Offset(sX, sY),
                        end = Offset(eX, eY),
                        strokeWidth = 3f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }

                // 4. Dual arches on the right wrapping around
                // Outer white arc
                val outerArcPath = androidx.compose.ui.graphics.Path().apply {
                    val startAngle = -65f
                    val sweepAngle = 130f
                    addArc(
                        oval = androidx.compose.ui.geometry.Rect(
                            left = this@Canvas.size.width * 0.10f,
                            top = this@Canvas.size.height * 0.10f,
                            right = this@Canvas.size.width * 0.90f,
                            bottom = this@Canvas.size.height * 0.90f
                        ),
                        startAngleDegrees = startAngle,
                        sweepAngleDegrees = sweepAngle
                    )
                }
                drawPath(
                    path = outerArcPath,
                    color = Color.White,
                    style = Stroke(width = 4f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )

                // Inner golden yellow arc
                val innerArcPath = androidx.compose.ui.graphics.Path().apply {
                    val startAngle = -50f
                    val sweepAngle = 100f
                    addArc(
                        oval = androidx.compose.ui.geometry.Rect(
                            left = this@Canvas.size.width * 0.22f,
                            top = this@Canvas.size.height * 0.22f,
                            right = this@Canvas.size.width * 0.78f,
                            bottom = this@Canvas.size.height * 0.78f
                        ),
                        startAngleDegrees = startAngle,
                        sweepAngleDegrees = sweepAngle
                    )
                }
                drawPath(
                    path = innerArcPath,
                    color = GoldAccent,
                    style = Stroke(width = 3f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "الأحـمـدي",
            color = GoldAccent,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.16f).sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RoleSelectorTopBar(
    currentRole: AppRole,
    onRoleSelected: (AppRole) -> Unit,
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp)),
        colors = CardDefaults.cardColors(containerColor = TealMedium),
        shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .windowInsetsPadding(WindowInsets.statusBars),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Right Side: Store Logo and Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onRoleSelected(AppRole.CUSTOMER) }
            ) {
                AlahmadiLogo(size = 38.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "مركز الأحمدي",
                        color = GoldAccent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "للجوالات ومستلزماتها",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Left Side: Theme Toggle + Menu Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Theme Toggle Button
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier.testTag("theme_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.NightsStay,
                        contentDescription = if (isDarkTheme) "الوضع النهاري" else "الوضع الليلي",
                        tint = GoldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Menu Button
                Box {
                    IconButton(
                        onClick = { menuExpanded = !menuExpanded },
                        modifier = Modifier.testTag("menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "القائمة الرئيسية",
                            tint = GoldAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Dropdown menu for role/portal selection
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier
                            .background(TealMedium)
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    ) {
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = "بوابة العميل", 
                                    color = if (currentRole == AppRole.CUSTOMER) GoldAccent else Color.White, 
                                    fontSize = 13.sp, 
                                    fontWeight = FontWeight.Bold
                                ) 
                            },
                            onClick = {
                                menuExpanded = false
                                onRoleSelected(AppRole.CUSTOMER)
                            },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.Home, 
                                    contentDescription = null, 
                                    tint = if (currentRole == AppRole.CUSTOMER) GoldAccent else TextGray
                                ) 
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = Color.White,
                                leadingIconColor = TextGray
                            )
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = "لوحة الإدارة", 
                                    color = if (currentRole == AppRole.ADMIN) GoldAccent else Color.White, 
                                    fontSize = 13.sp, 
                                    fontWeight = FontWeight.Bold
                                ) 
                            },
                            onClick = {
                                menuExpanded = false
                                onRoleSelected(AppRole.ADMIN)
                            },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.SupervisorAccount, 
                                    contentDescription = null, 
                                    tint = if (currentRole == AppRole.ADMIN) GoldAccent else TextGray
                                ) 
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = Color.White,
                                leadingIconColor = TextGray
                            )
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = "لوحة المناديب", 
                                    color = if (currentRole == AppRole.DELIVERY) GoldAccent else Color.White, 
                                    fontSize = 13.sp, 
                                    fontWeight = FontWeight.Bold
                                ) 
                            },
                            onClick = {
                                menuExpanded = false
                                onRoleSelected(AppRole.DELIVERY)
                            },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.LocalShipping, 
                                    contentDescription = null, 
                                    tint = if (currentRole == AppRole.DELIVERY) GoldAccent else TextGray
                                ) 
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = Color.White,
                                leadingIconColor = TextGray
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RatingBar(rating: Float, modifier: Modifier = Modifier, darkTheme: Boolean = false) {
    val starTint = if (isAppDarkTheme()) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f)
    Row(modifier = modifier) {
        repeat(5) { index ->
            val filled = index < rating.toInt()
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (filled) GoldAccent else starTint,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun ProductGridCard(
    product: Product,
    isFavorite: Boolean,
    onProductClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardColor = appCardColor()
    val borderColor = appCardBorderColor()
    val textColor = appTextColor()
    val subTextColor = appSubTextColor()
    val btnColor = appButtonContainerColor()
    val btnContentColor = appButtonContentColor()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(if (isAppDarkTheme()) 2.dp else 4.dp, RoundedCornerShape(24.dp))
            .clickable { onProductClick() }
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(24.dp),
        border = if (isAppDarkTheme()) null else BorderStroke(1.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Product Image Mock container using vector layouts
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(105.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isAppDarkTheme()) {
                                    listOf(TealDark, TealMedium)
                                } else {
                                    listOf(Color(0xFFF2F6F7), Color(0xFFE5ECEE))
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconForProduct(product.id),
                        contentDescription = null,
                        tint = if (isAppDarkTheme()) GoldAccent.copy(alpha = 0.3f) else TealDark.copy(alpha = 0.15f),
                        modifier = Modifier.size(48.dp)
                    )
                    
                    if (product.oldPrice != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(AlertError, RoundedCornerShape(bottomStart = 10.dp, topEnd = 0.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            val percent = (((product.oldPrice - product.price) / product.oldPrice) * 100).toInt()
                            Text(
                                text = "خصم %$percent",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                // Category Tag
                Text(
                    text = when (product.categoryId) {
                        1 -> "الهواتف الذكية"
                        2 -> "الشواحن والمنصات"
                        3 -> "السماعات والصوتيات"
                        4 -> "الكابلات والتوصيلات"
                        else -> "كفرات وحماية"
                    },
                    color = subTextColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Title
                Text(
                    text = product.nameAr,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))
                
                RatingBar(rating = product.rating, darkTheme = isAppDarkTheme())

                Spacer(modifier = Modifier.height(10.dp))

                // Pricing and Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${product.price} ر.س",
                            color = if (isAppDarkTheme()) GoldAccent else TealDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                        if (product.oldPrice != null) {
                            Text(
                                text = "${product.oldPrice} ر.س",
                                color = subTextColor.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                style = LocalTextStyle.current.copy(
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = { onAddToCart() },
                        modifier = Modifier
                            .size(32.dp)
                            .background(btnColor, RoundedCornerShape(10.dp))
                            .testTag("add_to_cart_btn_${product.id}"),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = btnContentColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "إضافة للسلة",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Favorite Button Overlay
            IconButton(
                onClick = { onFavoriteToggle() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .testTag("favorite_toggle_${product.id}")
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "المفضلة",
                    tint = if (isFavorite) AlertError else subTextColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (color, text) = when (status) {
        "جديد" -> AlertWarning to "جديد"
        "قيد التجهيز" -> AlertWarning to "قيد التجهيز"
        "جاهز للشحن" -> AlertInfo to "جاهز للشحن"
        "في الطريق" -> AlertInfo to "في الطريق"
        "تم التسليم" -> AlertSuccess to "تم التسليم"
        else -> AlertError to "ملغي"
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

fun getIconForProduct(productId: Int): ImageVector {
    return when (productId) {
        101 -> Icons.Default.Settings // iPhone (representing settings-gear or custom) -> custom render
        102 -> Icons.Default.Settings // Galaxy Ultra
        103 -> Icons.Default.PlayArrow // AirPods
        104 -> Icons.Default.Add // Anker Nano
        105 -> Icons.Default.Add // Baseus GaN
        106 -> Icons.Default.PlayArrow // JBL Tune
        107 -> Icons.Default.Refresh // Baseus Cable
        108 -> Icons.Default.Check // Glass screen
        else -> Icons.Default.Settings
    }
}

@Composable
fun ShimmerLoaderCard(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = alpha)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun isAppDarkTheme(): Boolean {
    return MaterialTheme.colorScheme.background == TealDark
}

@Composable
fun appBgColor(): Color {
    return MaterialTheme.colorScheme.background
}

@Composable
fun appSurfaceColor(): Color {
    return if (isAppDarkTheme()) TealMedium else Color.White
}

@Composable
fun appCardColor(): Color {
    return if (isAppDarkTheme()) CardBackground else Color.White
}

@Composable
fun appCardBorderColor(): Color {
    return if (isAppDarkTheme()) Color.Transparent else Color(0xFFE2E8F0)
}

@Composable
fun appTextColor(): Color {
    return if (isAppDarkTheme()) Color.White else TealDark
}

@Composable
fun appSubTextColor(): Color {
    return if (isAppDarkTheme()) TextGray else Color(0xFF5A737A)
}

@Composable
fun appAccentColor(): Color {
    return if (isAppDarkTheme()) GoldAccent else TealLight
}

@Composable
fun appAccentGoldColor(): Color {
    return GoldAccent
}

@Composable
fun appTealLightColor(): Color {
    return if (isAppDarkTheme()) TealLight else Color(0xFFE0F2F1)
}

@Composable
fun appTealDarkColor(): Color {
    return if (isAppDarkTheme()) TealDark else TealDark
}

@Composable
fun appButtonContainerColor(): Color {
    return if (isAppDarkTheme()) GoldAccent else TealMedium
}

@Composable
fun appButtonContentColor(): Color {
    return if (isAppDarkTheme()) TealDark else Color.White
}

@Composable
fun appPillBgColor(): Color {
    return if (isAppDarkTheme()) TealLight else Color(0xFFECEFF1)
}

@Composable
fun appPillTextColor(): Color {
    return if (isAppDarkTheme()) Color.White else TealDark
}

@Composable
fun appTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = appTextColor(),
    unfocusedTextColor = appTextColor(),
    focusedPlaceholderColor = appSubTextColor(),
    unfocusedPlaceholderColor = appSubTextColor(),
    focusedBorderColor = if (isAppDarkTheme()) GoldAccent else TealMedium,
    unfocusedBorderColor = if (isAppDarkTheme()) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.15f),
    focusedContainerColor = if (isAppDarkTheme()) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f),
    unfocusedContainerColor = if (isAppDarkTheme()) Color.White.copy(alpha = 0.02f) else Color.Black.copy(alpha = 0.01f),
    focusedLabelColor = if (isAppDarkTheme()) GoldAccent else TealMedium,
    unfocusedLabelColor = appSubTextColor()
)

