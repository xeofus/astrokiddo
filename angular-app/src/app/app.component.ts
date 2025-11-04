import {Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';
import {ApodResponse, DeckEnrichment, DeckService, GenerateReq, LessonDeck} from './deck.service';
import {firstValueFrom} from "rxjs";

@Component({
    selector: 'app-root',
    imports: [CommonModule, ReactiveFormsModule],
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit, OnDestroy {

  public form: FormGroup;
  public deck?: LessonDeck;
  public loading = false;
  public error?: string;
  public apod?: ApodResponse;
  public apodError?: string;
  public showSlideshow = false;
  public lastRequest?: GenerateReq;

  @ViewChild('revealRoot') private revealRoot?: ElementRef<HTMLDivElement>;
  private revealInstance: any;
  private revealInitTimeout?: number;

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

  ngOnDestroy() {
    this.destroyReveal();
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

  openSlideshow() {
    if (!this.deck) {
      return;
    }
    this.showSlideshow = true;
    if (this.revealInitTimeout) {
      window.clearTimeout(this.revealInitTimeout);
    }
    this.revealInitTimeout = window.setTimeout(() => this.initializeReveal(), 0);
  }

  closeSlideshow() {
    this.showSlideshow = false;
    if (this.revealInitTimeout) {
      window.clearTimeout(this.revealInitTimeout);
      this.revealInitTimeout = undefined;
    }
    this.destroyReveal();
  }

  private initializeReveal() {
    if (!this.deck) {
      return;
    }
    const container = this.revealRoot?.nativeElement;
    const revealGlobal = (window as any).Reveal;
    if (!revealGlobal || !container) {
      console.warn('Reveal.js failed to load.');
      return;
    }

    this.destroyReveal();

    this.revealInstance = new revealGlobal({
      container,
      embedded: true,
      hash: false,
      controls: true,
      progress: true,
      transition: 'slide',
      backgroundTransition: 'fade'
    });

    if (typeof this.revealInstance.initialize === 'function') {
      this.revealInstance.initialize();
    }
    if (typeof this.revealInstance.sync === 'function') {
      this.revealInstance.sync();
    }
    if (typeof this.revealInstance.layout === 'function') {
      this.revealInstance.layout();
    }
    if (typeof this.revealInstance.slide === 'function') {
      this.revealInstance.slide(0);
    }
  }

  private destroyReveal() {
    if (this.revealInstance && typeof this.revealInstance.destroy === 'function') {
      this.revealInstance.destroy();
    }
    this.revealInstance = undefined;
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
}
