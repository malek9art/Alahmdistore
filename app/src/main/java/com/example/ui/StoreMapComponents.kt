package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.viewmodel.StoreViewModel
import kotlin.math.sqrt

// Predefined landmark points for our simulated map
data class MapLandmark(
    val nameAr: String,
    val cityAr: String,
    val latitude: Double,
    val longitude: Double,
    // Relative position on a 400x400 canvas grid
    val gridX: Float,
    val gridY: Float
)

val simulatedLandmarks = listOf(
    MapLandmark("مركز الأحمدي للاتصالات (الرئيسي)", "صنعاء", 15.3533, 44.2058, 200f, 150f),
    MapLandmark("حي حدة - شارع جيبوتي", "صنعاء", 15.3120, 44.1850, 120f, 280f),
    MapLandmark("شارع الستين الغربي", "صنعاء", 15.3422, 44.1730, 80f, 180f),
    MapLandmark("حي الأصبحي - المقالح", "صنعاء", 15.2890, 44.2120, 240f, 340f),
    MapLandmark("شارع تعز - شميلة", "صنعاء", 15.3050, 44.2250, 290f, 300f),
    MapLandmark("كريتر - العقبة", "عدن", 12.7833, 45.0333, 150f, 320f),
    MapLandmark("خور مكسر - ساحل أبين", "عدن", 12.8250, 45.0250, 250f, 120f),
    MapLandmark("شارع جمال عبد الناصر", "تعز", 13.5780, 44.0150, 180f, 220f),
    MapLandmark("التحرير - وسط المدينة", "صنعاء", 15.3560, 44.2080, 210f, 110f),
    MapLandmark("شارع القصر الجمهوري", "صنعاء", 15.3450, 44.2150, 260f, 170f)
)

@Composable
fun InteractiveSimulatedMap(
    initialLat: Double? = null,
    initialLng: Double? = null,
    onLocationSelected: (Double, Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    // Current pin coordinates
    var selectedLat by remember { mutableStateOf(initialLat ?: 15.3533) }
    var selectedLng by remember { mutableStateOf(initialLng ?: 44.2058) }
    
    // Relative grid position
    var pinX by remember { mutableStateOf(200f) }
    var pinY by remember { mutableStateOf(150f) }

    // Synchronize initial location to grid position if provided
    LaunchedEffect(initialLat, initialLng) {
        if (initialLat != null && initialLng != null) {
            val matching = simulatedLandmarks.minByOrNull { 
                val dLat = it.latitude - initialLat
                val dLng = it.longitude - initialLng
                dLat * dLat + dLng * dLng
            }
            if (matching != null) {
                pinX = matching.gridX + ((initialLng - matching.longitude) * 1000f).toFloat()
                pinY = matching.gridY - ((initialLat - matching.latitude) * 1000f).toFloat()
            }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedLandmarkName by remember { mutableStateOf("مركز الأحمدي للاتصالات (الرئيسي)") }

    // Nearest neighborhood resolver
    val resolvedAddress = remember(selectedLat, selectedLng) {
        val nearest = simulatedLandmarks.minByOrNull { 
            val dLat = it.latitude - selectedLat
            val dLng = it.longitude - selectedLng
            dLat * dLat + dLng * dLng
        }
        if (nearest != null) {
            val distance = sqrt((nearest.latitude - selectedLat) * (nearest.latitude - selectedLat) + (nearest.longitude - selectedLng) * (nearest.longitude - selectedLng))
            if (distance < 0.005) {
                "${nearest.cityAr} - ${nearest.nameAr}"
            } else {
                "${nearest.cityAr} - جوار ${nearest.nameAr}"
            }
        } else {
            "صنعاء - اليمن"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(550.dp)
            .shadow(12.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isAppDarkTheme()) TealDark else Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isAppDarkTheme()) GoldAccent else TealMedium)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "اغلاق", tint = if (isAppDarkTheme()) Color.White else TealDark)
                }
                Text(
                    text = "تحديد موقع التوصيل من الخريطة 🗺️",
                    color = if (isAppDarkTheme()) GoldAccent else TealDark,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Default.Map,
                    contentDescription = null,
                    tint = if (isAppDarkTheme()) GoldAccent else TealMedium,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Suggestion Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث عن حي أو معلم (مثال: حدة)", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = appTextFieldColors(),
                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
            )

            // Search Results popup (simulated)
            if (searchQuery.isNotEmpty()) {
                val filtered = simulatedLandmarks.filter { 
                    it.nameAr.contains(searchQuery, ignoreCase = true) || 
                    it.cityAr.contains(searchQuery, ignoreCase = true) 
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp)
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isAppDarkTheme()) TealMedium else Color(0xFFF0F5F5))
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        if (filtered.isEmpty()) {
                            Text("لا توجد نتائج مطابقة", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(6.dp))
                        } else {
                            filtered.take(3).forEach { landmark ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedLat = landmark.latitude
                                            selectedLng = landmark.longitude
                                            pinX = landmark.gridX
                                            pinY = landmark.gridY
                                            selectedLandmarkName = landmark.nameAr
                                            searchQuery = ""
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${landmark.cityAr} - ${landmark.nameAr}", color = if (isAppDarkTheme()) Color.White else TealDark, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Hotspots Horizontal Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(simulatedLandmarks.take(6)) { landmark ->
                    val isSelected = selectedLandmarkName == landmark.nameAr
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) GoldAccent else (if (isAppDarkTheme()) TealMedium else Color(0xFFE6EFEF)),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedLat = landmark.latitude
                                selectedLng = landmark.longitude
                                pinX = landmark.gridX
                                pinY = landmark.gridY
                                selectedLandmarkName = landmark.nameAr
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = landmark.nameAr.split(" - ").last(),
                            color = if (isSelected) TealDark else (if (isAppDarkTheme()) Color.White else TealDark),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Simulated Map Area (Custom Canvas + draggable pin)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, if (isAppDarkTheme()) Color.DarkGray else Color.LightGray, RoundedCornerShape(12.dp))
                    .background(if (isAppDarkTheme()) Color(0xFF1B2E2E) else Color(0xFFEAF2F2))
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            pinX = offset.x.coerceIn(20f, size.width.toFloat() - 20f)
                            pinY = offset.y.coerceIn(20f, size.height.toFloat() - 20f)
                            
                            // Map grid coordinate conversion to lat/lng
                            // Center of map is Sanaa center (15.3533, 44.2058)
                            val gridCenterX = size.width / 2f
                            val gridCenterY = size.height / 2f
                            
                            selectedLng = 44.2058 + ((pinX - gridCenterX) / 5000.0)
                            selectedLat = 15.3533 - ((pinY - gridCenterY) / 5000.0)
                            selectedLandmarkName = "موقع مخصص على الخريطة"
                        }
                    }
            ) {
                // Map Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    // Draw Main Roads
                    // Ring road 1
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.3f),
                        radius = w * 0.3f,
                        center = Offset(w / 2f, h / 2f),
                        style = Stroke(width = 6f)
                    )
                    // Ring road 2
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.2f),
                        radius = w * 0.5f,
                        center = Offset(w / 2f, h / 2f),
                        style = Stroke(width = 4f)
                    )

                    // Intersecting grid roads
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = Offset(0f, h / 2f),
                        end = Offset(w, h / 2f),
                        strokeWidth = 5f
                    )
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = Offset(w / 2f, 0f),
                        end = Offset(w / 2f, h),
                        strokeWidth = 5f
                    )
                    
                    // Diagonal roads
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.15f),
                        start = Offset(0f, 0f),
                        end = Offset(w, h),
                        strokeWidth = 4f
                    )
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.15f),
                        start = Offset(w, 0f),
                        end = Offset(0f, h),
                        strokeWidth = 4f
                    )

                    // Predefined Landmarks as soft points
                    simulatedLandmarks.forEach { landmark ->
                        val relativeX = (landmark.gridX / 400f) * w
                        val relativeY = (landmark.gridY / 400f) * h
                        
                        drawCircle(
                            color = TealMedium.copy(alpha = 0.6f),
                            radius = 6f,
                            center = Offset(relativeX, relativeY)
                        )
                    }
                }

                // Landmark Labels overlay
                simulatedLandmarks.forEach { landmark ->
                    Box(
                        modifier = Modifier
                            .offset(
                                x = ((landmark.gridX / 400f) * 320).dp - 30.dp,
                                y = ((landmark.gridY / 400f) * 200).dp
                            )
                            .background(TealDark.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(2.dp)
                    ) {
                        Text(
                            text = landmark.nameAr.take(8) + "..",
                            color = Color.White,
                            fontSize = 6.sp
                        )
                    }
                }

                // Glowing selected address PIN
                Box(
                    modifier = Modifier
                        .offset(
                            x = (pinX / 400f * 320).dp - 12.dp,
                            y = (pinY / 400f * 200).dp - 24.dp
                        )
                        .size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsing indicator
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(GoldAccent.copy(alpha = 0.4f), CircleShape)
                    )
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = AlertError,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Address Detail Banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isAppDarkTheme()) TealMedium else Color(0xFFF5F9F9)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📍 العنوان المكتشف:",
                            color = if (isAppDarkTheme()) GoldAccent else TealDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = resolvedAddress,
                            color = if (isAppDarkTheme()) Color.White else Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "الإحداثيات: ${String.format("%.4f", selectedLat)} , ${String.format("%.4f", selectedLng)}",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                    }
                    
                    Button(
                        onClick = {
                            onLocationSelected(selectedLat, selectedLng, resolvedAddress)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent,
                            contentColor = TealDark
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "تأكيد الموقع ✅",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderMapViewerDialog(
    latitude: Double,
    longitude: Double,
    addressText: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAppDarkTheme()) TealDark else Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GoldAccent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = if (isAppDarkTheme()) Color.White else TealDark)
                        }
                        Text(
                            text = "تتبع موقع العميل على الخريطة 📍",
                            color = if (isAppDarkTheme()) GoldAccent else TealDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = AlertError)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = addressText,
                        color = if (isAppDarkTheme()) Color.White else TealDark,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "الإحداثيات الحقيقية: $latitude , $longitude",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Simulated Map view (non-interactive, just renders the pin)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isAppDarkTheme()) Color(0xFF1B2E2E) else Color(0xFFEAF2F2))
                            .border(1.dp, Color.Gray, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Map lines
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            drawCircle(color = Color.Gray.copy(alpha = 0.2f), radius = w * 0.3f, center = Offset(w/2, h/2), style = Stroke(4f))
                            drawLine(color = Color.Gray.copy(alpha = 0.2f), start = Offset(0f, h/2), end = Offset(w, h/2), strokeWidth = 3f)
                            drawLine(color = Color.Gray.copy(alpha = 0.2f), start = Offset(w/2, 0f), end = Offset(w/2, h), strokeWidth = 3f)
                        }

                        // Center pin representing delivery address
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = AlertError,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(TealDark, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("موقع التسليم", color = GoldAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = TealMedium, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("الرجوع إلى تفاصيل الطلب")
                    }
                }
            }
        }
    }
}
