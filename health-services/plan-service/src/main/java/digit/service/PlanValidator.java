package digit.service;

import com.jayway.jsonpath.JsonPath;
import digit.repository.PlanConfigurationRepository;
import digit.repository.PlanRepository;
import digit.util.MdmsUtil;
import digit.web.models.*;
import org.egov.common.utils.MultiStateInstanceUtil;
import org.egov.tracer.model.CustomException;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

import static digit.config.ServiceConstants.*;

@Component
public class PlanValidator {

    private PlanRepository planRepository;

    private PlanConfigurationRepository planConfigurationRepository;

    private MdmsUtil mdmsUtil;

    private MultiStateInstanceUtil centralInstanceUtil;

    public PlanValidator(PlanRepository planRepository, PlanConfigurationRepository planConfigurationRepository, MdmsUtil mdmsUtil, MultiStateInstanceUtil centralInstanceUtil) {
        this.planRepository = planRepository;
        this.planConfigurationRepository = planConfigurationRepository;
        this.mdmsUtil = mdmsUtil;
        this.centralInstanceUtil = centralInstanceUtil;
    }

    /**
     * This method performs business validations on plan create requests
     * @param request
     */
    public void validatePlanCreate(PlanRequest request) {
        String rootTenantId = centralInstanceUtil.getStateLevelTenant(request.getPlan().getTenantId());
        Object mdmsData = mdmsUtil.fetchMdmsData(request.getRequestInfo(), rootTenantId);

        // Validate activities
        validateActivities(request);

        // Validate plan configuration existence
        validatePlanConfigurationExistence(request);

        // Validate resources
        validateResources(request);

        // Validate resource-activity linkage
        validateResourceActivityLinkage(request);

        // Validate target-activity linkage
        validateTargetActivityLinkage(request);

        // Validate dependencies
        validateActivityDependencies(request);

        // Validate Target's Metrics against MDMS
        validateTargetMetrics(request, mdmsData);

        // Validate Metric Detail's Unit against MDMS
        validateMetricDetailUnit(request, mdmsData);
    }

    /**
     * This validation method validates if the dependent activities are valid and if they form a cycle
     * @param request
     */
    private void validateActivityDependencies(PlanRequest request) {
        // Check if dependent activity codes are valid
        validateDependentActivityCodes(request);

        // Check if dependent activities form a cycle
        checkForCycleInActivityDependencies(request);
    }

    /**
     * This method checks if the activity dependencies form a cycle
     * @param request
     */
    private void checkForCycleInActivityDependencies(PlanRequest request) {
        Map<String, List<String>> activityCodeVsDependenciesMap = request.getPlan().getActivities().stream()
                .collect(Collectors.toMap(Activity::getCode,
                        activity -> CollectionUtils.isEmpty(activity.getDependencies()) ? List.of() : activity.getDependencies()));

        activityCodeVsDependenciesMap.keySet().forEach(activityCode -> {
            activityCodeVsDependenciesMap.get(activityCode).forEach(dependency -> {
                if(activityCodeVsDependenciesMap.get(dependency).contains(activityCode))
                    throw new CustomException(CYCLIC_ACTIVITY_DEPENDENCY_CODE, CYCLIC_ACTIVITY_DEPENDENCY_MESSAGE);
            });
        });
    }

    /**
     * This method validates if the dependent activity codes are valid
     * @param request
     */
    private void validateDependentActivityCodes(PlanRequest request) {
        // Collect all activity codes
        Set<String> activityCodes = request.getPlan().getActivities().stream()
                .map(Activity::getCode)
                .collect(Collectors.toSet());

        // Check if the dependent activity codes are valid
        request.getPlan().getActivities().forEach(activity -> {
            if(!CollectionUtils.isEmpty(activity.getDependencies())) {
                activity.getDependencies().forEach(dependency -> {
                    if(!activityCodes.contains(dependency))
                        throw new CustomException(INVALID_ACTIVITY_DEPENDENCY_CODE, INVALID_ACTIVITY_DEPENDENCY_MESSAGE);
                });
            }
        });
    }


    /**
     * This method validates the activities provided in the request
     * @param request
     */
    private void validateActivities(PlanRequest request) {
        // Collect all activity codes
        if(request.getPlan().getActivities() == null)
            throw new CustomException(ACTIVITIES_CANNOT_BE_NULL_CODE, ACTIVITIES_CANNOT_BE_NULL_MESSAGE);

        Set<String> activityCodes = request.getPlan().getActivities().stream()
                .map(Activity::getCode)
                .collect(Collectors.toSet());

        // If activity codes are not unique, throw an exception
        if(activityCodes.size() != request.getPlan().getActivities().size()) {
            throw new CustomException(DUPLICATE_ACTIVITY_CODES, DUPLICATE_ACTIVITY_CODES_MESSAGE);
        }

        // If execution plan id is not provided, providing activities is mandatory
        if(ObjectUtils.isEmpty(request.getPlan().getCampaignId())
                && CollectionUtils.isEmpty(request.getPlan().getActivities())) {
            throw new CustomException(PLAN_ACTIVITIES_MANDATORY_CODE, PLAN_ACTIVITIES_MANDATORY_MESSAGE);
        }

        // If execution plan id is provided, providing activities is not allowed
        if(!ObjectUtils.isEmpty(request.getPlan().getCampaignId())
                && !CollectionUtils.isEmpty(request.getPlan().getActivities())) {
            throw new CustomException(PLAN_ACTIVITIES_NOT_ALLOWED_CODE, PLAN_ACTIVITIES_NOT_ALLOWED_MESSAGE);
        }

        // Validate activity dates
        if(!CollectionUtils.isEmpty(request.getPlan().getActivities())) {
            request.getPlan().getActivities().forEach(activity -> {
                if(activity.getPlannedEndDate() < activity.getPlannedStartDate())
                    throw new CustomException(INVALID_ACTIVITY_DATES_CODE, INVALID_ACTIVITY_DATES_MESSAGE);
            });
        }
    }

    /**
     * This method validates if the plan configuration id provided in the request exists
     * @param request
     */
    private void validatePlanConfigurationExistence(PlanRequest request) {
        // If plan id provided is invalid, throw an exception
        if(!ObjectUtils.isEmpty(request.getPlan().getPlanConfigurationId()) && CollectionUtils.isEmpty(planConfigurationRepository.search(PlanConfigurationSearchCriteria.builder()
                .id(request.getPlan().getPlanConfigurationId())
                .tenantId(request.getPlan().getTenantId())
                .build()))) {
            throw new CustomException(INVALID_PLAN_CONFIG_ID_CODE, INVALID_PLAN_CONFIG_ID_MESSAGE);
        }
    }

    /**
     * This method validates the resources provided in the request
     * @param request
     */
    private void validateResources(PlanRequest request) {
        // If plan configuration id is not provided, providing resources is mandatory
        if(ObjectUtils.isEmpty(request.getPlan().getPlanConfigurationId())
                && CollectionUtils.isEmpty(request.getPlan().getResources())) {
            throw new CustomException(PLAN_RESOURCES_MANDATORY_CODE, PLAN_RESOURCES_MANDATORY_MESSAGE);
        }

        // If plan configuration id is provided, providing resources is not allowed
        if(!ObjectUtils.isEmpty(request.getPlan().getPlanConfigurationId())
                && !CollectionUtils.isEmpty(request.getPlan().getResources())) {
            throw new CustomException(PLAN_RESOURCES_NOT_ALLOWED_CODE, PLAN_RESOURCES_NOT_ALLOWED_MESSAGE);
        }

        // Validate resource type existence
        if(!CollectionUtils.isEmpty(request.getPlan().getResources())) {
            request.getPlan().getResources().forEach(resource -> {
                // Validate resource type existence
            });
        }
    }

    /**
     * This method validates the linkage between resources and activities
     * @param request
     */
    private void validateResourceActivityLinkage(PlanRequest request) {
        if(ObjectUtils.isEmpty(request.getPlan().getPlanConfigurationId())
                && !CollectionUtils.isEmpty(request.getPlan().getActivities())) {
            // Collect all activity codes
            Set<String> activityCodes = request.getPlan().getActivities().stream()
                    .map(Activity::getCode)
                    .collect(Collectors.toSet());

            // Validate resource-activity linkage
            request.getPlan().getResources().forEach(resource -> {
                if(!activityCodes.contains(resource.getActivityCode()))
                    throw new CustomException(INVALID_RESOURCE_ACTIVITY_LINKAGE_CODE, INVALID_RESOURCE_ACTIVITY_LINKAGE_MESSAGE);
            });
        }
    }

    /**
     * This method validates the linkage between targets and activities
     * @param request
     */
    private void validateTargetActivityLinkage(PlanRequest request) {
        if(!CollectionUtils.isEmpty(request.getPlan().getActivities())) {
            // Collect all activity codes
            Set<String> activityCodes = request.getPlan().getActivities().stream()
                    .map(Activity::getCode)
                    .collect(Collectors.toSet());

            // Validate target-activity linkage
            request.getPlan().getTargets().forEach(target -> {
                if(!activityCodes.contains(target.getActivityCode()))
                    throw new CustomException(INVALID_TARGET_ACTIVITY_LINKAGE_CODE, INVALID_TARGET_ACTIVITY_LINKAGE_MESSAGE);
            });
        }
    }

    /**
     * This method performs business validations on plan update requests
     * @param request
     */
    public void validatePlanUpdate(PlanRequest request) {
        // Validate plan existence
        validatePlanExistence(request);

        String rootTenantId = centralInstanceUtil.getStateLevelTenant(request.getPlan().getTenantId());
        Object mdmsData = mdmsUtil.fetchMdmsData(request.getRequestInfo(), rootTenantId);

        // Validate activities
        validateActivities(request);

        // Validate activities uuid uniqueness
        validateActivitiesUuidUniqueness(request);

        // Validate plan configuration existence
        validatePlanConfigurationExistence(request);

        // Validate resources
        validateResources(request);

        // Validate resource uuid uniqueness
        validateResourceUuidUniqueness(request);

        // Validate target uuid uniqueness
        validateTargetUuidUniqueness(request);

        // Validate resource-activity linkage
        validateResourceActivityLinkage(request);

        // Validate target-activity linkage
        validateTargetActivityLinkage(request);

        // Validate dependencies
        validateActivityDependencies(request);

        // Validate Target's Metrics against MDMS
        validateTargetMetrics(request, mdmsData);

        // Validate Metric Detail's Unit against MDMS
        validateMetricDetailUnit(request, mdmsData);

    }

    /**
     * Validates that all target UUIDs within the provided PlanRequest are unique.
     *
     * @param request the PlanRequest containing the targets to be validated
     * @throws CustomException if any target UUIDs are not unique
     */
    private void validateTargetUuidUniqueness(PlanRequest request) {
        // Collect all target UUIDs
        Set<String> targetUuids = request.getPlan().getTargets().stream()
                .map(Target::getId)
                .collect(Collectors.toSet());

        // If target UUIDs are not unique, throw an exception
        if (targetUuids.size() != request.getPlan().getTargets().size()) {
            throw new CustomException(DUPLICATE_TARGET_UUIDS_CODE, DUPLICATE_TARGET_UUIDS_MESSAGE);
        }
    }

    /**
     * Validates that all resource UUIDs within the provided PlanRequest are unique.
     *
     * @param request the PlanRequest containing the resources to be validated
     * @throws CustomException if any resource UUIDs are not unique
     */
    private void validateResourceUuidUniqueness(PlanRequest request) {
        // Collect all resource UUIDs
        Set<String> resourceUuids = request.getPlan().getResources().stream()
                .map(Resource::getId)
                .collect(Collectors.toSet());

        // If resource UUIDs are not unique, throw an exception
        if (resourceUuids.size() != request.getPlan().getResources().size()) {
            throw new CustomException(DUPLICATE_RESOURCE_UUIDS_CODE, DUPLICATE_RESOURCE_UUIDS_MESSAGE);
        }
    }

    /**
     * Validates that all activity UUIDs within the provided PlanRequest are unique.
     *
     * @param request the PlanRequest containing the activities to be validated
     * @throws CustomException if any activity UUIDs are not unique
     */
    private void validateActivitiesUuidUniqueness(PlanRequest request) {
        // Collect all activity UUIDs
        Set<String> activityUuids = request.getPlan().getActivities().stream()
                .map(Activity::getId)
                .collect(Collectors.toSet());

        // If activity UUIDs are not unique, throw an exception
        if (activityUuids.size() != request.getPlan().getActivities().size()) {
            throw new CustomException(DUPLICATE_ACTIVITY_UUIDS_CODE, DUPLICATE_ACTIVITY_UUIDS_MESSAGE);
        }
    }


    /**
     * This method validates if the plan id provided in the update request exists
     * @param request
     */
    private void validatePlanExistence(PlanRequest request) {
        // If plan id provided is invalid, throw an exception
        if(CollectionUtils.isEmpty(planRepository.search(PlanSearchCriteria.builder()
                .ids(Collections.singleton(request.getPlan().getId()))
                .build()))) {
            throw new CustomException(INVALID_PLAN_ID_CODE, INVALID_PLAN_ID_MESSAGE);
        }
    }

    /**
     * Validates the target metrics within the provided PlanRequest against MDMS data.
     *
     * This method checks each target metric in the plan to ensure it exists in the MDMS data.
     * If a metric is not found, it throws a CustomException.
     *
     * @param request the PlanRequest containing the plan and target metrics to be validated
     * @param mdmsData the MDMS data against which the target metrics are validated
     * @throws CustomException if there is an error reading the MDMS data using JsonPath
     *                         or if any target metric is not found in the MDMS data
     */
    public void validateTargetMetrics(PlanRequest request, Object mdmsData) {
        Plan plan = request.getPlan();
        final String jsonPathForMetric = "$." + MDMS_PLAN_MODULE_NAME + "." + MDMS_MASTER_METRIC + ".*.code";

        List<Object> metricListFromMDMS = null;
        System.out.println("Jsonpath -> " + jsonPathForMetric);
        try {
            metricListFromMDMS = JsonPath.read(mdmsData, jsonPathForMetric);
        } catch (Exception e) {
            throw new CustomException(JSONPATH_ERROR_CODE, JSONPATH_ERROR_MESSAGE);
        }
        HashSet<Object> metricSetFromMDMS = new HashSet<>(metricListFromMDMS);
        plan.getTargets().stream().forEach(target -> {
            if (!metricSetFromMDMS.contains(target.getMetric())) {
                throw new CustomException(METRIC_NOT_FOUND_IN_MDMS_CODE, METRIC_NOT_FOUND_IN_MDMS_MESSAGE);
            }
        });

    }

    /**
     * Validates the metric unit details within the provided PlanRequest against MDMS data.
     *
     * This method extracts metric details from the plan and checks if each metric unit
     * is present in the MDMS data. If a metric unit is not found, it throws a CustomException.
     *
     * @param request the PlanRequest containing the plan and metric details to be validated
     * @param mdmsData the MDMS data against which the metric units are validated
     * @throws CustomException if there is an error reading the MDMS data using JsonPath
     *                         or if any metric unit is not found in the MDMS data
     */
    public void validateMetricDetailUnit(PlanRequest request, Object mdmsData) {
        Plan plan = request.getPlan();

        List<MetricDetail> metricDetails = plan.getTargets().stream()
                .map(Target::getMetricDetail)
                .toList();

        List<Object> metricUnitListFromMDMS;
        final String jsonPathForMetricUnit = "$." + MDMS_PLAN_MODULE_NAME + "." + MDMS_MASTER_UOM + ".*.uomCode";
        try {
            metricUnitListFromMDMS = JsonPath.read(mdmsData, jsonPathForMetricUnit);
        } catch (Exception e) {
            throw new CustomException(JSONPATH_ERROR_CODE, JSONPATH_ERROR_MESSAGE);
        }

        HashSet<Object> metricUnitSetFromMDMS = new HashSet<>(metricUnitListFromMDMS);
        metricDetails.stream().forEach(metricDetail -> {
            if (!metricUnitSetFromMDMS.contains(metricDetail.getMetricUnit())) {
                throw new CustomException(METRIC_UNIT_NOT_FOUND_IN_MDMS_CODE, METRIC_UNIT_NOT_FOUND_IN_MDMS_MESSAGE);
            }
        });

    }

}
