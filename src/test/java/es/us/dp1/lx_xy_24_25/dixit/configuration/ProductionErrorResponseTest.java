package es.us.dp1.lx_xy_24_25.dixit.configuration;

import es.us.dp1.lx_xy_24_25.dixit.exceptions.ExceptionHandlerController;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.ServletWebRequest;
import static org.junit.jupiter.api.Assertions.*;

class ProductionErrorResponseTest {
    @Test void unexpectedProductionErrorsDoNotReturnDatabaseDetails() {
        var advice = new ExceptionHandlerController();
        ReflectionTestUtils.setField(advice, "includeInternalDetails", false);
        var response = advice.globalExceptionHandler(new RuntimeException("SQL credentials=private-value"),
            new ServletWebRequest(new MockHttpServletRequest("GET", "/api/v1/games")));
        assertEquals(500, response.getStatusCode().value());
        assertEquals("An unexpected error occurred.", response.getBody().getMessage());
    }
    @Test void missingStaticResourcesReturn404InsteadOfGeneric500() {
        var response = new ExceptionHandlerController().missingStaticResource();
        assertEquals(404, response.getStatusCode().value());
    }
}
