package com.sol.office_app.service;

import com.sol.office_app.dto.BranchDTO;
import com.sol.office_app.entity.Branch;
import com.sol.office_app.mapper.BranchDTOMapper;
import com.sol.office_app.repository.BranchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BranchServiceImpl implements BranchService {
    @Autowired
    private BranchDTOMapper branchDTOMapper;

    @Autowired
    private BranchRepository branchRepository;

    @Override
    public Page<BranchDTO> findAll(Pageable pageable) {
        Page<Branch> branches = branchRepository.findAll(pageable);
        return branches.map(branchDTOMapper);
    }

    @Override
    public Page<BranchDTO> search(String query, Pageable pageable) {
        return null;
    }

    @Override
    public Optional<BranchDTO> get(String id) {
        return branchRepository.findById(id).map(branchDTOMapper);
    }

    @Override
    public Optional<BranchDTO> save(BranchDTO entity) {
        Branch branch = new Branch();
        branch.setName(entity.name());
        branch.setAlternateBranchCode(entity.alternateBranchCode());
        branch.setBranchAvailable(entity.branchAvailable());
        branch = branchRepository.save(branch);
        return Optional.of(branchDTOMapper.apply(branch));
    }

    @Override
    public Optional<BranchDTO> update(String id, BranchDTO entity) {
        Optional<Branch> branch = branchRepository.findById(id);
        if (branch.isPresent()) {
            Branch existingBranch = branch.get();
            existingBranch.setName(entity.name());
            existingBranch.setAlternateBranchCode(entity.alternateBranchCode());
            existingBranch.setBranchAvailable(entity.branchAvailable());
            existingBranch = branchRepository.save(existingBranch);
            return Optional.of(branchDTOMapper.apply(existingBranch));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<BranchDTO> delete(String id, BranchDTO entity) {
        Optional<Branch> existingBranchOpt = branchRepository.findById(id);
        if (existingBranchOpt.isPresent()) {
            branchRepository.deleteById(id);
            return Optional.of(branchDTOMapper.apply(existingBranchOpt.get()));
        } else {
            return Optional.empty();
        }
    }
}
