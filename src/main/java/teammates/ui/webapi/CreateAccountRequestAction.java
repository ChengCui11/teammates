package teammates.ui.webapi;

import teammates.common.datatransfer.AccountRequestStatus;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.EmailWrapper;
import teammates.storage.sqlentity.AccountRequest;
import teammates.ui.output.JoinLinkData;
import teammates.ui.request.AccountCreateRequest;
import teammates.ui.request.InvalidHttpRequestBodyException;

/**
 * Creates a new account request.
 */
public class CreateAccountRequestAction extends AdminOnlyAction {

    @Override
    public JsonResult execute()
            throws InvalidHttpRequestBodyException, InvalidOperationException {
        AccountCreateRequest createRequest = getAndValidateRequestBody(AccountCreateRequest.class);

        String instructorName = createRequest.getInstructorName().trim();
        String instructorEmail = createRequest.getInstructorEmail().trim();
        String instructorInstitution = createRequest.getInstructorInstitution().trim();
        // TODO: This is a placeholder. It should be obtained from AccountCreateRequest, in a separate PR.
        String comments = "PLACEHOLDER";

        AccountRequest accountRequest;

        try {
            accountRequest = sqlLogic.createAccountRequest(instructorName, instructorEmail, instructorInstitution,
                    AccountRequestStatus.PENDING, comments);
            taskQueuer.scheduleAccountRequestForSearchIndexing(instructorEmail, instructorInstitution);
        } catch (InvalidParametersException ipe) {
            throw new InvalidHttpRequestBodyException(ipe);
        }

        assert accountRequest != null;

        if (accountRequest.getRegisteredAt() != null) {
            throw new InvalidOperationException("Cannot create account request as instructor has already registered.");
        }

        String joinLink = accountRequest.getRegistrationUrl();

        EmailWrapper email = emailGenerator.generateNewInstructorAccountJoinEmail(
                instructorEmail, instructorName, joinLink);
        emailSender.sendEmail(email);
        EmailWrapper adminAlertEmail = sqlEmailGenerator.generateNewAccountRequestAdminAlertEmail(accountRequest);
        emailSender.sendEmail(adminAlertEmail);
        JoinLinkData output = new JoinLinkData(joinLink);
        return new JsonResult(output);
    }

}
