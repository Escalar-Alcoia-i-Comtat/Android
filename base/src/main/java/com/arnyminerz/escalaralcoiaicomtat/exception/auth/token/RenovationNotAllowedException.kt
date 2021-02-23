package com.arnyminerz.escalaralcoiaicomtat.exception.auth.token

import java.lang.Exception

class RenovationNotAllowedException(reason: String): Exception("Token renovation is not allowed. Reason: $reason")