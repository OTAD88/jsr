package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.data.repository.StoreThemeItem

enum class CinematicCategory {
    INTERACTIVE_WORLD,
    CINEMATIC
}

enum class CinematicSceneType {
    ASTRONAUT,
    OCEAN_ODYSSEY,
    SKY_SUMMIT,
    NEON_METROPOLIS,
    STAR_DESERT,
    ASTRAL_EXPRESS,
    SAKURA_GARDEN,
    AURORA_PRINCESS,
    LUMINOUS_BUTTERFLIES,
    OCEAN_PEARL,
    ROYAL_ROSE,
    MOONLIGHT_CRESCENT,
    CRYSTAL_PRISM,
    LAVENDER_BREEZE,
    SUNSET_PLUME,
    COTTON_CLOUD,
    CYBER_NEON,
    OBSIDIAN_MAGMA,
    ARCTIC_STORM,
    SHADOW_SAMURAI,
    COSMIC_VORTEX,
    DEEP_TRENCH,
    DESERT_FALCON,
    TITANIUM_MECH,
    EMERALD_PHANTOM,
    THUNDER_APEX,
    SUNLIT_BEDROOM,
    OVERLAND_CAMP_TRUCK,
    FROST_CAMPFIRE,
    FOREST_CABIN,
    STUDY_LOFT,
    DEV_TERMINAL,
    GRAND_MUSEUM,
    FOOTBALL_STADIUM,
    FRUTIGER_AERO,
    SUNRISE_TREE_MOUNTAIN_WATER
}

enum class CinematicAvatarFrameStyle {
    ORBITAL_RING,
    PETAL_HALO,
    PRISM_JEWEL,
    CRESCENT_CROWN,
    CYBER_BEZEL,
    FORGED_HEX,
    FLAME_CREST,
    PEARL_DOUBLE
}

enum class CinematicBubbleStyle {
    GLASS_GLOW,
    ROYAL_SILK,
    CYBER_CHAMFER,
    COSMIC_RIM
}

@Immutable
data class CinematicThemeVariant(
    val bgTop: Color,
    val bgMid: Color,
    val bgBottom: Color,
    val cardSurface: Color,
    val cardSurfaceWarm: Color,
    val elevatedSurface: Color,
    val pillSurface: Color,
    val pillHover: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val mutedText: Color,
    val accent: Color,
    val iconColor: Color,
    val lightColor: Color,
    val border: Color,
    val highlight: Color,
    val framePrimary: Color,
    val frameSecondary: Color,
    val bubbleOutStart: Color,
    val bubbleOutEnd: Color,
    val bubbleInBg: Color,
    val bubbleBorder: Color
)

@Immutable
data class CinematicThemeSpec(
    val id: String,
    val category: CinematicCategory,
    val titleAr: String,
    val titleEn: String,
    val descAr: String,
    val descEn: String,
    val sceneType: CinematicSceneType,
    val frameStyle: CinematicAvatarFrameStyle,
    val bubbleStyle: CinematicBubbleStyle,
    val darkVariant: CinematicThemeVariant,
    val lightVariant: CinematicThemeVariant,
    val usesFrostLight: Boolean = false
) {
    fun toStoreThemeItem(): StoreThemeItem {
        val preview = darkVariant
        return StoreThemeItem(
            id = id,
            creatorId = "system",
            creatorName = "استوديو جسر",
            title = titleAr,
            description = descAr,
            backgroundHex = preview.bgMid.toHexLong(),
            iconHex = preview.accent.toHexLong(),
            lightHex = lightVariant.lightColor.toHexLong(),
            lightEnabled = true,
            priceUsd = 0,
            isPublished = true
        )
    }
}

private fun Color.toHexLong(): Long {
    val a = ((alpha * 255f).toInt() and 0xFF).toLong() shl 24
    val r = ((red * 255f).toInt() and 0xFF).toLong() shl 16
    val g = ((green * 255f).toInt() and 0xFF).toLong() shl 8
    val b = ((blue * 255f).toInt() and 0xFF).toLong()
    return a or r or g or b
}

private fun darkPack(
    bgTop: Long,
    bgMid: Long,
    bgBottom: Long,
    accent: Long,
    light: Long,
    frameSecondary: Long,
    cardSurface: Long = 0xFF131926L,
    cardWarm: Long = 0xFF1B2334L,
    elevated: Long = 0xFF202A3EL,
    pill: Long = 0xFF27334BL
) = CinematicThemeVariant(
    bgTop = Color(bgTop),
    bgMid = Color(bgMid),
    bgBottom = Color(bgBottom),
    cardSurface = Color(cardSurface).copy(alpha = 0.72f),
    cardSurfaceWarm = Color(cardWarm).copy(alpha = 0.80f),
    elevatedSurface = Color(elevated).copy(alpha = 0.88f),
    pillSurface = Color(pill).copy(alpha = 0.86f),
    pillHover = Color(pill),
    inkPrimary = Color(0xFFF6F9FF),
    inkSecondary = Color(0xFFDCE4F5),
    mutedText = Color(0xFFB3C0D9),
    accent = Color(accent),
    iconColor = Color(accent),
    lightColor = Color(light),
    border = Color(accent).copy(alpha = 0.44f),
    highlight = Color(light).copy(alpha = 0.42f),
    framePrimary = Color(accent),
    frameSecondary = Color(frameSecondary),
    bubbleOutStart = Color(accent),
    bubbleOutEnd = Color(frameSecondary),
    bubbleInBg = Color(elevated).copy(alpha = 0.92f),
    bubbleBorder = Color(accent).copy(alpha = 0.62f)
)

private fun lightPack(
    bgTop: Long,
    bgMid: Long,
    bgBottom: Long,
    accent: Long,
    light: Long,
    frameSecondary: Long,
    cardSurface: Long = 0xFFFCF9F5L,
    cardWarm: Long = 0xFFF3ECF0L,
    elevated: Long = 0xFFFFFFFFL,
    pill: Long = 0xFFECE6F0L,
    inkPrimary: Long = 0xFF161822L
) = CinematicThemeVariant(
    bgTop = Color(bgTop),
    bgMid = Color(bgMid),
    bgBottom = Color(bgBottom),
    cardSurface = Color(cardSurface).copy(alpha = 0.76f),
    cardSurfaceWarm = Color(cardWarm).copy(alpha = 0.84f),
    elevatedSurface = Color(elevated).copy(alpha = 0.90f),
    pillSurface = Color(pill).copy(alpha = 0.88f),
    pillHover = Color(pill),
    inkPrimary = Color(inkPrimary),
    inkSecondary = Color(0xFF252A38),
    mutedText = Color(0xFF474E61),
    accent = Color(accent),
    iconColor = Color(accent),
    lightColor = Color(light),
    border = Color(accent).copy(alpha = 0.36f),
    highlight = Color(0xFFFFFFFF).copy(alpha = 0.78f),
    framePrimary = Color(accent),
    frameSecondary = Color(frameSecondary),
    bubbleOutStart = Color(accent),
    bubbleOutEnd = Color(frameSecondary),
    bubbleInBg = Color(elevated).copy(alpha = 0.94f),
    bubbleBorder = Color(accent).copy(alpha = 0.52f)
)

object CinematicThemesCatalog {
    const val ASTRONAUT_ID = "cinematic_astronaut"
    const val SUNRISE_TREE_MOUNTAIN_WATER_ID = "cinematic_world_sunrise_tree_mountain_water"

    val allThemes: List<CinematicThemeSpec> = listOf(
        // Featured Interactive Camera Worlds (React dynamically as user enters Settings, Profile, Chats)
        CinematicThemeSpec(
            id = ASTRONAUT_ID,
            category = CinematicCategory.INTERACTIVE_WORLD,
            titleAr = "رائد الفضاء السماوي",
            titleEn = "Celestial Astronaut",
            descAr = "مظلم: يسبح بالفضاء وتنزل الكاميرا للأرض بالإعدادات • مضيء: أمام شمس سماوية تتغير زواياها",
            descEn = "Dark: Orbiting in space & descending to Earth in Settings • Light: Sky heavens before the Sun",
            sceneType = CinematicSceneType.ASTRONAUT,
            frameStyle = CinematicAvatarFrameStyle.ORBITAL_RING,
            bubbleStyle = CinematicBubbleStyle.COSMIC_RIM,
            darkVariant = darkPack(
                bgTop = 0xFF050814, bgMid = 0xFF0C1430, bgBottom = 0xFF141D42,
                accent = 0xFF58D5FF, light = 0xFFFFCA68, frameSecondary = 0xFF7B61FF,
                cardSurface = 0xFF0E1528, cardWarm = 0xFF151F38, elevated = 0xFF192646, pill = 0xFF213158
            ),
            lightVariant = lightPack(
                bgTop = 0xFF3E98E8, bgMid = 0xFF7FD0FF, bgBottom = 0xFFE3F5FF,
                accent = 0xFF0D71C7, light = 0xFFFFDF73, frameSecondary = 0xFF38BDF8,
                cardSurface = 0xFFF4FAFF, cardWarm = 0xFFE5F2FF, elevated = 0xFFFFFFFF, pill = 0xFFD8ECFF,
                inkPrimary = 0xFF0D213B
            )
        ),
        CinematicThemeSpec(
            id = "cinematic_world_frutiger_aero",
            category = CinematicCategory.INTERACTIVE_WORLD,
            titleAr = "فروتيجر آيرو الكلاسيكي",
            titleEn = "Frutiger Aero Glass",
            descAr = "سماء زرقاء زجاجية، تلال خضراء لامعة، كرات مائية وفقاعات متلألئة تنغمس فيها الكاميرا بالإعدادات",
            descEn = "Glossy 2000s aqua skies, lush emerald hills, crystal water orbs & floating bubbles across screens",
            sceneType = CinematicSceneType.FRUTIGER_AERO,
            frameStyle = CinematicAvatarFrameStyle.PEARL_DOUBLE,
            bubbleStyle = CinematicBubbleStyle.GLASS_GLOW,
            darkVariant = darkPack(
                bgTop = 0xFF041428, bgMid = 0xFF082E4C, bgBottom = 0xFF072B2E,
                accent = 0xFF2BF2FF, light = 0xFFB8FFFF, frameSecondary = 0xFF39F29E,
                cardSurface = 0xFF081F33, cardWarm = 0xFF0D2D45, elevated = 0xFF123B57, pill = 0xFF174A6B
            ),
            lightVariant = lightPack(
                bgTop = 0xFF169DF2, bgMid = 0xFF68E3FF, bgBottom = 0xFFE2FFF3,
                accent = 0xFF0086C9, light = 0xFFFFFAD2, frameSecondary = 0xFF18BF74,
                cardSurface = 0xFFF1FDFF, cardWarm = 0xFFE0F8FA, elevated = 0xFFFFFFFF, pill = 0xFFCFF3F8,
                inkPrimary = 0xFF072A3B
            )
        ),
        CinematicThemeSpec(
            id = "cinematic_world_abyssal_diver",
            category = CinematicCategory.INTERACTIVE_WORLD,
            titleAr = "غواص الأعماق الملكية",
            titleEn = "Abyssal Ocean Odyssey",
            descAr = "تتحرك الكاميرا من سطح الأمواج إلى قاع المحيط المضيء عند فتح الإعدادات وتنعكس بالملف الشخصي",
            descEn = "Camera dives from sunlit surface reefs down to glowing ocean trenches across screens",
            sceneType = CinematicSceneType.OCEAN_ODYSSEY,
            frameStyle = CinematicAvatarFrameStyle.PEARL_DOUBLE,
            bubbleStyle = CinematicBubbleStyle.GLASS_GLOW,
            darkVariant = darkPack(
                bgTop = 0xFF031526, bgMid = 0xFF062840, bgBottom = 0xFF031828,
                accent = 0xFF2BF0E0, light = 0xFF8CFFFA, frameSecondary = 0xFF3B8DFF,
                cardSurface = 0xFF062134, cardWarm = 0xFF0B3048, elevated = 0xFF103F5C, pill = 0xFF164F70
            ),
            lightVariant = lightPack(
                bgTop = 0xFF42C5E8, bgMid = 0xFF8CE7FA, bgBottom = 0xFFE4FBFF,
                accent = 0xFF037F99, light = 0xFFFFF4BD, frameSecondary = 0xFF26A9E0,
                cardSurface = 0xFFF2FCFF, cardWarm = 0xFFE2F7FC, elevated = 0xFFFFFFFF, pill = 0xFFD2F1F9,
                inkPrimary = 0xFF092A36
            )
        ),
        CinematicThemeSpec(
            id = "cinematic_world_sky_summit",
            category = CinematicCategory.INTERACTIVE_WORLD,
            titleAr = "نسر القمم السحابية",
            titleEn = "Alpine Sky Glider",
            descAr = "تنحدر الكاميرا من فوق الغيوم نحو القمم المثلجة والوادي المضيء مع دوران سينمائي ناعم",
            descEn = "Camera glides smoothly from cloud peaks down into alpine valleys as you navigate",
            sceneType = CinematicSceneType.SKY_SUMMIT,
            frameStyle = CinematicAvatarFrameStyle.ORBITAL_RING,
            bubbleStyle = CinematicBubbleStyle.GLASS_GLOW,
            darkVariant = darkPack(
                bgTop = 0xFF0B1426, bgMid = 0xFF1A2947, bgBottom = 0xFF10182B,
                accent = 0xFF75D0FF, light = 0xFFFFD88E, frameSecondary = 0xFF53E0B8,
                cardSurface = 0xFF131F36, cardWarm = 0xFF1C2C4A, elevated = 0xFF23385C, pill = 0xFF2C4570
            ),
            lightVariant = lightPack(
                bgTop = 0xFF6AB8F7, bgMid = 0xFFBDE3FF, bgBottom = 0xFFF7FBFF,
                accent = 0xFF1967B2, light = 0xFFFFEB99, frameSecondary = 0xFF4FA8E8,
                cardSurface = 0xFFF7FBFF, cardWarm = 0xFFEBF4FD, elevated = 0xFFFFFFFF, pill = 0xFFDCEBF9,
                inkPrimary = 0xFF10243D
            )
        ),
        CinematicThemeSpec(
            id = "cinematic_world_neon_metropolis",
            category = CinematicCategory.INTERACTIVE_WORLD,
            titleAr = "مدينة النيون المدارية",
            titleEn = "Neon Skybridge City",
            descAr = "تهبط الكاميرا من الأبراج المدارية إلى قلب الشوارع المضيئة بالنيون مع زاوية متغيرة",
            descEn = "Descending aerial camera drops from high skybridges straight into neon avenues",
            sceneType = CinematicSceneType.NEON_METROPOLIS,
            frameStyle = CinematicAvatarFrameStyle.CYBER_BEZEL,
            bubbleStyle = CinematicBubbleStyle.CYBER_CHAMFER,
            darkVariant = darkPack(
                bgTop = 0xFF08081A, bgMid = 0xFF161336, bgBottom = 0xFF0A1429,
                accent = 0xFF00F0FF, light = 0xFFFF59B4, frameSecondary = 0xFF9B6BFF,
                cardSurface = 0xFF111229, cardWarm = 0xFF1A1B3B, elevated = 0xFF22244D, pill = 0xFF2D3063
            ),
            lightVariant = lightPack(
                bgTop = 0xFFC5E4FF, bgMid = 0xFFE9F2FF, bgBottom = 0xFFFDF2FF,
                accent = 0xFF0077C2, light = 0xFFFFB8E6, frameSecondary = 0xFF6A4CE6,
                cardSurface = 0xFFF8FAFF, cardWarm = 0xFFEEF2FC, elevated = 0xFFFFFFFF, pill = 0xFFE0E7FA,
                inkPrimary = 0xFF121836
            )
        ),
        CinematicThemeSpec(
            id = "cinematic_world_star_desert",
            category = CinematicCategory.INTERACTIVE_WORLD,
            titleAr = "مرصد الصحراء النجمي",
            titleEn = "Celestial Dune Observatory",
            descAr = "تنزل العدسة من قبة درب التبانة إلى الكثبان الذهبية والواحة مع حركة محيطية بالملف الشخصي",
            descEn = "Camera descends from Milky Way heavens onto sculpted dunes & glowing desert oasis",
            sceneType = CinematicSceneType.STAR_DESERT,
            frameStyle = CinematicAvatarFrameStyle.CRESCENT_CROWN,
            bubbleStyle = CinematicBubbleStyle.COSMIC_RIM,
            darkVariant = darkPack(
                bgTop = 0xFF0B0A1A, bgMid = 0xFF1D1736, bgBottom = 0xFF291B12,
                accent = 0xFFFFC15E, light = 0xFFFFE59E, frameSecondary = 0xFF8C7DFF,
                cardSurface = 0xFF191529, cardWarm = 0xFF261F3B, elevated = 0xFF33294D, pill = 0xFF40345E
            ),
            lightVariant = lightPack(
                bgTop = 0xFFFFCF8A, bgMid = 0xFFFFE8C2, bgBottom = 0xFFFFF8EC,
                accent = 0xFFB86614, light = 0xFFFFF2B3, frameSecondary = 0xFFE0922B,
                cardSurface = 0xFFFFFBF4, cardWarm = 0xFFFBEFD8, elevated = 0xFFFFFFFF, pill = 0xFFF5E2C1,
                inkPrimary = 0xFF331F0A
            )
        ),
        CinematicThemeSpec(
            id = "cinematic_world_astral_express",
            category = CinematicCategory.INTERACTIVE_WORLD,
            titleAr = "قطار المجرة السريع",
            titleEn = "Astral Star-Rail Express",
            descAr = "سكة نجمية سماوية متحركة تنحني وتدور زاويتها بتناغم سينمائي عند التنقل بين الواجهات",
            descEn = "Luminous celestial viaduct glinting through clouds & stars with sweeping screen angles",
            sceneType = CinematicSceneType.ASTRAL_EXPRESS,
            frameStyle = CinematicAvatarFrameStyle.PRISM_JEWEL,
            bubbleStyle = CinematicBubbleStyle.COSMIC_RIM,
            darkVariant = darkPack(
                bgTop = 0xFF0A0D24, bgMid = 0xFF171E4A, bgBottom = 0xFF0D1433,
                accent = 0xFFA4B8FF, light = 0xFFFFDF8E, frameSecondary = 0xFF5EE7DF,
                cardSurface = 0xFF121738, cardWarm = 0xFF1B2252, elevated = 0xFF232C66, pill = 0xFF2D387D
            ),
            lightVariant = lightPack(
                bgTop = 0xFFCFE6FF, bgMid = 0xFFECF4FF, bgBottom = 0xFFFFF5E6,
                accent = 0xFF3A5CD4, light = 0xFFFFE89B, frameSecondary = 0xFF28A4CC,
                cardSurface = 0xFFF9FBFF, cardWarm = 0xFFEDF2FC, elevated = 0xFFFFFFFF, pill = 0xFFDFE8FA,
                inkPrimary = 0xFF141D42
            )
        ),
        CinematicThemeSpec(
            id = "cinematic_world_sunlit_bedroom",
            category = CinematicCategory.INTERACTIVE_WORLD,
            titleAr = "غرفة الشمس والقمر",
            titleEn = "Sunlit & Moonlit Room",
            descAr = "مضيء: غرفة دافئة يدخلها ضوء الشمس • مظلم: نور القمر والنجوم من النافذة مع انتقال نحو المكتب بالإعدادات",
            descEn = "Light: Sunbeams pouring across a cozy bedroom • Dark: Silver moonlight & window nook view in Settings",
            sceneType = CinematicSceneType.SUNLIT_BEDROOM,
            frameStyle = CinematicAvatarFrameStyle.PEARL_DOUBLE,
            bubbleStyle = CinematicBubbleStyle.ROYAL_SILK,
            darkVariant = darkPack(
                bgTop = 0xFF0A1024, bgMid = 0xFF161F42, bgBottom = 0xFF0E1329,
                accent = 0xFF9EBFFF, light = 0xFFFFDF94, frameSecondary = 0xFF6B85E8,
                cardSurface = 0xFF131A36, cardWarm = 0xFF1D264D, elevated = 0xFF263161, pill = 0xFF303E78
            ),
            lightVariant = lightPack(
                bgTop = 0xFFFFDDA8, bgMid = 0xFFFFF0D4, bgBottom = 0xFFFCF6EA,
                accent = 0xFFB86B1A, light = 0xFFFFF6B3, frameSecondary = 0xFFE39E4C,
                cardSurface = 0xFFFFFCF7, cardWarm = 0xFFFAF0DE, elevated = 0xFFFFFFFF, pill = 0xFFF3E2C3,
                inkPrimary = 0xFF2D1E0E
            )
        ),
        CinematicThemeSpec(
            id = "cinematic_world_overland_truck",
            category = CinematicCategory.INTERACTIVE_WORLD,
            titleAr = "تخييم الشاحنة والخيمة",
            titleEn = "Overland Camp & Truck",
            descAr = "مضيء: جلسة تخييم بجانب الشاحنة • مظلم: خيمة مضاءة ونوم تحت النجوم • الإعدادات: الكاميرا تدخل داخل الشاحنة!",
            descEn = "Light: Relaxing beside the overland truck • Dark: Glowing pitched camp tent • Settings: Camera moves inside the cab!",
            sceneType = CinematicSceneType.OVERLAND_CAMP_TRUCK,
            frameStyle = CinematicAvatarFrameStyle.FORGED_HEX,
            bubbleStyle = CinematicBubbleStyle.COSMIC_RIM,
            darkVariant = darkPack(
                bgTop = 0xFF080E1A, bgMid = 0xFF132030, bgBottom = 0xFF1C140C,
                accent = 0xFFFFA547, light = 0xFFFFDE8F, frameSecondary = 0xFF47C79E,
                cardSurface = 0xFF111A26, cardWarm = 0xFF1A2636, elevated = 0xFF233245, pill = 0xFF2E4057
            ),
            lightVariant = lightPack(
                bgTop = 0xFF74BBE8, bgMid = 0xFFD9EED9, bgBottom = 0xFFF5E6CC,
                accent = 0xFF2B6F4C, light = 0xFFFFE599, frameSecondary = 0xFFD17A2E,
                cardSurface = 0xFFFCF9F2, cardWarm = 0xFFF2EBDC, elevated = 0xFFFFFFFF, pill = 0xFFE6DDD0,
                inkPrimary = 0xFF1D241C
            )
        ),
        CinematicThemeSpec(
            id = "cinematic_world_frost_campfire",
            category = CinematicCategory.INTERACTIVE_WORLD,
            titleAr = "سقيع الجبل والنار",
            titleEn = "Frost Peak & Campfire",
            descAr = "ثلج وسقيع بدلاً من الشمس • مظلم: شخص يتدفأ بالنار في الجبل • الإعدادات: ينام على فراشه بجانب النار",
            descEn = "Snow & Frost light toggle • Dark: Warming by mountain campfire • Settings: Sleeping beside glowing fire embers",
            sceneType = CinematicSceneType.FROST_CAMPFIRE,
            frameStyle = CinematicAvatarFrameStyle.ORBITAL_RING,
            bubbleStyle = CinematicBubbleStyle.GLASS_GLOW,
            usesFrostLight = true,
            darkVariant = darkPack(
                bgTop = 0xFF071324, bgMid = 0xFF0F243D, bgBottom = 0xFF1F1612,
                accent = 0xFF7CE0FF, light = 0xFFE5FAFF, frameSecondary = 0xFFFF9842,
                cardSurface = 0xFF0D1D2E, cardWarm = 0xFF142B42, elevated = 0xFF1C3957, pill = 0xFF25486B
            ),
            lightVariant = lightPack(
                bgTop = 0xFFB8E2FA, bgMid = 0xFFE3F4FD, bgBottom = 0xFFF7FCFF,
                accent = 0xFF0D6E9E, light = 0xFFFFFFFF, frameSecondary = 0xFF4BB6E8,
                cardSurface = 0xFFF7FCFF, cardWarm = 0xFFE6F4FA, elevated = 0xFFFFFFFF, pill = 0xFFD6EBF5,
                inkPrimary = 0xFF0C2333
            )
        ),
        CinematicThemeSpec(
            id = "cinematic_world_forest_cabin",
            category = CinematicCategory.INTERACTIVE_WORLD,
            titleAr = "غابات الصنوبر والكوخ",
            titleEn = "Mystic Pine Forest",
            descAr = "غابة أشجار شاهقة ويراعات ليلية متألقة تقترب فيها الكاميرا نحو الكوخ الخشبي الدافئ",
            descEn = "Towering pine woods with fireflies where camera glides into a warm lantern-lit cabin",
            sceneType = CinematicSceneType.FOREST_CABIN,
            frameStyle = CinematicAvatarFrameStyle.PETAL_HALO,
            bubbleStyle = CinematicBubbleStyle.GLASS_GLOW,
            darkVariant = darkPack(
                bgTop = 0xFF06150E, bgMid = 0xFF0E291B, bgBottom = 0xFF081A10,
                accent = 0xFF52E595, light = 0xFFCFFF8E, frameSecondary = 0xFFFFB24D,
                cardSurface = 0xFF0B1F14, cardWarm = 0xFF123020, elevated = 0xFF19402C, pill = 0xFF22523A
            ),
            lightVariant = lightPack(
                bgTop = 0xFFB2E8B8, bgMid = 0xFFE2F7DF, bgBottom = 0xFFF6FBEF,
                accent = 0xFF1B7542, light = 0xFFFFF4AD, frameSecondary = 0xFF47B36E,
                cardSurface = 0xFFF7FCF6, cardWarm = 0xFFE5F4E3, elevated = 0xFFFFFFFF, pill = 0xFFD4EBD2,
                inkPrimary = 0xFF122919
            )
        ),
        CinematicThemeSpec(
            id = "cinematic_world_study_loft",
            category = CinematicCategory.INTERACTIVE_WORLD,
            titleAr = "طاولة المذاكرة الدافئة",
            titleEn = "Cozy Study Desk",
            descAr = "مكتبة وكتب ومصباح دراسة يركّز على صفحات الكتاب والملاحظات عند دخول الإعدادات",
            descEn = "Sunlit bookshelves & glowing night banker lamp focusing onto open study books in Settings",
            sceneType = CinematicSceneType.STUDY_LOFT,
            frameStyle = CinematicAvatarFrameStyle.CRESCENT_CROWN,
            bubbleStyle = CinematicBubbleStyle.ROYAL_SILK,
            darkVariant = darkPack(
                bgTop = 0xFF120F0C, bgMid = 0xFF241D15, bgBottom = 0xFF17120D,
                accent = 0xFFF0B85A, light = 0xFFFFE59E, frameSecondary = 0xFF5FB89A,
                cardSurface = 0xFF1A1510, cardWarm = 0xFF261F17, elevated = 0xFF33291F, pill = 0xFF423629
            ),
            lightVariant = lightPack(
                bgTop = 0xFFF2DFC4, bgMid = 0xFFF9EFE0, bgBottom = 0xFFFFFBF4,
                accent = 0xFF8C5318, light = 0xFFFFF2BD, frameSecondary = 0xFF2F7D5C,
                cardSurface = 0xFFFFFDF9, cardWarm = 0xFFF7EEDF, elevated = 0xFFFFFFFF, pill = 0xFFEDE0CB,
                inkPrimary = 0xFF291C0E
            )
        ),
        CinematicThemeSpec(
            id = "cinematic_world_dev_terminal",
            category = CinematicCategory.INTERACTIVE_WORLD,
            titleAr = "استوديو البرمجة الليلي",
            titleEn = "Developer Code Studio",
            descAr = "شاشات برمجة وأكواد متوهجة تتقدم نحوها الكاميرا في الإعدادات وسط أجواء تركيز احترافية",
            descEn = "Minimal coder desk & dual displays zooming cleanly into glowing code blocks in Settings",
            sceneType = CinematicSceneType.DEV_TERMINAL,
            frameStyle = CinematicAvatarFrameStyle.CYBER_BEZEL,
            bubbleStyle = CinematicBubbleStyle.CYBER_CHAMFER,
            darkVariant = darkPack(
                bgTop = 0xFF080C14, bgMid = 0xFF0F1726, bgBottom = 0xFF0A101A,
                accent = 0xFF38F2A6, light = 0xFF66D9FF, frameSecondary = 0xFF7B8BFF,
                cardSurface = 0xFF0E1420, cardWarm = 0xFF151E30, elevated = 0xFF1D2940, pill = 0xFF273654
            ),
            lightVariant = lightPack(
                bgTop = 0xFFD9E6F7, bgMid = 0xFFEFF4FB, bgBottom = 0xFFFAFBFD,
                accent = 0xFF0A6AD4, light = 0xFF9AE3FF, frameSecondary = 0xFF129963,
                cardSurface = 0xFFFAFCFF, cardWarm = 0xFFEAF0FA, elevated = 0xFFFFFFFF, pill = 0xFFDBE4F3,
                inkPrimary = 0xFF11192B
            )
        ),
        CinematicThemeSpec(
            id = "cinematic_world_grand_museum",
            category = CinematicCategory.INTERACTIVE_WORLD,
            titleAr = "المتحف الملكي الكبير",
            titleEn = "Grand Heritage Museum",
            descAr = "قاعات رخامية وأقواس كلاسيكية وإضاءة أثرية تدخل قبتها الدائرية بالإعدادات",
            descEn = "Marble colonnade & illuminated artifacts stepping under the central rotunda dome in Settings",
            sceneType = CinematicSceneType.GRAND_MUSEUM,
            frameStyle = CinematicAvatarFrameStyle.PRISM_JEWEL,
            bubbleStyle = CinematicBubbleStyle.ROYAL_SILK,
            darkVariant = darkPack(
                bgTop = 0xFF141118, bgMid = 0xFF241E2B, bgBottom = 0xFF17131C,
                accent = 0xFFE8C168, light = 0xFFFFECA6, frameSecondary = 0xFFB084F5,
                cardSurface = 0xFF1B1621, cardWarm = 0xFF282130, elevated = 0xFF352C40, pill = 0xFF453A52
            ),
            lightVariant = lightPack(
                bgTop = 0xFFEFE4D4, bgMid = 0xFFF7F0E4, bgBottom = 0xFFFFFBF5,
                accent = 0xFF9E6E19, light = 0xFFFFF4C7, frameSecondary = 0xFFC79B3B,
                cardSurface = 0xFFFFFDFA, cardWarm = 0xFFF5EDE0, elevated = 0xFFFFFFFF, pill = 0xFFEADEC9,
                inkPrimary = 0xFF292014
            )
        ),
        CinematicThemeSpec(
            id = "cinematic_world_football_stadium",
            category = CinematicCategory.INTERACTIVE_WORLD,
            titleAr = "ملعب الأبطال الرياضي",
            titleEn = "Champions Football Arena",
            descAr = "مدرجات وأضواء ملعب ساطعة وعشب أخضر تنزل فيه الكاميرا نحو نقطة الجزاء بالإعدادات",
            descEn = "Floodlit football arena dropping from panoramic stands onto the center pitch spot in Settings",
            sceneType = CinematicSceneType.FOOTBALL_STADIUM,
            frameStyle = CinematicAvatarFrameStyle.FORGED_HEX,
            bubbleStyle = CinematicBubbleStyle.CYBER_CHAMFER,
            darkVariant = darkPack(
                bgTop = 0xFF07111F, bgMid = 0xFF0E2438, bgBottom = 0xFF082414,
                accent = 0xFF3DF584, light = 0xFFFFFFFF, frameSecondary = 0xFF47BFFF,
                cardSurface = 0xFF0B1A26, cardWarm = 0xFF12293B, elevated = 0xFF19384F, pill = 0xFF224A66
            ),
            lightVariant = lightPack(
                bgTop = 0xFF7FD0FF, bgMid = 0xFFD9F7E4, bgBottom = 0xFFEFFCF3,
                accent = 0xFF128244, light = 0xFFFFF4B8, frameSecondary = 0xFF2499D6,
                cardSurface = 0xFFF7FCF9, cardWarm = 0xFFE5F6EB, elevated = 0xFFFFFFFF, pill = 0xFFD4EDDE,
                inkPrimary = 0xFF0E2919
            )
        ),
        CinematicThemeSpec(
            id = SUNRISE_TREE_MOUNTAIN_WATER_ID,
            category = CinematicCategory.INTERACTIVE_WORLD,
            titleAr = "الشجرة والماء والجبل عند الشروق",
            titleEn = "Tree, Water & Mountain at Sunrise",
            descAr = "منظر شروق الشمس الذهبي خلف الجبال وانعكاسه على البحيرة والشجرة الهادئة مع حركة كاميرا سينمائية",
            descEn = "Golden sunrise over tranquil mountain lake and shoreline tree with dynamic parallax camera depth",
            sceneType = CinematicSceneType.SUNRISE_TREE_MOUNTAIN_WATER,
            frameStyle = CinematicAvatarFrameStyle.CRESCENT_CROWN,
            bubbleStyle = CinematicBubbleStyle.ROYAL_SILK,
            darkVariant = darkPack(
                bgTop = 0xFF0B1524, bgMid = 0xFF1B2436, bgBottom = 0xFF141820,
                accent = 0xFFFFA94D, light = 0xFFFFE59E, frameSecondary = 0xFF53B89E,
                cardSurface = 0xFF101824, cardWarm = 0xFF1A2230, elevated = 0xFF232E40, pill = 0xFF2E3C52
            ),
            lightVariant = lightPack(
                bgTop = 0xFFFFC978, bgMid = 0xFFFFE8B8, bgBottom = 0xFFF2F7E9,
                accent = 0xFFD96E18, light = 0xFFFFF5C2, frameSecondary = 0xFF2F8A4E,
                cardSurface = 0xFFFFFCF6, cardWarm = 0xFFFFF1DC, elevated = 0xFFFFFFFF, pill = 0xFFF7E5CB,
                inkPrimary = 0xFF24170B
            )
        ),

        // ================= UNIFIED CINEMATIC THEMES (MERGED) =================
        CinematicThemeSpec(
            id = "cinematic_girl_sakura",
            category = CinematicCategory.CINEMATIC,
            titleAr = "حديقة الساكورا",
            titleEn = "Sakura Garden",
            descAr = "أزهار الكرز المتساقطة مع إطار بتلات وردية وتصميم رسائل حريري",
            descEn = "Drifting cherry blossom petals with rose-petal avatar halo and silk bubbles",
            sceneType = CinematicSceneType.SAKURA_GARDEN,
            frameStyle = CinematicAvatarFrameStyle.PETAL_HALO,
            bubbleStyle = CinematicBubbleStyle.ROYAL_SILK,
            darkVariant = darkPack(0xFF1A0D1B, 0xFF2B142C, 0xFF150A18, 0xFFFF7EB6, 0xFFFFCBE3, 0xFFB84C8C, 0xFF201122, 0xFF2D1730, 0xFF361D39, 0xFF422446),
            lightVariant = lightPack(0xFFFFD8E8, 0xFFFFEAF2, 0xFFFFF6F9, 0xFFD9417B, 0xFFFFF1A8, 0xFFFF88B5, 0xFFFFF8FB, 0xFFFFEBF3, 0xFFFFFFFF, 0xFFFFDCE9, 0xFF341222)
        ),
        CinematicThemeSpec(
            id = "cinematic_girl_aurora",
            category = CinematicCategory.CINEMATIC,
            titleAr = "أميرة الشفق القطبي",
            titleEn = "Aurora Princess",
            descAr = "أمواج شفق متحركة بالبنفسجي والفيروزي مع إطار جوهرة وإشعاع قطبي",
            descEn = "Flowing emerald-violet aurora ribbons with faceted diamond avatar frame",
            sceneType = CinematicSceneType.AURORA_PRINCESS,
            frameStyle = CinematicAvatarFrameStyle.PRISM_JEWEL,
            bubbleStyle = CinematicBubbleStyle.GLASS_GLOW,
            darkVariant = darkPack(0xFF071324, 0xFF102542, 0xFF1C1642, 0xFF4CF0C8, 0xFFB28DFF, 0xFF8C6EFF),
            lightVariant = lightPack(0xFFD2F3FF, 0xFFE9EEFF, 0xFFF7F2FF, 0xFF0B9D8F, 0xFFFFDDF5, 0xFF8C5CE6, 0xFFF7FCFF, 0xFFEAF3FF, 0xFFFFFFFF, 0xFFE0ECFF, 0xFF112338)
        ),
        CinematicThemeSpec(
            id = "cinematic_girl_butterflies",
            category = CinematicCategory.CINEMATIC,
            titleAr = "فراشات الغسق المضيئة",
            titleEn = "Luminous Butterflies",
            descAr = "فراشات متألقة تطير في الخلفية مع إطار لؤلؤي مزدوج ورسائل ناعمة",
            descEn = "Fluttering luminous butterflies with pearl-braided avatar frame",
            sceneType = CinematicSceneType.LUMINOUS_BUTTERFLIES,
            frameStyle = CinematicAvatarFrameStyle.PEARL_DOUBLE,
            bubbleStyle = CinematicBubbleStyle.ROYAL_SILK,
            darkVariant = darkPack(0xFF160B24, 0xFF281542, 0xFF130A22, 0xFFC882FF, 0xFFFFD88E, 0xFFFF79B0, 0xFF1C102E, 0xFF271740, 0xFF311D4E, 0xFF3D255F),
            lightVariant = lightPack(0xFFEDE0FF, 0xFFF7EEFF, 0xFFFFF4FB, 0xFF8D44D8, 0xFFFFDF9B, 0xFFFF70AC, 0xFFFCF8FF, 0xFFF3E8FF, 0xFFFFFFFF, 0xFFEBD9FF, 0xFF291442)
        ),
        CinematicThemeSpec(
            id = "cinematic_girl_mermaid",
            category = CinematicCategory.CINEMATIC,
            titleAr = "لؤلؤة الأعماق",
            titleEn = "Ocean Pearl",
            descAr = "لآلئ صاعدة وأشعة مائية حالمة مع إطار لؤلؤي لامع ورسائل زجاجية",
            descEn = "Shimmering pearl currents and sea caustics with iridescent pearl frame",
            sceneType = CinematicSceneType.OCEAN_PEARL,
            frameStyle = CinematicAvatarFrameStyle.PEARL_DOUBLE,
            bubbleStyle = CinematicBubbleStyle.GLASS_GLOW,
            darkVariant = darkPack(0xFF041A24, 0xFF0A2E3B, 0xFF071F2C, 0xFF47E5DA, 0xFFA4FFF7, 0xFF3B8DFF, 0xFF08222E, 0xFF0E303F, 0xFF133E50, 0xFF194B5F),
            lightVariant = lightPack(0xFFC2F5FA, 0xFFE2FBFF, 0xFFF4FEFF, 0xFF0A8F99, 0xFFFFF4D2, 0xFF3BA6D9, 0xFFF6FEFF, 0xFFE4F8FB, 0xFFFFFFFF, 0xFFD5F2F7, 0xFF0C2A33)
        ),
        CinematicThemeSpec(
            id = "cinematic_girl_rose",
            category = CinematicCategory.CINEMATIC,
            titleAr = "القصر الملكي الوردي",
            titleEn = "Royal Velvet Rose",
            descAr = "بتلات ورد ملكي ذهبية مع إطار هلال وزهور ورسائل ملكية فاخرة",
            descEn = "Velvet rose geometry with rose-gold filigree avatar frame",
            sceneType = CinematicSceneType.ROYAL_ROSE,
            frameStyle = CinematicAvatarFrameStyle.PETAL_HALO,
            bubbleStyle = CinematicBubbleStyle.ROYAL_SILK,
            darkVariant = darkPack(0xFF210811, 0xFF3A0F1F, 0xFF19060D, 0xFFFF6B8B, 0xFFFFD4A8, 0xFFD99B38, 0xFF260C16, 0xFF361221, 0xFF42172A, 0xFF521E35),
            lightVariant = lightPack(0xFFFFD6DF, 0xFFFFEDF1, 0xFFFFF8F5, 0xFFC72B59, 0xFFFFE7AE, 0xFFE5A43B, 0xFFFFF9FA, 0xFFFFEBEF, 0xFFFFFFFF, 0xFFFFDCE4, 0xFF350E1B)
        ),
        CinematicThemeSpec(
            id = "cinematic_girl_moon",
            category = CinematicCategory.CINEMATIC,
            titleAr = "سيدة الهلال الفضي",
            titleEn = "Moonlight Crescent",
            descAr = "هلال متوهج وغبار نجوم ناعم مع إطار تاج قمري ومزاج حالم",
            descEn = "Luminous crescent moon and stardust with celestial crescent halo",
            sceneType = CinematicSceneType.MOONLIGHT_CRESCENT,
            frameStyle = CinematicAvatarFrameStyle.CRESCENT_CROWN,
            bubbleStyle = CinematicBubbleStyle.COSMIC_RIM,
            darkVariant = darkPack(0xFF0A1026, 0xFF141E42, 0xFF0D142E, 0xFFB8C9FF, 0xFFFFE59E, 0xFF7D9BFF, 0xFF111834, 0xFF19234A, 0xFF212E5E, 0xFF2A3A73),
            lightVariant = lightPack(0xFFD6E2FF, 0xFFEDF2FF, 0xFFFAF8FF, 0xFF4D64B8, 0xFFFFEEB5, 0xFF8CA0F0, 0xFFF9FAFF, 0xFFECF0FF, 0xFFFFFFFF, 0xFFE0E7FF, 0xFF1A2242)
        ),
        CinematicThemeSpec(
            id = "cinematic_girl_crystal",
            category = CinematicCategory.CINEMATIC,
            titleAr = "بلورات الكريستال السحري",
            titleEn = "Crystal Prism",
            descAr = "منشورات كريستال متلألئة تنكسر عليها الأضواء مع إطار ألماسي",
            descEn = "Rotating faceted crystal prisms with jeweled diamond avatar ring",
            sceneType = CinematicSceneType.CRYSTAL_PRISM,
            frameStyle = CinematicAvatarFrameStyle.PRISM_JEWEL,
            bubbleStyle = CinematicBubbleStyle.GLASS_GLOW,
            darkVariant = darkPack(0xFF130B29, 0xFF23164B, 0xFF100B24, 0xFFC596FF, 0xFF8BF0FF, 0xFF66D1FF, 0xFF1A1136, 0xFF24184A, 0xFF2E205E, 0xFF3B2975),
            lightVariant = lightPack(0xFFE9DEFF, 0xFFF5EEFF, 0xFFF2FAFF, 0xFF7B44D6, 0xFFFFE0FA, 0xFF48BBE8, 0xFFFAF7FF, 0xFFF0E8FF, 0xFFFFFFFF, 0xFFE5DAFF, 0xFF241245)
        ),
        CinematicThemeSpec(
            id = "cinematic_girl_lavender",
            category = CinematicCategory.CINEMATIC,
            titleAr = "نسيم اللافندر الذهبي",
            titleEn = "Lavender Breeze",
            descAr = "موجات أزهار اللافندر المتمايلة تحت النجوم أو شمس العصر الذهبية",
            descEn = "Swaying lavender waves under starlight or golden hour glow",
            sceneType = CinematicSceneType.LAVENDER_BREEZE,
            frameStyle = CinematicAvatarFrameStyle.PETAL_HALO,
            bubbleStyle = CinematicBubbleStyle.ROYAL_SILK,
            darkVariant = darkPack(0xFF141126, 0xFF241D45, 0xFF161329, 0xFFBB9BFF, 0xFFFFD891, 0xFF9E70FF, 0xFF1B1733, 0xFF262047, 0xFF30295A, 0xFF3B336E),
            lightVariant = lightPack(0xFFE6DCFF, 0xFFF3EEFF, 0xFFFFF5E8, 0xFF7451C9, 0xFFFFDF9B, 0xFFB084F5, 0xFFFCF9FF, 0xFFF2ECFF, 0xFFFFFFFF, 0xFFE6DFFF, 0xFF271D47)
        ),
        CinematicThemeSpec(
            id = "cinematic_girl_phoenix",
            category = CinematicCategory.CINEMATIC,
            titleAr = "ريشة الغروب الوردية",
            titleEn = "Silk Sunset Plume",
            descAr = "تدرجات المرجان والذهب الوردي مع أمواج حريرية وإطار متوهج",
            descEn = "Coral rose-gold ribbons with warm sunset plume glow",
            sceneType = CinematicSceneType.SUNSET_PLUME,
            frameStyle = CinematicAvatarFrameStyle.FLAME_CREST,
            bubbleStyle = CinematicBubbleStyle.ROYAL_SILK,
            darkVariant = darkPack(0xFF210E1A, 0xFF3B192B, 0xFF1C0C16, 0xFFFF8C73, 0xFFFFCF8B, 0xFFFF5E8E, 0xFF27121F, 0xFF381A2C, 0xFF472238, 0xFF572B45),
            lightVariant = lightPack(0xFFFFD8C4, 0xFFFFEDE2, 0xFFFFF7F0, 0xFFD9594C, 0xFFFFEAAC, 0xFFFF8578, 0xFFFFFAF7, 0xFFFFECE3, 0xFFFFFFFF, 0xFFFFE0D3, 0xFF3B1816)
        ),
        CinematicThemeSpec(
            id = "cinematic_girl_cloud",
            category = CinematicCategory.CINEMATIC,
            titleAr = "مملكة الغيوم السماوية",
            titleEn = "Cotton Sky Castle",
            descAr = "غيوم قطنية متدرجة وقوس ضوئي ناعم مع فوانيس سماوية مضيئة",
            descEn = "Dreamy cotton clouds and gentle sky prism with floating lights",
            sceneType = CinematicSceneType.COTTON_CLOUD,
            frameStyle = CinematicAvatarFrameStyle.PEARL_DOUBLE,
            bubbleStyle = CinematicBubbleStyle.GLASS_GLOW,
            darkVariant = darkPack(0xFF0E1A38, 0xFF1B2E5E, 0xFF121D3D, 0xFF8EC5FF, 0xFFFFC2EC, 0xFFFF9BD8, 0xFF142145, 0xFF1E3061, 0xFF263B75, 0xFF30498C),
            lightVariant = lightPack(0xFFBBE3FF, 0xFFE5F3FF, 0xFFFFEFF8, 0xFF2D7FD9, 0xFFFFF3BA, 0xFFF07BB8, 0xFFF9FCFF, 0xFFECF5FF, 0xFFFFFFFF, 0xFFDDECFF, 0xFF142947)
        ),

        // ================= 10 CINEMATIC THEMES FOR BOYS =================
        CinematicThemeSpec(
            id = "cinematic_boy_cyber",
            category = CinematicCategory.CINEMATIC,
            titleAr = "سايبر نيون المستقبلي",
            titleEn = "Cyber Neon Grid",
            descAr = "شبكة أفق ثلاثية الأبعاد متحركة بالنيون السماوي وإطار تكتيكي رقمي",
            descEn = "Scrolling 3D cyber horizon grid with tactical neon HUD avatar bezel",
            sceneType = CinematicSceneType.CYBER_NEON,
            frameStyle = CinematicAvatarFrameStyle.CYBER_BEZEL,
            bubbleStyle = CinematicBubbleStyle.CYBER_CHAMFER,
            darkVariant = darkPack(0xFF050A14, 0xFF0A1428, 0xFF0D1D36, 0xFF00E5FF, 0xFF7DF9FF, 0xFF0095FF, 0xFF0B1422, 0xFF111E32, 0xFF15253E, 0xFF1B304F),
            lightVariant = lightPack(0xFFCCEFFF, 0xFFE6F6FF, 0xFFF5FAFF, 0xFF007EA7, 0xFF80F0FF, 0xFF00A8E8, 0xFFF4FBFF, 0xFFE6F3FA, 0xFFFFFFFF, 0xFFD6ECF7, 0xFF092030)
        ),
        CinematicThemeSpec(
            id = "cinematic_boy_magma",
            category = CinematicCategory.CINEMATIC,
            titleAr = "بركان السبج الملتهب",
            titleEn = "Obsidian Magma",
            descAr = "شرارات بركانية صاعدة وتشققات صهارة متوهجة مع إطار معدني مصهور",
            descEn = "Rising volcanic embers and molten fissures with forged flame bezel",
            sceneType = CinematicSceneType.OBSIDIAN_MAGMA,
            frameStyle = CinematicAvatarFrameStyle.FLAME_CREST,
            bubbleStyle = CinematicBubbleStyle.CYBER_CHAMFER,
            darkVariant = darkPack(0xFF120605, 0xFF260D09, 0xFF170705, 0xFFFF6A2B, 0xFFFFC14D, 0xFFD92B1C, 0xFF1C0C09, 0xFF2B130E, 0xFF381913, 0xFF472119),
            lightVariant = lightPack(0xFFFFD8BF, 0xFFFFECE0, 0xFFFFF6EF, 0xFFC94414, 0xFFFFD275, 0xFFE87424, 0xFFFFF9F5, 0xFFFFEBDD, 0xFFFFFFFF, 0xFFFFDEC8, 0xFF331107)
        ),
        CinematicThemeSpec(
            id = "cinematic_boy_arctic",
            category = CinematicCategory.CINEMATIC,
            titleAr = "عاصفة القطب الشمالي",
            titleEn = "Arctic Storm",
            descAr = "قمم جليدية حادة وندف ثلجية سريعة مع إطار فولاذي جليدي",
            descEn = "Jagged alpine peaks and drifting snow crystals with glacier bezel",
            sceneType = CinematicSceneType.ARCTIC_STORM,
            frameStyle = CinematicAvatarFrameStyle.CYBER_BEZEL,
            bubbleStyle = CinematicBubbleStyle.GLASS_GLOW,
            darkVariant = darkPack(0xFF08121C, 0xFF102436, 0xFF091826, 0xFF5CD6FF, 0xFFC2F2FF, 0xFF3A8DDE, 0xFF0F1D2B, 0xFF16293D, 0xFF1C354D, 0xFF244361),
            lightVariant = lightPack(0xFFCDE9F8, 0xFFE9F5FC, 0xFFF7FBFF, 0xFF1474B8, 0xFFFFFFFF, 0xFF47A7EB, 0xFFF7FBFF, 0xFFEAF3FA, 0xFFFFFFFF, 0xFFDCEBF7, 0xFF0D2436)
        ),
        CinematicThemeSpec(
            id = "cinematic_boy_samurai",
            category = CinematicCategory.CINEMATIC,
            titleAr = "الساموراي الأحمر",
            titleEn = "Shadow Samurai",
            descAr = "قرص قرمزي عملاق وضباب جبلي وأوراق طائرة مع إطار قتالي مصقول",
            descEn = "Giant crimson eclipse sun and misty peaks with lacquered crest ring",
            sceneType = CinematicSceneType.SHADOW_SAMURAI,
            frameStyle = CinematicAvatarFrameStyle.FORGED_HEX,
            bubbleStyle = CinematicBubbleStyle.CYBER_CHAMFER,
            darkVariant = darkPack(0xFF12070A, 0xFF240D13, 0xFF0D0608, 0xFFFF384C, 0xFFFF8F7A, 0xFFB81D33, 0xFF190B0E, 0xFF261116, 0xFF33171D, 0xFF401E26),
            lightVariant = lightPack(0xFFF7E2D4, 0xFFFAF0E6, 0xFFFFFBF7, 0xFFC82333, 0xFFFFD8A8, 0xFF2D2528, 0xFFFCF9F5, 0xFFF3EAE0, 0xFFFFFFFF, 0xFFEBE0D4, 0xFF241416)
        ),
        CinematicThemeSpec(
            id = "cinematic_boy_vortex",
            category = CinematicCategory.CINEMATIC,
            titleAr = "أفق الثقب الكوني",
            titleEn = "Cosmic Horizon",
            descAr = "حلقات جاذبية دوارة وشهب نجمية مع إطار مداري فضائي مزدوج",
            descEn = "Swirling accretion rings and starlight streaks with orbital halo",
            sceneType = CinematicSceneType.COSMIC_VORTEX,
            frameStyle = CinematicAvatarFrameStyle.ORBITAL_RING,
            bubbleStyle = CinematicBubbleStyle.COSMIC_RIM,
            darkVariant = darkPack(0xFF060712, 0xFF111430, 0xFF090B1A, 0xFFFFA53B, 0xFF7DA8FF, 0xFF5B6BFF, 0xFF0F1226, 0xFF171C3A, 0xFF1E254D, 0xFF273061),
            lightVariant = lightPack(0xFFD6E0FF, 0xFFEDF1FF, 0xFFFFF4E3, 0xFF3955C7, 0xFFFFD885, 0xFFE07D24, 0xFFF8F9FF, 0xFFECEFFF, 0xFFFFFFFF, 0xFFDFE5FF, 0xFF141B3B)
        ),
        CinematicThemeSpec(
            id = "cinematic_boy_trident",
            category = CinematicCategory.CINEMATIC,
            titleAr = "أعماق المحيط والرادار",
            titleEn = "Deep Trench",
            descAr = "نبضات سونار دائرية وتيارات أزرق كهربائي مع إطار تيتانيوم بحري",
            descEn = "Submarine sonar waves and electric blue currents with dive bezel",
            sceneType = CinematicSceneType.DEEP_TRENCH,
            frameStyle = CinematicAvatarFrameStyle.CYBER_BEZEL,
            bubbleStyle = CinematicBubbleStyle.COSMIC_RIM,
            darkVariant = darkPack(0xFF030F17, 0xFF062030, 0xFF04141F, 0xFF1CB5FF, 0xFF8CE2FF, 0xFF0077C2, 0xFF071A26, 0xFF0C2738, 0xFF10344A, 0xFF15435E),
            lightVariant = lightPack(0xFFBAE8FF, 0xFFE0F4FF, 0xFFF4FBFF, 0xFF026AA7, 0xFFA6EDFF, 0xFF1995D6, 0xFFF5FBFF, 0xFFE4F4FD, 0xFFFFFFFF, 0xFFD4ECFA, 0xFF082436)
        ),
        CinematicThemeSpec(
            id = "cinematic_boy_falcon",
            category = CinematicCategory.CINEMATIC,
            titleAr = "صقر الصحراء الذهبية",
            titleEn = "Desert Falcon",
            descAr = "كثبان صحراوية سينمائية تحت نجوم الليل أو شمس ذهبية ساطعة",
            descEn = "Sculpted Arabian desert dunes under stars or golden desert sun",
            sceneType = CinematicSceneType.DESERT_FALCON,
            frameStyle = CinematicAvatarFrameStyle.ORBITAL_RING,
            bubbleStyle = CinematicBubbleStyle.COSMIC_RIM,
            darkVariant = darkPack(0xFF140F08, 0xFF261D10, 0xFF17120A, 0xFFF2B649, 0xFFFFE19C, 0xFFB87821, 0xFF1E170D, 0xFF2B2113, 0xFF382B19, 0xFF473720),
            lightVariant = lightPack(0xFFFFDEAA, 0xFFFBF0DA, 0xFFFFF8EC, 0xFFB56E14, 0xFFFFE9A3, 0xFFD98F2B, 0xFFFFFAF2, 0xFFF8EBD3, 0xFFFFFFFF, 0xFFF2DFBE, 0xFF30200B)
        ),
        CinematicThemeSpec(
            id = "cinematic_boy_mech",
            category = CinematicCategory.CINEMATIC,
            titleAr = "درع التيتانيوم السداسي",
            titleEn = "Titanium Mech",
            descAr = "شبكة درع سداسية مضاءة وإطار ميكانيكي سداسي للصور والرسائل",
            descEn = "Hexagonal titanium armor weave with precision mechanical collar frame",
            sceneType = CinematicSceneType.TITANIUM_MECH,
            frameStyle = CinematicAvatarFrameStyle.FORGED_HEX,
            bubbleStyle = CinematicBubbleStyle.CYBER_CHAMFER,
            darkVariant = darkPack(0xFF0D1015, 0xFF181E27, 0xFF10141B, 0xFF29D98E, 0xFFFF9248, 0xFF4D94FF, 0xFF151A22, 0xFF1E2631, 0xFF26303E, 0xFF303D4F),
            lightVariant = lightPack(0xFFDDE3EC, 0xFFEEF2F7, 0xFFF8FAFC, 0xFF1E6BC9, 0xFFE6F2FF, 0xFF0F9D68, 0xFFF8FAFC, 0xFFEBEEF3, 0xFFFFFFFF, 0xFFDCE2EB, 0xFF161D29)
        ),
        CinematicThemeSpec(
            id = "cinematic_boy_phantom",
            category = CinematicCategory.CINEMATIC,
            titleAr = "الشبح الزمردي",
            titleEn = "Emerald Phantom",
            descAr = "غابة زمردية غامضة مع أضواء يراعات متحركة وإطار زمردي حاد",
            descEn = "Emerald forest silhouettes and glowing mist with sharp jade frame",
            sceneType = CinematicSceneType.EMERALD_PHANTOM,
            frameStyle = CinematicAvatarFrameStyle.PRISM_JEWEL,
            bubbleStyle = CinematicBubbleStyle.GLASS_GLOW,
            darkVariant = darkPack(0xFF05140D, 0xFF0C291C, 0xFF071A11, 0xFF2BE88A, 0xFF9BFFCA, 0xFF129963, 0xFF091F15, 0xFF102E20, 0xFF163D2B, 0xFF1D4F38),
            lightVariant = lightPack(0xFFC8F2DC, 0xFFE6FAEF, 0xFFF4FCF7, 0xFF118A53, 0xFFFFF1BA, 0xFF2FB87A, 0xFFF5FCF8, 0xFFE3F7EC, 0xFFFFFFFF, 0xFFD4F0E1, 0xFF0D2E1D)
        ),
        CinematicThemeSpec(
            id = "cinematic_boy_thunder",
            category = CinematicCategory.CINEMATIC,
            titleAr = "عاصفة البرق الذهبية",
            titleEn = "Thunder Apex",
            descAr = "ومضات برق متوهجة وسحب عاصفة سريعة مع إطار مشحون بالطاقة",
            descEn = "Dynamic storm clouds and golden lightning arcs with charged bezel",
            sceneType = CinematicSceneType.THUNDER_APEX,
            frameStyle = CinematicAvatarFrameStyle.FORGED_HEX,
            bubbleStyle = CinematicBubbleStyle.CYBER_CHAMFER,
            darkVariant = darkPack(0xFF090D1C, 0xFF141B38, 0xFF0D1126, 0xFFFFCC33, 0xFF91A8FF, 0xFF5D6EFF, 0xFF11162B, 0xFF19203D, 0xFF212B50, 0xFF2B3766),
            lightVariant = lightPack(0xFFD4DCFA, 0xFFEBEEFC, 0xFFFFF8E6, 0xFF4249C4, 0xFFFFDC6B, 0xFFE09A00, 0xFFF8F9FE, 0xFFECEFFA, 0xFFFFFFFF, 0xFFDFE4F7, 0xFF15193B)
        )
    )

    fun findById(id: String?): CinematicThemeSpec? =
        if (id.isNullOrBlank()) null else allThemes.firstOrNull { it.id == id }

    fun toStoreThemeItems(): List<StoreThemeItem> = allThemes.map { it.toStoreThemeItem() }
}
