package uz.ekranoquvchi.mvp.content

/**
 * Til ko'prigi lug'ati (monografiya 10.3-10.4): inglizcha so'z -> o'zbek imlosida talaffuz.
 * DIQQAT: yozilishlar taxminiy. Lingvist va foydalanuvchi sinovida tasdiqlanishi kerak.
 * Foydalanuvchi lug'ati (setUserEntries) shu ro'yxatdan ustun turadi.
 */
object BridgeDictionary {
    private val builtIn: Map<String, String> = parse(
        """
        settings=setings
        whatsapp=vatsap
        wi-fi=vay-fay
        wifi=vay-fay
        bluetooth=blyutus
        ok=okey
        cancel=kensel
        save=seyv
        delete=dilit
        search=sorch
        home=houm
        back=bek
        menu=menyu
        share=sher
        logout=logaut
        password=pasvord
        download=daunlod
        downloads=daunlodz
        upload=aplod
        update=apdeyt
        updates=apdeyts
        install=instol
        camera=kamera
        gallery=galereya
        message=messej
        messages=messejes
        notification=notifikeyshn
        notifications=notifikeyshns
        battery=betari
        network=netvork
        mobile=mobayl
        data=deyta
        airplane=eyrplen
        mode=moud
        volume=volyum
        sound=saund
        display=displey
        brightness=braytnes
        language=lengvij
        keyboard=kiybord
        accessibility=aksesibiliti
        screen=skrin
        reader=rider
        talkback=tokbek
        google=gugl
        chrome=kroum
        youtube=yutub
        facebook=feysbuk
        messenger=messenjer
        gmail=jimeyl
        play=pley
        store=stor
        maps=meps
        photos=foutos
        files=faylz
        contacts=kontakts
        phone=foun
        call=kol
        calls=kolz
        clock=klok
        email=imeyl
        mail=meyl
        music=myuzik
        app=ep
        apps=eps
        application=eplikeyshn
        free=fri
        open=oupn
        close=klouz
        next=nekst
        previous=privyus
        done=dan
        loading=louding
        offline=oflayn
        online=onlayn
        like=layk
        follow=folou
        profile=profayl
        account=akkaunt
        user=yuzer
        name=neym
        reply=riplay
        forward=forvard
        copy=kopi
        paste=peyst
        select=silekt
        all=ol
        about=abaut
        location=lokeyshn
        flashlight=flashlayt
        timer=taymer
        stopwatch=stopuoch
        weather=veder
        news=nyus
        zoom=zum
        skype=skayp
        viber=vayber
        payme=peyme
        click=klik
        xiaomi=shaomi
        huawei=xuavey
        pixel=piksel
        iphone=ayfon
        hotspot=xotspot
        sync=sink
        backup=bekap
        storage=storij
        security=sekyuriti
        privacy=praysi
        permission=permishn
        permissions=permishns
        general=jenaral
        wallpaper=volpeyper
        # 0.4.0: qo'shimcha umumiy so'zlar (taxminiy talaffuz; lingvist tasdig'i kerak)
        light=layt
        night=nayt
        theme=tim
        text=tekst
        type=tayp
        time=taym
        date=deyt
        week=vik
        file=fayl
        voice=voys
        record=rekord
        recorder=rekorder
        player=pleyer
        browser=brauzer
        web=veb
        website=vebsayt
        window=vindou
        windows=vindouz
        image=imij
        images=imijiz
        picture=pikcher
        pictures=pikchers
        pause=poz
        resume=rizyum
        restart=ristart
        reset=riset
        refresh=rifresh
        reload=riloud
        retry=ritray
        continue=kontinyu
        later=leyter
        now=nau
        today=tudey
        tomorrow=tumorrou
        yesterday=yesterdey
        welcome=velkam
        hello=helou
        thanks=tenks
        please=pliz
        sorry=sori
        error=erer
        warning=voning
        feedback=fidbek
        report=ripot
        receive=risiv
        inbox=inboks
        archive=arkayv
        unread=anred
        new=nyu
        create=kriyeyt
        exit=egzit
        quit=kvit
        sign=sayn
        remove=rimuv
        view=vyu
        preview=privyu
        review=rivyu
        history=xistori
        favorites=feyvorits
        favorite=feyvorit
        bookmark=bukmark
        bookmarks=bukmarks
        recent=risent
        results=rizalts
        filter=filtr
        cart=kart
        pay=pey
        payment=peyment
        wallet=valet
        balance=balans
        card=kard
        restore=ristor
        memory=memori
        cache=kesh
        clear=klir
        allow=alau
        deny=dinay
        block=blok
        unblock=anblok
        mute=myut
        unmute=anmyut
        speaker=spiker
        headphones=xedfonz
        microphone=maykrofon
        map=mep
        navigation=naviygeyshn
        traffic=trafik
        shopping=shoping
        channel=chanal
        channels=chanals
        story=stori
        stories=storiz
        live=layv
        stream=strim
        subscribe=sabskrayb
        comment=koment
        comments=koments
        post=poust
        posts=pousts
        scanner=skaner
        barcode=barkod
        version=versiya
        developer=diveloper
        trial=trayl
        """.trimIndent()
    )

    @Volatile
    private var user: Map<String, String> = emptyMap()

    fun setUserEntries(text: String) {
        user = parse(text)
    }

    /** Tartib: ilova bo'yicha ("paket|so'z"), umumiy foydalanuvchi lug'ati, o'rnatilgan lug'at. */
    fun lookup(wordLower: String, pkg: String? = null): String? =
        (if (pkg != null) user["${pkg.lowercase()}|$wordLower"] else null) ?: user[wordLower] ?: builtIn[wordLower]

    fun size(): Int = builtIn.size

    /** Format: har qatorda "so'z=talaffuz" yoki "paket|so'z=talaffuz" (ilova bo'yicha); # bilan boshlangan qatorlar izoh. */
    fun parse(text: String): Map<String, String> {
        val out = HashMap<String, String>()
        for (line in text.lineSequence()) {
            val l = line.trim()
            if (l.isEmpty() || l.startsWith("#")) continue
            val i = l.indexOf('=')
            if (i <= 0 || i == l.length - 1) continue
            out[l.substring(0, i).trim().lowercase()] = l.substring(i + 1).trim()
        }
        return out
    }
}
