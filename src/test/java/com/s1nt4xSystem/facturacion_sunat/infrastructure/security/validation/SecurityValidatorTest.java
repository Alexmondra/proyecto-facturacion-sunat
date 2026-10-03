package com.s1nt4xSystem.facturacion_sunat.infrastructure.security.validation;

import com.s1nt4xSystem.facturacion_sunat.infrastructure.security.AuthenticatedPrincipal;
import com.s1nt4xSystem.facturacion_sunat.platform.account.model.CuentaSaas;
import com.s1nt4xSystem.facturacion_sunat.platform.account.repository.CuentaSaasRepository;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.model.EmpresaRouter;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.repository.EmpresaRouterRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityValidatorTest {

    @Mock
    private CuentaSaasRepository cuentaSaasRepository;

    @Mock
    private EmpresaRouterRepository empresaRouterRepository;

    private SecurityValidator securityValidator;

    @BeforeEach
    void setUp() {
        securityValidator = new SecurityValidator(cuentaSaasRepository, empresaRouterRepository);
    }

    @Test
    void validateTokenPresent_whenTokenNullOrEmpty_shouldThrow() {
        BusinessException ex1 = assertThrows(BusinessException.class, () ->
                securityValidator.validateTokenPresent(null));
        assertEquals(ErrorCode.SECURITY_TOKEN_REQUIRED.getCode(), ex1.getCode());

        BusinessException ex2 = assertThrows(BusinessException.class, () ->
                securityValidator.validateTokenPresent("   "));
        assertEquals(ErrorCode.SECURITY_TOKEN_REQUIRED.getCode(), ex2.getCode());
    }

    @Test
    void validateAndResolvePrincipal_whenTokenMatchesActiveSaasAccount_shouldReturnPrincipal() {
        CuentaSaas cuenta = new CuentaSaas();
        cuenta.setId(10L);
        cuenta.setTipo("ADMIN");
        cuenta.setEstado(true);

        when(cuentaSaasRepository.findByAccessKey("valid-admin-token")).thenReturn(Optional.of(cuenta));

        AuthenticatedPrincipal principal = securityValidator.validateAndResolvePrincipal("valid-admin-token");

        assertNotNull(principal);
        assertEquals(10L, principal.getSaasId());
        assertTrue(principal.isAdmin());
        assertEquals("valid-admin-token", principal.getAccessKey());
    }

    @Test
    void validateAndResolvePrincipal_whenTokenMatchesInactiveSaasAccount_shouldThrow() {
        CuentaSaas cuenta = new CuentaSaas();
        cuenta.setId(11L);
        cuenta.setTipo("CLIENTE");
        cuenta.setEstado(false);

        when(cuentaSaasRepository.findByAccessKey("inactive-token")).thenReturn(Optional.of(cuenta));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                securityValidator.validateAndResolvePrincipal("inactive-token"));
        assertEquals(ErrorCode.SECURITY_ACCOUNT_INACTIVE.getCode(), ex.getCode());
    }

    @Test
    void validateAndResolvePrincipal_whenTokenMatchesActiveEmpresaRouter_shouldReturnPrincipal() {
        CuentaSaas cuentaMatriz = new CuentaSaas();
        cuentaMatriz.setId(20L);
        cuentaMatriz.setEstado(true);

        EmpresaRouter router = new EmpresaRouter();
        router.setRuc("20123456789");
        router.setEstado("ACTIVO");
        router.setCuentaSaas(cuentaMatriz);

        when(cuentaSaasRepository.findByAccessKey("empresa-token")).thenReturn(Optional.empty());
        when(empresaRouterRepository.findByAccessKey("empresa-token")).thenReturn(Optional.of(router));

        AuthenticatedPrincipal principal = securityValidator.validateAndResolvePrincipal("empresa-token");

        assertNotNull(principal);
        assertEquals(20L, principal.getSaasId());
        assertTrue(principal.isEmpresa());
        assertEquals(router, principal.getEmpresaRouter());
    }

    @Test
    void validateAndResolvePrincipal_whenEmpresaInactive_shouldThrow() {
        EmpresaRouter router = new EmpresaRouter();
        router.setEstado("INACTIVO");

        when(cuentaSaasRepository.findByAccessKey("inactive-empresa-token")).thenReturn(Optional.empty());
        when(empresaRouterRepository.findByAccessKey("inactive-empresa-token")).thenReturn(Optional.of(router));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                securityValidator.validateAndResolvePrincipal("inactive-empresa-token"));
        assertEquals(ErrorCode.SECURITY_EMPRESA_INACTIVE.getCode(), ex.getCode());
    }

    @Test
    void validateAndResolvePrincipal_whenEmpresaParentAccountInactive_shouldThrow() {
        CuentaSaas cuentaMatriz = new CuentaSaas();
        cuentaMatriz.setId(30L);
        cuentaMatriz.setEstado(false);

        EmpresaRouter router = new EmpresaRouter();
        router.setEstado("ACTIVO");
        router.setCuentaSaas(cuentaMatriz);

        when(cuentaSaasRepository.findByAccessKey("parent-inactive-token")).thenReturn(Optional.empty());
        when(empresaRouterRepository.findByAccessKey("parent-inactive-token")).thenReturn(Optional.of(router));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                securityValidator.validateAndResolvePrincipal("parent-inactive-token"));
        assertEquals(ErrorCode.SECURITY_ACCOUNT_INACTIVE.getCode(), ex.getCode());
    }

    @Test
    void validateAndResolvePrincipal_whenTokenNotFound_shouldThrow() {
        when(cuentaSaasRepository.findByAccessKey("unknown-token")).thenReturn(Optional.empty());
        when(empresaRouterRepository.findByAccessKey("unknown-token")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                securityValidator.validateAndResolvePrincipal("unknown-token"));
        assertEquals(ErrorCode.SECURITY_INVALID_TOKEN.getCode(), ex.getCode());
    }

    @Test
    void validatePlatformAccess_whenEmpresaTriesToAccessPlatform_shouldThrowForbidden() {
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.builder()
                .saasId(1L)
                .tipo("EMPRESA")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                securityValidator.validatePlatformAccess(principal, "/api/v1/platform/accounts", "GET"));
        assertEquals(ErrorCode.SECURITY_PLATFORM_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    void validatePlatformAccess_whenNonAdminTriesToModifyPlans_shouldThrow() {
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.builder()
                .saasId(1L)
                .tipo("CLIENTE")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                securityValidator.validatePlatformAccess(principal, "/api/v1/platform/plans", "POST"));
        assertEquals(ErrorCode.SECURITY_PLAN_ADMIN_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void validatePlatformAccess_whenClienteTriesToCreateOrListAllAccounts_shouldThrow() {
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.builder()
                .saasId(1L)
                .tipo("CLIENTE")
                .build();

        // Create account
        BusinessException exCreate = assertThrows(BusinessException.class, () ->
                securityValidator.validatePlatformAccess(principal, "/api/v1/platform/accounts", "POST"));
        assertEquals(ErrorCode.SECURITY_ACCOUNT_CREATE_ADMIN_REQUIRED.getCode(), exCreate.getCode());

        // List all accounts
        BusinessException exList = assertThrows(BusinessException.class, () ->
                securityValidator.validatePlatformAccess(principal, "/api/v1/platform/accounts", "GET"));
        assertEquals(ErrorCode.SECURITY_ACCOUNT_LIST_ADMIN_REQUIRED.getCode(), exList.getCode());
    }

    @Test
    void validatePlatformAccess_whenAdminAccesses_shouldSucceed() {
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.builder()
                .saasId(1L)
                .tipo("ADMIN")
                .build();

        assertDoesNotThrow(() ->
                securityValidator.validatePlatformAccess(principal, "/api/v1/platform/plans", "POST"));
        assertDoesNotThrow(() ->
                securityValidator.validatePlatformAccess(principal, "/api/v1/platform/accounts", "POST"));
    }
}
