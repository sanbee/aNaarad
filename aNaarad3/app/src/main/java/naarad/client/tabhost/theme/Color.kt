/*
 * Copyright (C) 2026 Sanjay Bhatnagar
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should obtain a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package naarad.client.tabhost.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Fallback Material 3 Color Schemes for pre-Android 12 or when Dynamic Color is disabled
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF06B6D4),          // Cyan 500
    onPrimary = Color(0xFF0F172A),        // Dark Slate
    primaryContainer = Color(0xFF0891B2), // Cyan 600
    onPrimaryContainer = Color(0xFFECFEFF),
    secondary = Color(0xFF10B981),        // Emerald 500
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF059669),
    onSecondaryContainer = Color(0xFFECFDF5),
    tertiary = Color(0xFFF59E0B),         // Amber 500 (Lamp Glow)
    onTertiary = Color(0xFF0F172A),
    background = Color(0xFF0F172A),       // Slate 900
    onBackground = Color(0xFFF8FAFC),     // Slate 50
    surface = Color(0xFF1E293B),          // Slate 800
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),   // Slate 700
    onSurfaceVariant = Color(0xFFCBD5E1), // Slate 300
    outline = Color(0xFF64748B)           // Slate 500
)

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),          // Sky 600
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF059669),        // Emerald 600
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF047857),
    tertiary = Color(0xFFD97706),         // Amber 600
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),       // Slate 50
    onBackground = Color(0xFF0F172A),     // Slate 900
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),   // Slate 100
    onSurfaceVariant = Color(0xFF475569), // Slate 600
    outline = Color(0xFF94A3B8)           // Slate 400
)
