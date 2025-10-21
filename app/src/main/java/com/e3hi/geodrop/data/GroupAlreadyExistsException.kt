package com.e3hi.geodrop.data

/**
 * Indicates that an attempt was made to create a group with a code that already exists.
 */
class GroupAlreadyExistsException(val code: String) :
    Exception("Group $code already exists.")