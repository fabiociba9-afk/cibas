package com.aistudio.executivogo.trnsp.ui.maps

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.aistudio.executivogo.trnsp.ui.NavigationHelper
import com.aistudio.executivogo.trnsp.ui.WhatsAppHelper
import com.aistudio.executivogo.trnsp.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * DriverMarkerData padroniza a informação que será enviada para o Mapa.
 */
data class DriverMarkerData(
    val id: String,
    val name: String,
    val phone: String,
    val vehicle: String,
    val plate: String,
    val lat: Double,
    val lng: Double,
    val speed: Double,
    val bearing: Double,
    val isOnline: Boolean,
    val inTrip: Boolean,
    val currentPassenger: String = "",
    val destination: String = "",
    val lastUpdate: Long = 0L
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleMapLiveView(
    drivers: List<DriverMarkerData>,
    selectedDriverId: String? = null,
    onDriverSelected: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    initialLat: Double = -23.55052,
    initialLng: Double = -46.633308,
    initialZoom: Int = 13,
    showDriverSelectorBar: Boolean = true
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapReady by remember { mutableStateOf(false) }
    var currentMapType by remember { mutableStateOf("street") } // street, satellite
    var isTrafficEnabled by remember { mutableStateOf(false) }
    var focusedDriver by remember { mutableStateOf<DriverMarkerData?>(null) }

    // Atualiza o driver focado quando selectedDriverId mudar externamente
    LaunchedEffect(selectedDriverId, drivers) {
        if (!selectedDriverId.isNullOrBlank()) {
            focusedDriver = drivers.find { it.id == selectedDriverId }
            focusedDriver?.let { d ->
                if (d.lat != 0.0 && d.lng != 0.0) {
                    webViewRef?.evaluateJavascript("if (window.panToDriver) { window.panToDriver('${d.id}'); }", null)
                }
            }
        }
    }

    // Serializa lista de motoristas em JSON para alimentar o mapa
    val driversJson = remember(drivers) {
        val jsonArray = JSONArray()
        drivers.forEach { d ->
            val obj = JSONObject().apply {
                put("id", d.id)
                put("name", d.name)
                put("phone", d.phone)
                put("vehicle", d.vehicle)
                put("plate", d.plate)
                put("lat", d.lat)
                put("lng", d.lng)
                put("speed", d.speed)
                put("bearing", d.bearing)
                put("isOnline", d.isOnline)
                put("inTrip", d.inTrip)
                put("currentPassenger", d.currentPassenger)
                put("destination", d.destination)
                put("lastUpdate", d.lastUpdate)
            }
            jsonArray.put(obj)
        }
        jsonArray.toString()
    }

    val quotedJson = remember(driversJson) {
        JSONObject.quote(driversJson)
    }

    // Injeta os dados atualizados em tempo real no mapa sem recarregar a página
    LaunchedEffect(quotedJson, isMapReady) {
        if (isMapReady && webViewRef != null) {
            webViewRef?.evaluateJavascript(
                "if (window.updateDrivers) { window.updateDrivers($quotedJson); }",
                null
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    webViewRef = this
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        setGeolocationEnabled(true)
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        
                        allowFileAccess = true
                        allowContentAccess = true
                        allowFileAccessFromFileURLs = true
                        allowUniversalAccessFromFileURLs = true
                        javaScriptCanOpenWindowsAutomatically = true
                    }

                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                            handler?.proceed() // Ignora erros de certificado SSL em mapas incorporados
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isMapReady = true
                            view?.evaluateJavascript("if (window.invalidateSize) { window.invalidateSize(); }", null)
                            view?.evaluateJavascript("if (window.updateDrivers) { window.updateDrivers($quotedJson); }", null)
                        }
                    }

                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onMarkerClicked(driverId: String) {
                            val found = drivers.find { it.id == driverId }
                            focusedDriver = found
                            onDriverSelected?.invoke(driverId)
                        }

                        @JavascriptInterface
                        fun openNavigationApp(lat: Double, lng: Double, label: String) {
                            val uri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                setPackage("com.google.android.apps.maps")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            try {
                                ctx.startActivity(intent)
                            } catch (_: Exception) {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                ctx.startActivity(webIntent)
                            }
                        }
                    }, "AndroidBridge")

                    loadDataWithBaseURL(
                        "https://www.openstreetmap.org/",
                        buildGoogleMapHtml(initialLat, initialLng, initialZoom),
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            },
            update = { wv ->
                webViewRef = wv
            }
        )

        // Barra de Controles Superiores do Mapa (Rua / Satélite, Trânsito, Centralizar)
        Surface(
            color = NavyDark.copy(alpha = 0.92f),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Alternar tipo de mapa (Esri Street / Esri Satellite)
                FilledTonalIconButton(
                    onClick = {
                        val nextType = if (currentMapType == "street") "satellite" else "street"
                        currentMapType = nextType
                        webViewRef?.evaluateJavascript("if (window.setMapType) { window.setMapType('$nextType'); }", null)
                    },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (currentMapType == "satellite") EmeraldAccent else NavyPrimary,
                        contentColor = if (currentMapType == "satellite") NavyDark else Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (currentMapType == "satellite") Icons.Default.Satellite else Icons.Default.Map,
                        contentDescription = "Alternar Satélite/Mapa",
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Trânsito em tempo real
                FilledTonalIconButton(
                    onClick = {
                        isTrafficEnabled = !isTrafficEnabled
                        webViewRef?.evaluateJavascript("if (window.toggleTraffic) { window.toggleTraffic($isTrafficEnabled); }", null)
                    },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (isTrafficEnabled) AmberWarning else NavyPrimary,
                        contentColor = if (isTrafficEnabled) NavyDark else Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Traffic,
                        contentDescription = "Trânsito em Tempo Real",
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Centralizar frota (Fit Bounds)
                FilledTonalIconButton(
                    onClick = {
                        webViewRef?.evaluateJavascript("if (window.fitAllDrivers) { window.fitAllDrivers(); }", null)
                    },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = NavyPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterCenterFocus,
                        contentDescription = "Centralizar Frota",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Badge indicador de conexão com Mapa
        Surface(
            color = NavyPrimary.copy(alpha = 0.9f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(EmeraldAccent)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Live Map • ${drivers.count { it.lat != 0.0 && it.lng != 0.0 }} Ativos",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Card Inferior com Detalhes do Motorista Selecionado
        AnimatedVisibility(
            visible = focusedDriver != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (showDriverSelectorBar) 68.dp else 12.dp, start = 12.dp, end = 12.dp)
        ) {
            focusedDriver?.let { driver ->
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (driver.inTrip) Color(0xFF0284C7) else EmeraldAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = driver.name.ifBlank { "Motorista Executivo" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = NavyPrimary
                                    )
                                    Text(
                                        text = "${driver.vehicle} • ${driver.plate}".trim(' ', '•'),
                                        fontSize = 12.sp,
                                        color = SlateTextSecondary
                                    )
                                }
                            }
                            IconButton(onClick = { focusedDriver = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = SlateTextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (driver.inTrip) Color(0xFFE0F2FE) else Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (driver.inTrip) "EM CORRIDA (${driver.currentPassenger})" else "DISPONÍVEL",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (driver.inTrip) Color(0xFF0369A1) else EmeraldDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Text(
                                text = "Velocidade: %.0f km/h".format(driver.speed),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (driver.phone.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { WhatsAppHelper.openWhatsApp(context, driver.phone, "Olá, ${driver.name}! Central ExecutivoGo em contato.") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF16A34A))
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    NavigationHelper.openGoogleMaps(context, "${driver.lat},${driver.lng}")
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Navegar até ele", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Seletor Horizontal de Motoristas no Rodapé
        if (showDriverSelectorBar && drivers.isNotEmpty()) {
            Surface(
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(drivers, key = { it.id }) { driver ->
                        val isSelected = focusedDriver?.id == driver.id
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) NavyPrimary else Color(0xFFF1F5F9),
                            modifier = Modifier.clickable {
                                focusedDriver = driver
                                onDriverSelected?.invoke(driver.id)
                                if (driver.lat != 0.0 && driver.lng != 0.0) {
                                    webViewRef?.evaluateJavascript(
                                        "if (window.panToDriver) { window.panToDriver('${driver.id}'); }",
                                        null
                                    )
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (driver.inTrip) Color(0xFF0284C7) else EmeraldAccent)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = driver.name.take(16),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else NavyPrimary
                                )
                                if (driver.speed > 1.0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "• %.0fkm/h".format(driver.speed),
                                        fontSize = 10.sp,
                                        color = if (isSelected) EmeraldLight else SlateTextSecondary
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

/**
 * Gera a página HTML do mapa utilizando Leaflet + Esri World Street Map e Esri World Imagery.
 */
private fun buildGoogleMapHtml(initialLat: Double, initialLng: Double, initialZoom: Int): String {
    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <title>Mapa ExecutivoGo</title>
    <style>
        html, body {
            height: 100%;
            height: 100vh;
            margin: 0;
            padding: 0;
            background-color: #e5e7eb;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
        }
        #map {
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            width: 100%;
            height: 100%;
        }
        .driver-label {
            background-color: #0F172A;
            color: #FFFFFF;
            font-size: 10px;
            font-weight: 700;
            padding: 2px 6px;
            border-radius: 4px;
            border: 1px solid #10B981;
            white-space: nowrap;
            box-shadow: 0 2px 4px rgba(0,0,0,0.4);
            transform: translate(-50%, -130%);
            position: absolute;
        }
        .driver-pin {
            width: 32px;
            height: 32px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            box-shadow: 0 3px 8px rgba(0,0,0,0.5);
            border: 2px solid #FFFFFF;
            cursor: pointer;
            transition: transform 0.2s ease;
        }
        .driver-pin:hover {
            transform: scale(1.15);
        }
        .pin-free { background-color: #10B981; }
        .pin-trip { background-color: #0284C7; }
        .pin-offline { background-color: #64748B; }
        .info-card {
            padding: 8px;
            font-family: sans-serif;
            color: #0F172A;
            max-width: 220px;
        }
        .info-card h4 {
            margin: 0 0 4px 0;
            font-size: 14px;
            color: #0F172A;
        }
        .info-card p {
            margin: 2px 0;
            font-size: 12px;
            color: #475569;
        }
        .info-card .status {
            display: inline-block;
            padding: 2px 6px;
            border-radius: 4px;
            font-size: 10px;
            font-weight: bold;
            margin-top: 4px;
        }
        .status-free { background: #DCFCE7; color: #047857; }
        .status-trip { background: #E0F2FE; color: #0369A1; }
    </style>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js"></script>
</head>
<body>
    <div id="map"></div>
    <script>
        var map;
        var markers = {};
        var trafficLayer = null;
        var currentMapType = 'street';
        
        var esriStreet = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}', {
            maxZoom: 19,
            attribution: '© Esri'
        });

        var esriSatellite = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
            maxZoom: 19,
            attribution: '© Esri'
        });
        
        var googleTraffic = L.tileLayer('https://mt1.google.com/vt/lyrs=m,traffic&x={x}&y={y}&z={z}', {
            maxZoom: 20,
            attribution: '© Google Maps Trânsito'
        });

        map = L.map('map', {
            center: [$initialLat, $initialLng],
            zoom: $initialZoom,
            zoomControl: false,
            layers: [esriStreet]
        });

        setTimeout(() => { map.invalidateSize(true); }, 200);
        setTimeout(() => { map.invalidateSize(true); }, 800);

        window.invalidateSize = function() {
            if (map) {
                map.invalidateSize(true);
            }
        };

        function createCarIcon(isOnline, inTrip, bearing) {
            var color = !isOnline ? '#64748B' : (inTrip ? '#0284C7' : '#10B981');
            var rotation = bearing || 0;
            var svgHtml = '<div style="transform: rotate(' + rotation + 'deg); transition: transform 0.4s ease;">' +
                '<svg width="34" height="34" viewBox="0 0 36 36" fill="none" xmlns="http://www.w3.org/2000/svg">' +
                '<circle cx="18" cy="18" r="16" fill="' + color + '" stroke="#FFFFFF" stroke-width="2.5" />' +
                '<path d="M18 7L24 23L18 20L12 23L18 7Z" fill="#FFFFFF" />' +
                '</svg>' +
                '</div>';
            return L.divIcon({
                html: svgHtml,
                className: '',
                iconSize: [34, 34],
                iconAnchor: [17, 17],
                popupAnchor: [0, -18]
            });
        }

        window.updateDrivers = function(jsonStr) {
            try {
                var list = JSON.parse(jsonStr);
                var activeIds = {};
                var bounds = [];

                list.forEach(function(driver) {
                    if (!driver.lat || !driver.lng || driver.lat === 0 || driver.lng === 0) {
                        return;
                    }

                    activeIds[driver.id] = true;
                    var latLng = [driver.lat, driver.lng];
                    bounds.push(latLng);

                    var icon = createCarIcon(driver.isOnline, driver.inTrip, driver.bearing);

                    var destHtml = driver.destination ? '<p><b>Destino:</b> ' + driver.destination + '</p>' : '';
                    var popupContent = '<div class="info-card">' +
                        '<h4>' + (driver.name || 'Motorista') + '</h4>' +
                        '<p><b>Veículo:</b> ' + (driver.vehicle || 'Executivo') + ' (' + (driver.plate || '') + ')</p>' +
                        '<p><b>Velocidade:</b> ' + Math.round(driver.speed || 0) + ' km/h</p>' +
                        destHtml +
                        '<span class="status ' + (driver.inTrip ? 'status-trip' : 'status-free') + '">' +
                        (driver.inTrip ? 'EM CORRIDA: ' + (driver.currentPassenger || '') : 'DISPONÍVEL') +
                        '</span>' +
                        '</div>';

                    if (markers[driver.id]) {
                        markers[driver.id].setLatLng(latLng);
                        markers[driver.id].setIcon(icon);
                        markers[driver.id].setPopupContent(popupContent);
                    } else {
                        var marker = L.marker(latLng, { icon: icon }).addTo(map);
                        marker.bindPopup(popupContent);
                        marker.on('click', function() {
                            if (window.AndroidBridge && window.AndroidBridge.onMarkerClicked) {
                                window.AndroidBridge.onMarkerClicked(driver.id);
                            }
                        });
                        markers[driver.id] = marker;
                    }
                });

                Object.keys(markers).forEach(function(id) {
                    if (!activeIds[id]) {
                        map.removeLayer(markers[id]);
                        delete markers[id];
                    }
                });

                if (bounds.length > 0) {
                    var latLngBounds = L.latLngBounds(bounds);
                    map.fitBounds(latLngBounds, { padding: [40, 40], maxZoom: 16 });
                } else {
                    map.setView([-23.55052, -46.633308], 12);
                }
            } catch (err) {
                console.error("Erro em updateDrivers:", err);
            }
        };

        window.panToDriver = function(driverId) {
            if (markers[driverId]) {
                var pos = markers[driverId].getLatLng();
                map.flyTo(pos, 16, { animate: true, duration: 1.0 });
                markers[driverId].openPopup();
            }
        };

        window.fitAllDrivers = function() {
            var coords = [];
            Object.keys(markers).forEach(function(id) {
                coords.push(markers[id].getLatLng());
            });
            if (coords.length > 0) {
                var bounds = L.latLngBounds(coords);
                map.fitBounds(bounds, { padding: [40, 40], maxZoom: 16 });
            }
        };

        window.setMapType = function(type) {
            currentMapType = type;
            map.removeLayer(esriStreet);
            map.removeLayer(esriSatellite);
            if (type === 'satellite') {
                map.addLayer(esriSatellite);
            } else {
                map.addLayer(esriStreet);
            }
        };

        window.toggleTraffic = function(enabled) {
            if (enabled) {
                map.addLayer(googleTraffic);
            } else {
                map.removeLayer(googleTraffic);
            }
        };
    </script>
</body>
</html>
    """.trimIndent()
}
