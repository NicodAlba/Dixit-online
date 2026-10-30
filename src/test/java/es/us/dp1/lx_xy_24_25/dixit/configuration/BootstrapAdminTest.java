package es.us.dp1.lx_xy_24_25.dixit.configuration;

import java.sql.Connection;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BootstrapAdminTest {
    @Test void invalidInputNeverTouchesDatabase() throws Exception {
        Connection connection = mock(Connection.class);
        char[] valid = "Synthetic valid fixture".toCharArray();
        for (String username : new String[]{"", " ", " admin", "x".repeat(51)}) {
            assertThrows(IllegalArgumentException.class, () -> BootstrapAdmin.create(connection, username, valid, valid));
        }
        assertThrows(IllegalArgumentException.class, () -> BootstrapAdmin.create(connection, "operator", "short".toCharArray(), "short".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> BootstrapAdmin.create(connection, "operator", valid, "Mismatched fixture".toCharArray()));
        verifyNoInteractions(connection);
    }
    @Test void bcryptLimitIsUtf8BytesAndTransactionMustBeDedicated() throws Exception {
        char[] tooLong = "é".repeat(37).toCharArray();
        assertThrows(IllegalArgumentException.class, () -> BootstrapAdmin.validateInput("operator", tooLong, tooLong));
        Connection connection = mock(Connection.class);
        when(connection.getAutoCommit()).thenReturn(false);
        char[] password = "Synthetic valid fixture".toCharArray();
        assertThrows(IllegalStateException.class, () -> BootstrapAdmin.create(connection, "operator", password, password));
        verify(connection, never()).setAutoCommit(anyBoolean());
    }
}
