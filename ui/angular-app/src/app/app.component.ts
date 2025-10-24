import {Component} from '@angular/core';
import {FormBuilder, FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';
import {HttpClient} from '@angular/common/http';
import {DeckService, GenerateReq, LessonDeck} from './deck.service';
import {saveAs} from 'file-saver';

@Component({
    selector: 'app-root',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule],
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css']
})
export class AppComponent {

    public form: FormGroup;
    deck?: LessonDeck;
    loading = false;
    error?: string;

    constructor(private fb: FormBuilder, private deckSvc: DeckService) {
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
            this.deck = await this.deckSvc.generate(req).toPromise();
        } catch (e: any) {
            this.error = e?.message || 'Failed to generate deck';
        } finally {
            this.loading = false;
        }
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
