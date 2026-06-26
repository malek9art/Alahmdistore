package com.example.ui

import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.example.ui.theme.*
import com.example.viewmodel.StoreViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.sqrt

// Predefined landmark points for quick starting selections
data class MapLandmark(
    val nameAr: String,
    val cityAr: String,
    val latitude: Double,
    val longitude: Double,
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

// Helper functions for actual Geocoding
suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): String {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale("ar"))
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val adminArea = address.adminArea ?: ""
                val locality = address.locality ?: ""
                val subLocality = address.subLocality ?: ""
                val thoroughfare = address.thoroughfare ?: ""
                val featureName = address.featureName ?: ""
                
                val parts = listOfNotNull(
                    if (adminArea.isNotEmpty()) adminArea else null,
                    if (locality.isNotEmpty() && locality != adminArea) locality else null,
                    if (subLocality.isNotEmpty()) subLocality else null,
                    if (thoroughfare.isNotEmpty()) thoroughfare else null,
                    if (featureName.isNotEmpty() && featureName != thoroughfare && featureName != locality) featureName else null
                ).distinct()
                
                if (parts.isNotEmpty()) parts.joinToString(" - ") else "اليمن - موقع محدد"
            } else {
                "اليمن (إحداثيات: ${String.format(Locale.US, "%.4f", lat)}, ${String.format(Locale.US, "%.4f", lng)})"
            }
        } catch (e: Exception) {
            "اليمن (إحداثيات: ${String.format(Locale.US, "%.4f", lat)}, ${String.format(Locale.US, "%.4f", lng)})"
        }
    }
}

suspend fun geocodeAddress(context: Context, query: String): List<Pair<String, Pair<Double, Double>>> {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale("ar"))
            val addresses = geocoder.getFromLocationName(query, 5)
            addresses?.map { address ->
                val adminArea = address.adminArea ?: ""
                val locality = address.locality ?: ""
                val thoroughfare = address.thoroughfare ?: ""
                val featureName = address.featureName ?: ""
                val name = listOfNotNull(
                    if (adminArea.isNotEmpty()) adminArea else null,
                    if (locality.isNotEmpty() && locality != adminArea) locality else null,
                    if (thoroughfare.isNotEmpty()) thoroughfare else null,
                    if (featureName.isNotEmpty() && featureName != thoroughfare) featureName else null
                ).distinct().joinToString(" - ")
                Pair(if (name.isNotEmpty()) name else query, Pair(address.latitude, address.longitude))
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

// Utility to open external Google Maps
fun openGoogleMapsApp(context: Context, lat: Double, lng: Double) {
    try {
        val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(موقع العميل)")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng"))
            context.startActivity(webIntent)
        }
    } catch (e: Exception) {
        Toast.makeText(context, "فشل فتح تطبيق خرائط جوجل: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

// Parses latitude/longitude from text (direct coordinate or Google Maps link)
fun parseLatLng(text: String): Pair<Double, Double>? {
    val cleanText = text.trim()
    // Pattern 1: Coordinate pair like "15.3533, 44.2058"
    val regex = """(-?\d+\.\d+)\s*,\s*(-?\d+\.\d+)""".toRegex()
    val match = regex.find(cleanText)
    if (match != null) {
        val lat = match.groupValues[1].toDoubleOrNull()
        val lng = match.groupValues[2].toDoubleOrNull()
        if (lat != null && lng != null) {
            return Pair(lat, lng)
        }
    }
    
    // Pattern 2: URL containing query param q=15.3533,44.2058
    val qRegex = """q=(-?\d+\.\d+),(-?\d+\.\d+)""".toRegex()
    val qMatch = qRegex.find(cleanText)
    if (qMatch != null) {
        val lat = qMatch.groupValues[1].toDoubleOrNull()
        val lng = qMatch.groupValues[2].toDoubleOrNull()
        if (lat != null && lng != null) {
            return Pair(lat, lng)
        }
    }
    
    // Pattern 3: URL containing path @15.3533,44.2058
    val pathRegex = """@(-?\d+\.\d+),(-?\d+\.\d+)""".toRegex()
    val pathMatch = pathRegex.find(cleanText)
    if (pathMatch != null) {
        val lat = pathMatch.groupValues[1].toDoubleOrNull()
        val lng = pathMatch.groupValues[2].toDoubleOrNull()
        if (lat != null && lng != null) {
            return Pair(lat, lng)
        }
    }
    
    return null
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun InteractiveSimulatedMap(
    initialLat: Double? = null,
    initialLng: Double? = null,
    onLocationSelected: (Double, Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Real Coordinates States
    var selectedLat by remember { mutableStateOf(initialLat ?: 15.3533) }
    var selectedLng by remember { mutableStateOf(initialLng ?: 44.2058) }
    
    // Address state fetched from Geocoder
    var resolvedAddress by remember { mutableStateOf("جاري تحديد العنوان الفعلي...") }
    var isGeocoding by remember { mutableStateOf(false) }

    // Geocoding effect on coordinate change
    LaunchedEffect(selectedLat, selectedLng) {
        isGeocoding = true
        resolvedAddress = reverseGeocode(context, selectedLat, selectedLng)
        isGeocoding = false
    }

    // Relative grid position for fallback visual canvas
    var pinX by remember { mutableStateOf(200f) }
    var pinY by remember { mutableStateOf(150f) }

    // Synchronize coordinates to Canvas grid when changed externally (GPS, Search, Paste)
    LaunchedEffect(selectedLat, selectedLng) {
        val matching = simulatedLandmarks.minByOrNull { 
            val dLat = it.latitude - selectedLat
            val dLng = it.longitude - selectedLng
            dLat * dLat + dLng * dLng
        }
        if (matching != null) {
            pinX = (matching.gridX + ((selectedLng - matching.longitude) * 2000f).toFloat()).coerceIn(20f, 380f)
            pinY = (matching.gridY - ((selectedLat - matching.latitude) * 2000f).toFloat()).coerceIn(20f, 380f)
        } else {
            pinX = 200f
            pinY = 150f
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var realSearchResults by remember { mutableStateOf<List<Pair<String, Pair<Double, Double>>>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Search trigger
    LaunchedEffect(searchQuery) {
        if (searchQuery.trim().length > 2) {
            isSearching = true
            realSearchResults = geocodeAddress(context, searchQuery)
            isSearching = false
        } else {
            realSearchResults = emptyList()
        }
    }

    // Google Maps Paste Coordinates State
    var pasteInput by remember { mutableStateOf("") }

    // Location Permission Manager
    val locationPermissionState = rememberPermissionState(
        permission = android.Manifest.permission.ACCESS_FINE_LOCATION
    )
    var isGpsLoading by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(580.dp)
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
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = if (isAppDarkTheme()) Color.White else TealDark)
                }
                Text(
                    text = "تحديد موقع التوصيل الفعلي 📍",
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

            Spacer(modifier = Modifier.height(4.dp))

            // Real address text field with real geocoder lookup
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث عن حي، مدينة، أو معلم حقيقي (صنعاء، الستين)", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty() || isSearching) {
                        IconButton(onClick = { searchQuery = "" }) {
                            if (isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = GoldAccent)
                            } else {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
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

            // Real address lookup dropdown
            if (realSearchResults.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp)
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isAppDarkTheme()) TealMedium else Color(0xFFF0F5F5)),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.3f))
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 120.dp)
                            .padding(4.dp)
                    ) {
                        items(realSearchResults) { result ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedLat = result.second.first
                                        selectedLng = result.second.second
                                        searchQuery = ""
                                        Toast.makeText(context, "تم تحديد: ${result.first}", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(result.first, color = if (isAppDarkTheme()) Color.White else TealDark, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // GPS, Google Maps and Coordinate Paste row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // GPS Button
                Button(
                    onClick = {
                        if (locationPermissionState.status.isGranted) {
                            isGpsLoading = true
                            try {
                                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                                fusedLocationClient.lastLocation
                                    .addOnSuccessListener { location: Location? ->
                                        isGpsLoading = false
                                        if (location != null) {
                                            selectedLat = location.latitude
                                            selectedLng = location.longitude
                                            Toast.makeText(context, "تم جلب موقعك الحالي بنجاح! 🛰️", Toast.LENGTH_SHORT).show()
                                        } else {
                                            // Fallback to LocationManager if lastLocation is null
                                            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                                            val providers = locationManager.getProviders(true)
                                            var bestLocation: Location? = null
                                            for (provider in providers) {
                                                val loc = locationManager.getLastKnownLocation(provider) ?: continue
                                                if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                                                    bestLocation = loc
                                                }
                                            }
                                            if (bestLocation != null) {
                                                selectedLat = bestLocation.latitude
                                                selectedLng = bestLocation.longitude
                                                Toast.makeText(context, "تم جلب موقعك الحالي بنجاح! 🛰️", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "تأكد من تفعيل ميزة تحديد الموقع (GPS) في هاتفك.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                    .addOnFailureListener {
                                        isGpsLoading = false
                                        Toast.makeText(context, "فشل تحديد الموقع: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                            } catch (e: SecurityException) {
                                isGpsLoading = false
                                Toast.makeText(context, "خطأ في صلاحيات تحديد الموقع", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            locationPermissionState.launchPermissionRequest()
                        }
                    },
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealMedium, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isGpsLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 1.5.dp)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                        Text(
                            text = if (locationPermissionState.status.isGranted) "موقعي عبر GPS 🛰️" else "تفعيل الـ GPS 🔓",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Google Maps Direct App Button
                Button(
                    onClick = {
                        openGoogleMapsApp(context, selectedLat, selectedLng)
                    },
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text("خرائط Google 🗺️", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Coordinate Paste field
            OutlinedTextField(
                value = pasteInput,
                onValueChange = { pasteInput = it },
                placeholder = { Text("الصق هنا الإحداثيات أو رابط مشاركة خرائط Google", fontSize = 10.sp) },
                trailingIcon = {
                    if (pasteInput.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val parsed = parseLatLng(pasteInput)
                                if (parsed != null) {
                                    selectedLat = parsed.first
                                    selectedLng = parsed.second
                                    pasteInput = ""
                                    Toast.makeText(context, "تم استخراج وتطبيق الإحداثيات بنجاح! 📋", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "لم نتمكن من استخراج الإحداثيات، يرجى التأكد من صحة الرابط أو النص الملصق.", Toast.LENGTH_LONG).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "تطبيق المنسوخ", tint = GoldAccent)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = appTextFieldColors(),
                textStyle = LocalTextStyle.current.copy(fontSize = 10.sp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Draggable Map Canvas indicating real coordinates
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
                            
                            val gridCenterX = size.width / 2f
                            val gridCenterY = size.height / 2f
                            
                            selectedLng = 44.2058 + ((pinX - gridCenterX) / 2000.0)
                            selectedLat = 15.3533 - ((pinY - gridCenterY) / 2000.0)
                        }
                    }
            ) {
                // Interactive grid visual representing the map
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    // Outer rings representing distances
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.3f),
                        radius = w * 0.3f,
                        center = Offset(w / 2f, h / 2f),
                        style = Stroke(width = 6f)
                    )
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.2f),
                        radius = w * 0.5f,
                        center = Offset(w / 2f, h / 2f),
                        style = Stroke(width = 4f)
                    )

                    // Roads
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
                    
                    // Diagonal streets
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
                    
                    // Draw quick landmarks
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

                // Quick Landmarks Labels Overlay
                simulatedLandmarks.forEach { landmark ->
                    Box(
                        modifier = Modifier
                            .offset(
                                x = ((landmark.gridX / 400f) * 320).dp - 30.dp,
                                y = ((landmark.gridY / 400f) * 160).dp
                            )
                            .background(TealDark.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(2.dp)
                            .clickable {
                                selectedLat = landmark.latitude
                                selectedLng = landmark.longitude
                            }
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
                            y = (pinY / 400f * 160).dp - 24.dp
                        )
                        .size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
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

            Spacer(modifier = Modifier.height(6.dp))

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
                            text = "📍 العنوان المكتشف الحقيقي:",
                            color = if (isAppDarkTheme()) GoldAccent else TealDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isGeocoding) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = GoldAccent)
                                Text("جاري فك تشفير الإحداثيات عبر خدمة خرائط Google...", color = Color.Gray, fontSize = 10.sp)
                            }
                        } else {
                            Text(
                                text = resolvedAddress,
                                color = if (isAppDarkTheme()) Color.White else Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "الإحداثيات الفعلية: ${String.format(Locale.US, "%.5f", selectedLat)} , ${String.format(Locale.US, "%.5f", selectedLng)}",
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
    val context = LocalContext.current
    
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
                    .height(500.dp),
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
                            text = "تتبع موقع العميل الفعلي 📍",
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
                        text = "الإحداثيات الحقيقية للعميل: $latitude , $longitude",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Map view representing the coordinates
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

                    // Turn-by-Turn Delivery navigation button
                    Button(
                        onClick = {
                            openGoogleMapsApp(context, latitude, longitude)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TealDark),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null)
                            Text("فتح الملاحة وتتبع الموقع في Google Maps 🗺️", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = TealMedium, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("الرجوع إلى تفاصيل الطلب")
                    }
                }
            }
        }
    }
}
