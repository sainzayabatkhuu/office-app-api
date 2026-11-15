package com.sol.office.identity.service;

import com.sol.office.common.api.PageResponse;
import com.sol.office.common.exception.BusinessException;
import com.sol.office.common.exception.ConflictException;
import com.sol.office.common.exception.NotFoundException;
import com.sol.office.common.security.CurrentUser;
import com.sol.office.identity.domain.CorporateCustomer;
import com.sol.office.identity.domain.Customer;
import com.sol.office.identity.domain.CustomerParts.Address;
import com.sol.office.identity.domain.CustomerType;
import com.sol.office.identity.domain.IndividualCustomer;
import com.sol.office.identity.dto.CustomerDtos.CorporateDetails;
import com.sol.office.identity.dto.CustomerDtos.CustomerDto;
import com.sol.office.identity.dto.CustomerDtos.IndividualDetails;
import com.sol.office.identity.repository.BranchRepository;
import com.sol.office.identity.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.sol.office.identity.service.CoreParameterService.hasText;
import static com.sol.office.identity.service.CoreParameterService.like;

/**
 * Customer information (STDCIF): individual (R) and corporate (C) customers, ported from office-app-api.
 * The type decides the detail table, so it cannot change after creation. Users without multi-branch access
 * only see and maintain customers of their own branch.
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private static final int NUMBER_LENGTH = 9;

    private final CustomerRepository repository;
    private final BranchRepository branchRepository;

    /** All filters optional; text filters are case-insensitive "contains". */
    @Transactional(readOnly = true)
    public PageResponse<CustomerDto> search(CurrentUser user, String customerNumber, String name, CustomerType type,
                                            String branchCode, Pageable pageable) {
        String branch = BranchScope.effectiveBranch(user, hasText(branchCode) ? branchCode.trim() : null);
        Specification<Customer> spec = (root, q, cb) -> cb.conjunction();
        if (hasText(customerNumber)) {
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("customerNumber")), like(customerNumber)));
        }
        if (hasText(name)) {
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), like(name)),
                    cb.like(cb.lower(root.get("shortName")), like(name))));
        }
        if (type != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("customerType"), type));
        }
        if (branch != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("branchCode"), branch));
        }
        return PageResponse.of(repository.findAll(spec, pageable), CustomerService::toDto);
    }

    @Transactional(readOnly = true)
    public CustomerDto get(CurrentUser user, String id) {
        return toDto(find(user, id));
    }

    @Transactional
    public CustomerDto create(CurrentUser user, CustomerDto request) {
        if (request.customerType() == null || request.customerType() == CustomerType.B) {
            throw new BusinessException("VALIDATION_FAILED", "Customer type must be R (individual) or C (corporate).");
        }
        String number = hasText(request.customerNumber()) ? request.customerNumber().trim() : nextCustomerNumber();
        if (number.length() > NUMBER_LENGTH || !number.matches("[A-Za-z0-9]+")) {
            throw new BusinessException("VALIDATION_FAILED", "Customer number must be up to 9 letters or digits.");
        }
        if (repository.existsByCustomerNumber(number)) {
            throw new ConflictException("Customer number already exists: " + number);
        }
        Customer customer = request.customerType() == CustomerType.R ? new IndividualCustomer() : new CorporateCustomer();
        customer.setId(request.customerType().name() + number);
        customer.setCustomerType(request.customerType());
        customer.setCustomerNumber(number);
        apply(user, customer, request);
        return toDto(repository.save(customer));
    }

    @Transactional
    public CustomerDto update(CurrentUser user, String id, CustomerDto request) {
        Customer customer = find(user, id);
        if (request.customerType() != null && request.customerType() != customer.getCustomerType()) {
            throw new BusinessException("TYPE_IMMUTABLE", "The customer type cannot be changed.");
        }
        apply(user, customer, request);
        return toDto(repository.saveAndFlush(customer));
    }

    @Transactional
    public void delete(CurrentUser user, String id) {
        repository.delete(find(user, id));
    }

    private Customer find(CurrentUser user, String id) {
        Customer customer = repository.findById(id).orElseThrow(() -> NotFoundException.of("Customer", id));
        if (!user.canAccessBranch(customer.getBranchCode())) {
            // same answer as a missing id: don't reveal customers of other branches
            throw NotFoundException.of("Customer", id);
        }
        return customer;
    }

    private void apply(CurrentUser user, Customer c, CustomerDto r) {
        if (!hasText(r.fullName())) {
            throw new BusinessException("VALIDATION_FAILED", "Full name is required.");
        }
        if (!hasText(r.branchCode()) || !branchRepository.existsByCode(r.branchCode().trim())) {
            throw new BusinessException("VALIDATION_FAILED", "A valid branch is required.");
        }
        BranchScope.check(user, r.branchCode().trim());
        c.setFullName(r.fullName().trim());
        c.setShortName(r.shortName());
        c.setBranchCode(r.branchCode().trim());
        c.setCustomerCategory(r.customerCategory());
        c.setIsPrivateCustomer(r.isPrivateCustomer());

        if (c instanceof IndividualCustomer ind) {
            IndividualDetails d = r.individual();
            if (d == null || !hasText(d.firstName()) || !hasText(d.lastName())) {
                throw new BusinessException("VALIDATION_FAILED", "First and last name are required for an individual customer.");
            }
            ind.setPrefix1(d.prefix1());
            ind.setPrefix2(d.prefix2());
            ind.setPrefix3(d.prefix3());
            ind.setFirstName(d.firstName().trim());
            ind.setMiddleName(d.middleName());
            ind.setLastName(d.lastName().trim());
            ind.setContactInfo(d.contactInfo());
            ind.setCorrespondenceAddress(d.correspondenceAddress());
            boolean same = "Y".equals(d.sameAsCorrespondenceAddress());
            ind.setSameAsCorrespondenceAddress(same ? "Y" : "N");
            ind.setPermanentAddress(same ? copy(d.correspondenceAddress()) : d.permanentAddress());
            ind.setLanguage(hasText(d.language()) ? d.language() : "mn");
            ind.setCommunicationMode(d.communicationMode());
            ind.setGender(d.gender());
            ind.setNationalId(d.nationalId());
            ind.setBirthPlace(d.birthPlace());
            ind.setBirthCountry(d.birthCountry());
            ind.setDateOfBirth(d.dateOfBirth());
            ind.setMothersMaidenName(d.mothersMaidenName());
            ind.setResidentStatus(d.residentStatus());
            ind.setPassport(d.passport());
            ind.setKycDetails(d.kycDetails());
            ind.setStaff(Boolean.TRUE.equals(d.staff()));
            ind.setMinor(Boolean.TRUE.equals(d.minor()));
            ind.setGuardian(d.guardian());
        } else if (c instanceof CorporateCustomer corp) {
            CorporateDetails d = r.corporate();
            if (d == null || !hasText(d.name())) {
                throw new BusinessException("VALIDATION_FAILED", "Company name is required for a corporate customer.");
            }
            corp.setName(d.name().trim());
            corp.setContactInfo(d.contactInfo());
            corp.setCorrespondenceAddress(d.correspondenceAddress());
            boolean same = "Y".equals(d.sameAsCorrespondenceAddress());
            corp.setSameAsCorrespondenceAddress(same ? "Y" : "N");
            corp.setRegistrationAddress(same ? copy(d.correspondenceAddress()) : d.registrationAddress());
            corp.setLanguage(hasText(d.language()) ? d.language() : "mn");
            corp.setCommunicationMode(d.communicationMode());
            corp.setIncorporation(d.incorporation());
            corp.setKyc(d.kyc());
        }
    }

    private static CustomerDto toDto(Customer c) {
        IndividualDetails individual = null;
        CorporateDetails corporate = null;
        if (c instanceof IndividualCustomer i) {
            individual = new IndividualDetails(i.getPrefix1(), i.getPrefix2(), i.getPrefix3(), i.getFirstName(),
                    i.getMiddleName(), i.getLastName(), i.getContactInfo(), i.getCorrespondenceAddress(),
                    i.getPermanentAddress(), i.getSameAsCorrespondenceAddress(), i.getLanguage(), i.getCommunicationMode(),
                    i.getGender(), i.getNationalId(), i.getBirthPlace(), i.getBirthCountry(), i.getDateOfBirth(),
                    i.getMothersMaidenName(), i.getResidentStatus(), i.getPassport(), i.getKycDetails(), i.isStaff(),
                    i.isMinor(), i.getGuardian());
        } else if (c instanceof CorporateCustomer k) {
            corporate = new CorporateDetails(k.getName(), k.getContactInfo(), k.getCorrespondenceAddress(),
                    k.getRegistrationAddress(), k.getSameAsCorrespondenceAddress(), k.getLanguage(),
                    k.getCommunicationMode(), k.getIncorporation(), k.getKyc());
        }
        return new CustomerDto(c.getId(), c.getCustomerType(), c.getCustomerNumber(), c.getFullName(), c.getShortName(),
                c.getBranchCode(), c.getCustomerCategory(), c.getIsPrivateCustomer(), individual, corporate);
    }

    /** Next numeric customer number (9 digits, zero padded). */
    private String nextCustomerNumber() {
        long next = repository.findMaxNumericCustomerNumber().map(n -> Long.parseLong(n) + 1).orElse(1L);
        return String.format("%0" + NUMBER_LENGTH + "d", next);
    }

    private static Address copy(Address a) {
        return a == null ? null : a.copy();
    }
}
