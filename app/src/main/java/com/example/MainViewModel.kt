package com.example

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Models representing Chasknap Hub core entities

enum class ProductType(val displayName: String, val basePrice: Double) {
    MUG_11OZ("Taza 11oz Standard", 12.00),
    MUG_15OZ("Taza Cerámica 15oz", 16.00),
    POLO_CASUAL("Polo Algodón Urban", 25.00),
    DEPORTIVO_JERSEY("Jersey Deportivo PRO", 35.00),
    FRAME_PROMO("Cuadro de Promoción Premium", 45.00)
}

data class GroupMember(
    val id: String,
    val name: String,
    val shirtNumber: String,
    val size: String,
    val garmentType: ProductType
)

data class CartItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val price: Double,
    val quantity: Int,
    val imageUrl: String? = null
)

data class TogaRental(
    val selectedColor: String = "Negro Clásico",
    val rentalDate: String = "2026-12-10",
    val name: String = "",
    val quantity: Int = 0
)

enum class TrackingStep(val stepName: String, val desc: String, val icon: String) {
    DESIGN("En Diseño", "Tu boceto urbano se está digitalizando", "✏️"),
    PROD("En Producción", "Imprimiendo y estampando tus tazas/prendas", "🔥"),
    READY("Listo para Entrega", "Empaquetado y listo para despegar", "📦"),
    ON_THE_WAY("En Camino", "Chasknap-rider en ruta con tus pedidos", "⚡")
}

data class OrderStatusState(
    val trackingCode: String,
    val currentStep: TrackingStep,
    val lastUpdated: String,
    val messages: List<String>
)

data class DeliveredWork(
    val id: String,
    val title: String,
    val description: String,
    val category: String, // "Tazas", "Polos", "Camisetas", "Cuadros"
    val date: String,
    val imageUri: Uri? = null,
    val mockType: ProductType? = null,
    val mockColor: Long = 0xFFEF007F,
    val mockText: String = "CHASKNAP"
)

data class AppState(
    // Personalizer module
    val selectedProduct: ProductType = ProductType.POLO_CASUAL,
    val customImageUri: Uri? = null,
    val customImageFilename: String? = null,
    val chosenColorHex: Long = 0xFFFF0D7B, // default hot neon magenta (no green)
    val scaleFactor: Float = 1.0f,
    val textOverlay: String = "CHASKNAP '26",
    val textOverlayColorHex: Long = 0xFFFFFFFF,
    val rotationAngle: Float = 0f,
    val isAutoRotating: Boolean = false,
    val isArMode: Boolean = false,
    val arPitch: Float = -15f,
    val arYaw: Float = 45f,
    val arScale: Float = 1f,

    // Group customization module (Team & Promo) starts empty
    val members: List<GroupMember> = emptyList(),

    // PromoGrad Togas & Photography Bookings
    val togaRental: TogaRental = TogaRental(),
    val hasPhotoBooking: Boolean = false,
    val photoPackageName: String = "",

    // Delivered Showcase Works (User uploaded & presets)
    val deliveredWorks: List<DeliveredWork> = listOf(
        DeliveredWork("d1", "Tazas Mate Urbanas - Promo San Marcos", "Cerámica mate con logo metalizado oro", "Tazas", "2026-04-12", mockType = ProductType.MUG_11OZ, mockColor = 0xFF10121A, mockText = "UNMSM '26"),
        DeliveredWork("d2", "Polos Street Oversized - Crew Chasknap", "Algodón pesado 20/1 con transfer táctil premium", "Polos", "2026-05-01", mockType = ProductType.POLO_CASUAL, mockColor = 0xFFFF7300, mockText = "CHASKNAP OG"),
        DeliveredWork("d3", "Jerseys de Fútbol - Real Batería FC", "Tecnología Dri-Fit con sublimado total digital", "Camisetas", "2026-05-18", mockType = ProductType.DEPORTIVO_JERSEY, mockColor = 0xFF0A66C2, mockText = "REAL BATERIA"),
        DeliveredWork("d4", "Cuadros de Graduación - Colegio Nacional", "Folleto con vidrio de alta resistencia", "Cuadros", "2026-05-20", mockType = ProductType.FRAME_PROMO, mockColor = 0xFFFFFFEA, mockText = "PROMO G-26")
    ),

    // Shopping Cart
    val cartItems: List<CartItem> = emptyList(),

    // Order tracking state
    val activeTracking: OrderStatusState = OrderStatusState(
        trackingCode = "CK-2026-993",
        currentStep = TrackingStep.PROD,
        lastUpdated = "12:05 PM",
        messages = listOf(
            "Tu pedido CK-2026-993 fue validado con éxito.",
            "¡El equipo de de diseño Chasknap le dio el visto bueno urbano!",
            "El pedido ingresó a los hornos de templado a las 10:45 AM."
        )
    ),

    // Chat interface
    val chatHistory: List<Pair<String, Boolean>> = listOf( // String to isUser
        "¡Ey! Bienvenido al soporte Chasknap. ¿Cómo va esa promo o equipo?" to false,
        "¿Pueden hacer envíos a provincias para pedidos masivos de casacas?" to true,
        "¡Obvio! Chasknap llega a todo el Perú por Olva, Shalom y Flores. Para grupos grandes, el envío es gratis." to false
    )
)

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppState())
    val uiState: StateFlow<AppState> = _uiState.asStateFlow()

    fun selectProduct(product: ProductType) {
        _uiState.update { it.copy(selectedProduct = product) }
    }

    fun setCustomImage(uri: Uri?, filename: String?) {
        _uiState.update { it.copy(customImageUri = uri, customImageFilename = filename) }
    }

    fun updateColor(colorHex: Long) {
        _uiState.update { it.copy(chosenColorHex = colorHex) }
    }

    fun updateScale(scale: Float) {
        _uiState.update { it.copy(scaleFactor = scale) }
    }

    fun updateTextOverlay(text: String) {
        _uiState.update { it.copy(textOverlay = text) }
    }

    fun updateTextOverlayColor(colorHex: Long) {
        _uiState.update { it.copy(textOverlayColorHex = colorHex) }
    }

    private var rotateJob: Job? = null

    fun toggleAutoRotate() {
        val nextState = !_uiState.value.isAutoRotating
        _uiState.update { it.copy(isAutoRotating = nextState) }
        rotateJob?.cancel()
        if (nextState) {
            rotateJob = viewModelScope.launch {
                while (true) {
                    _uiState.update { 
                        it.copy(rotationAngle = (it.rotationAngle + 1.5f) % 360f)
                    }
                    delay(16)
                }
            }
        }
    }

    fun stopAutoRotate() {
        if (_uiState.value.isAutoRotating) {
            _uiState.update { it.copy(isAutoRotating = false) }
            rotateJob?.cancel()
        }
    }

    fun updateRotation(angle: Float) {
        stopAutoRotate()
        _uiState.update { it.copy(rotationAngle = angle) }
    }

    fun toggleArMode(enabled: Boolean) {
        _uiState.update { it.copy(isArMode = enabled) }
    }

    fun updateArAngles(pitch: Float, yaw: Float) {
        _uiState.update { it.copy(arPitch = pitch, arYaw = yaw) }
    }

    fun updateArScale(scale: Float) {
        _uiState.update { it.copy(arScale = scale) }
    }

    // Member Management
    fun addMember(name: String, shirtNum: String, size: String, type: ProductType) {
        val newMember = GroupMember(
            id = System.currentTimeMillis().toString(),
            name = name,
            shirtNumber = shirtNum,
            size = size,
            garmentType = type
        )
        _uiState.update { it.copy(members = it.members + newMember) }
    }

    fun removeMember(id: String) {
        _uiState.update { it.copy(members = it.members.filterNot { m -> m.id == id }) }
    }

    // Toga Planner
    fun updateTogaRental(color: String, date: String, name: String, quantity: Int) {
        _uiState.update {
            it.copy(
                togaRental = TogaRental(color, date, name, quantity)
            )
        }
    }

    fun togglePhotoBooking(packageTitle: String, enable: Boolean) {
        _uiState.update {
            it.copy(
                hasPhotoBooking = enable,
                photoPackageName = if (enable) packageTitle else ""
            )
        }
    }

    // Pricing & Summary Logic
    val baseMemberPrice: Double = 18.00 // flat mock team rate before garment offset

    fun calculateTotalAndDiscounts(): Triple<Double, Double, Double> {
        val state = _uiState.value
        var itemTotal = 0.0

        // Calculate cost for members
        state.members.forEach { member ->
            itemTotal += member.garmentType.basePrice
        }

        // Togas & photo
        val togaCount = state.togaRental.quantity
        itemTotal += togaCount * 22.00 // toga rental fee
        if (state.hasPhotoBooking) {
            itemTotal += 150.0 // photography flat package
        }

        val totalUnits = state.members.size + (if (state.hasPhotoBooking) 1 else 0)

        // Volume Discount tiers
        val discountPercentage = when {
            totalUnits >= 30 -> 0.35 // 35% discount for major orders
            totalUnits >= 15 -> 0.20 // 20% discount
            totalUnits >= 6 -> 0.10  // 10% discount
            else -> 0.0
        }

        val discountAmount = itemTotal * discountPercentage
        val finalPrice = itemTotal - discountAmount

        return Triple(itemTotal, discountAmount, finalPrice)
    }

    // Shopping Cart Operations
    fun addToCart(title: String, subtitle: String, price: Double, count: Int, image: String? = null) {
        val currentItems = _uiState.value.cartItems.toMutableList()
        val index = currentItems.indexOfFirst { it.title == title && it.subtitle == subtitle }
        if (index != -1) {
            val existing = currentItems[index]
            currentItems[index] = existing.copy(quantity = existing.quantity + count)
        } else {
            currentItems.add(CartItem(System.currentTimeMillis().toString(), title, subtitle, price, count, image))
        }
        _uiState.update { it.copy(cartItems = currentItems) }
    }

    fun removeFromCart(id: String) {
        _uiState.update { it.copy(cartItems = it.cartItems.filterNot { item -> item.id == id }) }
    }

    fun clearCart() {
        _uiState.update { it.copy(cartItems = emptyList()) }
    }

    // Live chat simulator
    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val updatedHistory = _uiState.value.chatHistory + (text to true)
        _uiState.update { it.copy(chatHistory = updatedHistory) }

        // Simulated bot responses based on commands
        val botResponse = when {
            text.lowercase().contains("hola") || text.lowercase().contains("ey") -> {
                "¡Hey bro! 🤙 ¿Listo para redefinir el outfit de tu batería? Dime qué customizamos hoy."
            }
            text.lowercase().contains("precio") || text.lowercase().contains("descuento") -> {
                "¡Tenemos ofertazas urbanas! 🔥 A partir de 6 unidades tienes 10%, a partir de 15 un 20%, y si superas las 30 camisetas deportivas o tazas te aplicamos un **35% de descuento directo**."
            }
            text.lowercase().contains("toga") || text.lowercase().contains("gradu") -> {
                "En el módulo PromoGrad puedes coordinar togas de todos los colores urbanos, reservar fecha y hasta contratar fotógrafo profesional."
            }
            else -> {
                "¡Brutal! El equipo Chasknap está al tanto. Nos pondremos en contacto vía WhatsApp (+51 987 654 321) para cerrar los detalles de diseño."
            }
        }

        _uiState.update { it.copy(chatHistory = it.chatHistory + (botResponse to false)) }
    }

    // Add dynamic delivered user work
    fun addDeliveredWork(title: String, description: String, category: String, date: String, imageUri: Uri?, mockType: ProductType? = null, mockColor: Long = 0xFFEF007F, mockText: String = "CHASKNAP") {
        val newWork = DeliveredWork(
            id = System.currentTimeMillis().toString(),
            title = title,
            description = description,
            category = category,
            date = date,
            imageUri = imageUri,
            mockType = mockType ?: ProductType.POLO_CASUAL,
            mockColor = mockColor,
            mockText = mockText
        )
        _uiState.update { it.copy(deliveredWorks = listOf(newWork) + it.deliveredWorks) }
    }
}
