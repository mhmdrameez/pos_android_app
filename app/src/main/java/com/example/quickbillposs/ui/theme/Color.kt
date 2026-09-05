package com.example.quickbillposs.ui.theme

import androidx.compose.ui.graphics.Color

// Primary POS Brand Colors (from web CSS theme)
val PosSteelBlue = Color(0xFF1E5790)        // --color-primary: #1e5790
val PosSteelBlueHover = Color(0xFF174A7A)   // --color-primary-hover: #174a7a
val PosSteelBlueDark = PosSteelBlueHover
val PosSteelBlueLight = Color(0xFF2E6BC6)

// Keypad & Workspace Backgrounds
val PosBgMain = Color(0xFFF5F6FA)           // body background: #f5f6fa
val PosBgSidebar = Color(0xFFFFFFFF)        // left sidebar background
val PosBgKeypadKey = Color(0xFFF0F1F4)      // --color-keypad: #f0f1f4
val PosBgKeypadKeyHover = Color(0xFFE3E6EB) // --color-keypad-hover: #e3e6eb
val PosBgCartPanel = Color(0xFFF0F1F4)
val PosBorder = Color(0xFFE3E6EB)

// Text Colors
val PosTextDark = Color(0xFF0F172A)
val PosTextMuted = Color(0xFF64748B)
val PosTextWhite = Color(0xFFFFFFFF)

// Accent Tints
val SuggestionChipBg = Color(0xFFEEF2FF)
val SuggestionChipText = Color(0xFF4F46E5)
val AccentGreen = Color(0xFF10B981)
val AccentRed = Color(0xFFEF4444)

// Legacy compatibility aliases
val PrimaryBlue = PosSteelBlue
val PrimaryBlueDark = PosSteelBlueDark
val PrimaryBlueLight = PosSteelBlueLight
val NavyBackground = Color(0xFF0F172A)
val NavySurface = Color(0xFF1E293B)
val NavyCard = Color(0xFF1A2340)
val NavyBorder = Color(0xFF2D3E5F)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = PosTextMuted
val TextOnPrimary = PosTextWhite
val LightBackground = PosBgMain
val LightSurface = Color(0xFFFFFFFF)
val LightCard = PosBgKeypadKey
val LightBorder = PosBorder
val LightTextPrimary = PosTextDark
val LightTextSecondary = PosTextMuted
