package com.akkhara.keyboard.translation

import com.akkhara.keyboard.engine.ModelTask

/** Fixed sentences for judging the translation model (#12). Debug builds only. */
internal val sampleSentences: List<Pair<ModelTask, String>> =
    listOf(
        "မင်္ဂလာပါ။ နေကောင်းလား။",
        "ဒီနေ့ ညနေ ငါးနာရီမှာ တွေ့ကြရအောင်။",
        "ကျေးဇူးအများကြီးတင်ပါတယ်။",
        "ဈေးကို သွားပြီး ဆန်နဲ့ ကြက်ဥ ဝယ်ခဲ့ပါ။",
        "မနက်ဖြန် မိုးရွာမယ်လို့ ထင်တယ်။",
        "ကျွန်တော် ရန်ကုန်မှာ နေပါတယ်။",
        "ဒီစာအုပ်က ဘယ်လောက်လဲ။",
        "ဆရာဝန်နဲ့ ချိန်းထားတာကို နောက်ကျနေပြီ။",
        "အမေ့ကို ဖုန်းဆက်ဖို့ မမေ့ပါနဲ့။",
        "ဒီအစည်းအဝေးကို နောက်အပတ်အထိ ရွှေ့လို့ရမလား။",
    ).map { ModelTask.MyanmarToEnglish to it } +
        listOf(
            "Good morning! Did you sleep well?",
            "I will send you the report by Friday.",
            "Where is the nearest bus stop?",
            "Please call me when you arrive.",
            "The food at this restaurant is delicious.",
            "I'm sorry, I can't come to the party tonight.",
            "My phone battery is almost empty.",
            "Can you help me carry these boxes?",
            "We need to buy a birthday present for Mya Mya.",
            "The train leaves at seven thirty in the morning.",
        ).map { ModelTask.EnglishToMyanmar to it }
