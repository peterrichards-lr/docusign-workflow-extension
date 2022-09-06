package com.liferay.workflow.docusign.settings;

import com.liferay.workflow.docusign.configuration.DocuSignMailerConfiguration;
import com.liferay.workflow.docusign.configuration.DocuSignMailerConfigurationWrapper;
import com.liferay.workflow.extensions.common.settings.BaseSettingsHelper;
import org.osgi.service.component.annotations.*;

@Component(immediate = true, service = DocuSignMailerSettingsHelper.class)
public class DocuSignMailerSettingsHelperImpl extends BaseSettingsHelper<DocuSignMailerConfiguration, DocuSignMailerConfigurationWrapper> implements DocuSignMailerSettingsHelper {
    @Reference(
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY
    )
    protected void addDocuSignMailerConfigurationWrapper(
            final DocuSignMailerConfigurationWrapper
                    configurationWrapper) {
        _log.debug("Adding a DocuSign mailer configuration\n[{}]", configurationWrapper.toString());
        super.addConfigurationWrapper(configurationWrapper);
    }

    @SuppressWarnings("unused")
    protected void removeDocuSignMailerConfigurationWrapper(
            final DocuSignMailerConfigurationWrapper
                    configurationWrapper) {
        _log.debug("Removing a DocuSign mailer configuration\n[{}]", configurationWrapper.toString());
        super.removeConfigurationWrapper(configurationWrapper);
    }
}
