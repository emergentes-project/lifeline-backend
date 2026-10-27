package com.upc.pe.lifeline.shared.interfaces.rest.configuration;

import com.upc.pe.lifeline.shared.domain.exceptions.DomainException;
import com.upc.pe.lifeline.shared.interfaces.rest.GlobalExceptionHandler;
import com.upc.pe.lifeline.shared.interfaces.rest.resources.MessageResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestExceptionHandlerTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler(), new RestExceptionHandler()).build();
    }

    @Test
    void domainErrorsKeepTheirHttpStatusAndPublicMessage() throws Exception {
        mvc.perform(get("/test/domain-error"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Case not found"));
    }

    @Test
    void unsupportedImageRequestsExplainTheExpectedContentType() throws Exception {
        mvc.perform(post("/test/images").contentType(MediaType.APPLICATION_OCTET_STREAM).content("image"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message", containsString("multipart/form-data")));
    }

    @Test
    void missingImagePartsReturnABadRequestWithThePartName() throws Exception {
        mvc.perform(multipart("/test/images"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required multipart part 'file' is missing"));
    }

    @RestController
    public static class ProbeController {
        @GetMapping("/test/domain-error")
        public void domainError() {
            throw new DomainException("Case not found", HttpStatus.NOT_FOUND) { };
        }

        @PostMapping(value = "/test/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public MessageResource image(@RequestPart("file") MultipartFile file) {
            return new MessageResource("Image received");
        }
    }
}
