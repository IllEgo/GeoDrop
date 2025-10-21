package com.e3hi.geodrop.data

/**
 * Indicates that a requested group could not be found in Firestore.
 */
class GroupNotFoundException(val code: String) : Exception("Group $code doesn't exist.")