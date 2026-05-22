package com.example

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge window drawing
        androidx.activity.enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

// Preset Urban Stickers (Youth designs that users can select to preview on 3D models)
data class UrbanSticker(
    val name: String,
    val styleDesc: String,
    val text: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val isRetroFlame: Boolean = false,
    val isGraffiti: Boolean = false,
    val isCrest: Boolean = false,
    val isChasknapLogo: Boolean = false
)

val PRESET_STICKERS = listOf(
    UrbanSticker("Chasknap Oficial", "Logo Ink Brush", "CHASKNAP CHS", Color.White, ElectricPink, isChasknapLogo = true),
    UrbanSticker("Chasknap OG", "Graffiti Tag", "CHASKNAP '26", CyberCyan, ElectricPink, isGraffiti = true),
    UrbanSticker("Street Flame", "Neon Fire", "GAME ON", ElectricPink, WarningYellow, isRetroFlame = true),
    UrbanSticker("Promo Gold", "School Shield", "PROMO '26", WarningYellow, CyberCyan, isCrest = true),
    UrbanSticker("Cyber Wave", "Digital Retro", "FUTURE NOW", CyberCyan, ElectricPink, isRetroFlame = true),
    UrbanSticker("Night Rider", "Vapor Synth", "SPEED MONSTER", ElectricPink, WarningYellow, isGraffiti = true)
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainAppScreen() {
    val viewModel: MainViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Navigation state: 0 = Diseña Ya, 1 = Team & Promo, 2 = PromoGrad, 3 = Seguimiento & Soporte
    var activeTab by remember { mutableStateOf(0) }
    var selectedStickerIndex by remember { mutableStateOf(0) }

    // Floating shopping cart open/close
    var cartOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MidnightOnyx,
        bottomBar = {
            NavigationBar(
                containerColor = CyberSlate,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                val tabItems = listOf(
                    Triple(0, "Diseña Ya", Icons.Filled.Brush),
                    Triple(1, "Team & Promo", Icons.Filled.People),
                    Triple(2, "PromoGrad", Icons.Filled.School),
                    Triple(3, "Tracking", Icons.Filled.QrCodeScanner)
                )
                tabItems.forEach { (index, title, icon) ->
                    val isSelected = activeTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { activeTab = index },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = if (isSelected) CyberCyan else TextMuted
                            )
                        },
                        label = {
                            Text(
                                text = title,
                                color = if (isSelected) TextWhite else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = CyberSlate.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main content background design (urban street elements)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        // Background radial gradient for modern dark glowing look
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(ElectricPink.copy(alpha = 0.08f), Color.Transparent),
                                center = Offset(size.width * 0.9f, size.height * 0.2f),
                                radius = size.width * 0.7f
                            )
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(CyberCyan.copy(alpha = 0.08f), Color.Transparent),
                                center = Offset(size.width * 0.1f, size.height * 0.7f),
                                radius = size.width * 0.7f
                            )
                        )
                    }
            )

            // Header Row
            Column {
                HeaderRow(
                    cartCount = state.cartItems.sumOf { it.quantity },
                    onCartClick = { cartOpen = !cartOpen }
                )

                // Animated transition between screens with spring physics
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        slideInHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) { width -> if (targetState > initialState) width else -width } with
                                slideOutHorizontally(
                                    animationSpec = spring(stiffness = Spring.StiffnessHigh)
                                ) { width -> if (targetState > initialState) -width else width }
                    },
                    modifier = Modifier.weight(1f)
                ) { target ->
                    when (target) {
                        0 -> DesignerTab(
                            viewModel = viewModel,
                            state = state,
                            selectedStickerIndex = selectedStickerIndex,
                            onStickerSelected = { selectedStickerIndex = it }
                        )
                        1 -> TeamPromoTab(viewModel = viewModel, state = state)
                        2 -> PromoGradTab(viewModel = viewModel, state = state)
                        3 -> TrackingTab(viewModel = viewModel, state = state)
                    }
                }
            }

            // Slide-up overlay Shopping Cart
            AnimatedVisibility(
                visible = cartOpen,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                CartDrawer(
                    state = state,
                    onClose = { cartOpen = false },
                    onRemove = { viewModel.removeFromCart(it) },
                    onCheckout = {
                        coroutineScope.launch {
                            viewModel.clearCart()
                            Toast.makeText(context, "💥 ¡Pedido Enviado! El asesor Chasknap te escribirá al WhatsApp.", Toast.LENGTH_LONG).show()
                            cartOpen = false
                            activeTab = 3 // go to tracking
                        }
                    }
                )
            }
        }
    }
}

// Global Header Component
@Composable
fun HeaderRow(cartCount: Int, onCartClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChasknapLogo(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, CyberCyan, RoundedCornerShape(12.dp))
                    .background(MidnightOnyx)
                    .padding(4.dp),
                color = Color.White,
                splatterColor = ElectricPink
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "CHASKNAP",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = TextWhite,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "HUB URBANO '26",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan,
                    letterSpacing = 2.sp
                )
            }
        }

        // Cart Icon with floating bubble count
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(CyberSlate)
                .clickable { onCartClick() }
                .testTag("open_cart_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.ShoppingCart,
                contentDescription = "Carrito Chasknap",
                tint = TextWhite,
                modifier = Modifier.size(22.dp)
            )
            if (cartCount > 0) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(ElectricPink)
                        .align(Alignment.TopEnd)
                        .offset(x = (-2).dp, y = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cartCount.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// SCREEN 1: DISEÑA YA (Interactive 3D Preview + Personalizer + AR)
@Composable
fun DesignerTab(
    viewModel: MainViewModel,
    state: AppState,
    selectedStickerIndex: Int,
    onStickerSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setCustomImage(uri, "logo_promo.png")
            Toast.makeText(context, "📸 ¡Logo cargado! Míralo en el modelo interactivo.", Toast.LENGTH_SHORT).show()
        }
    }

    // Colors available for customizing
    val customColors = listOf(
        0xFF10121A to "Negro Cuero",
        0xFFFFFFEA to "Crema Soft",
        0xFFFF0D7B to "Fucsia Electric",
        0xFF00FFCC to "Menta Fresh",
        0xFFFF7300 to "Fuego Urbano",
        0xFF0A66C2 to "Azul Royal"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("designer_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode switch: Catalog OR AR Mode
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberSlate)
                    .padding(4.dp)
            ) {
                Button(
                    onClick = { viewModel.toggleArMode(false) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!state.isArMode) CyberCyan else Color.Transparent,
                        contentColor = if (!state.isArMode) MidnightOnyx else TextMuted
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_3d_studio")
                ) {
                    Icon(Icons.Filled.ThreeDRotation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Studio 3D", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = { viewModel.toggleArMode(true) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isArMode) CyberCyan else Color.Transparent,
                        contentColor = if (state.isArMode) MidnightOnyx else TextMuted
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_ar_project")
                ) {
                    Icon(Icons.Filled.RemoveRedEye, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Proyectar AR", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Product selection row
        item {
            Column {
                Text(
                    text = "1. SELECCIONA EL LIENZO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProductType.values().forEach { type ->
                        val isSelected = state.selectedProduct == type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) CyberCyan.copy(alpha = 0.15f) else CyberSlate)
                                .border(
                                    2.dp,
                                    if (isSelected) CyberCyan else Color.Transparent,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { viewModel.selectProduct(type) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("product_tab_${type.name}")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = when (type) {
                                        ProductType.MUG_11OZ -> "☕"
                                        ProductType.MUG_15OZ -> "🥛"
                                        ProductType.POLO_CASUAL -> "👕"
                                        ProductType.DEPORTIVO_JERSEY -> "🏃"
                                        ProductType.FRAME_PROMO -> "🖼️"
                                    },
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = type.displayName.split(" ")[0],
                                    color = if (isSelected) TextWhite else TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recomendados "MIS DISEÑOS / SUGERENCIAS DE DISEÑO" section
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✨ MIS DISEÑOS COMPARTIDOS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        letterSpacing = 1.5.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElectricPink.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("PREDETERMINADOS", color = ElectricPink, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = "Toca un diseño curado para cargarlo en el mockup interactivo 3D.",
                    fontSize = 10.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))

                val presetsForProduct = when (state.selectedProduct) {
                    ProductType.MUG_11OZ, ProductType.MUG_15OZ -> listOf(
                        Triple("Vapor Noir", "Negro Profundo", Pair(0xFF10121A, "VAPOR WAVE")),
                        Triple("Electric Cyan", "Cian Brillo", Pair(0xFF00E5FF, "GLITCH")),
                        Triple("Street Dusk", "Naranja Atardecer", Pair(0xFFFF7300, "MUG CHASKNAP")),
                        Triple("Minimal Sand", "Crema Estilo", Pair(0xFFFFFFEA, "CLASSY"))
                    )
                    ProductType.POLO_CASUAL -> listOf(
                        Triple("Streetwear OG", "Negro Street", Pair(0xFF10121A, "CREW '26")),
                        Triple("Neon Splat", "Fucsia Vibrante", Pair(0xFFFF0D7B, "NEON BEAT")),
                        Triple("Vaporwave Sun", "Cian Brillo", Pair(0xFF00E5FF, "OUTRUN")),
                        Triple("Cyber Core", "Amarillo Metal", Pair(0xFFFFEA00, "CYBERPUNK"))
                    )
                    ProductType.DEPORTIVO_JERSEY -> listOf(
                        Triple("Real Batería", "Azul Royal", Pair(0xFF0A66C2, "BATERIA 10")),
                        Triple("Cyber Striker", "Fucsia Veloz", Pair(0xFFFF0D7B, "STRIKER 07")),
                        Triple("Shadow Ninja", "Negro Sombra", Pair(0xFF10121A, "SHADOW 99")),
                        Triple("Golden Goal", "Campeón Gold", Pair(0xFFFFEA00, "GOLD VALOR"))
                    )
                    ProductType.FRAME_PROMO -> listOf(
                        Triple("Class of 2026", "Crema Elegante", Pair(0xFFFFFFEA, "PROMO 2026")),
                        Triple("Midnight Frame", "Negro Retrato", Pair(0xFF0F111A, "SOUVENIR")),
                        Triple("Neon Promo", "Fucsia Grad", Pair(0xFFFF0D7B, "GRAD GROUP")),
                        Triple("Memorial Cian", "Cian Brillo", Pair(0xFF00E5FF, "HISTORY"))
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    presetsForProduct.forEach { (name, colorName, setup) ->
                        val (colorVal, textVal) = setup
                        val isCurrentlyApplied = state.chosenColorHex == colorVal && state.textOverlay == textVal
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentlyApplied) CyberSlate.copy(alpha = 0.5f) else CyberSlate
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                if (isCurrentlyApplied) ElectricPink else BorderGrey
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .width(135.dp)
                                .clickable {
                                    viewModel.updateColor(colorVal)
                                    viewModel.updateTextOverlay(textVal)
                                    viewModel.setCustomImage(null, null)
                                    Toast.makeText(context, "🎨 Diseño '$name' aplicado al Mockup 3D!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorVal))
                                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = colorName,
                                    fontSize = 9.sp,
                                    color = TextMuted
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "\"$textVal\"",
                                    fontSize = 9.sp,
                                    color = ElectricPink,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3D Canvas / AR View Area
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSlate),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .border(1.dp, BorderGrey, RoundedCornerShape(24.dp))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!state.isArMode) {
                        // 3D Studio Rendering Mode
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Gira con un arrastre. Doble toque para nivelar.",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (state.isAutoRotating) ElectricPink.copy(alpha = 0.15f) else MidnightOnyx)
                                        .border(
                                            width = 1.dp,
                                            color = if (state.isAutoRotating) ElectricPink else BorderGrey,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.toggleAutoRotate() }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("auto_rotate_button"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Autorenew,
                                        contentDescription = "Auto-Rotate",
                                        tint = if (state.isAutoRotating) ElectricPink else CyberCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (state.isAutoRotating) "GIRANDO" else "AUTO-ROTAR",
                                        color = if (state.isAutoRotating) ElectricPink else TextWhite,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            viewModel.updateRotation(state.rotationAngle + dragAmount.x * 0.5f)
                                        }
                                    }
                                    .pointerInput(Unit) {
                                        detectTapGestures(onDoubleTap = { viewModel.updateRotation(0f) })
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Real-time Vector 3D Engine simulation
                                SimulatedProduct3D(
                                    type = state.selectedProduct,
                                    backgroundHex = state.chosenColorHex,
                                    rotation = state.rotationAngle,
                                    scale = state.scaleFactor,
                                    stampText = state.textOverlay,
                                    textColorHex = state.textOverlayColorHex,
                                    sticker = if (state.customImageUri == null) PRESET_STICKERS[selectedStickerIndex] else null,
                                    customImageUri = state.customImageUri
                                )
                            }
                        }
                    } else {
                        // Realidad Aumentada Projector Simulator Panel
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Virtual camera background (street loft mock)
                            ArCameraBackgroundSimulation()

                            // Simulated spatial item floating with interactive placement controls
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            viewModel.updateArAngles(
                                                pitch = state.arPitch + dragAmount.y * 0.5f,
                                                yaw = state.arYaw + dragAmount.x * 0.5f
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(200.dp)
                                            .scale(state.arScale),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        SimulatedProduct3D(
                                            type = state.selectedProduct,
                                            backgroundHex = state.chosenColorHex,
                                            rotation = state.arYaw,
                                            scale = state.scaleFactor,
                                            stampText = state.textOverlay,
                                            textColorHex = state.textOverlayColorHex,
                                            sticker = if (state.customImageUri == null) PRESET_STICKERS[selectedStickerIndex] else null,
                                            customImageUri = state.customImageUri
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MidnightOnyx.copy(alpha = 0.82f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Masa Virtual: Chasknap AR Proyectado",
                                            color = CyberCyan,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // AR control sliders
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.ZoomIn, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Slider(
                                        value = state.arScale,
                                        onValueChange = { viewModel.updateArScale(it) },
                                        valueRange = 0.5f..2.0f,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = CyberCyan,
                                            activeTrackColor = CyberCyan,
                                            inactiveTrackColor = Color.DarkGray
                                        )
                                    )
                                    Text(
                                        text = "${(state.arScale * 100).toInt()}%",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        modifier = Modifier.width(36.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Customization Studio controls
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSlate),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. PERSONALIZACIÓN URBANA",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricPink,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Base Material Color picker
                    Text(text = "Color de Fondo del Lienzo", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        customColors.forEach { (colorHex, name) ->
                            val isChosen = state.chosenColorHex == colorHex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorHex))
                                    .border(
                                        width = 3.dp,
                                        color = if (isChosen) CyberCyan else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.updateColor(colorHex) }
                                    .testTag("color_button_${name.replace(" ", "_")}"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isChosen) {
                                    Icon(Icons.Filled.Check, contentDescription = "Active", tint = MidnightOnyx, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dynamic Stamp text field
                    Text(text = "Texto / Tag Estampado", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.textOverlay,
                        onValueChange = { viewModel.updateTextOverlay(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_overlay_text_field"),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedContainerColor = MidnightOnyx,
                            unfocusedContainerColor = MidnightOnyx,
                            focusedIndicatorColor = ElectricPink,
                            unfocusedIndicatorColor = BorderGrey
                        ),
                        placeholder = { Text("Escribe el nombre de tu promo o número", color = Color.Gray) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Preset stickers or Image upload selector
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Logo de Equipo / Sticker", fontSize = 11.sp, color = TextMuted)
                        TextButton(
                            onClick = { filePickerLauncher.launch("image/*") },
                            modifier = Modifier.testTag("upload_file_picker")
                        ) {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Subir JPG/PNG", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (state.customImageUri != null) {
                        // Display loaded user logo
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MidnightOnyx)
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.DarkGray)
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(state.customImageUri),
                                    contentDescription = "User uploaded",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.customImageFilename ?: "imagen_usuario.png",
                                    color = TextWhite,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text("Alineado en el centro del modelo 3D", color = TextMuted, fontSize = 10.sp)
                            }
                            IconButton(
                                onClick = { viewModel.setCustomImage(null, null) },
                                modifier = Modifier.testTag("clear_uploaded_image")
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color.Red)
                            }
                        }
                    } else {
                        // Offer local urban preset stickers instead
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            PRESET_STICKERS.forEachIndexed { idx, logo ->
                                val active = selectedStickerIndex == idx
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (active) WarningYellow.copy(alpha = 0.15f) else MidnightOnyx)
                                        .border(
                                            2.dp,
                                            if (active) WarningYellow else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onStickerSelected(idx) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Column {
                                        Text(text = logo.name, color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(text = logo.styleDesc, color = TextMuted, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action add to cart item
        item {
            Button(
                onClick = {
                    val label = "${state.selectedProduct.displayName} Custom"
                    val desc = if (state.customImageUri != null) "Cargado con archivo local" else "Estilo sticker: ${PRESET_STICKERS[selectedStickerIndex].name}"
                    viewModel.addToCart(
                        title = label,
                        subtitle = "$desc - Tonalidad: #${state.chosenColorHex.toString(16).takeLast(6).uppercase()}",
                        price = state.selectedProduct.basePrice,
                        count = 1
                    )
                    Toast.makeText(context, "🛒 ¡Añadido al Carrito Chasknap!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("add_custom_item_to_cart"),
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = MidnightOnyx),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.AddShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AÑADIR PERSONALIZACIÓN AL PEDIDO (S/. ${String.format("%.2f", state.selectedProduct.basePrice)})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // 4. TRABAJOS ENTREGADOS SHOWCASE / PORTAFOLIO DE TRABAJOS
        item {
            var showUploadDialog by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "📸 TRABAJOS ENTREGADOS REALES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningYellow,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Galería real de pedidos Chasknap entregados con éxito",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }

                    Button(
                        onClick = { showUploadDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricPink,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_upload_delivered_work")
                    ) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Subir Fotos", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (state.deliveredWorks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CyberSlate)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aún no has subido fotos de tus trabajos entregados.", color = TextMuted, fontSize = 11.sp)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        state.deliveredWorks.forEach { work ->
                            DeliveredWorkCardItem(
                                work = work,
                                viewModel = viewModel,
                                context = context
                            )
                        }
                    }
                }
            }

            // Beautiful interactive dialog for uploading delivered jobs
            if (showUploadDialog) {
                var inputTitle by remember { mutableStateOf("") }
                var inputDesc by remember { mutableStateOf("") }
                var inputCategory by remember { mutableStateOf("Tazas") }
                var inputDate by remember { mutableStateOf("2026-05-22") }
                var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }

                val singlePhotoLaunch = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    if (uri != null) {
                        selectedPhotoUri = uri
                    }
                }

                AlertDialog(
                    onDismissRequest = { showUploadDialog = false },
                    containerColor = CyberSlate,
                    titleContentColor = TextWhite,
                    textContentColor = TextMuted,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = WarningYellow)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pág. de Trabajo Entregado", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "Sube la foto del producto físico entregado y colócalo en tu portafolio público de Chasknap para tus clientes.",
                                fontSize = 10.sp,
                                color = TextMuted
                            )

                            // Image Picker Trigger Button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MidnightOnyx)
                                    .border(1.dp, BorderGrey, RoundedCornerShape(12.dp))
                                    .clickable { singlePhotoLaunch.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedPhotoUri != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(selectedPhotoUri),
                                        contentDescription = "Preview",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = CyberCyan)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Seleccionar Fotografía de Galería", color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Title input
                            OutlinedTextField(
                                value = inputTitle,
                                onValueChange = { inputTitle = it },
                                label = { Text("Título del Trabajo Entregado", color = TextMuted, fontSize = 11.sp) },
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedContainerColor = MidnightOnyx,
                                    unfocusedContainerColor = MidnightOnyx,
                                    focusedIndicatorColor = CyberCyan,
                                    unfocusedIndicatorColor = BorderGrey
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Desc input
                            OutlinedTextField(
                                value = inputDesc,
                                onValueChange = { inputDesc = it },
                                label = { Text("Detalle o Materiales", color = TextMuted, fontSize = 11.sp) },
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedContainerColor = MidnightOnyx,
                                    unfocusedContainerColor = MidnightOnyx,
                                    focusedIndicatorColor = CyberCyan,
                                    unfocusedIndicatorColor = BorderGrey
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Category selector
                            Column {
                                Text("Categoría:", color = TextMuted, fontSize = 11.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("Tazas", "Polos", "Camisetas", "Cuadros").forEach { cat ->
                                        val selected = inputCategory == cat
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (selected) ElectricPink else BorderGrey)
                                                .clickable { inputCategory = cat }
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(text = cat, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Date input
                            OutlinedTextField(
                                value = inputDate,
                                onValueChange = { inputDate = it },
                                label = { Text("Fecha de entrega", color = TextMuted, fontSize = 11.sp) },
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedContainerColor = MidnightOnyx,
                                    unfocusedContainerColor = MidnightOnyx,
                                    focusedIndicatorColor = CyberCyan,
                                    unfocusedIndicatorColor = BorderGrey
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (inputTitle.isNotBlank()) {
                                    val mockType = when (inputCategory) {
                                        "Tazas" -> ProductType.MUG_11OZ
                                        "Polos" -> ProductType.POLO_CASUAL
                                        "Camisetas" -> ProductType.DEPORTIVO_JERSEY
                                        else -> ProductType.FRAME_PROMO
                                    }
                                    viewModel.addDeliveredWork(
                                        title = inputTitle,
                                        description = inputDesc.ifBlank { "Modelo personalizado de alta fidelidad urban look." },
                                        category = inputCategory,
                                        date = inputDate,
                                        imageUri = selectedPhotoUri,
                                        mockType = mockType,
                                        mockColor = 0xFFFF0D7B,
                                        mockText = inputTitle.take(10).uppercase()
                                    )
                                    Toast.makeText(context, "✅ ¡Trabajo subido y guardado con éxito!", Toast.LENGTH_SHORT).show()
                                    showUploadDialog = false
                                } else {
                                    Toast.makeText(context, "Por favor indica un título para tu trabajo.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = MidnightOnyx)
                        ) {
                            Text("Guardar Portafolio", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUploadDialog = false }) {
                            Text("Cancelar", color = TextMuted)
                        }
                    }
                )
            }
        }
    }
}

// Vector 3D Renderer Simulator built from scratches on Compose Canvas
@Composable
fun SimulatedProduct3D(
    type: ProductType,
    backgroundHex: Long,
    rotation: Float,
    scale: Float,
    stampText: String,
    textColorHex: Long,
    sticker: UrbanSticker?,
    customImageUri: Uri?
) {
    val context = LocalContext.current
    val painter = if (customImageUri != null) rememberAsyncImagePainter(
        ImageRequest.Builder(context).data(customImageUri).build()
    ) else null

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val containerRadius = size.minDimension / 2.3f

        when (type) {
            ProductType.MUG_11OZ, ProductType.MUG_15OZ -> {
                // RENDER ROTATING CYLINDRICAL MUG
                val rad = 100f
                val h = if (type == ProductType.MUG_15OZ) 280f else 220f
                val radX = 140f
                val radY = 40f

                // Draw handle (projects based on sin rotation)
                val handleRotOffset = (rotation * Math.PI / 180f).toFloat()
                val handleX = center.x + radX * Math.cos(handleRotOffset.toDouble()).toFloat()
                val isHandleBehind = Math.sin(handleRotOffset.toDouble()) < 0

                if (isHandleBehind) {
                    drawMugHandle(this, h, center, handleX)
                }

                // Draw solid Mug Cylinder body
                val brushColors = listOf(
                    Color(backgroundHex),
                    Color(backgroundHex).copy(alpha = 0.85f),
                    Color(backgroundHex).copy(alpha = 0.6f),
                    Color.White.copy(alpha = 0.25f), // glossy light highlight
                    Color(backgroundHex)
                )
                val bodyBrush = Brush.linearGradient(
                    colors = brushColors,
                    start = Offset(center.x - radX, center.y),
                    end = Offset(center.x + radX, center.y)
                )

                // Fill main body rectangle
                drawRect(
                    brush = bodyBrush,
                    topLeft = Offset(center.x - radX, center.y - h / 2f),
                    size = Size(radX * 2f, h)
                )

                // Bottom Ellipse cover
                drawOval(
                    color = Color(backgroundHex),
                    topLeft = Offset(center.x - radX, center.y + h / 2f - radY),
                    size = Size(radX * 2f, radY * 2f)
                )

                // Fill Top ellipse cup rim
                drawOval(
                    brush = bodyBrush,
                    topLeft = Offset(center.x - radX, center.y - h / 2f - radY),
                    size = Size(radX * 2f, radY * 2f)
                )

                // Draw inner rim lip
                drawOval(
                    color = Color.Black.copy(alpha = 0.45f),
                    topLeft = Offset(center.x - radX + 8f, center.y - h / 2f - radY + 4f),
                    size = Size(radX * 2f - 16f, radY * 2f - 8f),
                    style = Stroke(width = 4f)
                )

                if (!isHandleBehind) {
                    drawMugHandle(this, h, center, handleX)
                }

                // Draw Customizable Wrap Texture mapped around cylinder
                val wrapXOffset = ((rotation % 360) / 360f) * radX * 4f
                val clipLeft = center.x - radX * 0.75f
                val clipWidth = radX * 1.5f

                // Render customizable sticker or text wrap simulated on surface
                // Using rotation to shift horizontally
                val wrapCenter = center.x - wrapXOffset % (radX * 3f) + (radX * 1.5f)

                if (wrapCenter in (center.x - radX)..(center.x + radX)) {
                    // Draw user stamp text
                    if (stampText.isNotEmpty()) {
                        drawCircle(
                            color = Color(textColorHex).copy(alpha = 0.15f),
                            radius = 35f,
                            center = Offset(wrapCenter, center.y)
                        )
                    }

                    // Render selected preset sticker
                    if (customImageUri == null && sticker != null) {
                        if (sticker.isChasknapLogo) {
                            drawChasknapBrushLogo(
                                drawScope = this,
                                center = Offset(wrapCenter, center.y),
                                sizeFactor = 100f,
                                color = sticker.primaryColor,
                                splatterColor = sticker.secondaryColor,
                                scaleX = 1f
                            )
                        } else {
                            drawCircle(
                                color = sticker.primaryColor.copy(alpha = 0.75f),
                                radius = 30f,
                                center = Offset(wrapCenter, center.y - 10f)
                            )
                            drawRoundRect(
                                color = sticker.secondaryColor,
                                topLeft = Offset(wrapCenter - 25f, center.y + 15f),
                                size = Size(50f, 15f),
                                cornerRadius = CornerRadius(4f)
                            )
                        }
                    }
                }
            }

            ProductType.POLO_CASUAL, ProductType.DEPORTIVO_JERSEY -> {
                // RENDER ROTATING BASE POLO/SHIRT
                val shirtAlphaAngle = (rotation * Math.PI / 180.0)
                val scaleXProj = Math.cos(shirtAlphaAngle).toFloat() // squish to simulate 3D rotation
                val flipFactor = if (Math.sin(shirtAlphaAngle) >= 0) 1f else -1f

                rotate(degrees = if (flipFactor < 0) 180f else 0f, pivot = center) {
                    // Draw sleeves
                    val sleeveBrush = Brush.linearGradient(
                        colors = listOf(Color(backgroundHex), Color(backgroundHex).copy(alpha = 0.7f))
                    )

                    val mainColor = Color(backgroundHex)

                    // Draw Sleeve Paths Left & Right
                    val p = Path().apply {
                        moveTo(center.x - 170f * scaleXProj, center.y - 150f)
                        lineTo(center.x - 220f * scaleXProj, center.y - 60f)
                        lineTo(center.x - 150f * scaleXProj, center.y - 20f)
                        lineTo(center.x - 110f * scaleXProj, center.y - 70f)
                        close()
                    }
                    drawPath(p, brush = sleeveBrush)

                    val p2 = Path().apply {
                        moveTo(center.x + 170f * scaleXProj, center.y - 150f)
                        lineTo(center.x + 220f * scaleXProj, center.y - 60f)
                        lineTo(center.x + 150f * scaleXProj, center.y - 20f)
                        lineTo(center.x + 110f * scaleXProj, center.y - 70f)
                        close()
                    }
                    drawPath(p2, brush = sleeveBrush)

                    // Draw Main Torso Body
                    drawRoundRect(
                        color = mainColor,
                        topLeft = Offset(center.x - 110f * scaleXProj, center.y - 120f),
                        size = Size(220f * scaleXProj, 270f),
                        cornerRadius = CornerRadius(16f)
                    )

                    // Draw Collar/Neck trim
                    drawOval(
                        color = if (type == ProductType.DEPORTIVO_JERSEY) WarningYellow else Color.Black.copy(alpha = 0.5f),
                        topLeft = Offset(center.x - 50f * scaleXProj, center.y - 135f),
                        size = Size(100f * scaleXProj, 30f)
                    )

                    // If Jersey PRO, draw gorgeous sport stripes on side
                    if (type == ProductType.DEPORTIVO_JERSEY) {
                        drawRect(
                            color = ElectricPink.copy(alpha = 0.82f),
                            topLeft = Offset(center.x - 85f * scaleXProj, center.y - 120f),
                            size = Size(15f * scaleXProj, 270f)
                        )
                        drawRect(
                            color = ElectricPink.copy(alpha = 0.82f),
                            topLeft = Offset(center.x + 70f * scaleXProj, center.y - 120f),
                            size = Size(15f * scaleXProj, 270f)
                        )
                    }

                    // Draw customizable sticker graphic overlay
                    val visualCenter = Offset(center.x, center.y - 10f)

                    if (customImageUri == null && sticker != null) {
                        // Drawing static vector simulation for presets
                        if (sticker.isChasknapLogo) {
                            drawChasknapBrushLogo(
                                drawScope = this,
                                center = visualCenter,
                                sizeFactor = 90f,
                                color = sticker.primaryColor,
                                splatterColor = sticker.secondaryColor,
                                scaleX = scaleXProj
                            )
                        } else if (sticker.isGraffiti) {
                            drawCircle(
                                color = sticker.primaryColor,
                                radius = 28f * scaleXProj,
                                center = visualCenter
                            )
                            drawRoundRect(
                                color = sticker.secondaryColor,
                                topLeft = Offset(visualCenter.x - 22f * scaleXProj, visualCenter.y + 12f),
                                size = Size(44f * scaleXProj, 14f),
                                cornerRadius = CornerRadius(4f)
                            )
                        } else if (sticker.isRetroFlame) {
                            val fPath = Path().apply {
                                moveTo(visualCenter.x - 25f * scaleXProj, visualCenter.y + 20f)
                                cubicTo(
                                    visualCenter.x - 15f * scaleXProj, visualCenter.y - 15f,
                                    visualCenter.x - 5f * scaleXProj, visualCenter.y - 30f,
                                    visualCenter.x, visualCenter.y - 45f
                                )
                                cubicTo(
                                    visualCenter.x + 5f * scaleXProj, visualCenter.y - 30f,
                                    visualCenter.x + 15f * scaleXProj, visualCenter.y - 15f,
                                    visualCenter.x + 25f * scaleXProj, visualCenter.y + 20f
                                )
                            }
                            drawPath(fPath, burgerGradient(sticker.primaryColor, sticker.secondaryColor))
                        } else {
                            // Crest shape
                            val cPath = Path().apply {
                                moveTo(visualCenter.x - 25f * scaleXProj, visualCenter.y - 25f)
                                lineTo(visualCenter.x + 25f * scaleXProj, visualCenter.y - 25f)
                                lineTo(visualCenter.x + 25f * scaleXProj, visualCenter.y + 10f)
                                cubicTo(
                                    visualCenter.x + 25f * scaleXProj, visualCenter.y + 25f,
                                    visualCenter.x, visualCenter.y + 35f,
                                    visualCenter.x, visualCenter.y + 35f
                                )
                                cubicTo(
                                    visualCenter.x, visualCenter.y + 35f,
                                    visualCenter.x - 25f * scaleXProj, visualCenter.y + 25f,
                                    visualCenter.x - 25f * scaleXProj, visualCenter.y + 10f
                                )
                                close()
                            }
                            drawPath(cPath, color = sticker.primaryColor)
                        }
                    }

                    // Render custom overlay text tag nicely centered
                    if (stampText.isNotEmpty()) {
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.6f),
                            topLeft = Offset(center.x - 70f * scaleXProj, center.y + 80f),
                            size = Size(140f * scaleXProj, 30f),
                            cornerRadius = CornerRadius(8f)
                        )
                    }
                }
            }

            ProductType.FRAME_PROMO -> {
                // RENDER ROTATING PROMO FRAME (CUADRO)
                val frameAlpha = (rotation * Math.PI / 180.0)
                val scaleX = Math.cos(frameAlpha).toFloat()

                val widthVal = 240f * scaleX
                val heightVal = 300f

                // Inner frame glass glow / gradient
                val glassBrush = Brush.linearGradient(
                    colors = listOf(CyberSlate, Color(backgroundHex).copy(alpha = 0.95f), MidnightOnyx)
                )

                // Render shadow
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.35f),
                    topLeft = Offset(center.x - widthVal / 2f + 12f, center.y - heightVal / 2f + 12f),
                    size = Size(widthVal, heightVal),
                    cornerRadius = CornerRadius(20f)
                )

                // Outer wood/metal framing
                drawRoundRect(
                    color = WarningYellow,
                    topLeft = Offset(center.x - widthVal / 2f, center.y - heightVal / 2f),
                    size = Size(widthVal, heightVal),
                    cornerRadius = CornerRadius(16f),
                    style = Stroke(width = 16f)
                )

                // Inner mat content
                drawRoundRect(
                    brush = glassBrush,
                    topLeft = Offset(center.x - widthVal / 2f + 8f, center.y - heightVal / 2f + 8f),
                    size = Size(widthVal - 16f, heightVal - 16f),
                    cornerRadius = CornerRadius(10f)
                )

                // Gold promo star crest logo in center
                val matItemCenter = Offset(center.x, center.y - 20f)
                if (customImageUri == null && sticker != null) {
                    if (sticker.isChasknapLogo) {
                        drawChasknapBrushLogo(
                            drawScope = this,
                            center = matItemCenter,
                            sizeFactor = 120f,
                            color = sticker.primaryColor,
                            splatterColor = sticker.secondaryColor,
                            scaleX = scaleX
                        )
                    } else {
                        drawCircle(
                            color = sticker.primaryColor,
                            radius = 45f * scaleX,
                            center = matItemCenter
                        )
                        drawRoundRect(
                            color = sticker.secondaryColor,
                            topLeft = Offset(matItemCenter.x - 30f * scaleX, matItemCenter.y + 12f),
                            size = Size(60f * scaleX, 15f),
                            cornerRadius = CornerRadius(4f)
                        )
                    }
                }

                if (stampText.isNotEmpty()) {
                    drawRect(
                        color = Color.Black.copy(alpha = 0.8f),
                        topLeft = Offset(center.x - 60f * scaleX, center.y + 70f),
                        size = Size(120f * scaleX, 24f)
                    )
                }
            }
        }
    }
}

// Helper methods for simulated 3D rendering
private fun drawMugHandle(scope: DrawScope, h: Float, center: Offset, handleX: Float) {
    scope.drawArc(
        color = Color.LightGray.copy(alpha = 0.88f),
        startAngle = -90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(Math.min(handleX, center.x + 30f), center.y - h / 3f),
        size = Size(60f, h * 0.66f),
        style = Stroke(width = 24f)
    )
    scope.drawArc(
        color = Color.DarkGray,
        startAngle = -90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(Math.min(handleX, center.x + 30f), center.y - h / 3f),
        size = Size(60f, h * 0.66f),
        style = Stroke(width = 10f)
    )
}

private fun burgerGradient(c1: Color, c2: Color) = Brush.verticalGradient(
    colors = listOf(c1, c2)
)

// Simulated AR background layout representing custom street view or cool rooms
@Composable
fun ArCameraBackgroundSimulation() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Render custom grid lines and street neon glow to represent AR calibration
                val space = 40.dp.toPx()
                for (x in 0..(size.width / space).toInt()) {
                    drawLine(
                        color = CyberCyan.copy(alpha = 0.15f),
                        start = Offset(x * space, 0f),
                        end = Offset(x * space, size.height),
                        strokeWidth = 1f
                    )
                }
                for (y in 0..(size.height / space).toInt()) {
                    drawLine(
                        color = CyberCyan.copy(alpha = 0.15f),
                        start = Offset(0f, y * space),
                        end = Offset(size.width, y * space),
                        strokeWidth = 1f
                    )
                }

                // Cyber cyan projection target circles
                drawCircle(
                    color = CyberCyan.copy(alpha = 0.4f),
                    radius = size.minDimension * 0.38f,
                    center = center,
                    style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.FilterCenterFocus,
                contentDescription = null,
                tint = WarningYellow.copy(alpha = 0.7f),
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "CAM - ENFOQUE AUTOMÁTICO CHASKNAP",
                color = TextWhite.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}


// SCREEN 2: TEAM & PROMO (Grupos, Tallas y Presupuestos)
@Composable
fun TeamPromoTab(viewModel: MainViewModel, state: AppState) {
    val context = LocalContext.current
    var inputName by remember { mutableStateOf("") }
    var inputNumber by remember { mutableStateOf("") }
    var inputSize by remember { mutableStateOf("M") }
    var inputGarment by remember { mutableStateOf(ProductType.DEPORTIVO_JERSEY) }

    val sizes = listOf("XS", "S", "M", "L", "XL", "XXL")

    // Stats calculated from viewModel
    val (baseTotal, discountTotal, finalTotal) = viewModel.calculateTotalAndDiscounts()
    val totalQty = state.members.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("team_promo_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Title Info
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Group, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gestor Team & Promo", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text(
                            "Arma tu lista de integrantes, números de camiseta y casacas promocionales en un instante.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Add Member Roster Quick Form Component
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSlate),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AÑADIR INTEGRANTE AL ROSTER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = CyberCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Nombre Completo") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_member_name"),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedContainerColor = MidnightOnyx,
                            unfocusedContainerColor = MidnightOnyx,
                            focusedIndicatorColor = CyberCyan,
                            unfocusedIndicatorColor = BorderGrey
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = inputNumber,
                            onValueChange = { inputNumber = it },
                            label = { Text("Camiseta #") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_member_number"),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedContainerColor = MidnightOnyx,
                                unfocusedContainerColor = MidnightOnyx,
                                focusedIndicatorColor = CyberCyan,
                                unfocusedIndicatorColor = BorderGrey
                            )
                        )

                        // Size chooser dropdown / horizontal boxes
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text("Talla", fontSize = 11.sp, color = TextMuted)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                sizes.forEach { sizeOption ->
                                    val isSelected = inputSize == sizeOption
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) CyberCyan else MidnightOnyx)
                                            .border(
                                                1.dp,
                                                if (isSelected) Color.Transparent else BorderGrey,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { inputSize = sizeOption },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = sizeOption,
                                            color = if (isSelected) MidnightOnyx else TextWhite,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Garment Selector Row
                    Text("Prenda Personalizada", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProductType.values().forEach { garment ->
                            val isChosen = inputGarment == garment
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isChosen) CyberCyan.copy(alpha = 0.15f) else MidnightOnyx)
                                    .border(
                                        1.dp,
                                        if (isChosen) CyberCyan else BorderGrey,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { inputGarment = garment }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = garment.displayName.split(" ")[0],
                                    fontSize = 10.sp,
                                    color = if (isChosen) TextWhite else TextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (inputName.isBlank()) {
                                Toast.makeText(context, "Por favor escribe un nombre.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.addMember(inputName, inputNumber, inputSize, inputGarment)
                            // Clear inputs
                            inputName = ""
                            inputNumber = ""
                            Toast.makeText(context, "✅ Miembro registrado", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = MidnightOnyx),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_register_member")
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Registar Atleta/Estudiante", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active members count & discount progress indicator badge list
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Métricas de Roster", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ElectricPink)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Desc: ${
                                    when {
                                        totalQty >= 30 -> "35% OFF"
                                        totalQty >= 15 -> "20% OFF"
                                        totalQty >= 6 -> "10% OFF"
                                        else -> "Precio Regular"
                                    }
                                }",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress indicators for next discount limit standard
                    val progress = when {
                        totalQty >= 30 -> 1.0f
                        totalQty >= 15 -> 0.5f + (totalQty - 15) / 30f
                        totalQty >= 6 -> 0.2f + (totalQty - 6) / 18f
                        else -> totalQty / 30f
                    }
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = CyberCyan,
                        trackColor = MidnightOnyx
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when {
                            totalQty < 6 -> "¡Faltan ${6 - totalQty} unidades para liberar 10% de descuento!"
                            totalQty in 6..14 -> "¡Faltan ${15 - totalQty} unidades para saltar al 20% OFF!"
                            totalQty in 15..29 -> "¡Agrega ${30 - totalQty} prendas más para reventar el 35% de Descuento Especial!"
                            else -> "¡Has desbloqueado el descuento definitivo de Mayorista Chasknap (35% OFF)!"
                        },
                        fontSize = 11.sp,
                        color = WarningYellow,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Active roster list row header
        item {
            Text(
                text = "LISTA DE ROSTER (${state.members.size} INTEGRANTES)",
                fontSize = 11.sp,
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // Dynamic listed items
        if (state.members.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Roster limpio. Comienza agregando a la directiva o jugadores.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(state.members) { member ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MidnightOnyx),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderGrey, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (member.shirtNumber.isNotEmpty()) member.shirtNumber else "#",
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = member.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Talla: ${member.size}", color = TextMuted, fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = member.garmentType.displayName.split(" ")[0],
                                    color = CyberCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.removeMember(member.id) },
                            modifier = Modifier.testTag("remove_member_${member.id}")
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = Color.LightGray)
                        }
                    }
                }
            }
        }

        // Total Pricing invoice review model
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSlate),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "RESUMEN DE COTIZACIÓN DE ROSTER",
                        color = ElectricPink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Prendas:", color = TextMuted, fontSize = 12.sp)
                        Text("$totalQty unidades", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal de Roster:", color = TextMuted, fontSize = 12.sp)
                        Text("S/. ${String.format("%.2f", baseTotal)}", color = TextWhite, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Descuentos de la Promo:", color = WarningYellow, fontSize = 12.sp)
                        Text("- S/. ${String.format("%.2f", discountTotal)}", color = WarningYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Divider(modifier = Modifier.padding(vertical = 10.dp), color = BorderGrey)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TOTAL NETO:", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        Text(
                            text = "S/. ${String.format("%.2f", finalTotal)}",
                            color = WarningYellow,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Batch Order Add to shopping card button
                    Button(
                        onClick = {
                            if (state.members.isEmpty()) {
                                Toast.makeText(context, "El roster está vacío.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.addToCart(
                                title = "Roster Grupal Chasknap (${state.members.size} prendas)",
                                subtitle = "Lista consolidada con descuento de volumen",
                                price = finalTotal,
                                count = 1
                            )
                            Toast.makeText(context, "🛒 ¡Roster Grupal añadido al Carrito!", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = MidnightOnyx),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_roster_to_cart")
                    ) {
                        Icon(Icons.Filled.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Añadir Todo el Roster al Carrito", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


// SCREEN 3: PROMOGRAD (Alquiler de Togas, Cuadros y Fotografía)
@Composable
fun PromoGradTab(viewModel: MainViewModel, state: AppState) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val togaColors = listOf("Negro Clásico", "Verde Esmeralda", "Azul Eléctrico", "Rojo Rubí", "Dorado Imperial Out")

    // Picker dialog for date setup
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, day: Int ->
            val finalDate = "$year-${month + 1}-$day"
            viewModel.updateTogaRental(
                color = state.togaRental.selectedColor,
                date = finalDate,
                name = state.togaRental.name,
                quantity = state.togaRental.quantity
            )
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("promograd_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(WarningYellow.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.CardMembership, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Catálogo PromoGrad '26", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text(
                            "Servicio integral de vestuario, galerías fotográficas para el recuerdo y togas de tela premium.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Togas rental form module
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSlate),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. ALQUILER DE TOGAS Y BIRRETES",
                        color = WarningYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Color de Estola y Detalles", color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        togaColors.forEach { col ->
                            val active = state.togaRental.selectedColor == col
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) WarningYellow.copy(alpha = 0.15f) else MidnightOnyx)
                                    .border(1.dp, if (active) WarningYellow else BorderGrey, RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.updateTogaRental(
                                            col,
                                            state.togaRental.rentalDate,
                                            state.togaRental.name,
                                            state.togaRental.quantity
                                        )
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = col, color = if (active) TextWhite else TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Date picker action button
                    Text("Fecha de Evento de Graduación", color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MidnightOnyx)
                            .clickable { datePickerDialog.show() }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Fecha: ${state.togaRental.rentalDate}", color = TextWhite, fontSize = 12.sp)
                        }
                        Text("CAMBIAR", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quantities
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cantidad de alumnos:", color = TextMuted, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                if (state.togaRental.quantity > 1) {
                                    viewModel.updateTogaRental(
                                        state.togaRental.selectedColor,
                                        state.togaRental.rentalDate,
                                        state.togaRental.name,
                                        state.togaRental.quantity - 1
                                    )
                                }
                            }) {
                                Icon(Icons.Filled.RemoveCircleOutline, contentDescription = null, tint = TextMuted)
                            }
                            Text(
                                text = state.togaRental.quantity.toString(),
                                color = TextWhite,
                                modifier = Modifier.padding(horizontal = 12.dp),
                                fontWeight = FontWeight.Black
                            )
                            IconButton(onClick = {
                                viewModel.updateTogaRental(
                                    state.togaRental.selectedColor,
                                    state.togaRental.rentalDate,
                                    state.togaRental.name,
                                    state.togaRental.quantity + 1
                                )
                            }) {
                                Icon(Icons.Filled.AddCircleOutline, contentDescription = null, tint = CyberCyan)
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = BorderGrey)

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("Cotización Alquiler", color = TextMuted, fontSize = 11.sp)
                            Text("S/. ${(state.togaRental.quantity * 22.00)}", color = WarningYellow, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                        Button(
                            onClick = {
                                viewModel.addToCart(
                                    title = "Alquiler: ${state.togaRental.quantity} Togas",
                                    subtitle = "Estola: ${state.togaRental.selectedColor} - Reservado: ${state.togaRental.rentalDate}",
                                    price = state.togaRental.quantity * 22.00,
                                    count = 1
                                )
                                Toast.makeText(context, "🎓 ¡Alquiler añadido al carrito!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarningYellow, contentColor = MidnightOnyx),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Añadir Alquiler", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Section 2 Photo Packages options
        item {
            Text(
                text = "2. SERVICIOS FOTOGRÁFICOS",
                fontSize = 11.sp,
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // Display photo packages
        val photoPackages = listOf(
            Triple("Paquete Oro Chasknap", "Álbum de fotos digital, 1 foto grupal + 2 retratos impresos con marco de madera premium", 99.00),
            Triple("Estudio Graduación PRO", "Sesión de estudio completa, maquillaje opcional, 3 fotos individuales, descarga de archivos en alta definición", 150.00)
        )

        items(photoPackages) { (title, features, price) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSlate),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderGrey),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = title, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = "S/. $price", color = WarningYellow, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = features, color = TextMuted, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            viewModel.addToCart(title, "Paquete fotográfico individual", price, 1)
                            Toast.makeText(context, "📸 ¡Servicio fotográfico añadido!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = MidnightOnyx),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Pedir Paquete", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


// SCREEN 4: TRACKING & DIRECT WHATSAPP SUPPORT CHAT
@Composable
fun TrackingTab(viewModel: MainViewModel, state: AppState) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var userMessageInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tracking_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tracker Dashboard Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Rastreador Chasknap", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text("Código de Pedido: ${state.activeTracking.trackingCode}", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Actualizado: ${state.activeTracking.lastUpdated}", color = CyberCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Step visualization vertical or inline
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TrackingStep.values().forEach { step ->
                            val currentIdx = state.activeTracking.currentStep.ordinal
                            val stepIdx = step.ordinal
                            val isCompleted = stepIdx <= currentIdx
                            val isActive = stepIdx == currentIdx

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isActive) ElectricPink else if (isCompleted) CyberCyan else Color.DarkGray
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = step.icon, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = step.stepName,
                                    color = if (isActive || isCompleted) TextWhite else TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Log status messages
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSlate),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "HISTORIAL DEL PEDIDO",
                        color = ElectricPink,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    state.activeTracking.messages.forEach { msg ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(text = "⚡", color = ElectricPink, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = msg, color = TextWhite, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Live Simulated Chat (Support Hub)
        item {
            Text(
                text = "CHAT SOPORTE WHATSAPP DIRECTO",
                fontSize = 11.sp,
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSlate),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Chat window messages box scrollable
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.chatHistory.forEach { (msg, isUser) ->
                                AlignMsgRow(message = msg, isUser = isUser)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Input box row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = userMessageInput,
                            onValueChange = { userMessageInput = it },
                            placeholder = { Text("Escribe una consulta (ej. precios, envios)...", color = Color.Gray, fontSize = 11.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("support_chat_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedContainerColor = MidnightOnyx,
                                unfocusedContainerColor = MidnightOnyx,
                                focusedIndicatorColor = CyberCyan,
                                unfocusedIndicatorColor = BorderGrey
                            )
                        )

                        IconButton(
                            onClick = {
                                if (userMessageInput.isNotBlank()) {
                                    viewModel.sendChatMessage(userMessageInput)
                                    userMessageInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CyberCyan)
                                .testTag("send_chat_msg_btn")
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = "Send", tint = MidnightOnyx, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlignMsgRow(message: String, isUser: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 240.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 0.dp,
                        bottomEnd = if (isUser) 0.dp else 12.dp
                    )
                )
                .background(if (isUser) CyberCyan else MidnightOnyx)
                .padding(10.dp)
        ) {
            Text(
                text = message,
                color = if (isUser) MidnightOnyx else TextWhite,
                fontSize = 11.sp,
                fontWeight = if (isUser) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}


// SHOPPING CART DRAWER OVERLAY
@Composable
fun CartDrawer(
    state: AppState,
    onClose: () -> Unit,
    onRemove: (String) -> Unit,
    onCheckout: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberSlate),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
            .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(2.dp, BorderGrey, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .testTag("cart_drawer_sheet")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Drag target icon / Row header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = CyberCyan)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "TU CANASTA URBAN",
                        color = TextWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.testTag("close_cart_button")) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = TextWhite)
                }
            }

            Divider(modifier = Modifier.padding(vertical = 10.dp), color = BorderGrey)

            // List of items inside cart drawer
            Box(modifier = Modifier.weight(1f)) {
                if (state.cartItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Tu canasta está limpia. Diseña o agrega del catálogo para despegar.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.cartItems) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MidnightOnyx)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = item.subtitle,
                                        color = TextMuted,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.quantity} x S/. ${String.format("%.2f", item.price)}",
                                        color = WarningYellow,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(onClick = { onRemove(item.id) }, modifier = Modifier.testTag("delete_cart_item_${item.id}")) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove", tint = Color.LightGray)
                                }
                            }
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 10.dp), color = BorderGrey)

            // Checkout invoice recap
            val finalPayable = state.cartItems.sumOf { it.price * it.quantity }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Estimado", color = TextMuted, fontSize = 11.sp)
                    Text(
                        text = "S/. ${String.format("%.2f", finalPayable)}",
                        color = WarningYellow,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Button(
                    onClick = onCheckout,
                    enabled = state.cartItems.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricPink,
                        disabledContainerColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("btn_cart_checkout")
                ) {
                    Icon(Icons.Filled.FlashOn, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PROCESAR PEDIDO", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// Custom brand drawing utilities for the official ink brush Chasknap Logo
@Composable
fun ChasknapLogo(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    splatterColor: Color = ElectricPink
) {
    Canvas(modifier = modifier) {
        drawChasknapBrushLogo(
            drawScope = this,
            center = Offset(size.width / 2f, size.height / 2f),
            sizeFactor = minOf(size.width, size.height),
            color = color,
            splatterColor = splatterColor,
            scaleX = 1f
        )
    }
}

fun drawChasknapBrushLogo(
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
    center: Offset,
    sizeFactor: Float,
    color: Color,
    splatterColor: Color,
    scaleX: Float = 1f
) {
    with(drawScope) {
        val minDim = sizeFactor
        val outerRadius = minDim * 0.36f
        val innerRadius = minDim * 0.20f

        // Outer brush arc stroke paint
        val strokePaint1 = Stroke(
            width = minDim * 0.10f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        // Outer arc is almost closed, open on the right (from -45 deg to 255 deg)
        drawArc(
            color = color,
            startAngle = -45f,
            sweepAngle = 295f,
            useCenter = false,
            topLeft = Offset(center.x - outerRadius * scaleX, center.y - outerRadius),
            size = Size(outerRadius * 2f * scaleX, outerRadius * 2f),
            style = strokePaint1
        )
        // Outer brush secondary bristle (slight offset thin neon blue line)
        drawArc(
            color = CyberCyan.copy(alpha = 0.8f),
            startAngle = -40f,
            sweepAngle = 285f,
            useCenter = false,
            topLeft = Offset(center.x - (outerRadius + 2f) * scaleX, center.y - (outerRadius + 2f)),
            size = Size((outerRadius + 2f) * 2f * scaleX, (outerRadius + 2f) * 2f),
            style = Stroke(width = minDim * 0.015f, cap = StrokeCap.Round)
        )
        // Secondary bristle white line
        drawArc(
            color = color.copy(alpha = 0.5f),
            startAngle = -50f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(center.x - (outerRadius - 2f) * scaleX, center.y - (outerRadius - 2f)),
            size = Size((outerRadius - 2f) * 2f * scaleX, (outerRadius - 2f) * 2f),
            style = Stroke(width = minDim * 0.01f, cap = StrokeCap.Round)
        )

        // Inner brush arc (C shape inside, open on the right)
        val strokePaint2 = Stroke(
            width = minDim * 0.07f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        drawArc(
            color = color,
            startAngle = -35f,
            sweepAngle = 245f,
            useCenter = false,
            topLeft = Offset(center.x - innerRadius * scaleX, center.y - innerRadius),
            size = Size(innerRadius * 2f * scaleX, innerRadius * 2f),
            style = strokePaint2
        )
        drawArc(
            color = CyberCyan.copy(alpha = 0.8f),
            startAngle = -30f,
            sweepAngle = 235f,
            useCenter = false,
            topLeft = Offset(center.x - (innerRadius - 1f) * scaleX, center.y - (innerRadius - 1f)),
            size = Size((innerRadius - 1f) * 2f * scaleX, (innerRadius - 1f) * 2f),
            style = Stroke(width = minDim * 0.012f, cap = StrokeCap.Round)
        )

        // Splatter star center location on the right side
        val splatterCenter = Offset(
            center.x + minDim * 0.20f * scaleX,
            center.y - minDim * 0.10f
        )

        // Multi-point splatter star / paint splatter
        val splatterPath = Path().apply {
            val numPoints = 8
            val outerLen = minDim * 0.11f
            val innerLen = minDim * 0.045f
            for (i in 0 until numPoints) {
                val angleRadStr = (i * 360f / numPoints) * (Math.PI / 180f)
                val angleRadMid = ((i + 0.5f) * 360f / numPoints) * (Math.PI / 180f)

                val x1 = (splatterCenter.x + Math.cos(angleRadStr) * outerLen * scaleX).toFloat()
                val y1 = (splatterCenter.y + Math.sin(angleRadStr) * outerLen).toFloat()

                val x2 = (splatterCenter.x + Math.cos(angleRadMid) * innerLen * scaleX).toFloat()
                val y2 = (splatterCenter.y + Math.sin(angleRadMid) * innerLen).toFloat()

                if (i == 0) {
                    moveTo(x1, y1)
                } else {
                    lineTo(x1, y1)
                }
                lineTo(x2, y2)
            }
            close()
        }
        drawPath(
            path = splatterPath,
            color = splatterColor
        )

        // Tiny splatter drops
        drawCircle(
            color = splatterColor,
            radius = minDim * 0.018f,
            center = Offset(splatterCenter.x + minDim * 0.11f * scaleX, splatterCenter.y + minDim * 0.04f)
        )
        drawCircle(
            color = splatterColor,
            radius = minDim * 0.014f,
            center = Offset(splatterCenter.x - minDim * 0.13f * scaleX, splatterCenter.y + minDim * 0.07f)
        )
        drawCircle(
            color = splatterColor,
            radius = minDim * 0.010f,
            center = Offset(splatterCenter.x + minDim * 0.02f * scaleX, splatterCenter.y - minDim * 0.12f)
        )
    }
}

// Custom delivered work card with interactive spring-based scale animation (bounce down on press, click to view mockup)
@Composable
fun DeliveredWorkCardItem(
    work: DeliveredWork,
    viewModel: MainViewModel,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = CyberSlate),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderGrey),
        modifier = modifier
            .width(220.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try {
                            awaitRelease()
                        } finally {
                            pressed = false
                        }
                    },
                    onTap = {
                        if (work.mockType != null) {
                            viewModel.selectProduct(work.mockType)
                            viewModel.updateColor(work.mockColor)
                            viewModel.updateTextOverlay(work.mockText)
                            if (work.imageUri != null) {
                                viewModel.setCustomImage(work.imageUri, work.title)
                            } else {
                                viewModel.setCustomImage(null, null)
                            }
                            Toast.makeText(context, "👀 ¡Visualizando diseño de este trabajo en el Mockup 3D!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (work.imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(work.imageUri),
                        contentDescription = work.title,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (work.mockType != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(work.mockColor), MidnightOnyx)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = when (work.mockType) {
                                    ProductType.MUG_11OZ, ProductType.MUG_15OZ -> "☕"
                                    ProductType.POLO_CASUAL -> "👕"
                                    ProductType.DEPORTIVO_JERSEY -> "🏃"
                                    ProductType.FRAME_PROMO -> "🖼️"
                                },
                                fontSize = 28.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = work.mockText,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Category Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ElectricPink)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = work.category,
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Saved / Delivered status tag
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberCyan)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = MidnightOnyx, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "ENTREGADO",
                            color = MidnightOnyx,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = work.title,
                    color = TextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = work.description,
                    color = TextMuted,
                    fontSize = 9.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(26.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fecha: ${work.date}",
                        color = TextMuted,
                        fontSize = 8.sp
                    )
                    Text(
                        text = "Ver Mockup ➔",
                        color = CyberCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

