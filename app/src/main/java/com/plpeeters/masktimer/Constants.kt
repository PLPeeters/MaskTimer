package com.plpeeters.masktimer

const val STOP_WEARING_EXTRA = "STOP_WEARING"
const val PAUSE_OR_RESUME_WEARING_EXTRA = "PAUSE_OR_RESUME_WEARING"
const val SWAP_MASK_EXTRA = "SWAP_MASK"
const val REPLACE_EXTRA = "REPLACE"
const val MASK_EXTRA = "MASK"
const val ACTION_STOP_WEARING = "STOP_WEARING"
const val ACTION_REPLACE = "REPLACE"
const val ACTION_DELETE = "DELETE"
const val MASK_ALARM_REQUEST_CODE = 0


object Preferences {
    const val ACTIONS_SHOWN = "actions_shown"
    const val SURGICAL_MASK_EXPIRATION_HOURS = "surgical_mask_expiration_hours"
    const val FFP_MASK_EXPIRATION_HOURS = "ffp_mask_expiration_hours"
    const val PREVENT_EXPIRATION_NOTIFICATION_DISMISSAL = "prevent_expiration_notification_dismissal"
}
