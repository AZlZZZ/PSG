import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  constructor(private authService: AuthService, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    if (this.authService.isLoggedIn()) {
      const userRole = this.authService.getRole();
      const requiredRoles = route.data['roles'] as Array<string>;
      
      if (!requiredRoles || requiredRoles.includes(userRole || '')) {
        return true;
      }
      
      // Not authorized for this specific role, redirect appropriately
      if (userRole === 'ROLE_ADMIN') {
        this.router.navigate(['/admin']);
      } else {
        this.router.navigate(['/user']);
      }
      return false;
    }

    // Not logged in, redirect to login
    this.router.navigate(['/login']);
    return false;
  }
}
