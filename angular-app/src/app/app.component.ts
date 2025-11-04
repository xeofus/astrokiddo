import {Component, OnInit} from '@angular/core';
import {FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';
import {ApodResponse, DeckEnrichment, DeckService, GenerateReq, LessonDeck} from './deck.service';
import {saveAs} from 'file-saver-es';
import {firstValueFrom} from "rxjs";

@Component({
    selector: 'app-root',
    imports: [CommonModule, ReactiveFormsModule],
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {

  public form: FormGroup;
  public deck?: LessonDeck;
  public loading = false;
  public error?: string;
  public apod?: ApodResponse;
  public apodError?: string;


  constructor(private deckSvc: DeckService) {
      this.form = new FormGroup({
          topic: new FormControl('spiral galaxies'),
          gradeLevel: new FormControl('8-10'),
          locale: new FormControl('en'),
      });
  }

  async ngOnInit() {
    try {
      this.apod = await firstValueFrom(this.deckSvc.apod());
    } catch (e: any) {
      this.apodError = e?.message || 'Failed to load Astronomy Picture of the Day';
    }
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
