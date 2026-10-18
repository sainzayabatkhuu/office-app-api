package com.sol.office_app.service;

import com.sol.office_app.exception.ApiException;
import org.springframework.http.HttpStatus;

import com.sol.office_app.common.GeneralService;
import com.sol.office_app.dto.CustomerDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomerService implements GeneralService<CustomerDTO, Long> {
    @Override
    public Page<CustomerDTO> findAll(Pageable pageable) {
        throw notImplemented();
    }

    @Override
    public Page<CustomerDTO> search(String query, Pageable pageable) {
        throw notImplemented();
    }

    @Override
    public Optional<CustomerDTO> get(Long id) {
        throw notImplemented();
    }

    @Override
    public Optional<CustomerDTO> save(CustomerDTO entity) {
        throw notImplemented();
    }

    @Override
    public Optional<CustomerDTO> update(Long aLong, CustomerDTO entity) {
        throw notImplemented();
    }

    @Override
    public void delete(Long aLong) {
        throw notImplemented();
    }

    /** Customer maintenance (STDCIF) is not built yet; say so instead of returning empty bodies. */
    private static ApiException notImplemented() {
        return new ApiException(HttpStatus.NOT_IMPLEMENTED, "NOT_IMPLEMENTED", "Customer maintenance is not implemented yet.");
    }
}
