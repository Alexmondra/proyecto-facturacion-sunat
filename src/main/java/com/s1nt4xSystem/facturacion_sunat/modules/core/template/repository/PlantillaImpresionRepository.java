package com.s1nt4xSystem.facturacion_sunat.modules.core.template.repository;

import com.s1nt4xSystem.facturacion_sunat.modules.core.template.model.PlantillaImpresion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlantillaImpresionRepository extends JpaRepository<PlantillaImpresion, UUID> {

    List<PlantillaImpresion> findByEmpresaIdAndEstadoTrue(UUID empresaId);

    @Query("SELECT p FROM PlantillaImpresion p " +
           "WHERE p.empresaId = :empresaId " +
           "AND p.tipoFormato = :tipoFormato " +
           "AND p.estado = true " +
           "ORDER BY CASE WHEN p.sucursalId = :sucursalId THEN 0 ELSE 1 END")
    List<PlantillaImpresion> findActivasPorFormato(@Param("empresaId") UUID empresaId,
                                                   @Param("sucursalId") UUID sucursalId,
                                                   @Param("tipoFormato") String tipoFormato);

    default Optional<PlantillaImpresion> resolverPlantilla(UUID empresaId, UUID sucursalId, String tipoFormato) {
        List<PlantillaImpresion> list = findActivasPorFormato(empresaId, sucursalId, tipoFormato);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
