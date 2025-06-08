package com.sol.office_app.service;

import com.sol.office_app.common.GeneralService;
import com.sol.office_app.dto.BranchDTO;
import com.sol.office_app.entity.Branch;
import com.sol.office_app.repository.BranchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

public interface BranchService extends GeneralService<BranchDTO, String> {

}

