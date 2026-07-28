package pro.masterdoc.feature.grants

/**
 * Resolves IdP project grant keys for a user in an org.
 * Used by service-to-service eligibility checks (e.g. WO assignee must have `equipment`).
 */
fun interface UserGrantsLookup {
    fun grantKeys(orgId: String, userId: String): List<String>
}
