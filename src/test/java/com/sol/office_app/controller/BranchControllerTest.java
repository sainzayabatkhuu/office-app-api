package com.sol.office_app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sol.office_app.dto.BranchDTO;
import com.sol.office_app.service.BranchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BranchControllerTest {

    private MockMvc mockMvc;
    @Mock
    private BranchService branchService;

    @InjectMocks
    private BranchController branchController;
    private BranchDTO mockBranch;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(branchController).setMessageConverters(new MappingJackson2HttpMessageConverter()).build();
        mockBranch = new BranchDTO("111", "Main Branch", "111", false);
    }

    @Test
    void testIndex() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<BranchDTO> page = new PageImpl<>(List.of(mockBranch), pageable, 1);
        when(branchService.findAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/branches"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].solId").value("111"))
                .andExpect(jsonPath("$.content[0].name").value("Main Branch"));
    }

    @Test
    void testGetById_Found() throws Exception {
        when(branchService.get("111")).thenReturn(Optional.of(mockBranch));

        mockMvc.perform(get("/api/v1/branches/111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solId").value("111"))
                .andExpect(jsonPath("$.name").value("Main Branch"));
    }

    @Test
    void testGetById_NotFound() throws Exception {
        when(branchService.get("2")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/branches/2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testSave_Success() throws Exception {
        when(branchService.save(any(BranchDTO.class))).thenReturn(Optional.of(mockBranch));

        mockMvc.perform(post("/api/v1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockBranch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solId").value("111"))
                .andExpect(jsonPath("$.name").value("Main Branch"));
    }

    @Test
    void testSave_Failure() throws Exception {
        when(branchService.save(any(BranchDTO.class))).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockBranch)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdate_Success() throws Exception {
        when(branchService.update(Mockito.eq("111"), any(BranchDTO.class))).thenReturn(Optional.of(mockBranch));

        mockMvc.perform(put("/api/v1/branches/111")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockBranch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solId").value("111"))
                .andExpect(jsonPath("$.name").value("Main Branch"));
    }

    @Test
    void testUpdate_NotFound() throws Exception {
        when(branchService.update(Mockito.eq("1"), any(BranchDTO.class))).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/branches/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockBranch)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDelete_Success() throws Exception {
        when(branchService.delete(Mockito.eq("111"), any(BranchDTO.class))).thenReturn(Optional.of(mockBranch));

        mockMvc.perform(delete("/api/v1/branches/111")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockBranch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solId").value("111"));
    }

    @Test
    void testDelete_NotFound() throws Exception {
        when(branchService.delete(Mockito.eq("1"), any(BranchDTO.class))).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/branches/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockBranch)))
                .andExpect(status().isNotFound());
    }
}