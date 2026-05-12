package com.reshma.vidyarthibus

import android.content.Intent
import com.google.firebase.FirebaseApp
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.*

// ✅ Firebase URL defined once
const val DB_URL =
    "https://vidyarthi-bus-81c94-default-rtdb.firebaseio.com"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        // ✅ Persistence must be called before any other Firebase usage
        try {
            FirebaseDatabase.getInstance(DB_URL)
                .setPersistenceEnabled(true)
        } catch (e: Exception) {
            // Already enabled — safe to ignore
        }

        setContent {
            var isDarkMode by remember { mutableStateOf(false) }
            MaterialTheme(
                colorScheme = if (isDarkMode)
                    darkColorScheme()
                else
                    lightColorScheme()
            ) {
                AppContent(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { isDarkMode = !isDarkMode }
                )
            }
        }
    }
}

sealed class Screen {

    object Splash : Screen()

    object Login : Screen()

    object Signup : Screen()

    object Home : Screen()

    object Profile : Screen()

    object Settings : Screen()

    object Admin : Screen()

    data class Details(
        val busId: String,
        val busNumber: String,
        val route: String,
        val driver: String,
        val status: String,
        val timing: String,
        val totalSeats: Int,
        val availableSeats: Int,
        val crowdStatus: String,
        val latitude: Double,
        val longitude: Double
    ) : Screen()
}

@Composable
fun AppContent(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {

    var currentScreen by remember {
        mutableStateOf<Screen>(Screen.Splash)
    }

    LaunchedEffect(Unit) {

        kotlinx.coroutines.delay(2500)

        if (currentScreen == Screen.Splash) {
            currentScreen = Screen.Login
        }
    }

    Scaffold(

        bottomBar = {

            if (
                currentScreen == Screen.Home ||
                currentScreen == Screen.Profile ||
                currentScreen == Screen.Settings
            ) {

                NavigationBar {

                    NavigationBarItem(
                        selected = currentScreen == Screen.Home,
                        onClick = {
                            currentScreen = Screen.Home
                        },
                        icon = {
                            Icon(
                                Icons.Filled.Home,
                                contentDescription = null
                            )
                        },
                        label = {
                            Text("Home")
                        }
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.Profile,
                        onClick = {
                            currentScreen = Screen.Profile
                        },
                        icon = {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null
                            )
                        },
                        label = {
                            Text("Profile")
                        }
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.Settings,
                        onClick = {
                            currentScreen = Screen.Settings
                        },
                        icon = {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = null
                            )
                        },
                        label = {
                            Text("Settings")
                        }
                    )
                }
            }
        }

    ) { paddingValues ->

        Box(
            modifier = Modifier.padding(paddingValues)
        ) {

            when (val screen = currentScreen) {

                is Screen.Splash -> {

                    SplashScreen()
                }

                is Screen.Login -> {

                    LoginScreen(
                        onLogin = {
                            currentScreen = Screen.Home
                        },
                        onSignupClick = {
                            currentScreen = Screen.Signup
                        }
                    )
                }

                is Screen.Signup -> {

                    SignupScreen(
                        onSignup = {
                            currentScreen = Screen.Home
                        },
                        onBack = {
                            currentScreen = Screen.Login
                        }
                    )
                }

                is Screen.Home -> {

                    HomeScreen(
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = onToggleDarkMode,
                        onBusClick = { bus ->

                            currentScreen = Screen.Details(
                                busId = bus["busId"] ?: "",
                                busNumber = bus["busNumber"] ?: "N/A",
                                route = bus["route"] ?: "N/A",
                                driver = bus["driver"] ?: "N/A",
                                status = bus["status"] ?: "N/A",
                                timing = bus["timing"] ?: "N/A",
                                totalSeats = bus["totalSeats"]
                                    ?.toIntOrNull() ?: 40,
                                availableSeats = bus["availableSeats"]
                                    ?.toIntOrNull() ?: 0,
                                crowdStatus = bus["crowdStatus"]
                                    ?.trim() ?: "EMPTY",
                                latitude = bus["latitude"]
                                    ?.toDoubleOrNull() ?: 0.0,
                                longitude = bus["longitude"]
                                    ?.toDoubleOrNull() ?: 0.0
                            )
                        },
                        onAdminClick = {
                            currentScreen = Screen.Admin
                        }
                    )
                }

                is Screen.Profile -> {

                    ProfileScreen(
                        onLogout = {
                            currentScreen = Screen.Login
                        }
                    )
                }

                is Screen.Settings -> {

                    SettingsScreen()
                }

                is Screen.Details -> {

                    BackHandler {
                        currentScreen = Screen.Home
                    }

                    BusDetailsScreen(
                        screen = screen,
                        onBack = {
                            currentScreen = Screen.Home
                        }
                    )
                }

                is Screen.Admin -> {

                    BackHandler {
                        currentScreen = Screen.Home
                    }

                    AdminScreen(
                        onBack = {
                            currentScreen = Screen.Home
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onBusClick: (Map<String, String>) -> Unit,
    onAdminClick: () -> Unit
) {
    val context = LocalContext.current
    val busList = remember {
        mutableStateListOf<Map<String, String>>()
    }
    var searchText by remember {
        mutableStateOf(TextFieldValue(""))
    }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // ✅ FIX 2: Proper Firebase listener with detailed error logging
    LaunchedEffect(Unit) {
        android.util.Log.d("FIREBASE", "Connecting to: $DB_URL")

        val ref = FirebaseDatabase
            .getInstance(DB_URL)
            .getReference("buses")

        // ✅ FIX 3: keepSynced ensures data loads even on slow connections
        ref.keepSynced(true)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                android.util.Log.d(
                    "FIREBASE",
                    "onDataChange called. Exists: ${snapshot.exists()}, Count: ${snapshot.childrenCount}"
                )

                busList.clear()
                isLoading = false
                errorMessage = ""

                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    errorMessage = "No buses found in database"
                    return
                }

                for (busSnapshot in snapshot.children) {
                    android.util.Log.d(
                        "FIREBASE",
                        "Processing bus key: ${busSnapshot.key}"
                    )

                    val bus = mutableMapOf<String, String>()
                    bus["busId"] = busSnapshot.key ?: ""

                    // ✅ FIX 4: Safe getValue with toString() — handles Long/Int/String from Firebase
                    bus["busNumber"] = busSnapshot
                        .child("busNumber")
                        .getValue(Any::class.java)?.toString() ?: "N/A"
                    bus["route"] = busSnapshot
                        .child("route")
                        .getValue(Any::class.java)?.toString() ?: "N/A"
                    bus["driver"] = busSnapshot
                        .child("driver")
                        .getValue(Any::class.java)?.toString() ?: "N/A"
                    bus["status"] = busSnapshot
                        .child("status")
                        .getValue(Any::class.java)?.toString() ?: "On Time"
                    bus["timing"] = busSnapshot
                        .child("timing")
                        .getValue(Any::class.java)?.toString() ?: "N/A"
                    bus["totalSeats"] = busSnapshot
                        .child("totalSeats")
                        .getValue(Any::class.java)?.toString() ?: "40"
                    bus["availableSeats"] = busSnapshot
                        .child("availableSeats")
                        .getValue(Any::class.java)?.toString() ?: "0"
                    // ✅ FIX 5: Trim crowdStatus to avoid hidden whitespace issues
                    bus["crowdStatus"] = busSnapshot
                        .child("crowdStatus")
                        .getValue(Any::class.java)?.toString()?.trim() ?: "EMPTY"
                    bus["latitude"] = busSnapshot
                        .child("latitude")
                        .getValue(Any::class.java)?.toString() ?: "0.0"
                    bus["longitude"] = busSnapshot
                        .child("longitude")
                        .getValue(Any::class.java)?.toString() ?: "0.0"

                    android.util.Log.d(
                        "FIREBASE",
                        "Added bus: ${bus["busNumber"]} | ${bus["route"]} | crowd: ${bus["crowdStatus"]}"
                    )

                    busList.add(bus)
                }

                android.util.Log.d(
                    "FIREBASE",
                    "Total buses loaded: ${busList.size}"
                )
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e(
                    "FIREBASE",
                    "Error code: ${error.code}, Message: ${error.message}, Details: ${error.details}"
                )
                errorMessage = "Error ${error.code}: ${error.message}"
                isLoading = false
            }
        })
    }

    val filteredList = busList.filter {
        val route = it["route"] ?: ""
        val busNum = it["busNumber"] ?: ""
        val query = searchText.text
        route.contains(query, ignoreCase = true) ||
                busNum.contains(query, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "🚌 Vidyarthi Bus",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onToggleDarkMode) {
                        Icon(
                            if (isDarkMode) Icons.Filled.Star else Icons.Filled.Star,
                            contentDescription = "Toggle Dark Mode"
                        )
                    }
                    IconButton(onClick = onAdminClick) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Admin"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchText.text,
                onValueChange = {
                    searchText = TextFieldValue(it)
                },
                label = {
                    Text("Search by Route or Bus Number")
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ✅ FIX 6: Map button with fallback for emulator (no internet)
            Button(
                onClick = {
                    try {
                        val uri = Uri.parse(
                            "https://www.google.com/maps/search/" +
                                    "?api=1&query=12.9716,77.5946"
                        )
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, uri)
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("MAP", "Maps not available: ${e.message}")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Live Map")
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Loading buses from Firebase...")
                    }
                }
                errorMessage.isNotEmpty() -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFB71C1C)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color.White,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                filteredList.isEmpty() -> {
                    // ✅ FIX 7: Show empty state when loaded but nothing found
                    Text(
                        text = "No buses match your search.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    Text(
                        text = "${filteredList.size} bus(es) found",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(filteredList) { bus ->
                    BusCard(
                        bus = bus,
                        onClick = { onBusClick(bus) }
                    )
                }
            }
        }
    }
}

@Composable
fun BusCard(
    bus: Map<String, String>,
    onClick: () -> Unit
) {
    // ✅ FIX 8: Use uppercase().trim() for safe comparison
    val crowdStatus = bus["crowdStatus"]?.uppercase()?.trim() ?: "EMPTY"

    val crowdColor = when (crowdStatus) {
        "EMPTY" -> Color(0xFF2E7D32)
        "MODERATE" -> Color(0xFFF57F17)
        "CROWDED" -> Color.Red
        else -> Color.Gray
    }

    val crowdEmoji = when (crowdStatus) {
        "EMPTY" -> "🟢 Empty"
        "MODERATE" -> "🟡 Moderate"
        "CROWDED" -> "🔴 Crowded"
        else -> "⚪ Unknown"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🚌 ${bus["busNumber"]}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                val statusText = bus["status"] ?: "Unknown"
                Text(
                    text = statusText,
                    color = if (statusText == "On Time")
                        Color(0xFF2E7D32) else Color.Red,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("📍 ${bus["route"] ?: "N/A"}")
            Spacer(modifier = Modifier.height(4.dp))
            Text("👤 Driver: ${bus["driver"] ?: "N/A"}")
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("🕐 ${bus["timing"] ?: "N/A"}")
                Text(
                    text = crowdEmoji,
                    color = crowdColor,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            val total = bus["totalSeats"]?.toIntOrNull() ?: 40
            val available = bus["availableSeats"]?.toIntOrNull() ?: 0
            // ✅ FIX 9: Clamp fillRatio to [0, 1] to avoid crash
            val occupied = (total - available).coerceAtLeast(0)
            val fillRatio = if (total > 0)
                (occupied.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            else 0f

            LinearProgressIndicator(
                progress = { fillRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = crowdColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$available seats available of $total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusDetailsScreen(
    screen: Screen.Details,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // ✅ FIX 10: Normalize crowdStatus for safe comparison
    val crowdStatus = screen.crowdStatus.uppercase().trim()

    val crowdColor = when (crowdStatus) {
        "EMPTY" -> Color(0xFF2E7D32)
        "MODERATE" -> Color(0xFFF57F17)
        "CROWDED" -> Color.Red
        else -> Color.Gray
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bus Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🚌 ${screen.busNumber}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = screen.route,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Status",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = screen.status,
                            fontWeight = FontWeight.Bold,
                            color = if (screen.status == "On Time")
                                Color(0xFF2E7D32) else Color.Red
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Timing",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = screen.timing,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Crowd Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (crowdStatus) {
                            "EMPTY" -> "🟢 Empty — Plenty of space!"
                            "MODERATE" -> "🟡 Moderate — Some seats available"
                            "CROWDED" -> "🔴 Crowded — Very few seats!"
                            else -> "⚪ Unknown (raw: ${screen.crowdStatus})"
                        },
                        color = crowdColor,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val occupied = (screen.totalSeats - screen.availableSeats)
                        .coerceAtLeast(0)
                    val fillRatio = if (screen.totalSeats > 0)
                        (occupied.toFloat() / screen.totalSeats.toFloat())
                            .coerceIn(0f, 1f)
                    else 0f

                    LinearProgressIndicator(
                        progress = { fillRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp),
                        color = crowdColor
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Available: ${screen.availableSeats}")
                        Text("Total: ${screen.totalSeats}")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Driver",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            screen.driver,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ FIX 11: Show coordinates on button so you know what's being tracked
            Button(
                onClick = {
                    try {
                        val uri = Uri.parse(
                            "https://www.google.com/maps/search/" +
                                    "?api=1&query=${screen.latitude},${screen.longitude}"
                        )
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, uri)
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("MAP", "Maps error: ${e.message}")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1565C0)
                )
            ) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Track Live Location (${screen.latitude}, ${screen.longitude})")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(onBack: () -> Unit) {

    var busNumber by remember { mutableStateOf("") }
    var route by remember { mutableStateOf("") }
    var driver by remember { mutableStateOf("") }
    var timing by remember { mutableStateOf("") }
    var totalSeats by remember { mutableStateOf("40") }
    var availableSeats by remember { mutableStateOf("40") }
    var selectedStatus by remember { mutableStateOf("On Time") }
    var selectedCrowd by remember { mutableStateOf("EMPTY") }
    var successMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val statusOptions = listOf("On Time", "Delayed", "Cancelled")
    val crowdOptions = listOf("EMPTY", "MODERATE", "CROWDED")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ Admin Panel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Add / Update Bus",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = busNumber,
                onValueChange = { busNumber = it },
                label = { Text("Bus Number (e.g. KA-09-1234)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = route,
                onValueChange = { route = it },
                label = { Text("Route (e.g. Mysore to Bangalore)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = driver,
                onValueChange = { driver = it },
                label = { Text("Driver Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = timing,
                onValueChange = { timing = it },
                label = { Text("Timing (e.g. 06:00 AM)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = totalSeats,
                    onValueChange = { totalSeats = it },
                    label = { Text("Total Seats") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = availableSeats,
                    onValueChange = { availableSeats = it },
                    label = { Text("Available Seats") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Bus Status:", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                statusOptions.forEach { option ->
                    FilterChip(
                        selected = selectedStatus == option,
                        onClick = { selectedStatus = option },
                        label = { Text(option) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Crowd Status:", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                crowdOptions.forEach { option ->
                    FilterChip(
                        selected = selectedCrowd == option,
                        onClick = { selectedCrowd = option },
                        label = { Text(option) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (successMessage.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (successMessage.contains("✅"))
                            Color(0xFF2E7D32)
                        else
                            Color(0xFFB71C1C)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = successMessage,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    if (busNumber.isNotBlank() && route.isNotBlank()) {
                        isLoading = true
                        successMessage = ""

                        val database = FirebaseDatabase
                            .getInstance(DB_URL)
                            .reference
                            .child("buses")

                        // ✅ FIX 12: Use busNumber as the key for easy lookup/update
                        // Replace special chars so the key is Firebase-safe
                        val safeKey = busNumber.trim()
                            .replace(" ", "_")
                            .replace(".", "-")

                        val busData = mapOf(
                            "busNumber" to busNumber.trim(),
                            "route" to route.trim(),
                            "driver" to driver.trim(),
                            "timing" to timing.trim(),
                            "status" to selectedStatus,
                            "totalSeats" to (totalSeats.toIntOrNull() ?: 40),
                            "availableSeats" to (availableSeats.toIntOrNull() ?: 40),
                            "crowdStatus" to selectedCrowd,
                            "latitude" to 12.9716,
                            "longitude" to 77.5946
                        )

                        database.child(safeKey)
                            .setValue(busData)
                            .addOnSuccessListener {
                                successMessage = "✅ Bus '$busNumber' saved successfully!"
                                isLoading = false
                                busNumber = ""
                                route = ""
                                driver = ""
                                timing = ""
                                totalSeats = "40"
                                availableSeats = "40"
                                selectedStatus = "On Time"
                                selectedCrowd = "EMPTY"
                            }
                            .addOnFailureListener { e ->
                                successMessage = "❌ Failed: ${e.message}"
                                isLoading = false
                                android.util.Log.e("FIREBASE_WRITE", "Write failed", e)
                            }
                    } else {
                        successMessage = "⚠️ Please fill Bus Number and Route!"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Bus to Firebase", fontSize = 16.sp)
                }
            }
        }
    }
}