package br.org.edu.ifrn.LojaCarro.config;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HorarioFuncionamentoTest {

    private static final LocalDate SEGUNDA = LocalDate.of(2030, 3, 4);
    private static final LocalDate SABADO = LocalDate.of(2030, 3, 9);
    private static final LocalDate DOMINGO = LocalDate.of(2030, 3, 10);

    private final HorarioFuncionamento horario = new HorarioFuncionamento("08:00", "18:00", "12:00");

    private boolean permite(LocalDate dia, int hora, int minuto) {
        LocalDateTime inicio = LocalDateTime.of(dia, LocalTime.of(hora, minuto));
        return horario.permite(inicio, inicio.plusMinutes(30));
    }

    @Test
    void gradeDoDiaTemHorariosDe30Minutos() {
        assertThat(horario.horariosDoDia(SEGUNDA)).hasSize(20)
                .startsWith(LocalTime.of(8, 0), LocalTime.of(8, 30))
                .endsWith(LocalTime.of(17, 30));
        assertThat(horario.horariosDoDia(SABADO)).hasSize(8).endsWith(LocalTime.of(11, 30));
        assertThat(horario.horariosDoDia(DOMINGO)).isEmpty();
    }

    @Test
    void testDrivePrecisaCaberInteiroNoExpediente() {
        assertThat(permite(SEGUNDA, 8, 0)).isTrue();
        assertThat(permite(SEGUNDA, 17, 30)).isTrue();
        assertThat(permite(SEGUNDA, 7, 59)).isFalse();
        assertThat(permite(SEGUNDA, 17, 31)).isFalse();
        assertThat(permite(SABADO, 11, 30)).isTrue();
        assertThat(permite(SABADO, 11, 45)).isFalse();
        assertThat(permite(DOMINGO, 10, 0)).isFalse();
    }

    @Test
    void sabadoPodeSerConfiguradoComoFechado() {
        HorarioFuncionamento semSabado = new HorarioFuncionamento("09:00", "17:00", "");

        assertThat(semSabado.horariosDoDia(SABADO)).isEmpty();
        assertThat(semSabado.horariosDoDia(SEGUNDA)).hasSize(16);
    }

    @Test
    void configuracaoInvalidaImpedeAInicializacao() {
        assertThatThrownBy(() -> new HorarioFuncionamento("18:00", "08:00", "12:00"))
                .isInstanceOf(IllegalStateException.class);
    }
}
