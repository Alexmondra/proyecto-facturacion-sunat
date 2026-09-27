package com.s1nt4xSystem.facturacion_sunat.platform.catalog.controller;

import com.s1nt4xSystem.facturacion_sunat.platform.catalog.dto.UbigeoResponse;
import com.s1nt4xSystem.facturacion_sunat.platform.catalog.service.CatalogoUbigeoService;
import com.s1nt4xSystem.facturacion_sunat.shared.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalogo/ubigeos")
public class CatalogoUbigeoController {

    private final CatalogoUbigeoService ubigeoService;

    public CatalogoUbigeoController(CatalogoUbigeoService ubigeoService) {
        this.ubigeoService = ubigeoService;
    }

    /**
     * Lista todos los ubigeos o busca por término (?query=iquitos o ?query=160101)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UbigeoResponse>>> listOrSearch(
            @RequestParam(required = false) String query) {
        List<UbigeoResponse> data = ubigeoService.searchUbigeos(query);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /**
     * Lista los 25 departamentos únicos de Perú
     */
    @GetMapping("/departamentos")
    public ResponseEntity<ApiResponse<List<String>>> getDepartamentos() {
        return ResponseEntity.ok(ApiResponse.ok(ubigeoService.getDepartamentos()));
    }

    /**
     * Lista las provincias de un departamento (?departamento=Loreto)
     */
    @GetMapping("/provincias")
    public ResponseEntity<ApiResponse<List<String>>> getProvincias(
            @RequestParam String departamento) {
        return ResponseEntity.ok(ApiResponse.ok(ubigeoService.getProvincias(departamento)));
    }

    /**
     * Lista los distritos con su código de ubigeo y bandera de Amazonía
     * (?departamento=Loreto&provincia=Maynas)
     */
    @GetMapping("/distritos")
    public ResponseEntity<ApiResponse<List<UbigeoResponse>>> getDistritos(
            @RequestParam String departamento,
            @RequestParam String provincia) {
        return ResponseEntity.ok(ApiResponse.ok(ubigeoService.getDistritos(departamento, provincia)));
    }

    /**
     * Consulta detalle de un ubigeo por su código oficial de 6 dígitos
     */
    @GetMapping("/{codigo}")
    public ResponseEntity<ApiResponse<UbigeoResponse>> getByCodigo(@PathVariable String codigo) {
        return ubigeoService.findByCodigo(codigo)
                .map(UbigeoResponse::fromEntity)
                .map(res -> ResponseEntity.ok(ApiResponse.ok(res)))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error("Ubigeo no encontrado con código: " + codigo)));
    }
}
