import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { SearchService } from '../search.service';
import { LoginDTO } from 'src/app/DTO/LoginDTO';

@Component({
  selector: 'app-login-page',
  templateUrl: './login-page.component.html',
  styleUrls: ['./login-page.component.css']
})
export class LoginPageComponent implements OnInit {
  loginForm: FormGroup;
  passwordVisible: boolean = false;
  showTwoFactorForm = false;
  enteredUsername: string = '';
  enteredPassword: string = '';

  constructor(private searchService: SearchService, private router: Router, private formBuilder: FormBuilder) {
    this.loginForm = this.formBuilder.group({
      username: ['', [Validators.required, Validators.pattern(/^\S+@\S+\.\S+$/), Validators.minLength(2)]],
      password: ['', [Validators.required, Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/), Validators.minLength(8)]],
    });
  }

  togglePasswordVisibility() {
    this.passwordVisible = !this.passwordVisible;
  }

  ngOnInit(): void {
  }

  login() {
    if (this.loginForm.valid) {
      this.enteredUsername = this.loginForm.value.username;
      this.enteredPassword = this.loginForm.value.password;

      let loginDTO: LoginDTO = {
        username: this.loginForm.value.username,
        password: this.loginForm.value.password
      };

      this.searchService.login(loginDTO).subscribe({
        next: () => {
          localStorage.setItem('username', this.enteredUsername);
          localStorage.setItem('password', this.enteredPassword);
          this.router.navigateByUrl('/')
        },
        error: (error: any) => {
          if (error.error) {
            alert(error.error.message);
          } else {
            alert("User with this email not found!");
          }
        }
      });
    }
  }

}
