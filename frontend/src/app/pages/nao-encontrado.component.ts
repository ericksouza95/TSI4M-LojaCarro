import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';

import { EstadoVazioComponent } from '../shared/components/estado-vazio/estado-vazio.component';

@Component({
  selector: 'app-nao-encontrado',
  imports: [RouterLink, MatButtonModule, EstadoVazioComponent],
  template: `
    <div class="container pagina">
      <app-estado-vazio icone="wrong_location" titulo="Página não encontrada"
                        mensagem="O endereço acessado não existe ou foi removido.">
        <a mat-flat-button routerLink="/">Ir para a página inicial</a>
      </app-estado-vazio>
    </div>
  `,
})
export class NaoEncontradoComponent {}
