import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';

export interface Slide {
    type: string;
    title?: string;
    text?: string;
    imageUrl?: string;
    attribution?: string;
}

export interface VocabularyItem {
    term: string;
    definition: string;
}

export interface EnrichmentMeta {
    model?: string;
}

export interface DeckEnrichment {
    hook?: string;
    simple_explanation?: string;
    why_it_matters?: string;
    class_question?: string;
    vocabulary?: VocabularyItem[];
    fun_fact?: string;
    attribution?: string;
    meta?: EnrichmentMeta;
}

export interface LessonDeck {
    id: string;
    topic: string;
    createdAt: string;
    slides: Slide[];
    enrichment?: DeckEnrichment;
}

export interface GenerateReq {
    topic: string;
    gradeLevel?: string;
    locale?: string;
}

export interface ApodResponse {
  date?: string;
  title?: string;
  explanation?: string;
  media_type?: string;
  url?: string;
  hdurl?: string;
  thumbnail_url?: string;
  copyright?: string;
  service_version?: string;
}

@Injectable({providedIn: 'root'})
export class DeckService {
  constructor(private http: HttpClient) {
  }

  generate(req: GenerateReq): Observable<LessonDeck> {
      return this.http.post<LessonDeck>('/api/decks/generate', req);
  }

  exportHtml(id: string): Observable<Blob> {
      return this.http.get(`/api/decks/${id}/export/html`, {responseType: 'blob'});
  }

  exportPdf(id: string): Observable<Blob> {
      return this.http.get(`/api/decks/${id}/export/pdf`, {responseType: 'blob'});
  }

  apod(date?: string): Observable<ApodResponse> {
    let params = new HttpParams();
    if (date) {
      params = params.set('date', date);
    }
    return this.http.get<ApodResponse>('/api/apod', {params});
  }
}
