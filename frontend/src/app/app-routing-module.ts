import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Login } from './auth/login';
import { Admin } from './admin/admin';
import { UserComponent } from './user/user';
import { AuthGuard } from './auth/auth.guard';

const routes: Routes = [
  { path: 'login', component: Login },
  { 
    path: 'admin', 
    component: Admin, 
    canActivate: [AuthGuard], 
    data: { roles: ['ROLE_ADMIN'] } 
  },
  { 
    path: 'user', 
    component: UserComponent, 
    canActivate: [AuthGuard], 
    data: { roles: ['ROLE_USER'] } 
  },
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: '**', redirectTo: '/login' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
