package com.ga.leave.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for updating a user's profile details.
 * All fields are optional; only non-null values will be applied.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    /** Updated first name. Null means no change. */
    private String firstName;

    /** Updated last name. Null means no change. */
    private String lastName;

    /** Updated phone number. Null means no change. */
    private Long phoneNumber;
}
