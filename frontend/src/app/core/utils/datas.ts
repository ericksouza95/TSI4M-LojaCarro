// Data local no formato aceito pela API (AAAA-MM-DD), sem conversão de fuso
export function paraDataIso(data: Date): string {
  const ano = data.getFullYear();
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  const dia = String(data.getDate()).padStart(2, '0');
  return `${ano}-${mes}-${dia}`;
}

// "2030-03-05" -> Date local (new Date("2030-03-05") seria interpretado em UTC e poderia virar o dia anterior)
export function deDataIso(texto: string): Date {
  const [ano, mes, dia] = texto.split('-').map(Number);
  return new Date(ano, mes - 1, dia);
}

export function hojeSemHorario(): Date {
  const agora = new Date();
  return new Date(agora.getFullYear(), agora.getMonth(), agora.getDate());
}

// Próximo dia em que a loja abre (domingo fechado)
export function proximoDiaUtil(apartirDe: Date = hojeSemHorario()): Date {
  const data = new Date(apartirDe);
  if (data.getDay() === 0) {
    data.setDate(data.getDate() + 1);
  }
  return data;
}
