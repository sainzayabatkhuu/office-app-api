package com.sol.office.report.service;

import com.sol.office.common.api.PageResponse;
import com.sol.office.common.exception.BusinessException;
import com.sol.office.common.exception.ConflictException;
import com.sol.office.common.exception.NotFoundException;
import com.sol.office.common.security.CurrentUser;
import com.sol.office.report.client.IdentityClient;
import com.sol.office.report.domain.ReportCategory;
import com.sol.office.report.domain.ReportDefinition;
import com.sol.office.report.domain.ReportParameter;
import com.sol.office.report.domain.ReportPermission;
import com.sol.office.report.dto.ReportDtos.CategoryRequest;
import com.sol.office.report.dto.ReportDtos.CategoryResponse;
import com.sol.office.report.dto.ReportDtos.ParameterUpdate;
import com.sol.office.report.dto.ReportDtos.PermissionRequest;
import com.sol.office.report.dto.ReportDtos.PermissionResponse;
import com.sol.office.report.dto.ReportDtos.ReportCreateRequest;
import com.sol.office.report.dto.ReportDtos.ReportDetail;
import com.sol.office.report.dto.ReportDtos.ReportMetadataRequest;
import com.sol.office.report.dto.ReportDtos.ReportSummary;
import com.sol.office.report.engine.JasperReportEngine;
import com.sol.office.report.engine.ParameterConverter;
import com.sol.office.report.engine.SystemParameters;
import com.sol.office.report.engine.TemplateParameter;
import com.sol.office.report.engine.TemplateStore;
import com.sol.office.report.engine.TemplateStore.StoredTemplate;
import com.sol.office.report.mapper.ReportMapper;
import com.sol.office.report.repository.ReportCategoryRepository;
import com.sol.office.report.repository.ReportDefinitionRepository;
import com.sol.office.report.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Report administration: templates, metadata, parameters, permissions and categories (REPORT_MANAGE).
 */
@Service
@RequiredArgsConstructor
public class ReportCatalogService {

    /** Granted on every new report so a freshly uploaded report is runnable by an administrator. */
    static final String DEFAULT_ROLE = "ADMIN";

    private final ReportDefinitionRepository definitionRepository;
    private final ReportCategoryRepository categoryRepository;
    private final TemplateStore templateStore;
    private final JasperReportEngine engine;
    private final FileStorage storage;
    private final IdentityClient identityClient;
    private final ReportMapper mapper;

    // ---- user-facing catalog

    /** Reports the current user may run. */
    @Transactional(readOnly = true)
    public List<ReportSummary> listAvailable(CurrentUser user, Long categoryId, String q) {
        if (user.roles().isEmpty()) {
            return List.of();
        }
        return definitionRepository.findAvailable(user.roles(), user.branchCode(), categoryId, StringUtils.hasText(q) ? q : null)
                .stream().map(mapper::toSummary).toList();
    }

    /** Report with the parameters the user has to fill in (server-filled ones are left out). */
    @Transactional(readOnly = true)
    public ReportDetail getForUser(CurrentUser user, Long id) {
        ReportDefinition report = definitionRepository.findWithPermissionsById(id)
                .filter(ReportDefinition::isActive)
                .orElseThrow(() -> NotFoundException.of("Report", id));
        if (ReportAccess.applicable(report, user).isEmpty()) {
            throw NotFoundException.of("Report", id);
        }
        System.out.println(report.getParameters());
        var parameters = report.getParameters().stream()
                .filter(p -> !p.isSystemParam())
                .filter(p -> user.multiBranchAccess() || !SystemParameters.BRANCH.equals(p.getInputType()))
                .map(mapper::toDto)
                .toList();
        return new ReportDetail(mapper.toSummary(report), parameters, null);
    }

    // ---- administration

    @Transactional(readOnly = true)
    public PageResponse<ReportSummary> search(String q, Boolean active, Pageable pageable) {
        return PageResponse.of(definitionRepository.search(StringUtils.hasText(q) ? q : null, active, pageable), mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public ReportDetail getForAdmin(Long id) {
        ReportDefinition report = load(id);
        return new ReportDetail(mapper.toSummary(report),
                report.getParameters().stream().map(mapper::toDto).toList(),
                report.getPermissions().stream().map(mapper::toResponse).toList());
    }

    @Transactional
    public ReportDetail create(ReportCreateRequest request, MultipartFile file, String mainTemplate) {
        if (definitionRepository.existsByCode(request.code())) {
            throw new ConflictException("Report code already exists: " + request.code());
        }
        CompiledTemplate compiled = storeAndCompile(request.code(), 1, file, mainTemplate);
        StoredTemplate template = compiled.template();
        try {
            ReportDefinition report = new ReportDefinition();
            report.setCode(request.code());
            applyMetadata(report, request.metadata());
            report.setTemplateVersion(1);
            report.setTemplateFileName(template.originalFileName());
            report.setTemplateStorageKey(template.mainKey());
            syncParameters(report, compiled.parameters());

            ReportPermission admin = new ReportPermission();
            admin.setReport(report);
            admin.setRoleCode(DEFAULT_ROLE);
            report.getPermissions().add(admin);

            definitionRepository.save(report);
            return getForAdmin(report.getId());
        } catch (RuntimeException e) {
            templateStore.delete(template);
            throw e;
        }
    }

    @Transactional
    public ReportDetail updateMetadata(Long id, ReportMetadataRequest request) {
        ReportDefinition report = load(id);
        applyMetadata(report, request);
        return getForAdmin(id);
    }

    /** Uploads a new template version. Parameter settings are kept for parameters that still exist. */
    @Transactional
    public ReportDetail uploadTemplate(Long id, MultipartFile file, String mainTemplate) {
        ReportDefinition report = load(id);
        int version = report.getTemplateVersion() + 1;
        CompiledTemplate compiled = storeAndCompile(report.getCode(), version, file, mainTemplate);
        report.setTemplateVersion(version);
        report.setTemplateFileName(compiled.template().originalFileName());
        report.setTemplateStorageKey(compiled.template().mainKey());
        syncParameters(report, compiled.parameters());
        return getForAdmin(id);
    }

    @Transactional(readOnly = true)
    public TemplateFile downloadTemplate(Long id) {
        ReportDefinition report = load(id);
        return new TemplateFile(storage.load(report.getTemplateStorageKey()), report.getTemplateFileName());
    }

    public record TemplateFile(Resource resource, String fileName) {
    }

    @Transactional
    public ReportDetail updateParameters(Long id, List<ParameterUpdate> updates) {
        ReportDefinition report = load(id);
        Map<String, ReportParameter> byName = report.getParameters().stream()
                .collect(Collectors.toMap(ReportParameter::getName, p -> p));
        for (ParameterUpdate u : updates) {
            ReportParameter p = byName.get(u.name());
            if (p == null) {
                throw new BusinessException("UNKNOWN_PARAMETER", "Template has no parameter '" + u.name() + "'");
            }
            if (StringUtils.hasText(u.defaultValue())) {
                ParameterConverter.convert(u.name(), u.defaultValue(), p.getJavaType());
            }
            p.setLabel(u.label());
            p.setRequired(u.required());
            p.setDefaultValue(StringUtils.hasText(u.defaultValue()) ? u.defaultValue() : null);
            p.setSortOrder(u.sortOrder());
        }
        report.getParameters().sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));
        return getForAdmin(id);
    }

    @Transactional
    public PermissionResponse addPermission(Long id, PermissionRequest request) {
        ReportDefinition report = load(id);
        String branch = StringUtils.hasText(request.branchCode()) ? request.branchCode() : null;
        boolean duplicate = report.getPermissions().stream()
                .anyMatch(p -> p.getRoleCode().equals(request.roleCode()) && Objects.equals(p.getBranchCode(), branch));
        if (duplicate) {
            throw new ConflictException("Permission already exists");
        }
        Set<String> knownRoles = identityClient.listRoles().stream()
                .map(IdentityClient.RoleSummary::code).collect(Collectors.toSet());
        if (!knownRoles.contains(request.roleCode())) {
            throw new BusinessException("UNKNOWN_ROLE", "Unknown role: " + request.roleCode());
        }
        ReportPermission permission = new ReportPermission();
        permission.setReport(report);
        permission.setRoleCode(request.roleCode());
        permission.setBranchCode(branch);
        permission.setExecutionMode(request.executionMode());
        report.getPermissions().add(permission);
        definitionRepository.flush();
        return mapper.toResponse(permission);
    }

    @Transactional
    public void removePermission(Long id, Long permissionId) {
        ReportDefinition report = load(id);
        if (!report.getPermissions().removeIf(p -> p.getId().equals(permissionId))) {
            throw NotFoundException.of("Permission", permissionId);
        }
    }

    // ---- categories

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAllByOrderBySortOrderAscNameAsc().stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByCode(request.code())) {
            throw new ConflictException("Category code already exists: " + request.code());
        }
        ReportCategory category = new ReportCategory();
        category.setCode(request.code());
        category.setName(request.name());
        category.setSortOrder(request.sortOrder());
        return mapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        ReportCategory category = categoryRepository.findById(id).orElseThrow(() -> NotFoundException.of("Category", id));
        category.setName(request.name());
        category.setSortOrder(request.sortOrder());
        return mapper.toResponse(category);
    }

    // ---- helpers

    private record CompiledTemplate(StoredTemplate template, List<TemplateParameter> parameters) {
    }

    /** Stores the upload and compiles it; nothing is kept if the template is invalid. */
    private CompiledTemplate storeAndCompile(String code, int version, MultipartFile file, String mainTemplate) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("TEMPLATE_REQUIRED", "Template file is required");
        }
        StoredTemplate template = templateStore.store(code, version, file, mainTemplate);
        try {
            return new CompiledTemplate(template, engine.compileTemplate(template.mainKey()));
        } catch (RuntimeException e) {
            templateStore.delete(template);
            throw e;
        }
    }

    private void applyMetadata(ReportDefinition report, ReportMetadataRequest m) {
        if (!m.allowedFormats().contains(m.defaultFormat())) {
            throw new BusinessException("INVALID_FORMATS", "Default format must be one of the allowed formats");
        }
        report.setTitle(m.title());
        report.setDescription(m.description());
        report.setCategory(m.categoryId() == null ? null
                : categoryRepository.findById(m.categoryId()).orElseThrow(() -> NotFoundException.of("Category", m.categoryId())));
        report.setAllowedFormatSet(m.allowedFormats());
        report.setDefaultFormat(m.defaultFormat());
        report.setExecutionMode(m.executionMode());
        report.setActive(m.active());
    }

    /** Aligns stored parameters with the template: keeps admin edits, adds new, drops removed ones. */
    private void syncParameters(ReportDefinition report, List<TemplateParameter> fromTemplate) {
        Map<String, ReportParameter> existing = new HashMap<>();
        report.getParameters().forEach(p -> existing.put(p.getName(), p));
        Set<String> names = fromTemplate.stream().map(TemplateParameter::name).collect(Collectors.toSet());
        report.getParameters().removeIf(p -> !names.contains(p.getName()));

        int order = 0;
        for (TemplateParameter tp : fromTemplate) {
            order += 10;
            ReportParameter p = existing.get(tp.name());
            if (p == null) {
                p = new ReportParameter();
                p.setReport(report);
                p.setName(tp.name());
                p.setLabel(StringUtils.hasText(tp.description()) ? tp.description() : tp.name());
                p.setSortOrder(order);
                p.setDefaultValue(literalDefault(tp.defaultExpression()));
                report.getParameters().add(p);
            }
            p.setJavaType(tp.javaType());
            p.setInputType(ParameterConverter.inputTypeFor(tp.javaType()));
            p.setSystemParam(SystemParameters.SERVER_ONLY.contains(tp.name()));
        }
        report.getParameters().sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));
    }

    /** Uses a template default only when it is a plain literal like {@code "000"} or {@code 10}. */
    private static String literalDefault(String expression) {
        if (expression == null) {
            return null;
        }
        String e = expression.trim();
        if (e.matches("\"[^\"]*\"")) {
            return e.substring(1, e.length() - 1);
        }
        return e.matches("-?\\d+(\\.\\d+)?[LlDdFf]?") ? e.replaceAll("[LlDdFf]$", "") : null;
    }

    private ReportDefinition load(Long id) {
        return definitionRepository.findWithParametersById(id).orElseThrow(() -> NotFoundException.of("Report", id));
    }
}
