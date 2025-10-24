
package com.astrokiddo.templates;

import com.astrokiddo.dto.ApodResponseDto;
import com.astrokiddo.dto.ImageSearchResponseDto;
import com.astrokiddo.model.Slide;
import com.astrokiddo.model.SlideType;

import java.util.List;

public class ContentTemplateEngine {

    public Slide keyVisualFromImageItem(ImageSearchResponseDto.Item item) {
        String title = firstTitle(item);
        String imageUrl = bestImageHref(item);
        String attribution = firstCenter(item);
        String text = shorten(firstDescription(item, "NASA imagery for the topic."), 420);
        return new Slide(SlideType.KEY_VISUAL, title, text, imageUrl, attribution);
    }

    public Slide explanation(String topic, ApodResponseDto apod, ImageSearchResponseDto.Item contextItem) {
        String title = "What is " + topic + "?";

        String apodText = (apod != null ? apod.getExplanation() : null);
        String text = isBlank(apodText)
                ? shorten(firstDescription(contextItem, "A concise overview focusing on key physical concepts and observational evidence."), 600)
                : shorten(apodText, 600);

        String apodUrl = (apod != null ? apod.getUrl() : null);
        String img = !isBlank(apodUrl) ? apodUrl : bestImageHref(contextItem);

        return new Slide(SlideType.EXPLANATION, title, text, img, "APOD / NASA");
    }

    public Slide whyItMatters(String topic) {
        String title = "Why it matters";
        String text = "How " + topic + " connects to missions, instruments, and daily tech (navigation, communications, climate and space weather).";
        return new Slide(SlideType.WHY_IT_MATTERS, title, text, null, "NASA");
    }

    public Slide questionForClass(String topic, String gradeLevel) {
        String title = "Question for class";
        String text = "If you could observe " + topic + " for one night, what instrument would you pick and why?";
        if (!isBlank(gradeLevel) && gradeLevel.matches("\\d+(-\\d+)?")) {
            text += " (Align difficulty for grade " + gradeLevel + ")";
        }
        return new Slide(SlideType.QUESTION, title, text, null, null);
    }

    public Slide furtherReading(String topic, ImageSearchResponseDto.Item item) {
        String title = "Further reading";
        String page = detailsPage(item);
        String img = bestImageHref(item);
        String text = "Explore more NASA assets on " + topic + ". Start with the image collection: "
                + (page != null ? page : "images.nasa.gov search.");
        return new Slide(SlideType.FURTHER_READING, title, text, img, "NASA Image & Video Library");
    }

    private ImageSearchResponseDto.Data firstData(ImageSearchResponseDto.Item item) {
        if (item == null) return null;
        List<ImageSearchResponseDto.Data> data = item.getData();
        if (data == null || data.isEmpty()) return null;
        for (ImageSearchResponseDto.Data d : data) {
            if (d != null) return d;
        }
        return null;
    }

    private String firstTitle(ImageSearchResponseDto.Item item) {
        ImageSearchResponseDto.Data d = firstData(item);
        String v = (d != null ? d.getTitle() : null);
        return isBlank(v) ? "Key Visual" : v;
    }

    private String firstDescription(ImageSearchResponseDto.Item item, String def) {
        ImageSearchResponseDto.Data d = firstData(item);
        String v = (d != null ? d.getDescription() : null);
        return isBlank(v) ? def : v;
    }

    private String firstCenter(ImageSearchResponseDto.Item item) {
        ImageSearchResponseDto.Data d = firstData(item);
        String v = (d != null ? d.getCenter() : null);
        return isBlank(v) ? "NASA" : v;
    }

    private String bestImageHref(ImageSearchResponseDto.Item item) {
        if (item == null || item.getLinks() == null) return null;

        for (ImageSearchResponseDto.Link l : item.getLinks()) {
            if (l != null && equalsIgnoreCase(l.getRender()) && !isBlank(l.getHref())) {
                return l.getHref();
            }
        }
        for (ImageSearchResponseDto.Link l : item.getLinks()) {
            if (l != null && !isBlank(l.getHref())) {
                String href = l.getHref().toLowerCase();
                if (href.endsWith(".jpg") || href.endsWith(".jpeg") || href.endsWith(".png")) return l.getHref();
            }
        }
        for (ImageSearchResponseDto.Link l : item.getLinks()) {
            if (l != null && !isBlank(l.getHref())) return l.getHref();
        }
        return null;
    }

    private String detailsPage(ImageSearchResponseDto.Item item) {
        ImageSearchResponseDto.Data d = firstData(item);
        if (d != null && !isBlank(d.getNasaId())) {
            return "https://images.nasa.gov/details-" + d.getNasaId();
        }
        return !isBlank(item != null ? item.getHref() : null) ? item.getHref() : null;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private boolean equalsIgnoreCase(String a) {
        return a != null && a.equalsIgnoreCase("image");
    }

    private String shorten(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 3)) + "...";
    }
}
