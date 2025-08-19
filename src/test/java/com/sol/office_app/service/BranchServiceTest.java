package com.sol.office_app.service;

import com.sol.office_app.dto.BranchDTO;
import com.sol.office_app.entity.Branch;
import com.sol.office_app.mapper.BranchDTOMapper;
import com.sol.office_app.repository.BranchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private BranchDTOMapper branchDTOMapper;

    @InjectMocks
    private BranchService branchService;

    @Test
    void testFindAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Branch branch = new Branch();
        branch.setSolId("123");
        branch.setName("Branch 1");
        branch.setAlternateBranchCode("123");
        branch.setBranchAvailable(false);

        BranchDTO dto = new BranchDTO("123", "Branch 1", "B001", true);

        Page<Branch> branchPage = new PageImpl<>(List.of(branch));
        when(branchRepository.findAll(pageable)).thenReturn(branchPage);
        when(branchDTOMapper.apply(branch)).thenReturn(dto);

        Page<BranchDTO> result = branchService.findAll(pageable);
        assertEquals(1, result.getTotalElements());
        assertEquals("Branch 1", result.getContent().get(0).name());
    }
    @Test
    void testGetById() {
        Branch branch = new Branch();
        branch.setSolId("123");
        branch.setName("Test Branch");
        branch.setAlternateBranchCode("123");
        branch.setBranchAvailable(false);

        BranchDTO dto = new BranchDTO("123", "Test Branch", "B001", true);

        when(branchRepository.findById("123")).thenReturn(Optional.of(branch));
        when(branchDTOMapper.apply(branch)).thenReturn(dto);

        Optional<BranchDTO> result = branchService.get("123");
        assertTrue(result.isPresent());
        assertEquals("Test Branch", result.get().name());
    }

    @Test
    void testSave() {
        BranchDTO input = new BranchDTO(null, "New Branch", "B002", true);
        Branch saved = new Branch();
        saved.setSolId("124");
        saved.setName("New Branch");
        saved.setAlternateBranchCode("B002");
        saved.setBranchAvailable(true);

        BranchDTO output = new BranchDTO("124", "New Branch", "B002", true);

        when(branchRepository.save(any(Branch.class))).thenReturn(saved);
        when(branchDTOMapper.apply(saved)).thenReturn(output);

        Optional<BranchDTO> result = branchService.save(input);
        assertTrue(result.isPresent());
        assertEquals("124", result.get().solId());
    }

    @Test
    void testUpdate_Found() {
        Branch existing = new Branch();
        existing.setSolId("123");
        existing.setName("Old");

        BranchDTO update = new BranchDTO("123", "Updated", "B009", false);
        Branch updated = new Branch();
        updated.setSolId("123");
        updated.setName("Updated");
        updated.setAlternateBranchCode("B009");
        updated.setBranchAvailable(false);

        BranchDTO dto = new BranchDTO("123", "Updated", "B009", false);

        when(branchRepository.findById("123")).thenReturn(Optional.of(existing));
        when(branchRepository.save(existing)).thenReturn(updated);
        when(branchDTOMapper.apply(updated)).thenReturn(dto);

        Optional<BranchDTO> result = branchService.update("123", update);
        assertTrue(result.isPresent());
        assertEquals("Updated", result.get().name());
    }

    @Test
    void testUpdate_NotFound() {
        BranchDTO update = new BranchDTO("123", "Updated", "B009", false);
        when(branchRepository.findById("123")).thenReturn(Optional.empty());

        Optional<BranchDTO> result = branchService.update("123", update);
        assertTrue(result.isEmpty());
    }

    @Test
    void testDelete_Found() {

        doNothing().when(branchRepository).deleteById("321");

        branchService.delete("321");
    }

    @Test
    void testDelete_NotFound() {
        when(branchRepository.findById("321")).thenReturn(Optional.empty());

        branchService.delete("321");
    }
}