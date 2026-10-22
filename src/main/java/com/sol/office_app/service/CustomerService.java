package com.sol.office_app.service;

import com.sol.office_app.dto.CustomerDTO;
import com.sol.office_app.dto.CustomerDTO.CorporateDetails;
import com.sol.office_app.dto.CustomerDTO.IndividualDetails;
import com.sol.office_app.entity.CorporateCustomer;
import com.sol.office_app.entity.Customer;
import com.sol.office_app.entity.IndividualCustomer;
import com.sol.office_app.enums.CustomerType;
import com.sol.office_app.exception.ApiException;
import com.sol.office_app.repository.BranchRepository;
import com.sol.office_app.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Customer information (STDCIF): individual (R) and corporate (C) customers. The type decides the table
 * (TABLE_PER_CLASS), so it cannot change after creation.
 */
@Service
public class CustomerService {

    private static final int NUMBER_LENGTH = 9;

    private final CustomerRepository repository;
    private final BranchRepository branchRepository;

    public CustomerService(CustomerRepository repository, BranchRepository branchRepository) {
        this.repository = repository;
        this.branchRepository = branchRepository;
    }

    /** All filters optional; text filters are case-insensitive "contains". */
    @Transactional(readOnly = true)
    public Page<CustomerDTO> search(String customerNumber, String name, CustomerType type, String branchCode, Pageable pageable) {
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
        if (hasText(branchCode)) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("branchCode"), branchCode.trim()));
        }
        return repository.findAll(spec, pageable).map(CustomerService::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<CustomerDTO> get(String id) {
        return repository.findById(id).map(CustomerService::toDto);
    }

    @Transactional
    public CustomerDTO create(CustomerDTO request) {
        if (request.customerType() == null || request.customerType() == CustomerType.B) {
            throw ApiException.badRequest("VALIDATION_FAILED", "Customer type must be R (individual) or C (corporate).");
        }
        String number = hasText(request.customerNumber()) ? request.customerNumber().trim() : nextCustomerNumber();
        if (number.length() > NUMBER_LENGTH || !number.matches("[A-Za-z0-9]+")) {
            throw ApiException.badRequest("VALIDATION_FAILED", "Customer number must be up to 9 letters or digits.");
        }
        if (repository.existsByCustomerNumber(number)) {
            throw ApiException.conflict("Customer number already exists: " + number);
        }
        Customer customer = request.customerType() == CustomerType.R ? new IndividualCustomer() : new CorporateCustomer();
        customer.setCustomerType(request.customerType());
        customer.setCustomerNumber(number);
        apply(customer, request);
        return toDto(repository.save(customer));
    }

    @Transactional
    public Optional<CustomerDTO> update(String id, CustomerDTO request) {
        Optional<Customer> found = repository.findById(id);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Customer customer = found.get();
        if (request.customerType() != null && request.customerType() != customer.getCustomerType()) {
            throw ApiException.badRequest("TYPE_IMMUTABLE", "The customer type cannot be changed.");
        }
        apply(customer, request);
        return Optional.of(toDto(customer));
    }

    @Transactional
    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw ApiException.notFound("Customer", id);
        }
        repository.deleteById(id);
    }

    private void apply(Customer c, CustomerDTO r) {
        if (!hasText(r.fullName())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "Full name is required.");
        }
        if (!hasText(r.branchCode()) || !branchRepository.existsById(r.branchCode())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "A valid branch is required.");
        }
        c.setFullName(r.fullName().trim());
        c.setShortName(r.shortName());
        c.setBranchCode(r.branchCode());
        c.setCustomerCategory(r.customerCategory());
        c.setIsPrivateCustomer(r.isPrivateCustomer());

        if (c instanceof IndividualCustomer ind) {
            IndividualDetails d = r.individual();
            if (d == null || !hasText(d.firstName()) || !hasText(d.lastName())) {
                throw ApiException.badRequest("VALIDATION_FAILED", "First and last name are required for an individual customer.");
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
                throw ApiException.badRequest("VALIDATION_FAILED", "Company name is required for a corporate customer.");
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

    private static CustomerDTO toDto(Customer c) {
        IndividualDetails individual = null;
        CorporateDetails corporate = null;
        if (c instanceof IndividualCustomer i) {
            individual = new IndividualDetails(i.getPrefix1(), i.getPrefix2(), i.getPrefix3(), i.getFirstName(),
                    i.getMiddleName(), i.getLastName(), i.getContactInfo(), i.getCorrespondenceAddress(),
                    i.getPermanentAddress(), i.getSameAsCorrespondenceAddress(), i.getLanguage(), i.getCommunicationMode(),
                    i.getGender(), i.getNationalId(), i.getBirthPlace(), i.getBirthCountry(), i.getDateOfBirth(),
                    i.getMothersMaidenName(), i.getResidentStatus(), i.getPassport(), i.getKycDetails(), i.getStaff(),
                    i.getMinor(), i.getGuardian());
        } else if (c instanceof CorporateCustomer k) {
            corporate = new CorporateDetails(k.getName(), k.getContactInfo(), k.getCorrespondenceAddress(),
                    k.getRegistrationAddress(), k.getSameAsCorrespondenceAddress(), k.getLanguage(),
                    k.getCommunicationMode(), k.getIncorporation(), k.getKyc());
        }
        return new CustomerDTO(c.getId(), c.getCustomerType(), c.getCustomerNumber(), c.getFullName(), c.getShortName(),
                c.getBranchCode(), c.getCustomerCategory(), c.getIsPrivateCustomer(), individual, corporate);
    }

    /** Next numeric CIF number (9 digits, zero padded). */
    private String nextCustomerNumber() {
        long next = repository.findTopByOrderByCustomerNumberDesc()
                .map(Customer::getCustomerNumber)
                .filter(n -> n.matches("\\d+"))
                .map(n -> Long.parseLong(n) + 1)
                .orElse(1L);
        return String.format("%0" + NUMBER_LENGTH + "d", next);
    }

    private static IndividualCustomer.Address copy(IndividualCustomer.Address a) {
        return a == null ? null : new IndividualCustomer.Address(a.getLine1(), a.getLine2(), a.getLine3(), a.getLine4(), a.getPincode(), a.getCountry());
    }

    private static CorporateCustomer.Address copy(CorporateCustomer.Address a) {
        return a == null ? null : new CorporateCustomer.Address(a.getAddressName(), a.getAddressCode(), a.getLine1(), a.getLine2(),
                a.getLine3(), a.getLine4(), a.getPincode(), a.getCountry());
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static String like(String s) {
        return "%" + s.trim().toLowerCase() + "%";
    }
}
