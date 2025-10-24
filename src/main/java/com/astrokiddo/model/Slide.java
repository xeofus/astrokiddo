
package com.astrokiddo.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Slide {
    private SlideType type;
    private String title;
    private String text;
    private String imageUrl;
    private String attribution;

    public Slide(SlideType type, String title, String text, String imageUrl, String attribution) {
        this.type = type;
        this.title = title;
        this.text = text;
        this.imageUrl = imageUrl;
        this.attribution = attribution;
    }
}
