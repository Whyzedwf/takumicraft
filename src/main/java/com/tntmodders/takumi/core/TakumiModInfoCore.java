package com.tntmodders.takumi.core;

import com.tntmodders.takumi.TakumiCraftCore;
import net.minecraftforge.fml.common.ModMetadata;

public class TakumiModInfoCore {
    
    public static void load(ModMetadata meta) {
        meta.modId = TakumiCraftCore.MODID;
        meta.name = "匠Craft[Takumi Craft]";
        meta.description = "改築にご注意ください。口凸口";
        meta.version = TakumiCraftCore.VERSION;
        meta.credits = "「ゆかり匠」のテクスチャは野良牛/バファムート氏に製作して頂きました。";
        meta.logoFile = "assets/takumicraft/TClogo.png";
        meta.url = "http://www.tntmodders.com";
        meta.updateUrl = "https://dl.dropboxusercontent.com/s/ak4j4n9xug7z17s/TakumiCraftUpdate.json";
        meta.authorList.add("Tom kate");
        meta.autogenerated = false;
    }
}

