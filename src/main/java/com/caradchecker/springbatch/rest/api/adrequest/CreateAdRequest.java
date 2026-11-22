package com.caradchecker.springbatch.rest.api.adrequest;

import com.fasterxml.jackson.annotation.JsonAlias;

public record CreateAdRequest(@JsonAlias("lien") String url, @JsonAlias("langue") String language, @JsonAlias("pays") String country) {
}
