# O'zbegim 0.7.0: ishoralar va menyular (TalkBack-mos)

Ishoralar TalkBack (9.1 va yangi) standartiga moslab qo'yilgan. Hammasini "Ishoralarni sozlash" ekranida o'zgartirish mumkin.
Eski versiyada saqlangan shaxsiy ishoralar yangilanishdan keyin bir marta tozalanadi.

## Bir barmoq
- O'ngga surish: keyingi element
- Chapga surish: oldingi element
- Pastga surish: tanlangan o'qish boshqaruvi bo'yicha keyingisi (sarlavha, so'z, harf va h.k.)
- Yuqoriga surish: tanlangan o'qish boshqaruvi bo'yicha oldingisi
- Yuqoriga, so'ng pastga: keyingi o'qish boshqaruvi
- Pastga, so'ng yuqoriga: oldingi o'qish boshqaruvi
- Yuqoriga, so'ng chapga: Bosh ekran
- Pastga, so'ng chapga: Orqaga
- Chapga, so'ng yuqoriga: So'nggi ilovalar
- O'ngga, so'ng pastga: Bildirishnomalar
- O'ngga, so'ng yuqoriga: Tezkor sozlamalar
- Pastga, so'ng o'ngga: O'zbegim menyusi
- Yuqoriga, so'ng o'ngga: Element amallari menyusi
- Chapga, so'ng pastga: Ekranda qidirish
- O'ngga, so'ng chapga: pastga aylantirish (slayderda qiymatni oshirish)
- Chapga, so'ng o'ngga: yuqoriga aylantirish (slayderda qiymatni kamaytirish)
- Ikki marta bosish: faollashtirish (tizim bajaradi)
- Ikki marta bosib ushlab turish: uzoq bosish (tizim bajaradi)

## Ikki barmoq
- Bir marta bosish: nutqni to'xtatish
- Ikki marta bosish: media ijro etish yoki to'xtatish
- Uch marta bosish: keyingisidan uzluksiz o'qish
- Ikki marta bosib ushlab turish: tarjima (Manba tili va Maqsad til sozlamalari bo'yicha)
- Yuqoriga, pastga, chapga, o'ngga surish: aylantirish. Qulf ekranida ikki barmoq bilan tepaga surish qulfni ochadi
- Bosh ekranda ikki barmoq bilan tepaga surish: ilovalar menyusi (rasmiy amal, «Ilovalar» tugmasi, barmoq surish ketma-ket
  sinab ko'riladi); chapga/o'ngga: bosh ekran sahifalari
- Pastga tortish: bosh ekranda (yoki ilovada aylantiriladigan narsa bo'lmasa) xabarnoma soyasi tushadi; soya ochiq bo'lsa, tezkor
  sozlamalar ochiladi. Ro'yxat boshida 3 soniya ichida ikki marta tortilsa ham soya tushadi
- Soya ochiqligida tepaga surish: soyani yopish

## Uch barmoq
- Bir marta bosish: O'zbegim menyusi
- Ikki marta bosish: nusxalash (tanlangan matn yoki fokusdagi element matni)
- Uch marta bosish: qo'yish
- Bir marta bosib ushlab turish: fokusdagi matnni harflab o'qish (har harf nomi bilan; ekranda qidirish endi chapga-pastga ishorasida va menyuda)
- Ikki marta bosib ushlab turish: ekranni sun'iy intellekt bilan tasvirlash
- Chapga surish: oldingi harf; o'ngga surish: keyingi harf (o'qish boshqaruvini almashtirmasdan, fokusdagi element matnida)
- Yuqoriga surish: oldingi so'z; pastga surish: keyingi so'z

## To'rt barmoq
- Bir marta bosish: soat va batareya
- Ikki marta bosish: ekran pardasi
- Uch marta bosish: maxfiy xabarni o'qish
- Yuqoriga surish: boshidan uzluksiz o'qish
- Pastga surish: keyingisidan uzluksiz o'qish
- Chapga surish: qo'ng'iroqni tugatish
- O'ngga surish: qo'ng'iroqqa javob berish

## O'qish boshqaruvlari
Elementlar, Sarlavhalar, Boshqaruv elementlari, Matn maydonlari, Havolalar, Harflar, So'zlar, Qatorlar, Xatboshilar, Nutq tezligi, Tafsilotlar.
Bevosita tanlash: O'zbegim menyusidagi "O'qish boshqaruvini tanlash" (yoki shu amalni ishoraga biriktiring): bir necha marta aylantirish shart emas.
"Nutq tezligi" tanlanganda yuqoriga surish tezlashtiradi, pastga surish sekinlashtiradi (qadam: Sozlamalar > Ishora bilan tezlik va ton qadami; 1 foizgacha).
Tezlik va Tafsilotlar rejimlari 12 soniya ishlatilmasa, o'zi "Elementlar"ga qaytadi.
"Harflar", "So'zlar", "Qatorlar", "Xatboshilar" Telegram xabari kabi tizim matn bo'ylab yurishni qo'llamaydigan elementlarda ham ishlaydi
(O'zbegim matn bo'ylab o'zi yuradi). Matn tugasa, keyingi elementga o'tiladi. "Qatorlar" bunday elementlarda ekran qatori emas, gap
(nuqta, undov, so'roq) yoki yangi qator bo'yicha yuradi.

## Matn bo'ylab yurish va harflab o'qish (ishoraga biriktiriladi)
Keyingi/oldingi harf, so'z, qator (gap), xatboshi; fokusdagi matnni harflab o'qish; oxirgi aytilganni harflab o'qish; ovoz tonini ko'tarish/pasaytirish.
Standart: uch barmoq bilan chapga/o'ngga - harf, yuqoriga/pastga - so'z, bosib ushlab turish - harflab o'qish. Matn boshi yoki oxiriga yetilsa
"Matn boshi" / "Matn oxiri" aytiladi. Ishoralarni "Ishoralar" ekranida o'zgartirish mumkin.

## Ro'yxatlarda yurish
Ro'yxatdagi oxirgi ko'rinadigan elementdan keyin ro'yxatdan tashqarida element (pastki panel, sarlavha) bo'lsa, ro'yxat avval aylantiriladi
va yangi element o'qiladi. Ro'yxat oxiriga yetgandagina fokus ro'yxatdan chiqadi.
"Tafsilotlar" tanlanganda pastga surish darajani oshiradi (past, o'rta, yuqori, maxsus), yuqoriga surish kamaytiradi (TalkBack'dagi "Verbosity" kabi).

## O'zbegim menyusi (pastga, so'ng o'ngga surish)
Hamma amal bir joyda: element amallari, avtomatik tarjima (bitta tugma), o'qish, qidiruv, havolalarga o'tish, rasmga tavsif berish,
ekranni tavsiflash, tarjima, tafsilotlar, O'zbegim sozlamalari, matnni nutqqa aylantirish sozlamalari, tizim amallari, holat,
nusxalash va tahrirlash, ekran pardasi, mediani boshqarish, qo'ng'iroq amallari, lug'at va yorliq, mashq, o'chirish.
Nutqni tezlashtirish va sekinlashtirish bu menyuda yo'q: u o'qish boshqaruvlarida (yuqoriga va pastga surish) o'zgaradi.
"Tizim amallari" ichida: orqaga, bosh ekran, so'nggi ilovalar, ilovalar menyusi, bildirishnomalar, tezkor sozlamalar, ekranni qulflash,
ekran rasmi, o'chirish menyusi, ekranni bo'lish. Menyuni "Yopish" tugmasi yoki Orqaga ishorasi bilan yopasiz.

## Element amallari menyusi
Fokusdagi elementning hamma amali: bosish, uzoq bosish, ochish, yig'ish, yopish, aylantirish hamda ilovaning maxsus amallari
(javob berish, o'chirish, ulashish va h.k.). Slayderda "Qiymatni oshirish" va "Qiymatni kamaytirish" chiqadi.

## Sozlamalarda
- Tafsilotlar (alohida ekran, TalkBack'dagi "Verbosity" kabi): daraja (past, o'rta, yuqori, maxsus), tugma va rasm turini aytish (standart: o'chiq,
  tugma faqat nomi bilan o'qiladi), boshqa element turlari, ro'yxat o'rni, foydalanish ko'rsatmalari, tavsif tartibi, oyna sarlavhalari,
  ekran o'chiq bo'lganda gapirish, yozilgan matn e'loni, ovoz balandligi o'zgarganda o'qishni to'xtatmaslik (standart: yoqiq).
- Nutq tezligi va Ovoz toni: aniq son yoziladi yoki 1 va 10 foizlik tugmalar bosiladi (tezlik 20-500, ton 20-300). Har o'zgarish darhol eshitiladi;
  "Bekor qilish" eski qiymatni qaytaradi. Onlayn (Azure) ovozda tezlik 50-200, ton 50-150 oralig'ida ishlaydi.
- Ishora bilan tezlik va ton qadami: 1, 2, 5, 10 (standart) yoki 20 foiz.
- Tezlashtirish: Javob tezligi (Oddiy, Tezkor (standart), Eng tezkor). Nutq tezligidan alohida: kutish vaqtlarini va ortiqcha ishni kamaytiradi.
- Avtomatik tarjima: bitta tugma (bosh ekranda, sozlamalarda yoki O'zbegim menyusida). Telefon tili o'zbekcha bo'lsa, boshqa tildagi matn
  tilni o'zi aniqlab o'zbekchaga tarjima qilinadi. Tarjima yozuvi: o'zbek lotin (hozirgi), kirill yoki yangi alifbo. Onlayn imkoniyatlar va
  sun'iy intellekt kaliti kerak; maxfiy ilovalar, parollar va tasdiqlash kodlari yuborilmaydi.
- Ikki barmoqli surishni ilovaga o'tkazish: faqat qulf ekranida (standart), doim yoki o'chiq.
- Mikrofon ochiq bo'lganda (ovozli xabar yozilayotganda): avtomatik xabarlar o'chadi, bosganlaringiz past ovozda aytiladi (standart).
- Rim raqamlari: "XIX asr" kabi yozuvlar "o'n to'qqizinchi asr" deb o'qiladi (asr, jahon, bob, qism, jild va h.k. so'zlari oldida,
  hamda "Napoleon III" kabi ismdan keyin). Telefon tili o'zbekcha bo'lganda ishlaydi.
- Tarjima: Manba tili (standart: Avtomatik), Maqsad til (standart: O'zbekcha). Onlayn imkoniyatlar yoqilgan va kalit kiritilgan bo'lishi kerak.
- Klaviatura rejimi: Standart (har harf ikki marta bosilganda yoziladi) yoki Teginib yozish (barmoq ko'tarilganda harf yoziladi).

## Jismoniy klaviatura (Alt bilan)
O'ng/chap strelka: keyingi/oldingi element. Pastga/yuqoriga strelka: o'qish boshqaruvi bo'yicha. Enter: faollashtirish.
R: keyingisidan o'qish. T: boshidan o'qish. S: nutqni to'xtatish. H: soat va batareya. B: Orqaga. O: takrorlash.
M: O'zbegim menyusi. A: element amallari. N: bildirishnomalar. Q: tezkor sozlamalar. C: ekran pardasi. Y: tarjima.
F: birinchi element. L: oxirgi element. W: keyingi oyna.

## Android versiyasi
- Ikki, uch va to'rt barmoqli ishoralar: Android 10 va undan yangisi. Android 9 da bir barmoqli ishoralar ishlaydi.
- Ikki barmoq bilan tizim darajasida aylantirish: Android 12 va undan yangisi.

## Aloqa va murojaat
Elektron pochta: husanboyabdusamadov6@gmail.com
Telegram: @Tarixchi1999
Ilovada: asosiy ekrandagi "Aloqa va murojaat" tugmasi yoki Sozlamalarning oxirgi bo'limi.
