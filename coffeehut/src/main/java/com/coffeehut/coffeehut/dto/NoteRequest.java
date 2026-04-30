// NoteRequest.java — Request body for the add-note endpoint -WeiqiWang

package com.coffeehut.coffeehut.dto;

import lombok.Data;

/**
 * Request body for {@code PATCH /api/staff/orders/{id}/note}.
 * <p>
 * Carries the note text submitted by staff via the Add Note modal
 * on the Order Detail page. Stored in {@code Order.staffNotes}.
 * </p>
 */
@Data
public class NoteRequest {
    /** The note content entered by the staff member. */
    private String note;
}
