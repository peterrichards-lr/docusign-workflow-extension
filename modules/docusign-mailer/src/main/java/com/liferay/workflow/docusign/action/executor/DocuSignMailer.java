package com.liferay.workflow.docusign.action.executor;

import com.liferay.digital.signature.manager.DSEnvelopeManager;
import com.liferay.digital.signature.model.DSEnvelope;
import com.liferay.digital.signature.model.DSDocument;
import com.liferay.digital.signature.model.DSRecipient;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.util.Base64;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.IntegerWrapper;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.kernel.workflow.WorkflowException;
import com.liferay.portal.kernel.workflow.WorkflowStatusManager;
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
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
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
    private DocuSignMailerSettingsHelper docuSignMailerSettingsHelper;

    @Reference
    private WorkflowActionExecutionContextService workflowActionExecutionContextService;

    @Reference
    private WorkflowStatusManager workflowStatusManager;

    @Reference
    private DSEnvelopeManager dsEnvelopeManager;

    @Reference
    private DLAppLocalService dlAppLocalService;

    @Override
    protected WorkflowActionExecutionContextService getWorkflowActionExecutionContextService() {
        return workflowActionExecutionContextService;
    }

    @Override
    protected DocuSignMailerSettingsHelper getSettingsHelper() {
        return docuSignMailerSettingsHelper;
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

    private boolean sendMail(Map<String, Serializable> workflowContext, DocuSignMailerConfigurationWrapper configuration) throws ActionExecutorException {
        final long companyId = GetterUtil.getLong(workflowContext.get(WorkflowConstants.CONTEXT_COMPANY_ID));
        final long groupId = GetterUtil.getLong((workflowContext.get( WorkflowConstants.CONTEXT_GROUP_ID)));
        final long folderId = configuration.getFolderId();

        _log.info("companyId: {}, groupId: {}, folderId: {}", companyId, groupId, folderId);

        final FileEntry fileEntry;
        try {
            fileEntry = dlAppLocalService.getFileEntryByFileName(groupId, folderId, "blank.pdf");
        } catch (PortalException e) {
            throw new ActionExecutorException("Unable to get file entry", e);
        }

        if (fileEntry == null) {
            _log.warn("The file entry was null");
            return false;
        }

        _log.info("fileEntry: {}", fileEntry);

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
        } catch (IOException e) {
            throw new ActionExecutorException("Unable to read content of stream", e);
        } finally {
            try {
                dis.close();
            } catch (IOException e) {
                _log.error("Failed to close stream", e);
            }
        }

        /*
        Based on the following code:

        https://github.com/liferay/liferay-portal/blob/4c2eee0f7807199ec66fa947d6a22dcfc5b186cc/modules/apps/digital-signature/digital-signature-test/src/testIntegration/java/com/liferay/digital/signature/manager/test/DSEnvelopeManagerTest.java
        https://github.com/liferay/liferay-portal/blob/4c2eee0f7807199ec66fa947d6a22dcfc5b186cc/modules/apps/digital-signature/digital-signature-web/src/main/java/com/liferay/digital/signature/web/internal/portlet/action/AddDSEnvelopeMVCResourceCommand.java
         */

        final IntegerWrapper integerWrapper = new IntegerWrapper();
        final DSEnvelope dsEnvelope = dsEnvelopeManager.addDSEnvelope(
                companyId, groupId,
        new DSEnvelope() {
            {
                dsDocuments = Collections.singletonList(
                        new DSDocument() {
                            {
                                data = Base64.encode(fileEntryData);
                                dsDocumentId = String.valueOf(fileEntry.getFileEntryId());
                                name = fileEntry.getFileName();
                                fileExtension = fileEntry.getExtension();
                            }
                        });
                dsRecipients = Collections.singletonList(
                        new DSRecipient() {
                            {
                                dsRecipientId = String.valueOf(integerWrapper.increment());
                                emailBlurb = "peter.richards@liferay.com";
                                name = "Peter Richards";
                            }
                        });
                emailBlurb = "This is a test email";
                emailSubject = "DocuSign Example";
                name = "Peter Richards";
                senderEmailAddress = "liferaybotics@liferay.com";
                status = "sent";
            }
        });
        _log.info("dsEnvelopeId: {}", dsEnvelope.getDSEnvelopeId());
        _log.info("status: {}", dsEnvelope.getStatus());
        return true;
    }

    private void updateWorkflowStatus(final int status, final Map<String, Serializable> workflowContext) throws WorkflowException {
        try {
            if (status > -1) {
                if (_log.isDebugEnabled()) {
                    final String workflowLabelStatus = WorkflowConstants.getStatusLabel(status);
                    _log.debug("Setting workflow status to {} [{}]", workflowLabelStatus, status);
                }
                workflowStatusManager.updateStatus(status, workflowContext);
            }
        } catch (final WorkflowException e) {
            throw new WorkflowException("Unable to update workflow status", e);
        }
    }
}
