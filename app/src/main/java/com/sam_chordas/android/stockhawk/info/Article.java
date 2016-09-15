package com.sam_chordas.android.stockhawk.info;

import java.io.Serializable;

/**
 * Created by Owen LaRosa on 9/8/16.
 */

public class Article implements Serializable {

    public String title;
    public String link;

    public Article(String title, String link) {
        this.title = title;
        this.link = link;
    }

}
