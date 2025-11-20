package com.saavedrafotografo.backend.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saavedrafotografo.backend.entity.Appointment;
import com.saavedrafotografo.backend.repository.AppointmentRepository;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "http://localhost:4200")
public class BookingController {

    private final AppointmentRepository appointmentRepository;

    public BookingController(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    // Create booking: frontend posts appointmentDate (ISO local)
    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody Map<String, Object> body) {
        String sessionId = asString(body.get("sessionId"));
        String dateStr = firstNonBlank(asString(body.get("appointmentDate")), asString(body.get("appointmentDateTime")));
        String itemsJson = asString(body.get("itemsJson"));
        if (isBlank(sessionId) || isBlank(dateStr)) {
            return ResponseEntity.badRequest().body(Map.of("error", "sessionId y appointmentDate son requeridos"));
        }
        if (appointmentRepository.existsBySessionId(sessionId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "La sesión ya tiene una reserva"));
        }
        LocalDateTime dateTime;
        try { dateTime = parseFlexibleDateTime(dateStr); }
        catch (DateTimeParseException ex) { return ResponseEntity.badRequest().body(Map.of("error", "Formato de fecha/hora inválido")); }

        // conflict check exact datetime
        List<Appointment> conflicts = appointmentRepository.findAllByAppointmentDateBetween(dateTime, dateTime);
        if (!conflicts.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Fecha y hora no disponibles"));
        }
        Appointment appt = new Appointment(sessionId, dateTime, itemsJson);
        appointmentRepository.save(appt);
        return ResponseEntity.ok(Map.of(
                "id", appt.getId(),
                "sessionId", appt.getSessionId(),
                "appointmentDate", appt.getAppointmentDate()
        ));
    }

    // Range by query: /api/bookings?from=ISO&to=ISO
    @GetMapping("")
    public ResponseEntity<?> getRangeAlt(@RequestParam(name = "from") String from,
                                         @RequestParam(name = "to") String to) {
        try {
            LocalDateTime sdt = parseFlexibleDateTime(from);
            LocalDateTime edt = parseFlexibleDateTime(to);
            List<Appointment> appts = appointmentRepository.findAllByAppointmentDateBetween(sdt, edt);
            return ResponseEntity.ok(appts.stream().map(a -> Map.of(
                "id", a.getId(),
                "sessionId", a.getSessionId(),
                "appointmentDate", a.getAppointmentDate()
            )).toList());
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Formato de fecha/hora inválido"));
        }
    }

    // Alternative range endpoint with dateStart/dateEnd or start/end
    @GetMapping("/range")
    public ResponseEntity<?> getRange(
        @RequestParam(name = "dateStart", required = false) String dateStart,
        @RequestParam(name = "dateEnd", required = false) String dateEnd,
        @RequestParam(name = "start", required = false) String start,
        @RequestParam(name = "end", required = false) String end
    ) {
        try {
            String startStr = firstNonBlank(dateStart, start);
            String endStr = firstNonBlank(dateEnd, end);
            if (isBlank(startStr) || isBlank(endStr)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Parámetros requeridos: dateStart/dateEnd o start/end"));
            }
            // Trim to date if time included
            startStr = startStr.length() >= 10 ? startStr.substring(0, 10) : startStr;
            endStr = endStr.length() >= 10 ? endStr.substring(0, 10) : endStr;
            LocalDate startDate = LocalDate.parse(startStr);
            LocalDate endDate = LocalDate.parse(endStr);
            LocalDateTime sdt = startDate.atStartOfDay();
            LocalDateTime edt = endDate.atTime(LocalTime.MAX);
            List<Appointment> appts = appointmentRepository.findAllByAppointmentDateBetween(sdt, edt);
            return ResponseEntity.ok(appts.stream().map(a -> Map.of(
                "id", a.getId(),
                "sessionId", a.getSessionId(),
                "appointmentDate", a.getAppointmentDate()
            )).toList());
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Formato de fecha inválido"));
        }
    }

    // Availability: accepts ?date=ISO or ?dateTime=ISO
    @GetMapping("/available")
    public ResponseEntity<?> isAvailable(@RequestParam(name = "date", required = false) String date,
                                         @RequestParam(name = "dateTime", required = false) String dateTimeParam) {
        try {
            String value = firstNonBlank(date, dateTimeParam);
            if (isBlank(value)) return ResponseEntity.badRequest().body(Map.of("error", "Parámetro requerido: date o dateTime"));
            LocalDateTime dt = parseFlexibleDateTime(value);
            List<Appointment> conflicts = appointmentRepository.findAllByAppointmentDateBetween(dt, dt);
            return ResponseEntity.ok(Map.of("available", conflicts.isEmpty()));
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Formato de fecha/hora inválido"));
        }
    }

    // Retrieve by session (query)
    @GetMapping("/by-session")
    public ResponseEntity<?> getBySessionParam(@RequestParam String sessionId) {
        return appointmentRepository.findBySessionId(sessionId)
            .<ResponseEntity<?>>map(appt -> ResponseEntity.ok(Map.of(
                "id", appt.getId(),
                "sessionId", appt.getSessionId(),
                "appointmentDate", appt.getAppointmentDate()
            )))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(new HashMap<>()));
    }

    // Retrieve by session (path variable)
    @GetMapping("/by-session/{sessionId}")
    public ResponseEntity<?> getBySession(@PathVariable String sessionId) {
        return getBySessionParam(sessionId);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String asString(Object o) { return o == null ? null : String.valueOf(o); }
    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) { if (!isBlank(v)) return v; }
        return null;
    }

    private static LocalDateTime parseFlexibleDateTime(String value) {
        try { return LocalDateTime.parse(value); } catch (Exception ignore) {}
        try { return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")); } catch (Exception ignore) {}
        try { return OffsetDateTime.parse(value).toLocalDateTime(); } catch (Exception ignore) {}
        try { return ZonedDateTime.parse(value).toLocalDateTime(); } catch (Exception ignore) {}
        try { return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")); } catch (Exception ignore) {}
        throw new DateTimeParseException("Invalid datetime", value, 0);
    }
}
