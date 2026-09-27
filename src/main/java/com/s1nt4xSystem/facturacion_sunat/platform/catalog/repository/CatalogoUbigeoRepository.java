package com.s1nt4xSystem.facturacion_sunat.platform.catalog.repository;

import com.s1nt4xSystem.facturacion_sunat.platform.catalog.model.CatalogoUbigeo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CatalogoUbigeoRepository extends JpaRepository<CatalogoUbigeo, Long> {

    Optional<CatalogoUbigeo> findByCodigoAndEstadoTrue(String codigo);

    Optional<CatalogoUbigeo> findByCodigo(String codigo);

    @Query("SELECT DISTINCT u.departamento FROM CatalogoUbigeo u WHERE u.estado = true ORDER BY u.departamento ASC")
    List<String> findDistinctDepartamentos();

    @Query("SELECT DISTINCT u.provincia FROM CatalogoUbigeo u WHERE UPPER(u.departamento) = UPPER(:departamento) AND u.estado = true ORDER BY u.provincia ASC")
    List<String> findDistinctProvinciasByDepartamento(@Param("departamento") String departamento);

    @Query("SELECT u FROM CatalogoUbigeo u WHERE UPPER(u.departamento) = UPPER(:departamento) AND UPPER(u.provincia) = UPPER(:provincia) AND u.estado = true ORDER BY u.distrito ASC")
    List<CatalogoUbigeo> findByDepartamentoAndProvincia(@Param("departamento") String departamento, @Param("provincia") String provincia);

    @Query("SELECT u FROM CatalogoUbigeo u WHERE u.estado = true AND " +
           "(UPPER(u.distrito) LIKE UPPER(CONCAT('%', :query, '%')) OR " +
           " UPPER(u.provincia) LIKE UPPER(CONCAT('%', :query, '%')) OR " +
           " UPPER(u.departamento) LIKE UPPER(CONCAT('%', :query, '%')) OR " +
           " u.codigo LIKE CONCAT(:query, '%')) ORDER BY u.codigo ASC")
    List<CatalogoUbigeo> searchUbigeos(@Param("query") String query);
}
