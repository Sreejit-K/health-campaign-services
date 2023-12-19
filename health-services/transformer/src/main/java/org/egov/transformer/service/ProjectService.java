package org.egov.transformer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import digit.models.coremodels.mdms.MasterDetail;
import digit.models.coremodels.mdms.MdmsCriteria;
import digit.models.coremodels.mdms.MdmsCriteriaReq;
import digit.models.coremodels.mdms.ModuleDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.common.models.project.BeneficiaryBulkResponse;
import org.egov.common.models.project.BeneficiarySearchRequest;
import org.egov.common.models.project.Project;
import org.egov.common.models.project.ProjectBeneficiary;
import org.egov.common.models.project.ProjectBeneficiarySearch;
import org.egov.common.models.project.ProjectRequest;
import org.egov.common.models.project.ProjectResponse;
import org.egov.tracer.model.CustomException;
import org.egov.transformer.Constants;
import org.egov.transformer.boundary.BoundaryNode;
import org.egov.transformer.boundary.BoundaryTree;
import org.egov.transformer.config.TransformerProperties;
import org.egov.transformer.http.client.ServiceRequestClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.egov.transformer.Constants.INTERNAL_SERVER_ERROR;
import static org.egov.transformer.Constants.MDMS_RESPONSE;
import static org.egov.transformer.Constants.PROJECT_TYPES;

@Component
@Slf4j
public class ProjectService {

    private final TransformerProperties transformerProperties;

    private final ServiceRequestClient serviceRequestClient;

    private final ObjectMapper objectMapper;

    private final BoundaryService boundaryService;

    private final MdmsService mdmsService;

    private final HouseholdService householdService;

//    private static final Map<String, Project> projectMap = new ConcurrentHashMap<>();

    public ProjectService(TransformerProperties transformerProperties,
                          ServiceRequestClient serviceRequestClient,
                          ObjectMapper objectMapper, BoundaryService boundaryService, MdmsService mdmsService, HouseholdService householdService) {
        this.transformerProperties = transformerProperties;
        this.serviceRequestClient = serviceRequestClient;
        this.objectMapper = objectMapper;
        this.boundaryService = boundaryService;
        this.mdmsService = mdmsService;
        this.householdService = householdService;
    }

    public Project getProject(String projectId, String tenantId) {
        List<Project> projects = searchProject(projectId, tenantId);
        Project project = null;
        if (!projects.isEmpty()) {
            project = projects.get(0);
        }
        return project;
    }

    public Project getProjectByName(String projectName, String tenantId) {
        List<Project> projects = searchProjectByName(projectName, tenantId);
        Project project = null;
        if (!projects.isEmpty()) {
            project = projects.get(0);
        }
        return project;
    }

    public Map<String, String> getBoundaryLabelToNameMapByProjectId(String projectId, String tenantId) {
        Project project = getProject(projectId, tenantId);
        String locationCode = project.getAddress().getBoundary();
        return getBoundaryLabelToNameMap(locationCode, tenantId);
    }

    public Map<String, String> getBoundaryLabelToNameMap(String locationCode, String tenantId) {
        BoundaryTree locationTree = boundaryService.getBoundary(locationCode, "ADMIN",
                tenantId);
        if (locationTree == null) {
            log.info("could not fetch location tree for code {}", locationCode);
            return new HashMap<>();
        }
        List<BoundaryNode> parentNodes = locationTree.getParentNodes();
        Map<String, String> resultMap = parentNodes.stream().collect(Collectors
                .toMap(BoundaryNode::getLabel, BoundaryNode::getName));
        resultMap.put(locationTree.getBoundaryNode().getLabel(), locationTree.getBoundaryNode().getName());
        return resultMap;
    }

    private List<Project> searchProjectByName(String projectName, String tenantId) {

        ProjectRequest request = ProjectRequest.builder()
                .requestInfo(RequestInfo.builder().
                        userInfo(User.builder()
                                .uuid("transformer-uuid")
                                .build())
                        .build())
                .projects(Collections.singletonList(Project.builder().name(projectName).tenantId(tenantId).build()))
                .build();

        try {
            log.info(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        ProjectResponse response;
        try {
            StringBuilder uri = new StringBuilder();
            uri.append(transformerProperties.getProjectHost())
                    .append(transformerProperties.getProjectSearchUrl())
                    .append("?limit=").append(transformerProperties.getSearchApiLimit())
                    .append("&offset=0")
                    .append("&tenantId=").append(tenantId);
            response = serviceRequestClient.fetchResult(uri,
                    request,
                    ProjectResponse.class);
        } catch (Exception e) {
            log.error("error while fetching project list {}", ExceptionUtils.getStackTrace(e));
            throw new CustomException("PROJECT_FETCH_ERROR",
                    "error while fetching project details for name: " + projectName);
        }
        return response.getProject();
    }

    private List<Project> searchProject(String projectId, String tenantId) {

        ProjectRequest request = ProjectRequest.builder()
                .requestInfo(RequestInfo.builder().
                userInfo(User.builder()
                        .uuid("transformer-uuid")
                        .build())
                .build())
                .projects(Collections.singletonList(Project.builder().id(projectId).tenantId(tenantId).build()))
                .build();

        ProjectResponse response;
        try {
            StringBuilder uri = new StringBuilder();
            uri.append(transformerProperties.getProjectHost())
                    .append(transformerProperties.getProjectSearchUrl())
                    .append("?limit=").append(transformerProperties.getSearchApiLimit())
                    .append("&offset=0")
                    .append("&tenantId=").append(tenantId);
            response = serviceRequestClient.fetchResult(uri,
                    request,
                    ProjectResponse.class);
        } catch (Exception e) {
            log.error("error while fetching project list {}", ExceptionUtils.getStackTrace(e));
            throw new CustomException("PROJECT_FETCH_ERROR",
                    "error while fetching project details for id: " + projectId);
        }
        return response.getProject();
    }

    public List<ProjectBeneficiary> searchBeneficiary(String projectBeneficiaryClientRefId, String tenantId) {
        BeneficiarySearchRequest request = BeneficiarySearchRequest.builder()
                .requestInfo(RequestInfo.builder().
                        userInfo(User.builder()
                                .uuid("transformer-uuid")
                                .build())
                        .build())
                .projectBeneficiary( ProjectBeneficiarySearch.builder().
                        clientReferenceId(Collections.singletonList(projectBeneficiaryClientRefId)).build())
                .build();
        BeneficiaryBulkResponse response;
        try {
            StringBuilder uri = new StringBuilder();
            uri.append(transformerProperties.getProjectHost())
                    .append(transformerProperties.getProjectBeneficiarySearchUrl())
                    .append("?limit=").append(transformerProperties.getSearchApiLimit())
                    .append("&offset=0")
                    .append("&tenantId=").append(tenantId);
            response = serviceRequestClient.fetchResult(uri,
                    request,
                    BeneficiaryBulkResponse.class);
        } catch (Exception e) {
            log.error("error while fetching beneficiary {}", ExceptionUtils.getStackTrace(e));
            throw new CustomException("PROJECT_BENEFICIARY_FETCH_ERROR",
                    "error while fetching beneficiary details for id: " + projectBeneficiaryClientRefId);
        }
        return response.getProjectBeneficiaries();
    }

    public List<String> getProducts(String tenantId, String projectTypeId) {
        String filter = "$[?(@.id == '" + projectTypeId + "')].resources.*.productVariantId";

        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().uuid("transformer-uuid").build())
                .build();

        JsonNode response = fetchMdmsResponse(requestInfo, tenantId, PROJECT_TYPES,
                transformerProperties.getMdmsModule(), filter);
        return convertToProjectTypeList(response);
    }

    private JsonNode fetchMdmsResponse(RequestInfo requestInfo, String tenantId, String name,
                                       String moduleName, String filter) {
        MdmsCriteriaReq serviceRegistry = getMdmsRequest(requestInfo, tenantId, name, moduleName, filter);
        try {
            return mdmsService.fetchConfig(serviceRegistry, JsonNode.class).get(MDMS_RESPONSE);
        } catch (Exception e) {
            throw new CustomException(INTERNAL_SERVER_ERROR, "Error while fetching mdms config");
        }
    }

    public JsonNode fetchBoundaryData(String tenantId,String filter) {
        List<JsonNode> projectTypes = new ArrayList<>();
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().uuid("transformer-uuid").build())
                .build();
        try {
            JsonNode response = fetchMdmsResponse(requestInfo, tenantId, PROJECT_TYPES,
                    transformerProperties.getMdmsModule(), filter);
            projectTypes = convertToProjectTypeJsonNodeList(response);
            //todo configure this id
            JsonNode requiredProjectType = projectTypes.stream().filter(projectType -> projectType.get("id").asText().equals("dbd45c31-de9e-4e62-a9b6-abb818928fd1")).findFirst().get();
            return requiredProjectType.get(Constants.BOUNDARY_DATA);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private List<String> convertToProjectTypeList(JsonNode jsonNode) {
        JsonNode projectTypesNode = jsonNode.get(transformerProperties.getMdmsModule()).withArray(PROJECT_TYPES);
        return new ObjectMapper().convertValue(projectTypesNode, new TypeReference<List<String>>() {
        });
    }

    private List<JsonNode> convertToProjectTypeJsonNodeList(JsonNode jsonNode) throws IOException {
        JsonNode projectTypesNode = jsonNode.get(transformerProperties.getMdmsModule()).withArray(PROJECT_TYPES);
        return objectMapper.readValue(projectTypesNode.traverse(), new TypeReference<List<JsonNode>>() {
        });
    }
    private MdmsCriteriaReq getMdmsRequest(RequestInfo requestInfo, String tenantId, String masterName,
                                           String moduleName, String filter) {
        MasterDetail masterDetail = new MasterDetail();
        masterDetail.setName(masterName);
        if(filter!=null && !filter.isEmpty()) {
            masterDetail.setFilter(filter);
        }
        List<MasterDetail> masterDetailList = new ArrayList<>();
        masterDetailList.add(masterDetail);
        ModuleDetail moduleDetail = new ModuleDetail();
        moduleDetail.setMasterDetails(masterDetailList);
        moduleDetail.setModuleName(moduleName);
        List<ModuleDetail> moduleDetailList = new ArrayList<>();
        moduleDetailList.add(moduleDetail);
        MdmsCriteria mdmsCriteria = new MdmsCriteria();
        mdmsCriteria.setTenantId(tenantId.split("\\.")[0]);
        mdmsCriteria.setModuleDetails(moduleDetailList);
        MdmsCriteriaReq mdmsCriteriaReq = new MdmsCriteriaReq();
        mdmsCriteriaReq.setMdmsCriteria(mdmsCriteria);
        mdmsCriteriaReq.setRequestInfo(requestInfo);
        return mdmsCriteriaReq;
    }
}
