package com.example.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.model.AppLanguage
import com.example.data.repository.StoreThemeItem
import kotlin.math.roundToInt

interface JesrStrings {
    val isEnglish: Boolean
    val appName: String
    val back: String
    val cancel: String
    val save: String
    val confirm: String
    val close: String
    val undo: String
    val retry: String
    val accept: String
    val reject: String
    val freeLabel: String

    // Drawer
    val navHome: String
    val navPrivacy: String
    val navAppearance: String
    val navThemeStore: String
    val navSettings: String
    val navProfile: String
    val signInWithGoogle: String
    val connectedWithGoogle: String
    val switchGoogleAccount: String
    val signOut: String

    // Home
    val addFriendsCd: String
    val searchFriendsCd: String
    val searchPlaceholder: String
    val clearSearchCd: String
    val emptyFriendsHint: String
    val noSearchResults: String
    val statusBlocked: String
    val statusAwaitingResume: String
    val statusOnlineMesh: String
    val statusOffline: String
    val mutedCd: String
    val openConversation: String
    val friendSettingsMenu: String
    val muteNotifications: String
    val unmuteNotifications: String
    val blockFriend: String
    val unblockFriend: String
    val removeFriend: String

    // Profile & Cropper
    val changeProfilePicCd: String
    val tapPhotoToChangeHint: String
    val displayNameLabel: String
    val editNameCd: String
    val uniqueJesrIdLabel: String
    val copyIdOnlyCd: String
    val defaultUserName: String
    val cropTitle: String
    val cropSubtitle: String
    val cropRotate: String
    val cropZoom: String
    val cropConfirm: String
    val cropLoading: String
    val cropLoadError: String

    // Add Friends Hub & Add by ID
    val addFriendsTitle: String
    val addFriendsSubtitle: String
    val hubAddByIdTitle: String
    val hubAddByIdSubtitle: String
    val hubNearbyTitle: String
    val hubNearbySubtitle: String
    val hubRequestsTitle: String
    val hubRequestsSubtitle: String
    val hubSentTitle: String
    val hubSentSubtitle: String
    val addByIdTitle: String
    val addByIdSubtitle: String
    val addByIdInputLabel: String
    val lookupAccountBtn: String
    val availableLocally: String
    val alreadyFriends: String
    val friendRequestSentCheck: String
    val sendFriendRequestBtn: String
    val accountNotFound: (String) -> String

    // Nearby People
    val nearbyTitle: String
    val nearbySubtitle: String
    val manualRefreshCd: String
    val optionalRadiusLabel: String
    val radiusAll: String
    val radiusClose: String
    val radiusMedium: String
    val radiusWide: String
    val noNearbyPeopleHint: String
    val friendBadge: String
    val sentBadge: String
    val sendRequestShort: String

    // Friend Requests
    val requestsTitle: String
    val requestsSubtitle: String
    val refreshRequestsCd: String
    val incomingTabCount: (Int) -> String
    val sentTabCount: (Int) -> String
    val noIncomingRequests: String
    val noSentRequests: String
    val mutualFriendsCount: (Int) -> String
    val statusAwaitingReply: (String) -> String

    // Conversation
    val backToFriendsCd: String
    val e2eeCd: String
    val chatLockedBlocked: String
    val chatAwaitingResume: String
    val chatOnlineRange: String
    val chatDirectUnavailable: String
    val chatSettingsCd: String
    val viewFriendProfile: String
    val friendPrivacySettings: String
    val directLostBanner: String
    val friendBlockedTitle: (String) -> String
    val friendBlockedBody: String
    val unblockBtn: String
    val resumeInviteTitle: String
    val resumeApprovedWaitingBody: (String) -> String
    val resumeHiddenUntilMutualBody: String
    val approveResumeBtn: String
    val emptyChatHint: String
    val attachmentPrefix: (String) -> String
    val removeAttachmentCd: String
    val sosActiveBanner: String
    val attachPhotoCd: String
    val sosCd: String
    val emergencyPlaceholder: String
    val messagePlaceholder: String
    val sendCd: String
    val attachedImageDefaultName: String
    val sentFromDistance: (Float) -> String
    val deliveryLocal: String
    val deliveryHop: (Int) -> String
    val deliverySending: String
    val deliveryQueued: String
    val deliveryDelivered: String
    val deliveryFailed: String

    // Dialogs & Friend Profile
    val blockDialogTitle: (String) -> String
    val blockDialogBody: String
    val confirmBlockBtn: String
    val removeDialogTitle: (String) -> String
    val removeDialogBody: String
    val confirmRemoveBtn: String
    val resumeSecondTitle: String
    val resumeSecondBody: (String) -> String
    val yesApproveBtn: String
    val deleteAllReqTitle: String
    val deleteAllReqBody: (Int) -> String
    val deleteAllBtn: String
    val statusMeshOnline: String
    val statusMeshOffline: String
    val keyFingerprint: (String) -> String
    val incomingMeshReqTitle: String
    val incomingMeshReqBody: (String) -> String

    // Friend Settings & Privacy Screens
    val friendSettingsTitle: String
    val friendNotFound: String
    val msgDistancePrivacyTitle: String
    val msgDistancePrivacyLabel: String
    val mutualFriendsPrivacyTitle: String
    val mutualFriendsPrivacyLabel: String
    val manageFriendTitle: String
    val muteFriend: String
    val globalPrivacyTitle: String
    val globalPrivacySubtitle: String
    val globalDistanceTitle: String
    val globalDistanceSubtitle: String
    val globalMutualTitle: String
    val globalMutualSubtitle: String
    val reqPrivacyTitle: String
    val reqPrivacySubtitle: String
    val friendExceptionsHeader: String
    val noFriendsForExceptions: String
    val seeMsgDistanceLabel: String
    val seeMutualFriendsLabel: String
    val reqPrivacyScreenTitle: String
    val reqPrivacyScreenSubtitle: String
    val muteReqSystemTitle: String
    val muteReqSystemSubtitle: String
    val blockReqSystemTitle: String
    val blockReqSystemSubtitle: String
    val addIdExceptionTitle: String
    val addExceptionCd: String
    val individualRulesHeader: String
    val muteUserRequests: String
    val blockUserRequestsSilently: String
    val deleteAllIncomingBtn: (Int) -> String

    // Appearance
    val appearanceTitle: String
    val appearanceSubtitle: String
    val quickRestoreOriginalBtn: String
    val fixedPresetsHeader: String
    val autoOriginalTitle: String
    val autoOriginalSubtitle: String
    val sunMoonToggleDarkCd: String
    val sunMoonToggleLightCd: String
    val myThemesHeader: String
    val storeBtn: String
    val unDeletableCd: String
    val tagFreeProtected: String
    val tagMyPaid: String
    val tagMyFree: String
    val tagAcquiredPaid: String
    val tagFree: String
    val activeLabel: String
    val applyLabel: String
    val customHeader: String
    val customTitle: String
    val customSubtitle: String
    val showLightTitle: String
    val lightEnabledSub: String
    val lightDisabledSub: String
    val bgColorTitle: String
    val bgColorSub: String
    val iconColorTitle: String
    val iconColorSub: String
    val lightColorTitle: String
    val lightColorSubEnabled: String
    val lightColorSubDisabled: String
    val baseColorTitle: String
    val baseColorSub: String
    val restoreBtn: String
    val undoLastChange: String
    val noUndoChanges: String
    val applyColorPrefix: (String) -> String
    val hideLabel: String
    val pickColorLabel: String
    val selectedLabel: String
    val lockedModeCd: String
    val lightStatusCd: String
    val undoApplyCd: String

    // Theme Store
    val themeStoreTitle: String
    val themeStoreSubtitle: String
    val storeGoogleSyncBanner: String
    val filterAll: String
    val filterFree: String
    val storeEmptyCategory: String
    val storeAllAcquiredMsg: String
    val designerPrefix: (String) -> String
    val storeAppliedNow: String
    val getThemeBtn: String
    val badgeBaseProtected: String
    val downloadedAppliedBadge: String
    val removeDownloadedCd: String
    val snowFrostLightTitle: String
    val snowFrostEnabledSub: String
    val snowFrostDisabledSub: String

    // Settings
    val settingsTitle: String
    val settingsSubtitle: String
    val languageSectionTitle: String
    val languageSectionSubtitle: String
    val arabicLabel: String
    val englishLabel: String
    val appearanceSettingsEntryTitle: String
    val appearanceSettingsEntrySubtitle: String
    val meshSwitchTitle: String
    val meshSwitchSubtitle: String
    val nearbyVisTitle: String
    val nearbyVisSubtitle: String
    val statsHeader: String
    val statRelayed: String
    val statDelivered: String
    val statQueued: String
    val statMaxTtl: String
    val statMaxTtlVal: String
    val e2eeTitle: String
    val publicKeyFingerprint: (String) -> String
    val aboutAppLabel: String

    // Telemetry card
    val telemetryActive: String
    val telemetryIdle: String
    val telemetryNodes: (Int) -> String
    val telemetryRelayed: (Int) -> String
    val telemetryQueued: (Int) -> String

    // Profile Cover Photo
    val coverPhotoTitle: String
    val changeCoverBtn: String
    val removeCoverBtn: String
    val addCoverHint: String
    val cropCoverTitle: String
    val cropCoverSubtitle: String

    // App Lock & Security
    val appLockTitle: String
    val appLockSubtitle: String
    val lockMethodNone: String
    val lockMethodBiometric: String
    val lockMethodPin: String
    val lockMethodPassword: String
    val lockMethodPattern: String
    val appLockUnlockHeader: String
    val appLockBioHint: String
    val appLockPinHint: String
    val appLockPasswordHint: String
    val appLockPatternHint: String
    val appLockBiometricPromptSub: String
    val appLockUseFingerprintBtn: String
    val appLockContinueBtn: String
    val appLockBackupSecretLabel: String
    val appLockWrongSecret: String
    val unlockBtn: String
    val appLockEnterPasswordLabel: String
    val appLockPatternTooShort: String
    val appLockBioReadyMsg: String
    val appLockBioEnrollHint: String
    val appLockNewPinLabel: String
    val appLockConfirmLabel: String
    val appLockNewPassLabel: String
    val appLockDrawPatternHint: String
    val appLockDrawPatternConfirmHint: String
    val appLockPatternMismatch: String
    val appLockPinTooShort: String
    val appLockPassTooShort: String
    val appLockSecretMismatch: String
    val lockNowBtn: String

    // Profile Cover Confirmation
    val confirmCoverDialogTitle: String
    val confirmCoverDialogBody: String
    val confirmCoverProceedBtn: String

    // Cinematic Variant Controls
    val cinematicBadge: String
    val darkVariantBtn: String
    val lightVariantBtn: String
}

fun formatEstimatedDistance(meters: Float, isEnglish: Boolean): String {
    val rounded = meters.roundToInt()
    return if (isEnglish) {
        if (rounded <= 1) "~1 m away" else "~$rounded m away"
    } else {
        when {
            rounded <= 1 -> "حوالي متر واحد (~1 م)"
            rounded == 2 -> "حوالي مترين (~2 م)"
            rounded in 3..10 -> "حوالي $rounded أمتار (~$rounded م)"
            else -> "حوالي $rounded متراً (~$rounded م)"
        }
    }
}

fun StoreThemeItem.localizedTitle(isEnglish: Boolean): String {
    CinematicThemesCatalog.findById(id)?.let { spec ->
        return if (isEnglish) spec.titleEn else spec.titleAr
    }
    if (!isEnglish) return title
    return when (id) {
        "base_jesr_core" -> "Original Theme (Base Interface)"
        "curated_olive_grove" -> "Calm Olive Grove"
        "curated_moon_studio" -> "Quiet Moonlight"
        "curated_royal_terracotta" -> "Royal Terracotta"
        "curated_cobalt_night" -> "Luxury Cobalt Night"
        "curated_emerald_dusk" -> "Emerald Dusk"
        else -> title
    }
}

fun StoreThemeItem.localizedDescription(isEnglish: Boolean): String {
    CinematicThemesCatalog.findById(id)?.let { spec ->
        return if (isEnglish) spec.descEn else spec.descAr
    }
    if (!isEnglish) return description
    return when (id) {
        "base_jesr_core" -> "Jesr's original core interface — Free and permanently protected"
        "curated_olive_grove" -> "Soft olive surface with warm glow and crisp dark ink icons"
        "curated_moon_studio" -> "Quiet nocturnal palette with silver moonlight rays and shimmer"
        "curated_royal_terracotta" -> "Rich terracotta craft surface paired with warm amber light"
        "curated_cobalt_night" -> "Deep midnight cobalt with luminous icons and sky-blue glow"
        "curated_emerald_dusk" -> "Calm emerald tones with a subtle glowing aura"
        else -> description
    }
}

fun StoreThemeItem.localizedCreator(isEnglish: Boolean): String {
    if (!isEnglish) return creatorName
    return when (creatorName) {
        "جسر الأساسي" -> "Jesr Core"
        "استوديو جسر" -> "Jesr Studio"
        "مصمم جسر" -> "Jesr Designer"
        else -> creatorName
    }
}

object ArabicJesrStrings : JesrStrings {
    override val isEnglish = false
    override val appName = "جسر"
    override val back = "رجوع"
    override val cancel = "إلغاء"
    override val save = "حفظ"
    override val confirm = "تأكيد"
    override val close = "إغلاق"
    override val undo = "تراجع"
    override val retry = "إعادة المحاولة"
    override val accept = "قبول"
    override val reject = "رفض"
    override val freeLabel = "مجاني"

    override val navHome = "الرئيسية"
    override val navPrivacy = "الخصوصية"
    override val navAppearance = "المظهر"
    override val navThemeStore = "متجر الثيمات"
    override val navSettings = "الإعدادات"
    override val navProfile = "الملف الشخصي"
    override val signInWithGoogle = "تسجيل دخول عبر Google"
    override val connectedWithGoogle = "متصل بحساب Google"
    override val switchGoogleAccount = "تبديل حساب Google"
    override val signOut = "تسجيل الخروج"

    override val addFriendsCd = "إضافة أصدقاء"
    override val searchFriendsCd = "بحث الأصدقاء والمحادثات"
    override val searchPlaceholder = "ابحث بالاسم أو معرف جسر أو نص محادثة..."
    override val clearSearchCd = "مسح البحث"
    override val emptyFriendsHint = "قائمة الأصدقاء فارغة\nاضغط على زر (+) بالأسفل لإضافة أصدقاء"
    override val noSearchResults = "لا توجد نتائج مطابقة لبحثك"
    override val statusBlocked = "محظور"
    override val statusAwaitingResume = "بانتظار استئناف التواصل"
    override val statusOnlineMesh = "متصل عبر الشبكة"
    override val statusOffline = "غير متصل"
    override val mutedCd = "مكتوم"
    override val openConversation = "فتح المحادثة"
    override val friendSettingsMenu = "إعدادات الصديق"
    override val muteNotifications = "كتم الإشعارات"
    override val unmuteNotifications = "إلغاء كتم الإشعارات"
    override val blockFriend = "حظر الصديق"
    override val unblockFriend = "إلغاء حظر الصديق"
    override val removeFriend = "إزالة الصديق"

    override val changeProfilePicCd = "تغيير صورة الملف الشخصي"
    override val tapPhotoToChangeHint = "اضغط على الصورة لاختيار صورة وقصها"
    override val displayNameLabel = "اسم العرض"
    override val editNameCd = "تعديل الاسم"
    override val uniqueJesrIdLabel = "معرف جسر الفريد (JISR ID)"
    override val copyIdOnlyCd = "نسخ المعرف فقط"
    override val defaultUserName = "مستخدم جسر"
    override val cropTitle = "قص الصورة الشخصية"
    override val cropSubtitle = "حرّك الصورة أو قرّبها لتحديد الجزء المناسب"
    override val cropRotate = "تدوير 90°"
    override val cropZoom = "التقريب"
    override val cropConfirm = "قص وحفظ"
    override val cropLoading = "جاري تجهيز الصورة..."
    override val cropLoadError = "تعذر تحميل الصورة المختارة"

    override val addFriendsTitle = "إضافة أصدقاء"
    override val addFriendsSubtitle = "اختر طريقة التواصل وإدارة الطلبات"
    override val hubAddByIdTitle = "الإضافة عبر معرّف المستخدم"
    override val hubAddByIdSubtitle = "البحث عن حساب بمعرف جسر الفريد وإرسال طلب صداقة"
    override val hubNearbyTitle = "الأشخاص القريبون"
    override val hubNearbySubtitle = "اكتشاف المستخدمين المفعّلين للظهور القريب في النطاق"
    override val hubRequestsTitle = "طلبات الصداقة"
    override val hubRequestsSubtitle = "عرض الطلبات الواردة وقبولها أو رفضها"
    override val hubSentTitle = "الطلبات التي أرسلتها"
    override val hubSentSubtitle = "متابعة حالة طلباتك التي أرسلتها أو إلغائها"
    override val addByIdTitle = "إضافة عبر المعرّف"
    override val addByIdSubtitle = "أدخل معرّف جسر الفريد للبحث عن الحساب"
    override val addByIdInputLabel = "معرّف جسر (مثال: JESR-4K89-M2W9)"
    override val lookupAccountBtn = "بحث عن الحساب"
    override val availableLocally = "متاح حالياً في النطاق المحلي"
    override val alreadyFriends = "أنتما أصدقاء بالفعل"
    override val friendRequestSentCheck = "تم إرسال طلب الصداقة ✓"
    override val sendFriendRequestBtn = "إرسال طلب صداقة"
    override val accountNotFound: (String) -> String = { id ->
        "لم يتم العثور على حساب مطابق للمعرّف ($id). تأكد من كتابة المعرّف بشكل صحيح أو قرب الجهاز في الشبكة المحلية."
    }

    override val nearbyTitle = "الأشخاص القريبون"
    override val nearbySubtitle = "المستخدمون المفعّلون للظهور في النطاق القريب"
    override val manualRefreshCd = "تحديث يدوي"
    override val optionalRadiusLabel = "نطاق البحث الاختياري"
    override val radiusAll = "كل النطاقات"
    override val radiusClose = "نطاق قريب"
    override val radiusMedium = "نطاق متوسط"
    override val radiusWide = "نطاق واسع"
    override val noNearbyPeopleHint = "لا يوجد أشخاص قريبون مفعّلون للظهور حالياً\nجرّب التحديث اليدوي"
    override val friendBadge = "صديق"
    override val sentBadge = "تم الإرسال"
    override val sendRequestShort = "إرسال طلب"

    override val requestsTitle = "طلبات الصداقة"
    override val requestsSubtitle = "اسحب للتبديل السريع بين الواردة والتي أرسلتها"
    override val refreshRequestsCd = "تحديث الطلبات"
    override val incomingTabCount: (Int) -> String = { c -> "الواردة ($c)" }
    override val sentTabCount: (Int) -> String = { c -> "الطلبات التي أرسلتها ($c)" }
    override val noIncomingRequests = "لا توجد طلبات صداقة واردة حالياً"
    override val noSentRequests = "لا توجد طلبات صداقة أرسلتها قيد الانتظار"
    override val mutualFriendsCount: (Int) -> String = { c -> "الأصدقاء المشتركون: $c" }
    override val statusAwaitingReply: (String) -> String = { t -> "الحالة: بانتظار الرد • $t" }

    override val backToFriendsCd = "العودة لقائمة الأصدقاء"
    override val e2eeCd = "تشفير طرفي"
    override val chatLockedBlocked = "المحادثة مقفلة (محظور)"
    override val chatAwaitingResume = "بانتظار موافقة استئناف التواصل"
    override val chatOnlineRange = "متصل في نطاق الشبكة"
    override val chatDirectUnavailable = "الاتصال المباشر غير متاح"
    override val chatSettingsCd = "إعدادات المحادثة"
    override val viewFriendProfile = "عرض ملف الصديق"
    override val friendPrivacySettings = "إعدادات خصوصية الصديق"
    override val directLostBanner = "انقطع الاتصال المباشر — رسائلك محفوظة وسيتم استئناف تسليمها عند عودة الاتصال دون تكرار"
    override val friendBlockedTitle: (String) -> String = { n -> "تم حظر $n" }
    override val friendBlockedBody = "التواصل متوقف وسجل المحادثة مقفل مؤقتاً مع الاحتفاظ بالرسائل. عند إلغاء الحظر، يلزم موافقة الطرفين لاستعادة المحادثة."
    override val unblockBtn = "إلغاء الحظر"
    override val resumeInviteTitle = "دعوة لاستئناف المحادثة"
    override val resumeApprovedWaitingBody: (String) -> String = { n -> "تم تسجيل موافقتك. سجل المحادثة وإرسال الرسائل سيعودان تلقائياً فور موافقة $n." }
    override val resumeHiddenUntilMutualBody = "تم رفع الحظر، ويبقى سجل المحادثة مخفياً حتى يوافق الطرفان على استئناف التواصل."
    override val approveResumeBtn = "الموافقة على استئناف التواصل"
    override val emptyChatHint = "لا توجد رسائل متبادلة في هذه المحادثة بعد.\nابدأ المحادثة بأمان عبر التشفير الطرفي."
    override val attachmentPrefix: (String) -> String = { name -> "مرفق: $name" }
    override val removeAttachmentCd = "إزالة المرفق"
    override val sosActiveBanner = "نداء طوارئ SOS مفعّل"
    override val attachPhotoCd = "إرفاق صورة"
    override val sosCd = "طوارئ"
    override val emergencyPlaceholder = "رسالة طوارئ..."
    override val messagePlaceholder = "اكتب رسالة..."
    override val sendCd = "إرسال"
    override val attachedImageDefaultName = "صورة مرفقة"
    override val sentFromDistance: (Float) -> String = { d -> "تم الإرسال من: ${formatEstimatedDistance(d, false)}" }
    override val deliveryLocal = "محلي"
    override val deliveryHop: (Int) -> String = { hops -> if (hops > 1) "قفزة $hops" else "جاري النقل" }
    override val deliverySending = "جاري النقل"
    override val deliveryQueued = "مؤجل في الطابور"
    override val deliveryDelivered = "تم التسليم"
    override val deliveryFailed = "تعذر الإرسال"

    override val blockDialogTitle: (String) -> String = { n -> "حظر $n؟" }
    override val blockDialogBody = "سيتم إيقاف التواصل وقفل سجل المحادثة مؤقتاً دون حذف الرسائل. عند إلغاء الحظر لاحقاً، يتطلب استئناف المحادثة موافقة الطرفين."
    override val confirmBlockBtn = "تأكيد الحظر"
    override val removeDialogTitle: (String) -> String = { n -> "إزالة $n نهائياً؟" }
    override val removeDialogBody = "• ستنتهي علاقة الصداقة بينكما.\n• سيتم حذف المحادثة وجميع الرسائل من كلا الحسابين عبر حذف متبادل.\n• ملاحظة مهمة: الحذف على مستوى التطبيق لا يزيل لقطات الشاشة أو الملفات المُصدّرة أو أي نسخ محفوظة خارج التطبيق."
    override val confirmRemoveBtn = "تأكيد الإزالة"
    override val resumeSecondTitle = "تأكيد ثانٍ لاستئناف التواصل"
    override val resumeSecondBody: (String) -> String = { n -> "هل أنت متأكد من موافقتك على استئناف التواصل مع $n؟ لن يعود سجل المحادثة والرسائل إلا بعد موافقة الطرفين معاً." }
    override val yesApproveBtn = "نعم، أوافق"
    override val deleteAllReqTitle = "حذف جميع الطلبات الواردة؟"
    override val deleteAllReqBody: (Int) -> String = { c -> "سيتم حذف جميع طلبات الصداقة الواردة الحالية ($c طلب) نهائياً من هذا الجهاز." }
    override val deleteAllBtn = "حذف الكل"
    override val statusMeshOnline = "الحالة: متصل في نطاق Mesh"
    override val statusMeshOffline = "الحالة: غير متصل حالياً"
    override val keyFingerprint: (String) -> String = { fp -> "بصمة التشفير: $fp..." }
    override val incomingMeshReqTitle = "طلب صداقة قريب عبر Mesh"
    override val incomingMeshReqBody: (String) -> String = { id -> "يرغب الجهاز $id في إضافتك إلى شبكة الأصدقاء وتبادل المفاتيح للتواصل المشفر." }

    override val friendSettingsTitle = "إعدادات الصديق"
    override val friendNotFound = "لم يتم العثور على بيانات الصديق."
    override val msgDistancePrivacyTitle = "خصوصية مسافة الرسائل"
    override val msgDistancePrivacyLabel = "السماح لهذا الصديق برؤية المسافة التي أرسلت منها رسائلي."
    override val mutualFriendsPrivacyTitle = "خصوصية الأصدقاء المشتركين"
    override val mutualFriendsPrivacyLabel = "السماح لهذا الصديق برؤية الأصدقاء المشتركين لدي."
    override val manageFriendTitle = "إدارة الصديق"
    override val muteFriend = "كتم الصديق"
    override val globalPrivacyTitle = "إعدادات الخصوصية العامة"
    override val globalPrivacySubtitle = "معطّلة افتراضياً • الاستثناءات الفردية تتجاوز الإعداد العام"
    override val globalDistanceTitle = "السماح لجميع الأصدقاء برؤية مسافة إرسال رسائلي"
    override val globalDistanceSubtitle = "مغلق افتراضياً؛ يمكنك تفعيله أو تعطيله لصديق محدد بالأسفل"
    override val globalMutualTitle = "السماح لجميع الأصدقاء برؤية الأصدقاء المشتركين"
    override val globalMutualSubtitle = "مغلق افتراضياً؛ تغيير هذا الخيار لا يؤثر على خصوصية المسافة"
    override val reqPrivacyTitle = "خصوصية طلبات الصداقة"
    override val reqPrivacySubtitle = "كتم أو حظر طلبات الصداقة وحذف الطلبات الواردة"
    override val friendExceptionsHeader = "استثناءات الأصدقاء (تحكم مستقل لكل صديق)"
    override val noFriendsForExceptions = "لا يوجد أصدقاء مضافون حالياً لتعيين استثناءات لهم."
    override val seeMsgDistanceLabel = "رؤية مسافة الرسائل"
    override val seeMutualFriendsLabel = "رؤية الأصدقاء المشتركين"
    override val reqPrivacyScreenTitle = "خصوصية طلبات الصداقة"
    override val reqPrivacyScreenSubtitle = "إدارة كتم الإشعارات والحظر بشكل منفصل"
    override val muteReqSystemTitle = "نظام كتم طلبات الصداقة"
    override val muteReqSystemSubtitle = "كتم جميع طلبات الصداقة الواردة\n(يخفي التنبيهات فقط دون منع استلام الطلبات)"
    override val blockReqSystemTitle = "نظام حظر طلبات الصداقة"
    override val blockReqSystemSubtitle = "حظر جميع طلبات الصداقة الواردة\n(يرفض الطلبات بصمت دون إبلاغ المُرسل)"
    override val addIdExceptionTitle = "إضافة استثناء فردي بمعرّف جسر"
    override val addExceptionCd = "إضافة استثناء"
    override val individualRulesHeader = "القواعد والاستثناءات الفردية"
    override val muteUserRequests = "كتم طلباته"
    override val blockUserRequestsSilently = "حظر طلباته بصمت"
    override val deleteAllIncomingBtn: (Int) -> String = { c -> "حذف جميع الطلبات الواردة ($c)" }

    override val appearanceTitle = "المظهر"
    override val appearanceSubtitle = "الأوضاع الثابتة، واجهاتي، ووضع التخصيص"
    override val quickRestoreOriginalBtn = "العودة للثيم الأصلي (الواجهة الأساسية)"
    override val fixedPresetsHeader = "أوضاع ثابتة (غير قابلة للتعديل)"
    override val autoOriginalTitle = "تلقائي (الثيم الأصلي)"
    override val autoOriginalSubtitle = "يتبع إعدادات النظام بالواجهة الأساسية الأصلية"
    override val sunMoonToggleDarkCd = "الوضع المظلم - اضغط للتبديل إلى الوضع المضيء"
    override val sunMoonToggleLightCd = "الوضع المضيء - اضغط للتبديل إلى الوضع المظلم"
    override val myThemesHeader = "الثيمات المنزلة"
    override val storeBtn = "المتجر"
    override val unDeletableCd = "أساسي لا يُحذف"
    override val tagFreeProtected = "مجاني • أساسي لا يُحذف"
    override val tagMyPaid = "تصميمي • 1$"
    override val tagMyFree = "تصميمي • مجاني"
    override val tagAcquiredPaid = "مقتنى • 1$"
    override val tagFree = "مجاني"
    override val activeLabel = "مفعّل"
    override val applyLabel = "تطبيق"
    override val customHeader = "التخصيص (الوضع الوحيد القابل للتعديل)"
    override val customTitle = "التخصيص"
    override val customSubtitle = "خيارات منفصلة للخلفية والأيقونات والضوء مع التراجع"
    override val showLightTitle = "إظهار الضوء والإشعاع"
    override val lightEnabledSub = "الضوء مفعّل"
    override val lightDisabledSub = "الضوء ملغى حالياً"
    override val bgColorTitle = "لون الخلفية"
    override val bgColorSub = "تخصيص لون الخلفية بالكامل بشكل منفصل"
    override val iconColorTitle = "لون الأيقونات"
    override val iconColorSub = "تخصيص لون الأيقونات بشكل مستقل"
    override val lightColorTitle = "لون الضوء"
    override val lightColorSubEnabled = "تخصيص لون الضوء والإشعاع بشكل مستقل"
    override val lightColorSubDisabled = "الضوء ملغى حالياً — فعّله بالأعلى لرؤية التغيير"
    override val baseColorTitle = "اللون الأساسي"
    override val baseColorSub = "محفوظ بشكل دائم — لا يمكن حذفه"
    override val restoreBtn = "استعادة"
    override val undoLastChange = "تراجع عن آخر تعديل"
    override val noUndoChanges = "لا توجد تعديلات سابقة للتراجع"
    override val applyColorPrefix: (String) -> String = { name -> "تطبيق $name" }
    override val hideLabel = "إخفاء"
    override val pickColorLabel = "اختيار اللون"
    override val selectedLabel = "محدد"
    override val lockedModeCd = "ثابت غير قابل للتعديل"
    override val lightStatusCd = "حالة الضوء"
    override val undoApplyCd = "تراجع عن آخر تطبيق"

    override val themeStoreTitle = "متجر الثيمات والواجهات"
    override val themeStoreSubtitle = "كل الثيمات مجانية • احصل على الثيم ليُركّب تلقائياً وينتقل لخانة «الثيمات المنزلة»"
    override val storeGoogleSyncBanner = "سجّل دخولك عبر Google لمزامنة ثيماتك المنزلة سحابياً"
    override val filterAll = "الكل"
    override val filterFree = "مجاني"
    override val storeEmptyCategory = "لا توجد ثيمات إضافية في هذا التصنيف حالياً\nستجد كل الثيمات المنزلة في خانة «الثيمات المنزلة» بالمظهر"
    override val storeAllAcquiredMsg = "جميع الثيمات موجودة الآن في خانة «الثيمات المنزلة» بالمظهر"
    override val designerPrefix: (String) -> String = { creator -> "تصميم: $creator" }
    override val storeAppliedNow = "مطبّق حالياً"
    override val getThemeBtn = "الحصول على الثيم"
    override val badgeBaseProtected = "أساسي • لا يُحذف"
    override val downloadedAppliedBadge = "تم التنزيل والتفعيل ✨"
    override val removeDownloadedCd = "إزالة وإرجاع للمتجر"
    override val snowFrostLightTitle = "إظهار الثلج والسقيع"
    override val snowFrostEnabledSub = "الثلج والسقيع يتساقطان على المشهد"
    override val snowFrostDisabledSub = "مشهد جبلي هادئ بدون تساقط ثلج"

    override val settingsTitle = "الإعدادات"
    override val settingsSubtitle = "إعدادات الشبكة المحلية واللغة والتشغيل"
    override val languageSectionTitle = "لغة التطبيق"
    override val languageSectionSubtitle = "تبديل فوري بين العربية والإنجليزية"
    override val arabicLabel = "العربية"
    override val englishLabel = "English"
    override val appearanceSettingsEntryTitle = "المظهر"
    override val appearanceSettingsEntrySubtitle = "الأوضاع الثابتة، واجهاتي، التخصيص، والثيم الأصلي"
    override val meshSwitchTitle = "تشغيل شبكة جسر المحلية (Mesh)"
    override val meshSwitchSubtitle = "تفعيل البث والاتصال عبر البلوتوث و Wi-Fi Direct بدون إنترنت"
    override val nearbyVisTitle = "الظهور في الأشخاص القريبون"
    override val nearbyVisSubtitle = "مغلق افتراضياً — تفعيله يسمح للأجهزة القريبة برؤية حسابك في قائمة القريبين دون كشف موقعك"
    override val statsHeader = "إحصائيات التمرير (Store-and-Forward)"
    override val statRelayed = "رسائل ممررة للأجهزة الأخرى"
    override val statDelivered = "رسائل تم تسليمها بنجاح"
    override val statQueued = "رسائل في طابور الانتظار"
    override val statMaxTtl = "أقصى عدد للقفزات (TTL)"
    override val statMaxTtlVal = "7 قفزات"
    override val e2eeTitle = "تشفير طرفي NIST P-256 + AES-GCM"
    override val publicKeyFingerprint: (String) -> String = { key -> "بصمة المفتاح العام: $key..." }
    override val aboutAppLabel = "تطبيق جسر (Jesr)"

    override val telemetryActive = "نشط"
    override val telemetryIdle = "سكون"
    override val telemetryNodes: (Int) -> String = { c -> "$c عقد" }
    override val telemetryRelayed: (Int) -> String = { c -> "$c مرر" }
    override val telemetryQueued: (Int) -> String = { c -> "$c طابور" }

    override val coverPhotoTitle = "غلاف الحساب"
    override val changeCoverBtn = "تخصيص الغلاف"
    override val removeCoverBtn = "حذف الغلاف"
    override val addCoverHint = "اضغط لاختيار صورة غلاف لحسابك"
    override val cropCoverTitle = "قص صورة الغلاف"
    override val cropCoverSubtitle = "اسحب أو كبّر لتحديد إطار غلاف حسابك"

    override val appLockTitle = "قفل التطبيق"
    override val appLockSubtitle = "حماية جسر ببصمة، رمز رقمي، كلمة مرور، أو نمط"
    override val lockMethodNone = "بدون قفل (مغلق)"
    override val lockMethodBiometric = "قفل بالبصمة"
    override val lockMethodPin = "رمز رقمي (أرقام)"
    override val lockMethodPassword = "رمز كتابة أو أرقام"
    override val lockMethodPattern = "قفل بالنمط"
    override val appLockUnlockHeader = "التطبيق مقفل"
    override val appLockBioHint = "استخدم البصمة لفتح تطبيق جسر"
    override val appLockPinHint = "أدخل الرمز الرقمي لفتح التطبيق"
    override val appLockPasswordHint = "أدخل رمز الكتابة أو الأرقام لفتح التطبيق"
    override val appLockPatternHint = "ارسم نمط القفل لفتح التطبيق"
    override val appLockBiometricPromptSub = "تأكيد الهوية بالبصمة لفتح جسر"
    override val appLockUseFingerprintBtn = "فتح بالبصمة"
    override val appLockContinueBtn = "تأكيد وفتح"
    override val appLockBackupSecretLabel = "رمز احتياطي"
    override val appLockWrongSecret = "الرمز أو النمط غير صحيح، حاول مرة أخرى"
    override val unlockBtn = "فتح التطبيق"
    override val appLockEnterPasswordLabel = "كلمة المرور أو الرمز"
    override val appLockPatternTooShort = "يجب توصيل 4 نقاط على الأقل"
    override val appLockBioReadyMsg = "سيتم تأكيد الهوية بالبصمة عند فتح التطبيق."
    override val appLockBioEnrollHint = "تأكد من تفعيل بصمة الإصبع في إعدادات الجهاز."
    override val appLockNewPinLabel = "أدخل الرمز الرقمي (4–8 أرقام)"
    override val appLockConfirmLabel = "تأكيد الرمز"
    override val appLockNewPassLabel = "أدخل رمز الكتابة أو الأرقام"
    override val appLockDrawPatternHint = "ارسم النمط (4 نقاط على الأقل)"
    override val appLockDrawPatternConfirmHint = "أعد رسم النمط للتأكيد"
    override val appLockPatternMismatch = "النمطان غير متطابقين، أعد الرسم"
    override val appLockPinTooShort = "أدخل 4 أرقام على الأقل"
    override val appLockPassTooShort = "أدخل 3 أحرف أو أرقام على الأقل"
    override val appLockSecretMismatch = "الرمزان غير متطابقين"
    override val lockNowBtn = "قفل التطبيق الآن"

    override val confirmCoverDialogTitle = "تغيير صورة غلاف الحساب؟"
    override val confirmCoverDialogBody = "هل أنت متأكد أنك تريد اختيار صورة جديدة وتعيينها غلافاً لحسابك؟"
    override val confirmCoverProceedBtn = "متابعة واختيار صورة"

    override val cinematicBadge = "سينمائي تفاعلي • خلفيات وإطارات"
    override val darkVariantBtn = "مظلم"
    override val lightVariantBtn = "مضيء"
}

object EnglishJesrStrings : JesrStrings {
    override val isEnglish = true
    override val appName = "Jesr"
    override val back = "Back"
    override val cancel = "Cancel"
    override val save = "Save"
    override val confirm = "Confirm"
    override val close = "Close"
    override val undo = "Undo"
    override val retry = "Retry"
    override val accept = "Accept"
    override val reject = "Decline"
    override val freeLabel = "Free"

    override val navHome = "Home"
    override val navPrivacy = "Privacy"
    override val navAppearance = "Appearance"
    override val navThemeStore = "Theme Store"
    override val navSettings = "Settings"
    override val navProfile = "Profile"
    override val signInWithGoogle = "Sign in with Google"
    override val connectedWithGoogle = "Signed in with Google"
    override val switchGoogleAccount = "Switch Google Account"
    override val signOut = "Sign out"

    override val addFriendsCd = "Add Friends"
    override val searchFriendsCd = "Search friends and chats"
    override val searchPlaceholder = "Search by name, Jesr ID, or message..."
    override val clearSearchCd = "Clear search"
    override val emptyFriendsHint = "No friends yet\nTap the (+) button at the bottom to add friends"
    override val noSearchResults = "No matching results found"
    override val statusBlocked = "Blocked"
    override val statusAwaitingResume = "Awaiting mutual resume"
    override val statusOnlineMesh = "Connected on Mesh"
    override val statusOffline = "Offline"
    override val mutedCd = "Muted"
    override val openConversation = "Open conversation"
    override val friendSettingsMenu = "Friend settings"
    override val muteNotifications = "Mute notifications"
    override val unmuteNotifications = "Unmute notifications"
    override val blockFriend = "Block friend"
    override val unblockFriend = "Unblock friend"
    override val removeFriend = "Remove friend"

    override val changeProfilePicCd = "Change profile photo"
    override val tapPhotoToChangeHint = "Tap photo to select and crop"
    override val displayNameLabel = "Display Name"
    override val editNameCd = "Edit name"
    override val uniqueJesrIdLabel = "Unique Jesr ID"
    override val copyIdOnlyCd = "Copy ID only"
    override val defaultUserName = "Jesr User"
    override val cropTitle = "Crop Profile Photo"
    override val cropSubtitle = "Drag or pinch to frame your avatar"
    override val cropRotate = "Rotate 90°"
    override val cropZoom = "Zoom"
    override val cropConfirm = "Crop & Save"
    override val cropLoading = "Loading image..."
    override val cropLoadError = "Could not load selected image"

    override val addFriendsTitle = "Add Friends"
    override val addFriendsSubtitle = "Choose how to connect and manage requests"
    override val hubAddByIdTitle = "Add by User ID"
    override val hubAddByIdSubtitle = "Search by unique Jesr ID and send a friend request"
    override val hubNearbyTitle = "Nearby People"
    override val hubNearbySubtitle = "Discover nearby opted-in users in local range"
    override val hubRequestsTitle = "Friend Requests"
    override val hubRequestsSubtitle = "View incoming requests and accept or decline"
    override val hubSentTitle = "Requests I Sent"
    override val hubSentSubtitle = "Track status of requests you sent or cancel them"
    override val addByIdTitle = "Add by ID"
    override val addByIdSubtitle = "Enter a unique Jesr ID to look up an account"
    override val addByIdInputLabel = "Jesr ID (e.g. JESR-4K89-M2W9)"
    override val lookupAccountBtn = "Search Account"
    override val availableLocally = "Available in local Mesh range"
    override val alreadyFriends = "Already Friends"
    override val friendRequestSentCheck = "Friend Request Sent ✓"
    override val sendFriendRequestBtn = "Send Friend Request"
    override val accountNotFound: (String) -> String = { id ->
        "No account found matching ($id). Check the ID spelling or make sure the device is in local range."
    }

    override val nearbyTitle = "Nearby People"
    override val nearbySubtitle = "Users opted into local nearby discovery"
    override val manualRefreshCd = "Refresh"
    override val optionalRadiusLabel = "Optional Search Range"
    override val radiusAll = "All Ranges"
    override val radiusClose = "Close"
    override val radiusMedium = "Medium"
    override val radiusWide = "Wide"
    override val noNearbyPeopleHint = "No opted-in nearby users detected right now\nTry refreshing manually"
    override val friendBadge = "Friend"
    override val sentBadge = "Sent"
    override val sendRequestShort = "Add"

    override val requestsTitle = "Friend Requests"
    override val requestsSubtitle = "Swipe to switch quickly between tabs"
    override val refreshRequestsCd = "Refresh requests"
    override val incomingTabCount: (Int) -> String = { c -> "Incoming ($c)" }
    override val sentTabCount: (Int) -> String = { c -> "Requests I Sent ($c)" }
    override val noIncomingRequests = "No incoming friend requests right now"
    override val noSentRequests = "No pending requests you sent"
    override val mutualFriendsCount: (Int) -> String = { c -> "Mutual friends: $c" }
    override val statusAwaitingReply: (String) -> String = { t -> "Pending reply • $t" }

    override val backToFriendsCd = "Back to friends list"
    override val e2eeCd = "End-to-end encrypted"
    override val chatLockedBlocked = "Chat locked (Blocked)"
    override val chatAwaitingResume = "Awaiting mutual resume approval"
    override val chatOnlineRange = "Connected in Mesh range"
    override val chatDirectUnavailable = "Direct link unavailable"
    override val chatSettingsCd = "Conversation settings"
    override val viewFriendProfile = "View friend profile"
    override val friendPrivacySettings = "Friend privacy settings"
    override val directLostBanner = "Direct link lost — messages are saved and will deliver automatically without duplicates on reconnect"
    override val friendBlockedTitle: (String) -> String = { n -> "$n is blocked" }
    override val friendBlockedBody = "Messaging is paused and chat history is locked without deleting messages. Unblocking requires both sides to approve resuming."
    override val unblockBtn = "Unblock"
    override val resumeInviteTitle = "Resume Chat Invitation"
    override val resumeApprovedWaitingBody: (String) -> String = { n -> "Your approval is recorded. Chat history and messaging will unlock once $n approves." }
    override val resumeHiddenUntilMutualBody = "Unblocked. Chat history stays locked until both users approve resuming."
    override val approveResumeBtn = "Approve Resume"
    override val emptyChatHint = "No messages in this conversation yet.\nStart chatting securely via E2EE."
    override val attachmentPrefix: (String) -> String = { name -> "Attachment: $name" }
    override val removeAttachmentCd = "Remove attachment"
    override val sosActiveBanner = "Emergency SOS Active"
    override val attachPhotoCd = "Attach image"
    override val sosCd = "Emergency SOS"
    override val emergencyPlaceholder = "Emergency message..."
    override val messagePlaceholder = "Write a message..."
    override val sendCd = "Send"
    override val attachedImageDefaultName = "Attached image"
    override val sentFromDistance: (Float) -> String = { d -> "Sent from: ${formatEstimatedDistance(d, true)}" }
    override val deliveryLocal = "Local"
    override val deliveryHop: (Int) -> String = { hops -> if (hops > 1) "Hop $hops" else "Sending" }
    override val deliverySending = "Sending"
    override val deliveryQueued = "Queued"
    override val deliveryDelivered = "Delivered"
    override val deliveryFailed = "Failed"

    override val blockDialogTitle: (String) -> String = { n -> "Block $n?" }
    override val blockDialogBody = "Communication will pause and chat history will be temporarily locked without deleting messages. Resuming later requires mutual consent."
    override val confirmBlockBtn = "Confirm Block"
    override val removeDialogTitle: (String) -> String = { n -> "Remove $n permanently?" }
    override val removeDialogBody = "• Your friendship will end.\n• This chat and all messages will be deleted mutually on both accounts.\n• Note: App-level deletion does not remove screenshots, exported files, or external copies."
    override val confirmRemoveBtn = "Confirm Remove"
    override val resumeSecondTitle = "Second Confirmation to Resume"
    override val resumeSecondBody: (String) -> String = { n -> "Confirm approving resuming chat with $n? Chat history returns only after both users approve." }
    override val yesApproveBtn = "Yes, Approve"
    override val deleteAllReqTitle = "Delete All Incoming Requests?"
    override val deleteAllReqBody: (Int) -> String = { c -> "All $c incoming friend request(s) will be permanently deleted from this device." }
    override val deleteAllBtn = "Delete All"
    override val statusMeshOnline = "Status: Online in Mesh"
    override val statusMeshOffline = "Status: Currently offline"
    override val keyFingerprint: (String) -> String = { fp -> "Key fingerprint: $fp..." }
    override val incomingMeshReqTitle = "Nearby Mesh Friend Request"
    override val incomingMeshReqBody: (String) -> String = { id -> "Device $id wants to add you as a friend and exchange E2EE keys." }

    override val friendSettingsTitle = "Friend Settings"
    override val friendNotFound = "Friend data not found."
    override val msgDistancePrivacyTitle = "Message Distance Privacy"
    override val msgDistancePrivacyLabel = "Allow this friend to see the distance my messages are sent from."
    override val mutualFriendsPrivacyTitle = "Mutual Friends Privacy"
    override val mutualFriendsPrivacyLabel = "Allow this friend to see my mutual friends."
    override val manageFriendTitle = "Manage Friend"
    override val muteFriend = "Mute Friend"
    override val globalPrivacyTitle = "Global Privacy Settings"
    override val globalPrivacySubtitle = "Off by default • Per-friend overrides take precedence"
    override val globalDistanceTitle = "Allow all friends to see message distance"
    override val globalDistanceSubtitle = "Off by default; override per friend below"
    override val globalMutualTitle = "Allow all friends to see mutual friends"
    override val globalMutualSubtitle = "Off by default; independent of distance privacy"
    override val reqPrivacyTitle = "Friend Request Privacy"
    override val reqPrivacySubtitle = "Mute or block incoming requests & clear requests"
    override val friendExceptionsHeader = "Friend Overrides (Independent per friend)"
    override val noFriendsForExceptions = "No friends added yet to configure overrides."
    override val seeMsgDistanceLabel = "See message distance"
    override val seeMutualFriendsLabel = "See mutual friends"
    override val reqPrivacyScreenTitle = "Friend Request Privacy"
    override val reqPrivacyScreenSubtitle = "Manage notification mute and silent block separately"
    override val muteReqSystemTitle = "Mute Friend Requests"
    override val muteReqSystemSubtitle = "Mute all incoming friend requests\n(Hides alerts only without blocking receipt)"
    override val blockReqSystemTitle = "Block Friend Requests"
    override val blockReqSystemSubtitle = "Block all incoming friend requests\n(Silently drops requests without notifying sender)"
    override val addIdExceptionTitle = "Add Per-ID Exception"
    override val addExceptionCd = "Add exception"
    override val individualRulesHeader = "Individual Rules & Exceptions"
    override val muteUserRequests = "Mute requests"
    override val blockUserRequestsSilently = "Silently block requests"
    override val deleteAllIncomingBtn: (Int) -> String = { c -> "Delete All Incoming ($c)" }

    override val appearanceTitle = "Appearance"
    override val appearanceSubtitle = "Presets, My Themes & Customization"
    override val quickRestoreOriginalBtn = "Restore Original Theme (Base UI)"
    override val fixedPresetsHeader = "Fixed Modes (Locked)"
    override val autoOriginalTitle = "Auto (Original Theme)"
    override val autoOriginalSubtitle = "Follows system appearance with Jesr's base theme"
    override val sunMoonToggleDarkCd = "Dark Mode - Tap to switch to Light Mode"
    override val sunMoonToggleLightCd = "Light Mode - Tap to switch to Dark Mode"
    override val myThemesHeader = "Downloaded Themes"
    override val storeBtn = "Store"
    override val unDeletableCd = "Protected Base"
    override val tagFreeProtected = "Free • Protected Base"
    override val tagMyPaid = "My Design • $1"
    override val tagMyFree = "My Design • Free"
    override val tagAcquiredPaid = "Owned • $1"
    override val tagFree = "Free"
    override val activeLabel = "Active"
    override val applyLabel = "Apply"
    override val customHeader = "Customize (Editable Mode)"
    override val customTitle = "Custom"
    override val customSubtitle = "Separate background, icon & light controls with Undo"
    override val showLightTitle = "Show Light & Glow"
    override val lightEnabledSub = "Light is enabled"
    override val lightDisabledSub = "Light is off"
    override val bgColorTitle = "Background Color"
    override val bgColorSub = "Customize background color independently"
    override val iconColorTitle = "Icon Color"
    override val iconColorSub = "Customize icon tint independently"
    override val lightColorTitle = "Light Color"
    override val lightColorSubEnabled = "Customize light beam & glow independently"
    override val lightColorSubDisabled = "Light is off — enable above to preview"
    override val baseColorTitle = "Base Color"
    override val baseColorSub = "Saved permanently — cannot be deleted"
    override val restoreBtn = "Restore"
    override val undoLastChange = "Undo last change"
    override val noUndoChanges = "No changes to undo"
    override val applyColorPrefix: (String) -> String = { name -> "Apply $name" }
    override val hideLabel = "Hide"
    override val pickColorLabel = "Pick color"
    override val selectedLabel = "Selected"
    override val lockedModeCd = "Locked fixed preset"
    override val lightStatusCd = "Light status"
    override val undoApplyCd = "Undo last change"

    override val themeStoreTitle = "Theme & UI Store"
    override val themeStoreSubtitle = "All Themes Free • Get any theme to auto-equip & move to Downloaded Themes"
    override val storeGoogleSyncBanner = "Sign in with Google to sync downloaded themes in the cloud"
    override val filterAll = "All"
    override val filterFree = "Free"
    override val storeEmptyCategory = "No remaining themes in this category right now\nFind all acquired themes under Downloaded Themes in Appearance!"
    override val storeAllAcquiredMsg = "All themes are now in Downloaded Themes under Appearance"
    override val designerPrefix: (String) -> String = { creator -> "By: $creator" }
    override val storeAppliedNow = "Applied"
    override val getThemeBtn = "Get Theme"
    override val badgeBaseProtected = "Base • Protected"
    override val downloadedAppliedBadge = "Downloaded & Applied ✨"
    override val removeDownloadedCd = "Remove & return to Store"
    override val snowFrostLightTitle = "Frost & Falling Snow"
    override val snowFrostEnabledSub = "Snowflakes & frost mist drift across the mountain"
    override val snowFrostDisabledSub = "Clear mountain air without falling snow"

    override val settingsTitle = "Settings"
    override val settingsSubtitle = "Local Mesh, Language & Radio Preferences"
    override val languageSectionTitle = "App Language"
    override val languageSectionSubtitle = "Instant switch between Arabic and English"
    override val arabicLabel = "العربية"
    override val englishLabel = "English"
    override val appearanceSettingsEntryTitle = "Appearance"
    override val appearanceSettingsEntrySubtitle = "Fixed modes, My Themes, customizer & original theme"
    override val meshSwitchTitle = "Enable Jesr Local Mesh"
    override val meshSwitchSubtitle = "Run offline discovery and relay over Bluetooth & Wi-Fi Direct"
    override val nearbyVisTitle = "Appear in Nearby People"
    override val nearbyVisSubtitle = "Off by default — lets nearby devices see you in Nearby People without sharing location"
    override val statsHeader = "Store-and-Forward Relay Stats"
    override val statRelayed = "Packets relayed for peers"
    override val statDelivered = "Messages delivered"
    override val statQueued = "Queued relay messages"
    override val statMaxTtl = "Max hop limit (TTL)"
    override val statMaxTtlVal = "7 hops"
    override val e2eeTitle = "E2EE: NIST P-256 + AES-GCM"
    override val publicKeyFingerprint: (String) -> String = { key -> "Public key fingerprint: $key..." }
    override val aboutAppLabel = "Jesr Mesh Messenger"

    override val telemetryActive = "Active"
    override val telemetryIdle = "Idle"
    override val telemetryNodes: (Int) -> String = { c -> "$c nodes" }
    override val telemetryRelayed: (Int) -> String = { c -> "$c relayed" }
    override val telemetryQueued: (Int) -> String = { c -> "$c queued" }

    override val coverPhotoTitle = "Profile Cover Photo"
    override val changeCoverBtn = "Customize Cover"
    override val removeCoverBtn = "Remove Cover"
    override val addCoverHint = "Tap to select a cover banner for your profile"
    override val cropCoverTitle = "Crop Cover Banner"
    override val cropCoverSubtitle = "Drag or pinch to frame your profile cover"

    override val appLockTitle = "App Lock"
    override val appLockSubtitle = "Protect Jesr with Fingerprint, Numeric PIN, Password, or Pattern"
    override val lockMethodNone = "Off (No Lock)"
    override val lockMethodBiometric = "Fingerprint / Biometric"
    override val lockMethodPin = "Numeric PIN"
    override val lockMethodPassword = "Text / Alphanumeric Password"
    override val lockMethodPattern = "Pattern Lock"
    override val appLockUnlockHeader = "Jesr is Locked"
    override val appLockBioHint = "Use your fingerprint to unlock Jesr"
    override val appLockPinHint = "Enter your numeric PIN to unlock"
    override val appLockPasswordHint = "Enter your password or passcode to unlock"
    override val appLockPatternHint = "Draw your unlock pattern"
    override val appLockBiometricPromptSub = "Authenticate with biometric to open Jesr"
    override val appLockUseFingerprintBtn = "Unlock with Fingerprint"
    override val appLockContinueBtn = "Continue & Unlock"
    override val appLockBackupSecretLabel = "Backup Passcode"
    override val appLockWrongSecret = "Incorrect passcode or pattern, try again"
    override val unlockBtn = "Unlock"
    override val appLockEnterPasswordLabel = "Password or Passcode"
    override val appLockPatternTooShort = "Connect at least 4 dots"
    override val appLockBioReadyMsg = "Biometric authentication will be requested when opening the app."
    override val appLockBioEnrollHint = "Make sure fingerprint is enrolled in Android settings."
    override val appLockNewPinLabel = "Enter Numeric PIN (4–8 digits)"
    override val appLockConfirmLabel = "Confirm"
    override val appLockNewPassLabel = "Enter text or alphanumeric password"
    override val appLockDrawPatternHint = "Draw pattern (at least 4 dots)"
    override val appLockDrawPatternConfirmHint = "Redraw pattern to confirm"
    override val appLockPatternMismatch = "Patterns do not match, draw again"
    override val appLockPinTooShort = "Enter at least 4 digits"
    override val appLockPassTooShort = "Enter at least 3 characters"
    override val appLockSecretMismatch = "Entries do not match"
    override val lockNowBtn = "Lock App Now"

    override val confirmCoverDialogTitle = "Change Account Cover Photo?"
    override val confirmCoverDialogBody = "Are you sure you want to pick a new photo for your account cover banner?"
    override val confirmCoverProceedBtn = "Choose Photo"

    override val cinematicBadge = "Interactive Cinematic • Animated & Framed"
    override val darkVariantBtn = "Dark"
    override val lightVariantBtn = "Light"
}

fun stringsFor(language: AppLanguage): JesrStrings = when (language) {
    AppLanguage.AR -> ArabicJesrStrings
    AppLanguage.EN -> EnglishJesrStrings
}

val LocalJesrStrings = staticCompositionLocalOf<JesrStrings> { ArabicJesrStrings }
