import {Component} from '@angular/core';
import {FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';
import {DeckEnrichment, DeckService, GenerateReq, LessonDeck} from './deck.service';
import {saveAs} from 'file-saver';
import {firstValueFrom} from "rxjs";

@Component({
    selector: 'app-root',
    imports: [CommonModule, ReactiveFormsModule],
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css']
})
export class AppComponent {

    public form: FormGroup;
    deck?: LessonDeck;
    loading = false;
    error?: string;

    constructor(private deckSvc: DeckService) {
        this.form = new FormGroup({
            topic: new FormControl('spiral galaxies'),
            gradeLevel: new FormControl('8-10'),
            locale: new FormControl('en'),
        });
    }

    // @ts-ignore
    async generate() {
        this.error = undefined;
        this.loading = true;
        try {
            const req: GenerateReq = this.form.getRawValue();
            this.deck = await firstValueFrom(this.deckSvc.generate(req));
        } catch (e: any) {
            this.error = e?.message || 'Failed to generate deck';
        } finally {
            this.loading = false;
        }
    }

    hasEnrichment(enrichment?: DeckEnrichment | null): boolean {
        if (!enrichment) {
            return false;
        }
        const {
            hook,
            simple_explanation,
            why_it_matters,
            class_question,
            fun_fact,
            vocabulary,
            attribution
        } = enrichment;

        const hasText = [hook, simple_explanation, why_it_matters, class_question, fun_fact, attribution]
            .some(value => typeof value === 'string' && value.trim().length > 0);
        const hasVocabulary = Array.isArray(vocabulary) && vocabulary.length > 0;

        return hasText || hasVocabulary;
    }

    exportHtml() {
        if (!this.deck) return;
        this.deckSvc.exportHtml(this.deck.id).subscribe(b => saveAs(b, (this.deck?.topic || 'deck') + '.html'));
    }

    exportPdf() {
        if (!this.deck) return;
        this.deckSvc.exportPdf(this.deck.id).subscribe(b => saveAs(b, (this.deck?.topic || 'deck') + '.pdf'));
    }
}
