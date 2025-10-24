
package com.astrokiddo.service;

import com.astrokiddo.model.LessonDeck;
import com.astrokiddo.model.Slide;
import org.springframework.stereotype.Service;

@Service
public class ExportService {
    public String toHtml(LessonDeck deck) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html lang='en'><head><meta charset='utf-8'>");
        sb.append("<meta name='viewport' content='width=device-width,initial-scale=1'>");
        sb.append("<title>").append(escape(deck.getTopic())).append(" — AstroKiddo</title>");
        sb.append("<style>");
        sb.append("body{font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;margin:24px;background:#0b0d12;color:#e6e6e6;}");
        sb.append(".deck{max-width:1000px;margin:0 auto;}");
        sb.append(".title{font-size:28px;font-weight:700;margin-bottom:4px;}");
        sb.append(".subtitle{opacity:.8;margin-bottom:24px;}");
        sb.append(".grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:16px;}");
        sb.append(".card{background:#151a22;border:1px solid #222a35;border-radius:16px;padding:16px;box-shadow:0 6px 20px rgba(0,0,0,.35);}");
        sb.append(".type{font-size:12px;letter-spacing:.12em;text-transform:uppercase;opacity:.7;margin-bottom:8px;}");
        sb.append(".card h3{margin:6px 0 8px 0;font-size:18px;}");
        sb.append(".card img{max-width:100%;border-radius:12px;display:block;margin:8px 0;}");
        sb.append(".attr{font-size:12px;opacity:.7;margin-top:8px;}");
        sb.append("</style></head><body><div class='deck'>");
        sb.append("<div class='title'>").append(escape(deck.getTopic())).append("</div>");
        sb.append("<div class='subtitle'>Generated ").append(deck.getCreatedAt()).append("</div>");
        sb.append("<div class='grid'>");
        for (Slide s : deck.getSlides()) {
            sb.append("<div class='card'>");
            sb.append("<div class='type'>").append(escape(s.getType().name())).append("</div>");
            if (s.getTitle() != null) sb.append("<h3>").append(escape(s.getTitle())).append("</h3>");
            if (s.getImageUrl() != null) sb.append("<img src='").append(escape(s.getImageUrl())).append("' alt=''>");
            if (s.getText() != null) sb.append("<p>").append(escape(s.getText())).append("</p>");
            if (s.getAttribution() != null)
                sb.append("<div class='attr'>© ").append(escape(s.getAttribution())).append("</div>");
            sb.append("</div>");
        }
        sb.append("</div></div></body></html>");
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
