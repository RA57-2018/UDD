import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SearchPageComponent } from './search-page/search-page.component';
import { LoginPageComponent } from './login-page/login-page.component';
import { VisualizationPageComponent } from './visualization-page/visualization-page.component';

const routes: Routes = [
  { path: '', redirectTo: 'search-page', pathMatch: 'full' },
  { path: 'search-page', component: SearchPageComponent },
  { path: 'login', component: LoginPageComponent },
  { path: 'visualizations', component: VisualizationPageComponent },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
