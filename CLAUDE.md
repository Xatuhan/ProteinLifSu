# Protein Lif Su — proje notları

Yerel çalışan (offline) Android protein / lif / su takipçisi. Kotlin + Jetpack Compose (Material 3),
Room (KSP), tek modül `app`. Ekranlar: ana ekran, Takvim, Ayarlar, Kendi yemeğin.

## Durum (2026-11-03)
- FAZ 5: açılış ANR'si çözüldü. Kök neden: ana iş parçacığında `LocalDate.format` (ay/gün adı → ICU yükleme,
  emülatörde ~1,65 sn). KURAL: ay/gün adı içeren tarih biçimlemesi YALNIZ ViewModel'de (Default) yapılır
  (`TrackerUiState.dateTitle/dateSubtitle`, `HistoryUiState.title`); composable'da `DateTimeFormatter` ile
  metinli alan biçimleme yok (saat "HH:mm" serbest). USDA ön yüklemesi ilk kareden sonra (IdleHandler),
  arka plan önceliğinde. Debug uygulama adı "Protein Lif Su Debug"; emülatör 2 GB RAM'de takaslıyor, debug
  orada 5 sn+ kareler verir — performans ölçümü release'te yapılır. Profil: `am start --start-profiler`
  + dmtrace ayrıştırma (simpleperf emülatörde çalışmıyor; Git Bash'te `MSYS_NO_PATHCONV=1`).

## Durum (2026-11-02)
- Günlük kayıt: aynı şeyler birleşir (`LogGroup`, `grouped()`; tüm su tek grup, besinler kimliğe göre);
  dokununca ayrıntılar (saat, ölçü, miktar, tek tek silme) açılır. Kullanıcı tercihi: kayıt satırlarında
  gri fon YOK; ayrım ince alt çizgi, açık grup içi sol dikey kılavuz çizgisiyle.
- FAZ 4 bitti: 121 Türk yemeği (standart tarif + USDA malzeme verisiyle protein/lif/su hesabı,
  `tools/turkish/`), çorba/içecek suyunun su göstergesine eklenmesi (sıvı besinler), USDA'ya su sütunu
  (934 sıvı). 32 test geçiyor; release 1,82 MB. TürKomp (resmi Türk veritabanı) 2026-11-02'de erişilemedi
  (TLS reset/zaman aşımı, VPN kapalıyken) → tarif hesabı yöntemi seçildi.
- FAZ 3 bitti: USDA tam veri seti (13 397 besin, Türkçe otomatik çeviri), dikey arama listesi, USDA ev ölçüleri.
  28 birim testi geçiyor (gerçek varlık dosyası dahil); release 1,77 MB; emülatörde denendi.
- FAZ 2: takvim (gün/hafta/ay), karanlık mod, TR/EN, kendi yemeğin (etiketten/tariften).
- Fiziksel cihazda (SM-A566B) deneme BEKLİYOR. Git deposu 2026-11-03'te kuruldu (dal `main`, uzak depo yok).

## Derleme
```
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
export JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustStore=C:/Users/xatuh/.gradle-trust/cacerts -Djavax.net.ssl.trustStorePassword=changeit"
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
```
- `JAVA_TOOL_OPTIONS` yalnız bu bilgisayardaki Norton TLS araya girmesi için: `~/.gradle-trust/cacerts`
  = JBR cacerts + Norton kökü (parmak izi F42E52D3…946A). Norton kökü yenilenirse bu kopyayı yeniden üret.
  `trustStoreType=Windows-ROOT` Gradle'da ÇALIŞMADI. Bu ayarı proje `gradle.properties`'e koyma.
- Sürümler: AGP 9.1.1 (9.3.1, Gradle 9.3.1 ile `ProjectTypeBinding` hatası verdi), Gradle 9.3.1,
  Kotlin 2.2.10, KSP 2.3.13, Room 2.8.5, Compose BOM 2025.12.00 (compileSdk 36 ile uyumlu son set).
- AGP 9: yerleşik Kotlin (kotlin-android eklentisi YOK); `resConfigs` yerine `androidResources.localeFilters`.
- Release, yerel deneme için debug anahtarıyla imzalı; debug paketi `com.proteinlifsu.debug`.
- Bash heredoc içinde Kotlin/Türkçe metin yazmak tırnak yüzünden iki kez bozuldu: dosyaları Write aracıyla yaz.

## Mimari
- `domain/`: Android'den bağımsız. `InputValidator` (tek kaynaklı sınırlar), `FoodCatalog`, `Recipe`,
  `Stats` (dönem özeti), `Fmt` (ayırıcı dile göre, String.format yok).
- `data/`: `TrackerRepository` arayüzü → `RoomTrackerRepository`. DB v3 (`AutoMigration(1,2)`, `(2,3)`; şemalar
  `app/schemas/`). Kayıt ve özel besinler UUID + `updatedAt` + `isSynced` + `isDeleted`; 30 günden eski
  silme kayıtları açılışta temizlenir. `addFood` değerleri çağırandan değil sözlükten/DB'den okur.
- `ui/`: `TrackerViewModel` (gün, ayarlar, özel besinler), `HistoryViewModel` (takvim). Gezinme kütüphanesiz
  (`MainActivity` içinde enum + BackHandler). Metinler `ui/i18n/Strings.kt` (TR/EN Kotlin nesnesi,
  `LocalStrings`); Android string kaynakları yalnız `app_name`. Renkler `LocalAppColors` (açık/koyu).

## Sınırlar (kullanıcı kararı)
- Su: tek seferde en fazla **1000 ml** (hazır ölçüler 200/330/500/1000; 1,5 L kaldırıldı).
- Besin: tek seferde en fazla 5000 g; besine özel `maxGramsPerEntry` → **whey tozu 50 g**.
- Özel besin en fazla 500; tarifte en fazla 40 malzeme, pişmiş ağırlık ≤ 20 000 g.

## Besin verisi
- **Tam veri seti (FAZ 3):** `app/src/main/assets/usda_foods.tsv` = 13 397 USDA besini (SR Legacy 2018-04 +
  FNDDS 2021-2023 + Foundation 2026-10), ≈2,6 MB metin, APK içinde 434 KB. Üretici: `tools/usda/build_foods.py`
  + sözlük `tools/usda/glossary.py` (Türkçe adlar sözlük tabanlı otomatik çeviri; çevrilmeyen kelime ≈%7,
  çoğu marka/özel ad). Yeniden üretmek (indirilmiş yerel dosyalarla, ağ yok):
  `py -I tools/usda/build_foods.py --sr <SR klasörü> --fndds <surveyDownload.json> --found <foundation.json> --out app/src/main/assets/usda_foods.tsv`.
  Kaynak dosyalar repoda değil (≈80 MB); yalnız https://fdc.nal.usda.gov'dan indirilir. USDA FDC verisi kamu malıdır (CC0).
- Uygulama dosyayı açılışta arka planda (`FoodIndexProvider`, IO) okur; arama `FoodIndex` (paralel diziler,
  iki dilde, Türkçe harf duyarsız) Default'ta, 150 ms beklemeli. 692 besinde USDA lif verisi yok → `fiberKnown=false`,
  arayüz "lif verisi yok" der, lif 0 sayılır. USDA ev ölçüleri (`HOUSEHOLD`, ad `entries.portion_label`, DB v3).
- 43 elle seçilmiş besin (aşağıda) ayrıca durur, aramada önce gelir; tam setten çıkarıldı (çift kayıt yok).
- **Türk yemekleri (FAZ 4):** `tools/turkish/recipes.py` (121 tarif, gram + verim) → `build_turkish.py` →
  ÜRETİLMİŞ `domain/TurkishDishes.kt` (elle düzenleme). Su = pişmiş ağırlık − kuru madde; protein/lif korunur.
  Kategori ölçüleri: çorba kase 200/250/350 g; sıvı (çorba, içecek, cacık) → `Food.liquid`, eklenince
  `waterMlFor(g)` ml su göstergesine yazılır (`entries.water_ml`). Kuzu yemeklerinde USDA kuzu kıyması kullanıldı.
  Yeniden üretim: `py -I tools/turkish/build_turkish.py --sr <SR klasörü> --out app/src/main/java/com/proteinlifsu/domain/TurkishDishes.kt`.
- USDA dosyasında 8. sütun su (g/100 g), 9. sütun sıvı (1/0): çorba/içecek ve "prepared/ready-to-serve"; kuru,
  toz, konsantre, condensed hariç.
- 43 yerleşik besin, protein/lif USDA FoodData Central SR Legacy (2018-04); her besinde `fdcId`.
  Birebir karşılığı olmayanlarda (beyaz peynir=feta, kaşar=cheddar, lor=ricotta, süzme=Greek, köfte=sade
  kıyma) not alanı bunu söyler; ayran = yoğurt 1:1 seyreltme (hesaplanmış). Mercimek çorbası ve leblebi
  USDA'da yok, çıkarıldı. Kase/tabak gramları tahmindir.
- Daha fazla yemek için kullanıcı **çevrimiçi arama yerine "kendi yemeğin"i** seçti (internet izni yok); ardından
  USDA veri setinin tamamının Türkçeleriyle gömülmesini istedi (FAZ 3).
- İnternet işlerinde kullanıcının açık talimatı: güvenlik birinci öncelik; yalnız resmi HTTPS kaynaklar, TLS
  doğrulaması asla kapatılmaz, Norton'a dokunulmaz, indirilen veri izole klasörde yalnız veri olarak işlenir.

## Tasarım kararı (kullanıcı tercihi)
- OLED-koyu "endüstriyel" ilk tasarım REDDEDİLDİ. Geçerli dil: sade, beyaz zemin, sıcak/cozy; koyu mod da
  aynı sadelikte (sıcak koyu gri, saf siyah değil). Canlı vurgular: turuncu #FF8A00, yeşil #00C46A,
  mavi #0095FF (koyuda biraz daha açık). Su ölçülerinde zemin yok, yalnız mavi kalın yazı.

## Güvenlik (doğrulandı, aapt2 ile)
- Sıfır izin; yalnız MainActivity exported; ProfileInstallReceiver kaldırıldı.
- `allowBackup=false`, `fullBackupContent=false`, `dataExtractionRules` her şeyi dışlar, cleartext kapalı.
- Release dex'inde `android/util/Log` referansı 0 (R8 `-assumenosideeffects`).
