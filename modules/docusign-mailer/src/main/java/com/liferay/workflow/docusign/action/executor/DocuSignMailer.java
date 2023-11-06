package com.liferay.workflow.docusign.action.executor;

import com.liferay.digital.signature.manager.DSEnvelopeManager;
import com.liferay.digital.signature.model.DSDocument;
import com.liferay.digital.signature.model.DSEnvelope;
import com.liferay.digital.signature.model.DSRecipient;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.util.Base64;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.kernel.workflow.WorkflowException;
import com.liferay.portal.kernel.workflow.WorkflowStatusManagerUtil;
import com.liferay.portal.workflow.kaleo.model.KaleoAction;
import com.liferay.portal.workflow.kaleo.runtime.ExecutionContext;
import com.liferay.portal.workflow.kaleo.runtime.action.executor.ActionExecutor;
import com.liferay.portal.workflow.kaleo.runtime.action.executor.ActionExecutorException;
import com.liferay.workflow.docusign.configuration.DocuSignMailerConfiguration;
import com.liferay.workflow.docusign.configuration.DocuSignMailerConfigurationWrapper;
import com.liferay.workflow.docusign.settings.DocuSignMailerSettingsHelper;
import com.liferay.workflow.extensions.common.action.executor.BaseWorkflowActionExecutor;
import com.liferay.workflow.extensions.common.context.WorkflowActionExecutionContext;
import com.liferay.workflow.extensions.common.context.service.WorkflowActionExecutionContextService;
import com.liferay.workflow.extensions.common.util.StringUtil;
import com.liferay.workflow.extensions.common.util.WorkflowExtensionsUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author peterrichards
 */
@Component(
        property = "com.liferay.portal.workflow.kaleo.runtime.action.executor.language=java",
        service = ActionExecutor.class,
        configurationPid = DocuSignMailerConfiguration.PID
)
public class DocuSignMailer extends BaseWorkflowActionExecutor<DocuSignMailerConfiguration, DocuSignMailerConfigurationWrapper, DocuSignMailerSettingsHelper> implements ActionExecutor {
    @Reference
    private DLAppLocalService dlAppLocalService;
    @Reference
    private DocuSignMailerSettingsHelper docuSignMailerSettingsHelper;
    @Reference
    private DSEnvelopeManager dsEnvelopeManager;
    @Reference
    private WorkflowActionExecutionContextService workflowActionExecutionContextService;

    private DSDocument buildDocument(final FileEntry fileEntry) throws ActionExecutorException {
        final byte[] fileEntryData = new byte[(int) fileEntry.getSize()];
        final InputStream inputStream;
        try {
            inputStream = fileEntry.getContentStream();
        } catch (PortalException e) {
            throw new ActionExecutorException("Unable to get input stream from file entry", e);
        }
        final DataInputStream dis = new DataInputStream(inputStream);

        try {
            dis.readFully(fileEntryData);
            return new DSDocument() {
                {
                    data = Base64.encode(fileEntryData);
                    dsDocumentId = String.valueOf(fileEntry.getFileEntryId());
                    name = fileEntry.getFileName();
                    fileExtension = fileEntry.getExtension();
                }
            };
        } catch (IOException e) {
            throw new ActionExecutorException("Unable to read content of stream", e);
        } finally {
            try {
                dis.close();
            } catch (IOException e) {
                _log.error("Failed to close stream", e);
            }
        }
    }

    private DSEnvelope buildEnvelope(final String envelopeName, final String subject, final String blurb, final List<DSRecipient> recipientList, final List<DSDocument> documentList, final String emailAddress) {
        return new DSEnvelope() {
            {
                name = envelopeName;
                emailSubject = subject;
                emailBlurb = blurb;
                dsRecipients = recipientList;
                dsDocuments = documentList;
                senderEmailAddress = emailAddress;
                status = "sent";
            }
        };
    }

    private String buildFromTemplate(final String template, final Map<String, Serializable> workflowContext) {
        return StringUtil.isBlank(template) ? "" :
                WorkflowExtensionsUtil.replaceTokens(template, workflowContext);
    }

    private DSRecipient buildRecipient(final int recipientId, final String recipientEmailAddress, final String fullName) {
        return new DSRecipient() {
            {
                dsRecipientId = String.valueOf(recipientId);
                emailAddress = recipientEmailAddress;
                name = fullName;
            }
        };
    }

    @Override
    protected void execute(final KaleoAction kaleoAction, final ExecutionContext executionContext, final WorkflowActionExecutionContext workflowActionExecutionContext, final DocuSignMailerConfigurationWrapper configuration) throws ActionExecutorException {
        final Map<String, Serializable> workflowContext = executionContext.getWorkflowContext();

        try {
            final boolean success = sendMail(workflowContext, configuration);
            if (configuration.isWorkflowStatusUpdatedOnSuccess() && success) {
                updateWorkflowStatus(configuration.getSuccessWorkflowStatus(), workflowContext);
            }
        } catch (final WorkflowException | RuntimeException e) {
            if (configuration.isWorkflowStatusUpdatedOnException()) {
                _log.error("Unexpected exception. See inner exception for details", e);
                try {
                    updateWorkflowStatus(configuration.getExceptionWorkflowStatus(), workflowContext);
                } catch (final WorkflowException ex) {
                    throw new ActionExecutorException("See inner exception", e);
                }
            } else {
                _log.error("Unexpected exception. See inner exception for details", e);
            }
        }
    }

    private FileEntry getDocument(final long groupId, final Map<String, Serializable> workflowContext, final DocuSignMailerConfigurationWrapper configuration) throws ActionExecutorException {
        try {
            final long folderId = configuration.getFolderId();
            final String lookupType = configuration.getDocumentLookupType();
            final String documentIdentifier = getDocumentIdentifier(workflowContext, configuration);
            switch (lookupType) {
                case "file-entry-id":
                    final long fileEntryId;
                    try {
                        fileEntryId = Long.parseLong(documentIdentifier);
                    } catch (NumberFormatException e) {
                        throw new ActionExecutorException("Unable to parse document identifier as a long - " + documentIdentifier);
                    }
                    return dlAppLocalService.getFileEntry(fileEntryId);
                case "title":
                    return dlAppLocalService.getFileEntry(groupId, folderId, documentIdentifier);
                case "external-reference-code":
                    return dlAppLocalService.getFileEntryByExternalReferenceCode(groupId, documentIdentifier);
                case "uuid":
                    return dlAppLocalService.getFileEntryByUuidAndGroupId(documentIdentifier, groupId);
                case "filename":
                    return dlAppLocalService.getFileEntryByFileName(groupId, folderId, documentIdentifier);
                default:
                    return null;
            }
        } catch (PortalException e) {
            throw new ActionExecutorException("Unable to get file entry", e);
        }
    }

    private String getDocumentIdentifier(final Map<String, Serializable> workflowContext, final DocuSignMailerConfigurationWrapper configuration) throws ActionExecutorException {
        if (configuration.isWorkflowKeyUsedForDocument()) {
            final String documentWorkflowContextKey = configuration.getDocumentWorkflowContextKey();
            if (StringUtil.isBlank(documentWorkflowContextKey)) {
                throw new ActionExecutorException("The documentWorkflowContextKey was blank");
            }
            if (!workflowContext.containsKey(documentWorkflowContextKey)) {
                throw new ActionExecutorException(documentWorkflowContextKey + " was not found in the workflowContext");
            }
            return String.valueOf(workflowContext.get(documentWorkflowContextKey));
        }
        final String document = configuration.getDocument();
        if (StringUtil.isBlank(document)) {
            throw new ActionExecutorException("The document was blank");
        }
        return document;
    }

    private String getEmailBody(final Map<String, Serializable> workflowContext, final DocuSignMailerConfigurationWrapper configuration) {
        final String template = configuration.getEmailBodyTemplate();
        return buildFromTemplate(template, workflowContext);
    }

    private String getEmailSubject(final Map<String, Serializable> workflowContext, final DocuSignMailerConfigurationWrapper configuration) {
        final String template = configuration.getEmailSubjectTemplate();
        return buildFromTemplate(template, workflowContext);
    }

    private String getEnvelopeName(final Map<String, Serializable> workflowContext, final DocuSignMailerConfigurationWrapper configuration) throws ActionExecutorException {
        if (configuration.isWorkflowKeyUsedForEnvelopeName()) {
            final String envelopeNameWorkflowContextKey = configuration.getEnvelopeNameWorkflowContextKey();
            if (StringUtil.isBlank(envelopeNameWorkflowContextKey)) {
                throw new ActionExecutorException("The envelopeNameWorkflowContextKey was blank");
            }
            if (!workflowContext.containsKey(envelopeNameWorkflowContextKey)) {
                throw new ActionExecutorException(envelopeNameWorkflowContextKey + " was not found in the workflowContext");
            }
            return String.valueOf(workflowContext.get(envelopeNameWorkflowContextKey));
        }
        final String envelopeName = configuration.getEnvelopeName();
        if (StringUtil.isBlank(envelopeName)) {
            throw new ActionExecutorException("The envelopeName was blank");
        }
        return envelopeName;
    }

    private String getRecipientEmailAddress(final Map<String, Serializable> workflowContext, final DocuSignMailerConfigurationWrapper configuration) throws ActionExecutorException {
        final String recipientWorkflowContextKey = configuration.getRecipientEmailAddressWorkflowContextKey();
        if (StringUtil.isBlank(recipientWorkflowContextKey)) {
            throw new ActionExecutorException("The recipientEmailAddressWorkflowContextKey was blank");
        }
        if (!workflowContext.containsKey(recipientWorkflowContextKey)) {
            throw new ActionExecutorException(recipientWorkflowContextKey + " was not found in the workflowContext");
        }
        return String.valueOf(workflowContext.get(recipientWorkflowContextKey));
    }

    private String getRecipientFullName(final Map<String, Serializable> workflowContext, final DocuSignMailerConfigurationWrapper configuration) throws ActionExecutorException {
        final String recipientWorkflowContextKey = configuration.getRecipientFullNameWorkflowContextKey();
        if (StringUtil.isBlank(recipientWorkflowContextKey)) {
            throw new ActionExecutorException("The recipientFullNameWorkflowContextKey was blank");
        }
        if (!workflowContext.containsKey(recipientWorkflowContextKey)) {
            throw new ActionExecutorException(recipientWorkflowContextKey + " was not found in the workflowContext");
        }
        return String.valueOf(workflowContext.get(recipientWorkflowContextKey));
    }

    private String getSenderEmailAddress(final Map<String, Serializable> workflowContext, final DocuSignMailerConfigurationWrapper configuration) throws ActionExecutorException {
        if (configuration.isWorkflowKeyUsedForSenderEmailAddress()) {
            final String senderWorkflowContextKey = configuration.getSenderEmailAddressWorkflowContextKey();
            if (StringUtil.isBlank(senderWorkflowContextKey)) {
                throw new ActionExecutorException("The recipientWorkflowContextKey was blank");
            }
            if (!workflowContext.containsKey(senderWorkflowContextKey)) {
                throw new ActionExecutorException(senderWorkflowContextKey + " was not found in the workflowContext");
            }
            return String.valueOf(workflowContext.get(senderWorkflowContextKey));
        }
        final String senderEmailAddress = configuration.getSenderEmailAddress();
        if (StringUtil.isBlank(senderEmailAddress)) {
            throw new ActionExecutorException("The senderEmailAddress was blank");
        }
        return senderEmailAddress;
    }

    @Override
    protected DocuSignMailerSettingsHelper getSettingsHelper() {
        return docuSignMailerSettingsHelper;
    }

    @Override
    protected WorkflowActionExecutionContextService getWorkflowActionExecutionContextService() {
        return workflowActionExecutionContextService;
    }

    private DSEnvelope sendEnvelope(final long companyId, final long siteGroupId, final DSEnvelope envelope) {
        envelope.setStatus("sent");
        return dsEnvelopeManager.addDSEnvelope(
                companyId, siteGroupId,
                envelope);
    }

    private boolean sendMail(Map<String, Serializable> workflowContext, DocuSignMailerConfigurationWrapper configuration) throws ActionExecutorException {
        final long companyId = GetterUtil.getLong(workflowContext.get(WorkflowConstants.CONTEXT_COMPANY_ID));
        final long groupId = GetterUtil.getLong((workflowContext.get(WorkflowConstants.CONTEXT_GROUP_ID)));

        _log.trace("companyId: {}, groupId: {}", companyId, groupId);

        final FileEntry fileEntry = getDocument(groupId, workflowContext, configuration);

        if (fileEntry == null) {
            _log.warn("The file entry was null");
            return false;
        }

        _log.trace("fileEntry: {}", fileEntry);

        final DSDocument document = buildDocument(fileEntry);

        final int recipientId = 1;
        final String emailAddress = getRecipientEmailAddress(workflowContext, configuration);
        final String fullName = getRecipientFullName(workflowContext, configuration);
        final DSRecipient recipient = buildRecipient(recipientId, emailAddress, fullName);

        final String envelopeName = getEnvelopeName(workflowContext, configuration);
        final String emailSubject = getEmailSubject(workflowContext, configuration);
        final String emailBlurb = getEmailBody(workflowContext, configuration);

        final String senderEmailAddress = getSenderEmailAddress(workflowContext, configuration);

        final DSEnvelope envelope = sendEnvelope(
                companyId, groupId,
                buildEnvelope(
                        envelopeName, emailSubject, emailBlurb,
                        Collections.singletonList(recipient),
                        Collections.singletonList(document),
                        senderEmailAddress
                ));

        _log.debug("dsEnvelopeId: {}", envelope.getDSEnvelopeId());
        _log.trace("status: {}", envelope.getStatus());

        return !StringUtil.isBlank(envelope.getDSEnvelopeId());
    }

    protected void updateWorkflowStatus(final int status, final Map<String, Serializable> workflowContext) throws WorkflowException {
        try {
            if (status > -1) {
                if (_log.isDebugEnabled()) {
                    final String workflowLabelStatus = WorkflowConstants.getStatusLabel(status);
                    _log.debug("Setting workflow status to {} [{}]", workflowLabelStatus, status);
                }
                WorkflowStatusManagerUtil.updateStatus(status, workflowContext);
            }
        } catch (final WorkflowException e) {
            throw new WorkflowException("Unable to update workflow status", e);
        }
    }
}
