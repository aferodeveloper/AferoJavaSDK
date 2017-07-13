/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;


/*
    {
        "error": "validationFailure",
        "error_description": "One or more fields have failed validation rules. See attached fieldErrors for specifics.",
        "fieldErrors": [
            {
                "error": "Length",
                "error_description": "length must be between 6 and 255",
                "field": "credential.password",
                "rejectedValue": "dhj"
            }
        ]
    }
*/

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorBody {

    public static final String ERROR_VALIDATION_FAILURE = "validationFailure";
    public static final String ERROR_ALREADY_EXISTS = "alreadyExists";
    public static final String ERROR_BAD_REQUEST = "invalid_grant";
    public static final String ERROR_UNAUTHORIZED = "unauthorized";
    public static final String FIELD_CREDENTIAL_ID = "credential.credentialId";
    public static final String FIELD_CREDENTIAL_PASSWORD = "credential.password";
    public static final String FIELD_USER_FIRSTNAME = "user.firstname";
    public static final String FIELD_USER_LASTNAME = "user.lastname";
    public static final String FIELD_ERROR_LENGTH = "Length";
    public static final String FIELD_ERROR_NOT_EMPTY = "NotEmpty";
    public static final String FIELD_ERROR_REJECTED_VALUE = "rejectedValue";

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FieldError {
        public String error;
        public String error_description;
        public String field;
        public String rejectedValue;

        public boolean isField(String f) {
            return field != null && field.equalsIgnoreCase(f);
        }

        public boolean isFieldError(String fe) {
            return error != null && error.equalsIgnoreCase(fe);
        }
    }

    public String error;
    public String error_description;
    public FieldError[] fieldErrors;

    @JsonIgnore
    public boolean isError(String err) {
        return error != null && error.equalsIgnoreCase(err);
    }
}
