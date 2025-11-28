package co.edu.uniquindio.barberia.repo;

import co.edu.uniquindio.barberia.domain.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CitaRepo extends JpaRepository<Cita, Long> {

    @Query("SELECT c FROM Cita c JOIN FETCH c.cliente JOIN FETCH c.barbero")
    List<Cita> findAllWithClienteAndBarbero();

    boolean existsByBarberoIdAndFechaHoraInicioLessThanAndFechaHoraFinGreaterThan(
            Long barberoId,
            LocalDateTime end,
            LocalDateTime start
    );

    boolean existsByClienteIdAndFechaHoraInicioLessThanAndFechaHoraFinGreaterThan(
            Long clienteId,
            LocalDateTime end,
            LocalDateTime start
    );

    List<Cita> findByBarberoIdAndFechaHoraInicioBetweenOrderByFechaHoraInicioAsc(
            Long barberoId,
            LocalDateTime desde,
            LocalDateTime hasta
    );
    @Modifying
    @Query("update Cita c set c.pagado = :pagado where c.id = :id")
    int actualizarPagado(@Param("id") Long id, @Param("pagado") boolean pagado);
}
