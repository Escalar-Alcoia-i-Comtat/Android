package com.arnyminerz.escalaralcoiaicomtat.exception

import org.json.JSONObject

class JSONResultException(jsonObject: JSONObject, message: String) :
    Exception(message + jsonObject.toString())