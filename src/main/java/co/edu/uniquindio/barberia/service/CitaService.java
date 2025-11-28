package co.edu.uniquindio.barberia.service;

import co.edu.uniquindio.barberia.api.dto.CrearCitaDTO;
import co.edu.uniquindio.barberia.domain.*;
import co.edu.uniquindio.barberia.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CitaService {

    private final CitaRepo citaRepo;
    private final ClienteRepo clienteRepo;
    private final BarberoRepo barberoRepo;
    private final HorarioBarberoRepo horarioRepo;
    private final ServicioRepo servicioRepo;

    @Transactional
    public Cita agendar(CrearCitaDTO dto) {
        Cliente cliente = clienteRepo.findById(dto.clienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no existe"));
        Barbero barbero = barberoRepo.findById(dto.barberoId())
                .orElseThrow(() -> new IllegalArgumentException("Barbero no existe"));
        Servicio servicio = servicioRepo.findById(dto.servicioId())
                .orElseThrow(() -> new IllegalArgumentException("Servicio no existe"));

        if (!barbero.isActivo()) {
            throw new IllegalStateException("El barbero está inactivo");
        }
        if (!servicio.isActivo()) {
            throw new IllegalStateException("El servicio no está disponible actualmente");
        }

        // ========= VALIDA HORARIO (todo en 1..7: 1=Lunes ... 7=Domingo) =========
        int diaSemana = dto.fechaHoraInicio().getDayOfWeek().getValue(); // 1..7
        var horarios = horarioRepo.findByBarberoIdAndDiaSemana(barbero.getId(), diaSemana);

        if (horarios.isEmpty()) {
            throw new IllegalArgumentException(
                    "No hay horario configurado para el barbero en ese día (diaSemana=" + diaSemana + ")"
            );
        }

        LocalTime ini = dto.fechaHoraInicio().toLocalTime();
        LocalTime fin = dto.fechaHoraFin().toLocalTime();

        boolean dentroHorario = horarios.stream().anyMatch(h ->
                !ini.isBefore(h.getHoraInicio()) && !fin.isAfter(h.getHoraFin())
        );

        if (!dentroHorario) {
            String bloques = horarios.stream()
                    .map(h -> h.getHoraInicio() + "-" + h.getHoraFin())
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException(
                    "La cita " + ini + "–" + fin + " no cabe en los bloques del día: " + bloques
            );
        }
        // ========= FIN VALIDA HORARIO =========

        // Validar solapamiento de citas
        if (citaRepo.existsByBarberoIdAndFechaHoraInicioLessThanAndFechaHoraFinGreaterThan(
                barbero.getId(), dto.fechaHoraFin(), dto.fechaHoraInicio())) {
            throw new IllegalArgumentException("El barbero tiene otra cita en ese horario");
        }
        if (citaRepo.existsByClienteIdAndFechaHoraInicioLessThanAndFechaHoraFinGreaterThan(
                cliente.getId(), dto.fechaHoraFin(), dto.fechaHoraInicio())) {
            throw new IllegalArgumentException("El cliente tiene otra cita en ese horario");
        }

        Cita cita = Cita.builder()
                .cliente(cliente)
                .barbero(barbero)
                .servicio(servicio)
                .fechaHoraInicio(dto.fechaHoraInicio())
                .fechaHoraFin(dto.fechaHoraFin())
                .estado(Cita.Estado.AGENDADA)
                .build();

        return citaRepo.save(cita);
    }

    @Transactional
    public void cancelar(Long id, String motivo) {
        Cita cita = citaRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cita no existe"));
        if (cita.getEstado() != Cita.Estado.AGENDADA)
            throw new IllegalStateException("Solo se pueden cancelar citas AGENDADAS");
        cita.setEstado(Cita.Estado.CANCELADA);
        citaRepo.save(cita);
    }

    @Transactional(readOnly = true)
    public List<Cita> listar() {
        return citaRepo.findAllWithClienteAndBarbero();
    }

    @Transactional
    public void marcarPagado(Long id, boolean pagadoNuevo) {
        // 1) Traer la cita para validar reglas de negocio
        Cita cita = citaRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cita no existe"));

        // 2) Solo permitir marcar pago cuando la cita YA terminó
        if (cita.getFechaHoraFin().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("La cita aún no ha finalizado");
        }

        // (Opcional) no permitir pagar citas canceladas
        // if (cita.getEstado() == Cita.Estado.CANCELADA) {
        //     throw new IllegalStateException("No se puede marcar pagada una cita cancelada");
        // }

        // 3) Blindaje: si ya está pagada, NO se puede revertir a DEBE
        if (Boolean.TRUE.equals(cita.getPagado()) && !pagadoNuevo) {
            throw new IllegalStateException("La cita ya fue pagada y no se puede revertir");
        }

        // 4) Idempotencia
        if (Boolean.TRUE.equals(cita.getPagado()) && pagadoNuevo) {
            return;
        }

        cita.setPagado(pagadoNuevo);
        citaRepo.save(cita);
    }

}
