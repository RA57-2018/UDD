import { Component, OnInit } from '@angular/core';
import { SearchService } from '../search.service';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { BooleanSearchDTO } from '../DTO/BooleanSearchDTO';

interface SearchResult {
  title: string;
  contentSr: string | null;
  contentEn: string | null;
  serverFilename: string;
  highlight: string;
}

@Component({
  selector: 'app-search-page',
  templateUrl: './search-page.component.html',
  styleUrls: ['./search-page.component.css']
})
export class SearchPageComponent implements OnInit {
  successMessage: string | null = null;
  errorMessage: string | null = null;
  queryForm: FormGroup;
  resultsContainerVisible = false;
  results: any[] = [];
  simpleSearchForm = new FormGroup({
    field: new FormControl(''),
    text: new FormControl('')
  })
  advancedSearchForm = new FormGroup({
    field1: new FormControl(''),
    value1: new FormControl(''),
    operation: new FormControl(''),
    field2: new FormControl(''),
    value2: new FormControl('')
  })

  ngOnInit(): void {
  }

  constructor(private searchService: SearchService, private formBuilder: FormBuilder) {
    this.queryForm = this.formBuilder.group({
      query: [''],
      queryType: ['simple'],
      isPhaseQuery: [false]
    });
  }

  submitFileUploadForm(event: Event, fileInput: HTMLInputElement): void {
    event.preventDefault();

    if (fileInput.files && fileInput.files.length > 0) {
      const formData = new FormData();
      formData.append('file', fileInput.files[0]);

      this.searchService.uploadFile(formData).subscribe({
        next: () => {
          this.successMessage = 'File uploaded and indexed successfully';
          this.errorMessage = null;
        },
        error: () => {
          this.errorMessage = 'An error occurred, please try again...';
          this.successMessage = null;
        }
      });
    } else {
      this.errorMessage = 'Please select a file';
      this.successMessage = null;
    }
  }

  togglePhaseQuery() {
    if (!this.isSimpleSearch()) {
      this.queryForm.patchValue({ isPhaseQuery: false });
    }
  }

  isSimpleSearch() {
    return this.queryForm.controls['queryType'].value === 'simple';
  }

  submitQuery() {
    if (this.queryForm.valid) {
      let query: any = {
        keywords: this.queryForm.value.query.split(' '),
        isPhaseQuery: false
      };

      if (this.queryForm.value.queryType === 'simple') {
        if (this.queryForm.value.isPhaseQuery) {
          query.isPhaseQuery = true;
        }
      }

      const method = this.queryForm.value.queryType === 'advanced' ? 'searchAdvanced' : 'searchSimple';

      this.searchService[method](query).subscribe({
        next: (data: any) => {
          this.displayResults(data);
        },
        error: (error) => {
          console.error('Error executing search:', error);
        }
      });
    } else {
      console.error('Error!');
    }
  }

  displayResults(data: any) {
    if (data.content && data.content.length > 0) {
      this.results = data.content.map((result: SearchResult) => {
        const contentToDisplay = result.contentSr !== null ? result.contentSr : result.contentEn;
        return {
          title: result.title,
          content: contentToDisplay,
          downloadLink: this.searchService.generateDownloadLink(result.serverFilename),
          downloadTitle: result.title.replace(/\s+/g, '-'),
          highlight: result.highlight
        };
      });
      this.resultsContainerVisible = true;
    } else {
      this.results = [];
      this.resultsContainerVisible = false;
      alert('No results found');
    }
  }

  clearResults() {
    this.results = [];
    this.resultsContainerVisible = false;
  }

  submitSimpleSearch() {
    const field = this.simpleSearchForm.value.field!;
    const text = this.simpleSearchForm.value.text!;

    this.searchService.simpleSearch(field, text).subscribe({
      next: (data: any) => {
        console.log(data);
        if (data.content && data.content.length > 0) {
          this.displayResults(data);
        } else {
          alert('No results found');
          this.results = [];
          this.resultsContainerVisible = false;
        }
      },
      error: (error) => {
        console.error('Error executing search:', error);
      }
    });
  }

  submitAdvancedSearch() {
    const field1 = this.advancedSearchForm.value.field1;
    const value1 = this.advancedSearchForm.value.value1;
    const operation = this.advancedSearchForm.value.operation;
    const field2 = this.advancedSearchForm.value.field2;
    const value2 = this.advancedSearchForm.value.value2;

    let searchResultDto = new BooleanSearchDTO({
      field1,
      value1,
      field2,
      value2,
      operation
    })

    this.searchService.booleanSearch(searchResultDto).subscribe({
      next: (data: any) => {
        if (data.content && data.content.length > 0) {
          this.displayResults(data);
        } else {
          alert('No results found');
          this.results = [];
          this.resultsContainerVisible = false;
        }
      },
      error: (error) => {
        console.error('Error executing search:', error);
      }
    });
  }
}
