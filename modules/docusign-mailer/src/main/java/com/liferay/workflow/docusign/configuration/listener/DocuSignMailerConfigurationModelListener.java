package com.liferay.workflow.docusign.configuration.listener;

import com.liferay.portal.configuration.persistence.listener.ConfigurationModelListener;
import com.liferay.workflow.docusign.configuration.DocuSignMailerConfiguration;
import com.liferay.workflow.extensions.common.configuration.persistence.listener.BaseConfigurationModelListener;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
        immediate = true,
        property = "model.class.name=" + DocuSignMailerConfiguration.PID,
        service = ConfigurationModelListener.class
)
public class DocuSignMailerConfigurationModelListener extends BaseConfigurationModelListener<DocuSignMailerConfiguration> {

    @Reference
    private ConfigurationAdmin _configurationAdmin;

    @Override
    protected ConfigurationAdmin getConfigurationAdmin() {
        return _configurationAdmin;
    }

    @Override
    protected Class<DocuSignMailerConfiguration> getConfigurationClass() {
        return DocuSignMailerConfiguration.class;
    }
}

