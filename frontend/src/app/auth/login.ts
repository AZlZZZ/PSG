import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from './auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  templateUrl: './login.html',
  styleUrl: './login.scss',
  standalone: false
})
export class Login {
  loginForm: FormGroup;
  error: string | null = null;
  loading = false;
  showPassword = false;

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });
  }

  onSubmit() {
    if (this.loginForm.invalid) {
      return;
    }

    this.loading = true;
    this.error = null;

    const { username, password } = this.loginForm.value;
    this.authService.login(username, password).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.role === 'ROLE_ADMIN') {
          this.router.navigate(['/admin']);
        } else {
          this.router.navigate(['/user']);
        }
      },
      error: (err) => {
        this.loading = false;
        this.error = err.status === 401 || err.status === 403 
          ? 'Nom d\'utilisateur ou mot de passe incorrect.' 
          : 'Erreur de connexion avec le serveur.';
      }
    });
  }
}
