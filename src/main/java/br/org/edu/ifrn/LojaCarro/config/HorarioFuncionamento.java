package br.org.edu.ifrn.LojaCarro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Horário de funcionamento da loja para test drives.
// Segunda a sexta: abertura até fechamento. Sábado: abertura até fechamento-sabado (vazio = fechado). Domingo: fechado.
@Component
public class HorarioFuncionamento {

    public static final int DURACAO_TEST_DRIVE_MINUTOS = 30;

    private final LocalTime abertura;
    private final LocalTime fechamento;
    private final LocalTime fechamentoSabado;

    public HorarioFuncionamento(@Value("${loja.agenda.abertura:08:00}") String abertura,
                                @Value("${loja.agenda.fechamento:18:00}") String fechamento,
                                @Value("${loja.agenda.fechamento-sabado:12:00}") String fechamentoSabado) {
        this.abertura = LocalTime.parse(abertura);
        this.fechamento = LocalTime.parse(fechamento);
        this.fechamentoSabado = fechamentoSabado == null || fechamentoSabado.isBlank() ? null : LocalTime.parse(fechamentoSabado);
        if (!this.abertura.isBefore(this.fechamento)) {
            throw new IllegalStateException("O horário de abertura deve ser anterior ao de fechamento.");
        }
    }

    // Horário de fechamento no dia, ou vazio se a loja não abre
    public Optional<LocalTime> fechamentoEm(LocalDate data) {
        DayOfWeek dia = data.getDayOfWeek();
        if (dia == DayOfWeek.SUNDAY) {
            return Optional.empty();
        }
        if (dia == DayOfWeek.SATURDAY) {
            return Optional.ofNullable(fechamentoSabado);
        }
        return Optional.of(fechamento);
    }

    // O test drive inteiro (início e fim) precisa caber no expediente do dia
    public boolean permite(LocalDateTime inicio, LocalDateTime fim) {
        Optional<LocalTime> fecha = fechamentoEm(inicio.toLocalDate());
        return fecha.isPresent()
                && inicio.toLocalDate().equals(fim.toLocalDate())
                && !inicio.toLocalTime().isBefore(abertura)
                && !fim.toLocalTime().isAfter(fecha.get());
    }

    // Grade de horários de 30 em 30 minutos do dia (vazia quando a loja está fechada)
    public List<LocalTime> horariosDoDia(LocalDate data) {
        List<LocalTime> horarios = new ArrayList<>();
        fechamentoEm(data).ifPresent(fecha -> {
            LocalTime inicio = abertura;
            LocalTime fim = inicio.plusMinutes(DURACAO_TEST_DRIVE_MINUTOS);
            // fim.isAfter(inicio) evita laço infinito se o horário passar da meia-noite
            while (fim.isAfter(inicio) && !fim.isAfter(fecha)) {
                horarios.add(inicio);
                inicio = fim;
                fim = inicio.plusMinutes(DURACAO_TEST_DRIVE_MINUTOS);
            }
        });
        return horarios;
    }

    public String descricao() {
        String sabado = fechamentoSabado == null ? "sábado fechado" : "sábado das " + abertura + " às " + fechamentoSabado;
        return "segunda a sexta das " + abertura + " às " + fechamento + ", " + sabado + ", domingo fechado";
    }
}
