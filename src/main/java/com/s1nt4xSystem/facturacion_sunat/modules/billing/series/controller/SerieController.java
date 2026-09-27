package com.s1nt4xSystem.facturacion_sunat.modules.billing.series.controller;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.dto.SerieRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.dto.SerieResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.dto.SerieUpdateRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.service.SerieService;
import com.s1nt4xSystem.facturacion_sunat.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenant/series")
public class SerieController {

    private final SerieService serieService;

    public SerieController(SerieService serieService) {
        this.serieService = serieService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SerieResponse>> createSerie(
            @Valid @RequestBody SerieRequest request) {
        SerieResponse response = serieService.createSerie(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Serie creada exitosamente"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SerieResponse>>> getAllSeries() {
        return ResponseEntity.ok(ApiResponse.ok(serieService.getAllSeries()));
    }

    @GetMapping("/by-branch/{sucursalId}")
    public ResponseEntity<ApiResponse<List<SerieResponse>>> getSeriesByBranch(@PathVariable UUID sucursalId) {
        return ResponseEntity.ok(ApiResponse.ok(serieService.getSeriesBySucursal(sucursalId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SerieResponse>> getSerieById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(serieService.getSerieById(id)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<SerieResponse>> patchSerie(
            @PathVariable UUID id,
            @Valid @RequestBody SerieUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                serieService.updateSerie(id, request), "Serie actualizada exitosamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SerieResponse>> updateSerie(
            @PathVariable UUID id,
            @Valid @RequestBody SerieUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                serieService.updateSerie(id, request), "Serie actualizada exitosamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSerie(@PathVariable UUID id) {
        serieService.deleteSerie(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Serie eliminada exitosamente"));
    }
}
