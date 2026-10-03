package com.s1nt4xSystem.facturacion_sunat.platform.account.service;

import com.s1nt4xSystem.facturacion_sunat.platform.account.dto.CuentaSaasRequest;
import com.s1nt4xSystem.facturacion_sunat.platform.account.dto.CuentaSaasResponse;
import com.s1nt4xSystem.facturacion_sunat.platform.account.model.CuentaSaas;
import com.s1nt4xSystem.facturacion_sunat.platform.account.repository.CuentaSaasRepository;
import com.s1nt4xSystem.facturacion_sunat.platform.plan.model.Plan;
import com.s1nt4xSystem.facturacion_sunat.platform.plan.service.PlanService;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.DomainException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ResourceNotFoundException;
import com.s1nt4xSystem.facturacion_sunat.platform.account.validation.CuentaSaasValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CuentaSaasServiceImpl implements CuentaSaasService {

    private final CuentaSaasRepository cuentaSaasRepository;
    private final PlanService planService;
    private final CuentaSaasValidator cuentaSaasValidator;

    @Autowired
    public CuentaSaasServiceImpl(CuentaSaasRepository cuentaSaasRepository,
                                 PlanService planService,
                                 CuentaSaasValidator cuentaSaasValidator) {
        this.cuentaSaasRepository = cuentaSaasRepository;
        this.planService = planService;
        this.cuentaSaasValidator = cuentaSaasValidator;
    }

    public CuentaSaasServiceImpl(CuentaSaasRepository cuentaSaasRepository, PlanService planService) {
        this(cuentaSaasRepository, planService, new CuentaSaasValidator(cuentaSaasRepository));
    }

    @Override
    public CuentaSaasResponse createAccount(CuentaSaasRequest request) {
        cuentaSaasValidator.validateNewAccount(request);
        Plan plan = planService.getPlanEntity(request.getPlanId());

        String accessKey = request.getAccessKey();
        if (accessKey == null || accessKey.trim().isEmpty()) {
            accessKey = "ak_" + UUID.randomUUID().toString().replace("-", "");
        } else {
            accessKey = accessKey.trim();
        }

        CuentaSaas cuenta = CuentaSaas.builder()
                .nombre(request.getNombre())
                .plan(plan)
                .accessKey(accessKey)
                .fechaCorte(request.getFechaCorte())
                .tipo(request.getTipo() != null && !request.getTipo().isBlank() ? request.getTipo().toUpperCase() : "CLIENTE")
                .consumidoMes(0)
                .estado(request.getEstado() != null ? request.getEstado() : true)
                .build();

        CuentaSaas saved = cuentaSaasRepository.save(cuenta);
        return CuentaSaasResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CuentaSaasResponse getAccountById(Long id) {
        return CuentaSaasResponse.fromEntity(getAccountEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public CuentaSaas getAccountEntity(Long id) {
        return cuentaSaasRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta SaaS", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaSaasResponse> getAllAccounts() {
        return cuentaSaasRepository.findAll().stream()
                .map(CuentaSaasResponse::fromEntity)
                .toList();
    }

    @Override
    public CuentaSaasResponse updateAccount(Long id, CuentaSaasRequest request) {
        CuentaSaas cuenta = getAccountEntity(id);

        if (request.getPlanId() != null && !request.getPlanId().equals(cuenta.getPlan().getId())) {
            Plan plan = planService.getPlanEntity(request.getPlanId());
            cuenta.setPlan(plan);
        }

        cuenta.setNombre(request.getNombre());
        cuenta.setFechaCorte(request.getFechaCorte());
        if (request.getTipo() != null && !request.getTipo().isBlank()) {
            cuenta.setTipo(request.getTipo().toUpperCase().trim());
        }
        if (request.getEstado() != null) {
            cuenta.setEstado(request.getEstado());
        }

        return CuentaSaasResponse.fromEntity(cuentaSaasRepository.save(cuenta));
    }

    @Override
    public CuentaSaasResponse updateStatus(Long id, Boolean estado) {
        CuentaSaas cuenta = getAccountEntity(id);
        cuenta.setEstado(estado);
        return CuentaSaasResponse.fromEntity(cuentaSaasRepository.save(cuenta));
    }

    @Override
    public CuentaSaasResponse regenerateAccessKey(Long id) {
        CuentaSaas cuenta = getAccountEntity(id);
        String newKey = "ak_" + UUID.randomUUID().toString().replace("-", "");
        cuenta.setAccessKey(newKey);
        return CuentaSaasResponse.fromEntity(cuentaSaasRepository.save(cuenta));
    }

    @Override
    public void deleteAccount(Long id) {
        CuentaSaas cuenta = getAccountEntity(id);
        cuenta.setEstado(false);
        cuentaSaasRepository.save(cuenta);
    }
}
