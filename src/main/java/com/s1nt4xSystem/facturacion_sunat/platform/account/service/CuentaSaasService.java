package com.s1nt4xSystem.facturacion_sunat.platform.account.service;

import com.s1nt4xSystem.facturacion_sunat.platform.account.dto.CuentaSaasRequest;
import com.s1nt4xSystem.facturacion_sunat.platform.account.dto.CuentaSaasResponse;
import com.s1nt4xSystem.facturacion_sunat.platform.account.model.CuentaSaas;

import java.util.List;

public interface CuentaSaasService {
    CuentaSaasResponse createAccount(CuentaSaasRequest request);
    CuentaSaasResponse getAccountById(Long id);
    CuentaSaas getAccountEntity(Long id);
    List<CuentaSaasResponse> getAllAccounts();
    CuentaSaasResponse updateAccount(Long id, CuentaSaasRequest request);
    CuentaSaasResponse updateStatus(Long id, Boolean estado);
    CuentaSaasResponse regenerateAccessKey(Long id);
    void deleteAccount(Long id);
}
