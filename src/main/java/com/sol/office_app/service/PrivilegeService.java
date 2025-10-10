package com.sol.office_app.service;


import com.sol.office_app.common.GeneralService;
import com.sol.office_app.dto.PrivilegeDTO;
import com.sol.office_app.entity.Privilege;
import com.sol.office_app.mapper.PrivilegeDTOMapper;
import com.sol.office_app.repository.PrivilegeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PrivilegeService implements GeneralService<PrivilegeDTO, Long> {
    @Autowired
    private PrivilegeDTOMapper privilegeDTOMapper;
    @Autowired
    private PrivilegeRepository privilegeRepository;

    @Override
    public Page<PrivilegeDTO> findAll(Pageable pageable) {
        Page<Privilege> privileges = privilegeRepository.findAll(pageable);
        return privileges.map(privilegeDTOMapper);
    }

    @Override
    public Page<PrivilegeDTO> search(String query, Pageable pageable) {
        return null;
    }

    @Override
    public Optional<PrivilegeDTO> get(Long id) {
        return privilegeRepository.findById(id).map(privilegeDTOMapper);
    }

    @Override
    public Optional<PrivilegeDTO> save(PrivilegeDTO entity) {
        Privilege privilege = new Privilege();
        privilege.setValue(entity.name());
        privilege.setDescription(entity.description());
        privilege = privilegeRepository.save(privilege);
        return Optional.of(privilegeDTOMapper.apply(privilege));
    }

    @Override
    public Optional<PrivilegeDTO> update(Long id, PrivilegeDTO entity) {
        Optional<Privilege> privilege = privilegeRepository.findById(id);
        if (privilege.isPresent()) {
            Privilege existingPrivilege = privilege.get();
            existingPrivilege.setValue(entity.name());
            existingPrivilege.setDescription(entity.description());
            privilegeRepository.save(existingPrivilege);
            return Optional.of(privilegeDTOMapper.apply(existingPrivilege));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void delete(Long id) {
        privilegeRepository.deleteById(id);
    }
}
