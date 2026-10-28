# Sado — Android musiqa pleyeri

Telefondagi musiqa fayllarini ijro etuvchi oflayn pleyer. Interfeys iPhone'dagi
«Liquid Glass» uslubida: pastki menyu, mini-pleyer va pleyer tugmalari ortidagi
tasvirni linzadek sindiradi.

## Imkoniyatlar

- **Kutubxona:** qo'shiqlar, albomlar, ijrochilar, papkalar; 4 xil saralash; aralashtirib ijro
- **Pleyer:** muqova butun ekranda, shisha boshqaruv paneli, vaqt chizig'i, aralashtirish, takrorlash (hammasi / bittasi)
- **Navbat:** keyingi qo'shiqlar, surib tartiblash, olib tashlash, tozalash
- **Ekvalayzer:** telefon polosalari, 8 ta tayyor sozlama, bass kuchaytirish, 3D ovoz
- **Pleylistlar:** yaratish, nomini o'zgartirish, o'chirish; aqlli ro'yxatlar (Sevimlilar, Ko'p tinglangan, Yaqinda tinglangan, Yangi qo'shilgan)
- **Qidiruv:** qo'shiq / ijrochi / albom bo'yicha, ovozli qidiruv, qidiruv tarixi
- **Uyqu taymeri:** 15 / 30 / 60 / 90 daqiqa
- Fon rejimida ijro, bildirishnoma va qulf ekranidan boshqarish, quloqchin uzilsa pauza
- Ilova qayta ochilganda oxirgi navbat va joy tiklanadi

## Shisha effekti qanday ishlaydi

| Android versiyasi | Effekt |
|---|---|
| 13 va yuqori | Haqiqiy sinish (AGSL shader) + rang ajralishi + yengil blur |
| 12 | Blur'li shisha |
| 8 – 11 | Yarim shaffof to'q panel |

Kod: `app/src/main/java/uz/sado/player/ui/glass/LiquidGlass.kt`

## APK olish (GitHub Actions)

`main` tarmog'iga har bir push'dan keyin GitHub Actions avtomatik APK yig'adi va
**Releases** bo'limiga joylaydi. Doimiy yuklab olish havolasi:

```
https://github.com/<FOYDALANUVCHI>/<REPO>/releases/latest/download/Sado.apk
```

Telefonda shu havolani oching → yuklab oling → o'rnating
(birinchi marta «Noma'lum manbalardan o'rnatish»ga ruxsat so'raladi).

Yangi versiya eski versiya ustiga o'rnatiladi — sevimlilar va pleylistlar saqlanib qoladi.

### Imzo kaliti haqida

`app/release.jks` — ilovaning imzo kaliti. U loyihada turgani uchun har bir yangi APK
eskisining ustiga muammosiz o'rnatiladi. Shaxsiy foydalanish uchun bu yetarli.
Ilovani Google Play'ga chiqarmoqchi bo'lsangiz, yangi kalit yarating va parollarni
repozitoriya **Settings → Secrets** bo'limiga `SADO_STORE_PASSWORD`, `SADO_KEY_ALIAS`,
`SADO_KEY_PASSWORD` nomlari bilan qo'shing.

## Kompyuterda yig'ish

Android Studio'da loyihani oching yoki:

```
gradle assembleDebug
```

Talablar: JDK 17, Android SDK 35, Gradle 8.11+.

## Texnologiyalar

Kotlin 2.1 · Jetpack Compose · Material 3 · Media3 (ExoPlayer + MediaSession) · AGSL
