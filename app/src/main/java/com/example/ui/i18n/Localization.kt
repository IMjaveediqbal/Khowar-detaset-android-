package com.example.ui.i18n

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String, val isRtl: Boolean) {
    ENGLISH("en", "English", "English", false),
    KHOWAR("khw", "Khowar", "کھوار", true),
    URDU("ur", "Urdu", "اردو", true)
}

object Strings {
    private val en = mapOf(
        "app_title" to "Khowar Dataset",
        "tagline" to "Preserving Khowar. Powering AI. Building the Future.",
        "mission_desc" to "The largest open, community-driven, human-validated digital linguistic resource for the Khowar language.",
        "nav_home" to "Home",
        "nav_explore" to "Explore Data",
        "nav_contribute" to "Contribute",
        "nav_validate" to "Validation Queue",
        "nav_stats" to "Statistics",
        "nav_research" to "API & Research",
        "nav_admin" to "Governance & Admin",
        "nav_docs" to "Documentation",
        "nav_profile" to "My Profile",
        
        // Actions
        "btn_explore" to "Explore Dataset",
        "btn_contribute" to "Contribute Data",
        "btn_add_word" to "Add Word",
        "btn_add_sentence" to "Add Sentence",
        "btn_record_voice" to "Record Voice",
        "btn_add_story" to "Add Story / Text",
        "btn_add_knowledge" to "Add Cultural Knowledge",
        "btn_upload_image" to "Upload Image",
        "btn_submit" to "Submit for Human Validation",
        "btn_approve" to "Approve & Publish",
        "btn_reject" to "Reject Record",
        "btn_request_changes" to "Request Changes",
        "btn_generate_api_key" to "Generate API Key",
        "btn_export_dataset" to "Download Verified Dataset",
        "btn_record" to "Start Recording",
        "btn_stop_record" to "Stop & Save",
        "btn_play" to "Play Audio",
        "btn_pause" to "Pause",
        
        // Dataset Stats
        "stat_total_words" to "Lexicon Entries",
        "stat_total_sentences" to "Parallel Sentences",
        "stat_speech_hours" to "Speech Corpus Hours",
        "stat_contributors" to "Active Contributors",
        "stat_at_a_glance" to "Our Dataset at a Glance",
        "stat_real_calculated" to "Real-time metrics computed directly from human-validated database records.",
        
        // Empty States
        "empty_no_data" to "No data has been contributed yet.",
        "empty_no_contributors" to "No contributors yet.",
        "empty_no_verified" to "No verified records yet.",
        "empty_stats_growing" to "Statistics will appear as the dataset grows.",
        "empty_queue_clean" to "Validation queue is empty. All submissions are up to date.",
        "empty_my_contributions" to "Your contribution history will appear here once you submit data.",
        
        // Trust Badges
        "badge_open" to "Open & Free",
        "badge_community" to "Community Driven",
        "badge_validated" to "Human Validated",
        "badge_privacy" to "Privacy First",
        "badge_license" to "CC BY-SA 4.0",
        
        // Legal & Consent
        "consent_title" to "Contributor Consent & License Agreement",
        "consent_body" to "I confirm that I hold the rights or native linguistic knowledge to submit this data. I consent to its publication under Creative Commons Attribution-ShareAlike 4.0 (CC BY-SA 4.0) for research, education, and language technology.",
        "consent_checkbox" to "I understand and agree to the open dataset license terms."
    )

    private val khw = mapOf(
        "app_title" to "کھوار ڈیٹاسیٹ",
        "tagline" to "کھوار زبانو تحفظ۔ مصنوعی ذہانتو ترقی۔ روشن مستقبل۔",
        "mission_desc" to "کھوار زبانو تحفظ، تحقیق اور جدید ڈیجیٹل ٹیکنالوجیا کوری اوپن کمیونٹی پلیٹ فارم۔",
        "nav_home" to "گھریلو صفحہ",
        "nav_explore" to "ڈیٹاسیٹ لوریٹ",
        "nav_contribute" to "کھوار مواد داخل کوریت",
        "nav_validate" to "تصدیقو فہرست",
        "nav_stats" to "اعداد و شمار",
        "nav_research" to "تحقیق اور API",
        "nav_admin" to "انتظام",
        "nav_docs" to "رہنمائی",
        "nav_profile" to "مہ پروفائل",
        
        // Actions
        "btn_explore" to "ڈیٹا لوریٹ",
        "btn_contribute" to "مواد داخل کوریت",
        "btn_add_word" to "لفظ داخل کوریت",
        "btn_add_sentence" to "جملہ داخل کوریت",
        "btn_record_voice" to "آواز ریکارڈ کوریت",
        "btn_add_story" to "قصہ / کہانی",
        "btn_add_knowledge" to "ثقافتی معلومات",
        "btn_upload_image" to "تصویر اپلوڈ کوریت",
        "btn_submit" to "تصدیقہ روانہ کوریت",
        "btn_approve" to "منظور اور شائع کوریت",
        "btn_reject" to "رد کوریت",
        "btn_request_changes" to "درستگیو درخواست",
        "btn_generate_api_key" to "نواں API کی بناوت",
        "btn_export_dataset" to "تصدیق شدہ ڈیٹا ڈاؤنلوڈ کوریت",
        "btn_record" to "ریکارڈنگ شروع کوریت",
        "btn_stop_record" to "بند کوریت اور محفوظ",
        "btn_play" to "آواز بوشیت",
        "btn_pause" to "روکیت",
        
        // Dataset Stats
        "stat_total_words" to "کھوار الفاظ",
        "stat_total_sentences" to "کھوار جملے",
        "stat_speech_hours" to "آوازی گھنٹے",
        "stat_contributors" to "معاونین",
        "stat_at_a_glance" to "ڈیٹاسیٹو خلاصہ",
        "stat_real_calculated" to "ڈیٹابیسو زندہ ریکارڈان مطابق تیار اعداد و شمار۔",
        
        // Empty States
        "empty_no_data" to "ہنیسو تان کھوار ڈیٹا داخل نو بیتی شئے!",
        "empty_no_contributors" to "ہنیسو تان معاونین نو بیتی شینی۔",
        "empty_no_verified" to "ہنیسو تان تصدیق شدہ ریکارڈ نو شئے!",
        "empty_stats_growing" to "ڈیٹاسیٹو ودھیو سورا اعداد و شمار ظاہر بونی۔",
        "empty_queue_clean" to "تصدیقو فہرست صاف شئے!",
        "empty_my_contributions" to "تہ داخل کڑدو ڈیٹا ہیا ظاہر بوئے۔",
        
        // Trust Badges
        "badge_open" to "مفت اور اوپن",
        "badge_community" to "کمیونٹیو محنت",
        "badge_validated" to "ماہرین تصدیق شدہ",
        "badge_privacy" to "رازداری اول",
        "badge_license" to "CC BY-SA 4.0",
        
        // Legal & Consent
        "consent_title" to "رضامندی اور لائسنس معاہدہ",
        "consent_body" to "آوا تصدیق کوروم کہ ہیا مواد مہ ذاتی یا ثقافتی علم شئے اور آوا کھوار تحفظو بچے ہیا مواد مفت اوپن لائسنسو تحت شائع کوریکو اجازت دوم۔",
        "consent_checkbox" to "آوا اوپن لائسنسو شرطین سورا متفق شوم۔"
    )

    private val ur = mapOf(
        "app_title" to "کھوار ڈیٹاسیٹ",
        "tagline" to "کھوار کا تحفظ۔ مصنوعی ذہانت کی ترقی۔ مستقبل کی تعمیر۔",
        "mission_desc" to "کھوار زبان کے تحفظ، لسانی تحقیق اور اے آئی ٹیکنالوجی کے لیے سب سے بڑا انسانی تصدیق شدہ اوپن ڈیٹا پلیٹ فارم۔",
        "nav_home" to "ہوم",
        "nav_explore" to "ڈیٹا دریافت کریں",
        "nav_contribute" to "مواد شامل کریں",
        "nav_validate" to "تصدیق کی قطار",
        "nav_stats" to "اعداد و شمار",
        "nav_research" to "تحقیق اور API",
        "nav_admin" to "انتظامیہ",
        "nav_docs" to "دستاویزات",
        "nav_profile" to "میرا پروفائل",
        
        // Actions
        "btn_explore" to "ڈیٹاسیٹ دیکھیں",
        "btn_contribute" to "مواد دیں",
        "btn_add_word" to "لفظ شامل کریں",
        "btn_add_sentence" to "جملہ شامل کریں",
        "btn_record_voice" to "آواز ریکارڈ کریں",
        "btn_add_story" to "کہانی / متن",
        "btn_add_knowledge" to "ثقافتی معلومات",
        "btn_upload_image" to "تصویر اپلوڈ",
        "btn_submit" to "تصدیق کے لیے بھیجیں",
        "btn_approve" to "منظور و شائع کریں",
        "btn_reject" to "مسترد کریں",
        "btn_request_changes" to "تبدیلی کی درخواست",
        "btn_generate_api_key" to "نیا API Key بنائیں",
        "btn_export_dataset" to "تصدیق شدہ ڈیٹا ڈاؤنلوڈ کریں",
        "btn_record" to "ریکارڈنگ شروع",
        "btn_stop_record" to "مکمل اور محفوظ کریں",
        "btn_play" to "سنیں",
        "btn_pause" to "روکیں",
        
        // Dataset Stats
        "stat_total_words" to "الفاظ کی تعداد",
        "stat_total_sentences" to "جملوں کی تعداد",
        "stat_speech_hours" to "آواز کے گھنٹے",
        "stat_contributors" to "فعال معاونین",
        "stat_at_a_glance" to "ڈیٹاسیٹ کا خلاصہ",
        "stat_real_calculated" to "ڈیٹابیس کے تصدیق شدہ ریکارڈ سے تیار کردہ لائیو اعداد و شمار۔",
        
        // Empty States
        "empty_no_data" to "ابھی تک کوئی ڈیٹا شامل نہیں کیا گیا۔",
        "empty_no_contributors" to "ابھی تک کوئی معاونین نہیں۔",
        "empty_no_verified" to "ابھی کوئی تصدیق شدہ ریکارڈ موجود نہیں۔",
        "empty_stats_growing" to "ڈیٹا کے اضافے کے ساتھ اعداد و شمار ظاہر ہوں گے۔",
        "empty_queue_clean" to "تصدیق کی قطار مکمل ہے، کوئی نیا ریکارڈ باقی نہیں۔",
        "empty_my_contributions" to "آپ کی جمع کردہ معلومات یہاں ظاہر ہوں گی۔",
        
        // Trust Badges
        "badge_open" to "اوپن اور مفت",
        "badge_community" to "کمیونٹی کا تعاون",
        "badge_validated" to "انسانی تصدیق شدہ",
        "badge_privacy" to "رازداری کا تحفظ",
        "badge_license" to "CC BY-SA 4.0",
        
        // Legal & Consent
        "consent_title" to "معاون کی رضامندی اور لائسنس کا معاہدہ",
        "consent_body" to "میں تصدیق کرتا/کرتی ہوں کہ یہ مواد درست ہے اور میں اسے کھوار زبان کے تحفظ اور تحقیقی استعمال کے لیے CC BY-SA 4.0 کے تحت شائع کرنے کی اجازت دیتا/دیتی ہوں۔",
        "consent_checkbox" to "میں شرائط و ضوابط سے اتفاق کرتا ہوں۔"
    )

    fun get(key: String, language: AppLanguage): String {
        val dict = when (language) {
            AppLanguage.ENGLISH -> en
            AppLanguage.KHOWAR -> khw
            AppLanguage.URDU -> ur
        }
        return dict[key] ?: en[key] ?: key
    }
}
