package com.sol.office_app.service;

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
        return null;
    }

    @Override
    public Page<CustomerDTO> search(String query, Pageable pageable) {
        return null;
    }

    @Override
    public Optional<CustomerDTO> get(Long id) {
        return Optional.empty();
    }

    @Override
    public Optional<CustomerDTO> save(CustomerDTO entity) {
        return Optional.empty();
    }

    @Override
    public Optional<CustomerDTO> update(Long aLong, CustomerDTO entity) {
        return Optional.empty();
    }

    @Override
    public void delete(Long aLong) {

    }
}
