import React, { useMemo, useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Button, Header, InboxSearchComposer, Loader } from "@egovernments/digit-ui-react-components";
import { useHistory, useLocation } from "react-router-dom";
import { myCampaignConfig } from "../../configs/myCampaignConfig";

/**
 * The `MyCampaign` function is a React component that displays a header with a campaign search title
 * and an inbox search composer with tabs for different configurations.
 * @returns The `MyCampaign` component is returning a React fragment containing a Header component with
 * a title fetched using the `useTranslation` hook, and a div with a className of
 * "inbox-search-wrapper" that contains an `InboxSearchComposer` component. The `InboxSearchComposer`
 * component is being passed props such as `configs`, `showTab`, `tabData`, and `onTabChange
 */
const MyCampaign = () => {
  const { t } = useTranslation();
  const location = useLocation();
  const history = useHistory();
  const moduleName = Digit?.Utils?.getConfigModuleName() || "commonCampaignUiConfig";
  const tenant = Digit.ULBService.getStateId();
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const [config, setConfig] = useState(myCampaignConfig?.myCampaignConfig?.[0]);
  const [tabData, setTabData] = useState(
    myCampaignConfig?.myCampaignConfig?.map((configItem, index) => ({ key: index, label: configItem.label, active: index === 0 ? true : false }))
  );

  const onTabChange = (n) => {
    setTabData((prev) => prev.map((i, c) => ({ ...i, active: c === n ? true : false })));
    setConfig(myCampaignConfig?.myCampaignConfig?.[n]);
  };

  useEffect(() => {
    window.Digit.SessionStorage.del("HCM_CAMPAIGN_MANAGER_FORM_DATA");
    window.Digit.SessionStorage.del("HCM_CAMPAIGN_MANAGER_UPLOAD_ID");
  }, []);

  const onClickRow = ({ original: row }) => {
    const currentTab = tabData?.find((i) => i?.active === true)?.label;
    switch (currentTab) {
      case "CAMPAIGN_ONGOING":
        history.push(`/${window.contextPath}/employee/campaign/setup-campaign?id=${row.id}&preview=${true}&action=${false}`);
        break;
      case "CAMPAIGN_COMPLETED":
        history.push(`/${window.contextPath}/employee/campaign/setup-campaign?id=${row.id}&preview=${true}&action=${false}`);
        break;
      case "CAMPAIGN_UPCOMING":
        history.push(`/${window.contextPath}/employee/campaign/setup-campaign?id=${row.id}&preview=${true}&action=${false}`);
        break;
      case "CAMPAIGN_DRAFTS":
        history.push(`/${window.contextPath}/employee/campaign/setup-campaign?id=${row.id}&draft=${true}&fetchBoundary=${true}`);
        break;
      case "CAMPAIGN_FAILED":
        history.push(`/${window.contextPath}/employee/campaign/setup-campaign?id=${row.id}&preview=${true}&action=${false}`);
        break;
      default:
        break;
    }
  };

  return (
    <React.Fragment>
      <Header styles={{ fontSize: "32px" }}>{t("CAMPAIGN_SEARCH_TITLE")}</Header>
      <div className="inbox-search-wrapper">
        <InboxSearchComposer
          configs={config}
          showTab={true}
          tabData={tabData}
          onTabChange={onTabChange}
          additionalConfig={{
            resultsTable: {
              onClickRow,
            },
          }}
        ></InboxSearchComposer>
      </div>
    </React.Fragment>
  );
};

export default MyCampaign;
