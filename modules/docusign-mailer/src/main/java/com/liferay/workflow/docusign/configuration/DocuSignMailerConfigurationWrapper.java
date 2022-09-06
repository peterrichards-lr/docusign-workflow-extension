package com.liferay.workflow.docusign.configuration;

import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.workflow.extensions.common.configuration.BaseActionExecutorConfigurationWrapper;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

import java.util.Map;
import java.util.stream.Collectors;

@Component(
        configurationPid = DocuSignMailerConfiguration.PID,
        immediate = true, service = DocuSignMailerConfigurationWrapper.class
)
public class DocuSignMailerConfigurationWrapper extends BaseActionExecutorConfigurationWrapper<DocuSignMailerConfiguration> {
    public long getFolderId() {
        return getConfiguration().folderId();
    }

    @Activate
    @Modified
    protected void activate(final Map<String, Object> properties) {
        _log.trace("Activating {} : {}", getClass().getSimpleName(), properties.keySet().stream().map(key -> key + "=" + properties.get(key).toString()).collect(Collectors.joining(", ", "{", "}")));
        final DocuSignMailerConfiguration configuration = ConfigurableUtil.createConfigurable(
                DocuSignMailerConfiguration.class, properties);

        super.setConfiguration(configuration);
    }
}
