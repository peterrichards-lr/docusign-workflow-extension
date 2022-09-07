package com.liferay.workflow.docusign.configuration;

import aQute.bnd.annotation.metatype.Meta;
import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;
import com.liferay.workflow.docusign.constants.DocuSignMailerConstants;
import com.liferay.workflow.extensions.common.configuration.BaseActionExecutorConfiguration;
import com.liferay.workflow.extensions.common.constants.WorkflowExtensionsConstants;

@ExtendedObjectClassDefinition(
        category = "workflow", scope = ExtendedObjectClassDefinition.Scope.GROUP,
        factoryInstanceLabelAttribute = WorkflowExtensionsConstants.CONFIG_WORKFLOW_NODE_ID
)
@Meta.OCD(
        factory = true,
        id = DocuSignMailerConfiguration.PID,
        localization = "content/Language", name = "config-docusign-mailer-name",
        description = "config-docusign-mailer-description"
)
public interface DocuSignMailerConfiguration extends BaseActionExecutorConfiguration {
    String PID = "com.liferay.workflow.docusign.configuration.DocuSignMailerConfiguration";

    @Meta.AD(
            deflt = WorkflowExtensionsConstants.CONFIG_WORKFLOW_NODE_ID_ACTION_DEFAULT,
            description = "config-workflow-node-identifier-description",
            id = WorkflowExtensionsConstants.CONFIG_WORKFLOW_NODE_ID,
            name = "config-workflow-node-identifier-name",
            required = false
    )
    String identifier();

    @Meta.AD(
            deflt = WorkflowExtensionsConstants.CONFIG_ENABLE_DEFAULT,
            description = "config-enable-description",
            name = "config-enable-name",
            required = false
    )
    boolean enable();

    @Meta.AD(
            deflt = WorkflowExtensionsConstants.CONFIG_UPDATE_WORKFLOW_STATUS_ON_SUCCESS_DEFAULT,
            description = "config-update-workflow-status-on-success-description",
            name = "config-update-workflow-status-on-success-name",
            required = false
    )
    boolean updateWorkflowStatusOnSuccess();

    @Meta.AD(
            deflt = WorkflowExtensionsConstants.CONFIG_SUCCESS_WORKFLOW_STATUS_DEFAULT,
            description = "config-success-workflow-status-description",
            name = "config-success-workflow-status-name",
            required = false
    )
    String successWorkflowStatus();

    @Meta.AD(
            deflt = WorkflowExtensionsConstants.CONFIG_UPDATE_WORKFLOW_STATUS_ON_EXCEPTION_DEFAULT,
            description = "config-update-workflow-status-on-exception-description",
            name = "config-update-workflow-status-on-exception-name",
            required = false
    )
    boolean updateWorkflowStatusOnException();

    @Meta.AD(
            deflt = WorkflowExtensionsConstants.CONFIG_EXCEPTION_WORKFLOW_STATUS_DEFAULT,
            description = "config-exception-workflow-status-description",
            name = "config-exception-workflow-status-name",
            required = false
    )
    String exceptionWorkflowStatus();

    @Meta.AD(
            deflt = DocuSignMailerConstants.CONFIG_FOLDER_IDENTIFIER_DEFAULT,
            description = "config-folder-identifier-description",
            name = "config-folder-identifier-name",
            required = false
    )
    long folderId();

    @Meta.AD(
            deflt = DocuSignMailerConstants.CONFIG_USE_WORKFLOW_CONTEXT_KEY_FOR_DOCUMENT_DEFAULT,
            description = "config-use-workflow-context-key-for-document-description",
            name = "config-use-workflow-context-key-for-document-name",
            required = false
    )
    boolean useWorkflowContextKeyForDocument();

    @Meta.AD(
            deflt = DocuSignMailerConstants.CONFIG_DOCUMENT_WORKFLOW_CONTEXT_KEY_DEFAULT,
            description = "config-document-workflow-context-key-description",
            name = "config-document-workflow-context-key-name",
            required = false
    )
    String documentWorkflowContextKey();

    @Meta.AD(
            deflt = DocuSignMailerConstants.CONFIG_DOCUMENT_DEFAULT,
            description = "config-document-description",
            name = "config-document-name",
            required = false
    )
    String document();

    @Meta.AD(
            deflt = DocuSignMailerConstants.CONFIG_DOCUMENT_LOOKUP_TYPE_DEFAULT,
            description = "config-document-lookup-type-description",
            name = "config-document-lookup-type-name",
            required = false
    )
    String documentLookupType();

    @Meta.AD(
            deflt = DocuSignMailerConstants.CONFIG_USE_WORKFLOW_CONTEXT_KEY_FOR_ENVELOPE_NAME_DEFAULT,
            description = "config-use-workflow-context-key-for-envelope-name-description",
            name = "config-use-workflow-context-key-for-envelope-name-name",
            required = false
    )
    boolean useWorkflowContextKeyForEnvelopeName();

    @Meta.AD(
            deflt = DocuSignMailerConstants.CONFIG_ENVELOPE_NAME_WORKFLOW_CONTEXT_KEY_DEFAULT,
            description = "config-envelope-name-workflow-context-key-description",
            name = "config-envelope-name-workflow-context-key-name",
            required = false
    )
    String envelopeNameWorkflowContextKey();

    @Meta.AD(
            deflt = DocuSignMailerConstants.CONFIG_ENVELOPE_NAME_DEFAULT,
            description = "config-envelope-name-description",
            name = "config-envelope-name-name",
            required = false
    )
    String envelopeName();

    @Meta.AD(
            deflt = DocuSignMailerConstants.CONFIG_USE_WORKFLOW_CONTEXT_KEY_FOR_SENDER_EMAIL_ADDRESS_DEFAULT,
            description = "config-use-workflow-context-key-for-sender-email-address-description",
            name = "config-use-workflow-context-key-for-sender-email-address-name",
            required = false
    )
    boolean useWorkflowContextKeyForSenderEmailAddress();

    @Meta.AD(
            deflt = DocuSignMailerConstants.CONFIG_SENDER_EMAIL_ADDRESS_WORKFLOW_CONTEXT_KEY_DEFAULT,
            description = "config-sender-email-address-workflow-context-key-description",
            name = "config-sender-email-address-workflow-context-key-name",
            required = false
    )
    String senderEmailAddressWorkflowContextKey();

    @Meta.AD(
            deflt = DocuSignMailerConstants.CONFIG_SENDER_EMAIL_ADDRESS_DEFAULT,
            description = "config-sender-email-address-description",
            name = "config-sender-email-address-name",
            required = false
    )
    String senderEmailAddress();

    @Meta.AD(
            deflt = DocuSignMailerConstants.CONFIG_RECIPIENT_EMAIL_ADDRESS_WORKFLOW_CONTEXT_KEY_DEFAULT,
            description = "config-recipient-email-address-workflow-context-key-description",
            name = "config-recipient-email-address-workflow-context-key-name",
            required = false
    )
    String recipientEmailAddressWorkflowContextKey();

    @Meta.AD(
            deflt = DocuSignMailerConstants.CONFIG_RECIPIENT_FULL_NAME_WORKFLOW_CONTEXT_KEY_DEFAULT,
            description = "config-recipient-full-name-workflow-context-key-description",
            name = "config-recipient-full-name-workflow-context-key-name",
            required = false
    )
    String recipientFullNameWorkflowContextKey();

    @Meta.AD(
            deflt = DocuSignMailerConstants.CONFIG_EMAIL_SUBJECT_TEMPLATE_DEFAULT,
            description = "config-email-subject-template-description",
            name = "config-email-subject-template-name",
            required = false
    )
    String emailSubjectTemplate();

    @Meta.AD(
            deflt = DocuSignMailerConstants.CONFIG_EMAIL_BODY_TEMPLATE_DEFAULT,
            description = "config-email-body-template-description",
            name = "config-email-body-template-name",
            required = false
    )
    String emailBodyTemplate();
}
