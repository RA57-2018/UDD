import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from 'src/environments/environment';
import { LoginDTO } from 'src/app/DTO/LoginDTO';
import { BooleanSearchDTO } from './DTO/BooleanSearchDTO';

@Injectable({
  providedIn: 'root'
})
export class SearchService {

  private baseUrl = environment.baseUrl;
  private indexUrl = "api/index";
  private searchUrl = "api/search";
  private fileUrl = "api/file";
  private loginUrl = "api/file";

  constructor(private http: HttpClient) { }

  uploadFile(formData: FormData) {
    return this.http.post(`${this.baseUrl}${this.indexUrl}`, formData)
  }

  searchSimple(query: any) {
    return this.http.post(`${this.baseUrl}${this.searchUrl}/simple`, query);
  }

  searchAdvanced(query: any) {
    return this.http.post(`${this.baseUrl}${this.searchUrl}/advanced`, query);
  }

  generateDownloadLink(serverFilename: string): string {
    return `${this.baseUrl}${this.fileUrl}/${serverFilename}`;
  }

  login(loginDTO: LoginDTO) {
    return this.http.post(`${this.baseUrl}${this.loginUrl}`, loginDTO)
  }

  simpleSearch(field: string, text: string) {
    return this.http.get(`${this.baseUrl}${this.searchUrl}/simpleSearch?field=${field}&text=${text}`)
  }

  booleanSearch(booleanSearchDto: BooleanSearchDTO) {
    return this.http.post(`${this.baseUrl}${this.searchUrl}/boolean`, booleanSearchDto)
  }
}
