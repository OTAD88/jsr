package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Editorial Sunlit Paper & Cinematic Analog Film Palette (Light Theme)
val StoneBackdropLight = Color(0xFFD6C9BA)      // Sunlit warm stone background
val StoneBackdropMid = Color(0xFFA7998A)        // Mid-tone sand backdrop
val StoneBackdropShadow = Color(0xFF28221D)     // Deep cinematic diagonal shadow
val StoneBackdropDeep = Color(0xFF171412)       // Deepest filmic vignette black

val CreamPaper = Color(0xFFF4EDE3)              // Primary floating card surface
val CreamPaperWarm = Color(0xFFEBE2D6)          // Secondary subtle layer surface
val PeekingPanelSurface = Color(0xFFE1D8CC)     // Layered peeking card surface
val PureWhitePill = Color(0xFFFFFFFF)           // Elevated hero pill surface

val TaupePill = Color(0xFFDDD3C6)               // Soft collapsible stone pill
val TaupePillHover = Color(0xFFD1C6B7)          // Pressed/active stone pill
val SwatchStoneLight = Color(0xFFE2DBD1)        // Circular swatch light
val SwatchStoneMid = Color(0xFFCEC5B9)          // Circular swatch mid
val SwatchStoneDark = Color(0xFFB5AA9C)         // Circular swatch dark

val InkBlack = Color(0xFF131312)                // Solid CTA pill & primary ink
val InkCharcoal = Color(0xFF2B2825)             // Secondary dark ink
val StoneMutedText = Color(0xFF6F685F)          // Muted labels & metadata
val PaperBorder = Color(0x1F131312)             // Subtle 12% ink border
val PaperHighlight = Color(0x99FFFFFF)          // Crisp sunlit edge highlight

// Night Studio Paper & Moonlight Editorial Palette (Dark Theme)
val DarkBackdropBase = Color(0xFF12100E)
val DarkCardSurface = Color(0xFF1E1B18)
val DarkCardWarm = Color(0xFF282420)
val DarkPillSurface = Color(0xFF312C27)
val DarkPillElevated = Color(0xFF2D2824)
val DarkInkPrimary = Color(0xFFF4EDE3)
val DarkInkSecondary = Color(0xFFD0C6BA)
val DarkMutedText = Color(0xFFA69B8F)
val DarkBorderSubtle = Color(0x28F4EDE3)

// Brand & Tactile Earthy Pill Accents
val AmberEmblem = Color(0xFFF2B632)             // Warm sunlit amber emblem & active dot
val AmberLight = Color(0xFFFFD670)
val AmberContainer = Color(0x33F2B632)

val CobaltAccent = Color(0xFF1D4ED8)
val TerracottaAccent = Color(0xFF9F4726)        // Burnt terracotta pill / SOS accent
val TerracottaContainer = Color(0x269F4726)
val OchreAccent = Color(0xFFA76D2C)
val SageOliveAccent = Color(0xFF748654)         // Muted olive sage active link
val SageOliveContainer = Color(0x24748654)
val SlateDarkAccent = Color(0xFF232527)

// Compatibility Tokens
val CharcoalDark = StoneBackdropShadow
val CharcoalSurface = CreamPaper
val CharcoalCard = CreamPaperWarm
val CharcoalGlass = CreamPaper.copy(alpha = 0.94f)
val CharcoalGlassBorder = PaperBorder

val DeepOlive = PeekingPanelSurface
val DeepOliveCard = CreamPaperWarm

val MutedSage = SageOliveAccent
val MutedSageLight = SageOliveAccent
val MutedSageContainer = SageOliveContainer
val MutedSageText = InkBlack

val RadarCyan = SageOliveAccent
val RadarCyanLight = SageOliveAccent
val RadarCyanContainer = SageOliveContainer

val WarmAmber = AmberEmblem
val WarmAmberLight = OchreAccent
val WarmAmberContainer = AmberContainer

val FogGray = StoneMutedText
val FogGrayLight = StoneMutedText
val FogGrayMuted = SwatchStoneMid

val SoftOffWhite = InkBlack
val SoftOffWhiteMuted = InkCharcoal

val DangerMuted = TerracottaAccent
val DangerContainer = TerracottaContainer

val SuccessGreen = SageOliveAccent
