package com.example.abren.responses

import com.example.abren.models.Request

data class RequestsResponse(var requested: List<Request?>,
                            var accepted: List<Request?>)
