package co.edu.uniquindio.barberia.api.controller;

import co.edu.uniquindio.barberia.api.dto.CrearCitaDTO;
import co.edu.uniquindio.barberia.domain.Cita;
import co.edu.uniquindio.barberia.service.CitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService service;

    /** POST /api/citas */
    @PostMapping
    public ResponseEntity<Cita> crear(@Valid @RequestBody CrearCitaDTO dto) {
        return ResponseEntity.ok(service.agendar(dto));
    }

    /** GET /api/citas */
    @GetMapping
    public ResponseEntity<List<Cita>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    /** DELETE /api/citas/{id}?motivo=... */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(
            @PathVariable Long id,
            @RequestParam(defaultValue = "sin_motivo") String motivo) {
        service.cancelar(id, motivo);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ping")
    public String ping() {
        return "citas-ok";
    }

    /** PUT /api/citas/{id}/pagado?pagado=true|false */
    @PutMapping("/{id}/pagado")
    public ResponseEntity<?> setPagado(
            @PathVariable Long id,
            @RequestParam boolean pagado) {
        service.marcarPagado(id, pagado); // ← usar `service`
        return ResponseEntity.ok(Map.of("message", "Pago actualizado", "id", id, "pagado", pagado));
    }
}
