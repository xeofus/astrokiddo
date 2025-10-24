
package com.astrokiddo.controller;

import com.astrokiddo.model.LessonDeck;
import com.astrokiddo.service.ExportService;
import com.astrokiddo.store.DeckStore;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/decks")
public class ExportController {

    private final DeckStore store;
    private final ExportService exportService;

    public ExportController(DeckStore store, ExportService exportService) {
        this.store = store;
        this.exportService = exportService;
    }

    @GetMapping("/{id}/export/html")
    public ResponseEntity<byte[]> exportHtml(@PathVariable String id) {
        LessonDeck deck = store.get(id).orElseThrow(() -> new NoSuchElementException("Deck not found: " + id));
        String html = exportService.toHtml(deck);
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        String filename = deck.getTopic().replaceAll("[^a-zA-Z0-9._-]", "_") + ".html";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename + " ")
                .contentType(MediaType.TEXT_HTML)
                .body(bytes);
    }

    @GetMapping("/{id}/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable String id) throws Exception {
        LessonDeck deck = store.get(id).orElseThrow(() -> new NoSuchElementException("Deck not found: " + id));
        String html = exportService.toHtml(deck);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(html, null);
        builder.toStream(baos);
        builder.run();

        byte[] pdf = baos.toByteArray();
        String filename = deck.getTopic().replaceAll("[^a-zA-Z0-9._-]", "_") + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename + " ")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
