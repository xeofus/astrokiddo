import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';

export interface Slide {
    type: string;
    title?: string;
    text?: string;
    imageUrl?: string;
    attribution?: string;
}

export interface LessonDeck {
    id: string;
    topic: string;
    createdAt: string;
    slides: Slide[];
}

export interface GenerateReq {
    topic: string;
    gradeLevel?: string;
    locale?: string;
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
}
